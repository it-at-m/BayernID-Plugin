package de.muenchen.keycloak.custom.forms.login.freemarker;

import java.util.Arrays;

public enum IDPs {
    // spotless:off
    //Benennung in den versch. Umgebungen:
    //------------------------------------
    //BayernID:          sso --> buergerkonto; ssotest --> buergerkonto; ssodev --> saml
    //M-Login:           sso --> n/a;          ssotest --> mlogin;       ssodev --> oidc
    //NutzerkontoBund:   sso --> n/a;          ssotest --> n/a;          ssodev --> nutzerkontobund
    //Verimi:            zukünftig falls eingeführt: verimi
    //ELSTER_NEZO:      sso --> nezo;          ssotest --> nezo;         ssodev --> nezo
    //Google:           sso --> google;        ssotest --> google;       ssodev --> google
    //Intern:           sso --> intern;        ssotest --> intern;       ssodev --> intern

    //       Scope                   Alias                                 Authlevels                                             RequestedAttributeSet
    BayernID(
            "BayernID",        new String[]{"buergerkonto", "saml"}, new String[]{"level1", "level2", "level3", "level4"}, new String[]{"person", "legalEntity", "any"}),
    MLogin(
            "M-Login",         new String[]{"mlogin", "oidc"},       new String[]{"level1"},                               new String[]{"person", "any"}),
    NutzerkontoBund(
            "NutzerkontoBund", new String[]{"nutzerkontobund"},      new String[]{"level1", "level2", "level3", "level4"}, new String[]{"person", "any"}),
    ELSTER_NEZO(
            "ELSTER_NEZO",     new String[]{"nezo"},                 new String[]{"level1", "level2", "level3"},           new String[]{"legalEntity", "any"}),
    Verimi(
            "Verimi",          new String[]{"verimi"},               new String[]{"level1", "level2", "level3"},           new String[]{"person", "any"}),
    Google(
            "Google",          new String[]{"google"},               new String[]{"level1"},                               new String[]{"person", "any"}),
    Intern(
            "Intern",          new String[]{"intern"},               new String[]{"level1", "level2", "level3"},           new String[]{"person", "legalEntity", "any"});
    // spotless:on

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
