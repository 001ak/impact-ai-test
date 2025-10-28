package com.impactai.impactai.parser;

import java.util.List;

public class ParsedMethodNode {
    private String methodName;
    private String className;
    private List<String> calledMethods;
    private List<String> annotations;
    private int startLine;
    private int endLine;

    // NEW: Enhanced fields for risk calculation
    private boolean isCommentOnly = false;
    private boolean isCriticalMethod = false;
    private int methodComplexity = 0; // Count of methods called

    // Existing getters/setters...
    public String getMethodName() { return methodName; }
    public void setMethodName(String n) { this.methodName = n; }
    public String getClassName() { return className; }
    public void setClassName(String n) { this.className = n; }
    public List<String> getCalledMethods() { return calledMethods; }
    public void setCalledMethods(List<String> cm) { this.calledMethods = cm; }
    public List<String> getAnnotations() { return annotations; }
    public void setAnnotations(List<String> a) { this.annotations = a; }
    public int getStartLine() { return startLine; }
    public void setStartLine(int n) { this.startLine = n; }
    public int getEndLine() { return endLine; }
    public void setEndLine(int n) { this.endLine = n; }

    // NEW: Enhanced getters/setters
    public boolean isCommentOnly() { return isCommentOnly; }
    public void setCommentOnly(boolean b) { this.isCommentOnly = b; }
    public boolean isCriticalMethod() { return isCriticalMethod; }
    public void setCriticalMethod(boolean b) { this.isCriticalMethod = b; }
    public int getMethodComplexity() { return methodComplexity; }
    public void setMethodComplexity(int c) { this.methodComplexity = c; }
}
