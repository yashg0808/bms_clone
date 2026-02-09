package com.bookmyshow.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * SMS Service - Sends SMS notifications.
 * In production, this would integrate with Twilio, AWS SNS, or similar.
 */
@Slf4j
@Service
public class SmsService {

    @Value("${notification.sms.enabled:false}")
    private boolean smsEnabled;

    @Async
    public void sendSms(String phoneNumber, String message) {
        if (!smsEnabled) {
            log.info("SMS sending disabled. Would send to {}: {}", phoneNumber, message);
            return;
        }

        // In production: Use Twilio SDK
        // Message.creator(
        //     new PhoneNumber(phoneNumber),
        //     new PhoneNumber(twilioFromNumber),
        //     message
        // ).create();

        log.info("SMS sent to {}: {}", phoneNumber, message);
    }
}
