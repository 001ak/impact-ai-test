package com.impactai.impactai.service;

import com.impactai.impactai.model.PRChangeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

import java.util.*;

@Service
public class GitHubPRFileFetcherService {

    private static final Logger logger = LoggerFactory.getLogger(GitHubPRFileFetcherService.class);

    @Value("${github.token}")
    private String githubToken;

    /**
     * Fetch changed files for a PR with PATCH data for line-level detection
     *
     * API: GET /repos/{owner}/{repo}/pulls/{pr_number}/files
     * Returns: List of files with filename, status, patch, etc.
     */
    public List<PRChangeInfo> fetchChangedFiles(String owner, String repo, int prNumber) {
        List<PRChangeInfo> changedFiles = new ArrayList<>();

        if (owner == null || owner.isEmpty() || repo == null || repo.isEmpty()) {
            logger.error("Invalid owner or repo: owner={}, repo={}", owner, repo);
            return changedFiles;
        }

        if (prNumber <= 0) {
            logger.error("Invalid PR number: {}", prNumber);
            return changedFiles;
        }

        String url = String.format("https://api.github.com/repos/%s/%s/pulls/%d/files", owner, repo, prNumber);
        logger.debug("Fetching PR files from GitHub API: {}", url);

        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "token " + githubToken);
            headers.set("Accept", "application/vnd.github+json");
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> request = new HttpEntity<>(headers);

            ResponseEntity<Map[]> response = restTemplate.exchange(url, HttpMethod.GET, request, Map[].class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                logger.warn("GitHub API returned status: {}", response.getStatusCode());
                return changedFiles;
            }

            Map[] files = response.getBody();
            if (files == null || files.length == 0) {
                logger.info("No changed files found for PR#{}", prNumber);
                return changedFiles;
            }

            logger.info("Fetched {} changed files for PR#{}", files.length, prNumber);

            // Parse each file entry
            for (Map<String, Object> file : files) {
                try {
                    PRChangeInfo changeInfo = parseFileEntry(file);
                    if (changeInfo != null) {
                        changedFiles.add(changeInfo);
                        logger.debug("Added changed file: {} (status: {}, patch size: {} bytes)",
                                changeInfo.getFilePath(),
                                changeInfo.getChangeType(),
                                changeInfo.getPatch() != null ? changeInfo.getPatch().length() : 0);
                    }
                } catch (Exception e) {
                    logger.error("Error parsing file entry: {}", e.getMessage());
                    // Continue processing other files
                }
            }

            logger.info("Successfully processed {} changed files for PR#{}", changedFiles.size(), prNumber);
            return changedFiles;

        } catch (HttpClientErrorException.Forbidden e) {
            logger.error("GitHub API access forbidden (403): {}. Check token permissions.", e.getMessage());
            return changedFiles;
        } catch (HttpClientErrorException.Unauthorized e) {
            logger.error("GitHub API authentication failed (401): {}. Check token validity.", e.getMessage());
            return changedFiles;
        } catch (HttpClientErrorException e) {
            logger.error("GitHub API error ({}): {}", e.getStatusCode(), e.getMessage());
            return changedFiles;
        } catch (Exception e) {
            logger.error("Unexpected error fetching changed files for PR#{}: {}", prNumber, e.getMessage(), e);
            return changedFiles;
        }
    }

    /**
     * Parse a single file entry from GitHub API response
     *
     * GitHub returns:
     * {
     *   "filename": "src/main/java/MyFile.java",
     *   "status": "modified",
     *   "additions": 10,
     *   "deletions": 5,
     *   "changes": 15,
     *   "patch": "@@ -10,7 +10,9 @@\n ..."
     * }
     */
    private PRChangeInfo parseFileEntry(Map<String, Object> file) {
        try {
            String filePath = (String) file.get("filename");
            String statusStr = (String) file.get("status");
            String patch = (String) file.get("patch");

            if (filePath == null || filePath.isEmpty()) {
                logger.warn("File entry has no filename");
                return null;
            }

            logger.debug("Parsing file entry: filename={}, status={}", filePath, statusStr);

            // Convert GitHub status string to PRChangeInfo.ChangeType enum
            PRChangeInfo.ChangeType changeType = PRChangeInfo.parseChangeType(statusStr);

            // Create PRChangeInfo object
            PRChangeInfo changeInfo = new PRChangeInfo(filePath, changeType, patch);

            // Log patch extraction
            if (patch != null && !patch.isEmpty()) {
                int lines = patch.split("\n").length;
                logger.debug("File {} has patch with {} lines", filePath, lines);
            } else {
                logger.warn("File {} has no patch data (status: {}). Will use fallback logic.", filePath, statusStr);
            }

            return changeInfo;

        } catch (ClassCastException e) {
            logger.error("Error parsing file entry - type mismatch: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            logger.error("Error parsing file entry: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Alternative: Fetch file content from specific SHA (if needed for downloading new version)
     * Format: GET /repos/{owner}/{repo}/contents/{path}?ref={sha}
     * Returns base64-encoded content
     */
    public String fetchFileContent(String owner, String repo, String filePath, String sha) {
        if (owner == null || repo == null || filePath == null || sha == null) {
            logger.error("Invalid parameters for file content fetch");
            return null;
        }

        String url = String.format("https://api.github.com/repos/%s/%s/contents/%s?ref=%s",
                owner, repo, filePath, sha);
        logger.debug("Fetching file content from: {}", url);

        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "token " + githubToken);
            headers.set("Accept", "application/vnd.github+json");
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> request = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                logger.warn("GitHub API returned status: {} for file: {}", response.getStatusCode(), filePath);
                return null;
            }

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                logger.warn("Empty response for file: {}", filePath);
                return null;
            }

            String content = (String) responseBody.get("content");
            if (content == null || content.isEmpty()) {
                logger.warn("No content found for file: {}", filePath);
                return null;
            }

            // Decode base64
            String decodedContent = new String(java.util.Base64.getDecoder().decode(content));
            logger.debug("Successfully fetched content for file: {} ({} bytes)", filePath, decodedContent.length());
            return decodedContent;

        } catch (HttpClientErrorException.NotFound e) {
            logger.warn("File not found (404): {}", filePath);
            return null;
        } catch (HttpClientErrorException e) {
            logger.error("GitHub API error ({}): {} for file: {}", e.getStatusCode(), e.getMessage(), filePath);
            return null;
        } catch (Exception e) {
            logger.error("Error fetching file content for {}: {}", filePath, e.getMessage(), e);
            return null;
        }
    }
}
