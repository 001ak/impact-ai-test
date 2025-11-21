package com.impactai.impactai.service;

import com.impactai.impactai.parser.ParsedDependencyNode;
import com.impactai.impactai.parser.java.JavaParserImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Service
public class RepoParserService {

    @Autowired
    private JavaParserImpl javaParserImpl;

    public List<ParsedDependencyNode> parseFullRepo(String repoLocalPath) {
        List<ParsedDependencyNode> allNodes = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(Path.of(repoLocalPath))) {
            paths.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .forEach(path -> {
                        System.out.println("Full scan parsing: " + path);
                        List<ParsedDependencyNode> nodes = javaParserImpl.parseFile(path.toString());
                        allNodes.addAll(nodes);
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return allNodes;
    }
}
