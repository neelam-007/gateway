/**
 * $Id$
 */
package com.l7tech.server.policy.assertion;

import com.l7tech.common.audit.Auditor;
import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.message.TarariKnob;
import com.l7tech.common.xml.InvalidXpathException;
import com.l7tech.common.xml.TarariLoader;
import com.l7tech.common.xml.tarari.ServerTarariContext;
import com.l7tech.common.xml.tarari.TarariMessageContext;
import com.l7tech.common.xml.tarari.TarariMessageContextImpl;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RequestAcceleratedXpathAssertion;
import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.server.audit.AuditContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.tarari.xml.xpath.RAXContext;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerRequestAcceleratedXpathAssertion implements ServerAssertion {
    private static final Logger logger = Logger.getLogger(ServerRequestAcceleratedXpathAssertion.class.getName());
    private final ApplicationContext applicationContext;
    private final String expr;
    private final ServerRequestXpathAssertion softwareDelegate;

    public ServerRequestAcceleratedXpathAssertion(RequestAcceleratedXpathAssertion assertion, ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        this.softwareDelegate = new ServerRequestXpathAssertion(assertion);
        String expr = assertion.getXpathExpression().getExpression();
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

        TarariKnob tknob = (TarariKnob) context.getRequest().getKnob(TarariKnob.class);
        if (tknob == null) {
            auditor.logAndAudit(AssertionMessages.ACCEL_XPATH_NO_CONTEXT);
            return softwareDelegate.checkRequest(context);
        }

        TarariMessageContext tmc = tknob.getContext();
        TarariMessageContextImpl tmContext;
        if (tmc instanceof TarariMessageContextImpl) {
            tmContext = (TarariMessageContextImpl)tmc;
        } else {
            throw new PolicyAssertionException("Request had a TarariKnob but TarariMessageContext was of the wrong type");
        }

        RAXContext raxContext = tmContext.getRaxContext();
        int numMatches = raxContext.getCount(index);
        if (numMatches > 0) {
            auditor.logAndAudit(AssertionMessages.XPATH_SUCCEED_REQUEST);
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
    }
}
