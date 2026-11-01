package com.mule.linter;

import com.mule.linter.model.ValidationIssue;
import org.w3c.dom.Element;

import java.io.File;

/**
 * The XML file that is being checked. Rules use it to create their warnings.
 */
public class RuleContext {

    private final File projectFolder;
    private final File file;

    public RuleContext(File projectFolder, File file) {
        this.projectFolder = projectFolder;
        this.file = file;
    }

    /** A warning about this element. The report points at it with the doc:id (or flow name) and the line. */
    public ValidationIssue warning(String code, String message, Element element) {
        return warning(code, message, element, Xml.identifier(element));
    }

    /** Same, but with our own text to point at the element. */
    public ValidationIssue warning(String code, String message, Element element, String identifier) {
        return new ValidationIssue(code, "warning", message, relativePath(), identifier, Xml.line(element));
    }

    /** Path of the file inside the project. It always uses /, so the report looks the same everywhere. */
    private String relativePath() {
        return projectFolder.toPath().relativize(file.toPath()).toString().replace('\\', '/');
    }
}
