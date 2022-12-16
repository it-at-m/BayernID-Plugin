package de.muenchen.keycloak.custom.broker.saml;

import org.keycloak.sessions.AuthenticationSessionModel;
import org.w3c.dom.Element;

import java.util.*;
import java.util.stream.Collectors;

public class AuthNoteHelper {
    public static void setRequestedScopes(String requestedScopes, AuthenticationSessionModel authSession) {
        authSession.setAuthNote("REQUESTED_SCOPES", requestedScopes);
    }

    public static String getRequestedScopes(AuthenticationSessionModel authSession) {
        return authSession.getAuthNote("REQUESTED_SCOPES");
    }

    public static List<String> getRequestedScopesAsList(AuthenticationSessionModel authSession) {
        String requestedScopesString = authSession.getAuthNote("REQUESTED_SCOPES");
        if (requestedScopesString != null) {
            return Arrays.asList(requestedScopesString.split("##"));
        }
        return null;
    }

    public static void setEffectiveScopes(String effectiveScopes, AuthenticationSessionModel authSession) {
        authSession.setAuthNote("EFFECTIVE_SCOPES", effectiveScopes);
    }

    public static void setEffectiveScopes(Set<String> effectiveScopes, AuthenticationSessionModel authSession) {
        authSession.setAuthNote("EFFECTIVE_SCOPES", String.join("##", effectiveScopes));
    }

    public static String getEffectiveScopes(AuthenticationSessionModel authSession) {
        return authSession.getAuthNote("EFFECTIVE_SCOPES");
    }

    public static Set<String> getEffectiveScopesAsSet(AuthenticationSessionModel authSession) {
        String requestedScopes = authSession.getAuthNote("EFFECTIVE_SCOPES");
        return createSetFromString(requestedScopes);
    }

    public static void setEnhancedEffectiveScopes(String enhancedEffectiveScopes, AuthenticationSessionModel authSession) {
        authSession.setAuthNote("ENHANCED_EFFECTIVE_SCOPES", enhancedEffectiveScopes);
    }

    public static void setEnhancedEffectiveScopes(Set<String> enhancedEffectiveScopes, AuthenticationSessionModel authSession) {
        authSession.setAuthNote("ENHANCED_EFFECTIVE_SCOPES", String.join("##", enhancedEffectiveScopes));
    }

    public static String getEnhancedEffectiveScopes(AuthenticationSessionModel authSession) {
        return authSession.getAuthNote("ENHANCED_EFFECTIVE_SCOPES");
    }

    public static Set<String> getEnhancedEffectiveScopesAsSet(AuthenticationSessionModel authSession) {
        String enhancedEffectiveScopes = authSession.getAuthNote("ENHANCED_EFFECTIVE_SCOPES");
        return createSetFromString(enhancedEffectiveScopes);
    }

    public static void setAuthMethods(String authMethods, AuthenticationSessionModel authSession) {
        authSession.setAuthNote("AUTH_METHODS", authMethods);
    }

    public static String getAuthMethods(AuthenticationSessionModel authSession) {
        return authSession.getAuthNote("AUTH_METHODS");
    }

    public static Set<String> getAuthMethodsAsSet(AuthenticationSessionModel authSession) {
        String authMethods = authSession.getAuthNote("AUTH_METHODS");
        return createSetFromString(authMethods);
    }

    public static void setAllowedMethods(String allowedMethods, AuthenticationSessionModel authSession) {
        authSession.setAuthNote("ALLOWED_METHODS", allowedMethods);
    }

    public static String getAllowedMethods(AuthenticationSessionModel authSession) {
        return authSession.getAuthNote("ALLOWED_METHODS");
    }

    public static void setIdpHint(String allowedMethods, AuthenticationSessionModel authSession) {
        authSession.setAuthNote("IDP_HINT", allowedMethods);
    }

    public static String getIdpHint(AuthenticationSessionModel authSession) {
        return authSession.getAuthNote("IDP_HINT");
    }

    public static Set<String> getAllowedMethodsAsSet(AuthenticationSessionModel authSession) {
        String allowedMethods = authSession.getAuthNote("ALLOWED_METHODS");
        return createSetFromString(allowedMethods);
    }

    public static void setRequestedAttributes(String requestedAttributes, AuthenticationSessionModel authSession) {
        authSession.setAuthNote("REQUESTED_ATTRIBUTES", requestedAttributes);
    }

    public static String getRequestedAttributes(AuthenticationSessionModel authSession) {
        return authSession.getAuthNote("REQUESTED_ATTRIBUTES");
    }

    public static void setAuthContext(String authContext, AuthenticationSessionModel authSession) {
        authSession.setAuthNote("AUTH_CONTEXT", authContext);
    }

    public static String getAuthContext(AuthenticationSessionModel authSession) {
        return authSession.getAuthNote("AUTH_CONTEXT");
    }

    public static void setRequestedAttributeSet(String requestedAttributeSet, AuthenticationSessionModel authSession) {
        authSession.setAuthNote("REQUESTED_ATTRIBUTE_SET", requestedAttributeSet);
    }

    public static String getRequestedAttributeSet(AuthenticationSessionModel authSession) {
        return authSession.getAuthNote("REQUESTED_ATTRIBUTE_SET");
    }

    public static void setOtherOptions(String otherOptions, AuthenticationSessionModel authSession) {
        authSession.setAuthNote("OTHER_OPTIONS", otherOptions);
    }

    public static String getOtherOptions(AuthenticationSessionModel authSession) {
        return authSession.getAuthNote("OTHER_OPTIONS");
    }

    public static void setAuthNote(Element el, String note, AuthenticationSessionModel authSession) {
        String tagName = el.getLocalName();
        String nameAttribute = el.getAttribute("Name");
        String authNodeName = "SAML_EXTENSION";
        if (tagName != null && !tagName.equals("")) {

            authNodeName += "_" + tagName.toUpperCase();
            if (nameAttribute != null && !nameAttribute.equals("")) {
                authNodeName += "_" + nameAttribute.toUpperCase();
            }
        }
        PreprocessorHelper.logger.info("Setting AuthNote " + authNodeName + " with value " + note);
        authSession.setAuthNote(authNodeName, note);
    }

    private static Set<String> createSetFromString(String s) {
        if (s != null && s.length() > 0) {
            Set<String> allowedMethods = new HashSet<>();
            Collections.addAll(allowedMethods, s.split("##"));
            return allowedMethods;
        }
        return null;
    }


}
