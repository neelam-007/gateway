/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.xml.xpath;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.util.DomUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.xml.DomElementCursor;
import com.l7tech.xml.ElementCursor;
import com.l7tech.xml.InvalidXpathException;
import com.l7tech.xml.SaxonUtils;
import net.sf.saxon.dom.DocumentWrapper;
import net.sf.saxon.dom.NodeWrapper;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.s9api.*;
import org.jaxen.FunctionContext;
import org.jaxen.JaxenException;
import org.jaxen.XPathFunctionContext;
import org.jaxen.dom.DOMXPath;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.xpath.XPathExpressionException;
import java.math.BigDecimal;
import java.net.URI;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A version of CompiledXpath that works with DOM.
 */
public class DomCompiledXpath extends CompiledXpath {
    private static final Logger logger = Logger.getLogger(DomCompiledXpath.class.getName());
    private static final FunctionContext XPATH_FUNCTIONS = new XPathFunctionContext(false);

    private final AtomicReference<DOMXPath> domXpath = new AtomicReference<DOMXPath>();
    private final AtomicReference<XPathExecutable> saxonXpath = new AtomicReference<XPathExecutable>();
    private final AtomicBoolean skipSaxon = new AtomicBoolean(false);
    private final AtomicBoolean skipJaxen = new AtomicBoolean(false);

    /**
     * Create a CompiledXpath instance that will only ever support Jaxen as its XPath engine.  The expression
     * will be compiled immediately.
     * <p/>
     * In most cases, you should not be using this constructor -- instead, you should use
     * {@link XpathExpression#compile()}, which will produce a hardware-accelerated
     * CompiledXpath if the hardware is available.
     *
     * @param xp the expression to compile.  Must not be null.
     * @throws InvalidXpathException if the expression is invalid or uses an undeclared namespace prefix.
     */
    public DomCompiledXpath(CompilableXpath xp) throws InvalidXpathException {
        super(xp.getExpressionForJaxen(), xp.getXpathVersion(), xp.getNamespaces());
        domXpath.set(makeJaxenXpathIfPossible());
        saxonXpath.set(makeSaxonXpathIfPossible());
        if (domXpath.get() == null && saxonXpath.get() == null)
            throw new InvalidXpathException("XPath version and/or syntax not supported by any available XPath processor");
    }

    /**
     * Get the compiled Jaxen DOMXPath.  This may require lazy compilation.
     *
     * @return the compiled Jaxen DOMXpath.  Never null.
     * @throws InvalidXpathException if the expression is invalid or uses an undeclared namespace prefix.
     */
    protected DOMXPath getDomXpath() throws InvalidXpathException {
        if (skipJaxen.get())
            return null;

        DOMXPath dxp = domXpath.get();
        if (dxp == null) {
            synchronized (this) {
                dxp = domXpath.get();
                if (dxp == null) {
                    dxp = makeJaxenXpathIfPossible();
                    domXpath.set(dxp);
                }
            }
        }
        return dxp;
    }

    private DOMXPath makeJaxenXpathIfPossible() throws InvalidXpathException {
        if (!"1.0".equals(getXpathVersion())) {
            // Not gonna work, has to be Saxon
            skipJaxen.set(true);
            return null;
        }
        return makeJaxenXpath();
    }

        /**
        * Make a Jaxen compiled xpath from the current expression.
        *
        * @return a new DOMXPath, compiled for use with Jaxen.  Never null.
        * @throws InvalidXpathException if the current expression is invalid or uses an undeclared namespace prefix.
        */
    private DOMXPath makeJaxenXpath() throws InvalidXpathException {
        String expression = getExpression();
        Map namespaceMap = getNamespaceMap();

        if (expression == null)
            throw new InvalidXpathException("No expression is currently set");

        try {
            final DOMXPath domXpath = new DOMXPath(expression);
            domXpath.setFunctionContext(XPATH_FUNCTIONS); // no JAXEN extensions

            // Stupidly, Jaxen bakes the variable context into the DOMXPath, so we'll have to rendezvous
            // with a thread-local variable finder to pick up the appropriate values later.
            domXpath.setVariableContext(new XpathVariableFinderVariableContext(null));

            if (namespaceMap != null) {
                for (Object o : namespaceMap.keySet()) {
                    String key = (String) o;
                    String uri = (String) namespaceMap.get(key);
                    domXpath.addNamespace(key, uri);
                }
            }

            // fail early for expressions that are syntactically valid but that use incorrect
            // namespace prefixes or functions
            try {
                XpathVariableContext.doWithVariableFinder( new XpathVariableFinder(){
                    @Override
                    public Object getVariableValue(String namespaceUri, String variableName) {
                        return null;
                    }
                }, new Callable<Object>(){
                    @Override
                    public Object call() throws Exception {
                        domXpath.evaluate(XmlUtil.stringAsDocument("<test xmlns=\"http://test.com/testing\"/>"));
                        return null;
                    }
                });
            } catch (JaxenException e) {
                throw e;
            } catch (Exception e) {
                throw ExceptionUtils.wrap(e);
            }

            return domXpath;
        } catch (JaxenException e) {
            throw new InvalidXpathException(e);
        }
    }

