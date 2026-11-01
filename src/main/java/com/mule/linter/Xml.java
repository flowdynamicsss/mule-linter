package com.mule.linter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Small helpers for looking around in an XML file. */
public final class Xml {

    private Xml() {
    }

    /** All elements with this namespace and name in the document. */
    public static List<Element> find(Document xml, String namespace, String name) {
        return toList(xml.getElementsByTagNameNS(namespace, name));
    }

    /** All elements with this namespace and name inside the given element. */
    public static List<Element> find(Element parent, String namespace, String name) {
        return toList(parent.getElementsByTagNameNS(namespace, name));
    }

    /** Every element in the document. */
    public static List<Element> all(Document xml) {
        return find(xml, "*", "*");
    }

    /**
     * What we show to point at the element. In this order: its doc:id (or the doc:id of a parent),
     * the name of the flow it is in, the name of the closest element that has one.
     * Empty if there is nothing.
     */
    public static String identifier(Element element) {
        for (Node node = element; node instanceof Element; node = node.getParentNode()) {
            String docId = ((Element) node).getAttributeNS(Namespaces.DOC, "id");
            if (!docId.isEmpty()) {
                return docId;
            }
        }
        for (Node node = element; node instanceof Element; node = node.getParentNode()) {
            Element candidate = (Element) node;
            boolean isFlow = "flow".equals(candidate.getLocalName()) || "sub-flow".equals(candidate.getLocalName());
            if (isFlow && Namespaces.CORE.equals(candidate.getNamespaceURI())) {
                return candidate.getAttribute("name");
            }
        }
        // Elements outside of flows (like connector configs) usually have a name
        for (Node node = element; node instanceof Element; node = node.getParentNode()) {
            String name = ((Element) node).getAttribute("name");
            if (!name.isEmpty()) {
                return name;
            }
        }
        return "";
    }

    /** The line where the element starts. Empty if the file was not read with XmlParser. */
    @SuppressWarnings("unchecked")
    public static Integer line(Element element) {
        Map<Node, Integer> lines = (Map<Node, Integer>) element.getOwnerDocument().getUserData(XmlParser.LINES_KEY);
        return lines == null ? null : lines.get(element);
    }

    private static List<Element> toList(NodeList nodes) {
        List<Element> elements = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            elements.add((Element) nodes.item(i));
        }
        return elements;
    }
}
