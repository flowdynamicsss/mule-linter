package com.mule.linter.model;

/**
 * Represents a single linting issue found while validating a Mule XML file.
 */
public class ValidationIssue {

    private final String code;
    private final String severity;
    private final String message;
    private final String file;
    private final String identifier;

    public ValidationIssue(String code, String severity, String message, String file, String identifier) {
        this.code = code;
        this.severity = severity;
        this.message = message;
        this.file = file;
        this.identifier = identifier;
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
