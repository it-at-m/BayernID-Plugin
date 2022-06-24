package de.muenchen.keycloak.custom.broker.saml.mappers;

import org.jboss.logging.Logger;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.ConfigConstants;
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
        RoleModel role = this.getRole(realm, session, mapperModel); //session ergänzt
        if (role == null) {
            return;
        }
        //LHM: aus Super-Klasse übernommen - ANFANG
        if (this.applies(mapperModel, context)) {
            user.grantRole(role);
        }
        //LHM: aus Super-Klasse übernommen - ENDE
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        RoleModel role = this.getRole(realm, session, mapperModel); //session ergänzt
        if (role == null) {
            return;
        }
        String roleName = retrieveRoleNameExtended(session, mapperModel);
        //LHM: aus Super-Klasse übernommen - ANFANG
        // KEYCLOAK-8730 if a previous mapper has already granted the same role, skip the checks so we don't accidentally remove a valid role.
        if (!context.hasMapperGrantedRole(roleName)) {
            if (this.applies(mapperModel, context)) {
                context.addMapperGrantedRole(roleName);
                user.grantRole(role);
            } else {
                user.deleteRoleMapping(role);
            }
        }
        //LHM: aus Super-Klasse übernommen - ENDE
    }

    private RoleModel getRole(final RealmModel realm, KeycloakSession session, final IdentityProviderMapperModel mapperModel) {
        String roleName = retrieveRoleNameExtended(session, mapperModel);
        //LHM: aus Super-Klasse übernommen - ANFANG
        RoleModel role = KeycloakModelUtils.getRoleFromString(realm, roleName);
        if (role == null) {
            LOGGER.warn("Unable to find role: " + roleName); //hier Semantik verändert: keine Exception, sondern nur Warning
        }
        return role;
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
