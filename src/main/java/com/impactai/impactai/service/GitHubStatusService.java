package com.impactai.impactai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class GitHubStatusService {

    private static final Logger logger = LoggerFactory.getLogger(GitHubStatusService.class);

    @Value("${github.token}")
    private String githubToken;

    public void setStatus(String owner, String repo, String sha, String state, String description, String context) {
        try {
            String url = String.format("https://api.github.com/repos/%s/%s/statuses/%s", owner, repo, sha);

            Map<String, Object> payload = new HashMap<>();
            payload.put("state", state);                  // "success" or "failure"
            payload.put("description", description);      // e.g. "Impact-AI Risk: HIGH – Merge Blocked"
            payload.put("context", context);              // e.g. "Impact-AI Risk"
            // payload.put("target_url", "https://your-app/impact-report?id=..."); // optional

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(githubToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            RestTemplate restTemplate = new RestTemplate();

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            logger.info("GitHub Status set: {} ({}): {}", sha, state, description);
        } catch (Exception e) {
            logger.error("Failed to post status to GitHub: {}", e.getMessage());
        }
    }
}