    protected XPathExecutable getSaxonXpath() throws InvalidXpathException {
        if (skipSaxon.get())
            return null;

        XPathExecutable xpe = saxonXpath.get();
        if (xpe == null) {
            synchronized (this) {
                xpe = saxonXpath.get();
                if (xpe == null) {
                    xpe = makeSaxonXpathIfPossible();
                    saxonXpath.set(xpe);
                }
            }
        }
        return xpe;
    }

    private XPathExecutable makeSaxonXpathIfPossible() throws InvalidXpathException {
        try {
            return makeSaxonXpath();
        } catch (InvalidXpathException e) {
            logger.log(Level.INFO, "Saxon support unavailable for xpath " + getExpression(), ExceptionUtils.getDebugException(e));
            skipSaxon.set(true);
            return null;
        }
    }

    private XPathExecutable makeSaxonXpath() throws InvalidXpathException {
            String expression = getExpression();
        Map<String, String> namespaceMap = getNamespaceMap();

        if (expression == null)
            throw new InvalidXpathException("No expression is currently set");

        final XPathCompiler compiler = SaxonUtils.getProcessor().newXPathCompiler();
        if (namespaceMap != null) for (Map.Entry<String, String> ns : namespaceMap.entrySet()) {
            compiler.declareNamespace(ns.getKey(), ns.getValue());
        }
        compiler.setLanguageVersion(getXpathVersion());
        try {
            return compiler.compile(expression);
        } catch (SaxonApiException e) {
            throw new InvalidXpathException(ExceptionUtils.getMessage(e), e);
        }
    }

    /**
     * Run a software-only XPath.
     *
     * @param cursor   the DOM cursor on which to run the xpath.  Must not be null.
     * @param variableFinder an XpathVariableFinder instance for processing variables in the expression, or null.
     * @return a new XpathResult instance.  Never null.
     * @throws XPathExpressionException if lazy compilation of the XPath reveals it to be invalid.
     */
    public XpathResult getXpathResult(final DomElementCursor cursor, @Nullable XpathVariableFinder variableFinder) throws XPathExpressionException {
        if (variableFinder == null)
            return getXpathResult(cursor);
        try {
            return XpathVariableContext.doWithVariableFinder(variableFinder, new Callable<XpathResult>() {
                @Override
                public XpathResult call() throws Exception {
                    return getXpathResult(cursor);
                }
            });
        } catch (XPathExpressionException e) {
            throw e;
        } catch (Exception e) {
            throw new XPathExpressionException(e);
        }
    }

    /**
     * A low-level utility method that returns a list of DOM Elements selected by evaluating this compiled XPath
     * against the root of the specified target document.
     *
     * @param targetDocument  the document to select against.  Required.
     * @param variableFinder  the XpathVariableFinder to use during the selection.  Required if {@link #usesVariables()} is true.
     * @return a List of zero or more DOM Element instances matched by this compiled XPath.  May be empty but never null.
     * @throws JaxenException if an exception occurred while matching.
     */
    public List<Element> rawSelectElements(final Document targetDocument, XpathVariableFinder variableFinder) throws JaxenException {
        try {
            return XpathVariableContext.doWithVariableFinder(variableFinder, new Callable<List<Element>>() {
                @Override
                public List<Element> call() throws Exception {
                    DOMXPath dx = getDomXpath();
                    List nodes = dx.selectNodes(targetDocument);
                    return XpathUtil.ensureAllResultsAreElements(nodes);
                }
            });
        } catch (JaxenException e) {
            throw e;
        } catch (Exception e) {
            throw new JaxenException(e);
        }
    }

    private XpathResult getXpathResult(DomElementCursor cursor) throws XPathExpressionException {
        if (saxonXpath.get() != null) {
            return getXpathResultWithSaxon(cursor);
        } else {
            return getXpathResultWithJaxen(cursor);
        }
    }

