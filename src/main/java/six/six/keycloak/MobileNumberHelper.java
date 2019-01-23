package six.six.keycloak;

import org.keycloak.models.UserModel;

import java.util.List;

public class MobileNumberHelper {
    public static String getMobileNumber(UserModel user) {
        List<String> mobileNumberCreds = user.getAttribute(KeycloakSmsConstants.ATTR_MOBILE);

        String mobileNumber = null;

        if (mobileNumberCreds != null && !mobileNumberCreds.isEmpty()) {
            mobileNumber = mobileNumberCreds.get(0);
        }

        return mobileNumber;
    }
}
