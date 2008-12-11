package com.l7tech.util;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.text.MessageFormat;

/**
 * DomUtils collects helper methods for DOM manipulation.
 */
@SuppressWarnings({"unchecked", "ForLoopReplaceableByForEach"})
public class DomUtils {
    private static final Logger logger = Logger.getLogger(DomUtils.class.getName());

    /** This is the namespace that the special namespace prefix "xmlns" logically belongs to. */
    public static final String XMLNS_NS = "http://www.w3.org/2000/xmlns/";

    /** This is the namespace that the special namespace prefix "xml" logically belongs to. */
    //public static final String XML_NS = "http://www.w3.org/XML/1998/namespace";


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
     * @param parent the {@link Element} or {@link DocumentFragment} in which to search for children. Must be non-null.
     * @param nsuri the URI of the namespace to which the child must belong, NOT THE PREFIX!  May be null, in which
     *              case namespaces are not considered when checking for a match.
     * @param name the name of the element to find. Must be non-null.
     * @return First matching child {@link Element} or null if the specified parent contains no matching elements
     */
    public static Element findFirstChildElementByName( Node parent, String nsuri, String name ) {
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
     * @param parent the {@link Element} or {@link DocumentFragment} in which to search for children. Must be non-null.
     * @param nsuris the URIs of the namespaces to which the child must belong, NOT THE PREFIX!  Must be non-null and non-empty.
     * @param name the name of the element to find. Must be non-null.
     * @return First matching child {@link Element} or null if the specified parent contains no matching elements
     */
    public static Element findFirstChildElementByName( Node parent, String[] nsuris, String name ) {
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
     * with the specified name that is in the specified namespace, and throws
     * {@link com.l7tech.util.MissingRequiredElementException} if such an element cannot be found.
     *
     * The parent must belong to a DOM produced by a namespace-aware parser,
     * and the name must be undecorated.
     *
     * @param parent the {@link Element} in which to search for children. Must be non-null.
     * @param nsuri the URI of the namespace to which the child must belong, NOT THE PREFIX!  Use null to match localName in any namespace.
     * @param name the name of the element to find. Must be non-null.
     * @return First matching child {@link Element}
     * @throws TooManyChildElementsException if multiple matching child nodes are found
     * @throws MissingRequiredElementException if no matching child node is found
     */
    public static Element findExactlyOneChildElementByName( Element parent, String nsuri, String name ) throws TooManyChildElementsException, MissingRequiredElementException {
        return findOnlyOneChildElementByName0(parent, nsuri, name, true);
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
     * @throws TooManyChildElementsException if multiple matching child nodes are found
     */
    public static Element findOnlyOneChildElementByName( Element parent, String nsuri, String name ) throws TooManyChildElementsException {
        try {
            return findOnlyOneChildElementByName0(parent, nsuri, name, false);
        } catch (MissingRequiredElementException e) {
            throw new RuntimeException(e); // Can't happen--honest!
        }
    }

    private static Element findOnlyOneChildElementByName0(Element parent, String nsuri, String name, boolean mustBePresent)
        throws TooManyChildElementsException, MissingRequiredElementException {
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
        if (mustBePresent && result == null) throw new MissingRequiredElementException(MessageFormat.format("Required element {{0}}{1} not found", nsuri, name));
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
     * Find the first sibling Element that is after the given element.
     *
     * @return the Element or null if not found.
     */
    public static Element findNextElementSibling(final Element element) {
        Element sibling = null;
        Node current = element;

        while (current != null) {
            current = current.getNextSibling();
            if (current != null && current.getNodeType() == Node.ELEMENT_NODE) {
                sibling = (Element) current;
                break;
            }
        }

        return sibling;
    }

    /**
     * Find the first sibling Element that is before the given element.
     *
     * @return the Element or null if not found.
     */
    public static Element findPrevElementSibling(final Element element) {
        Element sibling = null;
        Node current = element;

        while (current != null) {
            current = current.getPreviousSibling();
            if (current != null && current.getNodeType() == Node.ELEMENT_NODE) {
                sibling = (Element) current;
                break;
            }
        }

        return sibling;
    }

    /**
     * Returns a list of all child {@link Element}s of a parent {@link Element}
     * with the specified name that are in the specified namespace.
     *
     * The parent must belong to a DOM produced by a namespace-aware parser,
     * and the name must be undecorated.
     *
     * @param parent the {@link Element} in which to search for children. Must be non-null.
     * @param nsuri the URI of the namespace to which the children must belong, NOT THE PREFIX!  May be null to request only the default namespace.
     * @param name the name of the elements to find. Must be non-null.
     * @return A {@link List} containing all matching child {@link Element}s. Will be empty if the specified parent contains no matching elements
     */
    public static List<Element> findChildElementsByName( Element parent, String nsuri, String name ) {
        if ( name == null ) throw new IllegalArgumentException( "name must be non-null!" );
        List found = new ArrayList<Element>();

        NodeList children = parent.getChildNodes();
        for ( int i = 0; i < children.getLength(); i++ ) {
            Node n = children.item(i);
            if ((n.getNodeType() == Node.ELEMENT_NODE) &&
                name.equals(n.getLocalName()) &&
                (((nsuri == null) && (n.getNamespaceURI() == null)) || ((nsuri != null) && nsuri.equals(n.getNamespaceURI()))))
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
    public static List<Element> findChildElementsByName(Element parent, String[] namespaces, String name) {
        if ( namespaces == null || namespaces.length < 1 || name == null )
            throw new IllegalArgumentException( "nsuri and name must be non-null!" );
        List<Element> found = new ArrayList<Element>();

        NodeList children = parent.getChildNodes();
        for ( int i = 0; i < children.getLength(); i++ ) {
            Node n = children.item(i);
            if ( n.getNodeType() == Node.ELEMENT_NODE && name.equals( n.getLocalName()) ) {
                for (int j = 0; j < namespaces.length; j++) {
                    String namespace = namespaces[j];
                    if (namespace.equals(n.getNamespaceURI()))
                        found.add((Element) n);
                }
            }
        }

        return found;
    }

    /**
     * Check if the given Node has any child Nodes of the specified type.
     *
     * <p>This will check only children, not all descendants.</p>
     *
     * @param parent   The parent node (may be null)
     * @param nodeType The type of Node to check for
     * @return True if the given node has a child Node of the given type
     */
    public static boolean hasChildNodesOfType(Node parent, short nodeType) {
        boolean hasChildNodesOfType = false;

        if (parent != null && parent.hasChildNodes()) {
            Node child = parent.getFirstChild();
            while (child != null && !hasChildNodesOfType) {
                if (child.getNodeType() == nodeType) {
                    hasChildNodesOfType = true;
                }
                child = child.getNextSibling();
            }
        }

        return hasChildNodesOfType;
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
     * @param parent The {@link Element} or {@link DocumentFragment} to which the new element is added
     * @param namespace
     * @param desiredPrefix
     */
    public static Element createAndAppendElementNS(Node parent, String localName, String namespace, String desiredPrefix) {
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
    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
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
     * Invokes the specified visitor on all immediate child elements of the specified element.
     *
     * @param element the element whose child elements to visit.  Required.
     * @param visitor a visitor to invoke on each immediate child element.  Required.
     */
    public static void visitChildElements(Element element, Functions.UnaryVoid<Element> visitor) {
        NodeList kids = element.getChildNodes();
        if (kids == null) return;
        int len = kids.getLength();
        if (len < 1) return;
        for (int i = 0; i < len; ++i) {
            Node kid = kids.item(i);
            if (kid instanceof Element)
                visitor.call((Element)kid);
        }
    }


    /**
     * Invokes the specified visitor on all descendants of the specified node.
     *
     * @param node the element whose child elements to visit.  Required.
     * @param visitor a visitor to invoke on each immediate child element.  Required.
     */
    public static void visitNodes( Node node, Functions.UnaryVoid<Node> visitor ) {
        if ( node != null ) {
            visitor.call( node );

            // visit attributes
            NamedNodeMap attrNodeMap = node.getAttributes();
            if ( attrNodeMap != null ) {
                for ( int n=0; n<attrNodeMap.getLength(); n++ ) {
                    Node attrNode = attrNodeMap.item(n);
                    visitor.call( attrNode );
                }
            }

            // visit children
            visitNodes( node.getFirstChild(), visitor );

            // visit siblings
            visitNodes( node.getNextSibling(), visitor );
        }
    }

    /**
     * Get the prefix for the given name "prefix:local"
     *
     * @param prefixAndLocal The prefixed name
     * @return The prefix or an empty string
     */
    public static String getNamespacePrefix(String prefixAndLocal) {
        String prefix = "";

        if (prefixAndLocal != null) {
            int index = prefixAndLocal.indexOf(':');
            if (index > -1) {
                prefix = prefixAndLocal.substring(0,index);
            }
        }

        return prefix;
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
            addToNamespaceMap(element, nsmap);

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
        NamedNodeMap foo = element.getAttributes();
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
                if ("xmlns".equals(attPrefix) && DomUtils.XMLNS_NS.equals(attNsUri)) {
                    entries.put(attValue, attLocalName);
                } else if ("xmlns".equals(attLocalName)) {
                    entries.put(attValue, null);
                }
            }
        }
        NodeList nodes = element.getElementsByTagName("*");
        for (int i = 0; i < nodes.getLength(); i++) {
            Node n = nodes.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                NamedNodeMap foo1 = n.getAttributes();
                // Find xmlns:foo, xmlns=
                for (int j = 0; j < foo1.getLength(); j++) {
                    Attr attrNode = (Attr) foo1.item(j);
                    String attPrefix = attrNode.getPrefix();
                    String attNsUri = attrNode.getNamespaceURI();
                    String attLocalName = attrNode.getLocalName();
                    String attValue = attrNode.getValue();

                    if (entries.get(attValue) != null) continue;

                    // Bug 2053: Avoid adding xmlns="" to the map
                    if (attValue != null && attValue.trim().length() > 0) {
                        if ("xmlns".equals(attPrefix) && DomUtils.XMLNS_NS.equals(attNsUri)) {
                            entries.put(attValue, attLocalName);
                        } else if ("xmlns".equals(attLocalName)) {
                            entries.put(attValue, null);
                        }
                    }
                }
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

    /**
     * Strip all elements and attributes using the given namespace from the
     * given node.
     *
     * <p>Note that this does not remove any namespace declarations.</p>
     *
     * @param node The node to check.
     */
    public static void stripNamespace(Node node, String namespace) {
        if (node != null && namespace != null) {
            // attributes
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                NamedNodeMap nodeAttrs = node.getAttributes();
                List attrsForRemoval = new ArrayList();
                for (int n=0; n<nodeAttrs.getLength(); n++) {
                    Attr attribute = (Attr) nodeAttrs.item(n);
                    if (namespace.equals(attribute.getNamespaceURI())) {
                        attrsForRemoval.add(attribute);
                    }
                }
                for (Iterator iterator = attrsForRemoval.iterator(); iterator.hasNext();) {
                    Attr attribute = (Attr) iterator.next();
                    ((Element)node).removeAttributeNode(attribute);
                }
            }

            // children
            NodeList nodes = node.getChildNodes();
            List nodesForRemoval = new ArrayList();
            for (int n=0; n<nodes.getLength(); n++) {
                Node child = nodes.item(n);
                if (namespace.equals(child.getNamespaceURI())) {
                    nodesForRemoval.add(child);
                }
                else {
                    stripNamespace(child, namespace);
                }
            }
            for (Iterator iterator = nodesForRemoval.iterator(); iterator.hasNext();) {
                Node nodeToRemove = (Node) iterator.next();
                node.removeChild(nodeToRemove);                
            }
        }
    }

    /**
     * Hoist all namespace declarations to the
     * @param element
     */
    public static Element normalizeNamespaces(Element element) {
        // First clone the original to work on the clone
        element = (Element) element.cloneNode(true);

        // (need a set to track unique)
        // First, build map of all namespace URI -> unique prefix

        Map lastPrefixToUri = new HashMap();
        Map lastUriToPrefix = new HashMap();
        Map uniquePrefixToUri = new HashMap();
        Map uniqueUriToPrefix = new HashMap();
        Map prefixOldToNew = new HashMap();
        normalizeNamespacesRecursively(element,
                lastUriToPrefix,
                lastPrefixToUri,
                uniqueUriToPrefix,
                uniquePrefixToUri,
                prefixOldToNew);

        // Element tree has been translated -- now just add namespace decls back onto root element.
        for (Iterator i = uniqueUriToPrefix.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry) i.next();
            String uri = (String) entry.getKey();
            String prefix = (String) entry.getValue();
            if (uri == null || prefix == null) throw new IllegalStateException();
            element.setAttributeNS(XMLNS_NS, "xmlns:" + prefix, uri);
        }

        // We are done, we think
        return element;
    }

    private static final Pattern MATCH_QNAME = Pattern.compile("^\\s*([^:\\s]+):(\\S+?)\\s*$");

    /**
     * Add the specified element's namespace declarations to the specified map(prefix -> namespace).
     * The default namespace is represented with the prefix "" (empty string).
     */
    private static void addToNamespaceMap(Element element, Map nsmap) {
        NamedNodeMap attrs = element.getAttributes();
        int numAttr = attrs.getLength();
        for (int i = 0; i < numAttr; ++i) {
            Attr attr = (Attr)attrs.item(i);
            if ("xmlns".equals(attr.getName()))
                nsmap.put("", attr.getValue()); // new default namespace
            else if ("xmlns".equals(attr.getPrefix())) // new namespace decl for prefix
                nsmap.put(attr.getLocalName(), attr.getValue());
        }
    }
    /**
     * Accumlate a map of all namespace URIs used by this element and all its children.
     *
     * @param element the element to collect
     * @param uriToPrefix  a Map(namespace uri => last seen prefix).
     */
    private static void normalizeNamespacesRecursively(Element element,
                                                       Map uriToPrefix,
                                                       Map prefixToUri,
                                                       Map uniqueUriToPrefix,
                                                       Map uniquePrefixToUri,
                                                       Map prefixOldToNew)
    {
        uriToPrefix = new HashMap(uriToPrefix);
        prefixToUri = new HashMap(prefixToUri);
        prefixOldToNew = new HashMap(prefixOldToNew);

        // Update uriToPrefix and prefixToUri maps for the scope of the current element
        NamedNodeMap attrs = element.getAttributes();
        for (int j = 0; j < attrs.getLength(); j++) {
            Attr attrNode = (Attr) attrs.item(j);
            String attPrefix = attrNode.getPrefix();
            String attNsUri = attrNode.getNamespaceURI();
            String nsPrefix = attrNode.getLocalName();
            String nsUri = attrNode.getValue();

            if (nsUri != null && nsUri.trim().length() > 0) {
                nsUri = nsUri.trim();
                if ("xmlns".equals(attPrefix) && DomUtils.XMLNS_NS.equals(attNsUri)) {
                    String uniquePrefix = nsPrefix;
                    String existingUri = (String) uniquePrefixToUri.get(nsPrefix);
                    if (existingUri != null && !(existingUri.equals(nsUri))) {
                        // Redefinition of namespace.  First see, if we already have some other prefix pointing at it
                        uniquePrefix = (String) uniqueUriToPrefix.get(nsUri);
                        if (uniquePrefix == null) {
                            int n = 0;
                            do {
                                uniquePrefix = nsPrefix + ++n;
                            }
                            while (uniquePrefixToUri.containsKey(uniquePrefix));
                            if (logger.isLoggable(Level.FINEST))
                                logger.log(Level.FINEST, "Redefinition of namespace: {0}={1};  changing prefix to unique value: {2}",
                                        new Object[] { nsPrefix, nsUri, uniquePrefix });
                        } else {
                            if (logger.isLoggable(Level.FINEST))
                                logger.log(Level.FINEST, "Attempted reuse of namespace with colliding prefix: {0}; changing to existing value: {1}", new Object[] { nsPrefix, uniquePrefix });
                        }
                        prefixOldToNew.put(nsPrefix, uniquePrefix);
                    }

                    uniquePrefixToUri.put(uniquePrefix, nsUri);
                    uniqueUriToPrefix.put(nsUri, uniquePrefix);
                    uriToPrefix.put(nsUri, nsPrefix);
                    prefixToUri.put(nsPrefix, nsUri);
                }
            }
        }

        // Translate this element's own prefix, if required
        String oldPrefix = element.getPrefix();
        if (oldPrefix != null) {
            String newPrefix = (String) prefixOldToNew.get(oldPrefix);
            if (newPrefix != null) {
                if (logger.isLoggable(Level.FINEST))
                    logger.finest("Changing element prefix from " + oldPrefix + "to " + newPrefix);
                element.setPrefix(newPrefix);
            }
        }

        // Go through all child nodes, performing the following operations:
        // - removing soon-to-be-unneeded namespace decls
        // - modifying text nodes and attribute values that look like qnames that need to have prefixes translated
        // - translating attribute prefixes that need to be translated
        // - and recursing to child elements.

        // Collect list first, so we don't have a NodeList open as we modify it
        Set kids = new LinkedHashSet();
        NamedNodeMap attrList = element.getAttributes();
        for (int i = 0; i < attrList.getLength(); ++i)
            kids.add(attrList.item(i));
        NodeList kidNodeList = element.getChildNodes();
        for (int i = 0; i < kidNodeList.getLength(); ++i)
            kids.add(kidNodeList.item(i));

        KIDLOOP: for (Iterator i = kids.iterator(); i.hasNext();) {
            Node n = (Node) i.next();
            switch (n.getNodeType()) {
                case Node.ELEMENT_NODE:
                    normalizeNamespacesRecursively((Element)n, uriToPrefix, prefixToUri, uniqueUriToPrefix, uniquePrefixToUri, prefixOldToNew);
                    continue KIDLOOP;
                case Node.ATTRIBUTE_NODE:
                    // Check if it's an obsolete namespace decl
                    String attPrefix = n.getPrefix();
                    String attNsUri = n.getNamespaceURI();
                    String attValue = n.getNodeValue();
                    if (attValue != null && attValue.trim().length() > 0) {
                        if ("xmlns".equals(attPrefix) && DomUtils.XMLNS_NS.equals(attNsUri)) {
                            // Delete this namespace decl
                            if (logger.isLoggable(Level.FINEST))
                                logger.finest("Removing namespace decl (will move to top): " + attValue);
                            element.removeAttributeNode((Attr) n);
                            continue KIDLOOP; // no need to check it for qname -- we're deleting it
                        } else if ("xmlns".equals(n.getNodeName())) {
                            // Don't remove default namespace decl, and don't try to qname-mangle it, either
                            continue KIDLOOP;
                        }
                    }

                    // Translate prefix if necessary
                    String newPrefix = (String) prefixOldToNew.get(attPrefix);
                    if (newPrefix != null) {
                        if (logger.isLoggable(Level.FINEST))
                            logger.finest("Changing attr prefix from " + oldPrefix + " to " + newPrefix);
                        n.setPrefix(newPrefix);
                    }

                    /* FALLTHROUGH and check the attribute value to see if it looks like a qname */
                case Node.TEXT_NODE:
                case Node.CDATA_SECTION_NODE:
                    // Check if this node's text content looks like a qname currently in scope
                    String value = n.getNodeValue();
                    if (value == null || value.trim().length() < 1) break; // ignore empty text nodes
                    Matcher textMatcher = MATCH_QNAME.matcher(value);
                    if (textMatcher.matches()) {
                        String pfx = textMatcher.group(1);
                        String postfix = textMatcher.group(2);
                        if (prefixToUri.get(pfx) != null) {
                            newPrefix = (String) prefixOldToNew.get(pfx);
                            if (newPrefix != null) {
                                // Looks like a qname that needs translating.  Translate it.
                                String newText = MATCH_QNAME.matcher(value).replaceFirst(newPrefix + ":" + postfix);
                                n.setNodeValue(newText);
                            }
                        }
                    }
            }
        }
    }

}
