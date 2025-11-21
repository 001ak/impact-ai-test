package com.impactai.impactai.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class GitHubCommentService {

    @Value("${github.token}")
    private String githubToken;

    public void postComment(String owner, String repo, int prNumber, String markdownBody) {
        String url = String.format("https://api.github.com/repos/%s/%s/issues/%d/comments", owner, repo, prNumber);

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "token " + githubToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", "application/vnd.github+json");

        String bodyJson = String.format("{\"body\": \"%s\"}", markdownBody.replace("\"", "\\\"").replace("\n", "\\n"));

        HttpEntity<String> req = new HttpEntity<>(bodyJson, headers);
        restTemplate.postForEntity(url, req, String.class);
    }
}
