/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.policy.assertion;

import com.l7tech.common.message.Message;
import com.l7tech.common.message.TarariKnob;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.xml.InvalidXpathException;
import com.l7tech.common.xml.TarariLoader;
import com.l7tech.common.xml.XpathExpression;
import com.l7tech.common.xml.tarari.GlobalTarariContext;
import com.l7tech.common.xml.tarari.TarariMessageContext;
import com.l7tech.common.xml.tarari.TarariMessageContextImpl;
import com.l7tech.common.xml.tarari.util.TarariXpathConverter;
import com.l7tech.policy.assertion.*;
import com.l7tech.proxy.ConfigurationException;
import com.l7tech.proxy.datamodel.exceptions.*;
import com.l7tech.proxy.message.PolicyApplicationContext;
import com.tarari.xml.xpath.RAXContext;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.ParseException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Superclass for hardware-accelerated XPaths in the policy application code.
 * The "Accelerated" request and response xpath assertions are not "real" policy assertions; they are just
 * alternate implementations of the request and response xpath assertions that use the hardware instead.  The
 * ClientPolicyFactory instantiates the hardware-assisted versions if hardware support seems to be available.
 */
public class ClientAcceleratedXpathAssertion extends ClientXpathAssertion {
    private static final Logger logger = Logger.getLogger(ClientAcceleratedXpathAssertion.class.getName());
    protected final String expr;
    protected final ClientAssertion softwareDelegate;

    /**
     * Prepare a hardware accelerated xpath assertion.
     *
     * @param assertion   the Request or Response xpath assertion containing the xpath expression to use.  Mustn't be null.
     * @param isRequest  true if this applies to the Request message; false if it applies to the response.
     * @param softwareDelegate a ServerAssertion to which checkRequest() should be delegated if hardware acceleration can't be performed.
     */
    protected ClientAcceleratedXpathAssertion(XpathBasedAssertion assertion, boolean isRequest, ClientAssertion softwareDelegate) {
        super(assertion, isRequest);
        if (!(assertion instanceof RequestXpathAssertion) &&
            !(assertion instanceof ResponseXpathAssertion))
                throw new IllegalArgumentException(); // can't happen
        this.softwareDelegate = softwareDelegate;
        final XpathExpression xpathExpression = assertion.getXpathExpression();
        Map nsmap = xpathExpression.getNamespaces();
        String expr = xpathExpression.getExpression();
        try {
            // Convert this Xpath into tarari format
            expr = TarariXpathConverter.convertToTarariXpath(nsmap, expr);

            // Register this Xpath with the tarari hardware
            GlobalTarariContext tarariContext = TarariLoader.getGlobalContext();
            if (tarariContext != null)
                tarariContext.add(expr);
        } catch (InvalidXpathException e) {
            logger.log(Level.WARNING, "Assertion will always fail: Invalid Xpath expression: " + expr, e);
            expr = null;
        } catch (ParseException e) {
            logger.log(Level.WARNING, "Assertion will always fail: Invalid Xpath expression: " + expr, e);
            expr = null;
        }
        this.expr = expr;
    }

    public AssertionStatus decorateRequest(PolicyApplicationContext context) throws BadCredentialsException, OperationCanceledException, GeneralSecurityException, ClientCertificateException, IOException, SAXException, KeyStoreCorruptException, HttpChallengeRequiredException, PolicyRetryableException, PolicyAssertionException, InvalidDocumentFormatException, ConfigurationException
    {
        if (!isRequest)
            return AssertionStatus.NONE; // No action required during request decoration stage

        final AssertionStatus result = checkMatch(context.getRequest());
        if (result == null)
            return softwareDelegate.decorateRequest(context);
        return result;
    }

    public AssertionStatus unDecorateReply(PolicyApplicationContext context) throws BadCredentialsException, OperationCanceledException, GeneralSecurityException, IOException, SAXException, ResponseValidationException, KeyStoreCorruptException, PolicyAssertionException, InvalidDocumentFormatException
    {
        final AssertionStatus result;
        if (isRequest)
            result = checkMatch(context.getRequest()); // take same policy branches during undecorate stage as we took during decorate stage
        else
            result = checkMatch(context.getResponse());
        if (result == null)
            return softwareDelegate.unDecorateReply(context);
        return result;
    }

    private AssertionStatus checkMatch(Message mess) throws IOException
    {
        if (expr == null ) {
            logger.log(Level.WARNING, "XPath pattern is null or empty; assertion therefore fails.");
            return AssertionStatus.SERVER_ERROR;
        }

        GlobalTarariContext tarariContext = TarariLoader.getGlobalContext();
        if (tarariContext == null) {
            logger.log(Level.INFO, "Hardware acceleration not available; falling back to software xpath processing.");
            return null;
        }

        int index = tarariContext.getIndex(expr);
        if (index < 1) {
            logger.log(Level.INFO, "Hardware acceleration not supported for this xpath expression; falling back to software xpath processing.");
            return null;
        }

        TarariKnob tknob = null;
        try {
            // Ensure Tarari context is attached, if possible
            // TODO need a better way to attach this
            mess.isSoap();
            tknob = (TarariKnob) mess.getKnob(TarariKnob.class);
            if (tknob == null) {
                logger.log(Level.WARNING, "This message has no hardware acceleration context; falling back to software xpath processing.");
                return null;
            }

            TarariMessageContext tmc = tknob.getContext();
            TarariMessageContextImpl tmContext = (TarariMessageContextImpl)tmc;
            if (tmContext == null) {
                logger.log(Level.WARNING, "This message has no hardware acceleration context; falling back to software xpath processing.");
                return null;
            }

            RAXContext raxContext = tmContext.getRaxContext();
            int numMatches = raxContext.getCount(index);
            if (numMatches > 0) {
                final String r = isRequest ? "request" : "response";
                logger.log(Level.FINE, "XPath pattern matched " + r + "; assertion therefore succeeds.");
                return AssertionStatus.NONE;
            } else {
                return AssertionStatus.FALSIFIED;
            }
        } catch (SAXException e) {
            final String r = isRequest ? "Request" : "Response";
            logger.log(Level.WARNING, r + " not XML; cannot evaluate XPath expression");
            return AssertionStatus.FAILED;
        } catch (NoSuchPartException e) {
            logger.log(Level.WARNING, "The required attachment " + e.getWhatWasMissing() + "was not found in the request", e);
            return AssertionStatus.FAILED;
        }
    }

    protected void finalize() throws Throwable {
        if (expr != null) {
            // Decrement the reference count for this Xpath with the Tarari hardware
            GlobalTarariContext tarariContext = TarariLoader.getGlobalContext();
            if (tarariContext != null)
                tarariContext.remove(expr);
        }
        super.finalize();
    }
}
