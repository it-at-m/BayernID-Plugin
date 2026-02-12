package de.muenchen.keycloak.custom.authentication.authenticators;

import de.muenchen.keycloak.custom.broker.saml.PreprocessorHelper;
import jakarta.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.*;

/**
 * Authenticator that checks whether all relevant scopes of the current account-type (BayernID, BundID, ELSTER_NEZO)
 * that are required on the target client are present on the current user when doing a client-switch through SSO.
 */
public class RequireEffectiveScopesAuthenticator implements Authenticator {

    protected static final Logger logger = Logger.getLogger(RequireEffectiveScopesAuthenticator.class);

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
        String errorStringInternal;

        final String attributeName = resolveConfiguration(authenticatorConfig.getConfig(),
                de.muenchen.keycloak.custom.authentication.authenticators.RequireEffectiveScopesAuthenticatorFactory.ATTRIBUTE);
        final String customError = resolveConfiguration(authenticatorConfig.getConfig(),
                de.muenchen.keycloak.custom.authentication.authenticators.RequireEffectiveScopesAuthenticatorFactory.CUSTOM_ERROR);
        final String accountSource = resolveConfiguration(authenticatorConfig.getConfig(),
                RequireEffectiveScopesAuthenticatorFactory.ACCOUNT_SOURCE);
        final String restrictedScopes = resolveConfiguration(authenticatorConfig.getConfig(),
                RequireEffectiveScopesAuthenticatorFactory.RESTRICTED_SCOPES);

        if (attributeName == null || attributeName.trim().equalsIgnoreCase("")) {
            logger.error("Wrong / missing konfiguration in require-effective-scopes-plugin! AttributeName " + attributeName);
            errorString = "Login nicht möglich wegen fehlerhafter Konfiguration.";
            errorStringInternal = errorString;
            //Fehlerhafte Konfiguration - keine weitere Prüfung
        } else if (accountSource != null
                && !isUserWithAttribute(user, RequireEffectiveScopesAuthenticatorFactory.ACCOUNT_SOURCE, Set.of(accountSource))) {
                    logger.info("Trigger only applicable for accountSource " + accountSource + " - skipping.");
                    context.success();
                    return;
                } else {

                    Set<String> effectiveScopes = PreprocessorHelper.collectEffectiveScopes(context.getAuthenticationSession());
                    Set<String> deductedScopes = PreprocessorHelper.enhanceEffectiveScopes(effectiveScopes, context.getAuthenticationSession());

                    Set<String> scopesToCheck;
                    if (accountSource == null || accountSource.isEmpty()) {
                        scopesToCheck = deductedScopes;
                    } else {
                        scopesToCheck = restrictScopes(deductedScopes, restrictedScopes);
                        logger.debug("Restricted scopes FROM: " + String.join(", ", deductedScopes) + " TO: " + String.join(", ", scopesToCheck));
                    }

                    if (isUserWithAttribute(user, attributeName, scopesToCheck)) {
                        //Erfolg
                        context.success();
                        return;
                    }

                    errorStringInternal = createErrorString(user, attributeName, deductedScopes);
                    if (customError != null && !customError.trim().equalsIgnoreCase("")) {
                        errorString = customError;
                    } else {
                        errorString = errorStringInternal;
                    }
                }

        logger.infof("Access denied because of missing scope(s). realm=%s error-message=%s",
                realm.getName(), errorStringInternal);

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

    private Set<String> restrictScopes(Set<String> scopes, String restrictedScopes) {
        final List<String> restrictedScopesList = Arrays.asList(restrictedScopes.trim().split("##"));
        return scopes.stream().filter(restrictedScopesList::contains).collect(Collectors.toSet());
    }

    private void logoutUser(final AuthenticationFlowContext context, final UserModel user) {
        if (context == null || context.getRealm() == null || context.getSession() == null || context.getSession().sessions() == null) {
            return;
        }

        final UserSessionProvider usp = context.getSession().sessions();
        final RealmModel realm = context.getRealm();
        final List<UserSessionModel> userSessions = usp.getUserSessionsStream(realm, user).toList();
        logger.info("Found " + userSessions.size() + " Sessions of user " + user.getUsername() + " in realm " + realm.getName() + " to kill.");
        for (UserSessionModel userSession : userSessions) {
            usp.removeUserSession(realm, userSession);
        }
    }

    /**
     * Prüft, ob der angegebene User im Attribut attributeName einen der Werte aus attributeValues hat.
     *
     * @param user User zur Prüfung
     * @param attributeName attributName zur Prüfung
     * @return true wenn Wert vorhanden, false sonst
     */
    private boolean isUserWithAttribute(final UserModel user, final String attributeName,
            final Set<String> values) {
        if (user == null || user.getAttributeStream(attributeName) == null) {
            logger.warn("Could not find attribute " + attributeName + " on user  " + (user != null ? user.getUsername() : "UNKNOWN"));
            return false;
        }

        final List<String> userAttributeValues = user.getAttributeStream(attributeName).collect(Collectors.toList());
        logger.debug("Checking values " + String.join(",", values) + " against values " + String.join(",", userAttributeValues));
        if (!userAttributeValues.isEmpty()) {
            if (userAttributeValues.containsAll(values)) {
                logger.debug("Found all attribute values " + String.join(", ", userAttributeValues));
                return true;
            } else {
                logger.debug("Did not find all attribute values: " + String.join(", ", userAttributeValues) +
                        " in list of possible values: " + String.join(", ", values));
            }
        }
        return false;
    }

    /**
     * Erzeugt einen Error-String aus den angegebenen Parametern (für die Login-Seite).
     *
     * @param user the User
     * @param attributeName der attribute-name
     * @param deductedScopes die abgeleiteten Scopes
     * @return error String
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
     *
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
