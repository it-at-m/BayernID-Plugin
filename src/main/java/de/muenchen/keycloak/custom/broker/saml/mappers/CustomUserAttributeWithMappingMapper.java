package de.muenchen.keycloak.custom.broker.saml.mappers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jboss.logging.Logger;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.saml.SAMLIdentityProviderFactory;
import org.keycloak.broker.saml.mappers.UserAttributeMapper;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;

public class CustomUserAttributeWithMappingMapper extends UserAttributeMapper {

    protected static final Logger logger = Logger.getLogger(CustomUserAttributeWithMappingMapper.class);

    //ELSTER hardcoded ergänzt, damit man den Mapper auch in elster-authenticator verwenden kann
    //entspricht dort ElsterIdentityProviderFactory.PROVIDER_ID
    public static final String[] COMPATIBLE_PROVIDERS = { SAMLIdentityProviderFactory.PROVIDER_ID, "ELSTER" };

    public static final String PROVIDER_ID = "custom-saml-user-attribute-idp-mapper";
    public static final String MAPPING = "Mapping";
    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    @Override
    public String[] getCompatibleProviders() {
        return COMPATIBLE_PROVIDERS;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayCategory() {
        return "CUSTOM Attribute Importer";
    }

    @Override
    public String getDisplayType() {
        return "CUSTOM Attribute-with-Mapping Importer";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    public CustomUserAttributeWithMappingMapper() {
        final List<ProviderConfigProperty> inheritedProperties = super.getConfigProperties();
        configProperties.addAll(inheritedProperties);

        final ProviderConfigProperty property = new ProviderConfigProperty();
        property.setName(MAPPING);
        property.setLabel("Attribute Mapping");
        property.setHelpText("Makes it possible to provide a mapping between the incoming value and a target value.");
        property.setType(ProviderConfigProperty.MAP_TYPE);
        configProperties.add(property);
    }

    @Override
    public void importNewUser(final KeycloakSession session, final RealmModel realm, final UserModel user, final IdentityProviderMapperModel mapperModel,
            final BrokeredIdentityContext context) {
        super.importNewUser(session, realm, user, mapperModel, context);
        applyMapping(mapperModel, user);
    }

    @Override
    public void updateBrokeredUser(final KeycloakSession session, final RealmModel realm, final UserModel user, final IdentityProviderMapperModel mapperModel,
            final BrokeredIdentityContext context) {
        super.updateBrokeredUser(session, realm, user, mapperModel, context);
        applyMapping(mapperModel, user);
    }

    //Version Keycloak 22
    //    private void applyMapping(final IdentityProviderMapperModel mapperModel, final UserModel user) {
    //        if (user == null || mapperModel == null) {
    //            return;
    //        }
    //        final String attribute = mapperModel.getConfig().get(USER_ATTRIBUTE);
    //        final String attributeValue = user.getFirstAttribute(attribute);
    //
    //        logger.debug("Checking attribute " + attribute + ", found value " + attributeValue);
    //
    //        final Map<String, String> mapping = mapperModel.getConfigMap(MAPPING);
    //
    //        if (attributeValue != null && mapping != null && mapping.containsKey(attributeValue)) {
    //            final String newAttributeValue = mapping.get(attributeValue);
    //            logger.info("Setting " + attribute + " to " + newAttributeValue + " (mapping from " + attributeValue + ")");
    //            user.setSingleAttribute(attribute, newAttributeValue);
    //        }
    //    }

    //Version Keycloak 24
    private void applyMapping(final IdentityProviderMapperModel mapperModel, final UserModel user) {
        if (user == null || mapperModel == null) {
            return;
        }
        final String attribute = mapperModel.getConfig().get(USER_ATTRIBUTE);
        final String attributeValue = user.getFirstAttribute(attribute);

        logger.debug("Checking attribute " + attribute + ", found value " + attributeValue);

        final Map<String, List<String>> mapping = mapperModel.getConfigMap(MAPPING);

        if (attributeValue != null && mapping != null && mapping.containsKey(attributeValue)) {
            final List<String> newAttributeValueList = mapping.get(attributeValue);
            if (newAttributeValueList == null || newAttributeValueList.size() > 1) {
                logger.info("Liste für " + attributeValue + " ist nicht 1.");
            } else {
                final String newAttributeValue = newAttributeValueList.get(0);
                logger.info("Setting " + attribute + " to " + newAttributeValue + " (mapping from " + attributeValue + ")");
                user.setSingleAttribute(attribute, newAttributeValue);
            }
        }
    }
}
