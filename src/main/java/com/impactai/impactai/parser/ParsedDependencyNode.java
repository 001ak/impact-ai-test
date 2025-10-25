package com.impactai.impactai.parser;

import java.util.List;

public class ParsedDependencyNode {

    private String name;
    private String type; // "class", "interface", "bean", "controller", etc.
    private List<String> annotations;
    private List<String> extendsImplements;
    private List<String> injectedDependencies;
    private List<String> calledClasses;
    private List<String> endpoints;

    private List<ParsedMethodNode> methods;
    public List<ParsedMethodNode> getMethods() { return methods; }
    public void setMethods(List<ParsedMethodNode> methods) { this.methods = methods; }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(List<String> annotations) {
        this.annotations = annotations;
    }

    public List<String> getExtendsImplements() {
        return extendsImplements;
    }

    public void setExtendsImplements(List<String> extendsImplements) {
        this.extendsImplements = extendsImplements;
    }

    public List<String> getInjectedDependencies() {
        return injectedDependencies;
    }

    public void setInjectedDependencies(List<String> injectedDependencies) {
        this.injectedDependencies = injectedDependencies;
    }

    public List<String> getCalledClasses() {
        return calledClasses;
    }

    public void setCalledClasses(List<String> calledClasses) {
        this.calledClasses = calledClasses;
    }

    public List<String> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(List<String> endpoints) {
        this.endpoints = endpoints;
    }


    // getters/setters...
}
