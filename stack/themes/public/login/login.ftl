<#assign themeVersion>
    1.1.1
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

<!-- BundID (EUDI-Wallet) -->
<#assign eudiwalletURL>
    <@getProviderLogin alias="eudiwallet" providerList=social.providers/>
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
    <link rel="stylesheet" href="https://assets.muenchen.de/mde/1.1.15/css/fonts.css">
    <link rel="stylesheet" href="https://assets.muenchen.de/mde/1.1.15/css/style.css">
    <link rel="icon" type="image/x-icon" href="${url.resourcesPath}/img/favicon.ico">
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
</head>
<div class="login-site">
    <header class="m-page-header__top">
        <a href="https://www.muenchen.de/" rel="home">
            <img
                alt="Logo muenchen.de - das offizielle Stadtportal (Zur Startseite)"
                class="m-page-header__branding-image"
                src="${url.resourcesPath}/img/muenchende.svg"
            >
        </a>
        <div class="spacer"></div>
        <button
                alt="${kcSanitize(msg("backToApplication"))?no_esc}"
                class="m-button m-button--primary"
                onClick="(function(){
                    history.back();
                    return false;
                })();return false;"
        >
            <span>${kcSanitize(msg("cancel"))?no_esc}</span>
            <img
                    class="m-button__icon m-button__icon--after"
                    src="${url.resourcesPath}/img/icons/close.svg"
                    alt=""
            >
        </button>
    </header>

    <div>
        <h1 class="heading">${msg("heading")}</h1>

        <#if message?has_content && (message.type != 'warning' || !isAppInitiatedAction??)>
            <div class="card-container">
                <div class="card">
                      <#if message.type = 'error'>
                        <p class="error">
                            Der folgende Fehler ist aufgetreten: <br/>
                            ${kcSanitize(message.summary)?no_esc}
                        </p>
                      </#if>
                      <#if message.type != 'error'>
                        <p class="message">
                            Das System meldet folgende Nachricht: <br/>
                            ${kcSanitize(message.summary)?no_esc}
                        </p>
                      </#if>
                </div>
            </div>
        </#if>

        <div class="graphics-container">
            <img
                    class="munich-background"
                    src="${url.resourcesPath}/img/bg/bg-munich-left.svg"
                    alt=""
                    aria-hidden="true"
            />
            <div class="spacer"></div>
            <img
                    class="munich-background rathaus"
                    src="${url.resourcesPath}/img/bg/bg-munich-right.svg"
                    alt=""
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
                        <a
                            class="m-button m-button--primary"
                            href="${samlURL}"
                        >
                        <#else>
                        <a
                            class="m-button m-button--primary"
                            href="${buergerKontoURL}"
                        >
                        </#if>
                            <img class="icon left" src="${url.resourcesPath}/img/providers/bayernid.svg" alt=""/>
                            <span>${msg("buerger_bayernid_login_button")}</span>
                        </a>
                        <a
                            class="m-button m-button--link"
                            href="https://id.bayernportal.de/de/registration/eID"
                            target="_blank"
                        >
                            <span>${msg("buerger_bayernid_register_button")}</span>
                        </a>

                    <!-- BayernID UND BundID -->
                    <#elseif (samlURL?has_content || buergerKontoURL?has_content) && (bundidURL?has_content)>
                        <p>${msg("buerger_bayernidbundid_description_" + authlevel)}</p>

                        <a
                            href="${bundidURL}"
                            class="m-button m-button--primary"
                        >
                            <img class="icon left" src="${url.resourcesPath}/img/providers/bundid.svg" alt=""/>
                            <span>${msg("buerger_bundid_login_button")}</span>
                        </a>
                        <a
                            href="https://id.bund.de/de/registration/eID"
                            class="m-button m-button--link"
                            target="_blank"
                        >
                            <span>${msg("buerger_bundid_register_button")}</span>
                        </a>

                        <div class="seperator">
                            <div class="hr"></div>
                            <div class="text">oder</div>
                            <div class="hr"></div>
                        </div>

                        <#if samlURL?has_content>
                        <a
                            href="${samlURL}"
                            class="m-button m-button--primary"
                        >
                        <#else>
                        <a
                            href="${buergerKontoURL}"
                            class="m-button m-button--primary"
                        >
                            </#if>
                            <img class="icon left" src="${url.resourcesPath}/img/providers/bayernid.svg" alt=""/>
                            <span>${msg("buerger_bayernid_login_button")}</span>
                        </a>

                    <!-- Nur BundID -->
                    <#else>
                        <p>${msg("buerger_bundid_description_" + authlevel)}</p>

                        <a
                            href="${bundidURL}"
                            class="m-button m-button--primary"
                        >
                            <img class="icon left" src="${url.resourcesPath}/img/providers/bundid.svg" alt=""/>
                            ${msg("buerger_bundid_login_button")}
                        </a>
                        <a
                            href="https://id.bund.de/de/registration/eID"
                            class="m-button m-button--link"
                            target="_blank"
                        >
                            <span>${msg("buerger_bundid_register_button")}</span>
                        </a>
                    </#if>
					
					<#if (eudiwalletURL?has_content)>
						<p>
						<a
                            href="${eudiwalletURL}"
                            class="m-button m-button--primary"
                        >
                            <img class="icon left" src="${url.resourcesPath}/img/providers/bundid.svg" alt=""/>
                            ${msg("buerger_bundid_login_button")} (EUDI-Wallet)
                        </a>
					</#if>
                </div>
            </#if>


            <#if elsterNezoURL?has_content>
                <div class="card unternehmen">
                    <h2>${msg('unternehmen_heading')}</h2>
                    <p>${msg('unternehmen_description')}</p>

                    <a
                        href="${elsterNezoURL}"
                        class="m-button m-button--primary"
                    >
                        <img
                                class="icon left"
                                src="${url.resourcesPath}/img/providers/elster.svg"
                                alt=""
                        />
                        <span>${msg("unternehmen_login_button")}</span>
                    </a>

                    <a
                        href="https://www.elster.de/elsterweb/infoseite/nezo"
                        class="m-button m-button--link"
                        target="_blank"
                    >
                        <span>${msg("unternehmen_register_button")}</span>
                    </a>
                </div>
            </#if>


            <#if internURL?has_content>
                <div class="card mitarbeitende">
                    <h2>${msg("mitarbeiter_heading")}</h2>
                    <p>${msg("mitarbeiter_description")}</p>

                    <a
                        href="${internURL}"
                        class="m-button m-button--primary"
                    >
                        <img
                            class="icon left"
                            src="${url.resourcesPath}/img/providers/yubikey.png"
                            alt=""
                        />
                        ${msg("mitarbeiter_login_button")}
                    </a>
                </div>
            </#if>
        </div>
        <footer>
            <span
                style="display: none;"
            >
                ${themeVersion}
            </span>
        </footer>
    </div>
</div>