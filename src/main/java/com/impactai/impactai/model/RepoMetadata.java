package com.impactai.impactai.model;

public class RepoMetadata {
    private String repoFullName;
    private boolean isFullyParsed;
    private String lastParsedCommitSha;
    private long lastParsedTimestamp;

    // Getters and setters
    public String getRepoFullName() { return repoFullName; }
    public void setRepoFullName(String repoFullName) { this.repoFullName = repoFullName; }
    public boolean isFullyParsed() { return isFullyParsed; }
    public void setFullyParsed(boolean fullyParsed) { isFullyParsed = fullyParsed; }
    public String getLastParsedCommitSha() { return lastParsedCommitSha; }
    public void setLastParsedCommitSha(String lastParsedCommitSha) { this.lastParsedCommitSha = lastParsedCommitSha; }
    public long getLastParsedTimestamp() { return lastParsedTimestamp; }
    public void setLastParsedTimestamp(long lastParsedTimestamp) { this.lastParsedTimestamp = lastParsedTimestamp; }
}
