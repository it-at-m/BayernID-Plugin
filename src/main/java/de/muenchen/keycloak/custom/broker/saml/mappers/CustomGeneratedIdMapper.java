package de.muenchen.keycloak.custom.broker.saml.mappers;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.keycloak.broker.provider.AbstractIdentityProviderMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.saml.SAMLIdentityProviderFactory;
import org.keycloak.models.*;
import org.keycloak.protocol.saml.mappers.*;
import org.keycloak.provider.ProviderConfigProperty;

public class CustomGeneratedIdMapper extends AbstractIdentityProviderMapper {
    public static final String PROVIDER_ID = "custom-saml-generate-uuid-mapper";
    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    //ELSTER hardcoded ergänzt, damit man den Mapper auch in elster-authenticator verwenden kann
    //entspricht dort ElsterIdentityProviderFactory.PROVIDER_ID
    public static final String[] COMPATIBLE_PROVIDERS = { SAMLIdentityProviderFactory.PROVIDER_ID, "ELSTER" };

    public static final String USER_ATTRIBUTE = "user.attribute";

    static {
        ProviderConfigProperty property = new ProviderConfigProperty();
        property.setName(USER_ATTRIBUTE);
        property.setLabel("User Attribute Name");
        property.setHelpText(
                "User attribute name to store the generated UUID.  Use email, lastName, and firstName to map to those predefined user properties.");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);
    }

    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "CUSTOM UUID generator";
    }

    @Override
    public String[] getCompatibleProviders() {
        return COMPATIBLE_PROVIDERS;
    }

    @Override
    public String getDisplayCategory() {
        return AttributeStatementHelper.ATTRIBUTE_STATEMENT_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "CUSTOM Generate a new UUID into the configured user attribute.";
    }

    @Override
    public void importNewUser(final KeycloakSession session, final RealmModel realm, final UserModel user, final IdentityProviderMapperModel mapperModel,
            final BrokeredIdentityContext context) {
        super.importNewUser(session, realm, user, mapperModel, context);

        UUID uuid = UUID.randomUUID(); //generate UUID
        final String attribute = mapperModel.getConfig().get(USER_ATTRIBUTE);
        user.setSingleAttribute(attribute, uuid.toString());
    }

}
