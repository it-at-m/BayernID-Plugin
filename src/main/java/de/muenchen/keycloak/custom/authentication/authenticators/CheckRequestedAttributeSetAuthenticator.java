package de.muenchen.keycloak.custom.authentication.authenticators;

import de.muenchen.keycloak.custom.broker.saml.AuthNoteHelper;
import de.muenchen.keycloak.custom.config.domain.IDP;
import de.muenchen.keycloak.custom.config.spi.BayernIdConfigProvider;
import jakarta.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.*;

/**
 * Authenticator to check whether the requestedAttributeSet (only available on SAML requests) fits
 * to the current
 * account type (BayernID, BundID, ELSTER_NEZO) when switching clients through SSO.
 */
public class CheckRequestedAttributeSetAuthenticator implements Authenticator {

    protected static final Logger logger = Logger.getLogger(CheckRequestedAttributeSetAuthenticator.class);

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

        final String attributeName = resolveConfiguration(authenticatorConfig.getConfig(),
                RequireEffectiveScopesAuthenticatorFactory.ATTRIBUTE);
        final String customError = resolveConfiguration(authenticatorConfig.getConfig(),
                RequireEffectiveScopesAuthenticatorFactory.CUSTOM_ERROR);

        if (attributeName == null || attributeName.trim().equalsIgnoreCase("")) {
            logger.error("Wrong / missing konfiguration in check-requested-attribute-set! AttributeName " + attributeName);
            errorString = "Login nicht möglich wegen fehlerhafter Konfiguration.";
            //Fehlerhafte Konfiguration - keine weitere Prüfung
        } else {
            String requestedAttributeSet = AuthNoteHelper.getRequestedAttributeSet(context.getAuthenticationSession());
            logger.debug("Found requested attribute set: " + requestedAttributeSet);

            if (requestedAttributeSet == null || requestedAttributeSet.isEmpty()) {
                //wenn kein requestedAttributeSet vorliegend --> Trigger nicht anwendbar
                context.success();
                return;
            }

            BayernIdConfigProvider configProvider = context.getSession().getProvider(BayernIdConfigProvider.class);
            final String accountSource = user.getAttributeStream(attributeName).findFirst().isPresent()
                    ? user.getAttributeStream(attributeName).findFirst().get()
                    : null;
            logger.debug("Using accountSource: " + accountSource);
            final IDP idp = configProvider.findIDPByName(accountSource);
            logger.debug("requestedAttributeSets of provider is " + Arrays.toString(idp.getRequestedAttributeSets()));

            if (Arrays.asList(idp.getRequestedAttributeSets()).contains(requestedAttributeSet)) {
                //Erfolg
                context.success();
                return;
            }

            if (customError != null && !customError.trim().equalsIgnoreCase("")) {
                errorString = customError;
            } else {
                errorString = "Der angemeldete User hat nicht die richtige Kontoart.";
            }
        }

        logger.infof("Access denied because of not-matching requestedAttributeSet. realm=%s",
                realm.getName());

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
        final List<UserSessionModel> userSessions = usp.getUserSessionsStream(realm, user).toList();
        logger.info("Found " + userSessions.size() + " Sessions of user " + user.getUsername() + " in realm " + realm.getName() + " to kill.");
        for (UserSessionModel userSession : userSessions) {
            usp.removeUserSession(realm, userSession);
        }
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
