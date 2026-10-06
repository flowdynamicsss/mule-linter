package com.mule.linter.rules;

import com.mule.linter.RuleContext;
import com.mule.linter.model.ValidationIssue;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

/** Validates that APIkit entry flows define an HTTP listener at /api/*. */
public class ApiListenerPathRule implements LinterRule {

    private static final String RULE_CODE = "api-listener-path";
    private static final String CORE_NAMESPACE = "http://www.mulesoft.org/schema/mule/core";
    private static final String HTTP_NAMESPACE = "http://www.mulesoft.org/schema/mule/http";
    private static final String APIKIT_NAMESPACE = "http://www.mulesoft.org/schema/mule/apikit";
    private static final String REQUIRED_PATH = "/api/*";

    @Override
    public List<ValidationIssue> apply(Document document, RuleContext context) {
        List<ValidationIssue> issues = new ArrayList<>();
        NodeList flows = document.getElementsByTagNameNS(CORE_NAMESPACE, "flow");

        for (int i = 0; i < flows.getLength(); i++) {
            Element flow = (Element) flows.item(i);
            NodeList routers = flow.getElementsByTagNameNS(APIKIT_NAMESPACE, "router");
            if (routers.getLength() == 0) {
                continue;
            }

            NodeList listeners = flow.getElementsByTagNameNS(HTTP_NAMESPACE, "listener");
            if (listeners.getLength() == 0) {
                issues.add(new ValidationIssue(
                        RULE_CODE,
                        "warning",
                        "APIkit flow does not define an HTTP listener with path '" + REQUIRED_PATH + "'.",
                        context.getRelativePath(),
                        flow.getAttribute("name")
                ));
                continue;
            }

            for (int listenerIndex = 0; listenerIndex < listeners.getLength(); listenerIndex++) {
                Node node = listeners.item(listenerIndex);
                Element listener = (Element) node;
                String path = listener.getAttribute("path");
                if (!REQUIRED_PATH.equals(path)) {
                    String identifier = listener.getAttribute("doc:name");
                    issues.add(new ValidationIssue(
                            RULE_CODE,
                            "warning",
                            "HTTP listener path '" + path + "' should be '" + REQUIRED_PATH + "'.",
                            context.getRelativePath(),
                            identifier.isEmpty() ? flow.getAttribute("name") : identifier
                    ));
                }
            }
        }

        return issues;
    }
}