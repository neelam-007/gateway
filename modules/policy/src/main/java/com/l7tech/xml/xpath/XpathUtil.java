package com.l7tech.xml.xpath;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.DomUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.FullQName;
import com.l7tech.xml.DomElementCursor;
import com.l7tech.xml.ElementCursor;
import com.l7tech.xml.InvalidXpathException;
import com.l7tech.xml.SaxonUtils;
import org.jaxen.JaxenException;
import org.jaxen.JaxenHandler;
import org.jaxen.NamespaceContext;
import org.jaxen.SimpleNamespaceContext;
import org.jaxen.dom.DOMXPath;
import org.jaxen.saxpath.SAXPathException;
import org.jaxen.saxpath.XPathHandler;
import org.jaxen.saxpath.base.XPathReader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.soap.*;
import javax.xml.xpath.XPathExpressionException;
import java.text.ParseException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.xml.xpath.XpathVersion.*;

/**
 * Utility methods for dealing with XPaths.
 */
public class XpathUtil {
    public static final Logger logger = Logger.getLogger(XpathUtil.class.getName());

    /**
     * Check whether the specified XPath expression makes any use of its target document.
     * This can be used to distinguish expressions that must be evaluated against a meaningful
     * target document, such as "//foo", from those that do not require a meaningful target document,
     * such as "$var > 17".
     *
     * @param expr the expression to examine.  Required.
     * @param xpathVersion XPath version to assume when parsing expr as an XPath.  Required but may be UNSPECIFIED.
     * @return true if this expression might make use of a target document.  False if this expression is known to not require a target document.
     */
    public static boolean usesTargetDocument(@NotNull String expr, @NotNull XpathVersion xpathVersion) {
        if (XPATH_2_0.equals(xpathVersion)) {
            // TODO see if there is a reliable way of proving that an XPath 2.0 expression doesn't use the target document
            // For now, just assume the safe result, that any XPath 2.0 expression might do so.
            return true;
        }

        final boolean[] nodeUse = { false };
        try {
            XPathHandler handler = new XPathHandlerAdapter() {
                @Override
                public void startAbsoluteLocationPath() throws SAXPathException {
                    nodeUse[0] = true;
                }

                @Override
                public void startRelativeLocationPath() throws SAXPathException {
                    nodeUse[0] = true;
                }

                @Override
                public void startProcessingInstructionNodeStep(int i, String s) throws SAXPathException {
                    nodeUse[0] = true;
                }

                @Override
                public void startTextNodeStep(int i) throws SAXPathException {
                    nodeUse[0] = true;
                }

                @Override
                public void startCommentNodeStep(int i) throws SAXPathException {
                    nodeUse[0] = true;
                }

                @Override
                public void startAllNodeStep(int i) throws SAXPathException {
                    nodeUse[0] = true;
                }

                @Override
                public void startFunction(String prefix, String functionName) throws SAXPathException {
                    if ("id".equalsIgnoreCase(functionName))
                        nodeUse[0] = true;
                }
            };
            XPathReader reader = new XPathReader();
            reader.setXPathHandler(handler);
            reader.parse(expr);
        } catch (SAXPathException e) {
            logger.log(Level.INFO, "Unable to parse XPath expression to determine whether it refers to the target document: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            return true;
        }
        return nodeUse[0];
    }

    /**
     * Determine which namespace prefixes appear to be used by the specified XPath expression.
     *
     * @param expr the expression to examine.  Required.
     * @param xpathVersion XPath version to assume when parsing expr.  Required, but may be UNSPECIFIED.
     * @param lookForQnameLiterals if true, we will include the prefix of anything that looks like it might be a qname literal
     *                             that appears within a string literal.  Currently ignored if xpathVersion is XPATH_2_0
     * @return a set of prefixes which appear to be in use.  May be empty but never null.
     * @throws ParseException if the expression does not parse.
     */
    public static Set<String> getNamespacePrefixesUsedByXpath(@NotNull String expr, @NotNull XpathVersion xpathVersion, final boolean lookForQnameLiterals) throws ParseException {
        if (XPATH_2_0.equals(xpathVersion)) {
            try {
                return SaxonUtils.getNamespacePrefixesUsedByXpath(expr, xpathVersion);
            } catch (InvalidXpathException e) {
                throw (ParseException)new ParseException("Unable to parse XPath expression: " + ExceptionUtils.getMessage(e), 0).initCause(e);
            }
        }

        final Set<String> ret = new HashSet<>();

        XPathHandler handler = new XPathHandlerAdapter() {
            @Override
            public void startFunction(String prefix, String localName) throws SAXPathException {
                ret.add(prefix);
            }

            @Override
            public void startNameStep(int axis, String prefix, String localName) throws SAXPathException {
                ret.add(prefix);
            }

            @Override
            public void variableReference(String prefix, String variableName) throws SAXPathException {
                ret.add(prefix);
            }

            @Override
            public void literal(String s) throws SAXPathException {
                if (lookForQnameLiterals && DomUtils.isValidXmlQname(s)) {
                    try {
                        ret.add(FullQName.valueOf(s).getPrefix());
                    } catch (ParseException e) {
                        // can't happen
                        throw new SAXPathException(e);
                    }
                }
            }
        };
        XPathReader reader = new XPathReader();
        reader.setXPathHandler(handler);
        try {
            reader.parse(expr);
        } catch (SAXPathException e) {
            throw (ParseException)new ParseException("Unable to parse XPath expression", 0).initCause(e);
        }
        ret.remove(null);
        ret.remove("");
        return ret;
    }

    /**
     * Find all unprefixed variables used in the specified XPath expression.
     * This method ignores any variables that are used with a namespace prefix.
     * If the expression cannot be parsed, this method returns an empty list. 
     *
     * @param expr the expression to examine.  Required.
     * @param xpathVersion version of XPath to assume for parsing expr.  Required, but may be UNSPECIFIED.
     * @return a list of all unprefixed XPath variable references found in the expression.  May be empty but never null.
     */
    public static List<String> getUnprefixedVariablesUsedInXpath(@NotNull String expr, @NotNull XpathVersion xpathVersion) {
        final List<String> seenvars = new ArrayList<>();

        try {
            if (XPATH_2_0.equals(xpathVersion)) {
                seenvars.addAll(SaxonUtils.getUnprefixedVariablesUsedInXpath(expr, xpathVersion));
            } else {

                XPathHandler handler = new XPathHandlerAdapter() {
                    @Override
                    public void variableReference(String prefix, String localName) throws SAXPathException {
                        if (prefix == null || prefix.length() < 1)
                            seenvars.add(localName);
                    }
                };
                XPathReader reader = new XPathReader();
                reader.setXPathHandler(handler);
                reader.parse(expr);
            }
        } catch (SAXPathException | InvalidXpathException e) {
            logger.log(Level.FINE, "Unable to parse XPath expression to determine variables used: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            /* FALLTHROUGH and leave seenvars empty */
        }
        return seenvars;
    }

    /**
     * Check if the specified XPath expression uses any variables.
     * This counts variables with prefixes, and so will return true even if {@link #getUnprefixedVariablesUsedInXpath(String, XpathVersion)}
     * would return an empty list for the same expression.
     *
     * @param expr the expression to examine.  Required.
     * @return true if the expression might make use of any XPath variables
     * @throws XPathExpressionException if the expression cannot be parsed
     */
    public static boolean usesXpathVariables(String expr, @NotNull XpathVersion xpathVersion) throws XPathExpressionException {
        if (XPATH_2_0.equals(xpathVersion)) {
            // Assume it might use variables for purposes of this method
            return true;
        }

        final boolean[] sawAny = { false };
        try {
            XPathHandler handler = new XPathHandlerAdapter() {
                @Override
                public void variableReference(String prefix1, String localname) throws SAXPathException {
                    sawAny[0] = true;
                }
            };
            XPathReader reader = new XPathReader();
            reader.setXPathHandler(handler);
            reader.parse(expr);
        } catch (SAXPathException e) {
            throw new XPathExpressionException(e);
        }
        return sawAny[0];
    }

    /**
     * Get the map of namespaces that are declared in the SOAP message.
     * This is a utility method that is typically used to extract the
     * namespaces from the SOAP message before evaluating the XPath
     * expression.
     *
     * @param sm the sopamessage
     * @return the namespace <code>Map</code> as {prefix, URI}
     * @throws javax.xml.soap.SOAPException on SOAP processing error
     */
    public static Map<String,String> getNamespaces(SOAPMessage sm) throws SOAPException {
        Map<String, String> namespaces = new HashMap<>();
        SOAPPart sp = sm.getSOAPPart();
        SOAPEnvelope env = sp.getEnvelope();
        SOAPBody body = env.getBody();
        addNamespaces(namespaces, env);

        //Add namespaces of top body element
        Iterator bodyElements = body.getChildElements();
        while (bodyElements.hasNext()) {
            Object element = bodyElements.next();
            if(element instanceof SOAPElement) {
                addNamespaces(namespaces, (SOAPElement)element);
            }
        }
        return namespaces;
    }

    /**
     * Collects a namespaces from an <code>SOAPElement</code> into the
     * <code>Map</code>
     * <p/>
     * @param namespaces the collecting <code>Map</code>
     * @param element    the <code>SOAPElement</code>
     */
    public static void addNamespaces( final Map<String, String> namespaces,
                                      final SOAPElement element) {
        final String ePrefix = element.getElementName().getPrefix();
        final String eNamespace = element.getElementName().getURI();
        if ( eNamespace != null ) {
            putNamespaceWithPreferredPrefix( namespaces, ePrefix, eNamespace );
        }

        final Iterator iterator = element.getNamespacePrefixes();
        while (iterator.hasNext()) {
            final String prefix = (String)iterator.next();
            final String uri = element.getNamespaceURI(prefix);
            if ( uri != null ) {
                putNamespaceWithPreferredPrefix( namespaces, prefix, uri );
            }
        }
        
        final Iterator itchildren = element.getChildElements();
        while(itchildren.hasNext()) {
            final Object o = itchildren.next();
            if (o instanceof SOAPElement) {
                final SOAPElement childElement = (SOAPElement)o;
                addNamespaces(namespaces, childElement);
            }
        }
    }

    /**
     * Remove requested unused namespaces from the given map.
     * <p/>
     * Note that this ethod only works with XPath 1.0 expressions.
     *
     * @param expression The xpath expression (required)
     * @param namespaces The namespace map (required)
     * @param removePrefixes The set of prefixes to remove if unused (required)
     */
    public static void removeNamespaces( final String expression,
                                         final Map<String, String> namespaces,
                                         final Set<String> removePrefixes ) {
        if ( ConfigFactory.getBooleanProperty( "com.l7tech.xml.xpath.enableNamespaceCleanup", true ) ) {
            try {
                final Set<String> usedPrefixes = new HashSet<>();
                final XPathReader reader = new XPathReader();

                reader.setXPathHandler(new JaxenHandler() {
                    @Override
                    public void variableReference( final String prefix, final String variableName ) throws JaxenException {
                        usedPrefixes.add( prefix );
                        super.variableReference( prefix, variableName );
                    }

                    @Override
                    public void startFunction( final String prefix, final String functionName ) throws JaxenException {
                        usedPrefixes.add( prefix );
                        super.startFunction( prefix, functionName );
                    }

                    @Override
                    public void startNameStep( final int axis, final String prefix, final String localName ) throws JaxenException {
                        usedPrefixes.add( prefix );
                        super.startNameStep( axis, prefix, localName );
                    }
                });

                reader.parse( expression );
                Set<String> toRemove = new HashSet<>( removePrefixes );
                toRemove.removeAll( usedPrefixes );
                if ( !toRemove.isEmpty() ) {
                    namespaces.keySet().removeAll( toRemove );
                }
            } catch ( SAXPathException spe ) {
                // don't remove any namespaces
            }
        }
    }

    /**
     * Quietly evaluate the xpath expression. Only to be used where the xpath expression is hardcoded and is known
     * to be correct. e.g. a runtime exception throw here is a programming error.
     *
     * @param cursor Cursor to evaluate expression against.
     * @param namespaces Map of namespaces used in expression.
     * @param xpath xpath expression to evaluate
     * @return XpathResult result
     */
    @NotNull
    public static XpathResult getXpathResultQuietly(@NotNull final ElementCursor cursor,
                                                    @Nullable final Map<String, String> namespaces,
                                                    @NotNull final String xpath) {
        final XpathResult xpathResult;
        try {
            xpathResult = cursor.getXpathResult(
                    new XpathExpression(
                            xpath,
                            namespaces).compile());
        } catch (XPathExpressionException | InvalidXpathException e) {
            throw new RuntimeException("Unexpected error with xpath expression: " + e.getMessage(), e);
        }

        return xpathResult;
    }

    // - PRIVATE

    /**
     * Create a Jaxen NamespaceContext from the specified namespace Map.
     *
     * @param namespaces a Map of prefix to namespace URI.  Required.
     * @return a Jaxen NamespaceContext that will look up the specified namespaces. Never null.
     */
    private static NamespaceContext makeNamespaceContext(Map<String, String> namespaces) {
        if (namespaces == null) throw new NullPointerException();
        SimpleNamespaceContext sn = new SimpleNamespaceContext();
        for (String prefix : namespaces.keySet())
            sn.addNamespace(prefix, namespaces.get(prefix));
        return sn;
    }

    /**
     * Scan the specified list and, if it is empty or all its members are DOM Element instances, return it as a list of Element.
     *
     * <p>If the list contains any Element[] or Node[] then each element will be placed in the result list.</p>
     *
     * @param list the List to examine.  If null, this method will return an empty list.
     * @return the same List as a List< Element >, as long as it is either empty or contains only Element instances.
     * @throws JaxenException if the list contains anything other than an Element.
     */
    public static List<Element> ensureAllResultsAreElements( final List list ) throws JaxenException {
        if (list == null)
            return Collections.emptyList();

        final List<Element> resultList = new ArrayList<>();
        for (Object obj : list) {
            if ( obj instanceof org.w3c.dom.Node ) {  // fix for Bug #984
                org.w3c.dom.Node node = (org.w3c.dom.Node) obj;
                if (node.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE)
                    throw new JaxenException("Xpath result included a non-Element Node of type " + node.getNodeType());

                resultList.add( (Element) node );
            } else if ( obj instanceof org.w3c.dom.Node[] ) {
                for ( org.w3c.dom.Node node : (org.w3c.dom.Node[]) obj ) {
                    if (node.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE)
                        throw new JaxenException("Xpath result included a non-Element Node of type " + node.getNodeType());

                    resultList.add( (Element) node );
                }
            } else {
                throw new JaxenException("Xpath evaluation produced a non-empty result, but it wasn't of type Node.  Type: " + obj.getClass().getName());
            }
        }
        return resultList;
    }

    /**
     * Create an XPath expression for the given literal text.
     *
     * <p>This is for XPaths outside of XML documents where it is not possible
     * to escape the " and ' characters. This means that text containing both
     * characters must be represented using concatenated literals.</p>
     *
     * @param text The text to process
     * @return The expression.
     */
    public static String literalExpression( final String text ) {
        final StringBuilder expressionBuffer = new StringBuilder();

        if ( text.indexOf( '\'' ) < 0 ) {
            // use single quoted string literal
            expressionBuffer.append( '\'' );
            expressionBuffer.append( text );
            expressionBuffer.append( '\'' );
        } else if ( text.indexOf( '"' ) < 0 ) {
            // use double quoted string literal
            expressionBuffer.append( '"' );
            expressionBuffer.append( text );
            expressionBuffer.append( '"' );
        } else {
            // concatenate sub-strings with appropriate quoting
            expressionBuffer.append( "concat(" );
            int lastIndex = 0;
            int index = 0;
            while ( index < text.length() && (index = text.indexOf( '\'', index )) > -1 ) {
                if ( lastIndex != 0 ) {
                    expressionBuffer.append( ", " );
                }
                if ( index == lastIndex ) {
                    expressionBuffer.append( "\"'\"" );
                } else {
                    expressionBuffer.append( '\'' );
                    expressionBuffer.append( text.substring( lastIndex, index ));
                    expressionBuffer.append( "', \"'\"" );
                }

                lastIndex = index+1;
                index++;
            }

            if ( lastIndex != text.length() ) {
                expressionBuffer.append( ", '" );
                expressionBuffer.append( text.substring( lastIndex, text.length() ));
                expressionBuffer.append( '\'' );
            }

            expressionBuffer.append( ')' );
        }

        return expressionBuffer.toString();
    }

    /**
     * Immediately compile and evaluate the specified XPath against the root of the specified document, using
     * the specified namespace map and variable finder, and returning the evaluation result.
     *
     * @param targetDocument the document against which to evaluate the XPath, or null to use a placeholder/dummy document.
     * @param expression     the expression to compile and evaluate.  Required.
     * @param xpathVersion   the XPath version to assume for compiling the expression.  Required, but may be UNSPECIFIED.
     * @param namespaceMap   the namespace map to use.  May be null if the expression uses no namespace prefixes.
     * @param variableFinder the variable finder to use to look up variable values.  May be null if the expression uses no XPath variables.
     * @return the (cursor-enabled) result of evaluating the XPath, if evaluation was successful.
     * @throws InvalidXpathException if the expression cannot be compiled.
     */
    public static XpathResult testXpathExpression(@Nullable Document targetDocument, @NotNull String expression, @NotNull XpathVersion xpathVersion,
                                           @Nullable Map<String, String> namespaceMap, @Nullable XpathVariableFinder variableFinder)
        throws InvalidXpathException
    {
        CompiledXpath cx = new DomCompiledXpath(new XpathExpression(expression, xpathVersion, namespaceMap));

        ElementCursor cursor;
        if (targetDocument != null)
            cursor = new DomElementCursor(targetDocument, false);
        else
            cursor = new DomElementCursor(XmlUtil.createEmptyDocument("bogus", null, null));
        try {
            return cursor.getXpathResult(cx, variableFinder, true);
        } catch (XPathExpressionException e) {
            throw new InvalidXpathException(e);
        }
    }

    /**
     * Validate the given XPath expression.
     *
     * <p>This may detect XPath errors not found by running compileAndEvaluate with a
     * test document (such as unbound namespace prefixes)</p>
     *
     * @param expression     the expression to test.  Required.
     * @param xpathVersion   XPath version to assume when compiling the expression.  Required, but may be UNSPECIFIED.
     * @param namespaceMap   the namespace map to use.  May be null if the expression uses no namespace prefixes.
     * @throws InvalidXpathException if the expression is invalid.
     */
    public static void validate( final String expression,
                                 final XpathVersion xpathVersion,
                                 @Nullable final Map<String, String> namespaceMap ) throws InvalidXpathException {
        if (XPATH_2_0.equals(xpathVersion)) {
            SaxonUtils.validateSyntaxAndNamespacePrefixes(expression, xpathVersion, namespaceMap);
            return;
        }

        final Set<String> invalidPrefixes = new LinkedHashSet<>();

        final XPathHandler handler = new XPathHandlerAdapter() {
            @Override
            public void startNameStep( final int axis,
                                       final String prefix,
                                       final String localName ) throws SAXPathException {
                if ( prefix != null && !prefix.isEmpty() && (namespaceMap==null || !namespaceMap.containsKey( prefix ))) {
                    invalidPrefixes.add( prefix );
                }
            }
        };

        XPathReader reader = new XPathReader();
        reader.setXPathHandler(handler);
        try {
            reader.parse( expression );
        } catch (SAXPathException e) {
            throw new InvalidXpathException(e);
        }

        if ( !invalidPrefixes.isEmpty() ) {
            throw new InvalidXpathException("Namespace not found for prefix" + (invalidPrefixes.size()>1 ? "es " : " ") + invalidPrefixes);
        }
    }

    static DOMXPath compile(String expression, Map<String, String> namespaceMap, @Nullable XpathVariableFinder variableFinder) throws JaxenException {
        DOMXPath dx = new DOMXPath(expression);
        if (namespaceMap != null)
            dx.setNamespaceContext(makeNamespaceContext(namespaceMap));
        if (variableFinder != null)
            dx.setVariableContext(new XpathVariableFinderVariableContext(variableFinder));
        return dx;
    }
    
    private static void putNamespaceWithPreferredPrefix( final Map<String, String> namespaces,
                                                         String ePrefix,
                                                         final String eNamespace ) {
        if (ePrefix == null || ePrefix.length()==0) {
            ePrefix = "ns";
        }

        if ( !namespaces.containsKey(ePrefix) ) {
            namespaces.put(ePrefix, eNamespace);
        } else if ( !eNamespace.equals(namespaces.get(ePrefix)) &&
                    !namespaces.containsValue(eNamespace) ) {
            // add with an alternative prefix to avoid overwriting an existing mapping
            for ( int i=1; i<1000; i++ ) {
                final String prefix = ePrefix + i;
                if ( !namespaces.containsKey(prefix) ) {
                    namespaces.put(prefix, eNamespace);
                    break;
                }
            }
        }
    }

    /**
     * Convenience method to execute an xpath against an Element and return all found Elements.
     *
     * @param elementToSearch Element to execute xpath against
     * @param xpath           Xpath to use
     * @param namespaceMap    Required namespaces
     * @return list of all Elements found by the xpath.
     * @throws RuntimeException if any problems were found executing the xpath. This method should only be used
     *                          by callers where such an exception represents a programming error.
     */
    @NotNull
    public static List<Element> findElements(@NotNull final Element elementToSearch,
                                             @NotNull final String xpath,
                                             @Nullable final Map<String, String> namespaceMap) {
        final List<Element> toReturn = new ArrayList<>();

        final DomElementCursor includeCursor = new DomElementCursor(elementToSearch, false);
        final XpathResult result = getXpathResultQuietly(includeCursor, namespaceMap, xpath);

        if (result.getType() == XpathResult.TYPE_NODESET) {
            final XpathResultIterator iterator = result.getNodeSet().getIterator();
            while (iterator.hasNext()) {
                toReturn.add(iterator.nextElementAsCursor().asDomElement());
            }
        }

        return toReturn;
    }
}
