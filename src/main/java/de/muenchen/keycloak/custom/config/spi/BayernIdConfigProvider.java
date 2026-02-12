package de.muenchen.keycloak.custom.config.spi;

import de.muenchen.keycloak.custom.config.domain.DisplayInformation;
import de.muenchen.keycloak.custom.config.domain.IDP;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;

public interface BayernIdConfigProvider extends Provider, ProviderFactory<BayernIdConfigProvider> {

    public IDP findIDPByAlias(String alias);

    public IDP findIDPByName(String alias);

    public boolean isPublicRealm(RealmModel realm);

    public DisplayInformation getDisplayInformation();
}
