/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.util;

import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.*;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.*;
import java.util.logging.Logger;

import com.l7tech.common.xml.TooManyChildElementsException;
import com.ibm.xml.dsig.transform.W3CCanonicalizer2WC;
import com.ibm.xml.dsig.Canonicalizer;

/**
 * Thread-local XML parsing and pretty-printing utilities.
 * User: mike
 * Date: Aug 28, 2003
 * Time: 4:20:59 PM
 */
public class XmlUtil {
    private static final EntityResolver SAFE_ENTITY_RESOLVER = new EntityResolver() {
        public InputSource resolveEntity( String publicId, String systemId ) throws SAXException {
            String msg = "Document referred to an external entity with system id '" + systemId + "'";
            logger.warning( msg );
            throw new SAXException(msg);
        }
    };
    
    /** This is the namespace that the special namespace prefix "xmlns" logically belongs to. */
    public static final String XMLNS_NS = "http://www.w3.org/2000/xmlns/";
    
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

    private static ThreadLocal documentBuilder = new ThreadLocal() {
        protected synchronized Object initialValue() {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            try {
                DocumentBuilder builder = dbf.newDocumentBuilder();
                builder.setEntityResolver(SAFE_ENTITY_RESOLVER);
                return builder;
            } catch (ParserConfigurationException e) {
                throw new RuntimeException(e); // can't happen
            }
        }
    };

    public static DocumentBuilder getDocumentBuilder() {
        return (DocumentBuilder)documentBuilder.get();
    }

    public static Document stringToDocument(String inputXmlNotAUrl) throws IOException, SAXException {
        ByteArrayInputStream bis = new ByteArrayInputStream(inputXmlNotAUrl.getBytes());
        return parse(bis);
    }

    public static Document parse(InputStream input) throws IOException, SAXException {
        return getDocumentBuilder().parse(input);
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

    private static XMLSerializer getFormattedXmlSerializer() {
        return (XMLSerializer) formattedXMLSerializer.get();
    }

    private static ThreadLocal transparentXMLSerializer = new ThreadLocal() {
        protected synchronized Object initialValue() {
            return new W3CCanonicalizer2WC();
        }
    };

    private static Canonicalizer getTransparentXMLSerializer() {
        return (Canonicalizer)transparentXMLSerializer.get();
    }

    public static void documentToOutputStream(Document doc, OutputStream os) throws IOException {
        Canonicalizer canon = getTransparentXMLSerializer();
        canon.canonicalize(doc, os);
    }

    public static void documentToFormattedOutputStream(Document doc, OutputStream os) throws IOException {
        getFormattedXmlSerializer().setOutputCharStream(new OutputStreamWriter(os));
        getFormattedXmlSerializer().serialize(doc);
    }

    public static String documentToString(Document doc) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
        Canonicalizer canon = getTransparentXMLSerializer();
        canon.canonicalize(doc, out);
        return out.toString();
    }

    public static String documentToFormattedString(Document doc) throws IOException {
        final StringWriter sw = new StringWriter();
        getFormattedXmlSerializer().setOutputCharStream(sw);
        getFormattedXmlSerializer().serialize(doc);
        return sw.toString();
    }

    public static String elementToString(Element element) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
        Canonicalizer canon = getTransparentXMLSerializer();
        canon.canonicalize(element, out);
        return out.toString();
    }

    public static String elementToFormattedString(Element element) throws IOException {
        final StringWriter sw = new StringWriter();
        getFormattedXmlSerializer().setOutputCharStream(sw);
        getFormattedXmlSerializer().serialize(element);
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
     *<p>
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
     * @param nsuri the URI of the namespace to which the child must belong, NOT THE PREFIX!  Must be non-null.
     * @param name the name of the element to find. Must be non-null.
     * @return First matching child {@link Element} or null if the specified parent contains no matching elements
     * @throws com.l7tech.common.xml.TooManyChildElementsException if multiple matching child nodes are found
     */
    public static Element findOnlyOneChildElementByName( Element parent, String nsuri, String name ) throws TooManyChildElementsException {
        if ( nsuri == null || name == null ) throw new IllegalArgumentException( "nsuri and name must be non-null!" );
        NodeList children = parent.getChildNodes();
        Element result = null;
        for ( int i = 0; i < children.getLength(); i++ ) {
            Node n = children.item(i);
            if ( n.getNodeType() == Node.ELEMENT_NODE &&
                 name.equals( n.getLocalName()) &&
                 nsuri.equals( n.getNamespaceURI() ) ) {
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

    public static boolean elementIsEmpty( Element element ) {
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
     */
    public static String getTextValue(Element node) {
        StringBuffer output = new StringBuffer();
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node kid = children.item(i);
            if (kid.getNodeType() == Node.TEXT_NODE) {
                String thisTxt = kid.getNodeValue();
                if (thisTxt != null) {
                    thisTxt = thisTxt.trim();
                    if (thisTxt != null && thisTxt.length() > 0) {
                        output.append(thisTxt);
                    }
                }
            }
        }
        return output.toString();
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
    public static String findActivePrefixForNamespace(Element element, String namespace) {

        while (element != null) {
            NamedNodeMap attrs = element.getAttributes();
            int numAttr = attrs.getLength();
            for (int i = 0; i < numAttr; ++i) {
                Attr attr = (Attr)attrs.item(i);
                if (!"xmlns".equals(attr.getPrefix()))
                    continue;
                if (namespace.equals(attr.getValue()))
                    return attr.getLocalName();
            }

            if (element == element.getOwnerDocument().getDocumentElement())
                return null;

            element = (Element)element.getParentNode();
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
    public static String findUnusedNamespacePrefix(Element element, String desiredPrefix) {
        // Find all used prefixes
        Set usedPrefixes = new HashSet();
        while (element != null) {
            NamedNodeMap attrs = element.getAttributes();
            int numAttr = attrs.getLength();
            for (int i = 0; i < numAttr; ++i) {
                Attr attr = (Attr)attrs.item(i);
                if (!"xmlns".equals(attr.getPrefix()))
                    continue;
                usedPrefixes.add(attr.getLocalName());
            }
            if (element == element.getOwnerDocument().getDocumentElement())
                return desiredPrefix;
            element = (Element)element.getParentNode();
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
     * @param element
     * @param namespace
     * @param desiredPrefix
     * @return
     */
    public static String getOrCreatePrefixForNamespace(Element element, String namespace, String desiredPrefix) {
        String existingPrefix = findActivePrefixForNamespace(element, namespace);
        if (existingPrefix != null)
            return existingPrefix;
        String prefix = findUnusedNamespacePrefix(element, desiredPrefix);
        Attr decl = element.getOwnerDocument().createAttributeNS(XMLNS_NS, "xmlns:" + prefix);
        decl.setValue(namespace);
        element.setAttributeNodeNS(decl);
        return prefix;
    }
    
    /**
     * Creates an element and appends it to the end of Parent.  The element will be in the requested namespace.
     * If the namespace is already declared in parent or a direct ancestor then that prefix will be reused;
     * otherwise a new prefix will be declared in the new element that is as close as possible to desiredPrefix.
     * @param parent
     * @param namespace
     * @param desiredPrefix
     * @return
     */ 
    public static Element createAndAppendElementNS(Element parent, String localName, String namespace, String desiredPrefix) {
        Element element = parent.getOwnerDocument().createElementNS(namespace, localName);
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
     * @return
     */
    public static Element createAndInsertBeforeElementNS(Element desiredNextSibling, String localName,
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

    private static final Logger logger = Logger.getLogger(XmlUtil.class.getName());
}
