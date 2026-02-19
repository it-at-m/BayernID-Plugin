package de.muenchen.keycloak.custom.config.domain;

import java.util.Arrays;

public class IDP {

    private String name;
    private String scope;
    private String[] alias;
    private String[] authlevels;
    private String[] requestedAttributeSets;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String[] getAlias() {
        return alias;
    }

    public void setAlias(String[] alias) {
        this.alias = alias;
    }

    public String[] getAuthlevels() {
        return authlevels;
    }

    public void setAuthlevels(String[] authlevels) {
        this.authlevels = authlevels;
    }

    public String[] getRequestedAttributeSets() {
        return requestedAttributeSets;
    }

    public void setRequestedAttributeSets(String[] requestedAttributeSets) {
        this.requestedAttributeSets = requestedAttributeSets;
    }

    @Override
    public String toString() {
        return "IDP{" +
                "scope='" + scope + '\'' +
                ", alias=" + Arrays.toString(alias) +
                ", authlevels=" + Arrays.toString(authlevels) +
                ", requestedAttributeSets=" + Arrays.toString(requestedAttributeSets) +
                '}';
    }
}
