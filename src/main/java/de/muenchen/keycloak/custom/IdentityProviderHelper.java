package de.muenchen.keycloak.custom;

import de.muenchen.keycloak.custom.broker.saml.AuthNoteHelper;
import de.muenchen.keycloak.custom.forms.login.freemarker.IDPs;
import org.jboss.logging.Logger;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.util.Arrays;
import java.util.List;

public class IdentityProviderHelper {

    protected static final Logger logger = Logger.getLogger(IdentityProviderHelper.class);

    public static final String LEVEL_1 = "level1";
    public static final String LEVEL_2 = "level2";
    public static final String LEVEL_3 = "level3";
    public static final String LEVEL_4 = "level4";
    public static final String STORK_QAA_LEVEL_1 = "STORK-QAA-Level-1";
    public static final String STORK_QAA_LEVEL_2 = "STORK-QAA-Level-2";
    public static final String STORK_QAA_LEVEL_3 = "STORK-QAA-Level-3";
    public static final String STORK_QAA_LEVEL_4 = "STORK-QAA-Level-4";
    public static final String AUTH_CONTEXT = "AUTH_CONTEXT";

    /**
     * Checks whether the given Identity Provider should be removed from the list of login option if the provided
     * authLevel ist required as a minimum.
     *
     * @param alias The alias of the IdentityProvider to check
     * @param authLevel the AuthLevel to check
     * @return true if to be removed
     */
    public static boolean shouldBeRemoved(AuthenticationFlowContext context, String alias, final String authLevel, final String requestedAttributeSet) {
        List<String> activeClientScopes = ScopesHelper.getClientScopes(context);
        activeClientScopes = ScopesHelper.stripScopes(activeClientScopes);

        //logger.info("Client Scopes being checked " + activeClientScopes.stream().collect(Collectors.joining(", ")));

        final IDPs idp = IDPs.findIDPByAlias(alias);

        if (idp == null) {
            logger.warn("IDP " + alias + " not found in configured list. No Authlevel-Mapping available. Will not be processed / potentially removed.");
            return true;
        }

        if (activeClientScopes == null || !activeClientScopes.contains(idp.scope)) {
            //entfernen, wenn
            // - keine Client Scopes definiert ODER
            // - nicht als Scope im aktuellen Client definiert
            // (es MUSS pro Client definiert sein, welche AuthLevel zugelassen sind)
            logger.debug("removing " + alias + " because of not-matching client-scopes. ");
            return true;
        }

        if (authLevel != null && !Arrays.asList(idp.authlevels).contains(authLevel)) {
            //authLevel wurde im Request angegeben, aber kommt bei den zugelassenen AuthLevels des IDP nicht vor
            logger.debug("removing " + alias + " because of not-matching auth-level.");
            return true;
        }

        if (requestedAttributeSet != null && !Arrays.asList(idp.requestedAttributeSets).contains(requestedAttributeSet)) {
            //requestedAttributeSet wurde im Request angegeben, aber entspricht nicht dem requestedAttributeSet des IDP
            logger.debug("removing " + alias + " because of not-matching requestedAttributeSet.");
            return true;
        }

        return false;
    }


    /**
     * Finds the authlevel in the scopes of the current request (there should be only one set, otherwise indeterministic)
     * for OIDC or the requestedAuthnContext (mapped to an authlevel) for SAML2.
     *
     * @param authenticationSession AuthenticationSessionModel
     * @return the requested authlevel in OIDC or SAML Request
     */
    public static String findAuthLevel(final AuthenticationSessionModel authenticationSession) {
        if (authenticationSession == null) {
            return null;
        }

        final List<String> scopes = findScopeParams(authenticationSession);

        if (scopes != null) {
            //OIDC Call
            for (String scope : scopes) {
                if (scope.equalsIgnoreCase(LEVEL_1)
                        || scope.equalsIgnoreCase(LEVEL_2)
                        || scope.equalsIgnoreCase(LEVEL_3)
                        || scope.equalsIgnoreCase(LEVEL_4)) {
                    return scope;
                }
            }
        } else {
            //SAML Call

            final String authContext = authenticationSession.getAuthNote(AUTH_CONTEXT);
            if (authContext != null) {
                return mapStorkToAuthLevel(authContext.trim());
            }
        }

        return null;
    }

