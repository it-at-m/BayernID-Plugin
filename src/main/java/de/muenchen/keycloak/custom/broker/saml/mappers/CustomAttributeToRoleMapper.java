package de.muenchen.keycloak.custom.broker.saml.mappers;

import org.jboss.logging.Logger;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.ConfigConstants;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.saml.mappers.AttributeToRoleMapper;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;

/**
 * Erweitert den AttributeToRoleMapper um die Fähigkeit, bei Client-Roles den aktuellen Client dynamisch
 * als Variable zu setzen.
 * @author rowe42
 */
public class CustomAttributeToRoleMapper extends AttributeToRoleMapper {

    private static final Logger LOGGER = Logger.getLogger(CustomAttributeToRoleMapper.class);

    @Override
    public String getDisplayType() {
        return "Custom SAML Attribute to Role";
    }

    public static final String CLIENT_ID_PLACEHOLDER = "${clientId}";

    @Override
    public void importNewUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        String roleName = retrieveRoleNameExtended(session, mapperModel);

        //LHM: aus Super-Klasse übernommen - ANFANG
        if (isAttributePresent(mapperModel, context)) {
            RoleModel role = KeycloakModelUtils.getRoleFromString(realm, roleName);
            if (role == null) { //hier Semantik verändert: keine Exception, sondern nur Warning
                LOGGER.warn("Unable to find role: " + roleName);
            } else {
                user.grantRole(role);
            }
        }
        //LHM: aus Super-Klasse übernommen - ENDE
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        String roleName = retrieveRoleNameExtended(session, mapperModel);

        //LHM: aus Super-Klasse übernommen - ANFANG
        RoleModel role = KeycloakModelUtils.getRoleFromString(realm, roleName);
        if (role == null) { //hier Semantik verändert: keine Exception, sondern nur Warning
            LOGGER.warn("Unable to find role: " + roleName);
        } else {
            if (!this.isAttributePresent(mapperModel, context)) {
                user.deleteRoleMapping(role);
            } else {
                user.grantRole(role);
            }
        }
        //LHM: aus Super-Klasse übernommen - ENDE
    }

    //LHM Ergänzung ANFANG
    private String retrieveRoleNameExtended(KeycloakSession session, IdentityProviderMapperModel mapperModel) {
        String roleName = mapperModel.getConfig().get(ConfigConstants.ROLE);

        if (roleName != null && roleName.trim().startsWith(CLIENT_ID_PLACEHOLDER)) {
            ClientModel client = session.getContext().getAuthenticationSession().getClient();
            roleName = roleName.replace(CLIENT_ID_PLACEHOLDER, client.getClientId());
        }

        return roleName;
    }

    public CustomAttributeToRoleMapper() {
        getConfigProperties().get(3).setHelpText("Role to grant to user.  Click 'Select Role' button to browse roles, or just type it in the textbox.  To reference an application role the syntax is appname.approle, i.e. myapp.myrole. CUSTOM: you can also use ${clientId} for appname to pick the current app/client.");
    }

    //LHM Ergänzung ENDE

}
