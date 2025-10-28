package com.impactai.impactai.model;

public class LineRange {
    private final int startLine;
    private final int endLine;

    public LineRange(int startLine, int endLine) {
        this.startLine = startLine;
        this.endLine = endLine;
    }

    public int getStartLine() { return startLine; }
    public int getEndLine() { return endLine; }

    @Override
    public String toString() {
        return "[" + startLine + ", " + endLine + "]";
    }
}
