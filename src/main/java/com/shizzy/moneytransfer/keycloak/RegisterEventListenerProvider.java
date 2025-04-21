package com.shizzy.moneytransfer.keycloak;

import org.jetbrains.annotations.NotNull;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;


public class RegisterEventListenerProvider implements EventListenerProvider {

    private static final String ENDPOINT_SECRET = System.getenv("KEYCLOAK_EVENT_LISTENER_ENDPOINT_SECRET");
    private static final String ENDPOINT_URL = System.getenv("KEYCLOAK_EVENT_LISTENER_ENDPOINT_URL");

    private static final Logger logger = LoggerFactory.getLogger(RegisterEventListenerProvider.class);

    private final KeycloakSession session;

    public RegisterEventListenerProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void onEvent(Event event) {
        if (event.getType() == EventType.REGISTER) {
            logger.info("User Registered in: " + event.getUserId());
            setUserAttribute(event.getUserId());
            sendPostRequest(event.getUserId());
        }
    }

    private void setUserAttribute(String userId){
        UserModel userModel = session.users().getUserById(session.getContext().getRealm(), userId);
        userModel.setSingleAttribute("profileImageUrl", "");
    }

    @Override
    public void onEvent(AdminEvent adminEvent, boolean b) {
        System.out.println("Admin Event Occurred: " + adminEvent.getOperationType() + " for User: " + adminEvent.getResourcePath());
    }

    @Override
    public void close() {

    }

    private void sendPostRequest(String userId) {
        try {
            final HttpURLConnection conn = getHttpURLConnection(userId);

            int responseCode = conn.getResponseCode();
            logger.info("POST Response Code :: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) { //success
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                logger.info("The response: ",response);
            } else {
                logger.info("POST request not worked");
            }

        } catch (Exception e) {
            logger.error("Error occurred while sending POST request", e);
        }
    }

    @NotNull
    private static HttpURLConnection getHttpURLConnection(String userId) throws IOException {
        URL url = new URL(ENDPOINT_URL+"/api/v1/keycloak/events");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; utf-8");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Sig-Header", ENDPOINT_SECRET);
        conn.setDoOutput(true);

        logger.info("Sending POST request to: " + ENDPOINT_URL+"/api/v1/keycloak/events");

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = userId.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        return conn;
    }
}
