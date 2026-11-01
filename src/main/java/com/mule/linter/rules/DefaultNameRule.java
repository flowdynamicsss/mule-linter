package com.mule.linter.rules;

import com.mule.linter.Namespaces;
import com.mule.linter.RuleContext;
import com.mule.linter.Xml;
import com.mule.linter.model.ValidationIssue;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

/**
 * Flags an element that has no name, or still has the name Studio gave it by default
 * (for example "Set Variable"), because that only repeats what the element is.
 * <p>
 * The name is the doc:name attribute. The whole name is compared and the letter case doesn't matter,
 * so "set variable" is flagged but "Set Variable Order" is fine.
 * <p>
 * The same class is used for several elements, see the list in Rules.java.
 */
public class DefaultNameRule implements XmlRule {

    private final String code;
    private final String namespace;
    private final String elementName;
    private final String defaultName;

    public DefaultNameRule(String code, String namespace, String elementName, String defaultName) {
        this.code = code;
        this.namespace = namespace;
        this.elementName = elementName;
        this.defaultName = defaultName;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public List<ValidationIssue> check(Document xml, RuleContext file) {
        List<ValidationIssue> issues = new ArrayList<>();

        for (Element element : Xml.find(xml, namespace, elementName)) {
            String name = element.getAttributeNS(Namespaces.DOC, "name").trim();

            if (name.isEmpty() || name.equalsIgnoreCase(defaultName)) {
                issues.add(file.warning(code, defaultName + " has no name or the default one.", element));
            }
        }

        return issues;
    }
}
