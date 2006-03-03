/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.xml.tarari;

import com.l7tech.common.xml.DomElementCursor;
import com.l7tech.common.xml.TarariLoader;
import com.l7tech.common.xml.InvalidXpathException;
import com.l7tech.common.xml.xpath.*;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.server.tarari.GlobalTarariContextImpl;
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

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A CompiledXpath implementation that uses Tarari RAXJ features directly, and which will set up a fastxpath
 * expression if possible, but will fall back (lazily) to a DOM-based Jaxen xpath if all else fails.
 */
public class TarariCompiledXpath extends DomCompiledXpath {
    private static final Logger logger = Logger.getLogger(TarariCompiledXpath.class.getName());
    private static final RaxCursorFactory raxCursorFactory = new RaxCursorFactory();

    // Globally registered expr for simultaneous xpath, or null if it couldn't be registered.
    private final String globallyRegisteredExpr;

    // Parsed expression for direct XPath 1.0, or null if not available.
    private final Expression parsedDirectExpression;

    /**
     * Create a TarariCompiledXpath.  Do not call this directly unless you are GlobalTarariContextImpl.
     *
     * @param compilableXpath the XPath to compile.  Must not be null.
     */
    public TarariCompiledXpath(CompilableXpath compilableXpath, GlobalTarariContextImpl tarariContext) {
        super(compilableXpath.getExpressionForJaxen(), compilableXpath.getNamespaces());

        this.globallyRegisteredExpr = setupSimultaneousXpath(tarariContext, compilableXpath);
        this.parsedDirectExpression = setupDirectXpath(tarariContext, compilableXpath);
    }

