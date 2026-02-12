<#assign themeVersion>
    1.0.1
</#assign>

<#macro getProviderLogin alias providerList>
    <#list providerList as provider>
        <#if provider['alias']==alias>
            ${provider["loginUrl"]}
            <#break/>
        </#if>
    </#list>
</#macro>

<!-- BayernID -->
<#assign samlURL>
    <@getProviderLogin alias="saml" providerList=social.providers/>
</#assign>
<#assign buergerKontoURL>
    <@getProviderLogin alias="buergerkonto" providerList=social.providers/>
</#assign>

<!-- BundID -->
<#assign bundidURL>
    <@getProviderLogin alias="bundid" providerList=social.providers/>
</#assign>

<!-- Elster Unternehmenskonto -->
<#assign elsterNezoURL>
    <@getProviderLogin alias="nezo" providerList=social.providers/>
</#assign>

<!-- Intern -->
<#assign internURL>
    <@getProviderLogin alias="intern" providerList=social.providers/>
</#assign>

<head>
    <title>Bürgerservice-Anmeldung</title>
    <link rel="stylesheet" href="${url.resourcesPath}/css/styles.css">
    <link rel="stylesheet" href="${url.resourcesPath}/css/mucbutton.css">
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
</head>
<div class="site">
    <header>
        <a>
            <img class="header-image" src="${url.resourcesPath}/img/muenchende.png">
        </a>
        <div class="spacer"></div>
        <button
                class="m-button m-button--primary"
        >
            <img class="icon" src="${url.resourcesPath}/img/icons/close.svg">
        </button>
    </header>

    <div>
        <h1 class="heading">${msg("heading")}</h1>

        <div class="graphics-container">
            <img
                    class="munich-background"
                    src="${url.resourcesPath}/img/bg/bg-munich-left.svg"
                    alt="munich skyline background"
                    aria-hidden="true"
            />
            <div class="spacer"></div>
            <img
                    class="munich-background"
                    src="${url.resourcesPath}/img/bg/bg-munich-right.svg"
                    alt="munich skyline background"
                    aria-hidden="true"
            />
        </div>

        <div class="card-container">

            <!-- BayernID/BundID Karte für BürgerInnen-Login -->
            <#if (samlURL?has_content || buergerKontoURL?has_content) || bundidURL?has_content>
                <div class="card buerger">
                    <h2>${msg("buerger_heading")}</h2>

                    <!-- Nur BayernID -->
                    <#if (samlURL?has_content || buergerKontoURL?has_content) && !(bundidURL?has_content)>
                        <p>${msg("buerger_bayernid_description_" + authlevel)}</p>

                        <#if samlURL?has_content>
                        <a href="${samlURL}">
                        <#else>
                        <a href="${buergerKontoURL}">
                        </#if>
                            <button class="m-button m-button--primary" style="padding: 12px;">
                                <img class="icon" style="margin-right: 12px;" src="${url.resourcesPath}/img/providers/bayernid-dark.png" alt="BayernID Logo"/>
                                ${msg("buerger_bayernid_login_button")}
                                <img class="icon" style="margin-left: 12px;" src="${url.resourcesPath}/img/icons/ext-link.svg"/>
                            </button>
                        </a>
                        <a href="https://id.bayernportal.de/de/registration/eID" target="_blank">
                            <button class="m-button m-button--link">
                                ${msg("buerger_bayernid_register_button")}
                                <img class="icon" style="margin-left: 12px;" src="${url.resourcesPath}/img/icons/ext-link-blue.svg"/>
                            </button>
                        </a>

                    <!-- BayernID UND BundID -->
                    <#elseif (samlURL?has_content || buergerKontoURL?has_content) && (bundidURL?has_content)>
                        <p>${msg("buerger_bayernidbundid_description_" + authlevel)}</p>

                        <a href="${bundidURL}">
                            <button class="m-button m-button--primary" style="padding: 12px;">
                                <img class="icon" style="margin-right: 12px;" src="${url.resourcesPath}/img/providers/bundid.png" alt="BundID Logo"/>
                                ${msg("buerger_bundid_login_button")}
                                <img class="icon" style="margin-left: 12px;" src="${url.resourcesPath}/img/icons/ext-link.svg"/>
                            </button>
                        </a>
                        <a href="https://id.bund.de/de/registration/eID" target="_blank">
                            <button class="m-button m-button--link">
                                ${msg("buerger_bundid_register_button")}
                                <img class="icon" style="margin-left: 12px;" src="${url.resourcesPath}/img/icons/ext-link-blue.svg"/>
                            </button>
                        </a>

                        <div class="seperator">
                            <div class="hr"></div>
                            <div class="text">oder</div>
                            <div class="hr"></div>
                        </div>

                        <#if samlURL?has_content>
                        <a href="${samlURL}">
                        <#else>
                        <a href="${buergerKontoURL}">
                            </#if>
                            <button class="m-button m-button--primary" style="padding: 12px;">
                                <img class="icon" style="margin-right: 12px;" src="${url.resourcesPath}/img/providers/bayernid-dark.png" alt="BayernID Logo"/>
                                ${msg("buerger_bayernid_login_button")}
                                <img class="icon" style="margin-left: 12px;" src="${url.resourcesPath}/img/icons/ext-link.svg"/>
                            </button>
                        </a>

                    <!-- Nur BundID -->
                    <#else>
                        <p>${msg("buerger_bundid_description_" + authlevel)}</p>

                        <a href="${bundidURL}">
                            <button class="m-button m-button--primary" style="padding: 12px;">
                                <img class="icon" style="margin-right: 12px;" src="${url.resourcesPath}/img/providers/bundid.png" alt="BundID Logo"/>
                                ${msg("buerger_bundid_login_button")}
                                <img class="icon" style="margin-left: 12px;" src="${url.resourcesPath}/img/icons/ext-link.svg"/>
                            </button>
                        </a>
                        <a href="https://id.bund.de/de/registration/eID" target="_blank">
                            <button class="m-button m-button--link">
                                ${msg("buerger_bundid_register_button")}
                                <img class="icon" style="margin-left: 12px;" src="${url.resourcesPath}/img/icons/ext-link-blue.svg"/>
                            </button>
                        </a>

                    </#if>
                </div>
            </#if>


            <#if elsterNezoURL?has_content>
                <div class="card unternehmen">
                    <h2>${msg('unternehmen_heading')}</h2>
                    <p>${msg('unternehmen_description')}</p>

                    <a href="${elsterNezoURL}">
                        <button
                                class="m-button m-button--primary"
                                style="padding: 12px;"
                        >
                            <img
                                    class="icon"
                                    style="margin-right: 12px;"
                                    src="${url.resourcesPath}/img/providers/elster-dark.png"
                                    alt="Ekster Logo"
                            />
                            ${msg("unternehmen_login_button")}
                            <img
                                    class="icon"
                                    style="margin-left: 12px;"
                                    src="${url.resourcesPath}/img/icons/ext-link.svg"
                            />
                        </button>
                    </a>

                    <a href="https://www.elster.de/elsterweb/infoseite/nezo" target="_blank">
                        <button
                                class="m-button m-button--link"
                        >
                            ${msg("unternehmen_register_button")}
                            <img
                                    class="icon"
                                    style="margin-left: 12px;"
                                    src="${url.resourcesPath}/img/icons/ext-link-blue.svg"
                            />
                        </button>
                    </a>
                </div>
            </#if>


            <#if internURL?has_content>
                <div class="card mitarbeitende">
                    <h2>${msg("mitarbeiter_heading")}</h2>
                    <p>${msg("mitarbeiter_description")}</p>

                    <a href="${internURL}">
                        <button
                                class="m-button m-button--primary"
                                style="padding: 12px;"
                        >
                            <img
                                    class="icon"
                                    style="margin-right: 12px;"
                                    src="${url.resourcesPath}/img/providers/yubikey.png"
                                    alt="Yubikey Logo"
                            />
                            ${msg("mitarbeiter_login_button")}
                            <img
                                    class="icon"
                                    style="margin-left: 12px;"
                                    src="${url.resourcesPath}/img/icons/ext-link.svg"
                            />
                        </button>
                    </a>
                </div>
            </#if>
        </div>
        <footer>
            ${themeVersion}
        </footer>
    </div>
</div>