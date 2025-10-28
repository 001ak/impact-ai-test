package com.impactai.impactai.service;

import com.impactai.impactai.graph.DependencyGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class ImpactAnalysisService {
    private static final Logger logger = LoggerFactory.getLogger(ImpactAnalysisService.class);


    // Traverse the graph and return all impacted nodes for a list of changed classes/methods
    public ImpactReport analyzeImpact(DependencyGraph graph, List<String> changedNodeIds) {
        Set<String> allImpacted = new HashSet<>();
        int maxDepth = 0;

        Map<String, List<String>> impactedByNode = new HashMap<>();

        for (String changedId : changedNodeIds) {
            List<String> nodeImpact = new ArrayList<>();
            Queue<String> queue = new LinkedList<>();
            Set<String> visited = new HashSet<>();

            queue.add(changedId);
            visited.add(changedId);
            int localDepth = 0;

            while (!queue.isEmpty()) {
                int levelSize = queue.size();
                localDepth++;
                for (int i = 0; i < levelSize; i++) {
                    String current = queue.poll();
                    nodeImpact.add(current);
                    for (String neighbor : graph.getNeighborIds(current)) {
                        if (!visited.contains(neighbor)) {
                            visited.add(neighbor);
                            queue.add(neighbor);
                        }
                    }
                }
            }
            maxDepth = Math.max(maxDepth, localDepth);
            impactedByNode.put(changedId, nodeImpact);
            allImpacted.addAll(nodeImpact);
        }

        return new ImpactReport(changedNodeIds, new ArrayList<>(allImpacted), maxDepth, impactedByNode);
    }

    public static class ImpactReport {
        private final List<String> changedNodes;
        private final List<String> allImpactedNodes;
        private final int impactDepth;
        private final Map<String, List<String>> impactedByNode; // For reporting

        public ImpactReport(List<String> changedNodes, List<String> allImpactedNodes, int impactDepth, Map<String, List<String>> impactedByNode) {
            this.changedNodes = changedNodes;
            this.allImpactedNodes = allImpactedNodes;
            this.impactDepth = impactDepth;
            this.impactedByNode = impactedByNode;
        }

        // Getters...

        public List<String> getChangedNodes() { return changedNodes; }
        public List<String> getAllImpactedNodes() { return allImpactedNodes; }
        public int getImpactDepth() { return impactDepth; }
        public Map<String, List<String>> getImpactedByNode() { return impactedByNode; }
    }

    public String calculateRisk(ImpactAnalysisService.ImpactReport report) {
        int totalImpacted = report.getAllImpactedNodes().size();
        int depth = report.getImpactDepth();

        if (depth >= 3 || totalImpacted > 10) return "CRITICAL";
        if (depth == 2 || totalImpacted > 5) return "HIGH";
        if (depth == 1 || totalImpacted >= 2) return "MEDIUM";
        return "LOW";
    }

}
