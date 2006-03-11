/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 */

package com.l7tech.common.util;

import com.ibm.xml.dsig.Canonicalizer;
import com.ibm.xml.dsig.transform.W3CCanonicalizer2WC;
import com.l7tech.common.xml.TooManyChildElementsException;
import com.l7tech.common.io.BufferPoolByteArrayOutputStream;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.apache.xmlbeans.impl.xb.xsdschema.SchemaDocument;
import org.w3c.dom.*;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Thread-local XML parsing and pretty-printing utilities.
 */
public class XmlUtil {
    public static final String XML_VERSION = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n";
    public static final String TEXT_XML = "text/xml";

    private static final EntityResolver SAFE_ENTITY_RESOLVER = new EntityResolver() {
        public InputSource resolveEntity(String publicId, String systemId) throws SAXException {
            String msg = "Document referred to an external entity with system id '" + systemId + "'";
            logger.warning( msg );
            throw new SAXException(msg);
        }
    };

    private static final LSResourceResolver SAFE_LS_RESOURCE_RESOLVER = new LSResourceResolver() {
        public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
            String msg = "Document referred to an external entity with system id '" + systemId + "' of type '" + type + "'";
            logger.warning( msg );
            return new LSInputImpl(); // resolve to nothing, causes error
        }
    };
    
    /** This is the namespace that the special namespace prefix "xmlns" logically belongs to. */
    public static final String XMLNS_NS = "http://www.w3.org/2000/xmlns/";
    public static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
    public static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";
    public static final String JAXP_SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";
    
    /** This is the namespace that the special namespace prefix "xml" logically belongs to. */
    //public static final String XML_NS = "http://www.w3.org/XML/1998/namespace";

    /**
     * Returns a stateless, thread-safe {@link EntityResolver} that throws a SAXException upon encountering
     * external entity references but otherwise works as expected.  
     *
     * This EntityResolver should be used in ALL XML parsing to avoid DOS attacks.
     * @return a safe {@link EntityResolver} instance.
     */
    public static EntityResolver getSafeEntityResolver() {
        return SAFE_ENTITY_RESOLVER;
    }

    /**
     * Returns a stateless, thread-safe {@link LSResourceResolver} that resolves all schema and entity
     * references to an uninitialized LSInput implementation.
     *
     * @return a safe {@link LSResourceResolver} instance.
     */
    public static LSResourceResolver getSafeLSResourceResolver() {
        return SAFE_LS_RESOURCE_RESOLVER;
    }

    private static ThreadLocal documentBuilder = new ThreadLocal() {
        protected synchronized Object initialValue() {
            try {
                DocumentBuilder builder = dbf.newDocumentBuilder();
                builder.setEntityResolver(SAFE_ENTITY_RESOLVER);
                return builder;
            } catch (ParserConfigurationException e) {
                throw new RuntimeException(e); // can't happen
            }
        }
    };

    private static ThreadLocal documentBuilderAllowingDoctype = new ThreadLocal() {
        protected synchronized Object initialValue() {
            try {
                DocumentBuilder builder = dbfAllowingDoctype.newDocumentBuilder();
                builder.setEntityResolver(SAFE_ENTITY_RESOLVER);
                return builder;
            } catch (ParserConfigurationException e) {
                throw new RuntimeException(e); // can't happen
            }
        }
    };

    private static DocumentBuilder getDocumentBuilder() {
        return (DocumentBuilder)documentBuilder.get();
    }

    private static DocumentBuilder getDocumentBuilderAllowingDoctype() {
        return (DocumentBuilder)documentBuilderAllowingDoctype.get();
    }

    /** @return a new, empty DOM document with absolutely nothing in it. */
    public static Document createEmptyDocument() {
        return getDocumentBuilder().newDocument();
    }

    /** @return a new DOM document contianing only a single empty element. */ 
    public static Document createEmptyDocument(String rootElementName, String rootPrefix, String rootNs) {
        // TODO make a better-performing version of this
        try {
            if (rootElementName == null || rootElementName.length() < 1) throw new IllegalArgumentException();
            final String xml;
            if (rootNs != null) {
                if (rootPrefix != null) {
                    final String el = rootPrefix + ":" + rootElementName;
                    xml = "<" + el + " xmlns:" + rootPrefix + "=\"" + rootNs + "\"/>";
                } else {
                    xml = "<" + rootElementName + " xmlns=\"" + rootNs + "\"/>";
                }
            } else {
                xml = "<" + rootElementName + "/>";
            }
            return stringToDocument(xml);
        } catch (SAXException e) {
            throw new RuntimeException(e); // can't happen
        }
    }

    public static Document stringToDocument(String inputXmlNotAUrl) throws SAXException {
        Reader reader = new StringReader(inputXmlNotAUrl);
        try {
            return parse(reader, false);
        } catch (IOException e) {  // can't happen
            throw new SAXException("Unable to parse this XML string: " + e.getClass().getName() + ": " + e.getMessage(), e);
        }
    }

    /**
     * Create an XML document from the given InputStream. If the InputStream
     * data does NOT contain charset information and you know the charset you
     * should call the parse method that takes an encoding argument.
     *
     * @param input the InputStream that will produce the document's bytes
     * @return the parsed document
     * @throws IOException if an IO error occurs
     * @throws SAXException if there is a parsing error
     */
    public static Document parse(InputStream input) throws IOException, SAXException {
        return parse(new InputSource(input), false);
    }

    /**
     * Create an XML document from the given InputStream. If the InputStream
     * data contains charset information this SHOULD be ignored by the parser
     * and the specified one used.
     *
     * @param input the InputStream that will produce the document's bytes
     * @return the parsed document
     * @throws IOException if an IO error occurs
     * @throws SAXException if there is a parsing error
     */
    public static Document parse(InputStream input, String encoding) throws IOException, SAXException {
        InputSource is = new InputSource(input);
        is.setEncoding(encoding);
        return parse(is, false);
    }

    /**
     * Create an XML document from the given InputStream. If the InputStream
     * data does NOT contain charset information and you know the charset you
     * should create a Reader and pass that instead.
     *
     * @param input the InputStream that will produce the document's bytes
     * @param allowDoctype true to allow DOCTYPE processing instructions. <b>NOTE</b>: Don't allow DOCTYPEs on "foreign" documents (e.g. any document received over a network interface).
     * @return the parsed DOM Document
     * @throws IOException
     * @throws SAXException
     */
    public static Document parse(InputStream input, boolean allowDoctype) throws IOException, SAXException {
        return parse(new InputSource(input), allowDoctype);
    }

    /**
     * Create an XML document from the given Reader.
     *
     * <p>This method is intended to be used when you have character data to
     * parse. If you know the encoding but have binary data, call the method
     * that takes an InputStream and an encoding.</p>
     *
     * @param input the Reader that will produce the document's characters
     * @param allowDoctype true to allow DOCTYPE processing instructions. <b>NOTE</b>: Don't allow DOCTYPEs on "foreign" documents (e.g. any document received over a network interface).
     * @return the parsed document
     * @throws IOException if an IO error occurs
     * @throws SAXException if there is a parsing error
     */
    public static Document parse(Reader input, boolean allowDoctype) throws IOException, SAXException {
        return parse(new InputSource(input), allowDoctype);
    }

    /**
     * Create an XML document from the given SAX InputSource.
     *
     * @param source the input source
     * @param allowDoctype true to allow DOCTYPE processing instructions. <b>NOTE</b>: Don't allow DOCTYPEs on "foreign" documents (e.g. any document received over a network interface).
     * @return the parsed DOM Document
     * @throws IOException if an IO error occurs
     * @throws SAXException if there is a parsing error
     */
    public static Document parse(InputSource source, boolean allowDoctype) throws IOException, SAXException {
        DocumentBuilder parser = allowDoctype
                ? getDocumentBuilderAllowingDoctype()
                : getDocumentBuilder();
        return parser.parse(source);
    }

    private static ThreadLocal formattedXMLSerializer = new ThreadLocal() {
        protected synchronized Object initialValue() {
            XMLSerializer xmlSerializer = new XMLSerializer();
            OutputFormat of = new OutputFormat();
            of.setIndent(4);
            xmlSerializer.setOutputFormat(of);
            return xmlSerializer;
        }
    };

    private static ThreadLocal encodingXMLSerializer = new ThreadLocal() {
        protected synchronized Object initialValue() {
            XMLSerializer xmlSerializer = new XMLSerializer();
            return xmlSerializer;
        }
    };

    private static XMLSerializer getFormattedXmlSerializer() {
        return (XMLSerializer) formattedXMLSerializer.get();
    }

    private static XMLSerializer getEncodingXmlSerializer() {
        return (XMLSerializer) encodingXMLSerializer.get();
    }

    private static ThreadLocal transparentXMLSerializer = new ThreadLocal() {
        protected synchronized Object initialValue() {
            return new W3CCanonicalizer2WC();
        }
    };

    private static Canonicalizer getTransparentXMLSerializer() {
        return (Canonicalizer)transparentXMLSerializer.get();
    }

    public static void nodeToOutputStream(Node node, OutputStream os) throws IOException {
        Canonicalizer canon = getTransparentXMLSerializer();
        canon.canonicalize(node, os);
    }

    public static void nodeToOutputStream(Node node, OutputStream os, String encoding) throws IOException {
        OutputFormat of = new OutputFormat();
        of.setEncoding(encoding);

        XMLSerializer ser = getEncodingXmlSerializer();
        ser.setOutputFormat(of);
        ser.setOutputByteStream(os);
        if (node instanceof Document)
            ser.serialize((Document)node);
        else if (node instanceof Element)
            ser.serialize((Element)node);
        else
            throw new IllegalArgumentException("Node must be either a Document or an Element");
    }

    public static void nodeToFormattedOutputStream(Node node, OutputStream os) throws IOException {
        XMLSerializer ser = getFormattedXmlSerializer();
        ser.setOutputByteStream(os);
        if (node instanceof Document)
            ser.serialize((Document)node);
        else if (node instanceof Element)
            ser.serialize((Element)node);
        else
            throw new IllegalArgumentException("Node must be either a Document or an Element");
    }

    public static String nodeToString(Node node) throws IOException {
        final BufferPoolByteArrayOutputStream out = new BufferPoolByteArrayOutputStream(1024);
        try {
            nodeToOutputStream(node, out);
            return out.toString();
        } finally {
            out.close();
        }
    }

    public static byte[] toByteArray(Node node) throws IOException {
        final BufferPoolByteArrayOutputStream out = new BufferPoolByteArrayOutputStream(1024);
        try {
            nodeToOutputStream(node, out);
            return out.toByteArray();
        } finally {
            out.close();
        }
    }

    public static byte[] toByteArray(Node node, String encoding) throws IOException {
        final BufferPoolByteArrayOutputStream out = new BufferPoolByteArrayOutputStream(1024);
        try {
            nodeToOutputStream(node, out, encoding);
            return out.toByteArray();
        } finally {
            out.close();
        }
    }

    public static String nodeToFormattedString(Node node) throws IOException {
        final BufferPoolByteArrayOutputStream out = new BufferPoolByteArrayOutputStream(1024);
        try {
            nodeToFormattedOutputStream(node, out);
            return out.toString();
        } finally {
            out.close();
        }
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
     * Finds the first and only child Element of a parent Element, throwing if any extraneous additional
     * child elements are detected.  Child nodes other than elements (text nodes, processing instructions,
     * comments, etc) are ignored.
     *
     * @param parent the element in which to search for children.  Must be non-null.
     * @return First child element or null if there aren't any.
     * @throws TooManyChildElementsException if the parent has more than one child element.
     */
    public static Element findOnlyOneChildElement( Element parent ) throws TooManyChildElementsException {
        NodeList children = parent.getChildNodes();
        Element found = null;
        for ( int i = 0; i < children.getLength(); i++ ) {
            Node n = children.item(i);
            if ( n.getNodeType() == Node.ELEMENT_NODE ) {
                if (found != null)
                    throw new TooManyChildElementsException(found.getNamespaceURI(), found.getLocalName());
                found = (Element)n;
            }
        }
        return found;
    }

    /**
     * Generates a Map of the namespace URIs and prefixes of the specified Node and all of its ancestor Elements.
     * <p>
     * URIs that were default namespaces will get a prefix starting with "default".
     * TODO this needs to be merged with getNamespaceMap()
     * @param n the node from which to gather namespaces
     * @return a Map of namespace URIs to prefixes.
     */
    public static Map getAncestorNamespaces(Node n) {
        Node current = n;
        int dflt = 0;
        Map namespaces = new HashMap();
        while (current != null) {
            if (current instanceof Element) {
                Element el = (Element)current;
                NamedNodeMap attributes = el.getAttributes();
                for ( int i = 0; i < attributes.getLength(); i++ ) {
                    Attr attr = (Attr)attributes.item(i);
                    String name = attr.getName();
                    String uri = attr.getValue();
                    String prefix = null;
                    if ("xmlns".equals(name)) {
                        prefix = "default" + ++dflt;
                    } else if (name.startsWith("xmlns:")) {
                        prefix = name.substring(6);
                    }
                    if (prefix != null) namespaces.put(uri, prefix);
                }
            }
            current = current.getParentNode();
        }
        return namespaces;
    }

    /**
     * Finds the first child {@link Element} of a parent {@link Element}
     * with the specified name that is in the specified namespace.
     *<p>
     * The parent must belong to a DOM produced by a namespace-aware parser,
     * and the name must be undecorated.
     *
     * @param parent the {@link Element} in which to search for children. Must be non-null.
     * @param nsuri the URI of the namespace to which the child must belong, NOT THE PREFIX!  May be null, in which
     *              case namespaces are not considered when checking for a match.
     * @param name the name of the element to find. Must be non-null.
     * @return First matching child {@link Element} or null if the specified parent contains no matching elements
     */
    public static Element findFirstChildElementByName( Element parent, String nsuri, String name ) {
        if ( name == null ) throw new IllegalArgumentException( "name must be non-null!" );
        NodeList children = parent.getChildNodes();
        for ( int i = 0; i < children.getLength(); i++ ) {
            Node n = children.item(i);
            if ( n.getNodeType() == Node.ELEMENT_NODE &&
                 name.equals( n.getLocalName()) &&
                 (nsuri == null || nsuri.equals( n.getNamespaceURI() )) )
                return (Element)n;
        }
        return null;
    }

    /**
     * Finds the first child {@link Element} of a parent {@link Element}
     * with the specified name that is in one of the specified namespaces.
     *<p>
     * The parent must belong to a DOM produced by a namespace-aware parser,
     * and the name must be undecorated.
     *
     * @param parent the {@link Element} in which to search for children. Must be non-null.
     * @param nsuris the URIs of the namespaces to which the child must belong, NOT THE PREFIX!  Must be non-null and non-empty.
     * @param name the name of the element to find. Must be non-null.
     * @return First matching child {@link Element} or null if the specified parent contains no matching elements
     */
    public static Element findFirstChildElementByName( Element parent, String[] nsuris, String name ) {
        if ( nsuris == null || nsuris.length < 1 || name == null )
            throw new IllegalArgumentException( "nsuris and name must be non-null and non-empty" );
        NodeList children = parent.getChildNodes();
        for ( int i = 0; i < children.getLength(); i++ ) {
            Node n = children.item(i);
            if ( n.getNodeType() == Node.ELEMENT_NODE &&
                 name.equals( n.getLocalName()) ) {
                for ( int j = 0; j < nsuris.length; j++) {
                    if (nsuris[j].equals(n.getNamespaceURI()))
                        return (Element)n;
                }
            }
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
     * @param nsuri the URI of the namespace to which the child must belong, NOT THE PREFIX!  Use null to match localName in any namespace.
     * @param name the name of the element to find. Must be non-null.
     * @return First matching child {@link Element} or null if the specified parent contains no matching elements
     * @throws com.l7tech.common.xml.TooManyChildElementsException if multiple matching child nodes are found
     */
    public static Element findOnlyOneChildElementByName( Element parent, String nsuri, String name ) throws TooManyChildElementsException {
        if ( name == null ) throw new IllegalArgumentException( "name must be non-null!" );
        NodeList children = parent.getChildNodes();
        Element result = null;
        for ( int i = 0; i < children.getLength(); i++ ) {
            Node n = children.item(i);
            if ( n.getNodeType() == Node.ELEMENT_NODE &&
                 name.equals( n.getLocalName()) &&
                 (nsuri == null || nsuri.equals( n.getNamespaceURI() )) ) {
                if ( result != null ) throw new TooManyChildElementsException( nsuri, name );
                result = (Element)n;
            }
        }
        return result;
    }

    /**
     * same as findOnlyOneChildElementByName but allows for different namespaces
     */
    public static Element findOnlyOneChildElementByName(Element parent, String[] namespaces, String name) throws TooManyChildElementsException {
        for (int i = 0; i < namespaces.length; i++) {
            Element res = findOnlyOneChildElementByName(parent, namespaces[i], name);
            if (res != null) return res;
        }
        return null;
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
     * Same as other findChildElementsByName but allows for different namespaces. This is practical when
     * an element can have different versions of the same namespace.
     *
     * @param namespaces an array containing all possible namespaces
     */
    public static List findChildElementsByName(Element parent, String[] namespaces, String name) {
        if ( namespaces == null || namespaces.length < 1 || name == null )
            throw new IllegalArgumentException( "nsuri and name must be non-null!" );
        List found = new ArrayList();

        NodeList children = parent.getChildNodes();
        for ( int i = 0; i < children.getLength(); i++ ) {
            Node n = children.item(i);
            if ( n.getNodeType() == Node.ELEMENT_NODE && name.equals( n.getLocalName()) ) {
                for (int j = 0; j < namespaces.length; j++) {
                    String namespace = namespaces[j];
                    if (namespace.equals(n.getNamespaceURI()))
                        found.add( n );
                }
            }
        }

        return found;
    }

    /**
     * Check if the given document contains any processing instructions.
     *
     * @param document the document
     * @return true if processing instructions are found.
     */
    public static boolean hasProcessingInstructions(Document document) {
        return !findProcessingInstructions(document).isEmpty();
    }

    /**
     * Get the processing instructions for the document.
     *
     * @param document the document
     * @return the immutable list of processing instructions (org.w3c.dom.ProcessingInstruction)
     */
    public static List findProcessingInstructions(Document document) {
        List piList = Collections.EMPTY_LIST;

        if(document!=null) {
            NodeList nodes = document.getChildNodes();
            int piCount = 0;
            for(int n=0; n<nodes.getLength(); n++) {
                Node node = nodes.item(n);
                if(node.getNodeType()==Node.PROCESSING_INSTRUCTION_NODE) {
                    piCount++;
                }
            }
            if(piCount>0) {
                List piNodes = new ArrayList(piCount);
                for(int n=0; n<nodes.getLength(); n++) {
                    Node node = nodes.item(n);
                    if(node.getNodeType()==Node.PROCESSING_INSTRUCTION_NODE) {
                        piNodes.add(node);
                    }
                }
                piList = Collections.unmodifiableList(piNodes);
            }
        }

        return piList;
    }

    /**
     * Removes all child Elements of a parent Element
     * with the specified name that are in the specified namespace.
     *
     * The parent must elong to a DOM produced by a namespace-aware parser,
     * and the name must be undecorated.
     *
     * @param parent the element in which to search for children to remove.  Must be non-null.
     * @param nsuri the URI of the namespace to which the children must belong, NOT THE PREFIX!  Must be non-null.
     * @param name the name of the elements to find.  Must be non-null.
     */
    public static void removeChildElementsByName( Element parent, String nsuri, String name ) {
        List found = findChildElementsByName( parent, nsuri, name );
        for (Iterator i = found.iterator(); i.hasNext();) {
            Node node = (Node)i.next();
            parent.removeChild(node);
        }
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

    public static boolean elementIsEmpty( Node element ) {
        if ( !element.hasChildNodes() ) return true;
        Node kid = element.getFirstChild();
        while ( kid != null ) {
            if ( kid.getNodeType() != Node.ATTRIBUTE_NODE ) return false;
            kid = kid.getNextSibling();
        }
        return true;
    }

    /**
     * Gets the child text node value for an element.
     * @return a String consisting of all text nodes glued together and then trimmed.  May be empty but never null.
     */
    public static String getTextValue(Element node) {
        StringBuffer output = new StringBuffer();
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node kid = children.item(i);
            if (kid.getNodeType() == Node.TEXT_NODE || kid.getNodeType() == Node.CDATA_SECTION_NODE) {
                String thisTxt = kid.getNodeValue();
                if (thisTxt != null)
                    output.append(thisTxt);
            }
        }
        return output.toString().trim();
    }

    /**
     * If the specified element has a declaration of the specified namespace already in scope, returns a
     * ready-to-use prefix string for this namespace.  Otherwise returns null.
     * <p>
     * Element is searched for xmlns:* attributes defining the requested namespace.  If it doesn't have one,
     * it's parent is searched, and so on until it is located or we have searched all the way up to the documentElement.
     *<p>
     * This method only finds declarations that define a namespace prefix, and
     * will ignore declarations changing the default namespace.  So in this example:
     *
     *    &lt;foo xmlns="http://wanted_namespace /&gt;
     *
     * a query for wanted_namespace will return null.
     *
     * @param element   the element at which to start searching
     * @param namespace the namespace URI to look for
     * @return the prefix for this namespace in scope at the specified elment, or null if it is not declared with a prefix.
     *         Note that the default namespace is not considered to have been declared with a prefix.
     */
    public static String findActivePrefixForNamespace(Node element, String namespace) {

        while (element != null) {
            NamedNodeMap attrs = element.getAttributes();
            if (attrs != null) {
                int numAttr = attrs.getLength();
                for (int i = 0; i < numAttr; ++i) {
                    Attr attr = (Attr)attrs.item(i);
                    if (!"xmlns".equals(attr.getPrefix()))
                        continue;
                    if (namespace.equals(attr.getValue()))
                        return attr.getLocalName();
                }
            }

            if (element == element.getOwnerDocument().getDocumentElement())
                return null;

            element = element.getParentNode();
        }

        return null;
    }

    /**
     * Find a namespace prefix which is free to use within the specified element.  Caller specifies
     * the desired prefix.  This method will first check for the required prefix; then the required prefix with "1"
     * appended, then "2", etc.  The returned prefix is guaranteed to be undeclared by the specified element
     * or its direct ancestors.
     * @param element  the element to examine
     * @param desiredPrefix  the desired namespace prefix
     * @return An namespace prefix as close as possible to desiredPrefix that is undeclared by this element or
     *         its direct ancestors.
     */
    public static String findUnusedNamespacePrefix(Node element, String desiredPrefix) {
        // Find all used prefixes
        Set usedPrefixes = new HashSet();
        while (element != null) {
            NamedNodeMap attrs = element.getAttributes();
            if (attrs != null) {
                int numAttr = attrs.getLength();
                for (int i = 0; i < numAttr; ++i) {
                    Attr attr = (Attr)attrs.item(i);
                    if (!"xmlns".equals(attr.getPrefix()))
                        continue;
                    usedPrefixes.add(attr.getLocalName());
                }
            }
            if (element == element.getOwnerDocument().getDocumentElement())
                break;
            element = element.getParentNode();
        }

        // Generate an unused prefix
        long count = 0;
        String testPrefix = desiredPrefix;
        while (usedPrefixes.contains(testPrefix)) testPrefix = desiredPrefix + count++;

        return testPrefix;
    }

    /**
     * Finds an existing declaration of the specified namespace already in scope, or creates a new one in the
     * specified element, and then returns the active prefix for this namespace URI.  If a new prefix is declared,
     * it will be as close as possible to desiredPrefix (that is, identical unless some other namespace is already
     * using it in which case it will be desiredPrefix with one or more digits appended to make it unique).
     * @param element    the element under whose scope the namespace should be valid.  Must not be null.   
     * @param namespace  the namespace to be declared.  Must not be null or empty.
     * @param desiredPrefix  Preferred prefix, if a new namespace declaration is needed.  If this is specified, this method never returns null.
     * @return the prefix to use for this namespace.  May be null if it's the default prefix and desiredPrefix was null,
     *         but never empty.
     */
    public static String getOrCreatePrefixForNamespace(Element element, String namespace, String desiredPrefix) {
        String existingPrefix = findActivePrefixForNamespace(element, namespace);
        if (existingPrefix != null)
            return existingPrefix;
        String prefix = findUnusedNamespacePrefix(element, desiredPrefix);
        if (prefix != null && prefix.length() > 0) {
            Attr decl = element.getOwnerDocument().createAttributeNS(XMLNS_NS, "xmlns:" + prefix);
            decl.setValue(namespace);
            element.setAttributeNodeNS(decl);
        }
        return prefix == null || prefix.length() < 1 ? null : prefix;
    }

    /**
     * Find the first of the given namespaces that is in use.
     *
     * @param element the element to check
     * @param namespaceUris the namespace uris to check
     * @return the first used namespace or null if none are used
     */
    public static String findActiveNamespace(Element element, String[] namespaceUris) {
        String foundNamespaceURI = null;

        // check default ns
        String elementDefaultNamespaceURI = element.getNamespaceURI();
        if(elementDefaultNamespaceURI!=null) {
            for (int i = 0; i < namespaceUris.length; i++) {
                String namespaceUri = namespaceUris[i];
                if(elementDefaultNamespaceURI.equals(namespaceUri)) {
                    foundNamespaceURI = namespaceUri;
                    break;
                }
            }
        }

        // check ns declarations
        if(foundNamespaceURI==null) {
            for (int i = 0; i < namespaceUris.length; i++) {
                String namespaceUri = namespaceUris[i];
                if(findActivePrefixForNamespace(element, namespaceUri)!=null) {
                    foundNamespaceURI = namespaceUri;
                    break;
                }
            }
        }

        return foundNamespaceURI;
    }

    /**
     * Creates an empty element and appends it to the end of Parent.  The element will share the parent's namespace
     * URI and prefix.
     *
     * @param parent  parent element.  Must not be null.
     * @param localName  new local name.  Must not be null or empty.
     * @return the newly created element, which has already been appended to parent.
     */
    public static Element createAndAppendElement(Element parent, String localName) {
        Element element = parent.getOwnerDocument().createElementNS(parent.getNamespaceURI(), localName);
        parent.appendChild(element);
        element.setPrefix(parent.getPrefix());
        return element;
    }

    /**
     * Creates an element and appends it to the end of Parent.  The element will be in the requested namespace.
     * If the namespace is already declared in parent or a direct ancestor then that prefix will be reused;
     * otherwise a new prefix will be declared in the new element that is as close as possible to desiredPrefix.
     * @param parent
     * @param namespace
     * @param desiredPrefix
     */
    public static Element createAndAppendElementNS(Element parent, String localName, String namespace, String desiredPrefix) {
        Element element = parent.getOwnerDocument().createElementNS(namespace, localName);
        parent.appendChild(element);
        element.setPrefix(getOrCreatePrefixForNamespace(element, namespace, desiredPrefix));
        return element;
    }

    /**
     * Creates an element and inserts it as the first child of Parent.  The element will be in the requested namespace.
     * If the namespace is already declared in parent or a direct ancestor then that prefix will be reused;
     * otherwise a new prefix will be declared in the new element that is as close as possible to desiredPrefix.
     * @param parent
     * @param namespace
     * @param desiredPrefix
     */
    public static Element createAndPrependElementNS(Element parent, String localName, String namespace, String desiredPrefix) {
        if (desiredPrefix == null) desiredPrefix = "ns";
        Element element = parent.getOwnerDocument().createElementNS(namespace, localName);
        Node firstSib = parent.getFirstChild();
        if (firstSib != null)
            parent.insertBefore(element, firstSib);
        else
            parent.appendChild(element);
        element.setPrefix(getOrCreatePrefixForNamespace(element, namespace, desiredPrefix));
        return element;
    }

    /**
     * Creates an element and inserts it before desiredNextSibling, under desiredNextSibling's parent element.
     * The element will be in the requested namespace.  If the namespace is already declared in desiredNextSibling's
     * parent or a direct ancestor then that prefix will be reused; otherwise a new prefix will be declared in
     * the new element that is as close as possible to desiredPrefix.
     * @param desiredNextSibling
     * @param localName
     * @param namespace
     * @param desiredPrefix
     */
    public static Element createAndInsertBeforeElementNS(Node desiredNextSibling, String localName,
                                                         String namespace, String desiredPrefix)
    {
        Element parent = (Element)desiredNextSibling.getParentNode();
        Element element = parent.getOwnerDocument().createElementNS(namespace, localName);
        parent.insertBefore(element, desiredNextSibling);
        element.setPrefix(getOrCreatePrefixForNamespace(element, namespace, desiredPrefix));
        return element;
    }

    /** @return true iff. prospectiveAncestor is a direct ancestor of element (or is the same element). */
    public static boolean isElementAncestor(Element element, Element prospectiveAncestor) {
        while (element != null) {
            if (element == prospectiveAncestor)
                return true;
            if (element == element.getOwnerDocument().getDocumentElement())
                return false;
            element = (Element)element.getParentNode();
        }

        return false;
    }

    /**
     * Safely create a text node.  Just like node.getOwnerDocument().createTextNode(), except will
     * translate a null nodeValue into the empty string.  A warning is logged whenever
     * this safety net is used.
     *
     * @param factory
     * @param nodeValue
     */
    public static Text createTextNode(Node factory, String nodeValue) {
        if (nodeValue == null) {
            final String msg = "Attempt to create DOM text node with null value; using empty string instead.  Please report this.";
            logger.log(Level.WARNING, msg, new NullPointerException(msg));
            nodeValue = "";
        }
        if (factory.getNodeType() == Node.DOCUMENT_NODE)
            return ((Document)factory).createTextNode(nodeValue);
        return factory.getOwnerDocument().createTextNode(nodeValue);
    }

    /**
     * Create a document with an element of another document while maintaining the
     * namespace declarations of the parent elements.
     * @param schema the Element on which to base the new XML.
     * @return the new XML as a String.  Never null.
     * @throws IOException if the serializer has a problem reading the source DOM.
     */
    public static String elementToXml(Element schema) throws IOException {
        DocumentBuilder builder = getDocumentBuilder();
        Document schemadoc = builder.newDocument();
        Element newRootNode = (Element)schemadoc.importNode(schema, true);
        schemadoc.appendChild(newRootNode);
        // remember all namespace declarations of parent elements
        Node node = schema.getParentNode();
        while (node != null) {
            if (node instanceof Element) {
                Element el = (Element)node;
                NamedNodeMap attrsmap = el.getAttributes();
                for (int i = 0; i < attrsmap.getLength(); i++) {
                    Attr attrnode = (Attr)attrsmap.item(i);
                    if (attrnode.getName().startsWith("xmlns:")) {
                        newRootNode.setAttribute(attrnode.getName(), attrnode.getValue());
                    }
                }

            }
            node = node.getParentNode();
        }
        // output to string
        final StringWriter sw = new StringWriter(512);
        XMLSerializer xmlSerializer = new XMLSerializer();
        xmlSerializer.setOutputCharStream(sw);
        OutputFormat of = new OutputFormat();
        of.setIndent(4);
        xmlSerializer.setOutputFormat(of);
        xmlSerializer.serialize(schemadoc);
        return sw.toString();
    }

    public static class BadSchemaException extends Exception {
        public BadSchemaException(String s){super(s);}
        public BadSchemaException(Throwable e){super(e.getMessage(), e);}
    }

    public static String getSchemaTNS(String schemaSrc) throws BadSchemaException {
        if (schemaSrc == null) {
            throw new BadSchemaException("no xml");
        }
        /* can't do this as it ends up chocking on unresolved imports which is beyond the scope here
        // 1. pass through the javax.xml.validation.SchemaFactory
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        if (resolver != null) {
            factory.setResourceResolver(resolver);
        }
        try {
            factory.newSchema(new StreamSource(new ByteArrayInputStream(schemaSrc.getBytes())));
        } catch (Exception e) {
            throw new BadSchemaException(e);
        }
        */
        // 2. pass through SchemaDocument
        SchemaDocument sdoc;
        try {
            sdoc = SchemaDocument.Factory.parse(new StringReader(schemaSrc));
        } catch (Exception e) {
            throw new BadSchemaException(e);
        }
        return sdoc.getSchema().getTargetNamespace();
    }

    private static final Logger logger = Logger.getLogger(XmlUtil.class.getName());
    private static DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    public static final String XERCES_DISALLOW_DOCTYPE = "http://apache.org/xml/features/disallow-doctype-decl";

    static {
        dbf.setNamespaceAware(true);
        dbf.setAttribute(XERCES_DISALLOW_DOCTYPE, Boolean.TRUE);
    }

    private static DocumentBuilderFactory dbfAllowingDoctype = DocumentBuilderFactory.newInstance();
    static {
        dbfAllowingDoctype.setNamespaceAware(true);
    }

    /**
     * Get the map of all namespace declrations in scope for the current element.  If there is a default
     * namespace in scope, it will have the empty string "" as its key.
     * TODO this needs to be merged with getAncestorNamespaces()
     * @param element the element whose in-scope namespace declrations will be extracted.  Must not be null.
     * @return The map of namespace declarations in scope for this elements immediate children.
     */
    public static Map getNamespaceMap(Element element) {
        Map nsmap = new HashMap();

        while (element != null) {
            NamedNodeMap attrs = element.getAttributes();
            int numAttr = attrs.getLength();
            for (int i = 0; i < numAttr; ++i) {
                Attr attr = (Attr)attrs.item(i);
                if ("xmlns".equals(attr.getName()))
                    nsmap.put("", attr.getValue()); // new default namespace
                else if ("xmlns".equals(attr.getPrefix())) // new namespace decl for prefix
                    nsmap.put(attr.getLocalName(), attr.getValue());
            }

            if (element == element.getOwnerDocument().getDocumentElement())
                break;

            element = (Element)element.getParentNode();
        }

        return nsmap;
    }

    /** Replace all descendants of the specified Element with the specified text content. */
    public static void setTextContent(Element e, String text) {
        removeAllChildren(e);
        e.appendChild(createTextNode(e, text));
    }

    /** Remove all descendants from the specified element, rendering it empty. */
    public static void removeAllChildren(Element e) {
        NodeList kids = e.getChildNodes();
        for (int i = 0; i < kids.getLength(); ++i) {
            Node kid = kids.item(i);
            e.removeChild(kid);
        }
    }

    public static Map findAllNamespaces(Element element) {
        Map entries = new HashMap();
        collectNamespaces(element, entries);
        NodeList nodes = element.getElementsByTagName("*");
        for (int i = 0; i < nodes.getLength(); i++) {
            Node n = nodes.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                collectNamespaces((Element)n, entries);
            }
        }

        Map result = new HashMap();
        int ns = 1;
        for (Iterator i = entries.keySet().iterator(); i.hasNext();) {
            String uri = (String)i.next();
            String prefix = (String)entries.get(uri);
            if (prefix == null) prefix = "ns" + ns++;
            result.put(prefix, uri);
        }

        return result;
    }

    private static void collectNamespaces(Element n, Map entries) {
        NamedNodeMap foo = n.getAttributes();
        // Find xmlns:foo, xmlns=
        for (int j = 0; j < foo.getLength(); j++) {
            Attr attrNode = (Attr)foo.item(j);
            String attPrefix = attrNode.getPrefix();
            String attNsUri = attrNode.getNamespaceURI();
            String attLocalName = attrNode.getLocalName();
            String attValue = attrNode.getValue();

            if (entries.get(attValue) != null) continue;

            // Bug 2053: Avoid adding xmlns="" to the map
            if (attValue != null && attValue.trim().length() > 0) {
                if ("xmlns".equals(attPrefix) && XmlUtil.XMLNS_NS.equals(attNsUri)) {
                    entries.put(attValue, attLocalName);
                } else if ("xmlns".equals(attLocalName)) {
                    entries.put(attValue, null);
                }
            }
        }
    }

    /**
     * Checks that the namespace of the passed element is one of the namespaces
     * passed.
     *
     * @param el the element to check
     * @param possibleNamespaces (uris, not prefixes ...), may contain a null entry
     * @return  true if the namespace matches
     */
    public static boolean elementInNamespace(Element el, String[] possibleNamespaces) {
        boolean hasNamespace = false;
        String ns = el.getNamespaceURI();
        for (int i = 0; i < possibleNamespaces.length; i++) {
            if (ns==null) {
                if(possibleNamespaces[i]==null) {
                    hasNamespace = true;
                    break;
                }
            }
            else if (ns.equals(possibleNamespaces[i])) {
                hasNamespace = true;
                break;
            }
        }
        return hasNamespace;
    }

    /**
     * Strips leading and trailing whitespace from all text nodes under the specified element.
     * Whitespace-only text nodes have their text content replaced with the empty string.
     * Note that this is almost certain to break any signature that may have been made on this
     * XML.
     * <p>
     * It's a good idea to serialize and reparse the document, to get rid of the empty text nodes,
     * before passing it on to any further XML processing code that might not be expecting them.
     * <p>
     * <b>Note</b>: this method requires that the input DOM Document does not contain two consecutive
     * TEXT nodes.  You may be able to guarantee that this is the case be serializing and reparsing
     * the document, if there is any doubt about its current status, and assuming your parser provides
     * this guarantee.
     *
     * @param node  the element to convert.  This element and all children will have all child text nodes trimmed of
     *              leading and trailing whitespace.
     * @throws SAXException if the input element or one of its child elements is found to contain two consecutive
     *                      TEXT nodes.
     */
    public static void stripWhitespace(Element node) throws SAXException {
        NodeList children = node.getChildNodes();
        boolean lastWasText = false;
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            switch (n.getNodeType()) {
                case Node.TEXT_NODE:
                    if (lastWasText) throw new SAXException("Consecutive TEXT nodes are not supported");
                    String v = n.getNodeValue();
                    if (v == null) v = "";
                    n.setNodeValue(v.trim());
                    lastWasText = true;
                    break;
                case Node.ELEMENT_NODE:
                    stripWhitespace((Element)n);
                    lastWasText = false;
                    break;
                default:
                    lastWasText = false;
            }
        }
    }

    public static Document softXSLTransform(Document source, Transformer transformer, Map params) throws TransformerException {
        final DOMResult outputTarget = new DOMResult();
        if (params != null && !params.isEmpty()) {
            for (Iterator i = params.keySet().iterator(); i.hasNext();) {
                String name = (String) i.next();
                if (name == null) continue;
                Object value = params.get(name);
                if (value == null) continue;
                transformer.setParameter(name, value);
            }
        }
        transformer.transform(new DOMSource(source), outputTarget);
        final Node node = outputTarget.getNode();
        if (node instanceof Document) {
            return (Document)node;
        } else if (node != null) {
            return node.getOwnerDocument();
        } else {
            return null;
        }
    }
}
