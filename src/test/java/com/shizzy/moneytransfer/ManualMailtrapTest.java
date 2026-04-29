package com.shizzy.moneytransfer;

import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Standalone test for Mailtrap email sending
 * Run this with: mvn test -Dtest=ManualMailtrapTest
 */
public class ManualMailtrapTest {

    private static final String MAILTRAP_API_URL = "https://send.api.mailtrap.io/api/send";
    private static final String API_TOKEN = "a0d59b093b0a28403974e93a3d830f05";
    private static final String SENDER_EMAIL = "wavesend@sandbox.nexhrm.com";
    private static final String TEST_RECIPIENT = "fran64biz@gmail.com";

    public static void main(String[] args) {
        try {
            System.out.println("Testing Mailtrap email sending...");

            RestTemplate restTemplate = new RestTemplate();

            // Build request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("from", Map.of("email", SENDER_EMAIL, "name", "WaveSend"));
            requestBody.put("to", new Object[]{Map.of("email", TEST_RECIPIENT)});
            requestBody.put("subject", "Test Email from WaveSend - Mailtrap Integration");
            requestBody.put("text", "Hello! This is a test email from WaveSend using Mailtrap.");
            requestBody.put("html", "<html><body><h1>Test Email</h1><p>Hello! This is a test email from <strong>WaveSend</strong> using Mailtrap.</p><p>If you're receiving this, the integration is working perfectly!</p></body></html>");

            // Set up headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + API_TOKEN);

            // Create entity
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // Send request
            System.out.println("Sending email to: " + TEST_RECIPIENT);
            ResponseEntity<String> response = restTemplate.exchange(
                    MAILTRAP_API_URL,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            System.out.println("Response Status: " + response.getStatusCode());
            System.out.println("Response Body: " + response.getBody());

            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("\n✅ SUCCESS! Email sent successfully via Mailtrap!");
                System.out.println("Check your inbox at: " + TEST_RECIPIENT);
            } else {
                System.out.println("\n❌ FAILED! Status: " + response.getStatusCode());
            }

        } catch (Exception e) {
            System.err.println("\n❌ ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
