package com.mule.linter.rules;

import com.mule.linter.RuleContext;
import com.mule.linter.model.ValidationIssue;
import org.w3c.dom.Document;

import java.util.List;

/**
 * A check that runs on every XML file in src/main/mule.
 * To add a new one: make a class that implements this, then add it to the list in Rules.java.
 */
public interface XmlRule {

    /** The name of the rule in the report, e.g. choice-default-name. */
    String code();

    /** Checks one XML file and returns the problems it found. Empty if everything is fine. */
    List<ValidationIssue> check(Document xml, RuleContext file);
}
