package com.impactai.impactai.parser;

import java.util.List;

public interface LanguageParser {
    List<ParsedDependencyNode> parseFile(String filePath);
}
