package com.impactai.impactai.service;

import com.impactai.impactai.model.RepoMetadata;
import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RepoMetadataService {
    private final ConcurrentHashMap<String, RepoMetadata> repoMap = new ConcurrentHashMap<>();

    public boolean isRepoFullyParsed(String repoFullName) {
        RepoMetadata metadata = repoMap.get(repoFullName);
        return metadata != null && metadata.isFullyParsed();
    }

    public void markRepoAsFullyParsed(String repoFullName, String commitSha) {
        RepoMetadata metadata = repoMap.getOrDefault(repoFullName, new RepoMetadata());
        metadata.setRepoFullName(repoFullName);
        metadata.setFullyParsed(true);
        metadata.setLastParsedCommitSha(commitSha);
        metadata.setLastParsedTimestamp(System.currentTimeMillis());
        repoMap.put(repoFullName, metadata);
    }

    public RepoMetadata getMetadata(String repoFullName) {
        return repoMap.get(repoFullName);
    }
}
