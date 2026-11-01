package com.mule.linter;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Helps rules to tell where in a file an issue is. Studio doesn't put {@code doc:id} on every
 * element (a Transform Message or even a flow often has none), so the identifier has fallbacks.
 */
public final class ElementLocator {

    private static final String CORE_NAMESPACE = "http://www.mulesoft.org/schema/mule/core";
    private static final String DOCUMENTATION_NAMESPACE = "http://www.mulesoft.org/schema/mule/documentation";

    private ElementLocator() {
    }

    /**
     * Returns the first of these that exists: the {@code doc:id} of the element or of its closest
         * parent, the name of the flow / sub-flow it is in, or the name of the closest element that has
         * one (for global elements). Returns "" if there is none.
     */
    public static String identifier(Element element) {
        for (Node node = element; node instanceof Element; node = node.getParentNode()) {
            String docId = ((Element) node).getAttributeNS(DOCUMENTATION_NAMESPACE, "id");
            if (!docId.isEmpty()) {
                return docId;
            }
        }
        for (Node node = element; node instanceof Element; node = node.getParentNode()) {
            Element candidate = (Element) node;
            if (CORE_NAMESPACE.equals(candidate.getNamespaceURI())
                    && ("flow".equals(candidate.getLocalName()) || "sub-flow".equals(candidate.getLocalName()))) {
                return candidate.getAttribute("name");
            }
        }
        // Global elements, like a connector config, are not in a flow but usually have a name
        for (Node node = element; node instanceof Element; node = node.getParentNode()) {
            String name = ((Element) node).getAttribute("name");
            if (!name.isEmpty()) {
                return name;
            }
        }
        return "";
    }

    /** Line where the element starts, or null if the document was not read with {@link XmlParser}. */
    public static Integer line(Element element) {
        Object line = element.getUserData(XmlParser.LINE_KEY);
        return line instanceof Integer ? (Integer) line : null;
    }
}
