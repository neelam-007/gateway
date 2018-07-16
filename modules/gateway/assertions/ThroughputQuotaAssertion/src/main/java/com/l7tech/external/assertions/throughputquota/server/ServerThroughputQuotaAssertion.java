/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * <p>
 * User: flascell<br/>
 * Date: Apr 1, 2005<br/>
 */
package com.l7tech.external.assertions.throughputquota.server;

import com.ca.apim.gateway.extension.sharedstate.counter.CounterFieldOfInterest;
import com.ca.apim.gateway.extension.sharedstate.counter.SharedCounterProvider;
import com.ca.apim.gateway.extension.sharedstate.counter.exception.CounterLimitReachedException;
import com.l7tech.external.assertions.throughputquota.ThroughputQuotaAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.identity.User;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.extension.provider.sharedstate.SharedCounterConfigConstants;
import com.l7tech.server.extension.registry.sharedstate.SharedCounterProviderRegistry;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.Config;
import com.l7tech.util.SyspropUtil;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.ApplicationContext;

import javax.inject.Inject;
import java.io.IOException;
import java.time.Clock;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;

/**
 * Server side implementation of the ThroughputQuotaAssertion.
 */
public class ServerThroughputQuotaAssertion extends AbstractServerAssertion<ThroughputQuotaAssertion> {

    private final String[] varsUsed;
    private final String idVariable;
    private final String valueVariable;
    private final String periodVariable;
    private final String userVariable;
    private final String maxVariable;
    private final Clock clock;

    public static final String COUNTER_STORE_NAME = ThroughputQuotaAssertion.class.getName() + "-counterStore";
    private static final int DEFAULT_INCREMENT_DECREMENT_VALUE = 1;

    @Inject
    private Config config;

    private SharedCounterProviderRegistry counterProviderRegistry;

    private final Properties counterOperationProperties;

    public ServerThroughputQuotaAssertion(ThroughputQuotaAssertion assertion, ApplicationContext ctx) {
        super(assertion);
        varsUsed = assertion.getVariablesUsed();
        idVariable = assertion.idVariable();
        valueVariable = assertion.valueVariable();
        periodVariable = assertion.periodVariable();
        userVariable = assertion.userVariable();
        maxVariable = assertion.maxVariable();
        clock = Clock.systemUTC();

        counterOperationProperties = new Properties();
        counterOperationProperties.setProperty(SharedCounterConfigConstants.CounterOperations.KEY_WRITE_SYNC, String.valueOf(assertion.isSynchronous()));
        counterOperationProperties.setProperty(SharedCounterConfigConstants.CounterOperations.KEY_READ_SYNC, String.valueOf(assertion.isReadSynchronous()));

        counterProviderRegistry = ctx.getBean("sharedCounterProviderRegistry", SharedCounterProviderRegistry.class);
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        context.setVariable(idVariable, getCounterName(context));
        context.setVariable(periodVariable, getAssertionTimeUnitAsFieldOfInterest().getName());
        final User user = context.getDefaultAuthenticationContext().getLastAuthenticatedUser();
        if (user != null) {
            context.setVariable(userVariable, user.getName());
        } else {
            context.setVariable(userVariable, "");
        }
        final long quota = getQuota(context);
        context.setVariable(maxVariable, String.valueOf(quota));

        switch (assertion.getCounterStrategy()) {
            case ThroughputQuotaAssertion.ALWAYS_INCREMENT:
                return doIncrementAlways(context, quota);
            case ThroughputQuotaAssertion.INCREMENT_ON_SUCCESS:
                return doIncrementOnSuccess(context, quota);
            case ThroughputQuotaAssertion.DECREMENT:
                return doDecrement(context);
            case ThroughputQuotaAssertion.RESET:
                return doReset(context);
            default:
                // not supposed to happen
                throw new PolicyAssertionException(assertion, "This assertion is not configured properly. " +
                        "Unsupported counterStrategy: " + assertion.getCounterStrategy());
        }
    }

