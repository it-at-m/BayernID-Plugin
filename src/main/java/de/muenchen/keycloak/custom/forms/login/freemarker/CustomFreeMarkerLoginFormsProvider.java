package de.muenchen.keycloak.custom.forms.login.freemarker;

import de.muenchen.keycloak.custom.IdentityProviderHelper;
import jakarta.ws.rs.core.UriBuilder;
import java.util.*;
import org.jboss.logging.Logger;
import org.keycloak.forms.login.LoginFormsPages;
import org.keycloak.forms.login.freemarker.FreeMarkerLoginFormsProvider;
import org.keycloak.forms.login.freemarker.model.IdentityProviderBean;
import org.keycloak.models.KeycloakSession;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.theme.Theme;

/**
 * siehe
 * https://stackoverflow.com/questions/44072608/keycloak-access-cookie-and-or-url-query-params-inside-freemarker-template
 */
public class CustomFreeMarkerLoginFormsProvider extends FreeMarkerLoginFormsProvider {

    protected static final Logger logger = Logger.getLogger(CustomFreeMarkerLoginFormsProvider.class);

    public static final String SOCIAL = "social";
    public static final String PUBLIC = "public";
    public static final String DEMO = "demo";
    public static final String A61 = "A61";

    public CustomFreeMarkerLoginFormsProvider(final KeycloakSession session) {
        super(session);
    }

    @Override
    protected void createCommonAttributes(final Theme theme, final Locale locale, final Properties messagesBundle, final UriBuilder baseUriBuilder,
            final LoginFormsPages page) {
        super.createCommonAttributes(theme, locale, messagesBundle, baseUriBuilder, page);

        //die folgenden Prüfungen nur im Realm "public" (und aus historischen Gründen alternativ "demo" und "A61") durchführen (um Nebenwirkungen in anderen Realms zu minimieren)
        if (session == null || session.getContext() == null || session.getContext().getRealm() == null ||
                !Arrays.asList(PUBLIC, DEMO, A61).contains(session.getContext().getRealm().getName().trim())) {
            return;
        }

        AuthenticationSessionModel authenticationSession = session.getContext().getAuthenticationSession();

        final String authLevel = IdentityProviderHelper.findAuthLevel(authenticationSession);
        logger.info("Requested authLevel found on Scopes " + authLevel);

        String requestedAttributeSet = IdentityProviderHelper.findRequestedAttributeSet(authenticationSession);
        logger.info("RequestedAttributeSet found " + requestedAttributeSet);

        IdentityProviderBean ipb = (IdentityProviderBean) this.attributes.get(SOCIAL);

        if (ipb == null) {
            //Kein Attribut "social" gefunden - dadurch auch keine IdentityProvider konfiguriert
            return;
        }

        final List<IdentityProviderBean.IdentityProvider> ips = ipb.getProviders();

        if (ips == null) {
            //Keine IdentityProvider konfiguriert
            return;
        }

        final List<IdentityProviderBean.IdentityProvider> toBeRemoved = new ArrayList<>();

        for (IdentityProviderBean.IdentityProvider ip : ips) {
            if (IdentityProviderHelper.shouldBeRemoved(this.context, ip.getAlias(), authLevel, requestedAttributeSet)) {
                toBeRemoved.add(ip);
            }
        }

        for (IdentityProviderBean.IdentityProvider tbr : toBeRemoved) {
            ips.remove(tbr);
            logger.debug("Removed " + tbr.getAlias());
        }

    }

}
