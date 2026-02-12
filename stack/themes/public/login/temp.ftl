<!-- This is a temporary file to develop the initial design according to

https://www.figma.com/design/X8XCxBmS9VnYsAdnrbputf/Design-Spezifikation-_Meine-Anmeldung_?node-id=201-54888&m=dev

and will be ported to a *.ftl-Themefile in the end -->

<head>
    <title>Login-Testpage</title>
    <link rel="stylesheet" href="${url.resourcesPath}/css/styles.css">
    <link rel="stylesheet" href="${url.resourcesPath}/css/mucbutton.css">
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
</head>

<body>
<div class="site">
    <header>
        <a>
            <img class="header-image" src="${url.resourcesPath}/img/muenchende.png">
        </a>
        <div class="spacer"></div>

        <#if logoutConfirm.skipLink>
        <#else>
            <#if (client.baseUrl)?has_content>
            <a href="${client.baseUrl}">${kcSanitize(msg("backToApplication"))?no_esc}
                <button
                        class="m-button m-button--primary"
                >
                    <img class="icon" src="${url.resourcesPath}/img/icons/close.svg">
                </button>
            </a>
            </#if>
        </#if>
    </header>

    <div>
        <h1 class="heading">${msg("logoutHeading")}</h1>

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
            <div class="card">
                <h2>${msg("logoutConfirmHeader")}</h2>
                <p>${msg("logoutConfirmBody")}</p>

                <form class="form-actions" action="${url.logoutConfirmAction}"
                      onsubmit="confirmLogout.disabled = true; return true;" method="POST">
                    <input type="hidden" name="session_code" value="${logoutConfirm.code}">
                    <div class="${properties.kcFormGroupClass!}">
                        <div id="kc-form-options">
                            <div class="${properties.kcFormOptionsWrapperClass!}">
                            </div>
                        </div>

                        <div id="kc-form-buttons" class="${properties.kcFormGroupClass!}">
                            <button
                                    name="confirmLogout"
                                    type="submit"
                                    class="m-button m-button--primary"
                                    style="padding: 12px;"
                            >
                                ${msg("doLogout")}
                                <img
                                        class="icon"
                                        style="margin-left: 12px;"
                                        src="${url.resourcesPath}/img/icons/ext-link.svg"
                                />
                            </button>
                        </div>
                    </div>
                </form>
            </div>
        </div>
        <footer>
        </footer>
    </div>
</div>
</body>





<!--
Messages:


-->
