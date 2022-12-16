package de.muenchen.keycloak.custom.broker.saml.mappers;

import de.muenchen.keycloak.custom.broker.saml.CustomSamlAuthenticationPreprocessor;
import org.jboss.logging.Logger;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.saml.mappers.UserAttributeMapper;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;

public class CustomUserAttributeMapper extends UserAttributeMapper {

    protected static final Logger logger = Logger.getLogger(CustomUserAttributeMapper.class);

    public static final String FIELD_SCOPE = "attribute.scope";
    public static final String REQUIRED_ATTRIBUTE = "attribute.required";

    @Override
    public String getDisplayCategory() {
        return "CUSTOM Attribute Importer";
    }

    @Override
    public String getDisplayType() {
        return "CUSTOM Attribute-with-Scope Importer";
    }

    //TODO diesen Mapper noch mit dem CustomUserAttributeMapper aus require-attribute zusammenführen
    public CustomUserAttributeMapper() {
        ProviderConfigProperty property1 = findOrCreateProperty(FIELD_SCOPE);
        property1.setName(FIELD_SCOPE);
        property1.setLabel("CUSTOM Scope");
        property1.setHelpText("CUSTOM field to define which scope this attributes belongs to.");
        property1.setType(ProviderConfigProperty.STRING_TYPE);

        ProviderConfigProperty property2 = findOrCreateProperty(REQUIRED_ATTRIBUTE);
        property2.setName(REQUIRED_ATTRIBUTE);
        property2.setLabel("CUSTOM Required Attribute");
        property2.setHelpText("CUSTOM field to define whether this attribute is required from federated IDP.");
        property2.setType(ProviderConfigProperty.BOOLEAN_TYPE);
    }

    private ProviderConfigProperty findOrCreateProperty(String name) {
        for (ProviderConfigProperty property : getConfigProperties()) {
            logger.info("checking " + property.getName() + " against " + name);
            if (property.getName().equals(name)) {
                logger.info("found!");
                return property;
            }
        }

        //nicht gefunden - neu erstellen
        ProviderConfigProperty property = new ProviderConfigProperty();
        getConfigProperties().add(property);
        return property;
    }

    @Override
    public void importNewUser(final KeycloakSession session, final RealmModel realm, final UserModel user, final IdentityProviderMapperModel mapperModel, final BrokeredIdentityContext context) {
        super.importNewUser(session, realm, user, mapperModel, context);
    }

    @Override
    public void updateBrokeredUser(final KeycloakSession session, final RealmModel realm, final UserModel user, final IdentityProviderMapperModel mapperModel, final BrokeredIdentityContext context) {
        super.updateBrokeredUser(session, realm, user, mapperModel, context);
    }
}
