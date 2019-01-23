<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "title">
        ${msg("updatePhoneNumberTitle", realm.name)}
    <#elseif section = "header">
        ${msg("updatePhoneNumberTitle", realm.name)}
    <#elseif section = "form">
        <div class="govuk-grid-row">
            <div class="govuk-grid-column-full">
                <p>${msg("updatePhoneNumberMessage")}</p>
            </div>
            <form id="kc-totp-login-form" class="${properties.kcFormClass!} govuk-grid-column-two-thirds" action="${url.loginAction}" method="post">
                <div class="govuk-form-group">
                    <label for="mobile_number" class="govuk-label">${msg("phoneNumber")}</label>
                    <input id="mobile_number" name="mobile_number" type="text" class="govuk-input" value="${phoneNumber!''}" />
                </div>

                <div class="govuk-form-group">
                    <input class="govuk-button" type="submit" value="${msg("doSubmit")}"/>
                </div>
            </form>
        </div>
    </#if>
</@layout.registrationLayout>
