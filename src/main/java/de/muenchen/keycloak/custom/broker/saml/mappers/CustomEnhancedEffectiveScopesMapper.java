package de.muenchen.keycloak.custom.broker.saml.mappers;

import de.muenchen.keycloak.custom.broker.saml.AuthNoteHelper;
import java.util.*;
import org.jboss.logging.Logger;
import org.keycloak.broker.provider.AbstractIdentityProviderMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.saml.SAMLIdentityProviderFactory;
import org.keycloak.models.*;
import org.keycloak.provider.ProviderConfigProperty;

/**
 * Custom Mapper, der die effektiven (hergeleiteten) Scopes in einem User-Attribut ablegt.
 *
 * @author rowe42
 * @version $Revision: 1 $
 */
public class CustomEnhancedEffectiveScopesMapper extends AbstractIdentityProviderMapper {

    private static final Logger LOGGER = Logger.getLogger(CustomEnhancedEffectiveScopesMapper.class);

    public static final String[] COMPATIBLE_PROVIDERS = { SAMLIdentityProviderFactory.PROVIDER_ID };
    private static final Set<IdentityProviderSyncMode> IDENTITY_PROVIDER_SYNC_MODES = new HashSet<>(Arrays.asList(IdentityProviderSyncMode.values()));
    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    public static final String ATTRIBUTE = "Attribute";

    static {
        ProviderConfigProperty property;
        property = new ProviderConfigProperty();
        property.setName(ATTRIBUTE);
        property.setLabel("Attribute");
        property.setHelpText("The Attribute name where to store the scopes.");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setDefaultValue("");
        configProperties.add(property);
    }

    public static final String PROVIDER_ID = "saml-custom-effective-scopes-mapper";

    @Override
    public boolean supportsSyncMode(IdentityProviderSyncMode syncMode) {
        return IDENTITY_PROVIDER_SYNC_MODES.contains(syncMode);
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String[] getCompatibleProviders() {
        return COMPATIBLE_PROVIDERS;
    }

    @Override
    public String getDisplayCategory() {
        return "Preprocessor";
    }

    @Override
    public String getDisplayType() {
        return "Custom effective scopes mapper";
    }

    @Override
    public void preprocessFederatedIdentity(KeycloakSession session, RealmModel realm, IdentityProviderMapperModel mapperModel,
            BrokeredIdentityContext context) {
        String effectiveScopes = AuthNoteHelper.getEnhancedEffectiveScopes(context.getAuthenticationSession());
        if (effectiveScopes != null) {
            final String attribute = mapperModel.getConfig().get(ATTRIBUTE);
            List<String> list = new ArrayList<>(Arrays.asList(effectiveScopes.split("##")));
            context.setUserAttribute(attribute, list);
        }
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel,
            BrokeredIdentityContext context) {
        String effectiveScopes = AuthNoteHelper.getEnhancedEffectiveScopes(context.getAuthenticationSession());
        if (effectiveScopes != null) {
            final String attribute = mapperModel.getConfig().get(ATTRIBUTE);
            List<String> list = new ArrayList<>(Arrays.asList(effectiveScopes.split("##")));
            user.setAttribute(attribute, list);
        }
    }

    @Override
    public String getHelpText() {
        return "Custom requested scopes mapper";
    }

}
