package com.impactai.impactai.graph;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.io.*;

public class DependencyGraph {

    private static final Logger logger = LoggerFactory.getLogger(DependencyGraph.class);

    private final Map<String, GraphNode> nodeMap = new HashMap<>();

    // Existing methods (unchanged):

    public void addNode(GraphNode node) {
        if (node == null) {
            logger.warn("Attempted to add null node");
            return;
        }
        nodeMap.put(node.getId(), node);
        logger.debug("Added node to graph: {}", node.getId());
    }

    public GraphNode getNode(String nodeId) {
        return nodeMap.get(nodeId);
    }

    public Map<String, GraphNode> getNodeMap() {
        return nodeMap;
    }

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

    public int getNodeCount() {
        return nodeMap.size();
    }

    public void clear() {
        nodeMap.clear();
        logger.info("Graph cleared");
    }

    /**
     * Console-friendly, readable printout for large graphs.
     */
    public void printGraphSummary() {
        System.out.println("========== GRAPH STRUCTURE SUMMARY ==========");
        System.out.println("Total nodes: " + nodeMap.size());
        for (GraphNode node : nodeMap.values()) {
            System.out.printf(
                    "[%s] %s | calls: %-3d | ann: %-2d | neighbors: %s\n",
                    node.getType(),
                    node.getId(),
                    node.getCalledMethods() != null ? node.getCalledMethods().size() : 0,
                    node.getAnnotations() != null ? node.getAnnotations().size() : 0,
                    node.getNeighbors() != null ?
                            node.getNeighbors().stream().map(GraphNode::getId).toList()
                            : "[]"
            );
        }
        System.out.println("=============================================");
    }

    /**
     * Export graph as Cytoscape.js-friendly JSON: {"nodes":[...],"edges":[...]}
     */
    public void exportJson(String filename) throws IOException, JSONException {
        JSONArray nodesArr = new JSONArray();
        JSONArray edgesArr = new JSONArray();

        // Nodes
        for (GraphNode node : nodeMap.values()) {
            JSONObject n = new JSONObject();
            n.put("id", node.getId());
            n.put("type", node.getType());
            n.put("label", node.getName());
            n.put("calls", node.getCalledMethods() != null ? node.getCalledMethods().size() : 0);
            n.put("annotations", node.getAnnotations());
            nodesArr.put(n);
        }
        // Edges
        for (GraphNode node : nodeMap.values()) {
            if (node.getNeighbors() != null) {
                for (GraphNode neighbor : node.getNeighbors()) {
                    JSONObject e = new JSONObject();
                    e.put("source", node.getId());
                    e.put("target", neighbor.getId());
                    edgesArr.put(e);
                }
            }
        }
        JSONObject root = new JSONObject();
        root.put("nodes", nodesArr);
        root.put("edges", edgesArr);

        try (PrintWriter out = new PrintWriter(filename)) {
            out.println(root.toString(2)); // Pretty print with 2-space indent
        }
        logger.info("Exported graph to JSON: {}", filename);
    }

    /**
     * Full debug print - for legacy/testing
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
