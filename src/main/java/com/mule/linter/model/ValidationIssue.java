package com.mule.linter.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * A single problem found by a rule. This is what ends up in the JSON report.
 * <ul>
 *   <li>{@code code} - the rule that found it, e.g. {@code choice-missing-description}</li>
 *   <li>{@code severity} - for example {@code warning}</li>
 *   <li>{@code file} - path of the file, relative to the project (or the project name for project-level rules)</li>
 *   <li>{@code identifier} - helps to find the spot in the file: the doc:id, or the flow name if there is no doc:id; can be empty or null</li>
 *   <li>{@code line} - line in the file where the element starts; not set for project-level rules</li>
 * </ul>
 */
public class ValidationIssue {

    private final String code;
    private final String severity;
    private final String message;
    private final String file;
    private final String identifier;
    private final Integer line;

    public ValidationIssue(String code, String severity, String message, String file, String identifier) {
        this(code, severity, message, file, identifier, null);
    }

    public ValidationIssue(String code, String severity, String message, String file, String identifier,
                           Integer line) {
        this.code = code;
        this.severity = severity;
        this.message = message;
        this.file = file;
        this.identifier = identifier;
        this.line = line;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Integer getLine() {
        return line;
    }

    public String getCode() {
        return code;
    }

    public String getSeverity() {
        return severity;
    }

    public String getMessage() {
        return message;
    }

    public String getFile() {
        return file;
    }

    public String getIdentifier() {
        return identifier;
    }
}
