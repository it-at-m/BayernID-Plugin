package de.muenchen.keycloak.custom.authentication.authenticators.browser;

import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.authenticators.browser.IdentityProviderAuthenticatorFactory;
import org.keycloak.models.KeycloakSession;

public class CustomIdentityProviderAuthenticatorFactory extends IdentityProviderAuthenticatorFactory {

    public static final String PROVIDER_ID = "custom-identity-provider-redirector";

    @Override
    public String getDisplayType() {
        return "CUSTOM Identity Provider Redirector";
    }

    @Override
    public String getHelpText() {
        return "Redirects to default Identity Provider or Identity Provider specified with kc_idp_hint query parameter. " +
                "CUSTOM: Also redirects if only one IDP exists or all other are filtered out.";
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return new CustomIdentityProviderAuthenticator();
    }

    //    @Override
    //    public Authenticator createDisplay(KeycloakSession session, String displayType) {
    //        if (displayType == null) return new CustomIdentityProviderAuthenticator();
    //        if (!OAuth2Constants.DISPLAY_CONSOLE.equalsIgnoreCase(displayType)) return null;
    //        return AttemptedAuthenticator.SINGLETON;  // ignore this authenticator
    //    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
