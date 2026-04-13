package com.ayubo.notification_service.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class SmsSenderService {

    private final RestTemplate restTemplate;

    @Value("${notification.sms.enabled:false}")
    private boolean smsEnabled;

    @Value("${notification.sms.twilio.account-sid:}")
    private String twilioAccountSid;

    @Value("${notification.sms.twilio.auth-token:}")
    private String twilioAuthToken;

    @Value("${notification.sms.twilio.from-number:}")
    private String twilioFromNumber;

    public SmsSenderService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean sendSms(String toPhone, String message) {
        if (!smsEnabled || !StringUtils.hasText(toPhone)) {
            return false;
        }

        if (!StringUtils.hasText(twilioAccountSid) || !StringUtils.hasText(twilioAuthToken) || !StringUtils.hasText(twilioFromNumber)) {
            return false;
        }

        try {
            String twilioUrl = "https://api.twilio.com/2010-04-01/Accounts/" + twilioAccountSid + "/Messages.json";

            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("To", toPhone);
            form.add("From", twilioFromNumber);
            form.add("Body", message);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            String authValue = twilioAccountSid + ":" + twilioAuthToken;
            String encodedAuth = Base64.getEncoder().encodeToString(authValue.getBytes(StandardCharsets.UTF_8));
            headers.set("Authorization", "Basic " + encodedAuth);

            restTemplate.postForEntity(twilioUrl, new HttpEntity<>(form, headers), String.class);
            return true;
        } catch (Exception ex) {
            System.out.println("SMS send failed: " + ex.getMessage());
            return false;
        }
    }
}
