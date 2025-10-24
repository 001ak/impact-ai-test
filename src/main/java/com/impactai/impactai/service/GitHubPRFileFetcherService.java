package com.impactai.impactai.service;

import com.impactai.impactai.model.PRChangeInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class GitHubPRFileFetcherService {

    @Value("${github.token}")
    private String githubToken;

    public List<PRChangeInfo> fetchChangedFiles(String owner, String repo, int prNumber) {
        String url = String.format("https://api.github.com/repos/%s/%s/pulls/%d/files", owner, repo, prNumber);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "token " + githubToken);
        headers.set("Accept", "application/vnd.github+json");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<List> resp = restTemplate.exchange(url, HttpMethod.GET, entity, List.class);

        List<PRChangeInfo> result = new ArrayList<>();
        if (resp.getStatusCode().is2xxSuccessful()) {
            List<Map<String, Object>> files = resp.getBody();
            for (Map<String, Object> file : files) {
                String path = (String) file.get("filename");
                String status = (String) file.get("status"); // "added", "modified", "removed", "renamed"
                PRChangeInfo.ChangeType type;
                switch (status) {
                    case "added":    type = PRChangeInfo.ChangeType.ADDED; break;
                    case "removed":  type = PRChangeInfo.ChangeType.DELETED; break;
                    case "renamed":  type = PRChangeInfo.ChangeType.RENAMED; break;
                    default:         type = PRChangeInfo.ChangeType.MODIFIED;
                }
                result.add(new PRChangeInfo(path, type));
            }
        }
        return result;
    }
}
