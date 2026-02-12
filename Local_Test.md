# Local Test
This description shows a local test setup of the plugin

## Podman

Init:
```
cd stack

podman build . -t keycloak-bayernid-mock

podman run --name keycloak-bayernid-mock -p 8080:8080 keycloak-bayernid-mock start-dev
```

Extract Realm:
```
podman exec -it keycloak-bayernid-mock sh -c "cp -rp /opt/keycloak/data/h2 /tmp ;/opt/keycloak/bin/kc.sh export --file /tmp/realm-export.json --db dev-file --db-url 'jdbc:h2:file:/tmp/h2/keycloakdb;NON_KEYWORDS=VALUE'"

podman cp keycloak-bayernid-mock:/tmp/realm-export.json .
```

## Description

Setup is as follows:
- Client A (OIDC):\
  BayernID: profile, person, NEZO: organization, companyaddress, legalEntity

- Client B (OIDC):\
  BayernID: profile, person, no NEZO

- Client C (OIDC):\
  BayernID: profile, birthdate, person, NEZO: organization, companyaddress, legalEntity

- testclient (SAML):\
  BayernID: profile_saml, person_saml, NEZO: organization_saml, companyaddress_saml, legalEntity_saml

Testuser:
- Master: admin / admin
- BayernID: roland_bayernid / Test1234
- NEZO: ek-12345 / Test1234

URLs for Clients:
- [Client A](http://localhost:8080/auth/realms/public/protocol/openid-connect/auth?client_id=clienta&redirect_uri=https%3A%2F%2Fwww.example.org&state=f30d2f0b-13dc-4b51-88e9-bedf7e82346e&response_mode=fragment&response_type=code&scope=openid+debug&nonce=8d300768-03d6-45c1-8cd4-a93ef20023eb&code_challenge=yYxqnM7BekUKndzMhknUJ8Ku8_KvF_eAhs1QCjeI0Sc&code_challenge_method=S256)
- [Client B](http://localhost:8080/auth/realms/public/protocol/openid-connect/auth?client_id=clientb&redirect_uri=https%3A%2F%2Fwww.example.org&state=f30d2f0b-13dc-4b51-88e9-bedf7e82346e&response_mode=fragment&response_type=code&scope=openid+debug&nonce=8d300768-03d6-45c1-8cd4-a93ef20023eb&code_challenge=yYxqnM7BekUKndzMhknUJ8Ku8_KvF_eAhs1QCjeI0Sc&code_challenge_method=S256)
- [Client C](http://localhost:8080/auth/realms/public/protocol/openid-connect/auth?client_id=clientc&redirect_uri=https%3A%2F%2Fwww.example.org&state=f30d2f0b-13dc-4b51-88e9-bedf7e82346e&response_mode=fragment&response_type=code&scope=openid+debug&nonce=8d300768-03d6-45c1-8cd4-a93ef20023eb&code_challenge=yYxqnM7BekUKndzMhknUJ8Ku8_KvF_eAhs1QCjeI0Sc&code_challenge_method=S256)

Test Order:
- Step 1
  - Login to **Client B** with **BayernID**
  - User gets scopes `profile`, `person` 
  - Login to **Client A** - wants `profile`, `person`, `organisation`, `companyaddress`
  - Authenticator restricts: `BayernID->profile,birthdate,person`, Client A wants `profile,person` out of this -> fit
  - Login success (now at Client A) - because both clients support BayernID and requested scopes match
- Step 2
  - Change to **Client C**
  - User still has scopes `profile`, `person`
  - Client C wants `profile`, `birthdate`, `organisation`, `companyaddress`
  - Authenticator restricts: `BayernID->profile,birthdate`, Client C wants `profile,birthdate` out of this -> NO fit
  - Force-Logout
- Step 3
  - Login to **Client C** with **Elster-UK**
  - User gets scopes `organisation`, `companyaddress`
  - Login to **Client B** - wants `profile`
  - Authenticator restricts: `UK->organisation,companyaddress`, but Client B wants nothing of this -> trivial fit (but 
  other authenticator kicks in because UK not allowed on Client B)
  - Force-Logout
- Step 4
  - Login to **Client A** with **Elster-UK**
  - User gets scopes `organisation`, `companyaddress`
  - Login to SAML-Client **testclient** using file `test-aufruf LOKAL.html` in Browser
  - In the SAML-Call there is the RequestedAttributeSet requested as `person`
  - Although **testclient** allows both BayernID and Elster-UK, a force-logout happens because the SAML-Request explicitly
    requests `person`, which is not connected to Elster-UK (see file `bayernIdConfig.json`)