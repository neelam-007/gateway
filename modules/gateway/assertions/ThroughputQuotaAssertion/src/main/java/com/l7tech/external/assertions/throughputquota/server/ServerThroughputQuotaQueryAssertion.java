package com.l7tech.external.assertions.throughputquota.server;

import com.ca.apim.gateway.extension.sharedstate.counter.CounterFieldOfInterest;
import com.ca.apim.gateway.extension.sharedstate.counter.SharedCounterProvider;
import com.ca.apim.gateway.extension.sharedstate.counter.SharedCounterState;
import com.l7tech.external.assertions.throughputquota.ThroughputQuotaQueryAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.extension.registry.sharedstate.SharedCounterProviderRegistry;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.SyspropUtil;
import org.hibernate.HibernateException;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataAccessException;

import java.io.IOException;
import java.util.logging.Level;

/**
 * Server-side implementation of throughput quota query assertion.  Looks up a counter by its counter name,
 * and sets variables regarding its contents.
 */
public class ServerThroughputQuotaQueryAssertion extends AbstractServerAssertion<ThroughputQuotaQueryAssertion> {

    private final String[] variablesUsed;

    private final String counterNameRaw;
    private final String nameVar;
    private final String secVar;
    private final String minVar;
    private final String hrVar;
    private final String dayVar;
    private final String mntVar;
    private final String lastupdateVar;

    private SharedCounterProviderRegistry counterProviderRegistry;

    public ServerThroughputQuotaQueryAssertion(@NotNull final ThroughputQuotaQueryAssertion assertion, ApplicationContext context) {
        super(assertion);
        this.variablesUsed = assertion.getVariablesUsed();
        this.counterNameRaw = assertion.getCounterName();

        this.nameVar = prefix(ThroughputQuotaQueryAssertion.COUNTER_NAME);
        this.secVar = prefix(ThroughputQuotaQueryAssertion.COUNTER_SEC);
        this.minVar = prefix(ThroughputQuotaQueryAssertion.COUNTER_MIN);
        this.hrVar = prefix(ThroughputQuotaQueryAssertion.COUNTER_HR);
        this.dayVar = prefix(ThroughputQuotaQueryAssertion.COUNTER_DAY);
        this.mntVar = prefix(ThroughputQuotaQueryAssertion.COUNTER_MNT);
        this.lastupdateVar = prefix(ThroughputQuotaQueryAssertion.COUNTER_LASTUPDATE);
        counterProviderRegistry = context.getBean("sharedCounterProviderRegistry", SharedCounterProviderRegistry.class);
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        String counterName = ExpandVariables.process(counterNameRaw, context.getVariableMap(variablesUsed, getAudit()), getAudit());

        SharedCounterState counterState;
        try {
            counterState = getProvider().getCounterStore(ServerThroughputQuotaAssertion.COUNTER_STORE_NAME).query(counterName);
        } catch (DataAccessException | HibernateException e) {
            getAudit().logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{"Unable to query counter: " + ExceptionUtils.getMessage(e)}, e);
            return AssertionStatus.SERVER_ERROR;
        } catch (Exception e) {
            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                    new String[]{"Unexpected error while querying counter: " + e.getMessage()},
                    ExceptionUtils.getDebugException(e)
            );
            return AssertionStatus.FAILED;
        }

        if (counterState == null) {
            getAudit().logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Specified counter does not exist in the counter store");
            return AssertionStatus.FAILED;
        }

        context.setVariable(nameVar, counterState.getName());
        context.setVariable(secVar, counterState.getCount(CounterFieldOfInterest.SEC));
        context.setVariable(minVar, counterState.getCount(CounterFieldOfInterest.MIN));
        context.setVariable(hrVar, counterState.getCount(CounterFieldOfInterest.HOUR));
        context.setVariable(dayVar, counterState.getCount(CounterFieldOfInterest.DAY));
        context.setVariable(mntVar, counterState.getCount(CounterFieldOfInterest.MONTH));
        context.setVariable(lastupdateVar, String.valueOf(counterState.getLastUpdate()));
        return AssertionStatus.NONE;
    }

    private String prefix(String name) {
        final String prefix = assertion.getVariablePrefix();
        return prefix == null ? name : prefix + "." + name;
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
