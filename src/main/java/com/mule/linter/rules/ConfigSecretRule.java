package com.mule.linter.rules;

import com.mule.linter.RuleContext;
import com.mule.linter.Xml;
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
 * Looks for passwords, tokens, keys and similar things written directly in a config,
 * instead of coming from a secure property like ${secure::db.password}.
 */
public class ConfigSecretRule implements XmlRule {

    // Allowed values: ${secure::some.property} and #[p('secure::some.property')]
    private static final Pattern SECURE_PROPERTY = Pattern.compile("^\\$\\{\\s*secure::[^}\\s]+\\s*}$");
    private static final Pattern DATAWEAVE_SECURE_PROPERTY = Pattern.compile(
            "^#\\[\\s*p\\(\\s*['\"]secure::[^'\"]+['\"]\\s*\\)\\s*]$");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]");

    // An attribute or element with one of these words in its name holds a secret
    private static final String[] SECRET_WORDS = {
            "password", "passwd", "passphrase", "passcode", "userpass", "secret", "token",
            "credential", "apikey", "accesskey", "privatekey", "signingkey", "encryptionkey",
            "decryptionkey", "masterkey", "sharedkey", "keypassword", "keypass", "keystorepass",
            "keystorepassword", "truststorepass", "truststorepassword", "authorization"
    };

    @Override
    public String code() {
        return "config-hardcoded-secret";
    }

    @Override
    public List<ValidationIssue> check(Document xml, RuleContext file) {
        List<ValidationIssue> issues = new ArrayList<>();

        for (Element element : Xml.all(xml)) {
            if (!isInsideConfig(element)) {
                continue;
            }

            NamedNodeMap attributes = element.getAttributes();
            for (int i = 0; i < attributes.getLength(); i++) {
                Attr attribute = (Attr) attributes.item(i);
                if (isSecretName(attribute.getLocalName()) && isHardcoded(attribute.getValue())) {
                    String where = "attribute '" + attribute.getLocalName() + "' of '" + element.getTagName() + "'";
                    issues.add(file.warning(code(), "Hardcoded secret in " + where + ".", element));
                }
            }

            if (isSecretName(element.getLocalName()) && isHardcoded(textOf(element))) {
                String where = "element '" + element.getTagName() + "'";
                issues.add(file.warning(code(), "Hardcoded secret in " + where + ".", element));
            }
        }

        return issues;
    }

    /** True if the element is a config (name is "config" or ends with "-config") or is inside one. */
    private boolean isInsideConfig(Element element) {
        for (Node node = element; node instanceof Element; node = node.getParentNode()) {
            String name = node.getLocalName().toLowerCase(Locale.ROOT);
            if (name.equals("config") || name.endsWith("-config")) {
                return true;
            }
        }
        return false;
    }

    /** Ignores letter case and symbols, so api-key, apiKey and API_KEY all match. */
    private boolean isSecretName(String name) {
        String simpleName = NON_ALPHANUMERIC.matcher(name.toLowerCase(Locale.ROOT)).replaceAll("");
        for (String word : SECRET_WORDS) {
            if (simpleName.contains(word)) {
                return true;
            }
        }
        return false;
    }

    /** Any value counts as hardcoded, unless it is empty or a secure:: property. */
    private boolean isHardcoded(String value) {
        String trimmed = value.trim();
        return !trimmed.isEmpty()
                && !SECURE_PROPERTY.matcher(trimmed).matches()
                && !DATAWEAVE_SECURE_PROPERTY.matcher(trimmed).matches();
    }

    /** The text written directly in the element, without the text of the elements inside it. */
    private String textOf(Element element) {
        StringBuilder text = new StringBuilder();
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeType() == Node.TEXT_NODE) {
                text.append(children.item(i).getNodeValue());
            }
        }
        return text.toString();
    }
}
