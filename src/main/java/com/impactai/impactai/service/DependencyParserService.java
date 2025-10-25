package com.impactai.impactai.service;

import com.impactai.impactai.parser.ParsedDependencyNode;
import com.impactai.impactai.parser.java.JavaParserImpl;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

@Service
public class DependencyParserService {

    private final JavaParserImpl javaParserImpl;

    @Autowired
    public DependencyParserService(JavaParserImpl javaParserImpl) {
        this.javaParserImpl = javaParserImpl;
    }

    public List<ParsedDependencyNode> parseChangedFiles(List<String> changedFiles) {
        List<ParsedDependencyNode> allNodes = new ArrayList<>();
        for (String filePath : changedFiles) {
            if (filePath.endsWith(".java")) {
                System.out.println("== Parsing " + filePath);
                List<ParsedDependencyNode> nodes = javaParserImpl.parseFile(filePath);
                nodes.forEach(System.out::println);
                allNodes.addAll(nodes);
            } else {
                System.out.println("Skipping non-java file: " + filePath);
            }
        }
        return allNodes;
    }
}
