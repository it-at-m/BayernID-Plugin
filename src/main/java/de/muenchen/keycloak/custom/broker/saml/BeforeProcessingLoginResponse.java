package de.muenchen.keycloak.custom.broker.saml;

import org.jboss.logging.Logger;
import org.keycloak.dom.saml.v2.protocol.StatusResponseType;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.processing.api.saml.v2.response.SAML2Response;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.w3c.dom.Document;

import javax.xml.transform.TransformerException;
import java.util.logging.Level;

public class BeforeProcessingLoginResponse {

    protected static final Logger logger = Logger.getLogger(BeforeProcessingLoginResponse.class);

    // zu SCHRITT 3
    // ------------
    /**
     * Wird aus beforeProcessingLoginResponse (Schritt 3) heraus aufgerufen.
     * Prüft, ob die im Request eingetragenen Extension-Attribute nun im
     * Response auch wieder enthalten sind. Außerdem wird der Response im Log
     * ausgegeben.
     *
     * @param authSession
     * @param responseType
     */
    public void authenticationFinished(AuthenticationSessionModel authSession, StatusResponseType responseType) {
        logger.info("Auth finished");
        //Print Response
        //--------------
        if (logger.isDebugEnabled() || PreprocessorHelper.hasDebugScope(authSession)) {
            printResponse(responseType);
        }

        //Check InResponseTo
        //------------------
        String id = authSession.getClientNote("Generated_ID");

        if (id != null && !id.equals(responseType.getInResponseTo())) {
            logger.error("Found violation in ID");
        }

    }

    private void printResponse(StatusResponseType responseType) {
        try {
            SAML2Response saml2Response = new SAML2Response();
            Document doc = saml2Response.convert(responseType);
            logger.info("\n\n\n  Response: \n ------- \n " + PreprocessorHelper.printDocument(doc));

        } catch (ConfigurationException | ProcessingException | TransformerException | ParsingException ex) {
            java.util.logging.Logger.getLogger(CustomSamlAuthenticationPreprocessor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }



}