    private XpathResult getXpathResultWithSaxon(DomElementCursor cursor) throws XPathExpressionException {
        final XPathExecutable xpe;
        try {
            xpe = getSaxonXpath();
            XPathSelector selector = xpe.load();
            selector.setURIResolver(new URIResolver() {
                @Override
                public Source resolve(String href, String base) throws TransformerException {
                    throw new TransformerException("Extrenal ref not supported");
                }
            });

            Node node = cursor.asDomNode();
            DocumentWrapper documentWrapper = new DocumentWrapper(cursor.asDomElement().getOwnerDocument(), "", SaxonUtils.getConfiguration());
            NodeWrapper nodeWrapper = documentWrapper.wrap(node);
            XdmNode xdmNode = new XdmNode(nodeWrapper);
            selector.setContextItem(xdmNode);

            XpathVariableFinder finder = XpathVariableContext.getCurrentVariableFinder();
            if (finder != null) {
                Iterator<QName> vars = xpe.iterateExternalVariables();
                while (vars.hasNext()) {
                    QName var = vars.next();
                    try {
                        final Object value = finder.getVariableValue(var.getNamespaceURI(), var.getLocalName());
                        final XdmValue xdmValue = asXdmValue(value);
                        selector.setVariable(var, xdmValue);
                    } catch (NoSuchXpathVariableException e) {
                        throw (XPathExpressionException)new XPathExpressionException("Unknown XPath variable: " + var).initCause(e);
                    }
                }
            }

            final XdmValue result = selector.evaluate();

            if (result == null || result.size() == 0)
                return XpathResult.RESULT_EMPTY;

            XdmItem firstItem = result.itemAt(0);
            if (!firstItem.isAtomicValue()) {
                return new XpathResult.XpathResultAdapter() {
                    @Override
                    public short getType() {
                        return TYPE_NODESET;
                    }

                    @Override
                    public XpathResultNodeSet getNodeSet() {
                        return new XpathResultNodeSet() {
                            @Override
                            public boolean isEmpty() {
                                return false;
                            }

                            @Override
                            public int size() {
                                return result.size();
                            }

                            @Override
                            public XpathResultIterator getIterator() {
                                return new XpathResultIterator() {
                                    private XdmSequenceIterator i = result.iterator();
                                    private Node node = null;
                                    private Object nodeValueMaker = new Object() {
                                        @Override
                                        public String toString() {
                                            if (node == null) throw new IllegalStateException();
                                            return node.getTextContent();
                                        }
                                    };
                                    private Node nextNode = null; // If not null, this was unread from the iterator and is waiting to be returned

                                    @Override
                                    public boolean hasNext() {
                                        return i.hasNext();
                                    }

                                    @Override
                                    public void next(XpathResultNode template) throws NoSuchElementException {
                                        node = doNext();
                                        template.type = node.getNodeType();
                                        template.localNameHaver = node.getLocalName();
                                        template.prefixHaver = node.getPrefix();
                                        template.nodeNameHaver = node.getNodeName();
                                        template.nodeValueHaver = nodeValueMaker;
                                    }

                                    private Node doNext() {
                                        Node o;
                                        if (nextNode != null) {
                                            o = nextNode;
                                        } else {
                                            XdmItem item = i.next();
                                            o = asDomNode(item);
                                        }
                                        nextNode = null;


                                        if (!(o instanceof Node)) {
                                            // Not supposed to be possible
                                            throw new IllegalStateException("Saxonica xpath result nodeset included non-Node object of type " + o.getClass().getName());
                                        }

                                        return (Node)o;
                                    }

                                    @Override
                                    public ElementCursor nextElementAsCursor() throws NoSuchElementException {
                                        Node n = doNext();
                                        if (n.getNodeType() != Node.ELEMENT_NODE && n.getNodeType() != Node.DOCUMENT_NODE) {
                                            nextNode = n;
                                            return null;
                                        }
                                        node = n;
                                        return new DomElementCursor(n, false);
                                    }
                                };
                            }

                            private Node getNode(int ordinal) {
                                return asDomNode(result.itemAt(ordinal));
                            }

                            @Override
                            public int getType(int ordinal) {
                                final Node n = getNode(ordinal);
                                return n == null ? -1 : n.getNodeType();
                            }

                            @Override
                            public String getNodePrefix(int ordinal) {
                                final Node node = getNode(ordinal);
                                return node == null ? null : node.getPrefix();
                            }

                            @Override
                            public String getNodeLocalName(int ordinal) {
                                final Node node = getNode(ordinal);
                                return node == null ? null : node.getLocalName();
                            }

                            @Override
                            public String getNodeName(int ordinal) {
                                final Node node = getNode(ordinal);
                                return node == null ? null : node.getNodeName();
                            }

                            @Override
                            public String getNodeValue(int ordinal) {
                                final Node node = getNode(ordinal);
                                return node == null ? null : node.getTextContent();
                            }
                        };
                    }

                    private Node asDomNode(XdmItem item) {
                        Node o;
                        if (item instanceof XdmNode) {
                            XdmNode node = (XdmNode) item;
                            NodeInfo nodeInfo = node.getUnderlyingNode();
                            if (nodeInfo instanceof NodeWrapper) {
                                NodeWrapper nodeWrapper = (NodeWrapper) nodeInfo;
                                o = (Node)nodeWrapper.getUnderlyingNode();
                            } else
                                throw new IllegalStateException("Saxon xpath result nodeset included XdmNode containing non-NodeWrapper object of type " + nodeInfo.getClass().getName());
                        } else
                            throw new IllegalStateException("Saxon xpath result nodeset included non-XdmNode object of type " + item.getClass().getName());
                        return o;
                    }
                };
            }

            final XdmAtomicValue value = (XdmAtomicValue) firstItem;
            final Object objValue = value.getValue();
            final short type;
            if (objValue instanceof Boolean) {
                type = XpathResult.TYPE_BOOLEAN;
            } else if (objValue instanceof Number) {
                type = XpathResult.TYPE_NUMBER;
            } else {
                type = XpathResult.TYPE_STRING;
            }

            return new XpathResult.XpathResultAdapter() {
                @Override
                public short getType() {
                    return type;
                }

                @Override
                public String getString() {
                    return value.getStringValue();
                }

                @Override
                public boolean getBoolean() {
                    try {
                        return value.getBooleanValue();
                    } catch (SaxonApiException e) {
                        return false;
                    }
                }

                @Override
                public double getNumber() {
                    try {
                        return value.getDoubleValue();
                    } catch (SaxonApiException e) {
                        return 0;
                    }
                }
            };


        } catch (InvalidXpathException e) {
            throw new XPathExpressionException(e);
        } catch (SaxonApiException e) {
            throw new XPathExpressionException(e);
        } catch (RuntimeException rte) {
            throw new XPathExpressionException(rte);
        }
    }

