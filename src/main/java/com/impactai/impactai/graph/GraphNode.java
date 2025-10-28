package com.impactai.impactai.graph;

import java.util.*;

public class GraphNode {
    private String id; // unique: e.g., "com.example.A.method1" or "com.example.A"
    private String type; // "class", "method"
    private String name; // readable name
    private Set<GraphNode> neighbors = new HashSet<>();

    // NEW: Enhanced fields for risk calculation
    private List<String> calledMethods = new ArrayList<>();
    private List<String> annotations = new ArrayList<>();

    public GraphNode(String id, String type, String name) {
        this.id = id;
        this.type = type;
        this.name = name;
    }

    // Existing getters
    public String getId() { return id; }
    public String getType() { return type; }
    public String getName() { return name; }
    public Set<GraphNode> getNeighbors() { return neighbors; }
    public void addNeighbor(GraphNode node) { neighbors.add(node); }

    // NEW: Enhanced getters/setters
    public List<String> getCalledMethods() { return calledMethods; }
    public void setCalledMethods(List<String> calledMethods) {
        this.calledMethods = calledMethods != null ? calledMethods : new ArrayList<>();
    }
    public void addCalledMethod(String method) {
        if (method != null && !method.isEmpty()) {
            this.calledMethods.add(method);
        }
    }

    public List<String> getAnnotations() { return annotations; }
    public void setAnnotations(List<String> annotations) {
        this.annotations = annotations != null ? annotations : new ArrayList<>();
    }
    public void addAnnotation(String annotation) {
        if (annotation != null && !annotation.isEmpty()) {
            this.annotations.add(annotation);
        }
    }

    @Override
    public String toString() {
        return String.format("[%s] %s => neighbors: %s, calls: %d, annotations: %d",
                type, id, neighbors.stream().map(GraphNode::getId).toList(),
                calledMethods.size(), annotations.size());
    }
}
