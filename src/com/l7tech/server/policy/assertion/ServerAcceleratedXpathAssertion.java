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
import com.l7tech.common.xml.InvalidXpathException;
import com.l7tech.common.xml.TarariLoader;
import com.l7tech.common.xml.tarari.ServerTarariContext;
import com.l7tech.common.xml.tarari.TarariMessageContext;
import com.l7tech.common.xml.tarari.TarariMessageContextImpl;
import com.l7tech.policy.assertion.*;
import com.l7tech.server.audit.AuditContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.tarari.xml.xpath.RAXContext;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Common code used by both {@link ServerRequestAcceleratedXpathAssertion} and {@link ServerResponseAcceleratedXpathAssertion}.
 */
public abstract class ServerAcceleratedXpathAssertion implements ServerAssertion {
    protected static final Logger logger = Logger.getLogger(ServerAcceleratedXpathAssertion.class.getName());

    protected final ApplicationContext applicationContext;
    protected final String expr;
    protected final boolean isReq;
    protected final ServerAssertion softwareDelegate;

    /**
     * Prepare a hardware accelerated xpath assertion.
     *
     * @param assertion   the Request or Response xpath assertion containing the xpath expression to use.  Mustn't be null.
     * @param applicationContext  the application context from which to get the Tarari server context.  Mustn't be null.
     * @param softwareDelegate a ServerAssertion to which checkRequest() should be delegated if hardware acceleration can't be performed.
     */
    protected ServerAcceleratedXpathAssertion(XpathBasedAssertion assertion, ApplicationContext applicationContext, ServerAssertion softwareDelegate) {
        if (!(assertion instanceof RequestAcceleratedXpathAssertion) &&
            !(assertion instanceof ResponseAcceleratedXpathAssertion))
                throw new IllegalArgumentException(); // can't happen
        this.applicationContext = applicationContext;
        this.softwareDelegate = softwareDelegate;
        String expr = assertion.getXpathExpression().getExpression();
        isReq = assertion instanceof RequestXpathAssertion;
        try {
            // Register this Xpath with the tarari hardware
            ServerTarariContext tarariContext = TarariLoader.getServerContext();
            if (tarariContext != null)
                tarariContext.add(expr);
        } catch (InvalidXpathException e) {
            logger.log(Level.WARNING, "Assertion will always fail: Invalid Xpath expression: " + expr, e);
            expr = null;
        }
        this.expr = expr;
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        Auditor auditor = new Auditor((AuditContext) applicationContext.getBean("auditContext"), logger);
        if (expr == null ) {
            auditor.logAndAudit(AssertionMessages.XPATH_PATTERN_INVALID);
            return AssertionStatus.SERVER_ERROR;
        }

        ServerTarariContext tarariContext = TarariLoader.getServerContext();
        if (tarariContext == null) {
            auditor.logAndAudit(AssertionMessages.ACCEL_XPATH_NO_HARDWARE);
            return softwareDelegate.checkRequest(context);
        }

        int index = tarariContext.getIndex(expr);
        if (index < 1) {
            auditor.logAndAudit(AssertionMessages.ACCEL_XPATH_UNSUPPORTED_PATTERN);
            return softwareDelegate.checkRequest(context);
        }

        TarariKnob tknob = null;
        Message mess = isReq ? context.getRequest() : context.getResponse();
        try {
            mess.isSoap();
        } catch (SAXException e) {
            auditor.logAndAudit(isReq ? AssertionMessages.XPATH_REQUEST_NOT_XML : AssertionMessages.XPATH_RESPONSE_NOT_XML);
            return AssertionStatus.FAILED;
        }
        tknob = (TarariKnob) mess.getKnob(TarariKnob.class);
        if (tknob == null) {
            auditor.logAndAudit(AssertionMessages.ACCEL_XPATH_NO_CONTEXT);
            return softwareDelegate.checkRequest(context);
        }

        TarariMessageContext tmc = tknob.getContext();
        TarariMessageContextImpl tmContext = (TarariMessageContextImpl)tmc;

        RAXContext raxContext = tmContext.getRaxContext();
        int numMatches = raxContext.getCount(index);
        if (numMatches > 0) {
            auditor.logAndAudit(isReq ? AssertionMessages.XPATH_SUCCEED_REQUEST : AssertionMessages.XPATH_SUCCEED_RESPONSE);
            return AssertionStatus.NONE;
        } else {
            return AssertionStatus.FALSIFIED;
        }
    }

    protected void finalize() throws Throwable {
        if (expr != null) {
            // Decrement the reference count for this Xpath with the Tarari hardware
            ServerTarariContext tarariContext = TarariLoader.getServerContext();
            if (tarariContext != null)
                tarariContext.remove(expr);
        }
        super.finalize();
    }
}