    private static XdmValue asXdmValue(Object value) throws NoSuchXpathVariableException {
        final XdmValue xdm;
        if (value instanceof XdmValue) {
            xdm = (XdmValue) value;
        } else if (value == null) {
            throw new NoSuchXpathVariableException("Xpath variable has null value");
        } else if (value instanceof Boolean) {
            xdm = new XdmAtomicValue((Boolean) value);
        } else if (value instanceof CharSequence) {
            xdm = new XdmAtomicValue(((CharSequence)value).toString());
        } else if (value instanceof Long) {
            xdm = new XdmAtomicValue((Long) value);
        } else if (value instanceof Double) {
            xdm = new XdmAtomicValue((Double) value);
        } else if (value instanceof BigDecimal) {
            xdm = new XdmAtomicValue((BigDecimal) value);
        } else if (value instanceof Float) {
            xdm = new XdmAtomicValue((Float) value);
        } else if (value instanceof URI) {
            xdm = new XdmAtomicValue((URI) value);
        } else if (value instanceof QName) {
            xdm = new XdmAtomicValue((QName) value);
        } else if (value instanceof Iterable) {
            List<XdmItem> values = new ArrayList<XdmItem>();
            for (Object next : ((Iterable) value)) {
                final XdmValue val = asXdmValue(next);
                if (val instanceof XdmItem) {
                    XdmItem item = (XdmItem) val;
                    values.add(item);
                } else {
                    throw new NoSuchXpathVariableException("Xpath variable: Nested value has unsupported type " + val.getClass().getName());
                }
            }
            xdm = new XdmValue(values);
        } else if (value instanceof Node) {
            Node node = (Node) value;
            xdm = new XdmNode(new DocumentWrapper(DomUtils.getOwnerDocument(node), "", SaxonUtils.getConfiguration()));
        } else {
            throw new NoSuchXpathVariableException("Xpath variable: Nested value has unsupported type " + value.getClass().getName());
        }
        return xdm;
    }

