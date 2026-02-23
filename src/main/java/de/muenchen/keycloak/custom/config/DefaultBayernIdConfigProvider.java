package de.muenchen.keycloak.custom.config;

import de.muenchen.keycloak.custom.config.domain.BayernIdConfig;
import de.muenchen.keycloak.custom.config.domain.DisplayInformation;
import de.muenchen.keycloak.custom.config.domain.IDP;
import de.muenchen.keycloak.custom.config.spi.BayernIdConfigProvider;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import org.jboss.logging.Logger;
import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.util.JsonSerialization;

public class DefaultBayernIdConfigProvider implements BayernIdConfigProvider {

    protected static final Logger logger = Logger.getLogger(DefaultBayernIdConfigProvider.class);

    protected static final String PROVIDER_ID = "default";

    protected static final String CONFIG_FILE = "configFile";
    protected static final String CONFIG_FILE_DEFAULT = "conf/bayernIdConfig.json";

    protected BayernIdConfig config;

    @Override
    public BayernIdConfigProvider create(KeycloakSession keycloakSession) {
        return this;
    }

    @Override
    public void init(Scope scope) {
        String config = scope.get(CONFIG_FILE, CONFIG_FILE_DEFAULT);
        Path configPath = Paths.get(config);

        logger.info("Loading config from " + configPath.toAbsolutePath());
        if (Files.exists(configPath)) {
            // read from file
            this.config = readFromPath(configPath);
        } else {
            // read from classpath
            this.config = readFromClasspath(config);
        }

        logger.info("Bayern ID config: \n" + this.config);
    }

    private BayernIdConfig readFromPath(Path configPath) {
        logger.info("Reading config from path " + configPath.toAbsolutePath());
        BayernIdConfig config = null;
        try (InputStream in = Files.newInputStream(configPath)) {
            config = JsonSerialization.readValue(in, BayernIdConfig.class);
        } catch (IOException e) {
            throw new RuntimeException("DefaultBayernIdConfigProvider config file not readable: " + configPath.toAbsolutePath(), e);
        }
        return config;
    }

    private BayernIdConfig readFromClasspath(String configPath) {
        logger.info("Reading config from classpath " + configPath);
        BayernIdConfig config = null;
        try (InputStream in = BayernIdConfigProvider.class.getClassLoader().getResourceAsStream(configPath)) {
            if (in == null) {
                throw new IOException("DefaultBayernIdConfigProvider config file not found on classpath: " + configPath);
            }
            config = JsonSerialization.readValue(in, BayernIdConfig.class);
        } catch (IOException e) {
            throw new RuntimeException("DefaultBayernIdConfigProvider config file not readable: " + configPath, e);
        }
        return config;
    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {

    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public void close() {

    }

    @Override
    public IDP findIDPByAlias(String alias) {
        IDP result = config.getIdp().stream().filter(idp -> Arrays.asList(idp.getAlias()).contains(alias)).findFirst().orElse(null);
        return result;
    }

    public IDP findIDPByName(String name) {
        IDP result = config.getIdp().stream().filter(idp -> idp.getName().equals(name)).findFirst().orElse(null);
        return result;
    }

    @Override
    public boolean isPublicRealm(RealmModel realm) {
        return config.getPublicRealms().stream().filter(realmName -> realmName.equalsIgnoreCase(realm.getName().trim())).findFirst().isPresent();
    }

    @Override
    public DisplayInformation getDisplayInformation() {
        return config.getDisplayInformation();
    }
}
