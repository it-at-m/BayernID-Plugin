package de.muenchen.keycloak.custom.broker.saml;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLStreamWriter;
import org.keycloak.saml.SamlProtocolExtensionsAwareBuilder;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.StaxUtil;

/**
 * Derived from org.keycloak.saml.processing.core.util.KeycloakKeySamlExtensionGenerator
 *
 * @author rowe42
 */
public class AccounttypeExtensionGenerator implements SamlProtocolExtensionsAwareBuilder.NodeGenerator {

    public static final String NS_URI = "https://www.akdb.de/request/2018/09";

    public static final String NS_PREFIX = "akdb";

    public static final String KC_KEY_INFO_ELEMENT_NAME = "RequestedAttributeSet";

    private final Map<String, String> attributes;
    private final String content;

    public AccounttypeExtensionGenerator(Map attributes, String content) {
        this.attributes = attributes;
        this.content = content;
    }

    @Override
    public void write(XMLStreamWriter writer) throws ProcessingException {
        StaxUtil.writeStartElement(writer, NS_PREFIX, KC_KEY_INFO_ELEMENT_NAME, NS_URI);
        StaxUtil.writeNameSpace(writer, NS_PREFIX, NS_URI);
        if (this.attributes != null) {
            for (String key : attributes.keySet()) {
                StaxUtil.writeAttribute(writer, key, attributes.get(key));
            }

        }
        StaxUtil.writeCharacters(writer, content);
        StaxUtil.writeEndElement(writer);
        StaxUtil.flush(writer);
    }

    @Override
    public String toString() {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            XMLStreamWriter xmlStreamWriter = StaxUtil.getXMLStreamWriter(bos);
            write(xmlStreamWriter);
            return new String(bos.toByteArray(), GeneralConstants.SAML_CHARSET);
        } catch (ProcessingException ex) {
            Logger.getLogger(AccounttypeExtensionGenerator.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "";
    }

}
