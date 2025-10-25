package com.impactai.impactai.service;

import com.impactai.impactai.parser.ParsedDependencyNode;
import com.impactai.impactai.graph.DependencyGraph;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class GraphBuilderService {
    private DependencyGraph graph;

    public void build(List<ParsedDependencyNode> parsedNodes) {
        this.graph = new DependencyGraph();
        graph.buildGraph(parsedNodes);
    }

    public List<String> getImpactFromMethod(String methodId) {
        if (graph == null) return List.of();
        return graph.traceDownstreamImpacts(methodId);
    }
}
