package com.impactai.impactai.parser.java;

import com.impactai.impactai.parser.ParsedDependencyNode;
import com.impactai.impactai.parser.ParsedMethodNode;
import com.impactai.impactai.parser.LanguageParser;
import org.springframework.stereotype.Component;
import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.code.CtInvocation;
import java.util.*;

@Component
public class JavaParserImpl implements LanguageParser {

    @Override
    public List<ParsedDependencyNode> parseFile(String filePath) {
        Launcher launcher = new Launcher();
        launcher.addInputResource(filePath);
        launcher.buildModel();
        CtModel model = launcher.getModel();
        List<ParsedDependencyNode> result = new ArrayList<>();
        for (CtType<?> type : model.getAllTypes()) {
            ParsedDependencyNode node = new ParsedDependencyNode();
            node.setName(type.getQualifiedName());
            node.setType(type instanceof CtClass ? "class" :
                    type instanceof CtInterface ? "interface" : "unknown");

            // Annotations
            node.setAnnotations(new ArrayList<>());
            for (CtAnnotation<?> annotation : type.getAnnotations()) {
                node.getAnnotations().add(annotation.getAnnotationType().getQualifiedName());
            }

            // Inheritance
            List<String> extImplements = new ArrayList<>();
            type.getSuperInterfaces().forEach(i -> extImplements.add(i.getQualifiedName()));
            if (type instanceof CtClass) {
                CtTypeReference<?> superClass = ((CtClass<?>) type).getSuperclass();
                if (superClass != null) extImplements.add(superClass.getQualifiedName());
            }
            node.setExtendsImplements(extImplements);

            // Injected dependencies
            List<String> injected = new ArrayList<>();
            for (CtField<?> field : type.getFields()) {
                field.getAnnotations().forEach(ann -> {
                    String annName = ann.getAnnotationType().getQualifiedName();
                    if (annName.endsWith(".Autowired") || annName.endsWith(".Inject"))
                        injected.add(field.getType().getQualifiedName());
                });
            }
            node.setInjectedDependencies(injected);

            // Endpoint methods
            List<String> endpoints = new ArrayList<>();
            for (CtMethod<?> m : type.getMethods()) {
                for (CtAnnotation<?> ann : m.getAnnotations()) {
                    String annType = ann.getAnnotationType().getQualifiedName();
                    if (annType.contains(".RequestMapping") || annType.contains(".GetMapping") ||
                            annType.contains(".PostMapping")) {
                        endpoints.add(m.getSimpleName());
                    }
                }
            }
            node.setEndpoints(endpoints);

            // --------- METHOD-LEVEL PARSING ---------
            List<ParsedMethodNode> methods = new ArrayList<>();
            for (CtMethod<?> method : type.getMethods()) {
                ParsedMethodNode parsedMethod = new ParsedMethodNode();
                parsedMethod.setMethodName(method.getSimpleName());
                parsedMethod.setClassName(type.getQualifiedName());

                // Method annotations
                List<String> methodAnnotations = new ArrayList<>();
                for (CtAnnotation<?> ann : method.getAnnotations()) {
                    methodAnnotations.add(ann.getAnnotationType().getQualifiedName());
                }
                parsedMethod.setAnnotations(methodAnnotations);

                // Find all method calls in the body
                List<String> calledMethods = new ArrayList<>();
                method.filterChildren(child -> child instanceof CtInvocation)
                        .forEach(child -> {
                            CtInvocation<?> invocation = (CtInvocation<?>) child;
                            String calledName;
                            if (invocation.getExecutable().getDeclaringType() != null)
                                calledName = invocation.getExecutable().getDeclaringType().getQualifiedName() + "." + invocation.getExecutable().getSimpleName();
                            else
                                calledName = invocation.getExecutable().getSimpleName();
                            calledMethods.add(calledName);
                        });
                parsedMethod.setCalledMethods(calledMethods);

                methods.add(parsedMethod);
            }
            node.setMethods(methods);
            // ----------------------------------------

            result.add(node);
        }
        return result;
    }
}
