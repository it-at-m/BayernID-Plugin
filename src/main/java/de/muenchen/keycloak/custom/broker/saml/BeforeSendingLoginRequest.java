package de.muenchen.keycloak.custom.broker.saml;

import de.muenchen.keycloak.custom.IdentityProviderHelper;
import de.muenchen.keycloak.custom.broker.saml.domain.RequestedAttribute;
import de.muenchen.keycloak.custom.broker.saml.mappers.CustomUserAttributeMapper;
import org.jboss.logging.Logger;
import org.keycloak.dom.saml.v2.protocol.AuthnContextComparisonType;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.dom.saml.v2.protocol.ExtensionsType;
import org.keycloak.dom.saml.v2.protocol.RequestedAuthnContextType;
import org.keycloak.saml.SamlProtocolExtensionsAwareBuilder;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.util.*;

/**
 * Reichert den SAML-Request, der an die BayernID geschickt wird, um verschiedene Elemente v.a. im Extensions-Bereich an.
 * Diese kommen zum Teil aus dem SAML-Request, der von der Client-Applikation geschickt wurde, zum Teil aus der Keycloak-Konfiguration.
 */
public class BeforeSendingLoginRequest {

    protected static final Logger logger = Logger.getLogger(BeforeSendingLoginRequest.class);

    public static final String STORK_QAA_LEVEL_1 = "STORK-QAA-Level-1";
    public static final String STORK_QAA_LEVEL_2 = "STORK-QAA-Level-2";
    public static final String STORK_QAA_LEVEL_3 = "STORK-QAA-Level-3";
    public static final String STORK_QAA_LEVEL_4 = "STORK-QAA-Level-4";
    public static final String LEVEL_1 = "level1";
    public static final String LEVEL_2 = "level2";
    public static final String LEVEL_3 = "level3";
    public static final String LEVEL_4 = "level4";

    // zu SCHRITT 2
    // ------------

    public void process(AuthnRequestType authnRequest, AuthenticationSessionModel clientSession) {
        //Effective Scopes sammeln und als AuthNote ablegen
        Set<String> effectiveScopes = PreprocessorHelper.collectEffectiveScopes(clientSession);
        logger.debug("EffectiveScopes: " + String.join(", ", effectiveScopes));
        AuthNoteHelper.setEffectiveScopes(effectiveScopes, clientSession);

        //Effective Scopes um die Scopes anreichern (enhance), die aus den explizit angeforderten Attributen abgeleitet (deducted) werden
        Set<String> deductedScopes = PreprocessorHelper.enhanceEffectiveScopes(effectiveScopes, clientSession);
        logger.debug("deductedScopes: " + String.join(", ", deductedScopes));
        AuthNoteHelper.setEnhancedEffectiveScopes(deductedScopes, clientSession);

        //BayernID-Erweiterungen im SAML-Request ergänzen
        addBayernIDAdditions(authnRequest, clientSession);
    }

    /**
     * Holt die in Schritt 1 hinterlegten Extensions wieder aus der AuthNote und
     * setzt sie im SAML Request an den IdP. Außerdem wird der Request ins
     * Logfile ausgegeben.
     *
     * @param authnRequest AuthnRequestType
     * @param clientSession AuthenticationSessionModel
     */
    public void addBayernIDAdditions(AuthnRequestType authnRequest, AuthenticationSessionModel clientSession) {

        //Requested Attribute Set
        //-----------------------
        makeRequestedAttributeSet(authnRequest, clientSession);

        //Requested Auth Context
        //----------------------
        makeRequestedAuthContext(authnRequest, clientSession);

        //AuthenticationRequest: AuthnMethods / otherOptions und RequestedAttributes
        //--------------------------------------------------------------------------
       makeAuthenticationRequest(authnRequest, clientSession);

        //Print Request
        //-------------
        if (logger.isDebugEnabled() || PreprocessorHelper.hasDebugScope(clientSession)) {
            PreprocessorHelper.printRequest(authnRequest);
        }
    }

