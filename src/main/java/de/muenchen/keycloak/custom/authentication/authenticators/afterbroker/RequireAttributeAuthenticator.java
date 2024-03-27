package de.muenchen.keycloak.custom.authentication.authenticators.afterbroker;

import de.muenchen.keycloak.custom.ScopesHelper;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.*;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RequireAttributeAuthenticator implements Authenticator {

    protected static final Logger logger = Logger.getLogger(RequireAttributeAuthenticator.class);

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public void authenticate(final AuthenticationFlowContext context) {
        final RealmModel realm = context.getRealm();
        final UserModel user = context.getUser();
        final AuthenticatorConfigModel authenticatorConfig = context.getAuthenticatorConfig();
        String errorString;

        final String attributeName = resolveConfiguration(authenticatorConfig.getConfig(), RequireAttributeAuthenticatorFactory.ATTRIBUTE);
        final String attributeValues = resolveConfiguration(authenticatorConfig.getConfig(), RequireAttributeAuthenticatorFactory.ATTRIBUTE_VALUES);
        String dependOnScopes = resolveConfiguration(authenticatorConfig.getConfig(), RequireAttributeAuthenticatorFactory.CONDITIONAL_SCOPE);
        String regExp = resolveConfiguration(authenticatorConfig.getConfig(), RequireAttributeAuthenticatorFactory.REG_EXP);
        final String customError = resolveConfiguration(authenticatorConfig.getConfig(), RequireAttributeAuthenticatorFactory.CUSTOM_ERROR);

        if (dependOnScopes == null || !dependOnScopes.equals("true")) {
            dependOnScopes = "false";
        }

        if (regExp == null || !regExp.equals("true")) {
            regExp = "false";
        }

        if (attributeName == null || attributeName.trim().equalsIgnoreCase("")
        || attributeValues == null || attributeValues.trim().equalsIgnoreCase("")) {
            logger.error("Wrong / missing konfiguration in require-attribute-plugin! AttributeName " + attributeName
                    + " attributeValues " + attributeValues + " dependOnScopes " + dependOnScopes
                    + " regExp " + regExp);
            errorString = "Login nicht möglich wegen fehlerhafter Konfiguration.";
            //Fehlerhafte Konfiguration - keine weitere Prüfung
        } else {

            //attribute values sind mit ## getrennt
            final List<String> attributeValuesArray = Arrays.asList(attributeValues.trim().split("##"));
            logger.info("One of these attribute values is required as per config: " + String.join(", ", attributeValuesArray));

            List<String> filteredAttributeValues = attributeValuesArray;
            if (dependOnScopes.equalsIgnoreCase("true") && regExp.equalsIgnoreCase("false")) {
                logger.info("Depend-on-scopes is active and no regexp - filtering required attribute values by scopes assigned in client.");
                //dependOnScopes ist gesetzt und keine regulären Expressions aktiv
                filteredAttributeValues = filterValidAttributesByScopes(context, attributeValuesArray);

            }

            if (isUserWithAttribute(context, user, attributeName, filteredAttributeValues, regExp)) {
                //Erfolg
                context.success();
                return;
            }

            if (customError != null && !customError.trim().equalsIgnoreCase("")) {
                errorString = customError;
            } else {
                errorString = createErrorString(user, attributeName, filteredAttributeValues);
            }
        }

        logger.infof("Access denied because of missing attribute. realm=%s username=%s attribute=%s attributeValues=%s",
                realm.getName(), user.getUsername(), attributeName, attributeValues);

        //Variante Login-Seite (mit vorherigem Logout des Users)
        //User aus jeder seiner Sessions innerhalb dieses Realms ausloggen
        logoutUser(context, user);

        //Login-Seite mit Fehlermeldung anzeigen
        final Response challenge = context.form()
                .setStatus(Response.Status.FORBIDDEN)
                .setError(errorString)
                .createLoginUsernamePassword();

        context.forceChallenge(challenge);
    }


    private void logoutUser(final AuthenticationFlowContext context, final UserModel user) {
        if (context == null || context.getRealm() == null || context.getSession() == null || context.getSession().sessions() == null) {
            return;
        }

        final UserSessionProvider usp = context.getSession().sessions();
        final RealmModel realm = context.getRealm();
        final List<UserSessionModel> userSessions = usp.getUserSessionsStream(realm, user).collect(Collectors.toList());
        logger.info("Found " + userSessions.size() + " Sessions of user " + user.getUsername() + " in realm " + realm.getName() + " to kill.");
        for (UserSessionModel userSession : userSessions) {
            usp.removeUserSession(realm, userSession);
        }
    }

    /**
     * Prüft, ob der aktuelle client einen der im String angegebenen Scopes als default oder optional scope gesetzt hat.
     * @param context AuthenticationFlowContext
     * @param attributeValues List<String>
     * @return reduzierte Liste
     */
    private List<String> filterValidAttributesByScopes(final AuthenticationFlowContext context, final List<String> attributeValues) {

        //hole alle client scopes (default und optional)
        final List<String> clientScopes = ScopesHelper.getClientScopes(context);

        if (clientScopes == null) {
            //shortcut: wenn keine scopes definiert sind, brauchen wir auch nicht filtern
            return attributeValues;
        }

        //entferne Suffix "_oidc" oder "_saml" falls existent
        final List<String> strippedClientScopes  = ScopesHelper.stripScopes(clientScopes);

        final List<String> filteredAttributeValues = attributeValues.stream().filter(attributeValue ->
                (strippedClientScopes != null && strippedClientScopes.contains(attributeValue)))
        .collect(Collectors.toList());

        logger.info("Filtered attribute values from config: " + String.join(", " , filteredAttributeValues));

        return filteredAttributeValues;
    }



    /**
     * Prüft, ob der angegebene User im Attribut attributeName einen der Werte aus attributeValues hat.
     * @param user User zur Prüfung
     * @param attributeName attributName zur Prüfung
     * @param attributeValues die Werte, auf deren Existenz geprüft werden soll
     * @param regExp "true" wenn reguläre Expressions aktiv
     * @return true wenn Wert vorhanden, false sonst
     */
    private boolean isUserWithAttribute(final AuthenticationFlowContext context, final UserModel user, final String attributeName, final List<String> attributeValues, final String regExp) {
        if (user == null || user.getAttributeStream(attributeName) == null) {
            logger.info("Could not find attribute " + attributeName + " on user  " + (user != null ? user.getUsername() : "UNKNOWN"));
            return false;
        }

        if (attributeValues == null || attributeValues.isEmpty()) {
            String client = "UNKNOWN";
            if (context.getAuthenticationSession() != null && context.getAuthenticationSession().getClient() != null) {
                client = context.getAuthenticationSession().getClient().getName();
            }
            logger.warn("No attribute values on require-attribute step for attribute " + attributeName + " configured " +
                    "or all values filtered due to inadequate scopes in client " + client + ".");
            return false;
        }
        final List<String> userAttributeValues = user.getAttributeStream(attributeName).collect(Collectors.toList());
        if (!userAttributeValues.isEmpty()) {
            if (userAttributeValues.size() > 1) {
                logger.error("Found more than one attribute value on user --> error, but taking first one");
            }
            final String userAttributeValue = userAttributeValues.get(0);
            logger.debug("Found attribute " + attributeName + " on user with value " + userAttributeValue);
            logger.debug("Comparing " + attributeValues.get(0) + " with " + userAttributeValue + " is " + attributeValues.get(0).matches(userAttributeValue));

            boolean match = false;
            if (regExp.equalsIgnoreCase("true") &&
                    attributeValues.stream().anyMatch(userAttributeValue::matches)) {
                //RegExp-Matching darf nur genutzt werden wenn explizit aktiviert, sonst abweichendes Ergebnis
                match = true;
            } else if (attributeValues.contains(userAttributeValue)) {
                match = true;
            }

            if (match) {
                logger.info("Found matching value " + userAttributeValue + " on attribute " + attributeName);
                return true;
            }
        }
        logger.info("Did not find matching value on attribute " + attributeName);
        return false;
    }

    /**
     * Erzeugt einen Error-String aus den angegebenen Parametern (für die Login-Seite).
     * @param user UserModel
     * @param attributeName der attributeName
     * @param attributeValues List<String>
     * @return erzeugter Error-String
     */
    private String createErrorString(final UserModel user, final String attributeName, final List<String> attributeValues) {
        final String userName = user.getUsername();
        final String attributeValuesString = String.join(", ", attributeValues);
        if (attributeValuesString.trim().equalsIgnoreCase("")) {
            return "User " + userName + " hat in Attribut " + attributeName + " keinen zulässigen Wert.";
        }
        return "User " + userName + " hat in Attribut " + attributeName + " keinen der Werte " + attributeValuesString;
    }

    /**
     * Fetches Configuration item with the given key from the given config Map.
     * @param config the config map
     * @param key the key
     * @return the entry
     */
    protected String resolveConfiguration(final Map<String, String> config, final String key) {

        if (config == null) {
            return null;
        }

        final String value = config.get(key);
        if (value == null) {
            return "";
        }

        return value.trim();
    }

    @Override
    public void action(AuthenticationFlowContext context) {

    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
    }

    @Override
    public void close() {
    }
}
