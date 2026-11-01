package com.mule.linter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mule.linter.model.ValidationIssue;
import com.mule.linter.rules.ProjectRule;
import com.mule.linter.rules.Rules;
import com.mule.linter.rules.XmlRule;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Writes the JSON report. */
final class ReportWriter {

    private ReportWriter() {
    }

    /** Saves the report to the file and prints it too. */
    static void write(List<ValidationIssue> issues, Path reportPath) throws IOException {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("issuesFound", issues.size());
        report.put("byType", countByRule(issues));
        report.put("issues", issues);

        String json = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(report);

        Path folder = reportPath.toAbsolutePath().getParent();
        if (folder != null) {
            Files.createDirectories(folder);
        }
        Files.writeString(reportPath, json, StandardCharsets.UTF_8);

        System.out.println("Execution finished! Results written to " + reportPath.toAbsolutePath());
        System.out.println(json);
    }

    /** How many problems each rule found. Every rule is in the list, with 0 if it found nothing. */
    private static Map<String, Integer> countByRule(List<ValidationIssue> issues) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (ProjectRule rule : Rules.PROJECT) {
            counts.put(rule.code(), 0);
        }
        for (XmlRule rule : Rules.XML) {
            counts.put(rule.code(), 0);
        }
        for (ValidationIssue issue : issues) {
            counts.merge(issue.getCode(), 1, Integer::sum);
        }
        return counts;
    }
}
