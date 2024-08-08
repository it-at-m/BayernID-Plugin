package de.muenchen.keycloak.custom.config.spi;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class BayernIdConfigSpi implements Spi {

    @Override
    public boolean isInternal() {
        return false;
    }

    @Override
    public String getName() {
        return "idp";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return BayernIdConfigProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return BayernIdConfigProvider.class;
    }
}
