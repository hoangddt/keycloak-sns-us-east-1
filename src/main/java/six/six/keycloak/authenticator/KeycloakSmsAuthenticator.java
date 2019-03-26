package six.six.keycloak.authenticator;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import six.six.keycloak.KeycloakSmsConstants;
import six.six.keycloak.MobileNumberHelper;
import six.six.keycloak.requiredaction.action.required.KeycloakSmsMobilenumberRequiredAction;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.Date;
import java.util.List;

/**
 * Created by joris on 11/11/2016.
 */
public class KeycloakSmsAuthenticator implements Authenticator {

    private static Logger logger = Logger.getLogger(KeycloakSmsAuthenticator.class);

    public static final String CREDENTIAL_TYPE = "sms_validation";

    private enum CODE_STATUS {
        VALID,
        INVALID,
        EXPIRED
    }


    private boolean isOnlyForVerificationMode(boolean onlyForVerification, String mobileNumber, String mobileNumberVerified) {
        return (mobileNumber == null || onlyForVerification && !mobileNumber.equals(mobileNumberVerified));
    }

    private String getMobileNumber(UserModel user) {
        return MobileNumberHelper.getMobileNumber(user);
    }

    private String getMobileNumberVerified(UserModel user) {
        List<String> mobileNumberVerifieds = user.getAttribute(KeycloakSmsConstants.ATTR_MOBILE_VERIFIED);

        String mobileNumberVerified = null;
        if (mobileNumberVerifieds != null && !mobileNumberVerifieds.isEmpty()) {
            mobileNumberVerified = mobileNumberVerifieds.get(0);
        }
        return mobileNumberVerified;
    }

    private Boolean isAuthViaPhoneRequired(AuthenticationFlowContext context) {
        return context.getExecution().getRequirement() == AuthenticationExecutionModel.Requirement.REQUIRED;
    }

    private boolean send2FACodeViaSMS(AuthenticationFlowContext context, String mobileNumber) {
        logger.debug("send2FACodeViaSMS, phone: " + mobileNumber);

        AuthenticatorConfigModel config = context.getAuthenticatorConfig();

        long nrOfDigits = KeycloakSmsAuthenticatorUtil.getConfigLong(config, KeycloakSmsConstants.CONF_PRP_SMS_CODE_LENGTH, 8L);
        logger.debug("Using nrOfDigits " + nrOfDigits);

        long ttl = KeycloakSmsAuthenticatorUtil.getConfigLong(config, KeycloakSmsConstants.CONF_PRP_SMS_CODE_TTL, 10 * 60L); // 10 minutes in s

        logger.debug("Using ttl " + ttl + " (s)");

        String code = KeycloakSmsAuthenticatorUtil.getSmsCode(nrOfDigits);

        this.storeSMSCode(context, code, new Date().getTime() + (ttl * 1000)); // s --> ms
        logger.debug("Sending code to mobile number: " + mobileNumber + ", code is: " + code);
        return KeycloakSmsAuthenticatorUtil.sendSmsCode(mobileNumber, code, context);
    }

    private boolean verifyMobileNumber(AuthenticationFlowContext context, String mobileNumber) {
        logger.debug("Verifying phone: " + mobileNumber);
        boolean result = this.send2FACodeViaSMS(context, mobileNumber);
        boolean verifyResult = false;
        Response challenge = null;

        if (result) {
            logger.debug("SMS for verification phone number sent");
            challenge = context.form().createForm("sms-verify-phone-validation.ftl");
            context.challenge(challenge);
        } else {
            challenge = context.form()
                .setError("sms-auth.not.send")
                .createForm("sms-validation-error.ftl");
            context.failureChallenge(AuthenticationFlowError.INTERNAL_ERROR, challenge);
        }

        return verifyResult;
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        logger.debug("authenticate called ... context = " + context);
        UserModel user = context.getUser();
        AuthenticatorConfigModel config = context.getAuthenticatorConfig();

        if (isAuthViaPhoneRequired(context)) {
            String mobileNumber = this.getMobileNumber(user);
            String mobileNumberVerified = this.getMobileNumberVerified(user);

            if (mobileNumber == null) {
                logger.debug("No mobileNumber, asking for one");
                boolean isAskingFor = KeycloakSmsAuthenticatorUtil.getConfigBoolean(config, KeycloakSmsConstants.MOBILE_ASKFOR_ENABLED);
                if (isAskingFor) {
                    //Enable access and ask for mobilenumber
                    user.addRequiredAction(KeycloakSmsMobilenumberRequiredAction.PROVIDER_ID);
                    context.success();
                }
            }

            if (mobileNumberVerified != null) {
                boolean result = this.send2FACodeViaSMS(context, mobileNumberVerified);
                logger.debug("SMS send status: " + result);

                if (result) {
                    Response challenge = context.form().createForm("sms-validation.ftl");
                    context.challenge(challenge);
                } else {
                    Response challenge = context.form()
                        .setError("sms-auth.not.send")
                        .createForm("sms-validation-error.ftl");
                    context.failureChallenge(AuthenticationFlowError.INTERNAL_ERROR, challenge);
                }

            } else if (mobileNumber != null) {
                // phone is not verify -> verify
                logger.debug("Phone is not verify, verifying: " + this.getMobileNumber(user));

                // ToDo: check this number and number in update verify
                this.verifyMobileNumber(context, this.getMobileNumber(user));
                this.updateVerifiedMobilenumber(context);

                logger.debug("Verify complete, number verified: " + this.getMobileNumberVerified(user));
                // boolean result = this.send2FACodeViaSMS(context, this.getMobileNumber(user));
                
                // if (result) {
                //     Response challenge = context.form().createForm("sms-validation.ftl");
                //     context.challenge(challenge);
                // } else {
                //     Response challenge = context.form()
                //         .setError("sms-auth.not.send")
                //         .createForm("sms-validation-error.ftl");
                //     context.failureChallenge(AuthenticationFlowError.INTERNAL_ERROR, challenge);
                // }
            }
        } else {
            context.success();
        }

    }


