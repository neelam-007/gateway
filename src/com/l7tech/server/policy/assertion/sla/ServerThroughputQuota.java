/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Apr 1, 2005<br/>
 */
package com.l7tech.server.policy.assertion.sla;

import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.sla.CounterCache;
import com.l7tech.server.sla.CounterIDManager;
import com.l7tech.server.sla.CounterManager;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.sla.ThroughputQuota;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.common.xml.SoapFaultDetail;
import com.l7tech.common.xml.SoapFaultDetailImpl;
import com.l7tech.common.util.SoapFaultUtils;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.audit.AssertionMessages;

import java.io.IOException;
import java.util.logging.Logger;

import org.springframework.context.ApplicationContext;

/**
 * Server side implementation of the ThroughputQuota assertion.
 *
 * @author flascelles@layer7-tech.com
 */
public class ServerThroughputQuota implements ServerAssertion {
    private ThroughputQuota assertion;
    private final Logger logger = Logger.getLogger(getClass().getName());
    private final Auditor auditor;
    private ApplicationContext  applicationContext;
    private final String[] TIME_UNITS = {"second", "hour", "day", "month"};
    private CounterManager counter;
    private CounterIDManager counterIDManager;

    public ServerThroughputQuota(ThroughputQuota assertion, ApplicationContext ctx) {
        this.assertion = assertion;
        this.applicationContext = ctx;
        auditor = new Auditor(this, applicationContext, logger);
        counter = (CounterManager)applicationContext.getBean("counterCache");
        // counter = (CounterManager)applicationContext.getBean("counterManager");
        counterIDManager = (CounterIDManager)applicationContext.getBean("counterIDManager");
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        switch (assertion.getCounterStrategy()) {
            case ThroughputQuota.ALWAYS_INCREMENT:
                return doIncrementAlways(context);
            case ThroughputQuota.INCREMENT_ON_SUCCESS:
                return doIncrementOnSuccess(context);
            case ThroughputQuota.DECREMENT:
                return doDecrement(context);
        }
        // not supposed to happen
        throw new PolicyAssertionException("This assertion is not configured properly. " +
                                           "Unsupported counterStrategy: " + assertion.getCounterStrategy());
    }

    private AssertionStatus doIncrementOnSuccess(PolicyEnforcementContext context) throws IOException {
        boolean requiresIncrement = !alreadyIncrementedInThisContext(context);
        long counterid = getCounterId(context);
        long now = System.currentTimeMillis();
        long val = -1;
        if (requiresIncrement) {
            try {
                val = counter.incrementOnlyWithinLimitAndReturnValue(counterid,
                                                                     now,
                                                                     assertion.getTimeUnit(),
                                                                     assertion.getQuota());
                // no need to check the limit because the preceeding call would throw if limit was exceeded
                logger.finest("Value " + val + " still within quota " + assertion.getQuota());
                return AssertionStatus.NONE;
            } catch (CounterCache.LimitAlreadyReachedException e) {
                String msg = "throughput quota limit is already reached.";
                logger.info(msg);
                auditor.logAndAudit(AssertionMessages.THROUGHPUT_QUOTA_ALREADY_MET,
                                    new String[] {assertion.getCounterName()});
                SoapFaultDetail sfd = new SoapFaultDetailImpl(SoapFaultUtils.FC_SERVER, msg, null);
                context.setFaultDetail(sfd);
                return AssertionStatus.FALSIFIED;
            } finally {
                // no sync issue here: this flag array belongs to the context which lives inside one thread only
                context.getIncrementedCounters().add(assertion.getCounterName());
            }
        } else {
            val = counter.getCounterValue(counterid, assertion.getTimeUnit());
            if (val <= assertion.getQuota()) {
                logger.fine("the quota was not exceeded. " + val + " smaller than " + assertion.getQuota());
                return AssertionStatus.NONE;
            } else {
                String limit = "max " + assertion.getQuota() + " per " + TIME_UNITS[assertion.getTimeUnit()-1];
                String msg = "the quota " + assertion.getCounterName() + " [" + limit +
                             "] was exceeded " + "(current value is " + val + ")";
                auditor.logAndAudit(AssertionMessages.THROUGHPUT_QUOTA_EXCEEDED,
                                    new String[] {assertion.getCounterName(), limit, Long.toString(val)});
                logger.info(msg);
                SoapFaultDetail sfd = new SoapFaultDetailImpl(SoapFaultUtils.FC_SERVER, msg, null);
                context.setFaultDetail(sfd);
                return AssertionStatus.FALSIFIED;
            }
        }
    }

    private AssertionStatus doDecrement(PolicyEnforcementContext context) throws IOException {
        if (alreadyIncrementedInThisContext(context)) {
            long counterid = getCounterId(context);
            counter.decrement(counterid);
            logger.fine("counter decremented " + counterid);
        } else {
            logger.info("assertion was asked to decrement a counter but this same " +
                        "counter was not incremented in this countext.");
            // one could argue that this should result in error
        }
        return AssertionStatus.NONE;
    }

    private AssertionStatus doIncrementAlways(PolicyEnforcementContext context) throws IOException {
        boolean requiresIncrement = !alreadyIncrementedInThisContext(context);
        long counterid = getCounterId(context);
        long now = System.currentTimeMillis();
        long val = -1;
        if (requiresIncrement) {
            val = counter.incrementAndReturnValue(counterid, now, assertion.getTimeUnit());
            // no sync issue here: this flag array belongs to the context which lives inside one thread only
            context.getIncrementedCounters().add(assertion.getCounterName());
        } else {
            val = counter.getCounterValue(counterid, assertion.getTimeUnit());
        }
        if (val <= assertion.getQuota()) {
            logger.fine("the quota was not exceeded. " + val + " smaller than " + assertion.getQuota());
            return AssertionStatus.NONE;
        } else {
            String limit = "max " + assertion.getQuota() + " per " + TIME_UNITS[assertion.getTimeUnit()-1];
            String msg = "the quota " + assertion.getCounterName() + " [" + limit +
                         "] was exceeded " + "(current value is " + val + ")";
            auditor.logAndAudit(AssertionMessages.THROUGHPUT_QUOTA_EXCEEDED,
                                new String[] {assertion.getCounterName(), limit, Long.toString(val)});
            logger.info(msg);
            SoapFaultDetail sfd = new SoapFaultDetailImpl(SoapFaultUtils.FC_SERVER, msg, null);
            context.setFaultDetail(sfd);
            return AssertionStatus.FALSIFIED;
        }
    }

    private long getCounterId(PolicyEnforcementContext context) throws IOException {
        User user = null;
        if (assertion.isGlobal()) {
            logger.finest("checking counter against null user");
        } else {
            user = context.getAuthenticatedUser();
            logger.finest("checking counter against user " + user);
        }

        long counterid = 0;
        try {
            counterid = counterIDManager.getCounterId(assertion.getCounterName(), user);
        } catch (ObjectModelException e) {
            // should not happen
            throw new IOException("could not get counter id " + e.getMessage());
        }
        logger.finest("Counter id is " + counterid);
        return counterid;
    }

    private boolean alreadyIncrementedInThisContext(PolicyEnforcementContext context) {
        if (context.getIncrementedCounters().contains(assertion.getCounterName())) {
            return true;
        }
        return false;
    }
}
