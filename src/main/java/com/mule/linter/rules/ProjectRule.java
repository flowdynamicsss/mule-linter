package com.mule.linter.rules;

import com.mule.linter.model.ValidationIssue;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * A check for the whole project (not for one XML file). It runs once.
 * To add a new one: make a class that implements this, then add it to the list in Rules.java.
 */
public interface ProjectRule {

    /** The name of the rule in the report, e.g. project-naming-convention. */
    String code();

    /** Checks the project and returns the problems it found. Empty if everything is fine. */
    List<ValidationIssue> check(File projectFolder) throws IOException;
}