    /**
     * Maps the given STORK-QAA-Level to the corresponding authlevel ("level1" ... "level4").
     * @param stork STORK-QAA-Level to map
     * @return the corresponding authlevel
     */
    private static String mapStorkToAuthLevel(final String stork) {
        if (stork.equalsIgnoreCase(STORK_QAA_LEVEL_1) || stork.equalsIgnoreCase(LEVEL_1)) {
            return LEVEL_1;
        } else if (stork.equalsIgnoreCase(STORK_QAA_LEVEL_2) || stork.equalsIgnoreCase(LEVEL_2)) {
            return LEVEL_2;
        } else if (stork.equalsIgnoreCase(STORK_QAA_LEVEL_3) || stork.equalsIgnoreCase(LEVEL_3)) {
            return LEVEL_3;
        } else if (stork.equalsIgnoreCase(STORK_QAA_LEVEL_4) || stork.equalsIgnoreCase(LEVEL_4)) {
            return LEVEL_4;
        }
        return null;
    }

    public static String mapAuthLevelToEIDAS(final String authlevel) {
        if (authlevel.equalsIgnoreCase(STORK_QAA_LEVEL_1) || authlevel.equalsIgnoreCase(LEVEL_1)) {
            return "http://eidas.europa.eu/LoA/low";
        } else if (authlevel.equalsIgnoreCase(STORK_QAA_LEVEL_2) || authlevel.equalsIgnoreCase(LEVEL_2)) {
            return "http://eidas.europa.eu/LoA/?";
        } else if (authlevel.equalsIgnoreCase(STORK_QAA_LEVEL_3) || authlevel.equalsIgnoreCase(LEVEL_3)) {
            return "http://eidas.europa.eu/LoA/substantial";
        } else if (authlevel.equalsIgnoreCase(STORK_QAA_LEVEL_4) || authlevel.equalsIgnoreCase(LEVEL_4)) {
            return "http://eidas.europa.eu/LoA/high";
        } else {
            logger.warn("Error while mapping authlevel " + authlevel);
            return null;
        }
    }




    /**
     * Return the scopes currently set in the request.
     * @param clientSession AuthenticationSessionModel
     * @return the scopes in the current request as List of String, or null if not existing
     */
    public static List<String> findScopeParams(final AuthenticationSessionModel clientSession) {
        List<String> scopes = null;

        if (clientSession != null) {
            final String scopeParam = clientSession.getClientNote(OAuth2Constants.SCOPE);
            if (scopeParam != null) {
                scopes = Arrays.asList(scopeParam.split("\\s+"));
            }
        }
        return scopes;
    }

    /**
     * Prüft, ob (im Fall von OIDC) einer der Scopes any oder legalEntity existiert und
     * gibt in dem Fall diesen String zurück. Wird dann im Request als RequestedAttributeSet gesetzt.
     * @param clientSession the AuthenticationSessionModel
     * @return requestedAttributeSet als String
     */
    public static String findRequestedAttributeSet(final AuthenticationSessionModel clientSession) {
        if (clientSession == null) {
            return null;
        }

        List<String> scopes = IdentityProviderHelper.findScopeParams(clientSession);

        if (scopes != null && isOIDCRequest(clientSession)) {
            //OIDC Call
            for (String scope : scopes) {
                if (scope.equalsIgnoreCase("any")) {
                    return "any";
                } else if (scope.equalsIgnoreCase("legalEntity")) {
                    return "legalEntity";
                } else if (scope.equalsIgnoreCase("person")) {
                return "person";
                } else {
                    //Buergerkonto ist standard, keine Extension benoetigt
                }
            }
        } else {
            //SAML Call
            String requestedAttributeSet = AuthNoteHelper.getRequestedAttributeSet(clientSession);
            logger.debug("Found requestedAttributeSet " + requestedAttributeSet);
            return requestedAttributeSet;
        }

        return null;
    }

    public static boolean isOIDCRequest(AuthenticationSessionModel clientSession) {
        String protocol = clientSession.getProtocol();
        if (protocol == null) {
            logger.warn("No protocol found - cannot determine whether OIDC or SAML2 Call!");
        } else return protocol.equals("openid-connect");
        return false;
    }

}
