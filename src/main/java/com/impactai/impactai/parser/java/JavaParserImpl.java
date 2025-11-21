package com.impactai.impactai.parser.java;

import com.impactai.impactai.parser.ParsedDependencyNode;
import com.impactai.impactai.parser.ParsedMethodNode;
import com.impactai.impactai.parser.LanguageParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.code.CtInvocation;

import java.util.*;

@Component
public class JavaParserImpl implements LanguageParser {
    private static final Logger logger = LoggerFactory.getLogger(JavaParserImpl.class);


    @Override
    public List<ParsedDependencyNode> parseFile(String filePath) {
        Launcher launcher = new Launcher();
        launcher.addInputResource(filePath);
        launcher.buildModel();
        CtModel model = launcher.getModel();
        List<ParsedDependencyNode> result = new ArrayList<>();

        try {
            for (CtType<?> type : model.getAllTypes()) {
                ParsedDependencyNode node = new ParsedDependencyNode();
                node.setName(type.getQualifiedName());
                node.setType(type instanceof CtClass ? "class" :
                        type instanceof CtInterface ? "interface" : "unknown");

                // Annotations
                node.setAnnotations(new ArrayList<>());
                try {
                    for (CtAnnotation<?> annotation : type.getAnnotations()) {
                        node.getAnnotations().add(annotation.getAnnotationType().getQualifiedName());
                    }
                } catch (Exception e) {
                    System.err.println("Warning: Error extracting class annotations for " + type.getQualifiedName() + ": " + e.getMessage());
                }

                // Inheritance
                List<String> extImplements = new ArrayList<>();
                try {
                    type.getSuperInterfaces().forEach(i -> {
                        try {
                            extImplements.add(i.getQualifiedName());
                        } catch (Exception e) {
                            System.err.println("Warning: Error extracting superinterface for " + type.getQualifiedName());
                        }
                    });
                    if (type instanceof CtClass) {
                        CtTypeReference<?> superClass = ((CtClass<?>) type).getSuperclass();
                        if (superClass != null && !superClass.getQualifiedName().equals("java.lang.Object")) {
                            extImplements.add(superClass.getQualifiedName());
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Warning: Error extracting inheritance for " + type.getQualifiedName() + ": " + e.getMessage());
                }
                node.setExtendsImplements(extImplements);

                // Injected dependencies
                List<String> injected = new ArrayList<>();
                try {
                    for (CtField<?> field : type.getFields()) {
                        try {
                            field.getAnnotations().forEach(ann -> {
                                try {
                                    String annName = ann.getAnnotationType().getQualifiedName();
                                    if (annName.endsWith(".Autowired") || annName.endsWith(".Inject") ||
                                            annName.endsWith(".Value") || annName.endsWith(".Resource")) {
                                        String fieldType = field.getType().getQualifiedName();
                                        injected.add(fieldType);
                                    }
                                } catch (Exception e) {
                                    System.err.println("Warning: Error processing field annotation: " + e.getMessage());
                                }
                            });
                        } catch (Exception e) {
                            System.err.println("Warning: Error extracting field annotations: " + e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Warning: Error extracting injected dependencies for " + type.getQualifiedName() + ": " + e.getMessage());
                }
                node.setInjectedDependencies(injected);

                // Endpoint methods
                List<String> endpoints = new ArrayList<>();
                try {
                    for (CtMethod<?> m : type.getMethods()) {
                        try {
                            for (CtAnnotation<?> ann : m.getAnnotations()) {
                                String annType = ann.getAnnotationType().getQualifiedName();
                                if (annType.contains(".RequestMapping") || annType.contains(".GetMapping") ||
                                        annType.contains(".PostMapping") || annType.contains(".PutMapping") ||
                                        annType.contains(".DeleteMapping") || annType.contains(".PatchMapping")) {
                                    endpoints.add(m.getSimpleName());
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("Warning: Error extracting endpoint annotation for method: " + e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Warning: Error extracting endpoints for " + type.getQualifiedName() + ": " + e.getMessage());
                }
                node.setEndpoints(endpoints);

                // --------- METHOD-LEVEL PARSING WITH LINE NUMBERS ---------
                List<ParsedMethodNode> methods = new ArrayList<>();
                try {
                    for (CtMethod<?> method : type.getMethods()) {
                        ParsedMethodNode parsedMethod = new ParsedMethodNode();
                        parsedMethod.setMethodName(method.getSimpleName());
                        parsedMethod.setClassName(type.getQualifiedName());

                        // Extract line numbers (CRITICAL for line-level change detection)
                        int startLine = -1;
                        int endLine = -1;
                        try {
                            if (method.getPosition() != null && method.getPosition().isValidPosition()) {
                                startLine = method.getPosition().getLine();
                                endLine = method.getPosition().getEndLine();
                            } else {
                                System.out.println("Warning: No valid position for method " + method.getSimpleName() + " in " + type.getQualifiedName());
                            }
                        } catch (Exception e) {
                            System.err.println("Warning: Error extracting line numbers for method " + method.getSimpleName() + ": " + e.getMessage());
                        }
                        parsedMethod.setStartLine(startLine);
                        parsedMethod.setEndLine(endLine);

                        // Method annotations
                        List<String> methodAnnotations = new ArrayList<>();
                        try {
                            for (CtAnnotation<?> ann : method.getAnnotations()) {
                                try {
                                    methodAnnotations.add(ann.getAnnotationType().getQualifiedName());
                                } catch (Exception e) {
                                    System.err.println("Warning: Error processing method annotation: " + e.getMessage());
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("Warning: Error extracting method annotations for " + method.getSimpleName() + ": " + e.getMessage());
                        }
                        parsedMethod.setAnnotations(methodAnnotations);

                        // Find all method calls in the body
                        List<String> calledMethods = new ArrayList<>();
                        try {
                            method.filterChildren(child -> child instanceof CtInvocation)
                                    .forEach(child -> {
                                        try {
                                            CtInvocation<?> invocation = (CtInvocation<?>) child;
                                            String calledName;
                                            if (invocation.getExecutable().getDeclaringType() != null) {
                                                try {
                                                    calledName = invocation.getExecutable().getDeclaringType().getQualifiedName() + "." +
                                                            invocation.getExecutable().getSimpleName();
                                                } catch (Exception e) {
                                                    calledName = invocation.getExecutable().getSimpleName();
                                                }
                                            } else {
                                                calledName = invocation.getExecutable().getSimpleName();
                                            }
                                            if (calledName != null && !calledName.isEmpty()) {
                                                calledMethods.add(calledName);
                                            }
                                        } catch (Exception e) {
                                            System.err.println("Warning: Error processing method invocation: " + e.getMessage());
                                        }
                                    });
                        } catch (Exception e) {
                            System.err.println("Warning: Error extracting called methods for " + method.getSimpleName() + ": " + e.getMessage());
                        }
                        parsedMethod.setCalledMethods(calledMethods);

                        methods.add(parsedMethod);
                    }
                } catch (Exception e) {
                    System.err.println("Warning: Error parsing methods for " + type.getQualifiedName() + ": " + e.getMessage());
                }
                node.setMethods(methods);
                // --------------------------------------------------------

                result.add(node);
            }
        } catch (Exception e) {
            System.err.println("Critical error in parseFile for " + filePath + ": " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }
}
