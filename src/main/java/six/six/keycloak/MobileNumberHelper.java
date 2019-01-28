package six.six.keycloak;

import org.keycloak.models.UserModel;

public class MobileNumberHelper {
    public static String getMobileNumber(UserModel user) {
        String mobileNumberCreds = user.getFirstAttribute(KeycloakSmsConstants.ATTR_MOBILE);

        String mobileNumber = null;

        if (mobileNumberCreds != null && !mobileNumberCreds.isEmpty()) {
            mobileNumber = mobileNumberCreds;
        }

        return mobileNumber;
    }
}
