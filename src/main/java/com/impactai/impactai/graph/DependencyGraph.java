package com.impactai.impactai.graph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

public class DependencyGraph {

    private static final Logger logger = LoggerFactory.getLogger(DependencyGraph.class);

    private final Map<String, GraphNode> nodeMap = new HashMap<>();

    /**
     * Add a node to the graph
     */
    public void addNode(GraphNode node) {
        if (node == null) {
            logger.warn("Attempted to add null node");
            return;
        }
        nodeMap.put(node.getId(), node);
        logger.debug("Added node to graph: {}", node.getId());
    }

    /**
     * Get a node by ID
     */
    public GraphNode getNode(String nodeId) {
        return nodeMap.get(nodeId);
    }

    /**
     * Get all nodes in the graph
     * CRITICAL: This method is used everywhere for graph traversal
     */
    public Map<String, GraphNode> getNodeMap() {
        return nodeMap;
    }

    /**
     * Get neighbor IDs for a given node (used in graph traversal)
     */
    public List<String> getNeighborIds(String nodeId) {
        GraphNode node = nodeMap.get(nodeId);
        if (node == null) {
            logger.debug("Node not found: {}", nodeId);
            return Collections.emptyList();
        }

        List<String> neighborIds = new ArrayList<>();
        if (node.getNeighbors() != null) {
            for (GraphNode neighbor : node.getNeighbors()) {
                neighborIds.add(neighbor.getId());
            }
        }
        return neighborIds;
    }

    /**
     * Add an edge between two nodes
     */
    public void addEdge(String fromNodeId, String toNodeId) {
        GraphNode fromNode = nodeMap.get(fromNodeId);
        GraphNode toNode = nodeMap.get(toNodeId);

        if (fromNode == null) {
            logger.warn("Source node not found for edge: {}", fromNodeId);
            return;
        }
        if (toNode == null) {
            logger.warn("Target node not found for edge: {}", toNodeId);
            return;
        }

        fromNode.addNeighbor(toNode);
        logger.debug("Added edge: {} → {}", fromNodeId, toNodeId);
    }

    /**
     * Get total node count
     */
    public int getNodeCount() {
        return nodeMap.size();
    }

    /**
     * Clear the graph
     */
    public void clear() {
        nodeMap.clear();
        logger.info("Graph cleared");
    }

    /**
     * Print graph structure (for debugging)
     */
    public void printGraph() {
        logger.info("========== GRAPH STRUCTURE ==========");
        logger.info("Total nodes: {}", nodeMap.size());
        for (GraphNode node : nodeMap.values()) {
            logger.info(node.toString());
        }
        logger.info("=====================================");
    }

    @Override
    public String toString() {
        return String.format("DependencyGraph{nodes=%d}", nodeMap.size());
    }
}
