package com.impactai.impactai.controller;
import com.impactai.impactai.model.PRChangeInfo;
import com.impactai.impactai.parser.ParsedDependencyNode;
import com.impactai.impactai.service.*;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.ArrayList;
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

    @Autowired
    private DependencyParserService dependencyParserService;

    @Autowired
    private GraphBuilderService graphBuilderService;

    @Autowired
    private RepoMetadataService repoMetadataService;

    @Autowired
    private RepoParserService repoParserService;

    @Value("${github.token}")
    private String githubToken;

    @PostMapping("/pr")
    public ResponseEntity<?> handlePRWebhook(@RequestBody Map<String, Object> payload) {

        // Check if this is a pull request event
        if (!payload.containsKey("pull_request")) {
            System.out.println("Ignoring non-PR webhook event");
            return ResponseEntity.ok("Event ignored (not a PR event)");
        }

        Map<String, Object> pr = (Map<String, Object>) payload.get("pull_request");
        Map<String, Object> repo = (Map<String, Object>) payload.get("repository");

        // Null safety checks
        if (pr == null || repo == null) {
            System.err.println("Invalid webhook payload: missing pr or repo data");
            return ResponseEntity.badRequest().body("Invalid payload");
        }

        String owner = ((Map<String, Object>) repo.get("owner")).get("login").toString();
        String repoName = repo.get("name").toString();
        String repoFullName = owner + "/" + repoName;
        int prNumber = Integer.parseInt(pr.get("number").toString());
        String headSha = ((Map<String, Object>) pr.get("head")).get("sha").toString();

        System.out.println("=== Processing PR #" + prNumber + " for " + repoFullName + " ===");

        List<ParsedDependencyNode> allParsedNodes;

        // Always ensure repo is cloned/updated locally first
        String repoLocalPath = cloneOrFetchRepo(owner, repoName);

        if (!repoMetadataService.isRepoFullyParsed(repoFullName)) {
            System.out.println("=== FIRST PR FOR THIS REPO: PERFORMING FULL SCAN ===");

            // Parse entire repo
            allParsedNodes = repoParserService.parseFullRepo(repoLocalPath);

            // Mark repo as fully parsed
            repoMetadataService.markRepoAsFullyParsed(repoFullName, headSha);

            System.out.println("Full scan complete. Parsed " + allParsedNodes.size() + " nodes.");
        } else {
            System.out.println("=== INCREMENTAL PARSING (PR DIFF ONLY) ===");

            // Fetch changed files for this PR
            List<PRChangeInfo> changedFiles = prFileFetcher.fetchChangedFiles(owner, repoName, prNumber);

            // Map relative GitHub paths to absolute local paths
            List<String> absolutePaths = new ArrayList<>();
            for (PRChangeInfo info : changedFiles) {
                if (info.getFilePath().endsWith(".java")) {
                    // Convert relative path to absolute local path
                    String absolutePath = repoLocalPath + File.separator + info.getFilePath();
                    absolutePaths.add(absolutePath);
                    System.out.println("Mapped: " + info.getFilePath() + " -> " + absolutePath);
                }
            }

            // Parse only changed files with absolute paths
            allParsedNodes = dependencyParserService.parseChangedFiles(absolutePaths);

            System.out.println("Incremental parse complete. Parsed " + allParsedNodes.size() + " nodes.");
        }

        // Build/update graph
        graphBuilderService.build(allParsedNodes);

        return ResponseEntity.ok(allParsedNodes);
    }




    private String cloneOrFetchRepo(String owner, String repoName) {
        String repoUrl = "https://github.com/" + owner + "/" + repoName + ".git";

        // Fix Windows path issue - use system temp dir
        String localPath = System.getProperty("java.io.tmpdir") + owner + "_" + repoName;
        File localDir = new File(localPath);

        try {
            // Create credentials provider with your GitHub token
            UsernamePasswordCredentialsProvider credentialsProvider =
                    new UsernamePasswordCredentialsProvider(githubToken, "");

            if (localDir.exists()) {
                System.out.println("Repo already cloned at: " + localPath + ", pulling latest...");
                Git git = Git.open(localDir);
                git.pull()
                        .setCredentialsProvider(credentialsProvider)
                        .call();
            } else {
                System.out.println("Cloning repo: " + repoUrl + " to " + localPath);
                Git.cloneRepository()
                        .setURI(repoUrl)
                        .setDirectory(localDir)
                        .setCredentialsProvider(credentialsProvider)
                        .call();
            }
            System.out.println("Repo ready at: " + localPath);
        } catch (Exception e) {
            System.err.println("Error cloning/pulling repo: " + e.getMessage());
            e.printStackTrace();
        }
        return localPath;
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


