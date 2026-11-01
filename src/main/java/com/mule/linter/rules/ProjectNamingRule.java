package com.mule.linter.rules;

import com.mule.linter.model.ValidationIssue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Checks the project folder name. It should be kebab-case and have an API layer in it,
 * for example partner-papi-v1. Runs once per project, not for every XML file.
 */
public class ProjectNamingRule implements ProjectRule {

    private static final Pattern KEBAB_CASE = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");
    private static final List<String> API_LAYERS = List.of("sapi", "papi", "xapi");

    @Override
    public String code() {
        return "project-naming-convention";
    }

    @Override
    public List<ValidationIssue> check(File projectFolder) {
        List<ValidationIssue> issues = new ArrayList<>();
        String name = projectFolder.getName();

        if (!KEBAB_CASE.matcher(name).matches()) {
            issues.add(ValidationIssue.warning(code(), "Project name '" + name + "' is not kebab-case.", name));
        }

        boolean hasApiLayer = false;
        for (String part : name.split("-")) {
            if (API_LAYERS.contains(part)) {
                hasApiLayer = true;
            }
        }
        if (!hasApiLayer) {
            issues.add(ValidationIssue.warning(code(), "Project name '" + name + "' has no API layer (sapi/papi/xapi).", name));
        }

        return issues;
    }
}
