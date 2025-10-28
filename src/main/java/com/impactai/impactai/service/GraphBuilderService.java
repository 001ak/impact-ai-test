package com.impactai.impactai.service;

import com.impactai.impactai.graph.GraphNode;
import com.impactai.impactai.parser.ParsedDependencyNode;
import com.impactai.impactai.graph.DependencyGraph;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GraphBuilderService {
    private DependencyGraph graph;

    public Map<String, GraphNode> getNodeMap() {
        return nodeMap;
    }

    private final Map<String, GraphNode> nodeMap = new HashMap<>();

    public void build(List<ParsedDependencyNode> parsedNodes) {
        this.graph = new DependencyGraph();
        graph.buildGraph(parsedNodes);
    }

    public DependencyGraph getGraph() {
        if (this.graph == null) throw new IllegalStateException("Graph not yet built!");
        return this.graph;
    }

    public List<String> getImpactFromMethod(String methodId) {
        if (graph == null) return List.of();
        return graph.traceDownstreamImpacts(methodId);
    }
}
