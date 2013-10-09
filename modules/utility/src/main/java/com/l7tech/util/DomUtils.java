package com.l7tech.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DomUtils collects helper methods for DOM manipulation.
 */
public class DomUtils {
    private static final Logger logger = Logger.getLogger(DomUtils.class.getName());

    /** This is the namespace that the special namespace prefix "xmlns" logically belongs to. */
    public static final String XMLNS_NS = "http://www.w3.org/2000/xmlns/";

    /** This is the namespace that the special namespace prefix "xml" logically belongs to. */
    public static final String XML_NS = "http://www.w3.org/XML/1998/namespace";

    /** Regex character class ranges matching XML NameStartChar, but omitting colon. */
    public static final String PAT_RANGES_NCNameStartChar = "A-Za-z_\\xC0-\\xD6\\xD8-\\xF6\\xF8-\\u02FF\\u0370-\\u037D\\u037F-\\u1FFF\\u200C-\\u200D" +
            "\\u2070-\\u218F\\u2C00-\\u2FEF\\u3001-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFFD";  // TODO need to allow \\u10000-\\uEFFFF also, but \\u notation doesn't support code points outside the BMP

    /** Regex character class ranges matching XML NameChar, but omitting colon. */
    public static final String PAT_RANGES_NCNameChar = PAT_RANGES_NCNameStartChar + "\\-\\.0-9\\xB7\\u0300-\\u036F\\u203F-\\u2040";

    private static final String PAT_STR_NCName = "[" + PAT_RANGES_NCNameStartChar + "][" + PAT_RANGES_NCNameChar + "]*";

    /** Precompiled regex Pattern that will only match a string containing nothing but a valid XML Name (including those with one or more colons, including those with leading or trailing colons). */
    private static final Pattern PATTERN_NAME = Pattern.compile("^" + "[\\:" + PAT_RANGES_NCNameStartChar + "][\\:" + PAT_RANGES_NCNameChar + "]*" + "$");

    /** Precompiled regex Pattern that will only match a string containing nothing but a valid XML NCName (a Name that does not include any colon characters). */
    private static final Pattern PATTERN_NCNAME = Pattern.compile("^" + PAT_STR_NCName + "$");

    /** Precompiled regex Pattern that will only match a string containing nothing but a valid XML QName (optional NCName-plus-colon, followed by an NCName). */
    private static final Pattern PATTERN_QNAME = Pattern.compile("^(?:" + PAT_STR_NCName + "\\:)?" + PAT_STR_NCName + "$");

