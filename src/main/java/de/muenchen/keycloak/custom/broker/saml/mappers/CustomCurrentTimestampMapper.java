package de.muenchen.keycloak.custom.broker.saml.mappers;

import java.time.Instant;
import java.util.*;
import org.keycloak.broker.provider.AbstractIdentityProviderMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.saml.SAMLIdentityProviderFactory;
import org.keycloak.models.*;
import org.keycloak.protocol.saml.mappers.AttributeStatementHelper;
import org.keycloak.provider.ProviderConfigProperty;

public class CustomCurrentTimestampMapper extends AbstractIdentityProviderMapper {
    public static final String PROVIDER_ID = "custom-current-timestamp-mapper";
    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    //ELSTER hardcoded ergänzt, damit man den Mapper auch in elster-authenticator verwenden kann
    //entspricht dort ElsterIdentityProviderFactory.PROVIDER_ID
    public static final String[] COMPATIBLE_PROVIDERS = { SAMLIdentityProviderFactory.PROVIDER_ID, "ELSTER" };
    private static final Set<IdentityProviderSyncMode> IDENTITY_PROVIDER_SYNC_MODES = new HashSet<>(Arrays.asList(IdentityProviderSyncMode.values()));

    public static final String USER_ATTRIBUTE = "user.attribute";

    static {
        ProviderConfigProperty property = new ProviderConfigProperty();
        property.setName(USER_ATTRIBUTE);
        property.setLabel("User Attribute Name");
        property.setHelpText("User attribute name to store the timestamp.  Use email, lastName, and firstName to map to those predefined user properties.");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);
    }

    @Override
    public boolean supportsSyncMode(IdentityProviderSyncMode syncMode) {
        return IDENTITY_PROVIDER_SYNC_MODES.contains(syncMode);
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
        return "CUSTOM Current-Timestamp mapper";
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
        return "CUSTOM Mapper to set the current timestamp (Unix epoch time in seconds) into the configured user attribute.";
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

    private void applyMapping(final IdentityProviderMapperModel mapperModel, final UserModel user) {
        Instant instant = Instant.now();
        long timeStampMillis = instant.toEpochMilli();
        long timeStampSeconds = Math.floorDiv(timeStampMillis, 1000L);
        final String attribute = mapperModel.getConfig().get(USER_ATTRIBUTE);
        user.setSingleAttribute(attribute, String.valueOf(timeStampSeconds));
    }

}
