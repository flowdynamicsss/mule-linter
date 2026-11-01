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
 * Checks that the name of an apikit:config matches the RAML file it uses.
 * <p>
 * Example: name="orders-api-config" with orders-api.raml is fine,
 * but with payments-api.raml it gives a warning.
 */
public class ApikitNamingConsistencyRule implements XmlRule {

    @Override
    public String code() {
        return "apikit-naming-mismatch";
    }

    @Override
    public List<ValidationIssue> check(Document xml, RuleContext file) {
        List<ValidationIssue> issues = new ArrayList<>();

        for (Element config : Xml.find(xml, Namespaces.APIKIT, "config")) {
            String name = config.getAttribute("name");
            String nameWithoutConfig = removeEnding(name, "-config");
            String ramlName = ramlName(config.getAttribute("api"));

            // Nothing to compare if one of them is empty
            if (nameWithoutConfig.isEmpty() || ramlName.isEmpty()) {
                continue;
            }
            if (!nameWithoutConfig.equals(ramlName)) {
                String message = "Config name '" + name + "' doesn't match RAML '" + ramlName + "'.";
                issues.add(file.warning(code(), message, config, name));
            }
        }

        return issues;
    }

    /** The RAML file name, without the path before it and the .raml at the end. */
    private String ramlName(String api) {
        // The api value looks like resource::group:artifact:1.0:raml:zip:file.raml, we only need the last part
        String fileName = api.substring(api.lastIndexOf(':') + 1);
        return removeEnding(fileName, ".raml");
    }

    /** Cuts the ending off if the text has it. */
    private String removeEnding(String text, String ending) {
        return text.endsWith(ending) ? text.substring(0, text.length() - ending.length()) : text;
    }
}
