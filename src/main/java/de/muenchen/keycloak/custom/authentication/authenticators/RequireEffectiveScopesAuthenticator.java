package de.muenchen.keycloak.custom.authentication.authenticators;

import de.muenchen.keycloak.custom.broker.saml.PreprocessorHelper;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.*;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class RequireEffectiveScopesAuthenticator implements Authenticator {

    protected static final Logger logger = Logger.getLogger(RequireEffectiveScopesAuthenticator.class);

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public void authenticate(final AuthenticationFlowContext context) {
        logger.info("In RequireEffectiveScopesAuthenticator --> authenticate");

        final RealmModel realm = context.getRealm();
        final UserModel user = context.getUser();
        final AuthenticatorConfigModel authenticatorConfig = context.getAuthenticatorConfig();
        String errorString;

        final String attributeName = resolveConfiguration(authenticatorConfig.getConfig(), de.muenchen.keycloak.custom.authentication.authenticators.RequireEffectiveScopesAuthenticatorFactory.ATTRIBUTE);
        final String customError = resolveConfiguration(authenticatorConfig.getConfig(), de.muenchen.keycloak.custom.authentication.authenticators.RequireEffectiveScopesAuthenticatorFactory.CUSTOM_ERROR);


        if (attributeName == null || attributeName.trim().equalsIgnoreCase("")) {
            logger.error("Wrong / missing konfiguration in require-effective-scopes-plugin! AttributeName " + attributeName);
            errorString = "Login nicht möglich wegen fehlerhafter Konfiguration.";
            //Fehlerhafte Konfiguration - keine weitere Prüfung
        } else {

            Set<String> effectiveScopes = PreprocessorHelper.collectEffectiveScopes(context.getAuthenticationSession());
            Set<String> deductedScopes = PreprocessorHelper.enhanceEffectiveScopes(effectiveScopes, context.getAuthenticationSession());

            if (isUserWithAttribute(context, user, attributeName, deductedScopes)) {
                //Erfolg
                context.success();
                return;
            }

            if (customError != null && !customError.trim().equalsIgnoreCase("")) {
                errorString = customError;
            } else {
                errorString = createErrorString(user, attributeName, deductedScopes);
            }
        }

        logger.infof("Access denied because of missing attribute. realm=%s username=%s attribute=%s",
                realm.getName(), user.getUsername(), attributeName);

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
     * Prüft, ob der angegebene User im Attribut attributeName einen der Werte aus attributeValues hat.
     * @param user User zur Prüfung
     * @param attributeName attributName zur Prüfung
     * @return true wenn Wert vorhanden, false sonst
     */
    private boolean isUserWithAttribute(final AuthenticationFlowContext context, final UserModel user, final String attributeName, final Set<String> effectiveScopes) {
        // user.getAttribute(attributeName) ist deprecated
        // Neue Weg via Stream
        // user.getAttributeStream(attributeName).collect(Collectors.toList());

        if (user == null || user.getAttributeStream(attributeName) == null) {
            logger.info("Could not find attribute " + attributeName + " on user  " + (user != null ? user.getUsername() : "UNKNOWN"));
            return false;
        }

        final List<String> userAttributeValues = user.getAttributeStream(attributeName).collect(Collectors.toList());
        logger.info("Checking scopes " + String.join(",", effectiveScopes) + " against values " + String.join(",", userAttributeValues));
        if (userAttributeValues.size() > 0) {
            if (userAttributeValues.containsAll(effectiveScopes)) {
                logger.info("Found all attribute values " + String.join("", userAttributeValues));
                return true;
            } else {
                logger.info("Did not find all attribute values " + String.join("", userAttributeValues));
            }
        }
        return false;
    }

    /**
     * Erzeugt einen Error-String aus den angegebenen Parametern (für die Login-Seite).
     * @param user
     * @param attributeName
     * @param deductedScopes
     * @return
     */
    private String createErrorString(final UserModel user, final String attributeName, final Set<String> deductedScopes) {
        final String userName = user.getUsername();
        final String attributeValuesString = user.getAttributeStream(attributeName).collect(Collectors.joining(", "));
        final String deductedScopesString = String.join(", ", deductedScopes);
        if (attributeValuesString.trim().equalsIgnoreCase("")) {
            return "User " + userName + " hat keine Scopes gesetzt.";
        }
        return "User " + userName + " hat in der aktuellen Login-Session nicht alle benötigten Scopes " + deductedScopesString + ".\n\n"
                + "Der User hat nur diese Scopes: " + attributeValuesString;
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