    @Override
    public void action(AuthenticationFlowContext context) {
        logger.debug("action called ... context = " + context);
        CODE_STATUS status = this.validateCode(context);
        Response challenge = null;

        switch (status) {
            case EXPIRED:
                challenge = context.form()
                        .setError("sms-auth.code.expired")
                        .createForm("sms-validation.ftl");
                context.failureChallenge(AuthenticationFlowError.EXPIRED_CODE, challenge);
                break;

            case INVALID:
                challenge = context.form()
                        .setError("sms-auth.code.invalid")
                        .createForm("sms-validation.ftl");
                context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challenge);
                break;

            case VALID:
                context.success();
                break;
        }
    }


    private void updateVerifiedMobilenumber(AuthenticationFlowContext context) {
        AuthenticatorConfigModel config = context.getAuthenticatorConfig();
        UserModel user = context.getUser();
        boolean onlyForVerification = KeycloakSmsAuthenticatorUtil.getConfigBoolean(config, KeycloakSmsConstants.MOBILE_VERIFICATION_ENABLED);

        if (onlyForVerification) {
            //Only verification mode
            List<String> mobileNumberCreds = user.getAttribute(KeycloakSmsConstants.ATTR_MOBILE);
            if ((mobileNumberCreds != null) && !mobileNumberCreds.isEmpty()) {
                user.setAttribute(KeycloakSmsConstants.ATTR_MOBILE_VERIFIED, mobileNumberCreds);
            }
        }
    }

    // Store the code + expiration time in a UserCredential. Keycloak will persist these in the DB.
    // When the code is validated on another node (in a clustered environment) the other nodes have access to it's values too.
    private void storeSMSCode(AuthenticationFlowContext context, String code, Long expiringAt) {
        UserCredentialModel credentials = new UserCredentialModel();
        credentials.setType(KeycloakSmsConstants.USR_CRED_MDL_SMS_CODE);
        credentials.setValue(code);

        context.getSession().userCredentialManager().updateCredential(context.getRealm(), context.getUser(), credentials);

        credentials.setType(KeycloakSmsConstants.USR_CRED_MDL_SMS_EXP_TIME);
        credentials.setValue((expiringAt).toString());
        context.getSession().userCredentialManager().updateCredential(context.getRealm(), context.getUser(), credentials);
    }


    protected CODE_STATUS validateCode(AuthenticationFlowContext context) {
        CODE_STATUS result = CODE_STATUS.INVALID;

        logger.debug("validateCode called ... ");
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        String enteredCode = formData.getFirst(KeycloakSmsConstants.ANSW_SMS_CODE);
        KeycloakSession session = context.getSession();

        List codeCreds = session.userCredentialManager().getStoredCredentialsByType(context.getRealm(), context.getUser(), KeycloakSmsConstants.USR_CRED_MDL_SMS_CODE);
        /*List timeCreds = session.userCredentialManager().getStoredCredentialsByType(context.getRealm(), context.getUser(), KeycloakSmsAuthenticatorConstants.USR_CRED_MDL_SMS_EXP_TIME);*/

        CredentialModel expectedCode = (CredentialModel) codeCreds.get(0);
        /*CredentialModel expTimeString = (CredentialModel) timeCreds.get(0);*/

        logger.debug("Expected code = " + expectedCode + "    entered code = " + enteredCode);

        if (expectedCode != null) {
            result = enteredCode.equals(expectedCode.getValue()) ? CODE_STATUS.VALID : CODE_STATUS.INVALID;
            /*long now = new Date().getTime();

            logger.debug("Valid code expires in " + (Long.parseLong(expTimeString.getValue()) - now) + " ms");
            if (result == CODE_STATUS.VALID) {
                if (Long.parseLong(expTimeString.getValue()) < now) {
                    logger.debug("Code is expired !!");
                    result = CODE_STATUS.EXPIRED;
                }
            }*/
        }
        logger.debug("result: " + result);
        return result;
    }

    @Override
    public boolean requiresUser() {
        logger.debug("requiresUser called ... returning true");
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        logger.debug("configuredFor called ... session=" + session + ", realm=" + realm + ", user=" + user);
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        logger.debug("setRequiredActions called ... session=" + session + ", realm=" + realm + ", user=" + user);
    }

    @Override
    public void close() {
        logger.debug("close called ...");
    }

}
