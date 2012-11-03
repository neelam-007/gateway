package com.l7tech.external.assertions.hatq.server;

import com.l7tech.external.assertions.hatq.HalfAsyncThroughputQuotaAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.identity.User;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.sla.ThroughputQuota;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.server.policy.variable.ExpandVariables;
import org.hibernate.SessionFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Server side implementation of the HalfAsyncThroughputQuotaAssertion.
 *
 * @see com.l7tech.external.assertions.hatq.HalfAsyncThroughputQuotaAssertion
 */
public class ServerHalfAsyncThroughputQuotaAssertion extends AbstractServerAssertion<HalfAsyncThroughputQuotaAssertion> {
    private ApplicationContext applicationContext;
    private final String[] TIME_UNITS = {"second", "minute", "hour", "day", "month"};
    private final String[] varsUsed;
    private final String idVariable;
    private final String valueVariable;
    private final String periodVariable;
    private final String userVariable;
    private final String maxVariable;

    public ServerHalfAsyncThroughputQuotaAssertion(HalfAsyncThroughputQuotaAssertion assertion, ApplicationContext ctx) {
        super(assertion);
        this.applicationContext = ctx;
        varsUsed = assertion.getVariablesUsed();
        idVariable = assertion.idVariable();
        valueVariable = assertion.valueVariable();
        periodVariable = assertion.periodVariable();
        userVariable = assertion.userVariable();
        maxVariable = assertion.maxVariable();
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        context.setVariable(idVariable, getCounterName(context));
        context.setVariable(periodVariable, TIME_UNITS[assertion.getTimeUnit() - 1]);
        final User user = context.getDefaultAuthenticationContext().getLastAuthenticatedUser();
        if (user != null) {
            context.setVariable(userVariable, user.getName());
        } else {
            context.setVariable(userVariable, "");
        }
        final long quota = getQuota(context);
        context.setVariable(maxVariable, String.valueOf(quota));
        switch (assertion.getCounterStrategy()) {
            case ThroughputQuota.ALWAYS_INCREMENT:
                return doIncrementAlways(context,quota);
            case ThroughputQuota.INCREMENT_ON_SUCCESS:
                return doIncrementOnSuccess(context,quota);
            case ThroughputQuota.DECREMENT:
                return doDecrement(context);
        }

        // not supposed to happen
        throw new PolicyAssertionException(assertion, "This assertion is not configured properly. " +
            "Unsupported counterStrategy: " + assertion.getCounterStrategy());
    }

    private AssertionStatus doIncrementOnSuccess( final PolicyEnforcementContext context,
                                                  final long quota ) throws IOException {
        final String counterName = getCounterName(context);
        boolean requiresIncrement = !alreadyIncrementedInThisContext(context, counterName);
        long now = System.currentTimeMillis();
        long val;

        final HalfAsyncCounterManager counterManager = getCounterManager(applicationContext);

        if (requiresIncrement) {
            try {
                val = counterManager.checkValueAndRequestAsyncIncrementIfWithinLimit(counterName,
                    now,
                    assertion.getTimeUnit(),
                    quota);
                this.setValue(context, val);
                // no need to check the limit because the preceeding call would throw if limit was exceeded
                logger.finest("Value " + val + " still within quota " + quota);
                return AssertionStatus.NONE;
            } catch (LimitAlreadyReachedException e) {
                logAndAudit(AssertionMessages.THROUGHPUT_QUOTA_ALREADY_MET, counterName);
                if(assertion.isLogOnly()){
                    this.setValue(context, counterManager.getCounterValue(counterName, assertion.getTimeUnit()));
                    return AssertionStatus.NONE;
                }else{
                    return AssertionStatus.FALSIFIED;
                }
            } finally {
                // no sync issue here: this flag array belongs to the context which lives inside one thread only
                context.getIncrementedCounters().add(counterName);
            }
        } else {
            val = counterManager.getCounterValue(counterName, assertion.getTimeUnit());
            this.setValue(context, val);
            if (val <= quota) {
                logger.fine("the quota was not exceeded. " + val + " smaller than " + quota);
                return AssertionStatus.NONE;
            } else {
                String limit = "max " + quota + " per " + TIME_UNITS[assertion.getTimeUnit()-1];
                String msg = "the quota " + counterName + " [" + limit +
                    "] was exceeded " + "(current value is " + val + ")";
                logAndAudit(AssertionMessages.THROUGHPUT_QUOTA_EXCEEDED, counterName, limit, Long.toString(val));
                logger.info(msg);
                return assertion.isLogOnly() ? AssertionStatus.NONE : AssertionStatus.FALSIFIED;
            }
        }
    }

    private AssertionStatus doDecrement(PolicyEnforcementContext context) throws IOException {
        final HalfAsyncCounterManager counterManager = getCounterManager(applicationContext);

        final String counterName = getCounterName(context);
        if (alreadyIncrementedInThisContext(context, counterName)) {
            counterManager.decrement(counterName);
            logger.fine("counter decremented " + counterName);
            forgetIncrementInThisContext(context, counterName); // to prevent double decrement and enable re-increment
        } else {
            logger.info("assertion was asked to decrement a counter but the " +
                "counter was not previously recorded as incremented in this context.");
            // one could argue that this should result in error
        }
        this.setValue(context, -1);
        return AssertionStatus.NONE;
    }

