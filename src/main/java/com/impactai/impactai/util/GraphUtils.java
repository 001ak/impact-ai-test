package com.impactai.impactai.util;

import com.impactai.impactai.model.PRChangeInfo;
import com.impactai.impactai.parser.ParsedDependencyNode;
import com.impactai.impactai.parser.ParsedMethodNode;

import java.util.*;

public class GraphUtils {
    /**
     * Returns a list of node IDs (class and method nodes) touched by the changed files in the PR.
     *
     * @param changedFiles List of PRChangeInfo (each with getFilePath())
     * @param parsedNodes List of ParsedDependencyNode for the changed files
     * @return List of fully-qualified node IDs (e.g., com.example.Foo and com.example.Foo.method1)
     */
    public static List<String> extractChangedNodeIdsFromPR(List<PRChangeInfo> changedFiles, List<ParsedDependencyNode> parsedNodes) {
        Set<String> changedNodeIds = new HashSet<>();

        // Create a lookup set of file paths since PRChangeInfo may have relative paths
        Set<String> changedRelativePaths = new HashSet<>();
        for (PRChangeInfo info : changedFiles) {
            changedRelativePaths.add(info.getFilePath().replace("\\", "/")); // normalize slashes
        }

        // For each parsed node (class), include if from a changed file
        for (ParsedDependencyNode node : parsedNodes) {
            // Try to infer file match based on fully-qualified class name to file path
            // most likely the parsedNodes are only from the changed files already, but let's be strict
            String className = node.getName(); // e.g., com.example.MyClass
            // Most Java projects use "com/example/MyClass.java"
            String expectedRelativePath = className.replace(".", "/") + ".java";
            boolean isChanged = changedRelativePaths.stream().anyMatch(relativePath -> relativePath.endsWith(expectedRelativePath));
            if (isChanged) {
                changedNodeIds.add(className);

                // Also add all method nodes of this class
                if (node.getMethods() != null) {
                    for (ParsedMethodNode method : node.getMethods()) {
                        changedNodeIds.add(className + "." + method.getMethodName());
                    }
                }
            }
        }
        return new ArrayList<>(changedNodeIds);
    }
}
