/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.util;

import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.ArrayList;

/**
 * Thread-local XML parsing and pretty-printing utilities.
 * User: mike
 * Date: Aug 28, 2003
 * Time: 4:20:59 PM
 */
public class XmlUtil {
    private static ThreadLocal documentBuilder = new ThreadLocal() {
        protected synchronized Object initialValue() {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            try {
                return dbf.newDocumentBuilder();
            } catch (ParserConfigurationException e) {
                throw new RuntimeException(e); // can't happen
            }
        }
    };

    public static DocumentBuilder getDocumentBuilder() {
        return (DocumentBuilder) documentBuilder.get();
    }

    public static Document stringToDocument(String inputXmlNotAUrl) throws IOException, SAXException {
        ByteArrayInputStream bis = new ByteArrayInputStream(inputXmlNotAUrl.getBytes());
        return parse(bis);
    }

    public static Document parse(InputStream input) throws IOException, SAXException {
        return getDocumentBuilder().parse(input);
    }


    public static ThreadLocal xmlSerializer = new ThreadLocal() {
        protected synchronized Object initialValue() {
            XMLSerializer xmlSerializer = new XMLSerializer();
            OutputFormat of = new OutputFormat();
            of.setIndent(4);
            xmlSerializer.setOutputFormat(of);
            return xmlSerializer;
        }
    };

    public static XMLSerializer getXmlSerializer() {
        return (XMLSerializer) xmlSerializer.get();
    }

    public static void documentToOutputStream(Document doc, OutputStream os) throws IOException {
        getXmlSerializer().setOutputCharStream(new OutputStreamWriter(os));
        getXmlSerializer().serialize(doc);
    }

    public static String documentToString(Document doc) throws IOException {
        final StringWriter sw = new StringWriter();
        getXmlSerializer().setOutputCharStream(sw);
        getXmlSerializer().serialize(doc);
        return sw.toString();
    }

    public static String elementToString(Element element) throws IOException {
        final StringWriter sw = new StringWriter();
        getXmlSerializer().setOutputCharStream(sw);
        getXmlSerializer().serialize(element);
        return sw.toString();
    }

    /**
     * Finds the first child {@link Element} of a parent {@link Element}.
     * @param parent the {@link Element} in which to search for children. Must be non-null.
     * @return First child {@link Element} or null if the specified parent contains no elements
     */
    public static Element findFirstChildElement( Element parent ) {
        NodeList children = parent.getChildNodes();
        for ( int i = 0; i < children.getLength(); i++ ) {
            Node n = children.item(i);
            if ( n.getNodeType() == Node.ELEMENT_NODE ) return (Element)n;
        }
        return null;
    }

    /**
     * Finds the first child {@link Element} of a parent {@link Element}
     * with the specified name that is in the specified namespace.
     *
     * The parent must belong to a DOM produced by a namespace-aware parser,
     * and the name must be undecorated.
     *
     * @param parent the {@link Element} in which to search for children. Must be non-null.
     * @param nsuri the URI of the namespace to which the child must belong, NOT THE PREFIX!  Must be non-null.
     * @param name the name of the element to find. Must be non-null.
     * @return First matching child {@link Element} or null if the specified parent contains no matching elements
     */
    public static Element findFirstChildElementByName( Element parent, String nsuri, String name ) {
        if ( nsuri == null || name == null ) throw new IllegalArgumentException( "nsuri and name must be non-null!" );
        NodeList children = parent.getChildNodes();
        for ( int i = 0; i < children.getLength(); i++ ) {
            Node n = children.item(i);
            if ( n.getNodeType() == Node.ELEMENT_NODE &&
                 name.equals( n.getLocalName()) &&
                 nsuri.equals( n.getNamespaceURI() ) )
                return (Element)n;
        }
        return null;
    }

    /**
     * Finds one and only one child {@link Element} of a parent {@link Element}
     * with the specified name that is in the specified namespace.
     *
     * The parent must belong to a DOM produced by a namespace-aware parser,
     * and the name must be undecorated.
     *
     * @param parent the {@link Element} in which to search for children. Must be non-null.
     * @param nsuri the URI of the namespace to which the child must belong, NOT THE PREFIX!  Must be non-null.
     * @param name the name of the element to find. Must be non-null.
     * @return First matching child {@link Element} or null if the specified parent contains no matching elements
     * @throws MultipleChildElementsException if multiple matching child nodes are found
     */
    public static Element findOnlyOneChildElementByName( Element parent, String nsuri, String name ) throws MultipleChildElementsException {
        if ( nsuri == null || name == null ) throw new IllegalArgumentException( "nsuri and name must be non-null!" );
        NodeList children = parent.getChildNodes();
        Element result = null;
        for ( int i = 0; i < children.getLength(); i++ ) {
            Node n = children.item(i);
            if ( n.getNodeType() == Node.ELEMENT_NODE &&
                 name.equals( n.getLocalName()) &&
                 nsuri.equals( n.getNamespaceURI() ) ) {
                if ( result != null ) throw new MultipleChildElementsException( nsuri, name );
                result = (Element)n;
            }
        }
        return result;
    }

    public static class MultipleChildElementsException extends Exception {
        public MultipleChildElementsException( String nsuri, String name ) {
            super( "Multiple matching child elements found" );
            this.nsuri = nsuri;
            this.name = name;
        }

        public String getNsUri() {
            return nsuri;
        }

        public String getName() {
            return name;
        }

        private String nsuri;
        private String name;
    }

    /**
     * Returns a list of all child {@link Element}s of a parent {@link Element}
     * with the specified name that are in the specified namespace.
     *
     * The parent must belong to a DOM produced by a namespace-aware parser,
     * and the name must be undecorated.
     *
     * @param parent the {@link Element} in which to search for children. Must be non-null.
     * @param nsuri the URI of the namespace to which the children must belong, NOT THE PREFIX!  Must be non-null.
     * @param name the name of the elements to find. Must be non-null.
     * @return A {@link List} containing all matching child {@link Element}s. Will be empty if the specified parent contains no matching elements
     */
    public static List findChildElementsByName( Element parent, String nsuri, String name ) {
        if ( nsuri == null || name == null ) throw new IllegalArgumentException( "nsuri and name must be non-null!" );
        List found = new ArrayList();

        NodeList children = parent.getChildNodes();
        for ( int i = 0; i < children.getLength(); i++ ) {
            Node n = children.item(i);
            if ( n.getNodeType() == Node.ELEMENT_NODE &&
                 name.equals( n.getLocalName()) &&
                 nsuri.equals( n.getNamespaceURI() ) )
                found.add( n );
        }

        return found;
    }

    /**
     * Returns the content of the first text node child of the specified element, if any.
     * @param parent the element to examine
     * @return The first text node as a String, or null if there were no text nodes.
     */
    public static String findFirstChildTextNode( Element parent ) {
        NodeList children = parent.getChildNodes();
        for ( int i = 0, length = children.getLength(); i < length; i++ ) {
            Node n = children.item(i);
            if ( n.getNodeType() == Node.TEXT_NODE )
                return n.getNodeValue();
        }

        return null;
    }
}
