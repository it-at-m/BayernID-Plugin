/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.muenchen.keycloak.custom.authentication.authenticators.afterbroker;

import java.util.Arrays;
import java.util.List;
import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

public class RequireAttributeAuthenticatorFactory implements AuthenticatorFactory {
    public static final String PROVIDER_ID = "require-attribute";
    public static final String REG_EXP = "regExp";
    static final String ATTRIBUTE = "attribute";
    static final String ATTRIBUTE_VALUES = "attributeValues";
    static final String CONDITIONAL_SCOPE = "dependOnScope";
    static final String CUSTOM_ERROR = "customError";

    static final RequireAttributeAuthenticator SINGLETON = new RequireAttributeAuthenticator();

    @Override
    public Authenticator create(KeycloakSession session) {
        return SINGLETON;
    }

    //    @Override
    //    public Authenticator createDisplay(KeycloakSession session, String displayType) {
    //        if (displayType == null) return SINGLETON;
    //        if (!OAuth2Constants.DISPLAY_CONSOLE.equalsIgnoreCase(displayType)) return null;
    //        return AttemptedAuthenticator.SINGLETON;  // ignore this authenticator
    //    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getReferenceCategory() {
        return "require-attribute";
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    public static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED, AuthenticationExecutionModel.Requirement.DISABLED
    };

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public String getDisplayType() {
        return "Require Attribute";
    }

    @Override
    public String getHelpText() {
        return "CUSTOM - Validates that the user has a certain attribute.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        ProviderConfigProperty attribute = new ProviderConfigProperty();
        attribute.setType(ProviderConfigProperty.STRING_TYPE);
        attribute.setName(ATTRIBUTE);
        attribute.setLabel("Attribute name");
        attribute.setHelpText("Required attribute name that a user needs to have to proceed with the authentication. ");

        ProviderConfigProperty attributeValue = new ProviderConfigProperty();
        //        attributeValue.setType(ProviderConfigProperty.MULTIVALUED_STRING_TYPE);
        //Multi-Valued-String scheint im Keycloak22 und Keycloak24 nicht zu funktionieren.
        //Stattdessen als Workaround normaler String, die Werte werden mit ## abgetrennt
        attributeValue.setType(ProviderConfigProperty.STRING_TYPE);
        attributeValue.setName(ATTRIBUTE_VALUES);
        attributeValue.setLabel("Attribute value(s)");
        attributeValue.setHelpText("Required attribute value(s) that the attribute needs to have to proceed with the authentication.");

        ProviderConfigProperty regExp = new ProviderConfigProperty();
        regExp.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        regExp.setName(REG_EXP);
        regExp.setLabel("Attribute value(s) contain a regular expression.");
        regExp.setHelpText("If set, the attribute value(s) can contain a regular expression, e.g. [1-3] as range or .+ for at least one character.");

        ProviderConfigProperty conditionalScope = new ProviderConfigProperty();
        conditionalScope.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        conditionalScope.setName(CONDITIONAL_SCOPE);
        conditionalScope.setLabel("Depend on scope with same name as attribute values. Only applicable when NOT using regular expressions.");
        conditionalScope.setHelpText("If set, only those possible attribute value are checked where scopes with the same name exist on the client.");

        ProviderConfigProperty customError = new ProviderConfigProperty();
        customError.setType(ProviderConfigProperty.STRING_TYPE);
        customError.setName(CUSTOM_ERROR);
        customError.setLabel("Error message to display if failing.");
        customError.setHelpText("If set, the given message is displayed if this check is failing.");

        return Arrays.asList(attribute, attributeValue, regExp, conditionalScope, customError);
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

}
