package de.muenchen.keycloak.custom.broker.saml;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.dom.saml.v2.protocol.*;
import org.keycloak.models.*;
import org.keycloak.protocol.saml.preprocessor.SamlAuthenticationPreprocessor;
import org.keycloak.sessions.AuthenticationSessionModel;

/**
 * Derived from SAMLIdentityProvider.
 *
 * @author rowe42
 */
public class CustomSamlAuthenticationPreprocessor implements SamlAuthenticationPreprocessor {

    protected static final Logger logger = Logger.getLogger(CustomSamlAuthenticationPreprocessor.class);

    public static final String PROVIDER_ID = "custompreprocessor";

    protected KeycloakSession session;

    @Override
    public void close() {
    }

    @Override
    public SamlAuthenticationPreprocessor create(KeycloakSession session) {
        this.session = session;
        return this;
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    // ---
    // Ab hier kommen die verschiedenen Hooks
    // ---
    // SCHRITT 1
    /**
     * Wird aufgerufen, wenn die externe Anwendung per SAML zum Keycloak zum
     * Login weiterleitet. Wir entnehmen dann die Extensions und packen sie in
     * eine AuthNote in die AuthSession.
     *
     * @param authnRequest AuthnRequestType
     * @param authSession AuthenticationSessionModel
     * @return AuthnRequestType
     */
    @Override
    public AuthnRequestType beforeProcessingLoginRequest(AuthnRequestType authnRequest, AuthenticationSessionModel authSession) {
        if (PreprocessorHelper.isPublicRealm(authSession.getRealm().getName())) {
            logger.debug("beforeProcessingLoginRequest");
            BeforeProcessingLoginRequest beforeProcessingLoginRequest = new BeforeProcessingLoginRequest();
            beforeProcessingLoginRequest.process(authnRequest, authSession);
        }
        return SamlAuthenticationPreprocessor.super.beforeProcessingLoginRequest(authnRequest, authSession);
    }

    @Override
    public LogoutRequestType beforeProcessingLogoutRequest(LogoutRequestType logoutRequest, UserSessionModel authSession,
            AuthenticatedClientSessionModel clientSession) {
        logger.debug("beforeProcessingLogoutRequest");
        return SamlAuthenticationPreprocessor.super.beforeProcessingLogoutRequest(logoutRequest, authSession, clientSession);
    }

    // SCHRITT 2
    /**
     * Wird aufgerufen, wenn der Keycloak zum Login zum externen IdP (bei der
     * AKDB) weiterleitet. Wir setzen dann die Extensions gemäß dem, was wir aus
     * beforeProcessingLoginRequest erhalten haben.
     *
     * @param authnRequest AuthnRequestType
     * @param clientSession AuthenticationSessionModel
     * @return AuthnRequestType
     */
    @Override
    public AuthnRequestType beforeSendingLoginRequest(AuthnRequestType authnRequest, AuthenticationSessionModel clientSession) {
        if (PreprocessorHelper.isPublicRealm(clientSession.getRealm().getName())) {
            logger.debug("beforeSendingLoginRequest");
            BeforeSendingLoginRequest beforeSendingLoginRequest = new BeforeSendingLoginRequest();
            beforeSendingLoginRequest.process(authnRequest, clientSession);
        }
        return SamlAuthenticationPreprocessor.super.beforeSendingLoginRequest(authnRequest, clientSession);
    }

    @Override
    public LogoutRequestType beforeSendingLogoutRequest(LogoutRequestType logoutRequest,
            UserSessionModel authSession, AuthenticatedClientSessionModel clientSession) {
        logger.debug("beforeSendingLogoutRequest");
        return logoutRequest;
    }

    // SCHRITT 3
    /**
     * Wird aufgerufen, wenn der externe IdP (bei der AKDB) zum Keycloak
     * zurückleitet. Wir prüfen, ob die im Request geschickten Attribute wie
     * z.B. SessionToken_OriginSP im Reponse enthalten sind.
     *
     * @param statusResponse StatusResponseType
     * @param authSession AuthenticationSessionModel
     * @return StatusResponseType
     */
    @Override
    public StatusResponseType beforeProcessingLoginResponse(StatusResponseType statusResponse, AuthenticationSessionModel authSession) {
        if (PreprocessorHelper.isPublicRealm(authSession.getRealm().getName())) {
            logger.debug("beforeProcessingLoginResponse");
            BeforeProcessingLoginResponse beforeProcessingLoginResponse = new BeforeProcessingLoginResponse();
            beforeProcessingLoginResponse.authenticationFinished(authSession, statusResponse);
        }
        return SamlAuthenticationPreprocessor.super.beforeProcessingLoginResponse(statusResponse, authSession);
    }

    // SCHRITT 4
    /**
     * Wird aufgerufen, wenn der Keycloak letztlich wieder zur Anwendung
     * zurückleitet. Hier setzen wir das authlevel noch zusätzlich in der EIDAS-Form in den
     * AuthnContext,
     * damit wir daraus im Formularserver zugreifen können.
     *
     * @param statusResponse StatusResponseType
     * @param clientSession AuthenticatedClientSessionModel
     * @return StatusResponseType
     */
    @Override
    public StatusResponseType beforeSendingResponse(StatusResponseType statusResponse, AuthenticatedClientSessionModel clientSession) {
        if (PreprocessorHelper.isPublicRealm(clientSession.getRealm().getName())) {
            logger.debug("beforeSendingResponse");
            BeforeSendingResponse beforeSendingResponse = new BeforeSendingResponse();
            beforeSendingResponse.process(statusResponse);
        }
        return SamlAuthenticationPreprocessor.super.beforeSendingResponse(statusResponse, clientSession);
    }

}
