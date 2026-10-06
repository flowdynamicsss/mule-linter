package com.mule.linter.rules;

import com.mule.linter.RuleContext;
import com.mule.linter.model.ValidationIssue;
import org.w3c.dom.Document;

import java.util.List;

/**
 * Contract implemented by every lint rule. Each rule inspects a parsed Mule XML document
 * and returns the issues it found.
 */
public interface LinterRule {

    List<ValidationIssue> apply(Document document, RuleContext context);
}
