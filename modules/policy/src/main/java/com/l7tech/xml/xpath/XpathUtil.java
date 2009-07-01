package com.l7tech.xml.xpath;

import com.l7tech.util.ExceptionUtils;
import org.jaxen.JaxenException;
import org.jaxen.NamespaceContext;
import org.jaxen.SimpleNamespaceContext;
import org.jaxen.dom.DOMXPath;
import org.jaxen.saxpath.SAXPathException;
import org.jaxen.saxpath.XPathHandler;
import org.jaxen.saxpath.base.XPathReader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.soap.*;
import javax.xml.xpath.XPathExpressionException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility methods for dealing with XPaths.
 */
public class XpathUtil {
    public static final Logger logger = Logger.getLogger(XpathUtil.class.getName());

    /**
     * Find all unprefixed variables used in the specified XPath expression.
     * This method ignores any variables that are used with a namespace prefix.
     * If the expression cannot be parsed, this method returns an empty list. 
     *
     * @param expr the expression to examine.  Required.
     * @return a list of all unprefixed XPath variable references found in the expression.  May be empty but never null.
     */
    public static List<String> getUnprefixedVariablesUsedInXpath(String expr) {
        final List<String> seenvars = new ArrayList<String>();
        try {
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
        } catch (SAXPathException e) {
            logger.log(Level.INFO, "Unable to parse XPath expression to determine variables used: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            /* FALLTHROUGH and leave seenvars empty */
        }
        return seenvars;
    }

    /**
     * Check if the specified XPath expression uses any variables.
     * This counts variables with prefixes, and so will return true even if {@link #getUnprefixedVariablesUsedInXpath(String)}
     * would return an empty list for the same expression.
     *
     * @param expr the expression to examine.  Required.
     * @return true iff. the expression makes use of any XPath variables
     * @throws XPathExpressionException if the expression cannot be parsed
     */
    public static boolean usesXpathVariables(String expr) throws XPathExpressionException {
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
    public static Map getNamespaces(SOAPMessage sm) throws SOAPException {
        Map<String, String> namespaces = new HashMap<String, String>();
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
    public static void addNamespaces(Map<String, String> namespaces, SOAPElement element) {
        String ePrefix = element.getElementName().getPrefix();
        String eNamespace = element.getElementName().getURI();
        if (ePrefix != null && ePrefix.length()>0)
            namespaces.put(ePrefix, eNamespace);
        Iterator iterator = element.getNamespacePrefixes();
        while (iterator.hasNext()) {
            String prefix = (String)iterator.next();
            String uri = element.getNamespaceURI(prefix);
            if (prefix != null && uri != null) {
                if (prefix.length() > 0 && uri.length() > 0) {
                    namespaces.put(prefix, uri);
                }
            }
        }
        Iterator itchildren = element.getChildElements();
        while(itchildren.hasNext()) {
            Object o = itchildren.next();
            if (o instanceof SOAPElement) {
                final SOAPElement childElement = (SOAPElement)o;
                addNamespaces(namespaces, childElement);
            }
        }
    }

    /**
     * Create a Jaxen NamespaceContext from the specified namespace Map.
     *
     * @param namespaces a Map of prefix to namespace URI.  Required.
     * @return a Jaxen NamespaceContext that will look up the specified namespaces. Never null.
     */
    public static NamespaceContext makeNamespaceContext(Map<String, String> namespaces) {
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

        final List<Element> resultList = new ArrayList<Element>();
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
     * Use Jaxen to immediately compile and evaluate the specified XPath against the root of the specified document, using
     * the specified namespace map and variable finder, and returning the evaluation result.
     *
     * @param targetDocument the document against which to evaluate the XPath.  Required.
     * @param expression     the expression to compile and evaluate.  Required.
     * @param namespaceMap   the namespace map to use.  May be null if the expression uses no namespace prefixes.
     * @param variableFinder the variable finder to use to look up variable values.  May be null if the expression uses no XPath variables.
     * @return The result of the evaluation.
     * @throws JaxenException if the expression cannot be compiled, or if it returned a result other than a node list.
     */
    public static Object compileAndEvaluate(Document targetDocument, String expression, Map<String, String> namespaceMap, XpathVariableFinder variableFinder) throws JaxenException {
        DOMXPath dx = compile(expression, namespaceMap, variableFinder);
        return dx.evaluate(targetDocument);
    }

    /**
     * Use Jaxen to immediately compile and select nodes against the specified XPath against the root of the specified document, using
     * the specified namespace map and variable finder, and returning the evaluation result.
     *
     * @param targetDocument the document against which to evaluate the XPath.  Required.
     * @param expression     the expression to compile and evaluate.  Required.
     * @param namespaceMap   the namespace map to use.  May be null if the expression uses no namespace prefixes.
     * @param variableFinder the variable finder to use to look up variable values.  May be null if the expression uses no XPath variables.
     * @return The result of the calling selectNodes().
     * @throws JaxenException if the expression cannot be compiled, or if it returned a result other than a node list.
     */
    public static List compileAndSelect(Document targetDocument, String expression, Map<String, String> namespaceMap, XpathVariableFinder variableFinder) throws JaxenException {
        DOMXPath dx = compile(expression, namespaceMap, variableFinder);
        return dx.selectNodes(targetDocument);
    }

    private static DOMXPath compile(String expression, Map<String, String> namespaceMap, XpathVariableFinder variableFinder) throws JaxenException {
        DOMXPath dx = new DOMXPath(expression);
        if (namespaceMap != null)
            dx.setNamespaceContext(makeNamespaceContext(namespaceMap));
        if (variableFinder != null)
            dx.setVariableContext(new XpathVariableFinderVariableContext(variableFinder));
        return dx;
    }
}