    private void makeRequestedAttributeSet(AuthnRequestType authnRequest, AuthenticationSessionModel clientSession) {
        String requestedAttributeSetElement = IdentityProviderHelper.findRequestedAttributeSet(clientSession);

        if (requestedAttributeSetElement != null) {
            logger.debug("Setting requested Attribute Set: " + requestedAttributeSetElement);
            addExtension(authnRequest, makeAccountTypeExtension(requestedAttributeSetElement));
        } else {
            logger.debug("No requested attribute set found.");
        }
    }

    private void makeRequestedAuthContext(AuthnRequestType authnRequest, AuthenticationSessionModel clientSession) {
        String storkLevel = findStorkLevel(clientSession);
        if (storkLevel != null && !storkLevel.isEmpty()) {
            logger.debug("Setting requested authLevel: " + storkLevel);
            addRequestedAuthnContext(authnRequest, storkLevel);
        }
    }

    private void makeAuthenticationRequest(AuthnRequestType authnRequest, AuthenticationSessionModel clientSession) {
        Set<String> authnMethods = null;

        //bei SAML können die AuthMethods explizit im Extensions-Bereich angegeben werden
        if (PreprocessorHelper.isSAMLRequest(clientSession)) {
            authnMethods = AuthNoteHelper.getAuthMethodsAsSet(clientSession);
            if (authnMethods == null) {
                //V1 der Schnittstelle hat die authnMethods noch in AllowedMethods stehen
                authnMethods = AuthNoteHelper.getAllowedMethodsAsSet(clientSession);
            }
        }

        //falls nicht explizit gefunden, kann otherOptions sowohl bei SAML als auch bei OIDC gesetzt sein
        boolean otherOptions = findOtherOptions(clientSession);
        if (authnMethods == null && !otherOptions) {
            //keine explizite Anforderung und auch otherOptions nicht angefordert
            authnMethods = new HashSet<>(Arrays.asList("eID", "Benutzername", "Authega", "Elster"));
        }

        Set<RequestedAttribute> requestedAttributes = retrieveRequestedAttributes(clientSession);
        addExtension(authnRequest, makeAuthenticationRequestExtension(authnMethods, requestedAttributes));
    }


    /**
     * Holt die Requested Attributes für den SAML-Request an die BayernID aus den angeforderten Scopes
     * und / oder aus den Requested Attributes aus dem SAML-Request aus dem Client-Aufruf.
     *
     * Die Attributes aus dem SAML-Request haben dabei Vorrang vor den Attributes aus den Scopes.
     *
     * @param clientSession AuthenticationSessionModel
     * @return Die Requested Attributes für den SAML-Request an die BayernID.
     */
    public Set<RequestedAttribute> retrieveRequestedAttributes(AuthenticationSessionModel clientSession) {
        Map<String, RequestedAttribute> requestedAttributesFromScopes = deductAttributesFromEffectiveScopes(clientSession);
        Map<String, RequestedAttribute> requestedAttributesFromSAMLRequest = PreprocessorHelper.deriveRequestedAttributesFromSAMLRequest(clientSession);

        Set<RequestedAttribute> requestedAttributes = new HashSet<>();
        if (requestedAttributesFromSAMLRequest != null && !requestedAttributesFromSAMLRequest.isEmpty()) {
            //start mit allen angeforderten Attributes aus dem SAML Request
            requestedAttributes.addAll(requestedAttributesFromSAMLRequest.values());

            //alle requestedAttributesFromScopes ergänzen, die noch nicht aus dem SAML Request gekommen sind
            for (RequestedAttribute requestedAttributeFromScopes : requestedAttributesFromScopes.values()) {
                if (!requestedAttributesFromSAMLRequest.containsKey(requestedAttributeFromScopes.getName())) {
                    requestedAttributes.add(requestedAttributeFromScopes);
                }
            }
        } else {
            //falls keine Attribute im SAML Request angefordert sind, dann einfach die Attribute aus den Scopes
            requestedAttributes.addAll(requestedAttributesFromScopes.values());
        }

        return requestedAttributes;
    }

