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
 * Checks that an {@code <apikit:config>} is named after the RAML file it points to.
 * <p>
 * For example, {@code name="orders-api-config"} with {@code api="resource::...:orders-api.raml"}
 * is fine, but {@code name="orders-api-config"} pointing to {@code payments-api.raml} gets a warning.
 */
public class ApikitNamingConsistencyRule implements LinterRule {

    private static final String RULE_CODE = "apikit-naming-mismatch";
    private static final String APIKIT_NAMESPACE = "http://www.mulesoft.org/schema/mule/mule-apikit";

    @Override
    public List<ValidationIssue> apply(Document document, RuleContext context) {
        List<ValidationIssue> issues = new ArrayList<>();
        NodeList nodeList = document.getElementsByTagName("*");

        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (!(node instanceof Element) || !"config".equalsIgnoreCase(node.getLocalName())
                    || !APIKIT_NAMESPACE.equals(node.getNamespaceURI())) {
                continue;
            }

            // Compare the names without the "-config" / ".raml" parts, so only the real name is checked
            Element element = (Element) node;
            String nameAttr = element.getAttribute("name");
            String configStem = stripSuffix(nameAttr, "-config");
            String ramlStem = extractRamlStem(element.getAttribute("api"));

            // If either side is missing there is nothing to compare, so we skip it
            if (!configStem.isEmpty() && !ramlStem.isEmpty() && !configStem.equals(ramlStem)) {
                issues.add(new ValidationIssue(
                        RULE_CODE,
                        "warning",
                        "apikit:config name '" + nameAttr + "' does not match the RAML reference '" + ramlStem + "'.",
                        context.getRelativePath(),
                        nameAttr
                ));
            }
        }

        return issues;
    }

    /** Returns the RAML file name without the path prefix and ".raml" extension, or "" if there is no api value. */
    private String extractRamlStem(String apiAttribute) {
        if (apiAttribute == null || apiAttribute.isEmpty()) {
            return "";
        }
        // The api value can look like "resource::group:artifact:version:raml:zip:file.raml", so keep only the last part
        String fileName = apiAttribute.contains(":") ? apiAttribute.substring(apiAttribute.lastIndexOf(':') + 1) : apiAttribute;
        return stripSuffix(fileName, ".raml");
    }

    /** Removes the suffix from the value if it ends with it, otherwise returns the value unchanged. */
    private String stripSuffix(String value, String suffix) {
        return value.endsWith(suffix) ? value.substring(0, value.length() - suffix.length()) : value;
    }
}
