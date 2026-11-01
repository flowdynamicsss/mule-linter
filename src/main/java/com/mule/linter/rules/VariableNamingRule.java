package com.mule.linter.rules;

import com.mule.linter.Namespaces;
import com.mule.linter.RuleContext;
import com.mule.linter.Xml;
import com.mule.linter.model.ValidationIssue;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Checks that variables made with set-variable are camelCase, e.g. orderId. */
public class VariableNamingRule implements XmlRule {

    private static final Pattern CAMEL_CASE = Pattern.compile("[a-z][a-zA-Z0-9]*");

    @Override
    public String code() {
        return "variable-naming-convention";
    }

    @Override
    public List<ValidationIssue> check(Document xml, RuleContext file) {
        // set-variable exists in the core namespace and inside Transform Message
        List<Element> variables = new ArrayList<>(Xml.find(xml, Namespaces.CORE, "set-variable"));
        variables.addAll(Xml.find(xml, Namespaces.EE_CORE, "set-variable"));

        List<ValidationIssue> issues = new ArrayList<>();
        for (Element variable : variables) {
            String name = variable.getAttribute("variableName");
            if (!CAMEL_CASE.matcher(name).matches()) {
                issues.add(file.warning(code(), "Variable '" + name + "' is not camelCase.", variable));
            }
        }
        return issues;
    }
}
