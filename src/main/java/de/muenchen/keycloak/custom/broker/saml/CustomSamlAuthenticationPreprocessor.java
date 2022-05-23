package de.muenchen.keycloak.custom.broker.saml;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.OAuth2Constants;
import org.keycloak.dom.saml.v2.protocol.*;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.saml.preprocessor.SamlAuthenticationPreprocessor;
import org.keycloak.saml.SamlProtocolExtensionsAwareBuilder;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.saml.processing.api.saml.v2.response.SAML2Response;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.util.*;
import java.util.logging.Level;

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
     * @param authnRequest
     * @param authSession
     * @return
     */
    @Override
    public AuthnRequestType beforeProcessingLoginRequest(AuthnRequestType authnRequest, AuthenticationSessionModel authSession) {
        logger.debug("beforeProcessingLoginRequest");
        extractFromSaml(authnRequest, authSession);
        return SamlAuthenticationPreprocessor.super.beforeProcessingLoginRequest(authnRequest, authSession); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public LogoutRequestType beforeProcessingLogoutRequest(LogoutRequestType logoutRequest, UserSessionModel authSession, AuthenticatedClientSessionModel clientSession) {
        logger.debug("beforeProcessingLogoutRequest");
        return SamlAuthenticationPreprocessor.super.beforeProcessingLogoutRequest(logoutRequest, authSession, clientSession); //To change body of generated methods, choose Tools | Templates.
    }

    // SCHRITT 2
    /**
     * Wird aufgerufen, wenn der Keycloak zum Login zum externen IdP (bei der
     * AKDB) weiterleitet. Wir setzen dann die Extensions gemäß dem, was wir aus
     * beforeProcessingLoginRequest erhalten haben.
     *
     * @param authnRequest
     * @param clientSession
     * @return
     */
    @Override
    public AuthnRequestType beforeSendingLoginRequest(AuthnRequestType authnRequest, AuthenticationSessionModel clientSession) {
        logger.debug("beforeSendingLoginRequest");
        addBuergerkontoAdditions(authnRequest, clientSession);
        return SamlAuthenticationPreprocessor.super.beforeSendingLoginRequest(authnRequest, clientSession); //To change body of generated methods, choose Tools | Templates.
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
     * @param statusResponse
     * @param authSession
     * @return
     */
    @Override
    public StatusResponseType beforeProcessingLoginResponse(StatusResponseType statusResponse, AuthenticationSessionModel authSession) {
        logger.debug("beforeProcessingLoginResponse");
        authenticationFinished(authSession, statusResponse);
        return SamlAuthenticationPreprocessor.super.beforeProcessingLoginResponse(statusResponse, authSession); //To change body of generated methods, choose Tools | Templates.
    }

    // SCHRITT 4
    /**
     * Wird aufgerufen, wenn der Keycloak letztlich wieder zur Anwendung
     * zurückleitet. In unserem Fall machen wir hier bislang nichts
     * spezifisches.
     *
     * @param statusResponse
     * @param clientSession
     * @return
     */
    @Override
    public StatusResponseType beforeSendingResponse(StatusResponseType statusResponse, AuthenticatedClientSessionModel clientSession) {
        logger.debug("beforeSendingResponse");
        return SamlAuthenticationPreprocessor.super.beforeSendingResponse(statusResponse, clientSession); //To change body of generated methods, choose Tools | Templates.
    }

    // ---
    // Ab hier kommen die Helfer-Methoden, die wir aus den verschiedenen Hooks heraus aufrufen.
    // ---
    // zu SCHRITT 1
    // ------------
    /**
     * Wird aus beforeProcessingLoginRequest (Schritt 1) heraus aufgerufen. Holt
     * die Extensions aus dem SAML Request und legt sie in eine AuthNote in der
     * AuthSession.
     *
     * @param authnRequest
     * @param authSession
     */
    private void extractFromSaml(AuthnRequestType authnRequest, AuthenticationSessionModel authSession) {
        ExtensionsType extensions = authnRequest.getExtensions();

        if (extensions != null) {
            List<Object> list = extensions.getAny();
            for (Object o : list) {
                Element el = (Element) o;
                String tagName = el.getLocalName();
                String nameAttribute = el.getAttribute("Name");
                String authNodeName = "SAML_EXTENSION";
                if (tagName != null && !tagName.equals("")) {
                    authNodeName += "_" + tagName.toUpperCase();
                    if (nameAttribute != null && !nameAttribute.equals("")) {
                        authNodeName += "_" + nameAttribute.toUpperCase();
                    }
                }
                authSession.setAuthNote(authNodeName, el.getTextContent());
            }

        }

        RequestedAuthnContextType requestedAuthnContext = authnRequest.getRequestedAuthnContext();

        if (requestedAuthnContext != null && requestedAuthnContext.getAuthnContextClassRef() != null) {
            for (String ref : requestedAuthnContext.getAuthnContextClassRef()) {
                authSession.setAuthNote("AUTH_CONTEXT", ref);
            }
        }
    }

    // zu SCHRITT 2
    // ------------
    /**
     * Wird aus beforeSendingLoginRequest (Schritt 2) heraus aufgerufen. Holt
     * die in Schritt 1 hinterlegten Extensions wieder aus der AuthNote und
     * setzt sie im SAML Request an den IdP. Außerdem wird der Request ins
     * Logfile ausgegeben.
     *
     * @param authnRequest
     * @param clientSession
     */
    private void addBuergerkontoAdditions(AuthnRequestType authnRequest, AuthenticationSessionModel clientSession) {

        //Requested Attribute Set
        //-----------------------
        String requestedAttributeSetElement = findRequestedAttributeSet(clientSession);

        if (requestedAttributeSetElement != null) {
            logger.info("Requested Attribute Set: " + requestedAttributeSetElement);
            addExtension(authnRequest, makeAccountTypeExtension(requestedAttributeSetElement));
        } else {
            logger.debug("No requested attribute set found.");
        }

        //Requested Auth Context
        //----------------------
        String storkLevel = findStorkLevel(clientSession);
        if (storkLevel != null && !storkLevel.equals("")) {
            logger.info("Requested authLevel: " + storkLevel);
            addRequestedAuthnContext(authnRequest, storkLevel);
        }

        //AllowedMethods / otherOptions
        boolean otherOptions = findOtherOptions(clientSession);
        if (!otherOptions) {
            //other Options nicht explizit angefordert
            String[] allowedMethods = {"eID", "Benutzername", "Authega"};
            List<String> allowedMethodsArray = Arrays.asList(allowedMethods);
            addExtension(authnRequest, makeAllowedMethodsExtension(allowedMethodsArray));
        }

        //Print Request
        //-------------
        if (logger.isDebugEnabled() || hasDebugScope(clientSession)) {
            printRequest(authnRequest);
        }
    }

    /**
     * Prüft, ob (im Fall von OIDC) einer der Scopes any oder legalEntity existiert und
     * gibt in dem Fall diesen String zurück. Wird dann im REquest als RequestedAttributeSet gesetzt.
     * @param clientSession
     * @return
     */
    private String findRequestedAttributeSet(AuthenticationSessionModel clientSession) {
        List<String> scopes = findScopeParams(clientSession);

        if (scopes != null) {
            //OIDC Call
            for (String scope : scopes) {
                if (scope.equalsIgnoreCase("any")) {
                    return "any";
                } else if (scope.equalsIgnoreCase("legalEntity")) {
                    return "legalEntity";
                } else {
                    //Buergerkonto ist standard, keine Extension benoetigt
                }
            }
        } else {
            //SAML Call
            return clientSession.getAuthNote("SAML_EXTENSION_REQUESTEDATTRIBUTESET");
        }

        return null;
    }

    /**
     * Prüft, ob
     * - bei OIDC in den Scopes der Scope "otherOptions"
     * - oder bei SAML im Request unter Extensions ein Element <OtherOptions>true</OtherOptions>
     * existiert. In dem Fall wird der true zurückgeliefert, sonst false.
     * @param clientSession die clientSession, über die man die Scopes bekommt
     * @return true oder false
     */
    private boolean findOtherOptions(AuthenticationSessionModel clientSession) {
        List<String> scopes = findScopeParams(clientSession);

        if (scopes != null) {
            //OIDC Call
            for (String scope : scopes) {
                logger.info("Found scope " + scope);
                if (scope.equalsIgnoreCase("otherOptions")) {
                    logger.info("Return true");
                    return true;
                }
            }
        } else {
            //SAML Call
            String otherOptions = clientSession.getAuthNote("SAML_EXTENSION_OTHEROPTIONS");
            return otherOptions != null && otherOptions.equals("true");
        }

        return false;
    }

    /**
     * Findet den entsprechenden Scope Parameter im Request (bei OIDC).
     *
     * @param clientSession
     * @return
     */
    private List<String> findScopeParams(AuthenticationSessionModel clientSession) {
        List<String> scopes = null;

        String scopeParam = clientSession.getClientNote(OAuth2Constants.SCOPE);
        if (scopeParam != null) {
            logger.info("Scope Parameter in Request " + scopeParam);
            scopes = Arrays.asList(scopeParam.split("\\s+"));
        }

        return scopes;
    }



    private void addExtension(AuthnRequestType authnRequest, SamlProtocolExtensionsAwareBuilder.NodeGenerator extension) {
        ExtensionsType et = authnRequest.getExtensions();
        if (et == null) {
            et = new ExtensionsType();
        }
        et.addExtension(extension);
        authnRequest.setExtensions(et);
    }

    private void printRequest(AuthnRequestType authnRequest) {
        try {
            Document docAfter = SAML2Request.convert(authnRequest);
            logger.info("Request: \n ------ \n" + printDocument(docAfter));
        } catch (ProcessingException | ConfigurationException | ParsingException | TransformerException ex) {
            java.util.logging.Logger.getLogger(CustomSamlAuthenticationPreprocessor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void addRequestedAuthnContext(AuthnRequestType authnRequest, String level) {
        RequestedAuthnContextType type = new RequestedAuthnContextType();
        type.addAuthnContextClassRef(level);
        type.setComparison(AuthnContextComparisonType.MINIMUM);
        authnRequest.setRequestedAuthnContext(type);
    }

    private String findStorkLevel(AuthenticationSessionModel clientSession) {

        if (clientSession != null) {
            List<String> scopes = findScopeParams(clientSession);

            if (scopes != null) {
                //OIDC Call
                for (String scopeParam : scopes) {
                    if (scopeParam.equalsIgnoreCase("level1")) {
                        return "STORK-QAA-Level-1";
                    } else if (scopeParam.equalsIgnoreCase("level2")) {
                        return "STORK-QAA-Level-2";
                    } else if (scopeParam.equalsIgnoreCase("level3")) {
                        return "STORK-QAA-Level-3";
                    } else if (scopeParam.equalsIgnoreCase("level4")) {
                        return "STORK-QAA-Level-4";
                    }
                }
            } else {
                //SAML Call
                return clientSession.getAuthNote("AUTH_CONTEXT");
            }
        }

        return "";
    }

    private boolean hasDebugScope(AuthenticationSessionModel clientSession) {
        if (clientSession != null) {
            List<String> scopes = findScopeParams(clientSession);
            return scopes != null && scopes.contains("debug");
        }

        return false;
    }

    private SamlProtocolExtensionsAwareBuilder.NodeGenerator makeAccountTypeExtension(String content) {
        Map<String, String> attributes = new LinkedHashMap<>();

        AccounttypeExtensionGenerator accounttypeExtension = new AccounttypeExtensionGenerator(attributes, content);
        logger.debugf("  ---> %s", accounttypeExtension.toString());
        return accounttypeExtension;
    }

    private SamlProtocolExtensionsAwareBuilder.NodeGenerator makeAllowedMethodsExtension(List<String> methods) {
        Map<String, String> attributes = new LinkedHashMap<>();
//        attributes.put("Version", "1");

        AllowedMethodsExtensionGenerator allowedMethodsExtension = new AllowedMethodsExtensionGenerator(attributes, methods);
        logger.debugf("  ---> %s", allowedMethodsExtension.toString());
        return allowedMethodsExtension;
    }



    // zu SCHRITT 3
    // ------------
    /**
     * Wird aus beforeProcessingLoginResponse (Schritt 3) heraus aufgerufen.
     * Prüft, ob die im Request eingetragenen Extension-Attribute nun im
     * Response auch wieder enthalten sind. Außerdem wird der Response im Log
     * ausgegeben.
     *
     * @param authSession
     * @param responseType
     */
    public void authenticationFinished(AuthenticationSessionModel authSession, StatusResponseType responseType) {
        //Print Response
        //--------------
        if (logger.isDebugEnabled() || hasDebugScope(authSession)) {
            printResponse(responseType);
        }

        //Check InResponseTo
        //------------------
        String id = authSession.getClientNote("Generated_ID");

        if (id != null && !id.equals(responseType.getInResponseTo())) {
            logger.error("Found violation in ID");
        }

    }

    private void printResponse(StatusResponseType responseType) {
        try {
            SAML2Response saml2Response = new SAML2Response();
            Document doc = saml2Response.convert(responseType);
            logger.info("\n\n\n  Response: \n ------- \n " + printDocument(doc));

        } catch (ConfigurationException | ProcessingException | TransformerException | ParsingException ex) {
            java.util.logging.Logger.getLogger(CustomSamlAuthenticationPreprocessor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public String printDocument(Document doc) throws TransformerException {
        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);

        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

        transformer.transform(new DOMSource(doc), result);

        return writer.toString();
    }


}
