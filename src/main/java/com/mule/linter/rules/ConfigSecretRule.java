package com.mule.linter.rules;

import com.mule.linter.ElementLocator;
import com.mule.linter.RuleContext;
import com.mule.linter.model.ValidationIssue;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Looks for passwords, tokens, keys and similar values written directly in a config element
 * (or inside it) instead of coming from a secure property like {@code ${secure::db.password}}.
 */
public class ConfigSecretRule implements LinterRule {

    private static final String RULE_CODE = "config-hardcoded-secret";
        private static final Pattern SECURE_PROPERTY = Pattern.compile("^\\$\\{\\s*secure::[^}\\s]+\\s*}$");
        private static final Pattern DATAWEAVE_SECURE_PROPERTY = Pattern.compile(
            "^#\\[\\s*p\\(\\s*['\"]secure::[^'\"]+['\"]\\s*\\)\\s*]$");
    private static final String[] SENSITIVE_NAMES = {
            "password", "passwd", "passphrase", "passcode", "userpass", "secret", "token",
            "credential", "apikey", "accesskey", "privatekey", "signingkey", "encryptionkey",
            "decryptionkey", "masterkey", "sharedkey", "keypassword", "keypass", "keystorepass",
            "keystorepassword", "truststorepass", "truststorepassword", "authorization"
    };

    @Override
    public List<ValidationIssue> apply(Document document, RuleContext context) {
        List<ValidationIssue> issues = new ArrayList<>();
        NodeList elements = document.getElementsByTagName("*");

        for (int i = 0; i < elements.getLength(); i++) {
            Element element = (Element) elements.item(i);
            if (findNearestConfig(element) != null) {
                inspectElement(element, context, issues);
            }
        }

        return issues;
    }

    /** Returns the closest config element (the element itself or a parent), or null if it is not inside one. */
    private Element findNearestConfig(Element element) {
        Node current = element;
        while (current instanceof Element) {
            Element candidate = (Element) current;
            if (isConfigElement(candidate)) {
                return candidate;
            }
            current = current.getParentNode();
        }
        return null;
    }

    /** True for {@code config} and anything ending with {@code -config}, e.g. {@code http:listener-config}. */
    private boolean isConfigElement(Element element) {
        String name = element.getLocalName();
        if (name == null) {
            name = element.getTagName();
            int prefixEnd = name.indexOf(':');
            if (prefixEnd >= 0) {
                name = name.substring(prefixEnd + 1);
            }
        }
        String normalized = name.toLowerCase(Locale.ROOT);
        return "config".equals(normalized) || normalized.endsWith("-config");
    }

    /** Checks the attributes and the text of one element for sensitive names with hardcoded values. */
    private void inspectElement(Element element, RuleContext context, List<ValidationIssue> issues) {
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Attr attribute = (Attr) attributes.item(i);
            String attributeName = attribute.getLocalName() == null
                    ? attribute.getName()
                    : attribute.getLocalName();
            if (isSensitiveName(attributeName) && isHardcoded(attribute.getValue())) {
                addIssue(issues, context, element,
                        "attribute '" + attributeName + "' on element '" + element.getTagName() + "'");
            }
        }

        String elementName = element.getLocalName() == null ? element.getTagName() : element.getLocalName();
        if (isSensitiveName(elementName)) {
            String value = directText(element).trim();
            if (isHardcoded(value)) {
                addIssue(issues, context, element, "element '" + element.getTagName() + "'");
            }
        }

    }

    /** Text written directly in the element, without the text of its children. */
    private String directText(Element element) {
        StringBuilder text = new StringBuilder();
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.TEXT_NODE || child.getNodeType() == Node.CDATA_SECTION_NODE) {
                text.append(child.getNodeValue());
            }
        }
        return text.toString();
    }

    /** Ignores case and symbols, so {@code api-key}, {@code apiKey} and {@code API_KEY} all match. */
    private boolean isSensitiveName(String name) {
        String normalized = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        for (String sensitiveName : SENSITIVE_NAMES) {
            if (normalized.contains(sensitiveName)) {
                return true;
            }
        }
        return false;
    }

    /** A non-empty value counts as hardcoded unless it is a {@code secure::} property reference. */
    private boolean isHardcoded(String value) {
        String trimmed = value.trim();
        return !trimmed.isEmpty()
                && !SECURE_PROPERTY.matcher(trimmed).matches()
                && !DATAWEAVE_SECURE_PROPERTY.matcher(trimmed).matches();
    }

    private void addIssue(List<ValidationIssue> issues, RuleContext context, Element field, String location) {
        issues.add(new ValidationIssue(
                RULE_CODE,
                "warning",
                "Hardcoded sensitive value found in " + location + "; use a secure property reference.",
                context.getRelativePath(),
                ElementLocator.identifier(field),
                ElementLocator.line(field)
        ));
    }
}