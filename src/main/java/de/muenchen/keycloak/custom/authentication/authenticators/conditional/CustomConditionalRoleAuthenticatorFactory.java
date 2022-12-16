package de.muenchen.keycloak.custom.authentication.authenticators.conditional;

import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticator;
import org.keycloak.authentication.authenticators.conditional.ConditionalRoleAuthenticatorFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

public class CustomConditionalRoleAuthenticatorFactory extends ConditionalRoleAuthenticatorFactory {

        public static final String PROVIDER_ID = "custom-conditional-user-role";

        @Override
        public String getId() {
            return PROVIDER_ID;
        }

        @Override
        public String getDisplayType() {
            return "CUSTOM Condition - user role";
        }



        @Override
        public String getHelpText() {
            return "CUSTOM Flow is executed only if user has the given role.";
        }

        @Override
        public List<ProviderConfigProperty> getConfigProperties() {
            List<ProviderConfigProperty> configProperties = super.getConfigProperties();
            if (configProperties.size() > 0) {
                ProviderConfigProperty role = configProperties.get(0);
                if (role != null && CONDITIONAL_USER_ROLE.equals(role.getName())) {
                    String currentHelptText = role.getHelpText();
                    role.setHelpText(currentHelptText + " CUSTOM: Instead of the specific clientname you can also use ${clientId} as placeholder for the current client.");
                }
            }
            return configProperties;
        }

        @Override
        public ConditionalAuthenticator getSingleton() {
            return CustomConditionalRoleAuthenticator.SINGLETON;
        }

}
