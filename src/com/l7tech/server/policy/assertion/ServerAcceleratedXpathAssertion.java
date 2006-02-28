/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.message.Message;
import com.l7tech.common.message.TarariKnob;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.xml.InvalidXpathException;
import com.l7tech.common.xml.TarariLoader;
import com.l7tech.common.xml.XpathExpression;
import com.l7tech.common.xml.tarari.GlobalTarariContext;
import com.l7tech.common.xml.tarari.TarariMessageContext;
import com.l7tech.common.xml.tarari.TarariMessageContextImpl;
import com.l7tech.common.xml.tarari.util.TarariXpathConverter;
import com.l7tech.policy.assertion.*;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.tarari.xml.cursor.XmlCursor;
import com.tarari.xml.rax.cursor.RaxCursorFactory;
import com.tarari.xml.rax.cursor.RaxCursor;
import com.tarari.xml.rax.fastxpath.XPathResult;
import com.tarari.xml.rax.fastxpath.FNodeSet;
import com.tarari.xml.rax.fastxpath.FNode;
import com.tarari.xml.xpath10.expr.Expression;
import com.tarari.xml.xpath10.parser.ExpressionParser;
import com.tarari.xml.xpath10.parser.XPathParseContext;
import com.tarari.xml.xpath10.parser.XPathParseException;
import com.tarari.xml.xpath10.object.XObject;
import com.tarari.xml.xpath10.object.XNodeSet;
import com.tarari.xml.xpath10.XPathContext;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.text.ParseException;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Common code used by both {@link ServerRequestAcceleratedXpathAssertion} and {@link ServerResponseAcceleratedXpathAssertion}.
 * The "Accelerated" request and response xpath assertions are not "real" policy assertions; they are just
 * alternate implementations of the request and response xpath assertions that use the hardware instead.  The
 * ServerPolicyFactory instantiates the hardware-assisted versions if hardware support seems to be available.
 * <p/>
 * This implementation attempts to register the XPath for Tarari Simultaneous XPath.  If this fails, or
 * the necessary XPath wasn't present when a particular document was evaluated, it tries to fall back
 * to Tarari Direct XPath.  If that fails, it falls back to the fallback assertion given to the constructor (usually a
 * standard software-only ServerXpathAssertion).
 *
 */
public abstract class ServerAcceleratedXpathAssertion implements ServerAssertion {
    protected static final Logger logger = Logger.getLogger(ServerAcceleratedXpathAssertion.class.getName());

    private static final RaxCursorFactory raxCursorFactory = new RaxCursorFactory();

    private final Auditor auditor;

    // Globally registered expr for simultaneous xpath, or null if it couldn't be registered.
    private final String globallyRegisteredExpr;

    // Parsed expression for direct XPath 1.0, or null if not available.
    private final Expression parsedDirectExpression;

    // Software fallback assertion, for use as last resort.  Never null.
    private final ServerAssertion softwareDelegate;

    // If true, we are operating on the request; otherwise we are operating on the reply.
    private final boolean isReq;

    // Assertion bean holding variable names with prefixes
    private final SimpleXpathAssertion assertion;

    /**
     * Prepare a hardware accelerated xpath assertion.
     *
     * @param assertion   the Request or Response xpath assertion containing the xpath expression to use.  Mustn't be null.
     * @param applicationContext  the application context from which to get the Tarari server context.  Mustn't be null.
     * @param softwareDelegate a ServerAssertion to which checkRequest() should be delegated if hardware acceleration can't be performed.
     */
    protected ServerAcceleratedXpathAssertion(XpathBasedAssertion assertion, ApplicationContext applicationContext, ServerAssertion softwareDelegate) {
        if (!(assertion instanceof SimpleXpathAssertion))
                throw new IllegalArgumentException(); // can't happen
        this.softwareDelegate = softwareDelegate;
        this.assertion = (SimpleXpathAssertion)assertion;
        this.isReq = assertion instanceof RequestXpathAssertion;
        this.auditor = new Auditor(this, applicationContext, logger);

        GlobalTarariContext tarariContext = TarariLoader.getGlobalContext();

        final XpathExpression xpathExpression = assertion.getXpathExpression();
        this.globallyRegisteredExpr = setupSimultaneousXpath(tarariContext, xpathExpression);
        this.parsedDirectExpression = setupDirectXpath(tarariContext, xpathExpression);
    }

