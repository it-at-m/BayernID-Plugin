package de.muenchen.keycloak.custom.config.domain;

/**
 * Display-Informationen für SAML Authentication Request
 */
public class DisplayInformation {

    private String organizationDisplayName;

    public String getOrganizationDisplayName() {
        return organizationDisplayName;
    }

    public void setOrganizationDisplayName(String organizationDisplayName) {
        this.organizationDisplayName = organizationDisplayName;
    }

    @Override
    public String toString() {
        return "DisplayInformation{" +
                "organizationDisplayName='" + organizationDisplayName + '\'' +
                '}';
    }
}
