package com.mule.linter.rules;

import com.mule.linter.model.ValidationIssue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/** Validates PascalCase names for Java source files. */
public class JavaClassNamingRule {

    private static final String RULE_CODE = "java-class-naming-convention";

    public List<ValidationIssue> apply(File projectRoot) throws IOException {
        List<ValidationIssue> issues = new ArrayList<>();
        Path javaSourceRoot = new File(projectRoot, "src/main/java").toPath();
        if (!Files.isDirectory(javaSourceRoot)) {
            return issues;
        }

        try (Stream<Path> paths = Files.walk(javaSourceRoot)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> addIssueIfInvalid(path, projectRoot.toPath(), issues));
        }

        return issues;
    }

    private void addIssueIfInvalid(Path path, Path projectRoot, List<ValidationIssue> issues) {
        String fileName = path.getFileName().toString();
        String className = fileName.substring(0, fileName.length() - ".java".length());
        if ("package-info".equals(className) || "module-info".equals(className)
                || className.matches("[A-Z][A-Za-z0-9]*")) {
            return;
        }

        issues.add(new ValidationIssue(
                RULE_CODE,
                "warning",
                "Java class name '" + className + "' is not PascalCase.",
                projectRoot.relativize(path).toString().replace('\\', '/'),
                className
        ));
    }
}