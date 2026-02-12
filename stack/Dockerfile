FROM quay.io/keycloak/keycloak:26.4 as builder

# Enable health and metrics support
ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=true

WORKDIR /opt/keycloak
# for demonstration purposes only, please make sure to use proper certificates in production instead
RUN keytool -genkeypair -storepass password -storetype PKCS12 -keyalg RSA -keysize 2048 -dname "CN=server" -alias server -ext "SAN:c=DNS:localhost,IP:127.0.0.1" -keystore conf/server.keystore

# Add the provider JARs file to the providers directory
ADD --chown=keycloak:keycloak bayernid-plugin-2.0.1-SNAPSHOT.jar /opt/keycloak/providers/bayernid-plugin-2.0.1-SNAPSHOT.jar

# Add the custom themes
ADD --chown=keycloak:keycloak themes/ /opt/keycloak/themes

# Add the realm import file
ADD --chown=keycloak:keycloak realm-export.json /opt/keycloak/data/import/realm-export.json

# Build Keycloak with providers
RUN /opt/keycloak/bin/kc.sh build

# Import Realm
RUN /opt/keycloak/bin/kc.sh import --file /opt/keycloak/data/import/realm-export.json

FROM quay.io/keycloak/keycloak:26.4
COPY --from=builder /opt/keycloak/ /opt/keycloak/

ENV KC_HOSTNAME=localhost
ENV  KEYCLOAK_ADMIN=admin 
ENV KEYCLOAK_ADMIN_PASSWORD=admin 
ENV KC_HTTP_RELATIVE_PATH=/auth
ENV KC_FEATURES=scripts
ENV KC_LOG_LEVEL="INFO,de.muenchen.keycloak.custom:debug"

ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]

