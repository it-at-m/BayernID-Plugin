package de.muenchen.keycloak.custom.authentication.authenticators.browser;

import de.muenchen.keycloak.custom.IdentityProviderHelper;
import de.muenchen.keycloak.custom.broker.saml.AuthNoteHelper;
import org.jboss.logging.Logger;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.authentication.authenticators.browser.IdentityProviderAuthenticator;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.ClientSessionCode;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class CustomIdentityProviderAuthenticator extends IdentityProviderAuthenticator {

    private static final Logger LOG = Logger.getLogger(CustomIdentityProviderAuthenticator.class);

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        if (!redirectIfSamlIdpHint(context)) {
            if (!redirectIfOnlyOneIdp(context)) {
                super.authenticate(context);
            }
        }
    }

    /**
     * Prüft ob im SAML-Request im Extensions-Bereich ein IDP-Hint mit dem Tag IdpHint existiert.
     * Falls ja, wird direkt dorthin weitergeleitet.
     *
     * @param context AuthenticationFlowContext
     * @return true wenn IDP-Hint im SAML-Request gefunden und redirect initiiert wurde, false otherwise
     */
    private boolean redirectIfSamlIdpHint(AuthenticationFlowContext context) {
        String idpHint = AuthNoteHelper.getIdpHint(context.getAuthenticationSession());
        if (idpHint != null && !idpHint.isEmpty()) {
            redirect(context, idpHint);
            return true;
        }

        return false;
    }

    /**
     * Prüft ob nach Filterung aufgrund von Auth-Level oder RequestedAttributeSet nur ein einziger IDP übrig bleibt.
     * Falls ja, wird direkt dorthin weitergeleitet.
     *
     * @param context AuthenticationFlowContext
     @return true wenn nach Filterung nur ein IDP gefunden und redirect initiiert wurde, false otherwise
     */
    private boolean redirectIfOnlyOneIdp(AuthenticationFlowContext context) {
        final String authLevel = IdentityProviderHelper.findAuthLevel(context.getAuthenticationSession());
        LOG.debug("Requested authLevel found on Scopes " + authLevel);

        String requestedAttributeSet = IdentityProviderHelper.findRequestedAttributeSet(context.getAuthenticationSession());
        LOG.debug("RequestedAttributeSet found " + requestedAttributeSet);

        List<IdentityProviderModel> ipm = context.getRealm().getIdentityProvidersStream()
                .filter(IdentityProviderModel::isEnabled)
                .filter(identityProvider -> !IdentityProviderHelper.shouldBeRemoved(context, identityProvider.getAlias(), authLevel, requestedAttributeSet))
                .collect(Collectors.toList());

        if (ipm.size() == 1) {
            LOG.info("Found exactly one IDP after filtering: " + ipm.get(0).getAlias() + " - redirecting.");
            redirect(context, ipm.get(0).getAlias());
            return true;
        }

        return false;
    }

    //vollständig aus IdentityProviderAuthenticator kopiert (wegen private)
    private void redirect(AuthenticationFlowContext context, String providerId) {
        Optional<IdentityProviderModel> idp = context.getRealm().getIdentityProvidersStream()
                .filter(IdentityProviderModel::isEnabled)
                .filter(identityProvider -> Objects.equals(providerId, identityProvider.getAlias()))
                .findFirst();
        if (idp.isPresent()) {
            String accessCode = new ClientSessionCode<>(context.getSession(), context.getRealm(), context.getAuthenticationSession()).getOrGenerateCode();
            String clientId = context.getAuthenticationSession().getClient().getClientId();
            String tabId = context.getAuthenticationSession().getTabId();
            URI location = Urls.identityProviderAuthnRequest(context.getUriInfo().getBaseUri(), providerId, context.getRealm().getName(), accessCode, clientId, tabId);
            if (context.getAuthenticationSession().getClientNote(OAuth2Constants.DISPLAY) != null) {
                location = UriBuilder.fromUri(location).queryParam(OAuth2Constants.DISPLAY, context.getAuthenticationSession().getClientNote(OAuth2Constants.DISPLAY)).build();
            }
            Response response = Response.seeOther(location)
                    .build();
            // will forward the request to the IDP with prompt=none if the IDP accepts forwards with prompt=none.
            if ("none".equals(context.getAuthenticationSession().getClientNote(OIDCLoginProtocol.PROMPT_PARAM)) &&
                    Boolean.valueOf(idp.get().getConfig().get(ACCEPTS_PROMPT_NONE))) {
                context.getAuthenticationSession().setAuthNote(AuthenticationProcessor.FORWARDED_PASSIVE_LOGIN, "true");
            }
            LOG.debugf("Redirecting to %s", providerId);
            context.forceChallenge(response);
            return;
        }

        LOG.warnf("Provider not found or not enabled for realm %s", providerId);
        context.attempted();
    }
}
