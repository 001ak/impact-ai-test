package com.impactai.impactai.controller;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/webhook")
public class WebhookController {

    @PostMapping("/pr")
    public ResponseEntity<String> handlePRWebhook(@RequestBody Map<String, Object> payload,
                                                  @RequestHeader Map<String, String> headers) {
        // Print headers
        System.out.println("=== Webhook Headers ===");
        headers.forEach((key, value) -> System.out.println(key + ": " + value));

        // Print payload
        System.out.println("\n=== Webhook Payload ===");
        payload.forEach((key, value) -> System.out.println(key + ": " + value));

        // Optional: convert payload to pretty JSON string using Jackson
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String prettyJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
            System.out.println("\n=== Pretty JSON Payload ===");
            System.out.println(prettyJson);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ResponseEntity<>("Webhook received", HttpStatus.OK);
    }
}

