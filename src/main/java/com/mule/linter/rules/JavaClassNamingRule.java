package com.mule.linter.rules;

import com.mule.linter.model.ValidationIssue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Checks that the Java files in src/main/java are named in PascalCase. Runs once per project. */
public class JavaClassNamingRule implements ProjectRule {

    private static final Pattern PASCAL_CASE = Pattern.compile("[A-Z][A-Za-z0-9]*");

    @Override
    public String code() {
        return "java-class-naming-convention";
    }

    @Override
    public List<ValidationIssue> check(File projectFolder) throws IOException {
        List<ValidationIssue> issues = new ArrayList<>();
        Path projectPath = projectFolder.toPath();
        Path javaFolder = projectPath.resolve("src/main/java");
        if (!Files.isDirectory(javaFolder)) {
            return issues;
        }

        for (Path javaFile : findJavaFiles(javaFolder)) {
            String fileName = javaFile.getFileName().toString();
            String className = fileName.substring(0, fileName.length() - ".java".length());

            // package-info and module-info are special files, not classes
            boolean isSpecialFile = className.equals("package-info") || className.equals("module-info");
            if (isSpecialFile || PASCAL_CASE.matcher(className).matches()) {
                continue;
            }

            String path = projectPath.relativize(javaFile).toString().replace('\\', '/');
            String message = "Class name '" + className + "' is not PascalCase.";
            issues.add(new ValidationIssue(code(), "warning", message, path, className, null));
        }

        return issues;
    }

    private List<Path> findJavaFiles(Path folder) throws IOException {
        try (Stream<Path> paths = Files.walk(folder)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .collect(Collectors.toList());
        }
    }
}
