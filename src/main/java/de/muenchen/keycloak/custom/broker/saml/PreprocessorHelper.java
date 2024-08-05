package de.muenchen.keycloak.custom.broker.saml;

import de.muenchen.keycloak.custom.ScopesHelper;
import de.muenchen.keycloak.custom.broker.saml.domain.RequestedAttribute;
import de.muenchen.keycloak.custom.broker.saml.mappers.CustomUserAttributeMapper;
import java.io.StringWriter;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.jboss.logging.Logger;
import org.keycloak.OAuth2Constants;
import org.keycloak.broker.saml.mappers.UserAttributeMapper;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.w3c.dom.Document;

public class PreprocessorHelper {

    protected static final Logger logger = Logger.getLogger(PreprocessorHelper.class);

    public static Set<String> enhanceEffectiveScopes(Set<String> effectiveScopes, AuthenticationSessionModel clientSession) {
        Set<String> deductedScopes = PreprocessorHelper.deductScopesFromRequestedAttributes(clientSession);

        if (deductedScopes != null && !deductedScopes.isEmpty()) {
            deductedScopes.addAll(effectiveScopes);
            return deductedScopes;
        } else {
            return effectiveScopes;
        }
    }

    /**
     * Stellt die "effective Scopes" zusammen. Und zwar wie folgt:
     * - bei OIDC: alle default client scopes sowie alle optional client scopes, die explizit im Request
     * angefordert wurden.
     * - bei SAML2: alle im SAML-Request unter Extensions explizit angeforderten scopes ODER (falls
     * keine scopes explizit
     * angefordert wurden) alle default client scopes.
     *
     * Hinweis: Bei den Client Scopes wird ggf. zunächst das Suffix "_oidc" bzw "_saml" entfernt, falls
     * vorhanden.
     *
     * @param clientSession AuthenticationSessionModel
     * @return Effective Scopes als Set<String>
     */
    public static Set<String> collectEffectiveScopes(AuthenticationSessionModel clientSession) {
        final ClientModel currentClient = clientSession.getClient();

        final Set<String> effectiveScopes = new HashSet<>();

        if (currentClient == null) {
            return effectiveScopes;
        }

        //Version für RH-SSO 7.5 / Keycloak 15.0.2
        final Map<String, ClientScopeModel> defaultClientScopes = currentClient.getClientScopes(true);

        if (PreprocessorHelper.isOidcRequest(clientSession)) {
            //bei OIDC: alle default client scopes sind "effective" (aber Suffix _oidc vorher entfernen)
            effectiveScopes.addAll(
                    ScopesHelper.stripScopes(defaultClientScopes.keySet()));

            //bei OIDC: alle optional scopes, die explizit im Request angefordert wurden, sind "effective"
            final Map<String, ClientScopeModel> optionalClientScopes = currentClient.getClientScopes(false);
            List<String> requestedScopes = findScopeParams(clientSession);

            if (requestedScopes != null) {
                effectiveScopes.addAll(
                        requestedScopes.stream().filter(optionalClientScopes::containsKey).collect(Collectors.toList()));
            }
        } else if (PreprocessorHelper.isSamlRequest(clientSession)) {
            //bei SAML: alle im SAML Request angeforderten Scopes sind "effective"
            List<String> requestedScopes = AuthNoteHelper.getRequestedScopesAsList(clientSession);
            if (requestedScopes != null) {
                effectiveScopes.addAll(requestedScopes);
            }

            //und alle default Scopes sind "effective"
            effectiveScopes.addAll(
                    ScopesHelper.stripScopes(defaultClientScopes.keySet()));
        }

        return effectiveScopes;
    }

    /**
     * Falls im SAML-Request explizit Attribute angefordert wurden (Funktionalität ist aufgrund von
     * Kompatibilität
     * zur Original-AKDB-Schnittstelle vorhanden), werden die dabei komplett abgedeckten Scopes
     * ermittelt.
     *
     * @param clientSession AuthenticationSessionModel
     * @return vollständig durch im SAML-Request angeforderte Attribute abgedeckte Scopes
     */
    public static Set<String> deductScopesFromRequestedAttributes(AuthenticationSessionModel clientSession) {
        Map<String, RequestedAttribute> requestedAttributes = deriveRequestedAttributesFromSamlRequest(clientSession);

        if (requestedAttributes == null || requestedAttributes.isEmpty()) {
            return null;
        }

        Map<String, Boolean> scopes = new HashMap<>();

        //alle Mapper iterieren
        clientSession.getRealm().getIdentityProviderMappersStream().forEach(identityProviderMapperModel -> {
            String scope = identityProviderMapperModel.getConfig().get(CustomUserAttributeMapper.FIELD_SCOPE);
            if (scope != null && !scope.isEmpty()) {
                //scope Attribut gesetzt

                //attributeName aus Mapper holen
                String attributeName = identityProviderMapperModel.getConfig().get(UserAttributeMapper.ATTRIBUTE_NAME);
                if (attributeName != null) { //attributeName gesetzt
                    //prüfen: wurde attributeName in den requestedAttributes mit angefordert?
                    if (requestedAttributes.containsKey(attributeName)) {
                        //attributeName wurde im SAML Request angefordert
                        if (!scopes.containsKey(scope)) {
                            //noch nicht vorhanden --> auf TRUE setzen
                            scopes.put(scope, Boolean.TRUE);
                        }
                    } else {
                        //attributeName wurde im SAML Request NICHT angefordert
                        //--> auf FALSE setzen, selbst wenn es bisher auf TRUE stand
                        scopes.put(scope, Boolean.FALSE);
                    }

                }
            }
        });

        //alle Scopes sammeln, die auf TRUE stehen und zurückliefern
        Set<String> scopesSet = new HashSet<>();
        scopes.forEach((s, b) -> {
            if (b == Boolean.TRUE) {
                scopesSet.add(s);
            }
        });
        return scopesSet;
    }

