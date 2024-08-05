package de.muenchen.keycloak.custom.broker.saml;

import de.muenchen.keycloak.custom.IdentityProviderHelper;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;
import org.jboss.logging.Logger;
import org.keycloak.dom.saml.v2.assertion.*;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.dom.saml.v2.protocol.StatusResponseType;

public class BeforeSendingResponse {

    protected static final Logger logger = Logger.getLogger(BeforeSendingResponse.class);

    /**
     * List das authlevel aus dem AttributeStatement und schreibt es in den AuthnContext.
     *
     * @param statusResponse StatusResponseType
     */
    public void process(StatusResponseType statusResponse) {

        if (statusResponse instanceof ResponseType) {
            ResponseType responseType = (ResponseType) statusResponse;

            List<ResponseType.RTChoiceType> choiceTypes = responseType.getAssertions();
            if (choiceTypes != null) {

                for (ResponseType.RTChoiceType choiceType : choiceTypes) {
                    AssertionType assertion = choiceType.getAssertion();

                    if (assertion != null) {
                        Set<StatementAbstractType> statements = assertion.getStatements();

                        if (statements != null) {
                            String authlevel = getAuthlevelFromAttributeStatement(statements);

                            if (authlevel != null) {
                                String eidasLevel = IdentityProviderHelper.mapAuthLevelToEidas(authlevel);

                                if (eidasLevel != null) {
                                    setAuthlevelToAuthnStatement(eidasLevel, statements);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Holt das Authlevel aus dem Attribute Statement.
     *
     * @param statements Die Statements des SAML-Response.
     * @return Das Authlevel als String.
     */
    private String getAuthlevelFromAttributeStatement(Set<StatementAbstractType> statements) {
        for (StatementAbstractType statement : statements) {
            if (statement instanceof AttributeStatementType) {
                AttributeStatementType attributeStatement = (AttributeStatementType) statement;
                for (AttributeStatementType.ASTChoiceType act : attributeStatement.getAttributes()) {
                    if (act != null && act.getAttribute() != null && act.getAttribute().getName().equalsIgnoreCase("authlevel")) {
                        if (act.getAttribute().getAttributeValue() != null) {
                            if (act.getAttribute().getAttributeValue().size() != 1) {
                                logger.warn("Found Attribute " + act.getAttribute().getName() + " with " + act.getAttribute().getAttributeValue().size()
                                        + " Values! Should not happen!");
                            }
                            if (act.getAttribute().getAttributeValue().iterator().next() instanceof String) {
                                return (String) act.getAttribute().getAttributeValue().iterator().next();
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Setzt das Authlevel in den AuthnContext.
     *
     * @param authlevel Das authlevel, das gesetzt werden soll
     * @param statements Die Statements im SAML-Response.
     */
    private void setAuthlevelToAuthnStatement(String authlevel, Set<StatementAbstractType> statements) {
        for (StatementAbstractType statement : statements) {

            if (statement instanceof AuthnStatementType) {
                AuthnStatementType authnStatement = (AuthnStatementType) statement;
                AuthnContextType authnContext = authnStatement.getAuthnContext();
                if (authnContext != null && authnContext.getSequence() != null) {
                    try {
                        AuthnContextClassRefType acrt = new AuthnContextClassRefType(new URI(authlevel));
                        authnContext.getSequence().setClassRef(acrt);
                    } catch (URISyntaxException e) {
                        logger.warn("Not able to set authlevel - URISyntaxException " + authlevel);
                    }
                }
            }
        }
    }

}
