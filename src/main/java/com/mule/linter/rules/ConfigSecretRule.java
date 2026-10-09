package com.mule.linter.rules;

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

/** Flags hardcoded credential-like values inside Mule configuration elements. */
public class ConfigSecretRule implements LinterRule {

    private static final String RULE_CODE = "config-hardcoded-secret";
        private static final String DOCUMENTATION_NAMESPACE = "http://www.mulesoft.org/schema/mule/documentation";
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
            Element config = findNearestConfig(element);
            if (config != null) {
                inspectElement(element, config, context, issues);
            }
        }

        return issues;
    }

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

    private void inspectElement(Element element, Element config, RuleContext context,
                                List<ValidationIssue> issues) {
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Attr attribute = (Attr) attributes.item(i);
            String attributeName = attribute.getLocalName() == null
                    ? attribute.getName()
                    : attribute.getLocalName();
            if (isSensitiveName(attributeName) && isHardcoded(attribute.getValue())) {
                addIssue(issues, context, config, element,
                        "attribute '" + attributeName + "' on element '" + element.getTagName() + "'");
            }
        }

        String elementName = element.getLocalName() == null ? element.getTagName() : element.getLocalName();
        if (isSensitiveName(elementName)) {
            String value = directText(element).trim();
            if (isHardcoded(value)) {
                addIssue(issues, context, config, element, "element '" + element.getTagName() + "'");
            }
        }

    }

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

    private boolean isSensitiveName(String name) {
        String normalized = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        for (String sensitiveName : SENSITIVE_NAMES) {
            if (normalized.contains(sensitiveName)) {
                return true;
            }
        }
        return false;
    }

    private boolean isHardcoded(String value) {
        String trimmed = value.trim();
        return !trimmed.isEmpty()
                && !SECURE_PROPERTY.matcher(trimmed).matches()
                && !DATAWEAVE_SECURE_PROPERTY.matcher(trimmed).matches();
    }

    private void addIssue(List<ValidationIssue> issues, RuleContext context, Element config,
                          Element field, String location) {
        String docId = field.getAttributeNS(DOCUMENTATION_NAMESPACE, "id");
        if (docId.isEmpty()) {
            docId = config.getAttributeNS(DOCUMENTATION_NAMESPACE, "id");
        }
        issues.add(new ValidationIssue(
                RULE_CODE,
                "warning",
                "Hardcoded sensitive value found in " + location + "; use a secure property reference.",
                context.getRelativePath(),
                docId
        ));
    }
}