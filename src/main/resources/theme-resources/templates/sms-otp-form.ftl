<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=true; section>
    <#if section = "header">
        OTP Verification
    <#elseif section = "form">
        <form id="kc-sms-otp-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <div class="${properties.kcFormGroupClass!}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <label for="otp" class="${properties.kcLabelClass!}">Enter OTP</label>
                </div>
                <div class="${properties.kcInputWrapperClass!}">
                    <input type="text" id="otp_code" name="otp_code" class="${properties.kcInputClass!}" autofocus/>
                </div>
            </div>

            <div class="${properties.kcFormGroupClass!}">
                <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                    <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}" type="submit" value="Submit"/>
                    <input class="${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}" 
                        style="margin-top: 10px;" 
                        type="submit" 
                        name="resend" 
                        value="Resend OTP"/>
                </div>
            </div>
        </form>
    </#if>
</@layout.registrationLayout>
