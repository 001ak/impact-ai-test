package com.impactai.impactai.model;

import java.util.List;

import static com.impactai.impactai.model.PRChangeInfo.ChangeType.*;

public class PRChangeInfo {
    public enum ChangeType { ADDED, MODIFIED, DELETED, RENAMED }

    private String filePath;
    private ChangeType changeType;

    // NEW: Enhanced fields for line-level analysis
    private String patch; // unified diff from GitHub
    private List<LineRange> changedLines; // set after patch parsing

    // Constructors
    public PRChangeInfo() {}

    // Original constructor (backward compatibility)
    public PRChangeInfo(String filePath, ChangeType changeType) {
        this.filePath = filePath;
        this.changeType = changeType;
    }

    // Enhanced constructor
    public PRChangeInfo(String filePath, ChangeType changeType, String patch) {
        this.filePath = filePath;
        this.changeType = changeType;
        this.patch = patch;
    }

    // Original getters/setters (unchanged)
    public String getFilePath() { return filePath; }
    public ChangeType getChangeType() { return changeType; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public void setChangeType(ChangeType changeType) { this.changeType = changeType; }

    // NEW: Enhanced getters/setters
    public String getPatch() { return patch; }
    public void setPatch(String patch) { this.patch = patch; }
    public List<LineRange> getChangedLines() { return changedLines; }
    public void setChangedLines(List<LineRange> changedLines) { this.changedLines = changedLines; }

    // Utility method to convert GitHub status string to enum
    public static ChangeType parseChangeType(String status) {
        if (status == null) return MODIFIED;
        switch (status.toLowerCase()) {
            case "added": return ChangeType.ADDED;
            case "modified": return MODIFIED;
            case "deleted": return DELETED;
            case "renamed": return RENAMED;
            default: return MODIFIED;
        }
    }

    @Override
    public String toString() {
        return "PRChangeInfo{" +
                "filePath='" + filePath + '\'' +
                ", changeType=" + changeType +
                ", hasChangedLines=" + (changedLines != null ? changedLines.size() : 0) +
                '}';
    }
}
