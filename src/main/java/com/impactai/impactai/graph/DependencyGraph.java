package com.impactai.impactai.graph;

import com.impactai.impactai.parser.ParsedDependencyNode;
import com.impactai.impactai.parser.ParsedMethodNode;
import java.util.*;

public class DependencyGraph {
    private final Map<String, GraphNode> nodeMap = new HashMap<>();

    public void buildGraph(List<ParsedDependencyNode> parsedNodes) {
        // 1. Create nodes
        for (ParsedDependencyNode parsed : parsedNodes) {
            String classId = parsed.getName();
            nodeMap.putIfAbsent(classId, new GraphNode(classId, "class", parsed.getName()));

            // Add method nodes
            if (parsed.getMethods() != null) {
                for (ParsedMethodNode method : parsed.getMethods()) {
                    String methodId = method.getClassName() + "." + method.getMethodName();
                    nodeMap.putIfAbsent(methodId, new GraphNode(methodId, "method", methodId));
                }
            }
        }

        // 2. Create edges: inheritance/deps/calls
        for (ParsedDependencyNode parsed : parsedNodes) {
            GraphNode classNode = nodeMap.get(parsed.getName());
            for (String dep : parsed.getInjectedDependencies()) {
                if (nodeMap.containsKey(dep)) classNode.addNeighbor(nodeMap.get(dep));
            }
            for (String ext : parsed.getExtendsImplements()) {
                if (nodeMap.containsKey(ext)) classNode.addNeighbor(nodeMap.get(ext));
            }
            if (parsed.getMethods() != null) {
                for (ParsedMethodNode method : parsed.getMethods()) {
                    GraphNode methodNode = nodeMap.get(method.getClassName() + "." + method.getMethodName());
                    // Connect method to its class
                    methodNode.addNeighbor(classNode);
                    // Connect method calls
                    if (method.getCalledMethods() != null) {
                        for (String called : method.getCalledMethods()) {
                            if (nodeMap.containsKey(called)) methodNode.addNeighbor(nodeMap.get(called));
                        }
                    }
                }
            }
        }
    }

    public GraphNode getNode(String id) { return nodeMap.get(id); }

    public List<String> traceDownstreamImpacts(String nodeId) {
        List<String> impacted = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Queue<GraphNode> queue = new LinkedList<>();
        if (!nodeMap.containsKey(nodeId)) return impacted;
        queue.add(nodeMap.get(nodeId)); visited.add(nodeId);
        while (!queue.isEmpty()) {
            GraphNode current = queue.poll();
            impacted.add(current.getId());
            for (GraphNode neighbor : current.getNeighbors()) {
                if (!visited.contains(neighbor.getId())) {
                    visited.add(neighbor.getId());
                    queue.add(neighbor);
                }
            }
        }
        return impacted;
    }
}
