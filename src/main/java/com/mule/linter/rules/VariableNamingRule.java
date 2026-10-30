package com.mule.linter.rules;

import com.mule.linter.RuleContext;
import com.mule.linter.model.ValidationIssue;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

/** Checks that variables created with {@code set-variable} (core or Transform Message) use camelCase names. */
public class VariableNamingRule implements LinterRule {

    private static final String RULE_CODE = "variable-naming-convention";
    private static final String CORE_NAMESPACE = "http://www.mulesoft.org/schema/mule/core";
    private static final String EE_CORE_NAMESPACE = "http://www.mulesoft.org/schema/mule/ee/core";
    private static final String DOCUMENTATION_NAMESPACE = "http://www.mulesoft.org/schema/mule/documentation";

    @Override
    public List<ValidationIssue> apply(Document document, RuleContext context) {
        List<ValidationIssue> issues = new ArrayList<>();
        NodeList nodes = document.getElementsByTagNameNS("*", "set-variable");

        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            String namespace = node.getNamespaceURI();
            if (!CORE_NAMESPACE.equals(namespace) && !EE_CORE_NAMESPACE.equals(namespace)) {
                continue;
            }

            Element element = (Element) node;
            String variableName = element.getAttribute("variableName");
            if (!variableName.matches("[a-z][a-zA-Z0-9]*")) {
                issues.add(new ValidationIssue(
                        RULE_CODE,
                        "warning",
                        "Variable name '" + variableName + "' is not camelCase.",
                        context.getRelativePath(),
                        findDocId(element)
                ));
            }
        }

        return issues;
    }

    /** Looks for doc:id on the element or its closest parent, so the issue can be located in the file. */
    private String findDocId(Element element) {
        Node current = element;
        while (current instanceof Element) {
            String docId = ((Element) current).getAttributeNS(DOCUMENTATION_NAMESPACE, "id");
            if (!docId.isEmpty()) {
                return docId;
            }
            current = current.getParentNode();
        }
        return "";
    }
}