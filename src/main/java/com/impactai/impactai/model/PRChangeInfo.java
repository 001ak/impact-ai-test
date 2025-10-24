package com.impactai.impactai.model;

public class PRChangeInfo {
    public enum ChangeType { ADDED, MODIFIED, DELETED, RENAMED }
    private String filePath;
    private ChangeType changeType;

    public PRChangeInfo() {}

    public PRChangeInfo(String filePath, ChangeType changeType) {
        this.filePath = filePath;
        this.changeType = changeType;
    }

    public String getFilePath() { return filePath; }
    public ChangeType getChangeType() { return changeType; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public void setChangeType(ChangeType changeType) { this.changeType = changeType; }
}
