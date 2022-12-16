package de.muenchen.keycloak.custom.forms.login.freemarker;

import java.util.Arrays;

public enum IDPs {
    //Benennung in den versch. Umgebungen:
    //------------------------------------
    //BayernID:          sso --> buergerkonto; ssotest --> buergerkonto; ssodev --> saml
    //M-Login:           sso --> n/a;          ssotest --> mlogin;       ssodev --> oidc
    //NutzerkontoBund:   sso --> n/a;          ssotest --> n/a;          ssodev --> nutzerkontobund
    //Verimi:            zukünftig falls eingeführt: verimi
    //ELSTER_NEZO:       zukünftig falls eingeführt: nezo

    //       Scope                   Alias                                 Authlevels                                             RequestedAttributeSet
    BayernID(
            "BayernID",        new String[]{"buergerkonto", "saml"}, new String[]{"level1", "level2", "level3", "level4"}, new String[]{"person", "legalEntity", "any"}),
    MLogin(
            "M-Login",         new String[]{"mlogin", "oidc"},       new String[]{"level1", "level2"},                     new String[]{"person", "any"}),
    NutzerkontoBund(
            "NutzerkontoBund", new String[]{"nutzerkontobund"},      new String[]{"level1", "level2", "level3", "level4"}, new String[]{"person", "any"}),
    ELSTER_NEZO(
            "ELSTER_NEZO",     new String[]{"nezo"},                 new String[]{"level1", "level2", "level3"},           new String[]{"legalEntity", "any"}),
    Verimi(
            "Verimi",          new String[]{"verimi"},               new String[]{"level1", "level2", "level3"},            new String[]{"person", "any"});

    public final String scope;
    public final String[] alias;
    public final String[] authlevels;
    public final String[] requestedAttributeSets;

    IDPs(String scope, String[] alias, String[] authlevels, String[] requestedAttributeSets) {
        this.scope = scope;
        this.alias = alias;
        this.authlevels = authlevels;
        this.requestedAttributeSets = requestedAttributeSets;
    }

    public static IDPs findIDPByAlias(String alias) {
        for (IDPs idp : IDPs.values()) {
            if (Arrays.asList(idp.alias).contains(alias)) {
                return idp;
            }
        }
        return null;
    }

}
