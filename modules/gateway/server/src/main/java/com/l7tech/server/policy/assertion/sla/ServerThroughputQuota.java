/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Apr 1, 2005<br/>
 */
package com.l7tech.server.policy.assertion.sla;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.identity.User;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.sla.ThroughputQuota;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.sla.CounterManager;
import com.l7tech.util.Config;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.ApplicationContext;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Map;

/**
 * Server side implementation of the ThroughputQuota assertion.
 *
 * @author flascelles@layer7-tech.com
 */
public class ServerThroughputQuota extends AbstractServerAssertion<ThroughputQuota> {
    private ApplicationContext  applicationContext;
    private final String[] TIME_UNITS = {"second", "minute", "hour", "day", "month"};
    private final String[] varsUsed;
    private final String idVariable;
    private final String valueVariable;
    private final String periodVariable;
    private final String userVariable;
    private final String maxVariable;
    private final boolean synchronous;
    private final boolean readSynchronous;

    private final int DEFAULT_INCREMENT_DECREMENT_VALUE = 1;

    @Inject
    private Config config;

    public ServerThroughputQuota(ThroughputQuota assertion, ApplicationContext ctx) {
        super(assertion);
        this.applicationContext = ctx;
        varsUsed = assertion.getVariablesUsed();
        idVariable = assertion.idVariable();
        valueVariable = assertion.valueVariable();
        periodVariable = assertion.periodVariable();
        userVariable = assertion.userVariable();
        maxVariable = assertion.maxVariable();
        synchronous = assertion.isSynchronous();
        readSynchronous = assertion.isReadSynchronous();
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

        int incrementOrDecrementBy = getValueToIncrementOrDecrement(context);

        switch (assertion.getCounterStrategy()) {
            case ThroughputQuota.ALWAYS_INCREMENT:
                return doIncrementAlways(context,quota,incrementOrDecrementBy);
            case ThroughputQuota.INCREMENT_ON_SUCCESS:
                return doIncrementOnSuccess(context,quota,incrementOrDecrementBy);
            case ThroughputQuota.DECREMENT:
                return doDecrement(context,incrementOrDecrementBy);
            case ThroughputQuota.RESET:
                return doReset(context);
        }

        // not supposed to happen
        throw new PolicyAssertionException(assertion, "This assertion is not configured properly. " +
                                           "Unsupported counterStrategy: " + assertion.getCounterStrategy());
    }

    private int getValueToIncrementOrDecrement(PolicyEnforcementContext context) {
        int incrementOrDecrementBy = DEFAULT_INCREMENT_DECREMENT_VALUE;
        if(StringUtils.isNotBlank(assertion.getByValue())) {
            Map<String, ?> varMap = context.getVariableMap(varsUsed, getAudit());
            String value = ExpandVariables.process(assertion.getByValue(), varMap, getAudit());
            try {

                if(value == null) {
                    logAndAudit(AssertionMessages.NO_SUCH_VARIABLE, assertion.getByValue());
                    throw new AssertionStatusException( AssertionStatus.FAILED );
                }

                //if negative then policy assertion exception thrown
                incrementOrDecrementBy = Integer.parseInt(value);
                if(incrementOrDecrementBy < 0) {
                    logAndAudit(AssertionMessages.THROUGHPUT_QUOTA_INVALID_NEGATIVE_VALUE, assertion.getByValue());
                    throw new AssertionStatusException( AssertionStatus.FAILED );
                }

            } catch(NumberFormatException e) {
                logAndAudit(AssertionMessages.VARIABLE_INVALID_VALUE, assertion.getByValue(), "Integer");
                throw new AssertionStatusException( AssertionStatus.FAILED );
            }
        }
        return incrementOrDecrementBy;
    }

    private AssertionStatus doReset(PolicyEnforcementContext context) throws IOException {
        final CounterManager counterManager = applicationContext.getBean("counterManager", CounterManager.class);
        counterManager.reset(getCounterName(context));
        setValue(context, 0);

        return AssertionStatus.NONE;
    }