    private XpathResult getXpathResultWithJaxen(DomElementCursor cursor) throws XPathExpressionException {
        final DOMXPath xp;

        final List result;
        try {
            xp = getDomXpath();
            result = xp.selectNodes(cursor.asDomNode());
        } catch (JaxenException e) {
            throw new XPathExpressionException(e);
        } catch (RuntimeException rte) {
            // How does this happen?
            throw new XPathExpressionException(rte);
        } catch (InvalidXpathException e) {
            throw new RuntimeException(e); // can't happen
        }

        if (result == null || result.size() < 1)
            return XpathResult.RESULT_EMPTY;

        Object o = result.get(0);
        if (o instanceof Boolean) {
            final Boolean b = (Boolean)o;
            return new XpathResult.XpathResultAdapter() {
                @Override
                public short getType() {
                    return TYPE_BOOLEAN;
                }

                @Override
                public boolean getBoolean() {
                    return b;
                }
            };
        }
        if (o instanceof Double) {
            final Double d = (Double)o;
            return new XpathResult.XpathResultAdapter() {
                @Override
                public short getType() {
                    return TYPE_NUMBER;
                }

                @Override
                public double getNumber() {
                    return d;
                }
            };
        }
        if (o instanceof String) {
            final String s = (String)o;
            return new XpathResult.XpathResultAdapter() {
                @Override
                public short getType() {
                    return TYPE_STRING;
                }

                @Override
                public String getString() {
                    return s;
                }
            };
        }

        // No other result but node should be possible, but we'll check just in case
        if (!(o instanceof Node)) {
            throw new XPathExpressionException("XPath evaluation returned unsupported type " + o.getClass().getName());
        }

        return new XpathResult.XpathResultAdapter() {
            @Override
            public short getType() {
                return TYPE_NODESET;
            }

            @Override
            public XpathResultNodeSet getNodeSet() {
                return new XpathResultNodeSet() {
                    @Override
                    public boolean isEmpty() {
                        return result.isEmpty();
                    }

                    @Override
                    public int size() {
                        return result.size();
                    }

                    @Override
                    public XpathResultIterator getIterator() {
                        return new XpathResultIterator() {
                            private Iterator i = result.iterator();
                            private Node node = null;
                            private Object nodeValueMaker = new Object() {
                                @Override
                                public String toString() {
                                    if (node == null) throw new IllegalStateException();
                                    return node.getTextContent();
                                }
                            };
                            private Node nextNode = null; // If not null, this was unread from the iterator and is waiting to be returned

                            @Override
                            public boolean hasNext() {
                                return i.hasNext();
                            }

                            @Override
                            public void next(XpathResultNode template) throws NoSuchElementException {
                                node = doNext();
                                template.type = node.getNodeType();
                                template.localNameHaver = node.getLocalName();
                                template.prefixHaver = node.getPrefix();
                                template.nodeNameHaver = node.getNodeName();
                                template.nodeValueHaver = nodeValueMaker;
                            }

                            private Node doNext() {
                                Object o = nextNode == null ? i.next() : nextNode;
                                nextNode = null;
                                if (!(o instanceof Node)) {
                                    // Not supposed to be possible
                                    throw new IllegalStateException("Jaxen xpath result nodeset included non-Node object of type " + o.getClass().getName());
                                }
                                return (Node)o;
                            }

                            @Override
                            public ElementCursor nextElementAsCursor() throws NoSuchElementException {
                                Node n = doNext();
                                if (n.getNodeType() != Node.ELEMENT_NODE && n.getNodeType() != Node.DOCUMENT_NODE) {
                                    nextNode = n;
                                    return null;
                                }
                                node = n;
                                return new DomElementCursor(n, false);
                            }
                        };
                    }

                    private Node getNode(int ordinal) {
                        Object n = result.get(ordinal);
                        if (n instanceof Node)
                            return (Node)n;
                        // Not supposed to be possible
                        throw new IllegalStateException("Jaxen xpath result nodeset included non-Node object of type " + n.getClass().getName());
                    }

                    @Override
                    public int getType(int ordinal) {
                        final Node n = getNode(ordinal);
                        return n == null ? -1 : n.getNodeType();
                    }

                    @Override
                    public String getNodePrefix(int ordinal) {
                        final Node node = getNode(ordinal);
                        return node == null ? null : node.getPrefix();
                    }

                    @Override
                    public String getNodeLocalName(int ordinal) {
                        final Node node = getNode(ordinal);
                        return node == null ? null : node.getLocalName();
                    }

                    @Override
                    public String getNodeName(int ordinal) {
                        final Node node = getNode(ordinal);
                        return node == null ? null : node.getNodeName();
                    }

                    @Override
                    public String getNodeValue(int ordinal) {
                        final Node node = getNode(ordinal);
                        return node == null ? null : node.getTextContent();
                    }
                };
            }
        };
    }

}
