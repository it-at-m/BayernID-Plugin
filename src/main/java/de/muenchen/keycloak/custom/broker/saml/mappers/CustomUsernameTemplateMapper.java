package de.muenchen.keycloak.custom.broker.saml.mappers;

import org.keycloak.broker.saml.mappers.UsernameTemplateMapper;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jboss.logging.Logger;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.saml.SAMLEndpoint;
import org.keycloak.broker.saml.SAMLIdentityProviderFactory;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.assertion.SubjectType;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;

/**
 * Ermöglicht, das bPK auch als Username zu setzen.
 * @author rowe42
 */
public class CustomUsernameTemplateMapper extends UsernameTemplateMapper {

    private static final Logger LOGGER = Logger.getLogger(CustomUsernameTemplateMapper.class);

    public static final String[] COMPATIBLE_PROVIDERS = {SAMLIdentityProviderFactory.PROVIDER_ID};

    @Override
    public String[] getCompatibleProviders() {
        return COMPATIBLE_PROVIDERS;
    }

    @Override
    public String getDisplayType() {
        return "Custom Username Template Importer";
    }

    static Pattern substitution = Pattern.compile("\\$\\{([^}]+)\\}");

    @Override
    public void preprocessFederatedIdentity(KeycloakSession session, RealmModel realm, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        AssertionType assertion = (AssertionType) context.getContextData().get(SAMLEndpoint.SAML_ASSERTION);
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

        LOGGER.debug("(if new:) Setting Username to " + id);
        context.setModelUsername(id);
        //ERWEITRUNG ENDE

    }

}
