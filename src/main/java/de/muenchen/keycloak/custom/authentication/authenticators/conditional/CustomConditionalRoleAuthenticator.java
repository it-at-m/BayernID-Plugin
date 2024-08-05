package de.muenchen.keycloak.custom.authentication.authenticators.conditional;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.conditional.ConditionalRoleAuthenticator;
import org.keycloak.authentication.authenticators.conditional.ConditionalRoleAuthenticatorFactory;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;

public class CustomConditionalRoleAuthenticator extends ConditionalRoleAuthenticator {

    public static final ConditionalRoleAuthenticator SINGLETON = new CustomConditionalRoleAuthenticator();

    private static final Logger logger = Logger.getLogger(CustomConditionalRoleAuthenticator.class);
    public static final String CLIENT_ID_PLACEHOLDER = "${clientId}";

    //ganze Methode aus Superklasse kopiert und markierte Zeilen ergänzt
    @Override
    public boolean matchCondition(AuthenticationFlowContext context) {
        logger.debug("======= In CustomConditionalRoleAuthenticator ======= ");
        UserModel user = context.getUser();
        RealmModel realm = context.getRealm();
        AuthenticatorConfigModel authConfig = context.getAuthenticatorConfig();
        if (user != null && authConfig != null && authConfig.getConfig() != null) {
            String requiredRole = authConfig.getConfig().get(ConditionalRoleAuthenticatorFactory.CONDITIONAL_USER_ROLE);
            //ERGÄNZUNG LHM START
            logger.debug("Original role: " + requiredRole); //ergänzt
            ClientModel client = context.getAuthenticationSession().getClient(); //ergänzt
            requiredRole = resolveRoleName(requiredRole, client); //ergänzt
            logger.debug("After replacement: " + requiredRole); //ergänzt
            //ERGÄNZUNG LHM ENDE
            boolean negateOutput = Boolean.parseBoolean(authConfig.getConfig().get(ConditionalRoleAuthenticatorFactory.CONF_NEGATE));
            RoleModel role = KeycloakModelUtils.getRoleFromString(realm, requiredRole);
            if (role == null) {
                logger.errorv("Invalid role name submitted: {0}", requiredRole);
                return false;
            }

            return negateOutput != user.hasRole(role);
        }
        return false;
    }

    //Diese Methode wurde vollständig ergänzt (weitestgehend kopiert aus RequireRoleAuthenticator)
    protected String resolveRoleName(String roleName, ClientModel client) {

        if (roleName == null) {
            return null;
        }

        roleName = roleName.trim();

        if (roleName.equals("")) {
            return "";
        }

        if (roleName.startsWith(CLIENT_ID_PLACEHOLDER)) {
            roleName = roleName.replace(CLIENT_ID_PLACEHOLDER, client.getClientId());
        }

        return roleName;
    }

}
