<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "title">
        ${msg("loginTitle",realm.name)}
    <#elseif section = "header">
        ${msg("loginTitleHtml",realm.name)}
    <#elseif section = "form">
      <div class="govuk-grid-row">
            <form id="kc-totp-login-form" class="${properties.kcFormClass!} govuk-grid-column-two-thirds" action="${url.loginAction}" method="post">
                <div class="govuk-form-group">
                    <label for="totp" class="govuk-label">Please enter your mobile number</label>
                    <input id="mobile_number" name="mobile_number" type="text" class="govuk-input" />
                </div>

                <div class="govuk-form-group">
                    <label for="totp" class="govuk-label">Please confirm your mobile number</label>
                    <input id="mobile_number" name="mobile_number_confirm" type="text" class="govuk-input" />
                </div>

                <div class="govuk-form-group">
                    <div id="kc-form-options" class="${properties.kcFormOptionsClass!}">
                        <div class="${properties.kcFormOptionsWrapperClass!}">
                        </div>
                    </div>
                    <input class="govuk-button" name="login" id="kc-login" type="submit" value="${msg("doSubmit")}"/>
                </div>
            </form>
        </div>
        <#if client?? && client.baseUrl?has_content>
            <p><a id="backToApplication" href="${client.baseUrl}">${msg("backToApplication")}</a></p>
        </#if>
    </#if>
</@layout.registrationLayout>
