package de.muenchen.keycloak.custom.broker.saml;

import org.keycloak.saml.SamlProtocolExtensionsAwareBuilder;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.StaxUtil;

import javax.xml.stream.XMLStreamWriter;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Derived from org.keycloak.saml.processing.core.util.KeycloakKeySamlExtensionGenerator
 * @author rowe42
 */
public class AllowedMethodsExtensionGenerator implements SamlProtocolExtensionsAwareBuilder.NodeGenerator {

    public static final String NS_URI = "https://www.akdb.de/request/2018/09";

    public static final String NS_PREFIX = "akdb";

    public static final String KC_KEY_INFO_ELEMENT_NAME = "AuthenticationRequest";

    private final Map<String, String> attributes;
    private final List<String> methods;

    public AllowedMethodsExtensionGenerator(Map<String, String> attributes, List<String> methods) {
        this.attributes = attributes;
        this.methods = methods;
    }

    @Override
    public void write(XMLStreamWriter writer) throws ProcessingException {
        StaxUtil.writeStartElement(writer, NS_PREFIX, KC_KEY_INFO_ELEMENT_NAME, NS_URI); //akdb:AuthenticationRequest
        StaxUtil.writeAttribute(writer, "Version", "2");

        StaxUtil.writeNameSpace(writer, NS_PREFIX, NS_URI);
        if (this.attributes != null) {
            for (String key : attributes.keySet()) {
                StaxUtil.writeAttribute(writer, key, attributes.get(key));
            }

        }

        //Abschnitt AuthnMethods
        StaxUtil.writeStartElement(writer, NS_PREFIX, "AuthnMethods", NS_URI);
        for (String method : methods) {
            StaxUtil.writeStartElement(writer, NS_PREFIX, method, NS_URI); //Anfang method (z.B. akdb:eID)
            StaxUtil.writeStartElement(writer, NS_PREFIX, "Enabled", NS_URI); //Anfang Enabled
            StaxUtil.writeCharacters(writer, "true"); //Text, immer true
            StaxUtil.writeEndElement(writer); //Ende Enabled (z.B. akdb:eID)
            StaxUtil.writeEndElement(writer); //Ende method (z.B. akdb:eID)
        }
        StaxUtil.writeEndElement(writer); //AuthnMethods

        //Abschnitt RequestedAttributes
        StaxUtil.writeStartElement(writer, NS_PREFIX, "RequestedAttributes", NS_URI);

        //bPK
        StaxUtil.writeStartElement(writer, NS_PREFIX, "RequestedAttribute", NS_URI);
        StaxUtil.writeAttribute(writer, "Name", "urn:oid:1.2.40.0.10.2.1.1.149");
        StaxUtil.writeAttribute(writer, "RequiredAttribute", "true");
        StaxUtil.writeEndElement(writer);

        //legacyPostkorbHandle
        StaxUtil.writeStartElement(writer, NS_PREFIX, "RequestedAttribute", NS_URI);
        StaxUtil.writeAttribute(writer, "Name", "urn:oid:2.5.4.18");
        StaxUtil.writeAttribute(writer, "RequiredAttribute", "true");
        StaxUtil.writeEndElement(writer);

        //givenName
        StaxUtil.writeStartElement(writer, NS_PREFIX, "RequestedAttribute", NS_URI);
        StaxUtil.writeAttribute(writer, "Name", "urn:oid:2.5.4.42");
        StaxUtil.writeAttribute(writer, "RequiredAttribute", "true");
        StaxUtil.writeEndElement(writer);

        //surname
        StaxUtil.writeStartElement(writer, NS_PREFIX, "RequestedAttribute", NS_URI);
        StaxUtil.writeAttribute(writer, "Name", "urn:oid:2.5.4.4");
        StaxUtil.writeAttribute(writer, "RequiredAttribute", "true");
        StaxUtil.writeEndElement(writer);

        //mail
        StaxUtil.writeStartElement(writer, NS_PREFIX, "RequestedAttribute", NS_URI);
        StaxUtil.writeAttribute(writer, "Name", "urn:oid:0.9.2342.19200300.100.1.3");
        StaxUtil.writeAttribute(writer, "RequiredAttribute", "false");
        StaxUtil.writeEndElement(writer);

        StaxUtil.writeEndElement(writer); //RequestedAttributes

        StaxUtil.writeEndElement(writer); //AuthenticationRequest
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
