package com.impactai.impactai.graph;

import java.util.*;

public class GraphNode {
    private String id; // unique: e.g., "com.example.A.method1" or "com.example.A"
    private String type; // "class", "method"
    private String name; // readable name
    private Set<GraphNode> neighbors = new HashSet<>();

    public GraphNode(String id, String type, String name) {
        this.id = id;
        this.type = type;
        this.name = name;
    }
    public String getId() { return id; }
    public String getType() { return type; }
    public String getName() { return name; }
    public Set<GraphNode> getNeighbors() { return neighbors; }
    public void addNeighbor(GraphNode node) { neighbors.add(node); }
    @Override public String toString() {
        return String.format("[%s] %s => %s", type, id, neighbors.stream().map(GraphNode::getId).toList());
    }
}
