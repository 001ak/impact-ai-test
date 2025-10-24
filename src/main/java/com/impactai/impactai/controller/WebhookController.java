package com.impactai.impactai.controller;
import com.impactai.impactai.model.PRChangeInfo;
import com.impactai.impactai.service.GitHubPRFileFetcherService;
import com.impactai.impactai.service.PRParserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/webhook")
public class WebhookController {

    private final PRParserService prParserService;

    @Autowired
    private GitHubPRFileFetcherService prFileFetcher;

    public WebhookController(PRParserService prParserService) {
        this.prParserService = prParserService;
    }

    @PostMapping("/pr")
    public ResponseEntity<?> handlePRWebhook(@RequestBody Map<String, Object> payload) {
        // Extract owner, repo, and PR number from payload
        Map<String, Object> pr = (Map<String, Object>) payload.get("pull_request");
        Map<String, Object> repo = (Map<String, Object>) payload.get("repository");

        String owner = ((Map<String, Object>) repo.get("owner")).get("login").toString();
        String repoName = repo.get("name").toString();
        int prNumber = Integer.parseInt(pr.get("number").toString());

        List<PRChangeInfo> changedFiles = prFileFetcher.fetchChangedFiles(owner, repoName, prNumber);

        // Print to terminal for each file
        System.out.println("=== Changed Files from PR #" + prNumber + " ===");
        for (PRChangeInfo info : changedFiles) {
            System.out.printf("• %s [%s]%n", info.getFilePath(), info.getChangeType());
        }
        System.out.println("=======================");

        // Return for HTTP response
        return ResponseEntity.ok(changedFiles);
    }


    @PostMapping("/pr-test")
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


