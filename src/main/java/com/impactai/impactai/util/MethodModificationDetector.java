package com.impactai.impactai.util;

import com.impactai.impactai.model.LineRange;
import com.impactai.impactai.parser.ParsedMethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Detects which methods were actually modified based on changed line ranges
 */
public class MethodModificationDetector {

    private static final Logger logger = LoggerFactory.getLogger(MethodModificationDetector.class);

    /**
     * Check if a line range overlaps with method's line range
     * @param changedStart Start line of changed range
     * @param changedEnd End line of changed range
     * @param methodStart Start line of method
     * @param methodEnd End line of method
     * @return true if there's any overlap
     */
    public static boolean overlaps(int changedStart, int changedEnd, int methodStart, int methodEnd) {
        // Handle invalid line numbers
        if (changedStart < 0 || changedEnd < 0 || methodStart < 0 || methodEnd < 0) {
            logger.debug("Skipping overlap check due to invalid line numbers: changed[{},{}], method[{},{}]",
                    changedStart, changedEnd, methodStart, methodEnd);
            return false;
        }

        // Check if ranges overlap:
        // Overlap exists if: changedStart <= methodEnd AND changedEnd >= methodStart
        boolean isOverlap = changedStart <= methodEnd && changedEnd >= methodStart;

        if (isOverlap) {
            logger.debug("Overlap detected: changed[{},{}] overlaps with method[{},{}]",
                    changedStart, changedEnd, methodStart, methodEnd);
        }

        return isOverlap;
    }

    /**
     * Find all methods that were modified based on changed line ranges
     * @param methods List of all methods in file
     * @param changedLines List of changed line ranges
     * @return List of methods that have changes
     */
    public static List<ParsedMethodNode> findModifiedMethods(List<ParsedMethodNode> methods,
                                                             List<LineRange> changedLines) {
        List<ParsedMethodNode> modifiedMethods = new ArrayList<>();

        if (methods == null || methods.isEmpty()) {
            logger.warn("No methods provided for modification detection");
            return modifiedMethods;
        }

        if (changedLines == null || changedLines.isEmpty()) {
            logger.warn("No changed lines provided, returning empty modified methods list");
            return modifiedMethods;
        }

        for (ParsedMethodNode method : methods) {
            boolean isModified = false;

            // Check each changed line range against this method
            for (LineRange changed : changedLines) {
                if (overlaps(changed.getStartLine(), changed.getEndLine(),
                        method.getStartLine(), method.getEndLine())) {
                    isModified = true;
                    logger.debug("Method {} was modified (lines {}-{})",
                            method.getMethodName(), method.getStartLine(), method.getEndLine());
                    break;
                }
            }

            if (isModified) {
                modifiedMethods.add(method);
            }
        }

        logger.info("Found {} modified methods out of {} total methods",
                modifiedMethods.size(), methods.size());

        return modifiedMethods;
    }

    /**
     * Build fully qualified node IDs from modified methods
     * Format: "com.example.ClassName.methodName"
     */
    public static List<String> buildModifiedNodeIds(List<ParsedMethodNode> modifiedMethods) {
        List<String> nodeIds = new ArrayList<>();

        if (modifiedMethods == null || modifiedMethods.isEmpty()) {
            return nodeIds;
        }

        for (ParsedMethodNode method : modifiedMethods) {
            try {
                String nodeId = method.getClassName() + "." + method.getMethodName();
                nodeIds.add(nodeId);
                logger.debug("Added modified node: {}", nodeId);
            } catch (Exception e) {
                logger.error("Error building node ID for method {}", method.getMethodName(), e);
            }
        }

        logger.info("Built {} modified node IDs", nodeIds.size());
        return nodeIds;
    }
}
