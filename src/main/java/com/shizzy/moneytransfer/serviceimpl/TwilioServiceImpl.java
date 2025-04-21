package com.shizzy.moneytransfer.serviceimpl;

import com.shizzy.moneytransfer.config.TwilioConfig;
import com.twilio.Twilio;
import com.twilio.rest.verify.v2.service.Verification;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TwilioServiceImpl {

    @PostConstruct
    public void initTwilio() {
        Twilio.init(twilioConfig.getAccountSid(), twilioConfig.getAuthToken());
    }

    private final TwilioConfig twilioConfig;

    private Map<String, String> otpStorage = new HashMap<>();

    public void sendOTP(String toPhoneNumber) {
        String otp = generateOTP();
        otpStorage.put(toPhoneNumber, otp);

        String messageBody = "Your OTP is: " + otp;

        Message.creator(
                new PhoneNumber(toPhoneNumber),
                new PhoneNumber(twilioConfig.getTrialNumber()),
                messageBody
        ).create();
    }

    public void verify(){
        Verification verification = Verification.creator(
                        "VA05336db19dbd48a9ce3c399d85c06533",
                        "+2349122314170",
                        "sms")
                .create();
        System.out.println(verification.getSid());
    }

    public boolean verifyOTP(String phoneNumber, String otp) {
        String storedOtp = otpStorage.get(phoneNumber);
        return storedOtp != null && storedOtp.equals(otp);
    }

    private String generateOTP() {
        return String.valueOf(new SecureRandom().nextInt(900000) + 100000);
    }

}
