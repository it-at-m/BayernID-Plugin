package de.muenchen.keycloak.custom.config.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class BayernIdConfig {

    private List<IDP> idp = new ArrayList<>();
    private List<String> publicRealms = new ArrayList<>();
    private DisplayInformation displayInformation;

    public List<IDP> getIdp() {
        return idp;
    }

    public void setIdp(List<IDP> idp) {
        this.idp = idp;
    }

    public List<String> getPublicRealms() {
        return publicRealms;
    }

    public void setPublicRealms(List<String> publicRealms) {
        this.publicRealms = publicRealms;
    }

    public DisplayInformation getDisplayInformation() {
        return displayInformation;
    }

    public void setDisplayInformation(DisplayInformation displayInformation) {
        this.displayInformation = displayInformation;
    }

    @Override
    public String toString() {
        return "BayernIdConfig{" +
                "\n  idp=[\n    " + idp.stream().map(Objects::toString).collect(Collectors.joining("\n    ")) +
                "\n  ]\n  publicRealms=" + publicRealms +
                "\n  displayInformation=" + displayInformation +
                '}';
    }
}
