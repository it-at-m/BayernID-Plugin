# KeyCloak (RedHat SSO) BayernID Plugin

Die BayernID (ein Service der AKDB im Auftrag des Freistaats Bayern) bzw. BundID 
(bereitgestellt vom Bundesministerium für Digitales und Staatsmodernisierung) ist
eine digitale Identität, die Bürgerinnen und Bürger im Zusammenhang mit der Abwicklung digitaler
Verwaltungsdienste einsetzen können. Sie dient dazu, sich online eindeutig
zu identifizieren und bietet verschiedene Authentifizierungsstufen
(schwache Authentifizierung per Benutzername und Passwort, substantielle
Authentifizierung per ELSTER oder hohe Authentifizierung per eID/nPA).

Die direkte Integration der BayernID / BundID mit Fachverfahren erfolgt per SAML2-Protokoll.

Da eine direkte Integration jedes Fachverfahrens-Servers aufgrund
auszutauschender Zertifikate und Metadaten aufwändig ist, setzt die
Landeshauptstadt München einen zwischengeschalteten Identity Provider
(IDP) auf Basis der Software Keycloak (bzw. deren größtenteils baugleiches
kommerzielles Pendant RedHat Single Sign On (RH-SSO)) ein. Dies hat den
Vorteil, dass die angeschlossenen Fachverfahren nur mit diesem IDP eine
Vertrauensstellung aufbauen müssen und dafür sowohl OpenID-Connect (OIDC)
als auch SAML2 als Protokoll zur Verfügung stehen.

Die Anbindung eines Keycloak/RH-SSO an die BayernID/BundID unterstützt aber nicht
alle benötigten Funktionalitäten der BayernID. Bspw. muss für die Anforderung
eines Mindest-Vertrauensniveaus oder auch für die Anforderung expliziter Attribute
in den SAML2-Request eingegriffen werden, was nativ
vom Keycloak/RH-SSO nicht unterstützt wird.

Aus diesem Grund wurde von der Landeshauptstadt München ein Plugin
entwickelt, das eine entsprechende Erweiterung des Keycloak/RH-SSO
bewirkt. Damit ist es für die angebundenen Fachverfahren möglich, ein
gewünschtes Mindest-Vertrauensniveau oder spezifische Attribute (per Scopes oder einzeln)
explizit anzufordern - sowohl per OIDC als auch per SAML2. Außerdem
ermöglicht das Plugin, dass Userdatensätze eindeutig auf den gleichen
Datensatz im Keycloak/RH-SSO zu mappen, so dass dort bspw. auch eine
Autorisierung angewendet werden kann.

