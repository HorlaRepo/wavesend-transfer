package com.shizzy.moneytransfer.serviceimpl;

import com.google.gson.JsonSyntaxException;
import com.shizzy.moneytransfer.service.payment.handler.StripeEventHandler;
import com.stripe.model.Event;
import com.stripe.net.ApiResource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookService {

    private final List<StripeEventHandler> eventHandlers;

    public ResponseEntity<String> handleWebhook(String payload) {
        Event event;
        try {
            event = ApiResource.GSON.fromJson(payload, Event.class);
        } catch (JsonSyntaxException e) {
            log.error("Invalid JSON payload: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        try {
            for (StripeEventHandler handler : eventHandlers) {
                if (handler.canHandle(event.getType())) {
                    handler.handleEvent(event.getData().toJson());
                    return ResponseEntity.ok("Success");
                }
            }
            log.info("Unhandled event type: {}", event.getType());
            return ResponseEntity.ok("Unhandled event type");
        } catch (Exception e) {
            log.error("Error processing webhook: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing webhook");
        }
    }
}
