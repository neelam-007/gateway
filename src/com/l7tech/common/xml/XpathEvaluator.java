package com.l7tech.common.xml;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.jaxen.dom.DOMXPath;
import org.jaxen.XPath;
import org.jaxen.JaxenException;
import org.jaxen.NamespaceContext;
import org.jaxen.SimpleNamespaceContext;

import javax.xml.soap.*;
import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.util.HashMap;

/**
 * The class resovles an XPath expression against a XML document.
 * It is based on <a href="http://www.jaxen.org">Jaxen</a> XPath
 * implementation.
 * <p/>
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class XpathEvaluator {
    protected Document document;
    protected NamespaceContext nameSpaceContext;

    /**
     * protected constructor to support class extension. Use one of
     * factory methods to instantiate the class.
     */
    protected XpathEvaluator() {
    }

    /**
     * Creates the new evaluator for a given XML document with namespace
     * {prefix, URI} definitons specified in the <code>Map</code>.
     * <p/>
     *
     * @param doc        the DOM Documet
     * @param namespaces optional namespace definition, formed by the prefix
     *                   and the URI.
     * @return the new XpathEvaluator instance
     */
    public static XpathEvaluator newEvaluator(Document doc, Map namespaces) {
        if (doc == null) {
            throw new IllegalArgumentException();
        }
        NamespaceContext ctx = null;
        if (namespaces != null) {
            SimpleNamespaceContext sn = new SimpleNamespaceContext();
            for (Iterator iterator = namespaces.keySet().iterator(); iterator.hasNext();) {
                String prefix = (String)iterator.next();
                sn.addNamespace(prefix, (String)namespaces.get(prefix));
            }
            ctx = sn;
        }
        return newEvaluator(doc, ctx);
    }

    /**
     * Creates the new evaluator for a given XML document with namespace
     * resolver specified.
     * <p/>
     *
     * @param doc the DOM Documet
     * @param ctx optional namespace resolver
     * @return the new XpathEvaluator instance
     */
    public static XpathEvaluator newEvaluator(Document doc, NamespaceContext ctx) {
        if (doc == null) {
            throw new IllegalArgumentException();
        }
        XpathEvaluator xe = new XpathEvaluator();
        xe.document = doc;
        xe.nameSpaceContext = ctx;
        return xe;
    }

    /**
     * Get the map of namespaces that are declared in the SOAP message.
     * This is a utility method that is typically used to extract the
     * namespaces from the SOAP message before evaluating the XPath
     * expression.
     *
     * @param sm the sopamessage
     * @return the namespace <code>Map</code> as {prefix, URI}
     * @throws SOAPException on SOAP processing error
     */
    public static Map getNamespaces(SOAPMessage sm) throws SOAPException {
        Map namespaces = new HashMap();
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
    private static void addNamespaces(Map namespaces, SOAPElement element) {
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
     * Select the nodes that are selectable with this expression against
     * this evaluator's Document.
     *
     * @param expression the XPath expression
     * @return a List of Object (hopefully Node) - the nodes that are selectable by this XPath expression.
     * @throws JaxenException thrown on evaluation error
     */
    public List select(String expression) throws JaxenException {
        XPath xpath = new DOMXPath(expression);
        if (nameSpaceContext !=null) {
            xpath.setNamespaceContext(nameSpaceContext);
        }
        return xpath.selectNodes(document);
    }

    /**
     * Select the nodes that are selectable with this expression against
     * this evaluator's Document.  If any of the selected Nodes are not Elements,
     * this will throw a JaxenException.
     *
     * @param expression
     * @return a List of Element - the elements that are selectable by this XPath expression
     * @throws JaxenException if the expression is bad, or any non-Element nodes were selected
     */
    public List selectElements(String expression) throws JaxenException {
        XPath xpath = new DOMXPath(expression);
        if (nameSpaceContext !=null) {
            xpath.setNamespaceContext(nameSpaceContext);
        }
        List nodes = xpath.selectNodes(document);
        if (nodes == null)
            return nodes;
        for (Iterator i = nodes.iterator(); i.hasNext();) {
            Object obj = i.next();
            if (!(obj instanceof Node))  // fix for Bug #984
                throw new JaxenException("Xpath evaluation produced a non-empty result, but it wasn't of type Node.  Type: " + obj.getClass().getName());
            Node node = (Node)obj;
            if (node.getNodeType() != Node.ELEMENT_NODE)
                throw new JaxenException("Xpath result included a non-Element Node of type " + node.getNodeType());
        }
        return nodes;
    }

    /**
     * Evaluate the expression against this evaluator's Document.
     *
     * @param expression the XPath expression
     * @return the nodes that are selectable by this XPath expression.
     * @throws JaxenException thrown on evaluation error
     */
    public Object evaluate(String expression) throws JaxenException {
        XPath xpath = new DOMXPath(expression);
        if (nameSpaceContext !=null) {
            xpath.setNamespaceContext(nameSpaceContext);
        }
        return xpath.evaluate(document);
    }
}
