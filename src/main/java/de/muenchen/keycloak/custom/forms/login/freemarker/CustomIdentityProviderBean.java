package de.muenchen.keycloak.custom.forms.login.freemarker;

import de.muenchen.keycloak.custom.IdentityProviderHelper;
import java.util.List;
import org.jboss.logging.Logger;
import org.keycloak.forms.login.freemarker.model.IdentityProviderBean;
import org.keycloak.sessions.AuthenticationSessionModel;

public class CustomIdentityProviderBean extends IdentityProviderBean {

    protected static final Logger logger = Logger.getLogger(CustomIdentityProviderBean.class);

    public CustomIdentityProviderBean(IdentityProviderBean ipb) {
        super(ipb.getSession(), ipb.getRealm(), ipb.getBaseURI(), ipb.getFlowContext());
    }

    @Override
    public List<IdentityProvider> getProviders() {
        AuthenticationSessionModel authenticationSession = session.getContext().getAuthenticationSession();
        final String authLevel = IdentityProviderHelper.findAuthLevel(authenticationSession);
        logger.debug("Requested authLevel found on Scopes " + authLevel);

        String requestedAttributeSet = IdentityProviderHelper.findRequestedAttributeSet(authenticationSession);
        logger.debug("RequestedAttributeSet found " + requestedAttributeSet);

        List<IdentityProvider> ips = searchForIdentityProviders(null);
        return ips.stream().filter(ip -> IdentityProviderHelper.keepIdp(session, ip.getAlias(), authLevel, requestedAttributeSet))
                .toList();
    }

}
