package de.muenchen.keycloak.custom.authentication.authenticators.browser;

import de.muenchen.keycloak.custom.IdentityProviderHelper;
import de.muenchen.keycloak.custom.broker.saml.AuthNoteHelper;
import java.util.List;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.browser.IdentityProviderAuthenticator;
import org.keycloak.models.IdentityProviderModel;

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
     * Prüft ob nach Filterung aufgrund von Auth-Level oder RequestedAttributeSet nur ein einziger IDP
     * übrig bleibt.
     * Falls ja, wird direkt dorthin weitergeleitet.
     *
     * @param context AuthenticationFlowContext
     * @return true wenn nach Filterung nur ein IDP gefunden und redirect initiiert wurde, false
     *         otherwise
     */
    private boolean redirectIfOnlyOneIdp(AuthenticationFlowContext context) {
        final String authLevel = IdentityProviderHelper.findAuthLevel(context.getAuthenticationSession());
        LOG.debug("Requested authLevel found on Scopes " + authLevel);
        String requestedAttributeSet = IdentityProviderHelper.findRequestedAttributeSet(context.getAuthenticationSession());
        LOG.debug("RequestedAttributeSet found " + requestedAttributeSet);

        List<IdentityProviderModel> ipm = context.getRealm().getIdentityProvidersStream()
                .filter(IdentityProviderModel::isEnabled)
                .filter(identityProvider -> IdentityProviderHelper.keepIdp(context.getSession(), identityProvider.getAlias(), authLevel,
                        requestedAttributeSet))
                .toList();

        if (ipm.size() == 1) {
            LOG.info("Found exactly one IDP after filtering: " + ipm.getFirst().getAlias() + " - redirecting.");
            redirect(context, ipm.getFirst().getAlias());
            return true;
        }

        return false;
    }
}