    private int getValueToIncrementOrDecrement(PolicyEnforcementContext context) {
        int incrementOrDecrementBy = DEFAULT_INCREMENT_DECREMENT_VALUE;
        if (StringUtils.isNotBlank(assertion.getByValue())) {
            Map<String, ?> varMap = context.getVariableMap(varsUsed, getAudit());
            String value = ExpandVariables.process(assertion.getByValue(), varMap, getAudit());
            try {

                if (value == null) {
                    logAndAudit(AssertionMessages.NO_SUCH_VARIABLE, assertion.getByValue());
                    throw new AssertionStatusException(AssertionStatus.FAILED);
                }

                //if negative then policy assertion exception thrown
                incrementOrDecrementBy = Integer.parseInt(value);
                if (incrementOrDecrementBy < 0) {
                    logAndAudit(AssertionMessages.THROUGHPUT_QUOTA_INVALID_NEGATIVE_VALUE, assertion.getByValue());
                    throw new AssertionStatusException(AssertionStatus.FAILED);
                }

            } catch (NumberFormatException e) {
                logAndAudit(AssertionMessages.VARIABLE_INVALID_VALUE, assertion.getByValue(), "Integer");
                throw new AssertionStatusException(AssertionStatus.FAILED);
            }
        }
        return incrementOrDecrementBy;
    }

    private AssertionStatus doReset(PolicyEnforcementContext context) {
        getProvider().getCounterStore(COUNTER_STORE_NAME).reset(getCounterName(context));
        setValue(context, 0);
        return AssertionStatus.NONE;
    }

    private AssertionStatus doIncrementOnSuccess(final PolicyEnforcementContext context, final long quota) {
        boolean requiresIncrement = !alreadyIncrementedInThisContext(context);
        long val;

        String counterName = getCounterName(context);
        if (requiresIncrement) {
            try {
                int incrementValue = getValueToIncrementOrDecrement(context);
                val = getProvider().getCounterStore(COUNTER_STORE_NAME).updateAndGet(counterName, counterOperationProperties, getAssertionTimeUnitAsFieldOfInterest(), clock.millis(), incrementValue, quota);
                this.setValue(context, val);
                // no need to check the limit because the preceeding call would throw if limit was exceeded
                logger.log(Level.FINEST, "Value {0} still within quota {1}", new Object[]{val, quota});
                return AssertionStatus.NONE;
            } catch (CounterLimitReachedException e) {
                String msg = "throughput quota limit is already reached.";
                logger.info(msg);
                logAndAudit(AssertionMessages.THROUGHPUT_QUOTA_ALREADY_MET, counterName);
                if (assertion.isLogOnly()) {
                    this.setValue(context, getProvider().getCounterStore(COUNTER_STORE_NAME).get(counterName, counterOperationProperties, getAssertionTimeUnitAsFieldOfInterest()));
                    return AssertionStatus.NONE;
                } else {
                    return AssertionStatus.FALSIFIED;
                }
            } finally {
                // no sync issue here: this flag array belongs to the context which lives inside one thread only
                context.getIncrementedCounters().add(counterName);
            }
        } else {
            val = getProvider().getCounterStore(COUNTER_STORE_NAME).get(counterName, counterOperationProperties, getAssertionTimeUnitAsFieldOfInterest());
            this.setValue(context, val);
            if (val <= quota) {
                logger.log(Level.FINE, "the quota was not exceeded. {0} smaller than {1}", new Object[]{val, quota});
                return AssertionStatus.NONE;
            } else {
                String limit = "max " + quota + " per " + getAssertionTimeUnitAsFieldOfInterest().getName();
                String msg = "the quota " + counterName + " [" + limit +
                        "] was exceeded " + "(current value is " + val + ")";
                logAndAudit(AssertionMessages.THROUGHPUT_QUOTA_EXCEEDED, counterName, limit, Long.toString(val));
                logger.info(msg);
                return assertion.isLogOnly() ? AssertionStatus.NONE : AssertionStatus.FALSIFIED;
            }
        }
    }

    private AssertionStatus doDecrement(PolicyEnforcementContext context) {
        int decrementValue = 0 - getValueToIncrementOrDecrement(context);

        if (alreadyIncrementedInThisContext(context)) {
            final String counterName = getCounterName(context);
            long val = getProvider().getCounterStore(COUNTER_STORE_NAME).updateAndGet(
                    getCounterName(context),
                    counterOperationProperties,
                    getAssertionTimeUnitAsFieldOfInterest(),
                    clock.millis(),
                    decrementValue);
            logger.log(Level.FINE, "counter decremented {0}", counterName);
            forgetIncrementInThisContext(context); // to prevent double decrement and enable re-increment
            this.setValue(context, val);
        } else {
            logger.info("assertion was asked to decrement a counter but the " +
                    "counter was not previously recorded as incremented in this context.");
            // one could argue that this should result in error

            long val = getProvider().getCounterStore(COUNTER_STORE_NAME).get(
                    getCounterName(context),
                    counterOperationProperties,
                    getAssertionTimeUnitAsFieldOfInterest());
            this.setValue(context, val);
        }

        return AssertionStatus.NONE;
    }

