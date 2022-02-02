# KeyCloak (RedHat SSO) BayernID Plugin

## Bauen

```
mvn clean install
```

## Deployen

**Entweder:**

* KeyCloak/RH-SSO muss laufen
* Folgendes ausführen

```
mvn wildfly:deploy
```

**Oder:** 

Die Datei `bayernid-plugin-[VERSION].jar` aus dem Verzeichnis `target` (existiert nach dem Build-Prozess) in das KeyCloak-Verzeichnis 
`standalone/deployments` kopieren. Erst danach den KeyCloak/RH-SSO starten.


## Konfigurieren

Siehe Dokument `KeyCloak-Konfiguration.odt`

## Testen

Je nachdem, ob das Plugin per SAML2-Client oder OIDC-Client aufgerufen wird, verhält es sich unterschiedlich 
(z.B. hinsichtlich des angeforderten Authentifizierungsniveaus), so dass beides getestet werden sollte.

### OIDC

Für einen OIDC-Test kann grundsätzlich die Account-Anwendung verwendet werden. Diese ist folgendermaßen zu erreichen:
`https://<basis-URL-inkl-Realm>/account` 


Falls das Bürgerkonto als Default-Provider konfiguriert ist (unter Authentication->Identity Provider Redirector->Actions->Config->Default eintragen, 
z.B. "buergerkonto"), kommt man sofort zur Login-Maske des Bürgerkontos bei der AKDB, an sonsten kommt die Login-Maske des Keycloak/RH-SSO,
in der man dann "buergerkonto", "Bayern-ID" o.ä. anklickt (nicht direkt einloggen).

Es gibt eine Ausnahme wenn es darum geht zu testen, wie man ein höheres Authentifizierungsniveau anfordern kann. Das funktioniert bei OIDC über die Angabe der folgenden Scopes
- **level1**: mindestes Username+Passwort
- **level2**: [derzeit nicht belegt]
- **level3**: mindestens Authega
- **level4**: nPA mit eID

Außerdem gibt es noch die folgenden Scopes in Verbindung mit dem Unternehmenskonto (auch "AKDB Organisationskonto"):
- **any**: sowohl Bürgerkonto als auch Unternehmenskonto sind für den Login möglich
- **legalEntity**: nur Unternehmenskonto ist für den Login möglich
- (keine explizite Angabe): nur Login mit Bürgerkonto ist möglich

Die Scopes zum Authentifizierungsniveau und zum Unternehmenskonto sind kombinierbar.

Zudem gibt es noch den Scope **debug**. Wenn dieser zusätzlich gesetzt wird, werden SAML Request und Response im Logfile ausgegeben.

Das BayernID-Plugin ist derzeit so konfiguriert, dass nur die BayernID-eigenen Authentifizierungsmethoden eID, Authega und Benutzername+Passwort aktiv sind, d.h. die anderen Methoden wie temporär Login und Servicekonten (anderer Bundesländer / NutzerkontoBund oder anderer europäischer Staaten) sind standardmäßig deaktiviert. Der Grund ist, dass die bei den Servicekonten übertragenen Attribute sehr unterschiedlich sind, so dass man u.U. Schwierigkeiten mit der sauberen Verarbeitung im nachgelagerten Fachverfahren bekommt. Man kann aber explizit die anderen Authentifizierungsmethoden erzwingen über den Scope **otherOptions**.

Ein Test der beschriebenen Scopes kann wie folgt vorgenommen werden:

**Schritt 1**: Auth Endpoint aufrufen (im Browser):

```http://<basis-URL-inkl-Realm>/protocol/openid-connect/auth?response_type=code&client_id=eogov&redirect_uri=https://www.muenchen.de&scope=level3+legalEntity+openid```

In diesem Beispiel sollten dann nur noch Authega und nPA zur Auswahl stehen. Login durchführen.

**Schritt 2**: Muenchen.de wird aufgerufen, sieht in etwa so aus:

```https://www.muenchen.de/?session_state=32fca3d2-b398-4be7-9991-43ba9aba8159&code=[code]```

Code entnehmen

**Schritt 3**: Token Endpoint per RESTClient aufrufen (mit Code von oben) und Access-Token besorgen
```
POST
http://<basis-URL-inkl-Realm>/protocol/openid-connect/token
client_id=eogov&client_secret=<secret>&grant_type=authorization_code&redirect_uri=https://www.muenchen.de&code=<code>
```

### SAML2

Per SAML2 angebundene Clients müssen die Konfiguration, die bei OIDC per Scopes gesteuert wird, stattdessen im SAML-Request mitschicken. Bspw. wie folgt:

```xml
<samlp:AuthnRequest xmlns:samlp="urn:oasis:names:tc:SAML:2.0:protocol" ID="Login" Version="2.0" IssueInstant="2012-01-01T00:00:00Z">
    <samlp:Extensions>
        <bayernid:RequestedAttributeSet xmlns:bayernid="urn:bayernid:1.0">individualPerson</bayernid:RequestedAttributeSet>
        <otherOptions>true</otherOptions>
    </samlp:Extensions>
    <samlp:RequestedAuthnContext Comparison="minimum" xmlns:saml="urn:oasis:names:tc:SAML:2.0:assertion">
        <saml:AuthnContextClassRef>STORK-QAA-Level-3</saml:AuthnContextClassRef>
    </samlp:RequestedAuthnContext>
</samlp:AuthnRequest>
```

In diesem Beispiel sollte dann bei der AKDB dann nur noch die Bürgerkonto-Authentifizierungsmethoden mit starkem TrustLevel (nPA, Authega) zur Verfügung stehen, sowie die externen Anbindungen an Servicekonten etc.


Dies kann bspw. bei Shibboleth erreicht werden, indem man in der Datei `shibboleth2.xml` einen `SessionInitiator` mit entsprechendem Inhalt definiert.
