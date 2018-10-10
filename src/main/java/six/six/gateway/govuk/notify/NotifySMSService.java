package six.six.gateway.govuk.notify;

import org.jboss.logging.Logger;
import six.six.gateway.SMSService;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;

import java.util.HashMap;
import java.util.Map;

/**
 * GOV.UK Notify SMS service implementation
 */
public class NotifySMSService implements SMSService {

    private static Logger logger = Logger.getLogger(NotifySMSService.class);

    private static final String NOTIFICATION_REFERENCE = "Keycloak OTP";

    private final NotificationClient client;
    private final String templateId;

    public NotifySMSService(String notifyApiKey, String templateId) {
        client = new NotificationClient(notifyApiKey);
        this.templateId = templateId;
    }

    @Override
    public boolean send(String phoneNumber, String message, String login, String pw) {
        Map<String, String> personalisation = new HashMap<>();
        personalisation.put("message", message);

        try {
            client.sendSms(templateId, phoneNumber, personalisation, NOTIFICATION_REFERENCE);
            return true;
        } catch (NotificationClientException e) {
            logger.error("Failed to send SMS request to Notify API: " + e.getLocalizedMessage());
            return false;
        }
    }
}
