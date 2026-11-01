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
 * Checks that a flow with an APIkit router listens on /api/*.
 * Flows without a router are skipped.
 */
public class ApiListenerPathRule implements XmlRule {

    private static final String REQUIRED_PATH = "/api/*";

    @Override
    public String code() {
        return "api-listener-path";
    }

    @Override
    public List<ValidationIssue> check(Document xml, RuleContext file) {
        List<ValidationIssue> issues = new ArrayList<>();

        for (Element flow : Xml.find(xml, Namespaces.CORE, "flow")) {
            if (Xml.find(flow, Namespaces.APIKIT, "router").isEmpty()) {
                continue;
            }

            String flowName = flow.getAttribute("name");
            List<Element> listeners = Xml.find(flow, Namespaces.HTTP, "listener");

            if (listeners.isEmpty()) {
                issues.add(file.warning(code(), "No HTTP listener on '" + REQUIRED_PATH + "'.", flow, flowName));
            }

            for (Element listener : listeners) {
                String path = listener.getAttribute("path");
                if (!REQUIRED_PATH.equals(path)) {
                    // Point at the listener's name if it has one, otherwise at the flow
                    String listenerName = listener.getAttributeNS(Namespaces.DOC, "name");
                    String pointer = listenerName.isEmpty() ? flowName : listenerName;
                    String message = "Listener path '" + path + "' should be '" + REQUIRED_PATH + "'.";
                    issues.add(file.warning(code(), message, listener, pointer));
                }
            }
        }

        return issues;
    }
}
