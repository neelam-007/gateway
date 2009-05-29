/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Apr 1, 2005<br/>
 */
package com.l7tech.server.policy.assertion.sla;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.server.audit.Auditor;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.sla.ThroughputQuota;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.sla.CounterManager;
import com.l7tech.server.sla.CounterIDManager;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Server side implementation of the ThroughputQuota assertion.
 *
 * @author flascelles@layer7-tech.com
 */
public class ServerThroughputQuota extends AbstractServerAssertion<ThroughputQuota> {
    private ThroughputQuota assertion;
    private final Logger logger = Logger.getLogger(getClass().getName());
    private final Auditor auditor;
    private ApplicationContext  applicationContext;
    private final String[] TIME_UNITS = {"second", "minute", "hour", "day", "month"};
    private final String[] varsUsed;
    private final String idVariable;
    private final String valueVariable;
    private final String periodVariable;
    private final String userVariable;
    private final String maxVariable;

    public ServerThroughputQuota(ThroughputQuota assertion, ApplicationContext ctx) {
        super(assertion);
        this.assertion = assertion;
        this.applicationContext = ctx;
        auditor = new Auditor(this, applicationContext, logger);
        varsUsed = assertion.getVariablesUsed();
        idVariable = assertion.idVariable();
        valueVariable = assertion.valueVariable();
        periodVariable = assertion.periodVariable();
        userVariable = assertion.userVariable();
        maxVariable = assertion.maxVariable();
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        context.setVariable(idVariable, assertion.getCounterName());
        context.setVariable(periodVariable, TIME_UNITS[assertion.getTimeUnit() - 1]);
        User user = context.getDefaultAuthenticationContext().getLastAuthenticatedUser();
        if (user != null) {
            context.setVariable(userVariable, user.getName());
        } else {
            context.setVariable(userVariable, "");            
        }
        context.setVariable(maxVariable, String.valueOf(assertion.getQuota()));
        switch (assertion.getCounterStrategy()) {
            case ThroughputQuota.ALWAYS_INCREMENT:
                return doIncrementAlways(context);
            case ThroughputQuota.INCREMENT_ON_SUCCESS:
                return doIncrementOnSuccess(context);
            case ThroughputQuota.DECREMENT:
                return doDecrement(context);
        }
        // not supposed to happen
        throw new PolicyAssertionException(assertion, "This assertion is not configured properly. " +
                                           "Unsupported counterStrategy: " + assertion.getCounterStrategy());
    }

    private AssertionStatus doIncrementOnSuccess(PolicyEnforcementContext context) throws IOException {
        boolean requiresIncrement = !alreadyIncrementedInThisContext(context);
        long counterid = getCounterId(context);
        long now = System.currentTimeMillis();
        long val;
        CounterManager counter = (CounterManager)applicationContext.getBean("counterManager");
        if (requiresIncrement) {
            try {
                val = counter.incrementOnlyWithinLimitAndReturnValue(counterid,
                                                                     now,
                                                                     assertion.getTimeUnit(),
                                                                     assertion.getQuota());
                this.setValue(context, val);
                // no need to check the limit because the preceeding call would throw if limit was exceeded
                logger.finest("Value " + val + " still within quota " + assertion.getQuota());
                return AssertionStatus.NONE;
            } catch (CounterManager.LimitAlreadyReachedException e) {
                String msg = "throughput quota limit is already reached.";
                logger.info(msg);
                auditor.logAndAudit(AssertionMessages.THROUGHPUT_QUOTA_ALREADY_MET, assertion.getCounterName());
                return AssertionStatus.FALSIFIED;
            } finally {
                // no sync issue here: this flag array belongs to the context which lives inside one thread only
                // no need to resolve external variables here
                context.getIncrementedCounters().add(assertion.getCounterName());
            }
        } else {
            val = counter.getCounterValue(counterid, assertion.getTimeUnit());
            this.setValue(context, val);
            if (val <= assertion.getQuota()) {
                logger.fine("the quota was not exceeded. " + val + " smaller than " + assertion.getQuota());
                return AssertionStatus.NONE;
            } else {
                String limit = "max " + assertion.getQuota() + " per " + TIME_UNITS[assertion.getTimeUnit()-1];
                String msg = "the quota " + assertion.getCounterName() + " [" + limit +
                             "] was exceeded " + "(current value is " + val + ")";
                auditor.logAndAudit(AssertionMessages.THROUGHPUT_QUOTA_EXCEEDED, assertion.getCounterName(), limit, Long.toString(val));
                logger.info(msg);
                return AssertionStatus.FALSIFIED;
            }
        }
    }

