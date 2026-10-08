package de.muenchen.keycloak.custom.broker.saml.mappers;

import java.util.ArrayList;
import java.util.List;
import org.jboss.logging.Logger;
import org.keycloak.broker.provider.AbstractIdentityProviderMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.saml.SAMLEndpoint;
import org.keycloak.broker.saml.SAMLIdentityProviderFactory;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;

public class CustomReplaceBrokerUserIdMapper extends AbstractIdentityProviderMapper {

    private static final Logger LOGGER = Logger.getLogger(AbstractIdentityProviderMapper.class);

    public static final String PROVIDER_ID = "saml-replace-broker-userid-mapper";
    public static final String ATTRIBUTE_NAME_CONFIG = "attribute.name";

    public static final String[] COMPATIBLE_PROVIDERS = { SAMLIdentityProviderFactory.PROVIDER_ID, "ELSTER" };

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
        return "CUSTOM Replace Broker-User-ID Mapper";
    }

    @Override
    public String getDisplayType() {
        return "CUSTOM Replace Broker-User-ID Mapper";
    }

    @Override
    public String getHelpText() {
        return "Setzt die Broker-User-ID (Linked Identity) auf den Wert des konfigurierten Attributs. Fehlt das Attribut, bleibt die NameID erhalten (für Gast-User).";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        List<ProviderConfigProperty> configProperties = new ArrayList<>();
        ProviderConfigProperty property = new ProviderConfigProperty();
        property.setName(ATTRIBUTE_NAME_CONFIG);
        property.setLabel("Attribute Name");
        property.setHelpText("Name des SAML-Attributes für die User ID (z. B. bPK2)");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setDefaultValue("bPK2");
        configProperties.add(property);
        return configProperties;
    }

    @Override
    public void preprocessFederatedIdentity(KeycloakSession session, RealmModel realm,
            IdentityProviderMapperModel mapperModel,
            BrokeredIdentityContext context) {

        String targetAttributeName = mapperModel.getConfig().getOrDefault(ATTRIBUTE_NAME_CONFIG, "bPK2");

        // 1. Attributwert aus dem Kontext extrahieren
        String brokerUserId = null;
        AttributeType foundAttribute = findAttribute(context, targetAttributeName);

        if (foundAttribute != null && foundAttribute.getAttributeValue() != null
                && foundAttribute.getAttributeValue() != null && !foundAttribute.getAttributeValue().isEmpty()) {
            brokerUserId = foundAttribute.getAttributeValue().getFirst().toString();
        }

        // 2. Entscheidung treffen: brokeredUserId als Linked ID oder NameID behalten
        if (brokerUserId != null && !brokerUserId.isBlank()) {
            // brokeredUserId ist da -> Broker User ID explizit überschreiben
            context.setId(brokerUserId);
            context.setBrokerUserId(brokerUserId.trim());
            context.setUsername(brokerUserId.trim());
            LOGGER.debug("setting ID, brokerUserId and Username to " + brokerUserId.trim());
        } else {
            LOGGER.debug("Leaving ID, brokerUserId and Username as default (NameID).");
            // Falls brokeredUserId fehlt/leer ist: Nichts tun.
            // context.getBrokerUserId() enthält bereits die vom SAML-Provider gesetzte (zufällige) NameID.
        }
    }

    /**
     * Findet das Attribut mit dem angegebenen Namen in der aktuellen Saml Assertion.
     *
     * @param context aktueller SAML Kontext
     * @param name der Name des Attributs, das gesucht wird
     * @return das gesuchte Attribut oder null
     */
    private AttributeType findAttribute(BrokeredIdentityContext context, String name) {
        AssertionType assertion = (AssertionType) context.getContextData().get(SAMLEndpoint.SAML_ASSERTION);
        for (AttributeStatementType statement : assertion.getAttributeStatements()) {
            for (AttributeStatementType.ASTChoiceType choice : statement.getAttributes()) {
                AttributeType attr = choice.getAttribute();
                if (name.equals(attr.getName()) || name.equals(attr.getFriendlyName())) {
                    return attr;
                }
            }
        }
        return null;
    }
}