    private AssertionStatus doIncrementAlways(final PolicyEnforcementContext context, final long quota) {
        boolean requiresIncrement = !alreadyIncrementedInThisContext(context);
        long val;
        final String counterName = getCounterName(context);
        if (requiresIncrement) {
            int incrementValue = getValueToIncrementOrDecrement(context);
            val = getProvider().getCounterStore(COUNTER_STORE_NAME).updateAndGet(counterName, counterOperationProperties, getAssertionTimeUnitAsFieldOfInterest(), clock.millis(), incrementValue);
            // no sync issue here: this flag array belongs to the context which lives inside one thread only
            context.getIncrementedCounters().add(counterName);
        } else {
            val = getProvider().getCounterStore(COUNTER_STORE_NAME).get(counterName, counterOperationProperties, getAssertionTimeUnitAsFieldOfInterest());
        }
        this.setValue(context, val);
        if (val <= quota) {
            logger.log(Level.FINE, "the quota was not exceeded. {0} smaller than {1}", new Object[]{val, quota});
            return AssertionStatus.NONE;
        } else {
            String limit = "max " + quota + " per " + getAssertionTimeUnitAsFieldOfInterest().getName();
            logAndAudit(AssertionMessages.THROUGHPUT_QUOTA_EXCEEDED, counterName, limit, Long.toString(val));
            return assertion.isLogOnly() ? AssertionStatus.NONE : AssertionStatus.FALSIFIED;
        }
    }

    private String getCounterName(final PolicyEnforcementContext context) {
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

        return resolvedCounterName;
    }

    private boolean alreadyIncrementedInThisContext(PolicyEnforcementContext context) {
        return context.getIncrementedCounters().contains(getCounterName(context));
    }

    private void forgetIncrementInThisContext(PolicyEnforcementContext context) {
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

    private long getQuota(final PolicyEnforcementContext context) {
        long longValue;

        final String quota = assertion.getQuota();
        final String[] referencedVars = Syntax.getReferencedNames(quota);

        try {
            if (referencedVars.length > 0) {
                final String stringValue = ExpandVariables.process(quota, context.getVariableMap(referencedVars, getAudit()), getAudit());
                longValue = Long.parseLong(stringValue);
            } else {
                longValue = Long.parseLong(quota);
            }

            if (config.getBooleanProperty(ServerConfigParams.PARAM_THROUGHPUTQUOTA_ENFORCE_MAX_QUOTA, false)) {
                final long maxQuotaValue = config.getLongProperty(ServerConfigParams.PARAM_THROUGHPUTQUOTA_MAX_THROUGHPUT_QUOTA, ThroughputQuotaAssertion.MAX_THROUGHPUT_QUOTA);
                if (longValue > maxQuotaValue) {
                    // configuration error
                    logAndAudit(AssertionMessages.THROUGHPUT_QUOTA_INVALID_MAX_QUOTA, String.valueOf(longValue), String.valueOf(maxQuotaValue));
                    throw new AssertionStatusException(AssertionStatus.FAILED);
                }
            }
        } catch (NumberFormatException e) {
            logAndAudit(AssertionMessages.VARIABLE_INVALID_VALUE, assertion.getQuota(), "Long");
            throw new AssertionStatusException(AssertionStatus.FAILED);
        }

        return longValue;
    }

    private CounterFieldOfInterest getAssertionTimeUnitAsFieldOfInterest() {
        return CounterFieldOfInterest.values()[assertion.getTimeUnit()];
    }

    private SharedCounterProvider getProvider() {
        String providerName = SyspropUtil.getProperty(SharedCounterProviderRegistry.SYSPROP_COUNTER_PROVIDER);
        SharedCounterProvider counterProvider = counterProviderRegistry.getExtension(providerName);

        if (counterProvider == null) {
            logger.log(Level.WARNING, "Provider with name {0} cannot be found. Assertion will not work.", providerName);
            throw new AssertionStatusException(AssertionStatus.FAILED, "Counter provider with name " + providerName + " cannot be found. Policy cannot process request.");
        } else {
            logger.log(Level.FINE, "{0} is using counter provider: {1}",
                    new Object[]{getAssertion().getClass().getSimpleName(), counterProvider.getName()});
        }

        return counterProvider;
    }
}