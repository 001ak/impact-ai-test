package com.impactai.impactai.service;

import com.impactai.impactai.model.PRChangeInfo;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class PRParserService {

    public List<PRChangeInfo> parseGithubPayload(Map<String, Object> payload) {
        List<PRChangeInfo> result = new ArrayList<>();
        Object files = payload.get("pull_request");
        if (files instanceof Map) {
            Map<String, Object> prMap = (Map<String, Object>) files;
            if (prMap.containsKey("files")) {
                List<Map<String, Object>> filesList = (List<Map<String, Object>>) prMap.get("files");
                for (Map<String, Object> fileObj : filesList) {
                    String path = (String) fileObj.get("filename");
                    String status = (String) fileObj.get("status"); // "added", "modified", "removed", "renamed"
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
        }
        return result;
    }

    // Similar function for Bitbucket (you can implement when you receive a Bitbucket payload sample!)
    public List<PRChangeInfo> parseBitbucketPayload(Map<String, Object> payload) {
        // Placeholder
        return Collections.emptyList();
    }
}