    private AssertionStatus doIncrementAlways( final PolicyEnforcementContext context,
                                               final long quota ) throws IOException {
        final HalfAsyncCounterManager counterManager = getCounterManager(applicationContext);

        final String counterName = getCounterName(context);
        boolean requiresIncrement = !alreadyIncrementedInThisContext(context, counterName);
        long now = System.currentTimeMillis();
        long val;
        if (requiresIncrement) {
            val = counterManager.incrementAsyncAndReturnValue(counterName, now, assertion.getTimeUnit());
            // no sync issue here: this flag array belongs to the context which lives inside one thread only
            context.getIncrementedCounters().add(counterName);
        } else {
            val = counterManager.getCounterValue(counterName, assertion.getTimeUnit());
        }
        this.setValue(context, val);
        if (val <= quota) {
            logger.fine("the quota was not exceeded. " + val + " smaller than " + quota);
            return AssertionStatus.NONE;
        } else {
            String limit = "max " + quota + " per " + TIME_UNITS[assertion.getTimeUnit()-1];
            logAndAudit(AssertionMessages.THROUGHPUT_QUOTA_EXCEEDED, counterName, limit, Long.toString(val));
            return assertion.isLogOnly() ? AssertionStatus.NONE : AssertionStatus.FALSIFIED;
        }
    }

    public String getCounterName(final PolicyEnforcementContext context) throws IOException {
        String resolvedCounterName = assertion.getCounterName();
        if (varsUsed.length > 0) {
            resolvedCounterName = ExpandVariables.process(resolvedCounterName, context.getVariableMap(varsUsed, getAudit()), getAudit());
        }

        String errorMessage = null;
        if (resolvedCounterName == null || resolvedCounterName.trim().isEmpty()) {
            errorMessage = "The resolved Counter ID is empty.";
        } else if (resolvedCounterName.length() > 255) {
            errorMessage = "The resolved Counter ID length exceeds the maximum length, 255.";
        }
        if (errorMessage != null) {
            logAndAudit(AssertionMessages.THROUGHPUT_QUOTA_INVALID_COUNTER_ID, errorMessage);
            throw new AssertionStatusException(AssertionStatus.FALSIFIED, errorMessage);
        }

        final HalfAsyncCounterManager counterManager = getCounterManager(applicationContext);
        counterManager.ensureCounterExists(resolvedCounterName);

        return resolvedCounterName;
    }

    private boolean alreadyIncrementedInThisContext(PolicyEnforcementContext context, String counterName) throws IOException {
        return context.getIncrementedCounters().contains(counterName);
    }

    private void forgetIncrementInThisContext(PolicyEnforcementContext context, String counterName) throws IOException {
        int res = context.getIncrementedCounters().indexOf(counterName);
        if (res >= 0) {
            context.getIncrementedCounters().remove(res);
        } else {
            logger.fine("the counter was not already incremented in this context");
        }
    }

    private void setValue(PolicyEnforcementContext context, long value) {
        context.setVariable(valueVariable, String.valueOf(value));
    }

    private long getQuota( final PolicyEnforcementContext context ) {
        long longValue;

        final String quota = assertion.getQuota();
        final String[] referencedVars = Syntax.getReferencedNames(quota);

        try {
            if ( referencedVars.length > 0 ){
                final String stringValue = ExpandVariables.process(quota, context.getVariableMap(referencedVars, getAudit()), getAudit());
                longValue = Long.parseLong(stringValue);
            } else {
                longValue = Long.parseLong(quota);
            }
        } catch ( NumberFormatException e ) {
            logAndAudit(AssertionMessages.VARIABLE_INVALID_VALUE, assertion.getQuota(), "Long");
            throw new AssertionStatusException( AssertionStatus.FAILED );
        }

        return longValue;
    }


    private static final AtomicReference<HalfAsyncCounterManager> halfAsyncCounterManager = new AtomicReference<HalfAsyncCounterManager>();
    private static final Object counterManagerCreateLock = new Object();

    static HalfAsyncCounterManager getCounterManager(ApplicationContext applicationContext) {
        HalfAsyncCounterManager ret = halfAsyncCounterManager.get();
        if (ret == null) {
            synchronized (counterManagerCreateLock) {
                ret = halfAsyncCounterManager.get();
                if (ret == null) {
                    ret = createCounterManager(applicationContext);
                    halfAsyncCounterManager.set(ret);
                }
            }
        }
        return ret;
    }

    private static HalfAsyncCounterManager createCounterManager(ApplicationContext applicationContext) {
        SessionFactory sessionFactory = applicationContext.getBean("sessionFactory", SessionFactory.class);
        PlatformTransactionManager transactionManager = applicationContext.getBean("transactionManager", PlatformTransactionManager.class);

        HalfAsyncCounterManager ret = new HalfAsyncCounterManager(transactionManager);
        ret.setSessionFactory(sessionFactory);
        ret.afterPropertiesSet();
        return ret;
    }
}