**Hinweis:** Ein Klon dieses Repos findet sich auch bei [OpenCode](https://gitlab.opencode.de/landeshauptstadt-muenchen/bayernid-plugin).
Auch wenn beide Repos regelmäßig synchronisiert werden, ist das Github-Repo führend und sollte für Issues / MRs
verwendet werden.

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

Siehe Dokument `KeyCloak-Konfiguration.pdf`

## Testen

Je nachdem, ob das Plugin per SAML2-Client oder OIDC-Client aufgerufen wird, verhält es sich unterschiedlich 
(z.B. hinsichtlich des angeforderten Authentifizierungsniveaus), so dass beides getestet werden sollte.

### OIDC

Für einen OIDC-Test kann grundsätzlich die Account-Anwendung verwendet werden. Diese ist folgendermaßen zu erreichen:
`https://<basis-URL-inkl-Realm>/account` 


Falls das Bürgerkonto als Default-Provider konfiguriert ist (unter Authentication->Identity Provider Redirector->Actions->Config->Default eintragen, 
z.B. "buergerkonto"), kommt man sofort zur Login-Maske des Bürgerkontos bei der AKDB, an sonsten kommt die Login-Maske des Keycloak/RH-SSO,
in der man dann "buergerkonto", "Bayern-ID" o.ä. anklickt (nicht direkt einloggen).

Bei OIDC kann man per Anforderung von Scopes ein höheres Authentifizierungsniveau anfordern. Das funktioniert über die Angabe der folgenden Scopes
- **level1**: mindestes Username+Passwort
- **level2**: [derzeit nicht belegt]
- **level3**: mindestens Authega
- **level4**: nPA mit eID

Außerdem gibt es noch die folgenden Scopes in Verbindung mit dem ELSTER Unternehmenskonto (NEZO):
- **any** oder (keine explizite Angabe): sowohl Bürgerkonto als auch Unternehmenskonto sind für den Login möglich (falls BayernID und NEZO als Broker konfiguriert sind, erscheinen beide IDPs aus Auswahlmöglichkeit im Keycloak Loginscreen)
- **legalEntity**: nur Unternehmenskonto ist für den Login möglich 
- **person**: nur Login mit Bürgerkonto ist möglich

Die Scopes zum Authentifizierungsniveau und zum Unternehmenskonto sind kombinierbar.

Zudem gibt es noch den Scope **debug**. Wenn dieser zusätzlich gesetzt wird, werden SAML Request und Response im Logfile ausgegeben.

Außerdem können natürlich die weiteren definierten und als optional im Client konfigurierten Client Scopes angefordert werden, um die darin enthaltenen Attribute zu erhalten (z.B. `profile`, `email` oder `birthdate`).

Das BayernID-Plugin ist derzeit so konfiguriert, dass nur die BayernID-eigenen Authentifizierungsmethoden eID, Authega, ELSTER und Benutzername+Passwort aktiv sind, d.h. die anderen Methoden wie temporär Login und Servicekonten (anderer Bundesländer / NutzerkontoBund oder anderer europäischer Staaten) sind standardmäßig deaktiviert. Der Grund ist, dass die bei den Servicekonten übertragenen Attribute sehr unterschiedlich sind, so dass man u.U. Schwierigkeiten mit der sauberen Verarbeitung im nachgelagerten Fachverfahren bekommt. Man kann aber explizit die anderen Authentifizierungsmethoden erzwingen über den Scope **otherOptions**.

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
client_id=eogov&client_secret=<secret>&grant_type=authorization_code&code=<code>
```

### SAML2

Per SAML2 angebundene Clients müssen die Konfiguration, die bei OIDC per Scopes gesteuert wird, stattdessen im SAML-Request mitschicken. Bspw. wie folgt:

```xml
<samlp:AuthnRequest xmlns:samlp="urn:oasis:names:tc:SAML:2.0:protocol" ID="Login" Version="2.0" IssueInstant="2012-01-01T00:00:00Z">
    <samlp:Extensions>
        <bayernid:RequestedAttributeSet xmlns:bayernid="urn:bayernid:1.0">person</bayernid:RequestedAttributeSet>
        <akdb:AuthenticationRequest xmlns:akdb="https://www.akdb.de/request/2018/09">
            <akdb:AuthnMethods>
                <akdb:eID>
                    <akdb:Enabled>true</akdb:Enabled>
                </akdb:eID>
                <akdb:Elster>
                    <akdb:Enabled>true</akdb:Enabled>
                </akdb:Elster>
            </akdb:AuthnMethods>
            <akdb:RequestedAttributes>
                <!--gender-->
                <akdb:RequestedAttribute Name="urn:oid:1.3.6.1.4.1.33592.1.3.5"/>
                <!--givenName / Vorname-->
                <akdb:RequestedAttribute Name="urn:oid:2.5.4.42" RequiredAttribute="false"/>
                <!--surname / Nachname-->
                <akdb:RequestedAttribute Name="urn:oid:2.5.4.4" RequiredAttribute="true"/>
                <!--bPK2-->
                <akdb:RequestedAttribute Name="urn:oid:1.3.6.1.4.1.25484.494450.3" RequiredAttribute="true"/>
                <!--authlevel-->
                <akdb:RequestedAttribute Name="urn:oid:1.2.40.0.10.2.1.1.261.94" RequiredAttribute="true"/>
                <!--birthdate / Geburtsdatum - Scope birthdate -->
                <akdb:RequestedAttribute Name="urn:oid:1.2.40.0.10.2.1.1.55" RequiredAttribute="true"/>
            </akdb:RequestedAttributes>
        </akdb:AuthenticationRequest>
        <lhm:otherOptions xmlns:lhm="urn:lhm:1.0">true</lhm:otherOptions>
        <lhm:idpHint xmlns:lhm="urn:lhm:1.0">buergerkonto</lhm:idpHint>
        <lhm:RequestedScopes xmlns:lhm="https://www.muenchen.de/request/2022/03">
            <lhm:Scope>email</lhm:Scope>
            <lhm:Scope>debug</lhm:Scope>
        </lhm:RequestedScopes>
    </samlp:Extensions>
    <samlp:RequestedAuthnContext Comparison="minimum" xmlns:saml="urn:oasis:names:tc:SAML:2.0:assertion">
        <saml:AuthnContextClassRef>STORK-QAA-Level-3</saml:AuthnContextClassRef>
    </samlp:RequestedAuthnContext>
</samlp:AuthnRequest>
```

Dabei gibt es folgende Möglichkeiten:
* **bayernid:RequestedAttributeSet** (im Bereich Extensions): `any`, `legalEntity` oder `person` (s. OIDC für Beschreibung)
* **lhm:otherOptions** (im Bereich Extensions): Explizites Hinzukonfigurieren der weiteren Authentifizierungsmethoden bei der BayernID wie FINK (aber keine temporären Logins)
* **lhm:idpHint** (im Bereich Extensions): Durch Angabe eines Broker-Alias kann die Keylcoak-Loginseite übersprungen und direkt zum entsprechenden externen IDP (z.B. BayernID oder NEZO) weitergeleitet werden.
* **lhm:RequestedScopes** (im Bereich Extensions): Angabe der explizit gewünschten Scopes, die bei der BayernID angefordert werden sollen. 
  Falls keine explizite Angabe erfolgt, werden alle im zugehörigen SAML-Client definierten Client Scopes bei der BayernID angefordert.\
  In der Schlussfolgerung werden auch nur die Attribute im SAML-Response geliefert, die in den angeforderten Scopes definiert sind (weitere Attribute wurden ja gar nicht von der BayernID eingeholt).
* **akdb:AuthnMethods** (im Bereich Extension im Unterbereich `akdb:AuthenticationRequest`): Hier können explizit die Anmeldeoptionen der BayernID angegeben werden, die bei diesem Request aktiv sein sollen,  
  bspw. `Benutzername`, `eID` oder `Elster`, aber da die Anfrage direkt an die BayernID durchgereicht wird, sind auch alle weiteren derzeit oder künftig verfügbaren Anmeldeoptionen möglich.
* **akdb:RequestedAttributes**  (im Bereich Extension im Unterbereich `akdb:AuthenticationRequest`): Aus Gründen der Schnittstellenkompatibilität zur BayernID ist es über diesen Parameter zusätzlich zu den o.g. `RequestedScopes`
  möglich, direkt einzelne Attribute von der BayernID anzufordern. Es wird aber dringend empfohlen, stattdessen wenn möglich die `RequestedScopes` zu nutzen oder an sonsten alle Attribute anzufordern, die einen Scope vollständig abdecken,
  da es sonst zu ungewollten Effekten im SSO-bedingten Wechsel zwischen verschiedenen Anwendungen kommt.
* **RequestedAuthnContext** (separater Bereich im SAML-Request): Hierüber lässt sich einschränken, mit welcher Authentifizierungsstufe sich ein\*eine User\*in an der BayernID anmelden soll. 
  Es stehen hier derzeit die Optionen `STORK-QAA-Level-1`, `STORK-QAA-Level-3` und `STORK-QAA-Level-4` zur Verfügung.  In diesem Beispiel sollte dann bei der AKDB dann nur noch die Bürgerkonto-Authentifizierungsmethoden mit starkem TrustLevel zur Verfügung stehen


Diese Möglichkeiten kann man bspw. bei Shibboleth erreichen, indem man in der Datei `shibboleth2.xml` einen `SessionInitiator` mit entsprechendem Inhalt definiert.

## Built with
    Java 21

## Contributing

Contributions are what make the open source community such an amazing place to learn, inspire, and create. Any contributions you make are greatly appreciated.

If you have a suggestion that would make this better, please open an issue with the tag "enhancement", fork the repo and create a pull request. You can also simply open an issue with the tag "enhancement". Don't forget to give the project a star! Thanks again!

    Open an issue with the tag "enhancement"
    Fork the Project
    Create your Feature Branch (git checkout -b feature/AmazingFeature)
    Commit your Changes (git commit -m 'Add some AmazingFeature')
    Push to the Branch (git push origin feature/AmazingFeature)
    Open a Pull Request

### Coding Conventions

We use the [itm-java-codeformat](https://github.com/it-at-m/itm-java-codeformat) project to apply code formatting conventions.
To add those conventions to your favorite IDE, please have a look at the [README of itm-java-codeformat](https://github.com/it-at-m/itm-java-codeformat#verwendung).

## License

Distributed under the MIT License. See LICENSE for more information.
## Contact

it@m - opensource@muenchen.de