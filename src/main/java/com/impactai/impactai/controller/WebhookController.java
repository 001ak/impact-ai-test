package com.impactai.impactai.controller;

import com.impactai.impactai.service.*;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.Map;

@RestController
@RequestMapping("/api/webhook")
public class WebhookController {

    @Autowired
    private WebhookProcessingService webhookProcessingService;

    @Autowired
    private RepoMetadataService repoMetadataService;

    @Value("${github.token}")
    private String githubToken;

    /**
     * Main webhook endpoint - returns immediately, processes async
     */
    @PostMapping("/pr")
    public ResponseEntity<?> handleWebhook(@RequestBody Map<String, Object> payload) {

        // Handle ping event
        if (payload.containsKey("zen") && payload.containsKey("hook")) {
            return handlePingEvent(payload);
        }

        // Handle pull request event
        if (payload.containsKey("pull_request")) {
            return handlePRWebhook(payload);
        }

        return ResponseEntity.ok(Map.of("status", "ignored", "message", "Unrecognized event"));
    }

    /**
     * Handle ping - immediate response, async processing
     */
    private ResponseEntity<?> handlePingEvent(Map<String, Object> payload) {
        System.out.println("=== WEBHOOK PING RECEIVED ===");

        Map<String, Object> repo = (Map<String, Object>) payload.get("repository");
        if (repo == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "No repository info"));
        }

        try {
            String owner = ((Map<String, Object>) repo.get("owner")).get("login").toString();
            String repoName = repo.get("name").toString();
            String repoFullName = owner + "/" + repoName;
            String defaultBranch = repo.get("default_branch").toString();

            System.out.println("Ping for repo: " + repoFullName);

            // Clone/pull repo synchronously (fast operation)
            String repoLocalPath = cloneOrFetchRepo(owner, repoName);
            checkoutBranch(repoLocalPath, defaultBranch);

            // Process async (parsing takes time)
            webhookProcessingService.processPingAsync(owner, repoName, repoFullName,
                    defaultBranch, repoLocalPath);

            // Immediate response to GitHub
            return ResponseEntity.ok(Map.of(
                    "status", "accepted",
                    "message", "Webhook configured, baseline processing started",
                    "repo", repoFullName
            ));

        } catch (Exception e) {
            System.err.println("Error during ping: " + e.getMessage());
            return ResponseEntity.ok(Map.of(
                    "status", "accepted",
                    "message", "Webhook configured, baseline will be built on first PR"
            ));
        }
    }

    /**
     * Handle PR - immediate response, async processing
     */
    private ResponseEntity<?> handlePRWebhook(Map<String, Object> payload) {

        Map<String, Object> pr = (Map<String, Object>) payload.get("pull_request");
        Map<String, Object> repo = (Map<String, Object>) payload.get("repository");

        if (pr == null || repo == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid payload"));
        }

        try {
            String owner = ((Map<String, Object>) repo.get("owner")).get("login").toString();
            String repoName = repo.get("name").toString();
            String repoFullName = owner + "/" + repoName;
            String defaultBranch = repo.get("default_branch").toString();
            int prNumber = Integer.parseInt(pr.get("number").toString());
            String headSha = ((Map<String, Object>) pr.get("head")).get("sha").toString();
            String action = (String) payload.get("action");

            System.out.println("=== PR #" + prNumber + " for " + repoFullName + " ===");

            // Clone/pull repo synchronously (fast)
            String repoLocalPath = cloneOrFetchRepo(owner, repoName);
            checkoutBranch(repoLocalPath, defaultBranch);

            // Process async (parsing and analysis take time)
            webhookProcessingService.processPRAsync(owner, repoName, repoFullName,
                    defaultBranch, prNumber, headSha,
                    action, repoLocalPath);

            // Immediate response to GitHub
            return ResponseEntity.ok(Map.of(
                    "status", "accepted",
                    "message", "PR received, impact analysis in progress",
                    "pr_number", prNumber,
                    "repo", repoFullName
            ));

        } catch (Exception e) {
            System.err.println("Error handling PR webhook: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Clone repo or pull latest
     */
    private String cloneOrFetchRepo(String owner, String repoName) {
        String repoUrl = "https://github.com/" + owner + "/" + repoName + ".git";
        String localPath = System.getProperty("java.io.tmpdir") + owner + "_" + repoName;
        File localDir = new File(localPath);

        try {
            UsernamePasswordCredentialsProvider credentialsProvider =
                    new UsernamePasswordCredentialsProvider(githubToken, "");

            if (localDir.exists()) {
                System.out.println("Pulling latest for: " + localPath);
                Git git = Git.open(localDir);
                git.pull().setCredentialsProvider(credentialsProvider).call();
            } else {
                System.out.println("Cloning: " + repoUrl);
                Git.cloneRepository()
                        .setURI(repoUrl)
                        .setDirectory(localDir)
                        .setCredentialsProvider(credentialsProvider)
                        .call();
            }
            System.out.println("✓ Repo ready: " + localPath);
        } catch (Exception e) {
            System.err.println("Clone/pull error: " + e.getMessage());
            e.printStackTrace();
        }
        return localPath;
    }

    /**
     * Checkout branch
     */
    private void checkoutBranch(String repoLocalPath, String branchName) {
        try {
            Git git = Git.open(new File(repoLocalPath));
            git.checkout().setName(branchName).call();
            System.out.println("✓ Checked out: " + branchName);
        } catch (Exception e) {
            System.err.println("Checkout error: " + e.getMessage());
        }
    }

    /**
     * Test endpoint
     */
    @PostMapping("/pr-test")
    public ResponseEntity<String> handleTest(@RequestBody Map<String, Object> payload) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();
            String prettyJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
            System.out.println(prettyJson);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.ok("Webhook received");
    }
}
