package de.muenchen.keycloak.custom.broker.saml.mappers;

import org.keycloak.broker.provider.AbstractIdentityProviderMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.saml.SAMLEndpoint;
import org.keycloak.broker.saml.SAMLIdentityProviderFactory;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.assertion.SubjectType;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jboss.logging.Logger;

/**Copied from UsernameTemplateMapper. Extrahiert das eigentliche bPK aus dem base64-kodierten
 * bPK-String.
 *
 * @author rowe42
 * @version $Revision: 1 $
 */
public class CustomIdTemplateMapper extends AbstractIdentityProviderMapper {
    
    private static final Logger LOGGER = Logger.getLogger(CustomIdTemplateMapper.class);

    public static final String[] COMPATIBLE_PROVIDERS = {SAMLIdentityProviderFactory.PROVIDER_ID};
    private static final Set<IdentityProviderSyncMode> IDENTITY_PROVIDER_SYNC_MODES = new HashSet<>(Arrays.asList(IdentityProviderSyncMode.values()));
    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    public static final String TEMPLATE = "template";

    static {
        ProviderConfigProperty property;
        property = new ProviderConfigProperty();
        property.setName(TEMPLATE);
        property.setLabel("Template");
        property.setHelpText("Template to use to format the ID to import.  Substitutions are enclosed in ${}.  For example: '${ALIAS}.${NAMEID}'.  ALIAS is the provider alias.  NAMEID is that SAML name id assertion.  ATTRIBUTE.<NAME> references a SAML attribute where name is the attribute name or friendly name.");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setDefaultValue("${ATTRIBUTE.bPK}");
        configProperties.add(property);
    }

    public static final String PROVIDER_ID = "saml-custom-id-idp-mapper";

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
        return "Custom ID Template Importer";
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {

    }
    static Pattern substitution = Pattern.compile("\\$\\{([^}]+)\\}");

    @Override
    public void preprocessFederatedIdentity(KeycloakSession session, RealmModel realm, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        AssertionType assertion = (AssertionType)context.getContextData().get(SAMLEndpoint.SAML_ASSERTION);
        String template = mapperModel.getConfig().get(TEMPLATE);
        Matcher m = substitution.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String variable = m.group(1);
            if (variable.equals("ALIAS")) {
                m.appendReplacement(sb, context.getIdpConfig().getAlias());
            } else if (variable.equals("UUID")) {
                m.appendReplacement(sb, KeycloakModelUtils.generateId());
            } else if (variable.equals("NAMEID")) {
                SubjectType subject = assertion.getSubject();
                SubjectType.STSubType subType = subject.getSubType();
                NameIDType subjectNameID = (NameIDType) subType.getBaseID();
                m.appendReplacement(sb, subjectNameID.getValue());
            } else if (variable.startsWith("ATTRIBUTE.")) {
                String name = variable.substring("ATTRIBUTE.".length());
                String value = "";
                for (AttributeStatementType statement : assertion.getAttributeStatements()) {
                    for (AttributeStatementType.ASTChoiceType choice : statement.getAttributes()) {
                        AttributeType attr = choice.getAttribute();
                        if (name.equals(attr.getName()) || name.equals(attr.getFriendlyName())) {
                            List<Object> attributeValue = attr.getAttributeValue();
                            if (attributeValue != null && !attributeValue.isEmpty()) {
                                value = attributeValue.get(0).toString();
                            }
                            break;
                        }
                    }
                }
                //ERWEITERUNG
                if (variable.equalsIgnoreCase("attribute.bpk")) {
                    value = MapperHelper.extractIdFromBPK(value);
                }
                //ERWEITERUNG ENDE
                m.appendReplacement(sb, value);
            } else {
                m.appendReplacement(sb, m.group(1));
            }

        }
        m.appendTail(sb);
        //context.setModelUsername(sb.toString());
        
        //ERWEITERUNG
        String id = sb.toString();

        if (id.trim().isEmpty()) {
            id = "ID_MISSING_DO_NOT_USE";
        }

        LOGGER.debug("(if new:) Setting ID to " + id);
        context.setId(id);
        //ERWEITRUNG ENDE
    }
    
    @Override
    public String getHelpText() {
        return "Format the ID to import.";
    }

}