    private AssertionStatus doIncrementOnSuccess(final PolicyEnforcementContext context,
                                                 final long quota, int incrementValue) throws IOException {
        boolean requiresIncrement = !alreadyIncrementedInThisContext(context);
        long now = System.currentTimeMillis();
        long val;

        final CounterManager counterManager = applicationContext.getBean("counterManager", CounterManager.class);
        final String counterName = getCounterName(context);
        if (requiresIncrement) {
            try {
                val = counterManager.incrementOnlyWithinLimitAndReturnValue(synchronous, readSynchronous,
                                                                     counterName,
                                                                     now,
                                                                     assertion.getTimeUnit(),
                                                                     quota, incrementValue);
                this.setValue(context, val);
                // no need to check the limit because the preceeding call would throw if limit was exceeded
                logger.finest("Value " + val + " still within quota " + quota);
                return AssertionStatus.NONE;
            } catch (CounterManager.LimitAlreadyReachedException e) {
                String msg = "throughput quota limit is already reached.";
                logger.info(msg);
                logAndAudit(AssertionMessages.THROUGHPUT_QUOTA_ALREADY_MET, counterName);
                if(assertion.isLogOnly()){
                    this.setValue(context, counterManager.getCounterValue(readSynchronous, counterName, assertion.getTimeUnit()));
                    return AssertionStatus.NONE;
                }else{
                    return AssertionStatus.FALSIFIED;
                }
            } finally {
                // no sync issue here: this flag array belongs to the context which lives inside one thread only
                context.getIncrementedCounters().add(counterName);
            }
        } else {
            val = counterManager.getCounterValue(readSynchronous, counterName, assertion.getTimeUnit());
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

    private AssertionStatus doDecrement(PolicyEnforcementContext context, int decrementValue) throws IOException {
        long now = System.currentTimeMillis();
        final CounterManager counterManager = applicationContext.getBean("counterManager", CounterManager.class);

        if (alreadyIncrementedInThisContext(context)) {
            final String counterName = getCounterName(context);
            counterManager.decrement(synchronous, counterName, decrementValue, now);
            logger.fine("counter decremented " + counterName);
            forgetIncrementInThisContext(context); // to prevent double decrement and enable re-increment
        } else {
            logger.info("assertion was asked to decrement a counter but the " +
                        "counter was not previously recorded as incremented in this context.");
            // one could argue that this should result in error
        }
        this.setValue(context, -decrementValue);
        return AssertionStatus.NONE;
    }

    private AssertionStatus doIncrementAlways(final PolicyEnforcementContext context,
                                              final long quota, int incrementValue) throws IOException {
        final CounterManager counterManager = applicationContext.getBean("counterManager", CounterManager.class);

        boolean requiresIncrement = !alreadyIncrementedInThisContext(context);
        long now = System.currentTimeMillis();
        long val;
        final String counterName = getCounterName(context);
        if (requiresIncrement) {
            val = counterManager.incrementAndReturnValue(readSynchronous, synchronous, counterName, now, assertion.getTimeUnit(), incrementValue);
            // no sync issue here: this flag array belongs to the context which lives inside one thread only
            context.getIncrementedCounters().add(counterName);
        } else {
            val = counterManager.getCounterValue(readSynchronous, counterName, assertion.getTimeUnit());
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

        final CounterManager counterManager = (CounterManager)applicationContext.getBean("counterManager");
        try {
            counterManager.ensureCounterExists(resolvedCounterName);
        } catch (ObjectModelException e) {
            // should not happen
            throw new IOException("Could not get counter, " + resolvedCounterName + ": " + e.getMessage());
        }

        return resolvedCounterName;
    }

    private boolean alreadyIncrementedInThisContext(PolicyEnforcementContext context) throws IOException {
        return context.getIncrementedCounters().contains(getCounterName(context));
    }

    private void forgetIncrementInThisContext(PolicyEnforcementContext context) throws IOException {
        int res = context.getIncrementedCounters().indexOf(getCounterName(context));
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

            if (config.getBooleanProperty(ServerConfigParams.PARAM_THROUGHPUTQUOTA_ENFORCE_MAX_QUOTA, false)) {
                final long maxQuotaValue = config.getLongProperty(ServerConfigParams.PARAM_THROUGHPUTQUOTA_MAX_THROUGHPUT_QUOTA, ThroughputQuota.MAX_THROUGHPUT_QUOTA);
                if (longValue > maxQuotaValue) {
                    // configuration error
                    logAndAudit(AssertionMessages.THROUGHPUT_QUOTA_INVALID_MAX_QUOTA, String.valueOf(longValue), String.valueOf(maxQuotaValue));
                    throw new AssertionStatusException( AssertionStatus.FAILED );
                }
            }
        } catch ( NumberFormatException e ) {
            logAndAudit(AssertionMessages.VARIABLE_INVALID_VALUE, assertion.getQuota(), "Long");
            throw new AssertionStatusException( AssertionStatus.FAILED );
        }

        return longValue;
    }
}