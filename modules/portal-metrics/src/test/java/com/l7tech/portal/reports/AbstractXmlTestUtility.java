package com.l7tech.portal.reports;

import org.junit.Before;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public abstract class AbstractXmlTestUtility {
    protected XPath xpath;

    public void setupAbstractXmlTest() {
        xpath = XPathFactory.newInstance().newXPath();
    }

    protected Document buildDocumentFromXml(final String xml) throws Exception {
        final DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        final ByteArrayInputStream is = new ByteArrayInputStream(xml.getBytes());
        return builder.parse(is);
    }

    protected void assertNumberOfNodes(final Document document, final String nodePath, final int numExpected) throws Exception {
        final XPathExpression expression = xpath.compile(nodePath);
        final Object evaluated = expression.evaluate(document, XPathConstants.NODESET);
        assertNotNull(evaluated);
        final NodeList usages = (NodeList) evaluated;
        assertEquals(numExpected, usages.getLength());
    }

    protected void assertXPathTextContent(final Document document, final String xpathExpression, final String expectedTextContent) throws Exception {
        final XPathExpression expression = xpath.compile(xpathExpression);
        final Node result = (Node) expression.evaluate(document, XPathConstants.NODE);
        assertNotNull(result);
        assertEquals(expectedTextContent, result.getTextContent());
    }

    protected void assertNodeDoesNotExist(final Document document, final String xpathExpression) throws Exception{
        final XPathExpression expression = xpath.compile(xpathExpression);
        assertNull(expression.evaluate(document));
    }
}
