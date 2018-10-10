package six.six.gateway.govuk.notify;

import six.six.gateway.SMSService;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;

import java.util.HashMap;
import java.util.Map;

public class NotifySMSService implements SMSService {

    private final NotificationClient client;
    private final String templateId;

    public NotifySMSService(String notifyApiKey, String templateId) {
        client = new NotificationClient(notifyApiKey);
        this.templateId = templateId;
    }

    @Override
    public boolean send(String phoneNumber, String message, String login, String pw) {
        String reference = "Keycloak OTP";

        Map<String, String> personalisation = new HashMap<>();
        personalisation.put("message", message);

        try {
            client.sendSms(templateId, phoneNumber, personalisation, reference);
            return true;
        } catch (NotificationClientException e) {
            System.err.println("Failed to send SMS request to Notify API: " + e.getLocalizedMessage());
            return false;
        }
    }
}
