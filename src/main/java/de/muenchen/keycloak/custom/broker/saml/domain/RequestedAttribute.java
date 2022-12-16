package de.muenchen.keycloak.custom.broker.saml.domain;

public class RequestedAttribute {
    private String name;

    private String requiredAttribute;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRequiredAttribute() {
        return requiredAttribute;
    }

    public void setRequiredAttribute(String requiredAttribute) {
        this.requiredAttribute = requiredAttribute;
    }

}
