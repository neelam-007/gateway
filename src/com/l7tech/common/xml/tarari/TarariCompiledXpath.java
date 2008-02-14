/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.xml.tarari;

import com.l7tech.common.xml.InvalidXpathException;
import com.l7tech.common.xml.TarariLoader;
import com.l7tech.common.xml.SoftwareFallbackException;
import com.l7tech.common.xml.ElementCursor;
import com.l7tech.common.xml.xpath.*;
import com.tarari.xml.cursor.XmlCursor;
import com.tarari.xml.rax.cursor.RaxCursor;
import com.tarari.xml.rax.cursor.RaxCursorFactory;
import com.tarari.xml.rax.fastxpath.FNode;
import com.tarari.xml.rax.fastxpath.FNodeSet;
import com.tarari.xml.rax.fastxpath.XPathResult;
import com.tarari.xml.xpath10.XPathContext;
import com.tarari.xml.xpath10.expr.Expression;
import com.tarari.xml.xpath10.object.XNodeSet;
import com.tarari.xml.xpath10.object.XObject;
import com.tarari.xml.xpath10.parser.ExpressionParser;
import com.tarari.xml.xpath10.parser.XPathParseContext;
import com.tarari.xml.xpath10.parser.XPathParseException;
import org.w3c.dom.Node;

import javax.xml.xpath.XPathExpressionException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A CompiledXpath implementation that uses Tarari RAXJ features directly, and which will set up a fastXpath
 * expression if possible, but will fall back (lazily) to a DOM-based Jaxen xpath if all else fails.
 */
class TarariCompiledXpath extends DomCompiledXpath {
    private static final Logger logger = Logger.getLogger(TarariCompiledXpath.class.getName());
    private static final RaxCursorFactory raxCursorFactory = new RaxCursorFactory();

    // Globally registered expr for simultaneous xpath, or null if it couldn't be registered.
    private final FastXpath fastXpath;

    // Parsed expression for direct XPath 1.0, or null if not available.
    private final Expression directxpath;

    /**
     * Create a TarariCompiledXpath.  Do not call this directly unless you are GlobalTarariContextImpl.
     *
     * @param compilableXpath the XPath to compile.  Must not be null.
     * @param tarariContext the global tarari context.  Must not be null.
     */
    public TarariCompiledXpath(CompilableXpath compilableXpath, GlobalTarariContextImpl tarariContext) throws InvalidXpathException {
        super(compilableXpath.getExpressionForJaxen(), compilableXpath.getNamespaces());
        if (tarariContext == null) throw new NullPointerException();
        this.fastXpath = setupSimultaneousXpath(tarariContext, compilableXpath);
        this.directxpath = setupDirectXpath(compilableXpath);
        if (directxpath == null) throw new InvalidXpathException("No direct XPath"); // can't happen
    }

    /**
     * Prepare a parsed expression for use with Tarari XPath 1.0.  Will fail if hardware isn't present,
     * or if the expression isn't valid XPath 1.0.
     *
     * @param xp  the expression to attempt to preparse.  Must not be null.
     * @return the parsed Expression.  Never null.
     * @throws InvalidXpathException if the expression can't be parsed.
     */
    private static Expression setupDirectXpath(CompilableXpath xp) throws InvalidXpathException {
        ExpressionParser expressionParser = new ExpressionParser();

        // Configure the namespace map for Direct XPath 1.0
        XPathParseContext parseContext = expressionParser.getParseContext();
        Set uris = xp.getNamespaces().entrySet();
        for (Iterator i = uris.iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry)i.next();
            final String nsPrefix = (String)entry.getKey();
            final String nsUri = (String)entry.getValue();
            parseContext.declareNamespace(nsPrefix, nsUri);
        }

