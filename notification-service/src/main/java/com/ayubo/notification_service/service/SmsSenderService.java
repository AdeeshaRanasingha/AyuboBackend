package com.ayubo.notification_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class SmsSenderService {

    private static final Logger log = LoggerFactory.getLogger(SmsSenderService.class);

    private final RestTemplate restTemplate;

    @Value("${notification.sms.enabled:true}")
    private boolean smsEnabled;

    @Value("${notification.sms.twilio.account-sid:}")
    private String twilioAccountSid;

    @Value("${notification.sms.twilio.auth-token:}")
    private String twilioAuthToken;

    @Value("${notification.sms.twilio.from-number:}")
    private String twilioFromNumber;

    @Value("${notification.sms.twilio.messaging-service-sid:}")
    private String twilioMessagingServiceSid;

    public SmsSenderService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean sendSms(String toPhone, String message) {
        if (!smsEnabled || !StringUtils.hasText(toPhone)) {
            return false;
        }

        boolean hasFrom = StringUtils.hasText(twilioFromNumber);
        boolean hasMessagingService = StringUtils.hasText(twilioMessagingServiceSid);
        if (!StringUtils.hasText(twilioAccountSid) || !StringUtils.hasText(twilioAuthToken) || (!hasFrom && !hasMessagingService)) {
            log.warn("SMS skipped: configure TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN and either TWILIO_FROM_NUMBER or TWILIO_MESSAGING_SERVICE_SID.");
            return false;
        }

        try {
            String twilioUrl = "https://api.twilio.com/2010-04-01/Accounts/" + twilioAccountSid + "/Messages.json";

            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("To", toPhone);
            if (hasMessagingService) {
                form.add("MessagingServiceSid", twilioMessagingServiceSid);
            } else {
                form.add("From", twilioFromNumber);
            }
            form.add("Body", message);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            String authValue = twilioAccountSid + ":" + twilioAuthToken;
            String encodedAuth = Base64.getEncoder().encodeToString(authValue.getBytes(StandardCharsets.UTF_8));
            headers.set("Authorization", "Basic " + encodedAuth);

            restTemplate.postForEntity(twilioUrl, new HttpEntity<>(form, headers), String.class);
            return true;
        } catch (HttpStatusCodeException ex) {
            log.error("SMS send failed: HTTP {} {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            return false;
        } catch (Exception ex) {
            log.error("SMS send failed", ex);
            return false;
        }
    }
}