    private AssertionStatus doDecrement(PolicyEnforcementContext context) throws IOException {
        if (alreadyIncrementedInThisContext(context)) {
            long counterid = getCounterId(context);
            CounterManager counter = (CounterManager)applicationContext.getBean("counterManager");
            counter.decrement(counterid);
            logger.fine("counter decremented " + counterid);
            forgetIncrementInThisContext(context); // to prevent double decrement and enable re-increment
        } else {
            logger.info("assertion was asked to decrement a counter but the " +
                        "counter was not previously recorded as incremented in this context.");
            // one could argue that this should result in error
        }
        this.setValue(context, -1);
        return AssertionStatus.NONE;
    }

    private AssertionStatus doIncrementAlways(PolicyEnforcementContext context) throws IOException {
        boolean requiresIncrement = !alreadyIncrementedInThisContext(context);
        long counterid = getCounterId(context);
        long now = System.currentTimeMillis();
        long val;
        CounterManager counter = (CounterManager)applicationContext.getBean("counterManager");
        if (requiresIncrement) {
            val = counter.incrementAndReturnValue(counterid, now, assertion.getTimeUnit());
            // no sync issue here: this flag array belongs to the context which lives inside one thread only
            context.getIncrementedCounters().add(assertion.getCounterName());
        } else {
            val = counter.getCounterValue(counterid, assertion.getTimeUnit());
        }
        this.setValue(context, val);
        if (val <= assertion.getQuota()) {
            logger.fine("the quota was not exceeded. " + val + " smaller than " + assertion.getQuota());
            return AssertionStatus.NONE;
        } else {
            String limit = "max " + assertion.getQuota() + " per " + TIME_UNITS[assertion.getTimeUnit()-1];
            auditor.logAndAudit(AssertionMessages.THROUGHPUT_QUOTA_EXCEEDED, assertion.getCounterName(), limit, Long.toString(val));
            return AssertionStatus.FALSIFIED;
        }
    }

    private long getCounterId(PolicyEnforcementContext context) throws IOException {
        User user = null;
        if (assertion.isGlobal()) {
            logger.finest("checking counter against null user");
        } else {
            user = context.getDefaultAuthenticationContext().getLastAuthenticatedUser();
            logger.finest("checking counter against user " + user);
        }

        long counterid = 0;
        CounterIDManager counterIDManager = (CounterIDManager)applicationContext.getBean("counterIDManager");
        String resolvedCounterName = assertion.getCounterName();
        if (varsUsed.length > 0) {
            resolvedCounterName = ExpandVariables.process(resolvedCounterName, context.getVariableMap(varsUsed, auditor), auditor);
        }
        try {
            counterid = counterIDManager.getCounterId(resolvedCounterName, user);
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

    private void forgetIncrementInThisContext(PolicyEnforcementContext context) {
        int res = context.getIncrementedCounters().indexOf(assertion.getCounterName());
        if (res >= 0) {
            context.getIncrementedCounters().remove(res);
        } else {
            logger.fine("the counter was not already incremented in this context");
        }
    }

    private void setValue(PolicyEnforcementContext context, long value) {
        context.setVariable(valueVariable, String.valueOf(value));
    }
}
