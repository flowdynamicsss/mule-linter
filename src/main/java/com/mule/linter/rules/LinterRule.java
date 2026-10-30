package com.mule.linter.rules;

import com.mule.linter.RuleContext;
import com.mule.linter.model.ValidationIssue;
import org.w3c.dom.Document;

import java.util.List;

/**
 * Base interface for rules that check a Mule XML file.
 * To add a new rule, implement this interface and register it in {@code MuleLinter}.
 */
public interface LinterRule {

    /** Checks one parsed XML file and returns the issues found, or an empty list if everything is fine. */
    List<ValidationIssue> apply(Document document, RuleContext context);
}
