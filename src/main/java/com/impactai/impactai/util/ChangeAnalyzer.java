package com.impactai.impactai.util;

import com.impactai.impactai.model.LineRange;
import com.impactai.impactai.parser.ParsedMethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Analyzes code changes to determine if they're comment-only, logic changes, etc.
 */
public class ChangeAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(ChangeAnalyzer.class);

    // Critical method annotations
    private static final String[] CRITICAL_ANNOTATIONS = {
            "Transactional",
            "CacheEvict",
            "Cacheable",
            "Scheduled",
            "Async",
            "EventListener",
            "PreAuthorize",
            "Secured",
            "PostMapping",
            "PutMapping",
            "DeleteMapping"
    };

    /**
     * Check if a method is critical based on its annotations
     */
    public static boolean isCriticalMethod(ParsedMethodNode method) {
        if (method.getAnnotations() == null || method.getAnnotations().isEmpty()) {
            return false;
        }

        for (String annotation : method.getAnnotations()) {
            for (String criticalAnn : CRITICAL_ANNOTATIONS) {
                if (annotation.contains(criticalAnn)) {
                    logger.debug("Method {} is critical (annotation: {})", method.getMethodName(), criticalAnn);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Determine if a change is comment-only or logic change
     * For now, we assume any change is logic change (conservative)
     * In future, could parse diff to check for comments
     */
    public static boolean isCommentOnly(String patch) {
        if (patch == null || patch.isEmpty()) {
            return false;
        }

        // Simple heuristic: if only lines starting with + are comments
        String[] lines = patch.split("\n");
        int totalChangedLines = 0;
        int commentOnlyLines = 0;

        for (String line : lines) {
            if (line.startsWith("+") && !line.startsWith("+++")) {
                totalChangedLines++;
                // Check if it's a comment line
                String trimmed = line.substring(1).trim();
                if (trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.isEmpty()) {
                    commentOnlyLines++;
                }
            }
        }

        boolean isComment = (totalChangedLines > 0) && (commentOnlyLines == totalChangedLines);
        logger.debug("Patch analysis: {} changed lines, {} comment lines, isCommentOnly: {}",
                totalChangedLines, commentOnlyLines, isComment);
        return isComment;
    }

    /**
     * Calculate method complexity based on number of called methods
     */
    public static int calculateComplexity(ParsedMethodNode method) {
        int complexity = 0;
        if (method.getCalledMethods() != null) {
            complexity = method.getCalledMethods().size();
        }
        return complexity;
    }

    /**
     * Get complexity multiplier for risk calculation
     */
    public static double getComplexityMultiplier(int complexity) {
        if (complexity <= 5) return 1.0;
        if (complexity <= 15) return 1.5;
        return 2.0;
    }

    /**
     * Get depth multiplier for risk calculation
     */
    public static double getDepthMultiplier(int depth) {
        if (depth <= 1) return 1.0;
        if (depth == 2) return 1.5;
        return 2.5;
    }

    /**
     * Get affected nodes multiplier for risk calculation
     */
    public static double getAffectedMultiplier(int affectedCount) {
        if (affectedCount <= 2) return 1.0;
        if (affectedCount <= 5) return 1.5;
        if (affectedCount <= 10) return 2.0;
        return 2.5;
    }
}
