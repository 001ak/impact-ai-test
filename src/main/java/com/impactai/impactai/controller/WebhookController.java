package com.impactai.impactai.controller;

import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/webhook")
public class WebhookController {

    @PostMapping("/pr")
    public String handlePullRequest(@RequestBody Map<String, Object> payload,
                                    @RequestHeader Map<String, String> headers) {
        System.out.println("Received PR webhook:");

        System.out.println("Headers: " + headers);
        System.out.println("Payload: " + payload);
        return "OK";
    }
}

