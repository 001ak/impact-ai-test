package com.impactai.impactai.util;

import com.impactai.impactai.model.LineRange;
import com.impactai.impactai.model.PRChangeInfo;
import com.impactai.impactai.parser.ParsedDependencyNode;
import com.impactai.impactai.parser.ParsedMethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class GraphUtils {

    private static final Logger logger = LoggerFactory.getLogger(GraphUtils.class);

    /**
     * Extract changed node IDs from PR with LINE-LEVEL precision
     */
    public static List<String> extractChangedNodeIdsFromPR(List<PRChangeInfo> changedFiles,
                                                           List<ParsedDependencyNode> parsedNodes) {
        Set<String> changedNodeIds = new HashSet<>();

        if (changedFiles == null || changedFiles.isEmpty()) {
            logger.warn("No changed files provided");
            return new ArrayList<>(changedNodeIds);
        }

        if (parsedNodes == null || parsedNodes.isEmpty()) {
            logger.warn("No parsed nodes provided");
            return new ArrayList<>(changedNodeIds);
        }

        // Create a lookup map: filePath -> ParsedDependencyNode
        Map<String, ParsedDependencyNode> nodesByFile = buildFilePathMap(parsedNodes);

        for (PRChangeInfo changeInfo : changedFiles) {
            String filePath = changeInfo.getFilePath();
            PRChangeInfo.ChangeType changeType = changeInfo.getChangeType();
            List<LineRange> changedLines = changeInfo.getChangedLines();

            logger.debug("Processing changed file: {} (changeType: {})", filePath, changeType);

            // Find matching node for this file
            ParsedDependencyNode node = nodesByFile.get(filePath);

            if (node == null) {
                logger.warn("No parsed node found for file: {}. This file may not be Java or wasn't parsed.", filePath);
                continue;
            }

            // Get methods in this file
            List<ParsedMethodNode> methodsInFile = node.getMethods();

            if (methodsInFile == null || methodsInFile.isEmpty()) {
                logger.debug("No methods found in file: {}", filePath);
                continue;
            }

            // ===== LINE-LEVEL DETECTION =====
            if (changedLines != null && !changedLines.isEmpty()) {
                logger.debug("Using line-level detection for file: {} with {} changed ranges",
                        filePath, changedLines.size());

                // Find methods that actually overlap with changed lines
                List<ParsedMethodNode> modifiedMethods =
                        MethodModificationDetector.findModifiedMethods(methodsInFile, changedLines);

                // Add only modified methods as changed nodes
                List<String> modifiedNodeIds =
                        MethodModificationDetector.buildModifiedNodeIds(modifiedMethods);

                changedNodeIds.addAll(modifiedNodeIds);
                logger.info("File {} contributed {} changed method nodes", filePath, modifiedNodeIds.size());

            } else {
                // ===== FALLBACK: No patch data available =====
                logger.warn("No changed lines available for file: {}. Adding based on changeType.", filePath);

                // Handle based on change type
                switch (changeType) {
                    case DELETED:
                        // For deleted files, add only the class
                        changedNodeIds.add(node.getName());
                        logger.debug("Added deleted class: {}", node.getName());
                        break;

                    case ADDED:
                    case MODIFIED:
                    case RENAMED:
                    default:
                        // For added/modified/renamed without patch, add all methods as fallback
                        for (ParsedMethodNode method : methodsInFile) {
                            String nodeId = node.getName() + "." + method.getMethodName();
                            changedNodeIds.add(nodeId);
                            logger.debug("Added fallback node: {}", nodeId);
                        }
                        break;
                }
            }
        }

        logger.info("Total changed node IDs extracted: {}", changedNodeIds.size());
        return new ArrayList<>(changedNodeIds);
    }

    /**
     * Build a map of file paths to nodes for quick lookup
     * Tries multiple path formats to match
     */
    private static Map<String, ParsedDependencyNode> buildFilePathMap(List<ParsedDependencyNode> parsedNodes) {
        Map<String, ParsedDependencyNode> map = new HashMap<>();

        for (ParsedDependencyNode node : parsedNodes) {
            try {
                // Convert fully qualified class name to file path
                // e.g., com.example.MyClass -> src/main/java/com/example/MyClass.java
                String className = node.getName();
                String filePath1 = className.replace(".", "/") + ".java";
                String filePath2 = "src/main/java/" + filePath1;
                String filePath3 = "src/test/java/" + filePath1;

                map.put(filePath1, node);
                map.put(filePath2, node);
                map.put(filePath3, node);

                logger.debug("Registered node {} with paths: {}, {}, {}", className, filePath1, filePath2, filePath3);
            } catch (Exception e) {
                logger.error("Error building file path map for node", e);
            }
        }

        return map;
    }
}