    /**
     * Prepare a parsed expression for use with Tarari XPath 1.0.  Will fail if hardware isn't present,
     * or if the expression isn't valid XPath 1.0.
     *
     * @param tarariContext the global tarari context, or null if no hardware is available.
     * @param xp  the expression to attempt to preparse.  Must not be null.
     * @return the parsed Expression, or null if hardware not present or expression is invalid.
     */
    private static Expression setupDirectXpath(GlobalTarariContext tarariContext, CompilableXpath xp) {
        if (tarariContext == null) {
            // No hardware.
            return null;
        }

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
            // This expression isn't valid XPath 1.0.
            return null;
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
    private static String setupSimultaneousXpath(GlobalTarariContextImpl tarariContext, CompilableXpath xp) {
        if (tarariContext == null)
            throw new NullPointerException();

        // Convert this Xpath into tarari format
        String expr = xp.toTarariNormalForm();
        if (expr == null) {
            logger.log(Level.FINE, "Expression not supported by simultaneous XPath -- will fallback to direct XPath: " +
                    xp.getExpressionForTarariFastxpath());
            return null;
        }

        // Register this Xpath with the tarari hardware
        tarariContext.addXpath(expr);
        return expr;
    }

    public XpathResult getXpathResult(TarariElementCursor cursor) {
        if (globallyRegisteredExpr == null && parsedDirectExpression == null)
            return fallbackToSoftwareOnly(cursor);

        GlobalTarariContextImpl tarariContext = (GlobalTarariContextImpl)TarariLoader.getGlobalContext();
        if (tarariContext == null) {
            return fallbackToSoftwareOnly(cursor);
        }

        TarariMessageContextImpl tmContext = cursor.getTarariMessageContext();
        if (tmContext == null)
            return fallbackToSoftwareOnly(cursor);

        if (globallyRegisteredExpr == null)
            return fallbackToDirectXPath(cursor, tmContext);

        final int index = tarariContext.getXpathIndex(globallyRegisteredExpr, tmContext.getCompilerGeneration());
        if (index < 1)
            return fallbackToDirectXPath(cursor, tmContext);

        // We're now committed to using Simultaneous XPath results for this

        final XPathResult xpathResult = tmContext.getXpathResult();
        final int numMatches = xpathResult.getCount(index);

        // Bundle up the fastxpath result
        return new XpathResult.XpathResultAdapter() {
            FNodeSet ns = null;

            public boolean matches() {
                return numMatches > 0;
            }

            public short getType() {
                return TYPE_NODESET;
            }

            public XpathResultNodeSet getNodeSet() {
                if (ns == null) ns = xpathResult.getNodeSet(index);

                return new XpathResultNodeSet() {
                    public boolean isEmpty() {
                        return ns.size() > 0;
                    }

                    public int size() {
                        return ns.size();
                    }

                    public XpathResultIterator getIterator() {
                        return new XpathResultIterator() {
                            int cur = 0;
                            int size = size();
                            FNode node = null;

                            public boolean hasNext() {
                                return cur < ns.size();
                            }

                            public void next(XpathResultNode t) throws NoSuchElementException {
                                if (cur >= size) throw new NoSuchElementException("No more matching nodes");
                                node = ns.getNode(cur++);
                                t.type = getDomTypeForFnode(node);
                                t.localNameHaver = node.getLocalName();
                                t.nodeNameHaver = node.getQName();
                                t.prefixHaver = node.getPrefix();
                                t.valueHaver = this; // defer this one, since it might be expensive
                            }

                            public String toString() {
                                // The contract of next() allows us to do this here instead
                                return node == null ? null : node.getXPathValue();
                            }
                        };
                    }

                    public int getType(int ordinal) {
                        FNode node = ns.getNode(ordinal);
                        return getDomTypeForFnode(node);
                    }

                    public String getNodePrefix(int ordinal) {
                        FNode node = ns.getNode(ordinal);
                        return node.getPrefix();
                    }

                    public String getNodeLocalName(int ordinal) {
                        FNode node = ns.getNode(ordinal);
                        return node.getLocalName();
                    }

                    public String getNodeName(int ordinal) {
                        FNode node = ns.getNode(ordinal);
                        return node.getQName();
                    }

                    public String getNodeValue(int ordinal) {
                        FNode node = ns.getNode(ordinal);
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

    private XpathResult fallbackToDirectXPath(TarariElementCursor tec, TarariMessageContextImpl tctx)
    {
        if (parsedDirectExpression == null)
            return fallbackToSoftwareOnly(tec);

        // We're now committed to using Direct XPath results for this

        RaxCursor cursor = raxCursorFactory.createCursor("", tctx.getRaxDocument());
        XPathContext xpathContext = new XPathContext();
        xpathContext.setNode(cursor);

        final XObject xo = parsedDirectExpression.toXObject(xpathContext);
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
                logger.warning("Tarari direct XPath produced unsupported result type " + resultType);
                return null;
        }

        // It's a nodeset.
        final XNodeSet ns = xo.toNodeSet();

        // Bundle up the direct xpath nodeset
        return new XpathResult.XpathResultAdapter() {
            public short getType() {
                return TYPE_NODESET;
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
                                t.valueHaver = this; // defer this one, since it might be expensive
                            }

                            public String toString() {
                                // The contract of next() allows us to do this here instead
                                return node == null ? null : node.getNodeValue();
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

    private XpathResult fallbackToSoftwareOnly(TarariElementCursor cursor) {
        logger.warning("TarariElementCursor running TarariCompiledXpath has fallen all the way back to Jaxen");
        try {
            return super.getXpathResult(new DomElementCursor(cursor.asDomElement().getOwnerDocument()));
        } catch (InvalidXpathException e) {
            // Shouldn't be possible
            logger.log(Level.WARNING, "Invalid xpath expression: " + ExceptionUtils.getMessage(e), e);
            return null;
        }
    }

    protected void finalize() throws Throwable {
        if (globallyRegisteredExpr != null) {
            // Decrement the reference count for this Xpath with the Tarari hardware
            GlobalTarariContextImpl tarariContext = (GlobalTarariContextImpl)TarariLoader.getGlobalContext();
            if (tarariContext != null)
                tarariContext.removeXpath(globallyRegisteredExpr);
            else
                logger.severe("Registered a global xpath, the global context isn't here anymore"); // can't happen
        }
        super.finalize();
    }
}
