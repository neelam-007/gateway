/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.xml.xpath;

import com.l7tech.common.xml.DomElementCursor;
import com.l7tech.common.xml.InvalidXpathException;
import com.l7tech.common.util.XmlUtil;
import org.jaxen.JaxenException;
import org.jaxen.dom.DOMXPath;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

/**
 * A version of CompiledXpath that works with DOM.
 */
public class DomCompiledXpath extends CompiledXpath {
    private static final Logger logger = Logger.getLogger(DomCompiledXpath.class.getName());
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
     * {@link com.l7tech.common.xml.XpathExpression#compile()}, which will produce a hardware-accelerated
     * CompiledXpath if the hardware is available.
     *
     * @param xp
     * @throws com.l7tech.common.xml.InvalidXpathException if the expression is invalid or uses an undeclared namespace prefix.
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
        if (domXpath != null)
            return domXpath;
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
            DOMXPath domXpath = new DOMXPath(expression);

            if (namespaceMap != null) {
                for (Iterator i = namespaceMap.keySet().iterator(); i.hasNext();) {
                    String key = (String)i.next();
                    String uri = (String)namespaceMap.get(key);
                    domXpath.addNamespace(key, uri);
                }
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
     * @return a new XpathResult instance, or null if no result could be generated.
     * @throws InvalidXpathException if lazy compilation of the XPath reveals it to be invalid.
     */
    public XpathResult getXpathResult(DomElementCursor cursor) throws InvalidXpathException {
        DOMXPath xp = getDomXpath();

        final List result;
        try {
            result = xp.selectNodes(cursor.asDomNode());
        } catch (JaxenException e) {
            throw new InvalidXpathException(e);
        } catch (RuntimeException rte) {
            // How does this happen?
            throw new InvalidXpathException(rte);
        }

        if (result == null || result.size() < 1)
            return null;

        Object o = result.get(0);
        if (o instanceof Boolean) {
            final Boolean b = (Boolean)o;
            return new XpathResult.XpathResultAdapter() {
                public short getType() {
                    return TYPE_BOOLEAN;
                }

                public boolean getBoolean() {
                    return b.booleanValue();
                }
            };
        }
        if (o instanceof Double) {
            final Double d = (Double)o;
            return new XpathResult.XpathResultAdapter() {
                public short getType() {
                    return TYPE_NUMBER;
                }

                public double getNumber() {
                    return d.doubleValue();
                }
            };
        }
        if (o instanceof String) {
            final String s = (String)o;
            return new XpathResult.XpathResultAdapter() {
                public short getType() {
                    return TYPE_STRING;
                }

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
            public short getType() {
                return TYPE_NODESET;
            }

            public XpathResultNodeSet getNodeSet() {
                return new XpathResultNodeSet() {
                    public boolean isEmpty() {
                        return result.isEmpty();
                    }

                    public int size() {
                        return result.size();
                    }

                    public XpathResultIterator getIterator() {
                        return new XpathResultIterator() {
                            private Iterator i = result.iterator();
                            private Node node = null;

                            public boolean hasNext() {
                                return i.hasNext();
                            }

                            public void next(XpathResultNode template) throws NoSuchElementException {
                                Object o = i.next();
                                if (!(o instanceof Node)) {
                                    // Not supposed to be possible
                                    throw new IllegalStateException("Jaxen xpath result nodeset included non-Node object of type " + o.getClass().getName());
                                }
                                node = (Node)o;
                                template.type = node.getNodeType();
                                template.localNameHaver = node.getLocalName();
                                template.prefixHaver = node.getPrefix();
                                template.nodeNameHaver = node.getNodeName();
                                template.valueHaver = this; // expensive, so don't do this one unless it's asked-for
                            }

                            public String toString() {
                                // The XpathResultIterator.next() contract lets us cache state that only lasts until next next()
                                return node == null ? null : getNodeValue(node);
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

                    public int getType(int ordinal) {
                        return getNode(ordinal).getNodeType();
                    }

                    public String getNodePrefix(int ordinal) {
                        return getNode(ordinal).getPrefix();
                    }

                    public String getNodeLocalName(int ordinal) {
                        return getNode(ordinal).getLocalName();
                    }

                    public String getNodeName(int ordinal) {
                        return getNode(ordinal).getNodeName();
                    }

                    public String getNodeValue(int ordinal) {
                        return getNodeValue(getNode(ordinal));
                    }

                    String getNodeValue(Node n) {
                        if (n instanceof Element) {
                            Element element = (Element)n;
                            return XmlUtil.getTextValue(element);
                        }
                        return n.getNodeValue();
                    }
                };
            }
        };
    }
}
