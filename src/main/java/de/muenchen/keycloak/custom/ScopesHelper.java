package de.muenchen.keycloak.custom;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.jboss.logging.Logger;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;

public class ScopesHelper {

    protected static final Logger logger = Logger.getLogger(ScopesHelper.class);

    private static final String OIDC_SUFFIX = "_oidc";
    private static final String SAML_SUFFIX = "_saml";

    /**
     * Holt alle Client Scopes vom aktuellen Client - sowohl die default scopes als auch die optional
     * scopes.
     *
     * @return client scopes als Liste
     */
    public static List<String> getClientScopes(final KeycloakSession session) {
        final ClientModel currentClient = session.getContext().getClient();

        if (currentClient == null) {
            return null;
        }

        //        Version für RH-SSO 7.5 / Keycloak 15.0.2
        final Map<String, ClientScopeModel> defaultClientScopes = currentClient.getClientScopes(true);
        final Map<String, ClientScopeModel> optionalClientScopes = currentClient.getClientScopes(false);

        logger.debug("Default Scopes on current client: " + (defaultClientScopes != null ? String.join(", ", defaultClientScopes.keySet()) : ""));
        logger.debug("Optional Scopes on current client: " + (optionalClientScopes != null ? String.join(", ", optionalClientScopes.keySet()) : ""));

        if (defaultClientScopes == null && optionalClientScopes == null) {
            return null;
        }
        final List<String> availableScopes = new ArrayList<>();
        if (defaultClientScopes != null) {
            availableScopes.addAll(defaultClientScopes.keySet());
        }
        if (optionalClientScopes != null) {
            availableScopes.addAll(optionalClientScopes.keySet());
        }
        return availableScopes;
    }

    /**
     * Iteriert alle übergebenen Scopes und entfernt den Suffix "_oidc" oder "_saml" falls existent.
     *
     * @param clientScopes client-scopes, die iteriert werden sollen
     * @return neue Liste mit client-scopes, bei denen das Suffix entfernt ist
     */
    public static List<String> stripScopes(final Collection<String> clientScopes) {
        if (clientScopes == null) {
            return null;
        }

        final List<String> strippedScopes = new ArrayList<>();

        for (String scope : clientScopes) {
            if (scope.endsWith(OIDC_SUFFIX)) {
                strippedScopes.add(scope.substring(0, scope.lastIndexOf(OIDC_SUFFIX)));
            } else if (scope.endsWith(SAML_SUFFIX)) {
                strippedScopes.add(scope.substring(0, scope.lastIndexOf(SAML_SUFFIX)));
            } else {
                strippedScopes.add(scope);
            }
        }

        return strippedScopes;
    }

}
