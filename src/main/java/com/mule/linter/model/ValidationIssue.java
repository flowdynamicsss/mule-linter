package com.mule.linter.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * One problem found by a rule. This is what goes in the JSON report.
 * <ul>
 *   <li>code - which rule found it, e.g. choice-default-name</li>
 *   <li>severity - for now always "warning"</li>
 *   <li>file - the file, as a path inside the project (for project-wide rules it is the project name)</li>
 *   <li>identifier - helps to find the place: the doc:id, or the flow name if there is no doc:id. Can be empty</li>
 *   <li>line - the line in the file. Project-wide rules don't have one</li>
 * </ul>
 */
public class ValidationIssue {

    private final String code;
    private final String severity;
    private final String message;
    private final String file;
    private final String identifier;
    private final Integer line;

    /** A warning about a file or the whole project. It has no line and no identifier. */
    public static ValidationIssue warning(String code, String message, String file) {
        return new ValidationIssue(code, "warning", message, file, null, null);
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
