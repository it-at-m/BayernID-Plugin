package de.muenchen.keycloak.custom.broker.saml;

import de.muenchen.keycloak.custom.broker.saml.domain.RequestedAttribute;
import java.io.ByteArrayOutputStream;
import java.util.Set;
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
public class AuthenticationRequestExtensionGenerator implements SamlProtocolExtensionsAwareBuilder.NodeGenerator {

    public static final String NS_URI = "https://www.akdb.de/request/2018/09";

    public static final String CLASSIC_UI_URI = "https://www.akdb.de/request/2018/09/classic-ui/v1";

    public static final String NS_PREFIX = "akdb";

    public static final String CLASSIC_UI_PREFIX = "classic-ui";

    public static final String KC_KEY_INFO_ELEMENT_NAME = "AuthenticationRequest";

    private final Set<RequestedAttribute> requestedAttributes;
    private final Set<String> methods;

    public AuthenticationRequestExtensionGenerator(Set<String> methods, Set<RequestedAttribute> requestedAttributes) {
        this.requestedAttributes = requestedAttributes;
        this.methods = methods;
    }

    @Override
    public void write(XMLStreamWriter writer) throws ProcessingException {
        StaxUtil.writeStartElement(writer, NS_PREFIX, KC_KEY_INFO_ELEMENT_NAME, NS_URI); //akdb:AuthenticationRequest
        StaxUtil.writeAttribute(writer, "Version", "2");

        StaxUtil.writeNameSpace(writer, NS_PREFIX, NS_URI);

        //Abschnitt AuthnMethods
        if (methods != null && !methods.isEmpty()) {
            StaxUtil.writeStartElement(writer, NS_PREFIX, "AuthnMethods", NS_URI);
            for (String method : methods) {
                StaxUtil.writeStartElement(writer, NS_PREFIX, method, NS_URI); //Anfang method (z.B. akdb:eID)
                StaxUtil.writeStartElement(writer, NS_PREFIX, "Enabled", NS_URI); //Anfang Enabled
                StaxUtil.writeCharacters(writer, "true"); //Text, immer true
                StaxUtil.writeEndElement(writer); //Ende Enabled (z.B. akdb:eID)
                StaxUtil.writeEndElement(writer); //Ende method (z.B. akdb:eID)
            }
            StaxUtil.writeEndElement(writer); //AuthnMethods
        }

        //Abschnitt RequestedAttributes
        StaxUtil.writeStartElement(writer, NS_PREFIX, "RequestedAttributes", NS_URI);

        for (RequestedAttribute requestedAttribute : requestedAttributes) {
            StaxUtil.writeStartElement(writer, NS_PREFIX, "RequestedAttribute", NS_URI);
            StaxUtil.writeAttribute(writer, "Name", requestedAttribute.getName());
            if (requestedAttribute.getRequiredAttribute() != null) {
                StaxUtil.writeAttribute(writer, "RequiredAttribute",
                        requestedAttribute.getRequiredAttribute());
            }
            StaxUtil.writeEndElement(writer);
        }

        StaxUtil.writeEndElement(writer); //RequestedAttributes

        StaxUtil.writeStartElement(writer, NS_PREFIX, "DisplayInformation", NS_URI);

        StaxUtil.writeNameSpace(writer, CLASSIC_UI_PREFIX, CLASSIC_UI_URI);

        StaxUtil.writeStartElement(writer, CLASSIC_UI_PREFIX, "Version", CLASSIC_UI_URI);

        StaxUtil.writeStartElement(writer, CLASSIC_UI_PREFIX, "OrganizationDisplayName", CLASSIC_UI_URI);
        StaxUtil.writeCharacters(writer, "Landeshauptstadt M\u00FCnchen");
        StaxUtil.writeEndElement(writer); //OrganizationDisplayName

        StaxUtil.writeStartElement(writer, CLASSIC_UI_PREFIX, "Lang", CLASSIC_UI_URI);
        StaxUtil.writeCharacters(writer, "de");
        StaxUtil.writeEndElement(writer); //Lang

        StaxUtil.writeEndElement(writer); //Version
        StaxUtil.writeEndElement(writer); //DisplayInformation

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
