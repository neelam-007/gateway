package com.l7tech.external.assertions.ratelimit.server;

import com.l7tech.external.assertions.ratelimit.RateLimitQueryAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Looks up a counter and sets variables about its current state.
 */
public class ServerRateLimitQueryAssertion extends AbstractServerAssertion<RateLimitQueryAssertion> {
    private final String[] variablesUsed;

    // Variables.  TODO: Remove unused after func spec reviews
    private final String counterNameRaw;
    private final String nameVar;
    private final String concurrencyVar;
    private final String maxConcurrencyVar;
    private final String blackoutMillisRemainingVar;
    private final String idleMillisVar;
    private final String pointsVar;
    private final String pointsPerRequestVar;
    private final String requestsRemainingVar;
    private final String resolutionVar;

    public ServerRateLimitQueryAssertion(@NotNull final RateLimitQueryAssertion assertion) {
        super(assertion);
        this.variablesUsed = assertion.getVariablesUsed();
        this.counterNameRaw = assertion.getCounterName();

        this.nameVar = prefix(RateLimitQueryAssertion.COUNTER_NAME);
        this.blackoutMillisRemainingVar = prefix(RateLimitQueryAssertion.COUNTER_BLACKOUTMILLISREMAINING);
        this.concurrencyVar = prefix(RateLimitQueryAssertion.COUNTER_CONCURRENCY);
        this.maxConcurrencyVar = prefix(RateLimitQueryAssertion.COUNTER_MAXCONCURRENCY);
        this.idleMillisVar = prefix(RateLimitQueryAssertion.COUNTER_IDLEMILLIS);
        this.pointsVar = prefix(RateLimitQueryAssertion.COUNTER_POINTS);
        this.pointsPerRequestVar = prefix(RateLimitQueryAssertion.COUNTER_POINTSPERREQUEST);
        this.requestsRemainingVar = prefix(RateLimitQueryAssertion.COUNTER_REQUESTSREMAINING);
        this.resolutionVar = prefix(RateLimitQueryAssertion.COUNTER_RESOLUTION);
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        final String counterName = getConterName(context);
        RateLimitCounter counter = ServerRateLimitAssertion.findExistingCounter(counterName);

        if (counter == null) {
            // TODO should we report different information in this case?
            counter = new RateLimitCounter(counterName);
        }

        RateLimitCounter.StateSnapshot state = counter.query();
        final long now = System.currentTimeMillis();
        context.setVariable(nameVar, counter.getName());
        context.setVariable(concurrencyVar, counter.concurrency.get());
        // context.setVariable(maxConcurrencyVar, counter.lastMaxConcurrency.get());

        long idleMillis = now - state.lastUsed;
        //context.setVariable(idleMillisVar, idleMillis > 0 ? idleMillis : 0);

        final long blackoutUntil = counter.blackoutUntil.get();
        long blackoutRemaining = blackoutUntil > 0 ? blackoutUntil - now : 0;
        context.setVariable(blackoutMillisRemainingVar, blackoutRemaining > 0 ? blackoutRemaining : 0);

        //context.setVariable(pointsVar, state.pointsCreditedForIdleTime);
        //context.setVariable(pointsPerRequestVar, ServerRateLimitAssertion.POINTS_PER_REQUEST);
        context.setVariable(requestsRemainingVar, state.pointsCreditedForIdleTime.divide(ServerRateLimitAssertion.POINTS_PER_REQUEST));

        //context.setVariable(resolutionVar, ServerRateLimitAssertion.useNanos ? "nanos" : "millis");

        return AssertionStatus.NONE;
    }

    private String getConterName(PolicyEnforcementContext context) {
        return ExpandVariables.process(counterNameRaw, context.getVariableMap(variablesUsed, getAudit()), getAudit());
    }

    private String prefix(String name) {
        final String prefix = assertion.getVariablePrefix();
        return prefix == null ? name : prefix + "." + name;
    }
}
