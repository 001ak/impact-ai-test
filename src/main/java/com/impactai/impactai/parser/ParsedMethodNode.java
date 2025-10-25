package com.impactai.impactai.parser;

import java.util.List;

public class ParsedMethodNode {
    private String methodName;
    private String className;
    private List<String> calledMethods; // fully-qualified, e.g. "com.example.B.method2"
    private List<String> annotations;


    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public List<String> getCalledMethods() {
        return calledMethods;
    }

    public void setCalledMethods(List<String> calledMethods) {
        this.calledMethods = calledMethods;
    }

    public List<String> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(List<String> annotations) {
        this.annotations = annotations;
    }


    // Getters & setters...
}
