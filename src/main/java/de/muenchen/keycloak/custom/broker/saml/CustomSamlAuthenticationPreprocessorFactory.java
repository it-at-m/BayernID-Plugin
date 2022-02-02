package de.muenchen.keycloak.custom.broker.saml;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.protocol.saml.preprocessor.SamlAuthenticationPreprocessor;

/**
 * @author rowe42
 */
public class CustomSamlAuthenticationPreprocessorFactory {

    public static final String PROVIDER_ID = "custompreprocessor";
    
//    @Override
    public String getId() {
        return PROVIDER_ID;
    }
    
//    @Override
    public void init(Config.Scope config) {
    }

//    @Override
    public SamlAuthenticationPreprocessor create(KeycloakSession session) {
        return new CustomSamlAuthenticationPreprocessor();
    }

//    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

//    @Override
    public void close() {
    }

}
