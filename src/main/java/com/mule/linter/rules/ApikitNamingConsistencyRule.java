package com.mule.linter.rules;

import com.mule.linter.RuleContext;
import com.mule.linter.model.ValidationIssue;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

/** Flags {@code <apikit:config>} elements whose name and RAML reference don't share the same functionality+layer stem. */
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

            Element element = (Element) node;
            String nameAttr = element.getAttribute("name");
            String configStem = stripSuffix(nameAttr, "-config");
            String ramlStem = extractRamlStem(element.getAttribute("api"));

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

    private String extractRamlStem(String apiAttribute) {
        if (apiAttribute == null || apiAttribute.isEmpty()) {
            return "";
        }
        String fileName = apiAttribute.contains(":") ? apiAttribute.substring(apiAttribute.lastIndexOf(':') + 1) : apiAttribute;
        return stripSuffix(fileName, ".raml");
    }

    private String stripSuffix(String value, String suffix) {
        return value.endsWith(suffix) ? value.substring(0, value.length() - suffix.length()) : value;
    }
}
