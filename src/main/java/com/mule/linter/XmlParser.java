package com.mule.linter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Reads an XML file and remembers the line of every element, so the report can say where a problem is.
 * The normal XML reader can't do this, so we build the document ourselves while reading.
 * If a tag is split over several lines, we remember the last one.
 */
public class XmlParser {

    static final String LINES_KEY = "lines";

    // Made once and reused for every file, because making them is slow
    private final SAXParser parser;
    private final DocumentBuilder documentBuilder;

    public XmlParser() throws IOException {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            // No DOCTYPE and no external files, so a strange XML can't make us read other files
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setXIncludeAware(false);
            parser = factory.newSAXParser();
            documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException | SAXException e) {
            throw new IOException("Unable to initialize XML parser", e);
        }
    }

    public Document parse(File file) throws SAXException, IOException {
        Document document = documentBuilder.newDocument();
        // We keep all the lines in one map. It is much faster than saving the line on every element
        Map<Node, Integer> lines = new IdentityHashMap<>();
        document.setUserData(LINES_KEY, lines, null);
        parser.reset();
        parser.parse(file, new DomBuildingHandler(document, lines));
        return document;
    }

    private static class DomBuildingHandler extends DefaultHandler {

        private final Document document;
        private final Map<Node, Integer> lines;
        private Node current;
        private Locator locator;

        DomBuildingHandler(Document document, Map<Node, Integer> lines) {
            this.document = document;
            this.lines = lines;
            this.current = document;
        }

        @Override
        public void setDocumentLocator(Locator locator) {
            this.locator = locator;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            Element element = document.createElementNS(emptyToNull(uri), qName);
            for (int i = 0; i < attributes.getLength(); i++) {
                element.setAttributeNS(emptyToNull(attributes.getURI(i)), attributes.getQName(i), attributes.getValue(i));
            }
            if (locator != null) {
                lines.put(element, locator.getLineNumber());
            }
            current.appendChild(element);
            current = element;
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            current = current.getParentNode();
        }

        @Override
        public void characters(char[] chars, int start, int length) {
            // The text can come in pieces, so we glue the pieces together
            Node last = current.getLastChild();
            if (last instanceof Text) {
                ((Text) last).appendData(new String(chars, start, length));
            } else {
                current.appendChild(document.createTextNode(new String(chars, start, length)));
            }
        }

        private static String emptyToNull(String value) {
            return value == null || value.isEmpty() ? null : value;
        }
    }
}
