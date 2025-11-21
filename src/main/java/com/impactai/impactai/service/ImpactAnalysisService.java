package com.impactai.impactai.service;

import com.impactai.impactai.graph.DependencyGraph;
import com.impactai.impactai.graph.GraphNode;
import com.impactai.impactai.util.ChangeAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class ImpactAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(ImpactAnalysisService.class);

    /**
     * Traverse the graph and return all impacted nodes
     * Collects complexity and annotation data for risk calculation
     */
    public ImpactReport analyzeImpact(DependencyGraph graph, List<String> changedNodeIds) {
        Set<String> allImpacted = new HashSet<>();
        int maxDepth = 0;

        Map<String, List<String>> impactedByNode = new HashMap<>();
        Map<String, Integer> nodeComplexity = new HashMap<>();
        Map<String, List<String>> nodeAnnotations = new HashMap<>();

        logger.debug("Starting impact analysis for {} changed nodes", changedNodeIds.size());

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

                    // Collect node data
                    try {
                        GraphNode graphNode = graph.getNodeMap().get(current);
                        if (graphNode != null) {
                            // Store complexity
                            int complexity = graphNode.getCalledMethods() != null ?
                                    graphNode.getCalledMethods().size() : 0;
                            nodeComplexity.put(current, complexity);

                            // Store annotations
                            List<String> annotations = graphNode.getAnnotations() != null ?
                                    new ArrayList<>(graphNode.getAnnotations()) : new ArrayList<>();
                            nodeAnnotations.put(current, annotations);

                            logger.debug("Node {}: complexity={}, annotations={}",
                                    current, complexity, annotations.size());
                        }
                    } catch (Exception e) {
                        logger.warn("Error collecting data for node {}: {}", current, e.getMessage());
                    }

                    // Traverse neighbors
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

        logger.info("Impact analysis complete: {} total impacted nodes, max depth {}",
                allImpacted.size(), maxDepth);

        return new ImpactReport(
                changedNodeIds,
                new ArrayList<>(allImpacted),
                maxDepth,
                impactedByNode,
                nodeComplexity,
                nodeAnnotations,
                false,
                false
        );
    }

    /**
     * Sophisticated risk calculation with multiple factors
     */
    public String calculateRisk(ImpactReport report) {
        logger.info("========== RISK CALCULATION START ==========");

        // Override 1: Comment-only changes
        if (report.hasCommentOnlyChanges()) {
            logger.info("✓ Comment-only change detected → Forcing LOW risk");
            return "LOW";
        }

        // Override 2: Critical method with high impact
        if (report.hasCriticalMethodChanges() && report.getImpactDepth() >= 3) {
            logger.info("✓ Critical method with depth >= 3 → Forcing CRITICAL risk");
            return "CRITICAL";
        }

        // Calculate base risk
        double baseRisk = 1.0; // MEDIUM baseline
        logger.debug("Base risk: {}", baseRisk);

        // Apply multipliers
        double depthMultiplier = ChangeAnalyzer.getDepthMultiplier(report.getImpactDepth());
        logger.debug("Depth multiplier (depth={}): {}", report.getImpactDepth(), depthMultiplier);

        int affectedCount = report.getAllImpactedNodes().size() - report.getChangedNodes().size();
        double affectedMultiplier = ChangeAnalyzer.getAffectedMultiplier(affectedCount);
        logger.debug("Affected multiplier (affected={}): {}", affectedCount, affectedMultiplier);

        int totalComplexity = 0;
        int changedCount = 0;
        for (String changedId : report.getChangedNodes()) {
            Integer complexity = report.getNodeComplexity().getOrDefault(changedId, 0);
            totalComplexity += complexity;
            changedCount++;
            logger.debug("Changed node {}: complexity={}", changedId, complexity);
        }
        double avgComplexity = changedCount > 0 ? (double) totalComplexity / changedCount : 0;
        double complexityMultiplier = ChangeAnalyzer.getComplexityMultiplier((int) avgComplexity);
        logger.debug("Complexity multiplier (avg={}): {}", avgComplexity, complexityMultiplier);

        // Critical annotation multiplier
        double criticalMultiplier = 1.0;
        boolean foundCritical = false;
        for (String changedId : report.getChangedNodes()) {
            List<String> annotations = report.getNodeAnnotations().getOrDefault(changedId, new ArrayList<>());
            for (String annotation : annotations) {
                if (isCriticalAnnotation(annotation)) {
                    criticalMultiplier = 3.0;
                    foundCritical = true;
                    logger.info("✓ Critical annotation found in {}: {}", changedId, annotation);
                    break;
                }
            }
            if (foundCritical) break;
        }
        logger.debug("Critical multiplier: {}", criticalMultiplier);

        // Calculate final score
        double finalScore = baseRisk * depthMultiplier * affectedMultiplier *
                complexityMultiplier * criticalMultiplier;

        logger.info("Final score: {} × {} × {} × {} × {} = {}",
                baseRisk, depthMultiplier, affectedMultiplier, complexityMultiplier,
                criticalMultiplier, finalScore);

        String risk = mapScoreToRisk(finalScore);
        logger.info("========== RISK LEVEL: {} ==========", risk);

        return risk;
    }

    private String mapScoreToRisk(double score) {
        if (score <= 1.0) return "LOW";
        if (score <= 2.0) return "MEDIUM";
        if (score <= 3.5) return "HIGH";
        return "CRITICAL";
    }

    private boolean isCriticalAnnotation(String annotation) {
        String[] criticalAnnotations = {
                "Transactional", "CacheEvict", "Cacheable", "CachePut", "Scheduled",
                "Async", "EventListener", "PreAuthorize", "Secured", "RolesAllowed",
                "PostMapping", "PutMapping", "DeleteMapping", "PatchMapping"
        };

        for (String critical : criticalAnnotations) {
            if (annotation.contains(critical)) return true;
        }
        return false;
    }

    /**
     * Impact Report class
     */
    public static class ImpactReport {
        private final List<String> changedNodes;
        private final List<String> allImpactedNodes;
        private final int impactDepth;
        private final Map<String, List<String>> impactedByNode;
        private final Map<String, Integer> nodeComplexity;
        private final Map<String, List<String>> nodeAnnotations;
        private boolean hasCommentOnlyChanges;
        private boolean hasCriticalMethodChanges;

        public ImpactReport(List<String> changedNodes, List<String> allImpactedNodes, int impactDepth,
                            Map<String, List<String>> impactedByNode, Map<String, Integer> nodeComplexity,
                            Map<String, List<String>> nodeAnnotations, boolean hasCommentOnlyChanges,
                            boolean hasCriticalMethodChanges) {
            this.changedNodes = changedNodes;
            this.allImpactedNodes = allImpactedNodes;
            this.impactDepth = impactDepth;
            this.impactedByNode = impactedByNode;
            this.nodeComplexity = nodeComplexity;
            this.nodeAnnotations = nodeAnnotations;
            this.hasCommentOnlyChanges = hasCommentOnlyChanges;
            this.hasCriticalMethodChanges = hasCriticalMethodChanges;
        }

        public List<String> getChangedNodes() { return changedNodes; }
        public List<String> getAllImpactedNodes() { return allImpactedNodes; }
        public int getImpactDepth() { return impactDepth; }
        public Map<String, List<String>> getImpactedByNode() { return impactedByNode; }
        public Map<String, Integer> getNodeComplexity() { return nodeComplexity; }
        public Map<String, List<String>> getNodeAnnotations() { return nodeAnnotations; }
        public boolean hasCommentOnlyChanges() { return hasCommentOnlyChanges; }
        public boolean hasCriticalMethodChanges() { return hasCriticalMethodChanges; }
        public void setHasCommentOnlyChanges(boolean b) { this.hasCommentOnlyChanges = b; }
        public void setHasCriticalMethodChanges(boolean b) { this.hasCriticalMethodChanges = b; }
    }
}