    public Map<String, RequestedAttribute> deductAttributesFromEffectiveScopes(AuthenticationSessionModel clientSession) {
        Map<String, RequestedAttribute> requestedAttributes = new HashMap<>();
        Set<String> effectiveScopes = AuthNoteHelper.getEffectiveScopesAsSet(clientSession);
        if (effectiveScopes == null) {
            return requestedAttributes;
        }

        clientSession.getRealm().getIdentityProviderMappersStream().forEach(identityProviderMapperModel -> {
            String scope = identityProviderMapperModel.getConfig().get(CustomUserAttributeMapper.FIELD_SCOPE);
            if (scope != null) {
                //scope attribute gefunden
                if (effectiveScopes.contains(scope)) {
                    //scope dieses Mappers ist in den effective scopes enthalten --> bei BayernID anfordern
                    RequestedAttribute requestedAttribute = new RequestedAttribute();
                    requestedAttribute.setName(identityProviderMapperModel.getConfig().get(CustomUserAttributeMapper.ATTRIBUTE_NAME));
                    requestedAttribute.setRequiredAttribute(identityProviderMapperModel.getConfig().get(CustomUserAttributeMapper.REQUIRED_ATTRIBUTE));
                    requestedAttributes.put(requestedAttribute.getName(), requestedAttribute);
                }
            }
        });
        return requestedAttributes;
    }





    private void addExtension(AuthnRequestType authnRequest, SamlProtocolExtensionsAwareBuilder.NodeGenerator extension) {
        ExtensionsType et = authnRequest.getExtensions();
        if (et == null) {
            et = new ExtensionsType();
        }
        et.addExtension(extension);
        authnRequest.setExtensions(et);
    }

    private SamlProtocolExtensionsAwareBuilder.NodeGenerator makeAccountTypeExtension(String content) {
        Map<String, String> attributes = new LinkedHashMap<>();

        AccounttypeExtensionGenerator accounttypeExtension = new AccounttypeExtensionGenerator(attributes, content);
        logger.debugf("  ---> %s", accounttypeExtension.toString());
        return accounttypeExtension;
    }


    private SamlProtocolExtensionsAwareBuilder.NodeGenerator makeAuthenticationRequestExtension(Set<String> methods,
                                                                                                Set <RequestedAttribute> requestedAttributes) {
        AuthenticationRequestExtensionGenerator allowedMethodsExtension = new AuthenticationRequestExtensionGenerator(methods,
                requestedAttributes);
        logger.debugf("  ---> %s", allowedMethodsExtension.toString());
        return allowedMethodsExtension;
    }



    private String findStorkLevel(AuthenticationSessionModel clientSession) {

        if (clientSession != null) {
            List<String> scopes = PreprocessorHelper.findScopeParams(clientSession);

            if (scopes != null && PreprocessorHelper.isOIDCRequest(clientSession)) {
                //OIDC Call
                for (String scopeParam : scopes) {
                    if (scopeParam.equalsIgnoreCase(LEVEL_1)) {
                        return STORK_QAA_LEVEL_1;
                    } else if (scopeParam.equalsIgnoreCase(LEVEL_2)) {
                        return STORK_QAA_LEVEL_2;
                    } else if (scopeParam.equalsIgnoreCase(LEVEL_3)) {
                        return STORK_QAA_LEVEL_3;
                    } else if (scopeParam.equalsIgnoreCase(LEVEL_4)) {
                        return STORK_QAA_LEVEL_4;
                    }
                }
            } else {
                //SAML Call
                return AuthNoteHelper.getAuthContext(clientSession);
            }
        }

        return "";
    }



    private void addRequestedAuthnContext(AuthnRequestType authnRequest, String level) {
        RequestedAuthnContextType type = new RequestedAuthnContextType();
        type.addAuthnContextClassRef(level);
        type.setComparison(AuthnContextComparisonType.MINIMUM);
        authnRequest.setRequestedAuthnContext(type);
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
        List<String> scopes = PreprocessorHelper.findScopeParams(clientSession);

        if (scopes != null && PreprocessorHelper.isOIDCRequest(clientSession)) {
            //OIDC Call
            for (String scope : scopes) {
                if (scope.equalsIgnoreCase("otherOptions")) {
                    return true;
                }
            }
        } else {
            //SAML Call
            String otherOptions = AuthNoteHelper.getOtherOptions(clientSession);
            return otherOptions != null && otherOptions.equalsIgnoreCase("true");
        }

        return false;
    }


}