    /**
     * Liefert die im SAML-Request angeforderten Attribute in Form einer Map (zur späteren schnelleren
     * Auffindbarkeit
     * mit dem Attribut-Namen als Key.
     *
     * @param clientSession AuthenticationSessionModel
     * @return die im SAML-Request angeforderten Attribute in Form einer Map
     */
    public static Map<String, RequestedAttribute> deriveRequestedAttributesFromSamlRequest(AuthenticationSessionModel clientSession) {
        String requestedAttributesString = AuthNoteHelper.getRequestedAttributes(clientSession);

        if (requestedAttributesString != null) {
            Map<String, RequestedAttribute> requestedAttributes = new HashMap<>();

            Arrays.stream(requestedAttributesString.split("##")).forEach(s -> {
                String[] requestedAttributeString = s.split("\\|");
                if (requestedAttributeString.length > 0) {
                    RequestedAttribute ra = new RequestedAttribute();
                    ra.setName(requestedAttributeString[0]);
                    ra.setRequiredAttribute(requestedAttributeString.length > 1 ? requestedAttributeString[1] : null);
                    requestedAttributes.put(ra.getName(), ra);
                }
            });

            return requestedAttributes;
        }

        return null;
    }

    /**
     * Findet den entsprechenden Scope Parameter im Request (bei OIDC).
     *
     * @param clientSession AuthenticationSessionModel
     * @return List<String> mit Scopes aus OIDC-Request oder null
     */
    public static List<String> findScopeParams(AuthenticationSessionModel clientSession) {
        List<String> scopes = null;

        String scopeParam = clientSession.getClientNote(OAuth2Constants.SCOPE);
        if (scopeParam != null) {
            logger.debug("Scope Parameter in Request " + scopeParam);
            scopes = Arrays.asList(scopeParam.split("\\s+"));
        }

        return scopes; //Wichtig: scopes muss null nein, wenn keine ClientNote --> bezeichnet den SAML-Call
    }

    public static boolean hasDebugScope(AuthenticationSessionModel clientSession) {
        if (clientSession != null) {
            List<String> scopes = PreprocessorHelper.findScopeParams(clientSession);
            if (scopes != null && isOidcRequest(clientSession)) {
                //OIDC Call
                return scopes.contains("debug");
            } else {
                //SAML Call
                String requestedScopes = AuthNoteHelper.getRequestedScopes(clientSession);
                return requestedScopes != null && requestedScopes.contains("debug");
            }
        }

        return false;
    }

    public static boolean isOidcRequest(AuthenticationSessionModel clientSession) {
        String protocol = clientSession.getProtocol();
        if (protocol == null) {
            logger.warn("No protocol found - cannot determine whether OIDC or SAML2 Call!");
        } else return protocol.equals("openid-connect");
        return false;
    }

    public static boolean isSamlRequest(AuthenticationSessionModel clientSession) {
        String protocol = clientSession.getProtocol();
        if (protocol == null) {
            logger.warn("No protocol found - cannot determine whether OIDC or SAML2 Call!");
        } else return protocol.equals("saml");
        return false;
    }

    public static boolean isPublicRealm(String realm) {
        return realm != null && realm.equalsIgnoreCase("public") || realm.equalsIgnoreCase("demo") || realm.equalsIgnoreCase("A61");
    }

    public static void printRequest(AuthnRequestType authnRequest) {
        try {
            Document docAfter = SAML2Request.convert(authnRequest);
            logger.info("Request: \n ------ \n" + printDocument(docAfter));
        } catch (ProcessingException | ConfigurationException | ParsingException | TransformerException ex) {
            java.util.logging.Logger.getLogger(CustomSamlAuthenticationPreprocessor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static String printDocument(Document doc) throws TransformerException {
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
