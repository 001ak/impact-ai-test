package com.impactai.impactai.service;

import com.impactai.impactai.graph.DependencyGraph;
import com.impactai.impactai.graph.GraphNode;
import com.impactai.impactai.parser.ParsedDependencyNode;
import com.impactai.impactai.parser.ParsedMethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GraphBuilderService {

    private static final Logger logger = LoggerFactory.getLogger(GraphBuilderService.class);

    private DependencyGraph graph = new DependencyGraph();

    public void build(List<ParsedDependencyNode> parsedNodes) {
        logger.info("Building dependency graph from {} parsed nodes", parsedNodes.size());

        try {
            // Clear existing graph
            graph = new DependencyGraph();

            // Pass 1: Create all nodes
            logger.debug("Pass 1: Creating nodes...");
            for (ParsedDependencyNode parsedNode : parsedNodes) {
                try {
                    String nodeId = parsedNode.getName();
                    GraphNode classNode = new GraphNode(nodeId, "class",
                            nodeId.substring(nodeId.lastIndexOf(".") + 1));

                    // Add annotations to class node
                    if (parsedNode.getAnnotations() != null) {
                        classNode.setAnnotations(parsedNode.getAnnotations());
                    }

                    graph.addNode(classNode);
                    logger.debug("Added class node: {}", nodeId);

                    // Pass 1b: Create method nodes
                    if (parsedNode.getMethods() != null) {
                        for (ParsedMethodNode method : parsedNode.getMethods()) {
                            try {
                                String methodId = nodeId + "." + method.getMethodName();
                                GraphNode methodNode = new GraphNode(methodId, "method", method.getMethodName());

                                // Add called methods to method node
                                if (method.getCalledMethods() != null) {
                                    methodNode.setCalledMethods(method.getCalledMethods());
                                }

                                // Add annotations to method node
                                if (method.getAnnotations() != null) {
                                    methodNode.setAnnotations(method.getAnnotations());
                                }

                                graph.addNode(methodNode);
                                logger.debug("Added method node: {}", methodId);
                            } catch (Exception e) {
                                logger.error("Error creating method node for {}.{}: {}",
                                        nodeId, method.getMethodName(), e.getMessage());
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error creating node for {}: {}", parsedNode.getName(), e.getMessage());
                }
            }

            // Pass 2: Create edges (method calls)
            logger.debug("Pass 2: Creating edges...");
            for (ParsedDependencyNode parsedNode : parsedNodes) {
                String nodeId = parsedNode.getName();

                if (parsedNode.getMethods() != null) {
                    for (ParsedMethodNode method : parsedNode.getMethods()) {
                        try {
                            String methodId = nodeId + "." + method.getMethodName();
                            GraphNode methodNode = graph.getNodeMap().get(methodId);

                            if (methodNode != null && method.getCalledMethods() != null) {
                                for (String calledMethod : method.getCalledMethods()) {
                                    try {
                                        GraphNode calledNode = graph.getNodeMap().get(calledMethod);
                                        if (calledNode != null) {
                                            methodNode.addNeighbor(calledNode);
                                            logger.debug("Edge: {} → {}", methodId, calledMethod);
                                        }
                                    } catch (Exception e) {
                                        logger.debug("Could not find called method node: {}", calledMethod);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            logger.error("Error creating edges for method {}.{}: {}",
                                    nodeId, method.getMethodName(), e.getMessage());
                        }
                    }
                }

                // Pass 3: Create edges for injected dependencies
                logger.debug("Pass 3: Creating dependency injection edges...");
                if (parsedNode.getInjectedDependencies() != null) {
                    GraphNode classNode = graph.getNodeMap().get(nodeId);
                    if (classNode != null) {
                        for (String injected : parsedNode.getInjectedDependencies()) {
                            try {
                                // Find any node that matches this type
                                for (GraphNode node : graph.getNodeMap().values()) {
                                    if (node.getName().equals(injected) ||
                                            node.getId().contains(injected.substring(injected.lastIndexOf(".") + 1))) {
                                        classNode.addNeighbor(node);
                                        logger.debug("Injection edge: {} → {}", nodeId, injected);
                                        break;
                                    }
                                }
                            } catch (Exception e) {
                                logger.debug("Could not find injected dependency: {}", injected);
                            }
                        }
                    }
                }
            }

            logger.info("✓ Graph built successfully with {} nodes", graph.getNodeMap().size());
            graph.printGraph();

        } catch (Exception e) {
            logger.error("Error building graph: {}", e.getMessage(), e);
        }
    }

    public DependencyGraph getGraph() {
        return graph;
    }
}
