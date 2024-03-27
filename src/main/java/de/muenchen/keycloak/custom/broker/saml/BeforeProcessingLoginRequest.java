package de.muenchen.keycloak.custom.broker.saml;

import org.jboss.logging.Logger;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.dom.saml.v2.protocol.ExtensionsType;
import org.keycloak.dom.saml.v2.protocol.RequestedAuthnContextType;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.*;
import java.util.stream.Collectors;


/**
 * Verarbeitet im Fall von per SAML angebundenen Clients den SAML-Request und legt verschiedene Inhalte (v.a. aus dem Extensions-Bereich)
 * in AuthNotes.
 */
public class BeforeProcessingLoginRequest {

    protected static final Logger logger = Logger.getLogger(BeforeProcessingLoginRequest.class);

    public static final String REQUESTED_ATTRIBUTE_SET = "RequestedAttributeSet";
    public static final String AUTHENTICATION_REQUEST = "AuthenticationRequest";
    public static final String REQUESTED_SCOPES = "RequestedScopes";
    private static final String OTHER_OPTIONS = "otherOptions";
    private static final String IDP_HINT = "idpHint";

    // zu SCHRITT 1
    // ------------
    public void process(AuthnRequestType authnRequest, AuthenticationSessionModel authSession) {
        extractFromSaml(authnRequest, authSession);
    }

    /**
     * Holt die Extensions aus dem SAML Request und legt sie in AuthNotes in der
     * AuthSession.
     *
     * @param authnRequest AuthnRequestType
     * @param authSession AuthenticationSessionModel
     */
    public void extractFromSaml(AuthnRequestType authnRequest, AuthenticationSessionModel authSession) {
        ExtensionsType extensions = authnRequest.getExtensions();

        if (extensions != null) {
            logger.debug("Found Extensions. Iterating.");
            List<Object> list = extensions.getAny();
            for (Object o : list) {
                Element el = (Element) o;
                String tagName = el.getLocalName();
                logger.debug("Checking Extension with tagName " + tagName);
                if (tagName != null && !tagName.isEmpty()) {

                    if (tagName.equalsIgnoreCase(REQUESTED_ATTRIBUTE_SET)) {
                        AuthNoteHelper.setRequestedAttributeSet(el.getTextContent(), authSession);
                    }

                    else if (tagName.equalsIgnoreCase(AUTHENTICATION_REQUEST)) {
                        parseAuthenticationRequest(el, authSession);
                    }

                    else if (tagName.equalsIgnoreCase(REQUESTED_SCOPES)) {
                        parseRequestedScopes(el, authSession);
                    }

                    else if (tagName.equalsIgnoreCase(OTHER_OPTIONS)) {
                        AuthNoteHelper.setOtherOptions(el.getTextContent(), authSession);
                    }

                    else if (tagName.equalsIgnoreCase(IDP_HINT)) {
                        AuthNoteHelper.setIdpHint(el.getTextContent(), authSession);
                    }

                    else {
                        //Fallback für unbekannte Elemente
                        AuthNoteHelper.setAuthNote(el, el.getTextContent(), authSession);
                    }
                }
            }

        }

        RequestedAuthnContextType requestedAuthnContext = authnRequest.getRequestedAuthnContext();

        if (requestedAuthnContext != null && requestedAuthnContext.getAuthnContextClassRef() != null) {
            for (String ref : requestedAuthnContext.getAuthnContextClassRef()) {
                AuthNoteHelper.setAuthContext(ref, authSession);
            }
        }
    }

    private void parseAuthenticationRequest(Element el, AuthenticationSessionModel authSession) {
        for (Element child : getChildElementsAsList(el)) {
            String tagName = child.getLocalName();
            logger.debug("Checking child tagName " + tagName);
            if (tagName == null) {
                return;
            }

            // AllowedMethods
            if (tagName.equalsIgnoreCase("AllowedMethods")) { //V1
                logger.debug("Found child tagName AllowedMethods.");
                String allowedMethods = getChildElementsAsList(child).stream()
                        .map(Node::getTextContent).collect(Collectors.joining("##"));
                AuthNoteHelper.setAllowedMethods(allowedMethods, authSession);

                // AuthnMethods
            } else if (tagName.equalsIgnoreCase("AuthnMethods")) { //V2
                logger.debug("Found child tagName AuthnMethods.");
                List<String> authnMethodsList = new ArrayList<>();
                for (Element authnMethod : getChildElementsAsList(child)) {
                    if (getChildElementsAsMap(authnMethod).get("Enabled") != null &&
                            "true".equalsIgnoreCase(getChildElementsAsMap(authnMethod).get("Enabled").getTextContent())) {
                        authnMethodsList.add(authnMethod.getLocalName());
                    }
                }
                String authMethods = String.join("##", authnMethodsList);
                AuthNoteHelper.setAuthMethods(authMethods, authSession);

            }
            // RequestedAttributes
            else if (tagName.equalsIgnoreCase("RequestedAttributes")) { //V1 und V2
                logger.debug("Found child tagName RequestedAttributes.");
                String requestedAttributes = getChildElementsAsList(child).stream()
                        .map(c->c.getAttribute("Name") + "|" + c.getAttribute("RequiredAttribute"))
                        .collect(Collectors.joining("##"));
                AuthNoteHelper.setRequestedAttributes(requestedAttributes, authSession);
            }
        }
    }



    private void parseRequestedScopes(Element el, AuthenticationSessionModel authSession) {
        String requestedScopes = getChildElementsAsList(el).stream()
                .filter(c -> c.getLocalName().equalsIgnoreCase("Scope"))
                .map(Node::getTextContent)
                .collect(Collectors.joining("##"));
        AuthNoteHelper.setRequestedScopes(requestedScopes, authSession);
    }


    private List<Element> getChildElementsAsList(Element element) {
        List<Element> list = new ArrayList<>();
        for (int i = 0 ; i < element.getChildNodes().getLength(); i++) {
            Object o = element.getChildNodes().item(i);
            if (o instanceof Element) {
                list.add((Element) o);
            } else {
                logger.debug("Found child of type " + o.getClass() + " which is not Element.");
            }
        }
        return list;
    }

    private Map<String, Element> getChildElementsAsMap(Element element) {
        Map<String, Element> map = new HashMap<>();
        for (int i = 0 ; i < element.getChildNodes().getLength(); i++) {
            Object o = element.getChildNodes().item(i);
            if (o instanceof Element) {
                map.put(((Element) o).getLocalName(), (Element) o);
            } else {
                logger.debug("Found child of type " + o.getClass() + " which is not Element.");
            }
        }
        return map;
    }


}