    private static final boolean ALLOW_DUPLICATE_ID_ATTRS_ON_ELEMENT = ConfigFactory.getBooleanProperty( "com.l7tech.util.allowDuplicateIdAttrsOnElem", true );

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
     *  Finds the first descendant {@link Element} of an ancestor {@link Element}.
     * @param ancestor the {@link Element} in which to search for descendant. Must be non-null.
     * @param nsuri the URI of the namespace to which the descendant must belong, NOT THE PREFIX!  May be null, in which
     *              case namespaces are not considered when checking for a match.
     * @param elementName the name of the element to find. Should not be null.
     * @return  First matching descendant {@link Element} or null if the specified ancestor contains no matching elements
     */
    @Nullable
    public static Element findFirstDescendantElement(Element ancestor, String nsuri, String elementName) {
        if (!(ancestor.hasChildNodes()) || elementName == null)
            return null;

        //Root has children, so continue searching for them
        Node matchingNode = null;

        NodeList childNodes = ancestor.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node n = childNodes.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                matchingNode = (elementName.equals(n.getLocalName()) && (nsuri == null || nsuri.equals(n.getNamespaceURI())))?
                    n : findFirstDescendantElement((Element)n, nsuri, elementName);
                if (matchingNode != null) break;
            }
        }
        return (Element) matchingNode;
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
                for (String nsuri : nsuris) {
                    if (nsuri.equals(n.getNamespaceURI()))
                        return (Element) n;
                }
            }
        }
        return null;
    }

    /**
     * Finds one and only one child {@link Element} of a parent {@link Element}
     *
     * The parent must belong to a DOM produced by a namespace-aware parser.
     *
     * @param parent the {@link Element} in which to search for children. Must be non-null.
     * @return The only matching child {@link Element}
     * @throws TooManyChildElementsException if multiple child elements are found
     * @throws MissingRequiredElementException if no matching child elements are found
     */
    public static Element findExactlyOneChildElement( final Element parent ) throws MissingRequiredElementException, TooManyChildElementsException {
        final Element element = findOnlyOneChildElement( parent );
        if ( element == null ) {
            throw new MissingRequiredElementException(MessageFormat.format("Required child of element {0} not found",  parent.getLocalName()));
        }
        return element;
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
     * @param localName the local name of the element to find. Must be non-null.
     * @return The only matching child {@link Element}.  Never null.
     * @throws TooManyChildElementsException if multiple matching child nodes are found
     * @throws MissingRequiredElementException if no matching child node is found
     */
    public static Element findExactlyOneChildElementByName( Element parent, String nsuri, String localName) throws TooManyChildElementsException, MissingRequiredElementException {
        return findOnlyOneChildElementByName0(parent, nsuri, localName, true);
    }

    /**
     * Finds one and only one child {@link Element} of a parent {@link Element}
     * with the specified name that is in the default namespace, and throws
     * {@link com.l7tech.util.MissingRequiredElementException} if such an element cannot be found.
     *
     * @param parent the {@link Element} in which to search for children. Must be non-null.
     * @param name the name of the element to find. Must be non-null.
     * @return First matching child {@link Element}
     * @throws TooManyChildElementsException if multiple matching child nodes are found
     * @throws MissingRequiredElementException if no matching child node is found
     */
    public static Element findExactlyOneChildElementByName( final Element parent, final String name ) throws TooManyChildElementsException, MissingRequiredElementException {
        if ( name == null ) throw new IllegalArgumentException( "name must be non-null!" );
        NodeList children = parent.getChildNodes();
        Element result = null;
        for ( int i = 0; i < children.getLength(); i++ ) {
            Node n = children.item(i);
            if ( n.getNodeType() == Node.ELEMENT_NODE &&
                 name.equals( n.getNodeName()) &&
                 n.getNamespaceURI() == null) {
                if ( result != null ) throw new TooManyChildElementsException( "#default", name );
                result = (Element)n;
            }
        }
        if (result == null) throw new MissingRequiredElementException(MessageFormat.format("Required element {0} not found",  name));
        return result;
    }

    /**
     * Finds all child {@link Element} of a parent {@link Element}
     * with the specified name that is in the default namespace. This will never return null but it can return an empty
     * list. If there are no child elements with the matching name.
     *
     * @param parent the {@link Element} in which to search for children. Must be non-null.
     * @param name the name of the element to find. Must be non-null.
     * @return The list of child elements found with the matching name. Will never be null.
     */
    public static List<Element> findAllChildElementsByName( final Element parent, final String name ) {
        if ( name == null ) throw new IllegalArgumentException( "name must be non-null!" );
        NodeList children = parent.getChildNodes();
        List<Element> results = new ArrayList<>();
        for ( int i = 0; i < children.getLength(); i++ ) {
            Node n = children.item(i);
            if ( n.getNodeType() == Node.ELEMENT_NODE &&
                    name.equals( n.getNodeName()) &&
                    n.getNamespaceURI() == null) {
                results.add((Element)n);
            }
        }
        return results;
    }

    /**
     * Finds zero or one child {@link Element}s of a parent {@link Element}
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
        if (mustBePresent && result == null) throw new MissingRequiredElementException(MessageFormat.format("Required element '{'{0}'}'{1} not found", nsuri, name));
        return result;
    }

    /**
     * same as findOnlyOneChildElementByName but allows for different namespaces
     */
    public static Element findOnlyOneChildElementByName(Element parent, String[] namespaces, String name) throws TooManyChildElementsException {
        for (String namespace : namespaces) {
            Element res = findOnlyOneChildElementByName(parent, namespace, name);
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
        List<Element> found = new ArrayList<Element>();

        NodeList children = parent.getChildNodes();
        for ( int i = 0; i < children.getLength(); i++ ) {
            Node n = children.item(i);
            if ((n.getNodeType() == Node.ELEMENT_NODE) &&
                name.equals(n.getLocalName()) &&
                (((nsuri == null) && (n.getNamespaceURI() == null)) || ((nsuri != null) && nsuri.equals(n.getNamespaceURI()))))
                found.add( (Element) n );
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
                for ( String namespace : namespaces ) {
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
    public static List<ProcessingInstruction> findProcessingInstructions(Document document) {
        List<ProcessingInstruction> piList = Collections.emptyList();

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
                List<ProcessingInstruction> piNodes = new ArrayList<ProcessingInstruction>(piCount);
                for(int n=0; n<nodes.getLength(); n++) {
                    Node node = nodes.item(n);
                    if(node.getNodeType()==Node.PROCESSING_INSTRUCTION_NODE) {
                        piNodes.add((ProcessingInstruction)node);
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
        List<Element> found = findChildElementsByName( parent, nsuri, name );
        for (Element element : found) {
            parent.removeChild(element);
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
     *
     * @param node node whose child text to collect.  Required.
     * @return a String consisting of all text and cdata nodes glued together and then trimmed.  May be empty but never null.
     */
    public static String getTextValue(Element node) {
        return getTextValue(node, true);
    }

    /**
     * Gets the child text node value for an element.
     *
     * @param node node whose child text to collect.  Required.
     * @param trimResult true if the collected value should have leading and trailing whitespace trimmed before it is returned.
     * @return a String consisting of all text and cdata nodes glued together and (optionally) trimmed.  May be empty but never null.
     */
    public static String getTextValue(Element node, boolean trimResult) {
        StringBuilder output = new StringBuilder();
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node kid = children.item(i);
            if (kid.getNodeType() == Node.TEXT_NODE || kid.getNodeType() == Node.CDATA_SECTION_NODE) {
                String thisTxt = kid.getNodeValue();
                if (thisTxt != null)
                    output.append(thisTxt);
            }
        }
        return trimResult ? output.toString().trim() : output.toString();
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
     * @return the prefix for this namespace in scope at the specified element, or null if it is not declared with a prefix.
     *         Note that the default namespace is not considered to have been declared with a prefix.
     * @see Node#lookupPrefix(String) which is probably a better alternative for most uses
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
        Set<String> usedPrefixes = new HashSet<String>();
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
            for (String namespaceUri : namespaceUris) {
                if (elementDefaultNamespaceURI.equals(namespaceUri)) {
                    foundNamespaceURI = namespaceUri;
                    break;
                }
            }
        }

        // check ns declarations
        if(foundNamespaceURI==null) {
            for (String namespaceUri : namespaceUris) {
                if (findActivePrefixForNamespace(element, namespaceUri) != null) {
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
     * Creates an element for use with the given Parent.  The element will be in the requested namespace.
     * If the namespace is already declared in parent or a direct ancestor then that prefix will be reused;
     * otherwise a new prefix will be declared in the new element that is as close as possible to desiredPrefix.
     * @param parent The {@link Element} or {@link DocumentFragment} to which the new element is added
     * @param localName The local name for the element
     * @param namespace The namespace to use
     * @param desiredPrefix The desired prefix
     */
    public static Element createElementNS(Node parent, String localName, String namespace, String desiredPrefix) {
        final Element element = parent.getOwnerDocument().createElementNS(namespace, localName);
        element.setPrefix(getOrCreatePrefixForNamespace(element, namespace, desiredPrefix));
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
        visitNodes( node, visitor, false );
    }

    /**
     * Invokes the specified visitor on all descendants of the specified node.
     *
     * <p>NOTE: This optionally visits siblings also.</p>
     *
     * @param node the element whose child elements to visit.  Required.
     * @param visitor a visitor to invoke on each immediate child element.  Required.
     * @param visitSiblings true to visit siblings of the given node
     */
    public static void visitNodes( Node node, Functions.UnaryVoid<Node> visitor, boolean visitSiblings ) {
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
            visitNodes( node.getFirstChild(), visitor, true );

            // visit siblings
            if ( visitSiblings ) {
                Node sibling = node.getNextSibling();
                while ( sibling != null ) {
                    visitNodes(sibling , visitor, false );
                    sibling = sibling.getNextSibling();
                }
            }
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
     * Get the map of all namespace declarations in scope for the current element.
     *
     * If there is a default namespace in scope, it will have the empty string
     * "" as its key.
     *
     * @param element the element whose in-scope namespace declrations will be extracted.  Must not be null.
     * @return The map of namespace declarations in scope for this elements immediate children.
     */
    public static Map<String, String> getNamespaceMap(Element element) {
        Map<String, String> nsmap = new HashMap<String, String>();

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

    /**
     * Check if the target node is after the reference node in document order.
     * 
     * @param reference The reference node.
     * @param target The target node.
     * @return True if the reference node is after the target node
     */
    public static boolean isAfter( final Node reference, final Node target ) {
        boolean isAfter = false;

        if ( reference != null && target != null ) {
            isAfter = reference.compareDocumentPosition(target)==Node.DOCUMENT_POSITION_FOLLOWING;
        }

        return isAfter;
    }

    public static Map<String,String> findAllNamespaces(Element element) {
        Map<String,String> entries = new HashMap<String,String>();
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

        final Map<String,String> result = new HashMap<String,String>();
        outer:
        for ( final String uri : entries.keySet() ) {
            int ns = 1;
            String prefix = entries.get(uri);
            if ( prefix == null ) prefix = "ns";
            String checkedPrefix = prefix;
            while ( result.containsKey( checkedPrefix ) ) {
                if ( ns > 1000 ) continue outer;
                checkedPrefix = prefix + ns++;
            }
            result.put(checkedPrefix, uri);
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
        for (String possibleNamespace : possibleNamespaces) {
            if (ns == null) {
                if (possibleNamespace == null) {
                    hasNamespace = true;
                    break;
                }
            } else if (ns.equals(possibleNamespace)) {
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
                List<Attr> attrsForRemoval = new ArrayList<Attr>();
                for (int n=0; n<nodeAttrs.getLength(); n++) {
                    Attr attribute = (Attr) nodeAttrs.item(n);
                    if (namespace.equals(attribute.getNamespaceURI())) {
                        attrsForRemoval.add(attribute);
                    }
                }
                for (Attr attribute : attrsForRemoval) {
                    ((Element) node).removeAttributeNode(attribute);
                }
            }

            // children
            NodeList nodes = node.getChildNodes();
            List<Node> nodesForRemoval = new ArrayList<Node>();
            for (int n=0; n<nodes.getLength(); n++) {
                Node child = nodes.item(n);
                if (namespace.equals(child.getNamespaceURI())) {
                    nodesForRemoval.add(child);
                }
                else {
                    stripNamespace(child, namespace);
                }
            }
            for (Node nodeToRemove : nodesForRemoval) {
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

        Map<String,String> lastPrefixToUri = new HashMap<String,String>();
        Map<String,String> lastUriToPrefix = new HashMap<String,String>();
        Map<String,String> uniquePrefixToUri = new HashMap<String,String>();
        Map<String,String> uniqueUriToPrefix = new HashMap<String,String>();
        Map<String,String> prefixOldToNew = new HashMap<String,String>();
        normalizeNamespacesRecursively(element,
                lastUriToPrefix,
                lastPrefixToUri,
                uniqueUriToPrefix,
                uniquePrefixToUri,
                prefixOldToNew);

        // Element tree has been translated -- now just add namespace decls back onto root element.
        for (Object o : uniqueUriToPrefix.entrySet()) {
            Map.Entry entry = (Map.Entry) o;
            String uri = (String) entry.getKey();
            String prefix = (String) entry.getValue();
            if (uri == null || prefix == null) throw new IllegalStateException();
            element.setAttributeNS(XMLNS_NS, "xmlns:" + prefix, uri);
        }

        // We are done, we think
        return element;
    }

    /**
     * Check if the specified identifier string is a valid "Name" per the XML 1.0 specification.
     * <p/>
     * A Name consists of a more-or-less alphabetic start character followed by zero or more more-or-less alphanumeric characters
     * (but with certain punctuation allowed, notably including colon). The specfication defines this in terms of ranges of Unicode code points.
     * <p/>
     * Note: this method currently will not accept names containing characters in the range 10000-EFFFF (which is
     * outside the basic multilingual plane), even though these are technically supposed to be allowed per the XML 1.0 spec.
     *
     * @param identifier the identifier to check.  Required.
     * @return true iff. the specified identifier is a valid XML Name.
     */
    public static boolean isValidXmlName(CharSequence identifier) {
        return PATTERN_NAME.matcher(identifier).matches();
    }

    /**
     * Check if the specified identifier string is a valid "NCName" (No-Colon Name) per the "Namespaces in XML 1.0" specification.
     * <p/>
     * An "NCName" permits all the same characters as a "Name" with the exception of the colon character, which is disallowed.
     * <p/>
     * Note: this method currently will not accept names containing characters in the range 10000-EFFFF (which is
     * outside the basic multilingual plane), even though these are technically supposed to be allowed per the XML 1.0 spec.
     *
     * @param identifier the identifier to check.  Required.
     * @return true iff. the specified identifier is a valid XML NCName.
     */
    public static boolean isValidXmlNcName(CharSequence identifier) {
        return PATTERN_NCNAME.matcher(identifier).matches();
    }

    /**
     * Check if the specified identifier string is a valid "QName" (Qualified Name) per the "Namespaces in XML 1.0" specification.
     * <p/>
     * A "QName" is either a bare "NCName", or else an NCName preceded by an NCName prefix and a colon (that is, either "NCName" or "NCName:NCName").
     * <p/>
     * Note: this method currently will not accept names containing characters in the range 10000-EFFFF (which is
     * outside the basic multilingual plane), even though these are technically supposed to be allowed per the XML 1.0 spec.
     *
     * @param identifier the identifier to check.  Required.
     * @return true iff. the specified identifier is a valid XML QName.
     */
    public static boolean isValidXmlQname(CharSequence identifier) {
        return PATTERN_QNAME.matcher(identifier).matches();
    }

    /**
     * Check if the specified text is valid for use in an XML 1.0 document.
     *
     * <p>Such text can be used for attribute values or, CharacterData nodes.</p>
     *
     * <p>NOTE: The specification does not say any characters are invalid, it
     * instead defines which characters MUST be accepted as valid. It is
     * therefore possible that this method incorrectly detects valid text as
     * invalid.</p>
     *
     * <p>WARNING: This method will not currently detect invalid characters in
     * the range #x10000-#x10FFFF</p>
     *
     * @param text The text to check. Required.
     * @return true if the text is valid.
     */
    public static boolean isValidXmlContent(final CharSequence text) {
        boolean valid = true;

        for ( int i=0; i<text.length(); i++ ) {
            char character = text.charAt( i );

            if ( character != 0x9 &&
                 character != 0xA &&
                 character != 0xD &&
                 !(character >= 0x20 && character <= 0xD7FF) &&
                 !(character >= 0xE000 && character <= 0xFFFD) ) {
                valid = false;
                break;
            }
        }

        return valid;
    }

    // Very loose match of anything that looks like a prefixed qname, regardless of whether it also includes punctuation
    private static final Pattern MATCH_QNAME = Pattern.compile("^\\s*([^:\\s]+):(\\S+?)\\s*$");

    /**
     * Add the specified element's namespace declarations to the specified map(prefix -> namespace)
     * if they do not already exist.
     * 
     * The default namespace is represented with the prefix "" (empty string).
     */
    private static void addToNamespaceMap(Element element, Map<String,String> nsmap) {
        NamedNodeMap attrs = element.getAttributes();
        int numAttr = attrs.getLength();
        for (int i = 0; i < numAttr; ++i) {
            Attr attr = (Attr)attrs.item(i);
            if ("xmlns".equals(attr.getName())) {
                if ( !nsmap.containsKey(""))
                    nsmap.put("", attr.getValue()); // new default namespace
            } else if ("xmlns".equals(attr.getPrefix())) { // new namespace decl for prefix
                if ( !nsmap.containsKey(attr.getLocalName()))
                    nsmap.put(attr.getLocalName(), attr.getValue());
            }
        }
    }
    /**
     * Accumlate a map of all namespace URIs used by this element and all its children.
     *
     * @param element the element to collect
     * @param uriToPrefix  a Map(namespace uri => last seen prefix).
     */
    private static void normalizeNamespacesRecursively(Element element,
                                                       Map<String,String> uriToPrefix,
                                                       Map<String,String> prefixToUri,
                                                       Map<String,String> uniqueUriToPrefix,
                                                       Map<String,String> uniquePrefixToUri,
                                                       Map<String,String> prefixOldToNew)
    {
        uriToPrefix = new HashMap<String,String>(uriToPrefix);
        prefixToUri = new HashMap<String,String>(prefixToUri);
        prefixOldToNew = new HashMap<String,String>(prefixOldToNew);

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
                    String existingUri = uniquePrefixToUri.get(nsPrefix);
                    if (existingUri != null && !(existingUri.equals(nsUri))) {
                        // Redefinition of namespace.  First see, if we already have some other prefix pointing at it
                        uniquePrefix = uniqueUriToPrefix.get(nsUri);
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
            String newPrefix = prefixOldToNew.get(oldPrefix);
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
        Set<Node> kids = new LinkedHashSet<Node>();
        NamedNodeMap attrList = element.getAttributes();
        for (int i = 0; i < attrList.getLength(); ++i)
            kids.add(attrList.item(i));
        NodeList kidNodeList = element.getChildNodes();
        for (int i = 0; i < kidNodeList.getLength(); ++i)
            kids.add(kidNodeList.item(i));

        KIDLOOP: for (Node n : kids) {
            switch (n.getNodeType()) {
                case Node.ELEMENT_NODE:
                    normalizeNamespacesRecursively((Element) n, uriToPrefix, prefixToUri, uniqueUriToPrefix, uniquePrefixToUri, prefixOldToNew);
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
                    String newPrefix = prefixOldToNew.get(attPrefix);
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
                            newPrefix = prefixOldToNew.get(pfx);
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

    /**
     * Construct a Map from ID attribute value to target Element for the specified Document, recognizing only the
     * specified attribute names as ID attributes, and failing if any duplicate attribute values are detected.
     * <p/>
     * The returned Map is mutable, so additional mappings can be inserted if required (such as a mapping from the
     * empty string to the document element, for recognizing URI="" dsig references).
     *
     * @param doc the Document to examine.  Required.
     * @param idAttributeConfig a configuration of which ID attributes to recognize.  Required.
     *                          See {@link IdAttributeConfig#makeIdAttributeConfig(java.util.Collection)} for a way to make one of these.
     *                          Also see {@link SoapConstants} for some pregenerated configurations.
     * @return a Map from ID attribute values to the Element instance that owned the corresponding attribute.  Never null.
     * @throws InvalidDocumentFormatException  if more than one ID attribute contained the same value.
     */
    public static Map<String, Element> getElementByIdMap(Document doc, IdAttributeConfig idAttributeConfig) throws InvalidDocumentFormatException {
        Map<String, Element> map = new HashMap<String, Element>();
        NodeList elements = doc.getElementsByTagName("*");
        for (int i = 0; i < elements.getLength(); i++) {
            Element element = (Element)elements.item(i);

            String id = getElementIdValue(element, idAttributeConfig);
            if (id != null) {
                Element existing = map.put(id, element);
                if (existing != null)
                    throw new InvalidDocumentFormatException("Duplicate ID attribute value found in document: " + id);
            }
        }
        return map;
    }

    /**
     * Find an ID value for the specified element, if any, according to the specified IdAttributeConfig.
     *
     * @param element  the element to examine.  Required.
     * @param idAttributeConfig  Configuration of which elements are to be recognized as ID attributes.  Required.
     *                           See {@link IdAttributeConfig#makeIdAttributeConfig(java.util.Collection)} for a way to make one of these.
     *                          Also see {@link SoapConstants} for some pregenerated configurations.
     * @return the ID attribute value, or null if the element did not appear to have an ID attribute or it did but it was empty.  Never empty.
     * @throws InvalidDocumentFormatException  if the element contained more than one attribute recognized as an ID attribute.
     */
    public static String getElementIdValue(Element element, IdAttributeConfig idAttributeConfig) throws InvalidDocumentFormatException {
        Attr attr = getElementIdAttr(element, idAttributeConfig);
        if (attr == null) {
            return null;
        }
        String id = attr.getValue();
        return id != null && id.length() > 0 ? id : null;
    }

    /**
     * Find an ID attribute to use for the specified element, according to the specified IdAttributeConfig.
     *
     * @param element  the element to examine.  Required.
     * @param idAttributeConfig  Configuration of which elements are to be recognized as ID attributes.  Required.
     *                           See {@link IdAttributeConfig#makeIdAttributeConfig(java.util.Collection)} for a way to make one of these.
     *                          Also see {@link SoapConstants} for some pregenerated configurations.
     * @return the ID attribute node, or null if the element did not appear to have an ID attribute.
     * @throws InvalidDocumentFormatException  if the element contained more than one attribute recognized as an ID attribute.
     */
    public static Attr getElementIdAttr(Element element, IdAttributeConfig idAttributeConfig) throws InvalidDocumentFormatException {
        if (idAttributeConfig == null)
            throw new NullPointerException("idAttributeConfig is required");

        Attr ret = null;

        for (FullQName qn : idAttributeConfig.idAttrsInPreferenceOrder) {
            String wantNs = qn.getNsUri();

            // do "local" hack
            if ("local".equals(qn.getPrefix()) && wantNs != null) {
                // Check against element NS instead
                if (wantNs.equals(element.getNamespaceURI())) {
                    // Element NS is OK; match attr as local attr
                    wantNs = null;
                } else {
                    // Element NS mismatch; this ID attr doesn't apply here
                    continue;
                }
            }

            Attr attr = element.getAttributeNodeNS(wantNs, qn.getLocal());
            if (attr != null && attr.getValue() != null && attr.getValue().length() > 0) {
                if (ret != null) {
                    throw new InvalidDocumentFormatException("Multiple ID attributes found on element: " + element.getNodeName());
                }

                ret = attr;

                if (ALLOW_DUPLICATE_ID_ATTRS_ON_ELEMENT) // stop on highest-priority match
                    return ret;
            }
        }

        return ret;
    }

    /**
     * Search for an element by its ID value.
     * This does a linear search over the entire document.
     *
     * @param doc  the document to search.  Required.
     * @param elementId  the ID value to search for.  Required.
     * @param idAttributeConfig  Configuration of which elements are to be recognized as ID attributes.  Required.
     *                           See {@link IdAttributeConfig#makeIdAttributeConfig(java.util.Collection)} for a way to make one of these.
     *                          Also see {@link SoapConstants} for some pregenerated configurations.
     * @return the first Element in the document that has the requested ID attribute value.
     * @throws InvalidDocumentFormatException  if the element contained more than one attribute recognized as an ID attribute.
     */
    public static Element getElementByIdValue(Document doc, String elementId, IdAttributeConfig idAttributeConfig) throws InvalidDocumentFormatException {
        String url;
        if (elementId.charAt(0) == '#') {
            url = elementId.substring(1);
        } else
            url = elementId;
        NodeList elements = doc.getElementsByTagName("*");
        for (int i = 0; i < elements.getLength(); i++) {
            Element element = (Element)elements.item(i);
            if (url.equals(getElementIdValue(element, idAttributeConfig))) {
                return element;
            }
        }
        return null;
    }

    /**
     * Get the owner document of the specified node, unless it is a Document node, in which case it is assumed
     * to be its own owner.
     *
     * @param node a node to examine.  Required.
     * @return the owner document, or null.
     */
    public static Document getOwnerDocument(@NotNull Node node) {
        if (node instanceof Document) {
            return (Document) node;
        } else {
            return node.getOwnerDocument();
        }
    }
}
