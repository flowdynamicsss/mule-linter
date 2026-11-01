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
 * Base for rules that complain when a processor still has the display name Studio gives it
 * by default (for example "Set Variable"), because that only repeats the processor type.
 * <p>
 * The display name is stored in {@code doc:name}. It is compared to the default name as a
 * whole, ignoring upper/lower case, so "set variable" is default but "Set Variable Order" is not.
 */
abstract class DefaultDocNameRule implements LinterRule {

    private static final String DOCUMENTATION_NAMESPACE = "http://www.mulesoft.org/schema/mule/documentation";

    private final String ruleCode;
    private final String namespace;
    private final String localName;
    private final String defaultName;
    private final boolean flagMissing;
    private final String message;

    DefaultDocNameRule(String ruleCode, String namespace, String localName, String defaultName,
                       boolean flagMissing, String message) {
        this.ruleCode = ruleCode;
        this.namespace = namespace;
        this.localName = localName;
        this.defaultName = defaultName;
        this.flagMissing = flagMissing;
        this.message = message;
    }

    @Override
    public List<ValidationIssue> apply(Document document, RuleContext context) {
        List<ValidationIssue> issues = new ArrayList<>();
        NodeList nodes = document.getElementsByTagNameNS(namespace, localName);

        for (int i = 0; i < nodes.getLength(); i++) {
            Element element = (Element) nodes.item(i);
            String name = element.getAttributeNS(DOCUMENTATION_NAMESPACE, "name").trim();

            boolean missing = name.isEmpty();
            if (missing ? flagMissing : defaultName.equalsIgnoreCase(name)) {
                issues.add(new ValidationIssue(
                        ruleCode,
                        "warning",
                        message,
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
