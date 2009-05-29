/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.xml.xpath;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.xml.DomElementCursor;
import com.l7tech.xml.ElementCursor;
import com.l7tech.xml.InvalidXpathException;
import com.l7tech.util.ExceptionUtils;
import org.jaxen.FunctionContext;
import org.jaxen.JaxenException;
import org.jaxen.XPathFunctionContext;
import org.jaxen.dom.DOMXPath;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.xpath.XPathExpressionException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

/**
 * A version of CompiledXpath that works with DOM.
 */
public class DomCompiledXpath extends CompiledXpath {
    private static final Logger logger = Logger.getLogger(DomCompiledXpath.class.getName());
    private static final FunctionContext XPATH_FUNCTIONS = new XPathFunctionContext(false);
    private DOMXPath domXpath;

    /**
     * Create a CompiledXpath instance that will support Jaxen, but may also support other engines, if
     * added by a subclass.  The expression will not be compiled immediately by this constructor.
     *
     * @param expression the generic xpath expression, not optimized for any particular implementation.  Must not be null.
     * @param nsmap      the namespace map, or null if no qualified names are used by expression.
     */
    protected DomCompiledXpath(String expression, Map nsmap) {
        super(expression, nsmap);
        domXpath = null;
    }

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
        super(xp.getExpressionForJaxen(), xp.getNamespaces());
        domXpath = makeJaxenXpath();
    }

    /**
     * Get the compiled Jaxen DOMXPath.  This may require lazy compilation.
     *
     * @return the compiled Jaxen DOMXpath.  Never null.
     * @throws InvalidXpathException if the expression is invalid or uses an undeclared namespace prefix.
     */
    protected DOMXPath getDomXpath() throws InvalidXpathException {
        DOMXPath dxp;
        synchronized (this) {
            dxp = domXpath;
        }

        if (dxp != null)
            return dxp;
        
        synchronized (this) {
            return domXpath = makeJaxenXpath();
        }
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

    /**
     * Run a software-only XPath.
     *
     * @param cursor   the DOM cursor on which to run the xpath.  Must not be null.
     * @param variableFinder
     * @return a new XpathResult instance.  Never null.
     * @throws XPathExpressionException if lazy compilation of the XPath reveals it to be invalid.
     */
    public XpathResult getXpathResult(final DomElementCursor cursor, XpathVariableFinder variableFinder) throws XPathExpressionException {
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
            logger.warning("Jaxen xpath evaluation returned unsupported type " + o.getClass().getName());
            return null;
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
