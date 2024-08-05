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

package de.muenchen.keycloak.custom.authentication.authenticators;

import java.util.Arrays;
import java.util.List;
import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

public class RequireEffectiveScopesAuthenticatorFactory implements AuthenticatorFactory {
    public static final String PROVIDER_ID = "require-effective-scopes";
    static final String ATTRIBUTE = "attribute";
    static final String CUSTOM_ERROR = "customError";

    static final RequireEffectiveScopesAuthenticator SINGLETON = new RequireEffectiveScopesAuthenticator();

    @Override
    public Authenticator create(KeycloakSession session) {
        return SINGLETON;
    }

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
        return "require-effective-scopes";
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
        return "CUSTOM Require Effective Scopes";
    }

    @Override
    public String getHelpText() {
        return "CUSTOM - Validates that the given attribute contains only scopes that are effective in the current request.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        ProviderConfigProperty attribute = new ProviderConfigProperty();
        attribute.setType(ProviderConfigProperty.STRING_TYPE);
        attribute.setName(ATTRIBUTE);
        attribute.setLabel("Attribute name");
        attribute.setHelpText("Required attribute name that a user needs to have to proceed with the authentication. ");

        ProviderConfigProperty customError = new ProviderConfigProperty();
        customError.setType(ProviderConfigProperty.STRING_TYPE);
        customError.setName(CUSTOM_ERROR);
        customError.setLabel("Error message to display if failing.");
        customError.setHelpText("If set, the given message is displayed if this check is failing.");

        return Arrays.asList(attribute, customError);
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

}