        try {
            return expressionParser.parseExpression(xp.getExpressionForTarari());
        } catch (XPathParseException e) {
            throw new InvalidXpathException(e);
        }
    }

    /**
     * Try to prepare and globally register an expression for simultaneous XPath.  Will fail if hardware isn't present
     * or if the expression is too complex to convert to Tarari Normal Form.
     *
     * @param tarariContext  the global tarari context.  Must not be null.
     * @param xp the expression to attempt to convert and register.  Must not be null.
     * @return  the registered expression, if and only if it was registered successfully; otherwise null.
     */
    private static FastXpath setupSimultaneousXpath(GlobalTarariContextImpl tarariContext, CompilableXpath xp) {
        if (tarariContext == null)
            throw new NullPointerException();

        // Convert this Xpath into tarari format
        FastXpath fastXpath = xp.toTarariNormalForm();
        if (fastXpath == null) {
            logger.log(Level.FINE, "Expression not supported by simultaneous XPath (too complex) -- will fallback to direct XPath: " +
                    xp.getExpressionForTarariFastxpath());
            return null;
        }

        // Register this Xpath with the tarari hardware
        tarariContext.addXpath(fastXpath.getExpression());
        return fastXpath;
    }

    public XpathResult getXpathResult(TarariElementCursor cursor, boolean requireCursor) throws XPathExpressionException {
        final TarariMessageContextImpl tmContext = cursor.getTarariMessageContext();
        if (requireCursor || fastXpath == null || fastXpath.getExpression() == null || !cursor.isAtRoot())
            return fallbackToDirectXPath(tmContext); // expression was too complex to simplify into TNF or isn't matching against root

        final GlobalTarariContextImpl tarariContext = (GlobalTarariContextImpl)TarariLoader.getGlobalContext();
        final int index;
        final XPathResult xpathResult;
        try {
            index = tarariContext.getXpathIndex(fastXpath.getExpression(), tmContext.getCompilerGeneration());
            if (index < 1)
                return fallbackToDirectXPath(tmContext); // expression wasn't loaded into the card yet
            xpathResult = tmContext.getXpathResult();
        } catch (SoftwareFallbackException e) {
            return fallbackToDirectXPath(tmContext); // simultaneous xpath evaluation failed
        }

        // We're now committed to using Simultaneous XPath results for this
        final int numMatches = xpathResult.getCount(index);

        // See if we are supposed to transform the result to a boolean
        if (fastXpath.getCountComparison() != null) {
            // Yep -- convert to simple boolean
            return new XpathResult.XpathResultAdapter() {
                public short getType() {
                    return TYPE_BOOLEAN;
                }

                public boolean getBoolean() {
                    return fastXpath.getCountComparison().compare(Integer.valueOf(numMatches), fastXpath.getCountValue(), false);
                }
            };
        }

        if (numMatches < 1)
            return XpathResult.RESULT_EMPTY;

        // Bundle up the fastXpath result
        return new XpathResult.XpathResultAdapter() {
            FNodeSet ns = null;

            public boolean matches() {
                return numMatches > 0;
            }

            public short getType() {
                return TYPE_NODESET;
            }

            public XpathResultNodeSet getNodeSet() {
                return new XpathResultNodeSet() {
                    private FNodeSet ns() {
                        if (ns == null) ns = xpathResult.getNodeSet(index);
                        return ns;
                    }

                    public boolean isEmpty() {
                        return numMatches > 0;
                    }

                    public int size() {
                        return numMatches;
                    }

                    public XpathResultIterator getIterator() {
                        return new XpathResultIterator() {
                            int cur = 0;
                            int size = size();
                            FNode node = null;
                            final Object nodeValueMaker = new Object() {
                                public String toString() {
                                    if (node == null) throw new IllegalStateException();
                                    return node.getXPathValue();
                                }
                            };

                            public boolean hasNext() {
                                return ns() != null && cur < ns().size();
                            }

                            public void next(XpathResultNode t) throws NoSuchElementException {
                                if (cur >= size || ns() == null) throw new NoSuchElementException("No more matching nodes");
                                node = ns().getNode(cur++);
                                t.type = getDomTypeForFnode(node);
                                t.localNameHaver = node.getLocalName();
                                t.nodeNameHaver = node.getQName();
                                t.prefixHaver = node.getPrefix();
                                t.nodeValueHaver = nodeValueMaker;
                            }

                            public ElementCursor nextElementAsCursor() throws NoSuchElementException {
                                if (cur >= size || ns() == null) throw new NoSuchElementException("No more matching nodes");
                                return null; // not supported for FNodeSet
                            }
                        };
                    }

                    public int getType(int ordinal) {
                        if (ns() == null) return -1;
                        FNode node = ns().getNode(ordinal);
                        return getDomTypeForFnode(node);
                    }

                    public String getNodePrefix(int ordinal) {
                        if (ns() == null) return null;
                        FNode node = ns().getNode(ordinal);
                        return node.getPrefix();
                    }

                    public String getNodeLocalName(int ordinal) {
                        if (ns() == null) return null;
                        FNode node = ns().getNode(ordinal);
                        return node.getLocalName();
                    }

                    public String getNodeName(int ordinal) {
                        if (ns() == null) return null;
                        FNode node = ns().getNode(ordinal);
                        return node.getQName();
                    }

                    public String getNodeValue(int ordinal) {
                        if (ns() == null) return null;
                        FNode node = ns().getNode(ordinal);
                        return node.getXPathValue();
                    }
                };
            }
        };
    }

    private static int getDomTypeForFnode(FNode node) {
        switch (node.getType()) {
            case FNode.ATTRIBUTE_NODE:
                return Node.ATTRIBUTE_NODE;
            case FNode.ELEMENT_NODE:
                return Node.ELEMENT_NODE;
            case FNode.TEXT_NODE:
                return Node.TEXT_NODE;
            default:
                return -1;
        }
    }

    private XpathResult fallbackToDirectXPath(final TarariMessageContextImpl tctx) throws XPathExpressionException {
        // We're now committed to using Direct XPath results for this

        RaxCursor cursor = raxCursorFactory.createCursor("", tctx.getRaxDocument());
        XPathContext xpathContext = new XPathContext();
        xpathContext.setNode(cursor);

        final XObject xo = directxpath.toXObject(xpathContext);
        int resultType = xo.getType();
        switch (resultType) {
            case XObject.TYPE_BOOLEAN:
                return new XpathResult.XpathResultAdapter() {
                    public short getType() {
                        return TYPE_BOOLEAN;
                    }

                    public boolean getBoolean() {
                        return xo.toBooleanValue();
                    }
                };

            case XObject.TYPE_NUMBER:
                return new XpathResult.XpathResultAdapter() {
                    public short getType() {
                        return TYPE_NUMBER;
                    }

                    public double getNumber() {
                        return xo.toNumberValue();
                    }
                };

            case XObject.TYPE_STRING:
                return new XpathResult.XpathResultAdapter() {
                    public short getType() {
                        return TYPE_STRING;
                    }

                    public String getString() {
                        return xo.toStringValue();
                    }
                };

            case XObject.TYPE_NODESET:
                /* FALLTHROUGH and handle nodeset */
                break;

            default:
                // Unsupported result
                throw new XPathExpressionException("Tarari direct XPath produced unsupported result type " + resultType);
        }

        // It's a nodeset.
        final XNodeSet ns = xo.toNodeSet();
        if (ns.size() < 1)
            return XpathResult.RESULT_EMPTY;

        // Bundle up the direct xpath nodeset
        return new XpathResult.XpathResultAdapter() {
            public short getType() {
                return TYPE_NODESET;
            }

            public boolean matches() {
                return ns.size() > 0;
            }

            public XpathResultNodeSet getNodeSet() {
                return new XpathResultNodeSet() {
                    public boolean isEmpty() {
                        return ns.isEmpty();
                    }

                    public int size() {
                        return ns.size();
                    }

                    public XpathResultIterator getIterator() {
                        return new XpathResultIterator() {
                            int cur = 0;
                            int size = size();
                            XmlCursor node = null;
                            private final Object nodeValueMaker = new Object() {
                                public String toString() {
                                    return node.getNodeValue();
                                }
                            };

                            public boolean hasNext() {
                                return cur < ns.size();
                            }

                            public void next(XpathResultNode t) throws NoSuchElementException {
                                if (cur >= size) throw new NoSuchElementException("No more matching nodes");
                                node = ns.getNode(cur++);
                                t.type = getDomTypeForXmlCursor(node);
                                t.localNameHaver = node.getNodeLocalName();
                                t.nodeNameHaver = node.getNodeName();
                                t.prefixHaver = node.getNodePrefix();
                                t.nodeValueHaver = nodeValueMaker;
                            }

                            public ElementCursor nextElementAsCursor() throws NoSuchElementException {
                                if (cur >= size) throw new NoSuchElementException("No more matching nodes");
                                XmlCursor n = ns.getNode(cur);
                                if (n.getNodeType() != XmlCursor.ELEMENT && n.getNodeType() != XmlCursor.ROOT)
                                    return null;
                                node = n;
                                cur++;
                                return new TarariElementCursor(node, tctx, false);
                            }
                        };
                    }

                    public int getType(int ordinal) {
                        XmlCursor node = ns.getNode(ordinal);
                        return getDomTypeForXmlCursor(node);
                    }

                    public String getNodePrefix(int ordinal) {
                        XmlCursor node = ns.getNode(ordinal);
                        return node.getNodePrefix();
                    }

                    public String getNodeLocalName(int ordinal) {
                        XmlCursor node = ns.getNode(ordinal);
                        return node.getNodeLocalName();
                    }

                    public String getNodeName(int ordinal) {
                        XmlCursor node = ns.getNode(ordinal);
                        return node.getNodeName();
                    }

                    public String getNodeValue(int ordinal) {
                        XmlCursor node = ns.getNode(ordinal);
                        return node.getNodeValue();
                    }
                };
            }
        };
    }

    private static int getDomTypeForXmlCursor(XmlCursor node) {
        final int type = node.getNodeType();
        switch (type) {
            case XmlCursor.ATTRIBUTE:
                return Node.ATTRIBUTE_NODE;
            case XmlCursor.ELEMENT:
                return Node.ELEMENT_NODE;
            case XmlCursor.COMMENT:
                return Node.COMMENT_NODE;
            case XmlCursor.TEXT:
                return Node.TEXT_NODE;
            case XmlCursor.ROOT:
                return Node.DOCUMENT_NODE;
            default:
                return -1;
        }
    }

    protected void finalize() throws Throwable {
        if (fastXpath != null && fastXpath.getExpression() != null) {
            // Decrement the reference count for this Xpath with the Tarari hardware
            GlobalTarariContextImpl tarariContext = (GlobalTarariContextImpl)TarariLoader.getGlobalContext();
            if (tarariContext != null)
                tarariContext.removeXpath(fastXpath.getExpression());
            else
                logger.severe("Registered a global xpath, the global context isn't here anymore"); // can't happen
        }
        super.finalize();
    }
}