    /**
     * Prepare a parsed expression for use with Tarari XPath 1.0.  Will fail if hardware isn't present,
     * or if the expression isn't valid XPath 1.0.
     *
     * @param tarariContext  the global tarari context, or null if no hardware is available.
     * @param xpathExpression  the expression to attempt to preparse.  Must not be null.
     * @return the parsed Expression, or null if hardware not present or expression is invalid.
     */
    private static Expression setupDirectXpath(GlobalTarariContext tarariContext, XpathExpression xpathExpression) {
        if (tarariContext == null) {
            // No hardware.
            return null;
        }

        ExpressionParser expressionParser = new ExpressionParser();

        // Configure the namespace map for Direct XPath 1.0
        XPathParseContext parseContext = expressionParser.getParseContext();
        Set uris = xpathExpression.getNamespaces().entrySet();
        for (Iterator i = uris.iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry)i.next();
            final String nsPrefix = (String)entry.getKey();
            final String nsUri = (String)entry.getValue();
            parseContext.declareNamespace(nsPrefix, nsUri);
        }

        try {
            return expressionParser.parseExpression(xpathExpression.getExpression());
        } catch (XPathParseException e) {
            // This expression isn't valid XPath 1.0.
            return null;
        }
    }

    /**
     * Try to prepare and globally register an expression for simultaneous XPath.  Will fail if hardware isn't present
     * or if the expression is too complex to convert to Tarari Normal Form.
     *
     * @param tarariContext  the global tarari context, or null if no hardware is available.
     * @param xp the expression to attempt to convert and register.  Must not be null.
     * @return  the registered expression, if and only if it was registered successfully; otherwise null.
     */
    private static String setupSimultaneousXpath(GlobalTarariContext tarariContext, XpathExpression xp) {
        if (tarariContext == null) {
            // No hardware.
            return null;
        }

        final String es = xp.getExpression();
        try {
            // Convert this Xpath into tarari format
            String expr = TarariXpathConverter.convertToTarariXpath(xp.getNamespaces(),
                                                                    es);

            // Register this Xpath with the tarari hardware
            tarariContext.addXpath(expr);
            return expr;
        } catch (InvalidXpathException e) {
            logger.log(Level.INFO, "Expression not supported by simultaneous XPath -- will fallback to direct XPath: " + es, e);
            return null;
        } catch (ParseException e) {
            logger.log(Level.INFO, "Expression not supported by simultaneous XPath -- will fallback to direct XPath: " + es + ": " + e.getMessage());
            return null;
        }
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        context.setVariable(assertion.foundVariable(), SimpleXpathAssertion.FALSE);
        context.setVariable(assertion.countVariable(), "0");
        context.setVariable(assertion.resultVariable(), null);

        final Message mess = isReq ? context.getRequest() : context.getResponse();

        if (globallyRegisteredExpr == null && parsedDirectExpression == null)
            return fallbackToSoftwareOnly(context);

        GlobalTarariContext tarariContext = TarariLoader.getGlobalContext();
        if (tarariContext == null) {
            auditor.logAndAudit(AssertionMessages.ACCEL_XPATH_NO_HARDWARE);
            return fallbackToSoftwareOnly(context);
        }

        final TarariKnob tknob;
        try {
            // Ensure Tarari context is attached, if possible
            // TODO need a better way to attach this
            mess.isSoap();
            tknob = (TarariKnob) mess.getKnob(TarariKnob.class);
            if (tknob == null) {
                auditor.logAndAudit(AssertionMessages.ACCEL_XPATH_NO_CONTEXT);
                return fallbackToSoftwareOnly(context);
            }

            TarariMessageContext tmc = tknob.getContext();
            TarariMessageContextImpl tmContext = (TarariMessageContextImpl)tmc;
            if (tmContext == null) {
                auditor.logAndAudit(AssertionMessages.ACCEL_XPATH_NO_CONTEXT);
                return fallbackToSoftwareOnly(context);
            }

            if (globallyRegisteredExpr == null)
                return fallbackToDirectXPath(context, tmContext);

            int index = tarariContext.getXpathIndex(globallyRegisteredExpr, tmc.getCompilerGeneration());
            if (index < 1) {
                auditor.logAndAudit(AssertionMessages.ACCEL_XPATH_UNSUPPORTED_PATTERN);
                return fallbackToDirectXPath(context, tmContext);
            }

            // We're now committed to using Simultaneous XPath results for this

            XPathResult xpathResult = tmContext.getXpathResult();
            int numMatches = xpathResult.getCount(index);
            context.setVariable(assertion.countVariable(), Integer.toString(numMatches));
            if (numMatches > 0) {
                FNodeSet ns = xpathResult.getNodeSet(index);
                int numNodes = ns.size();
                // TODO when context variables support arrays, store more than the first one here
                if (numNodes > 0) {
                    FNode n = ns.getNode(0);
                    context.setVariable(assertion.resultVariable(), n.getXPathValue());
                    context.setVariable(assertion.foundVariable(), SimpleXpathAssertion.TRUE);
                    auditor.logAndAudit(isReq ? AssertionMessages.XPATH_SUCCEED_REQUEST : AssertionMessages.XPATH_SUCCEED_RESPONSE);

                    final short nodeType = n.getType();
                    switch (nodeType) {
                        case FNode.ELEMENT_NODE:
                            auditor.logAndAudit(AssertionMessages.XPATH_ELEMENT_FOUND);
                            return AssertionStatus.NONE;

                        case FNode.ATTRIBUTE_NODE:
                            auditor.logAndAudit(AssertionMessages.XPATH_TEXT_NODE_FOUND);
                            return AssertionStatus.NONE;

                        case FNode.TEXT_NODE:
                            auditor.logAndAudit(AssertionMessages.XPATH_OTHER_NODE_FOUND);
                            return AssertionStatus.NONE;

                        case FNode.UNSUPPORTED_NODE:
                        default:
                            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {"Simultaneous XPath result matched unsupported node type " + nodeType});
                            return AssertionStatus.NONE;
                    }

                }
                // (count > 0 but numnodes <= 0: can't happen)
                auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {"Simultaneous XPath result had positive count but empty nodeset"});
            }

            context.setVariable(assertion.foundVariable(), SimpleXpathAssertion.FALSE);
            return AssertionStatus.FALSIFIED;
        } catch (SAXException e) {
            auditor.logAndAudit(isReq ? AssertionMessages.XPATH_REQUEST_NOT_XML : AssertionMessages.XPATH_RESPONSE_NOT_XML);
            return AssertionStatus.FAILED;
        } catch (NoSuchPartException e) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_INFO_WITH_MORE_INFO, new String[] {"The required attachment " + e.getWhatWasMissing() + "was not found in the request"}, e);
            return AssertionStatus.FAILED;
        }
    }

    private AssertionStatus fallbackToDirectXPath(PolicyEnforcementContext context,
                                                  TarariMessageContextImpl tctx)
            throws PolicyAssertionException, IOException
    {
        if (parsedDirectExpression == null)
            return fallbackToSoftwareOnly(context);

        // We're now committed to using Direct XPath results for this

        RaxCursor cursor = raxCursorFactory.createCursor("", tctx.getRaxDocument());
        XPathContext xpathContext = new XPathContext();
        xpathContext.setNode(cursor);

        XObject xo = parsedDirectExpression.toXObject(xpathContext);
        int resultType = xo.getType();

        switch (resultType) {
            case XObject.TYPE_BOOLEAN:
                boolean booleanVal = xo.toBooleanValue();
                context.setVariable(assertion.resultVariable(), Boolean.toString(booleanVal));
                context.setVariable(assertion.countVariable(), "1");
                if (booleanVal) {
                    context.setVariable(assertion.foundVariable(), SimpleXpathAssertion.TRUE);
                    auditor.logAndAudit(AssertionMessages.XPATH_RESULT_TRUE);
                    return AssertionStatus.NONE;
                }
                auditor.logAndAudit(AssertionMessages.XPATH_RESULT_FALSE);
                return AssertionStatus.FALSIFIED;

            case XObject.TYPE_NUMBER:
                double numVal = xo.toNumberValue();
                context.setVariable(assertion.resultVariable(), Double.toString(numVal));
                context.setVariable(assertion.countVariable(), "1");
                context.setVariable(assertion.foundVariable(), SimpleXpathAssertion.TRUE);
                // TODO what to log for this?
                // auditor.logAndAudit(AssertionMessages.XPATH_TEXT_NODE_FOUND);
                return AssertionStatus.NONE;

            case XObject.TYPE_STRING:
                String strVal = xo.toStringValue();
                context.setVariable(assertion.resultVariable(), strVal);
                context.setVariable(assertion.countVariable(), "1");
                context.setVariable(assertion.foundVariable(), SimpleXpathAssertion.TRUE);
                // TODO what to log for this?
                // auditor.logAndAudit(AssertionMessages.XPATH_TEXT_NODE_FOUND);
                return AssertionStatus.NONE;

            case XObject.TYPE_VARIABLE:
                // TODO what do we do with this?
                auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {"Tarari Direct XPath evaluation produced unsupported XObject type TYPE_VARIABLE"});
                return AssertionStatus.FAILED;

            case XObject.TYPE_NODESET:
                XNodeSet ns = xo.toNodeSet();
                int numNodes = ns.size();
                context.setVariable(assertion.countVariable(), Integer.toString(numNodes));
                if (numNodes > 0) {
                    XmlCursor n = ns.getNode(0);
                    context.setVariable(assertion.resultVariable(), n.getNodeValue());
                    context.setVariable(assertion.foundVariable(), SimpleXpathAssertion.TRUE);

                    int nodeType = n.getNodeType();
                    switch (nodeType) {
                        case XmlCursor.ELEMENT:
                            auditor.logAndAudit(AssertionMessages.XPATH_ELEMENT_FOUND);
                            return AssertionStatus.NONE;
                        case XmlCursor.TEXT:
                            auditor.logAndAudit(AssertionMessages.XPATH_TEXT_NODE_FOUND);
                            return AssertionStatus.NONE;
                        default:
                            auditor.logAndAudit(AssertionMessages.XPATH_OTHER_NODE_FOUND);
                            return AssertionStatus.NONE;
                    }
                }
                return AssertionStatus.FALSIFIED;

            default:
                auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {"Tarari Direct XPath evaluation produced unknown XObject type " + resultType});
                return AssertionStatus.FAILED;
        }
    }

    private AssertionStatus fallbackToSoftwareOnly(PolicyEnforcementContext context) throws PolicyAssertionException, IOException {
        return softwareDelegate.checkRequest(context);
    }

    protected void finalize() throws Throwable {
        if (globallyRegisteredExpr != null) {
            // Decrement the reference count for this Xpath with the Tarari hardware
            GlobalTarariContext tarariContext = TarariLoader.getGlobalContext();
            if (tarariContext != null)
                tarariContext.removeXpath(globallyRegisteredExpr);
        }
        super.finalize();
    }
}
