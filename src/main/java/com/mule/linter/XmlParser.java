package com.mule.linter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;

/**
 * Parses a Mule XML file into a DOM document and remembers on which line every element starts.
 * The DOM parser can't do this, so the document is built by hand from SAX events.
 * <p>
 * The line is read later with {@link ElementLocator#line(Element)}. For a tag that is written on
 * several lines, it is the line where the tag ends.
 */
public class XmlParser {

    static final String LINE_KEY = "line";

    private final SAXParserFactory factory;

    public XmlParser() throws IOException {
        try {
            factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            // Harden against XXE/billion-laughs since we parse files from arbitrary project checkouts.
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setXIncludeAware(false);
        } catch (ParserConfigurationException | SAXException e) {
            throw new IOException("Unable to initialize XML parser", e);
        }
    }

    public Document parse(File file) throws SAXException, IOException {
        try {
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            SAXParser parser = factory.newSAXParser();
            parser.parse(file, new DomBuildingHandler(document));
            return document;
        } catch (ParserConfigurationException e) {
            throw new IOException("Unable to initialize XML parser", e);
        }
    }

    private static class DomBuildingHandler extends DefaultHandler {

        private final Document document;
        private Node current;
        private Locator locator;

        DomBuildingHandler(Document document) {
            this.document = document;
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
                element.setUserData(LINE_KEY, locator.getLineNumber(), null);
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
            current.appendChild(document.createTextNode(new String(chars, start, length)));
        }

        private static String emptyToNull(String value) {
            return value == null || value.isEmpty() ? null : value;
        }
    }
}
