package com.mule.linter.rules;

import com.mule.linter.RuleContext;
import com.mule.linter.model.ValidationIssue;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

/**
 * Flags {@code <choice>} components that do not document their purpose via {@code doc:description}.
 */
public class ChoiceRule implements LinterRule {

    private static final String RULE_CODE = "choice-missing-description";

    @Override
    public List<ValidationIssue> apply(Document document, RuleContext context) {
        List<ValidationIssue> issues = new ArrayList<>();
        NodeList nodeList = document.getElementsByTagName("*");

        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (!(node instanceof Element) || !"choice".equalsIgnoreCase(node.getLocalName())) {
                continue;
            }

            Element element = (Element) node;
            String docDescription = element.getAttribute("doc:description");

            if (docDescription.trim().isEmpty()) {
                String docId = element.getAttribute("doc:id");
                issues.add(new ValidationIssue(
                        RULE_CODE,
                        "warning",
                        "The <choice> component is missing a doc:description.",
                        context.getRelativePath(),
                        docId
                ));
            }
        }

        return issues;
    }
}
