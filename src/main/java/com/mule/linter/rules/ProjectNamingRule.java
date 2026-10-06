package com.mule.linter.rules;

import com.mule.linter.model.ValidationIssue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Validates that the project root folder name is kebab-case and includes a valid API layer (sapi/papi/xapi). */
public class ProjectNamingRule {

    private static final String RULE_CODE = "project-naming-convention";
    private static final List<String> VALID_LAYERS = Arrays.asList("sapi", "papi", "xapi");

    public List<ValidationIssue> apply(File projectRoot) {
        List<ValidationIssue> issues = new ArrayList<>();
        String name = projectRoot.getName();

        if (!name.matches("[a-z0-9]+(-[a-z0-9]+)*")) {
            issues.add(new ValidationIssue(
                    RULE_CODE,
                    "warning",
                    "Project name '" + name + "' is not kebab-case.",
                    name,
                    null
            ));
        }

        boolean hasLayer = Arrays.stream(name.split("-")).anyMatch(VALID_LAYERS::contains);
        if (!hasLayer) {
            issues.add(new ValidationIssue(
                    RULE_CODE,
                    "warning",
                    "Project name '" + name + "' does not include a valid API layer (sapi/papi/xapi).",
                    name,
                    null
            ));
        }

        return issues;
    }
}
