package com.l7tech.server.message;

import com.l7tech.server.message.metrics.LatencyMetrics;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.policy.assertion.ServerAssertion;
import org.jetbrains.annotations.Nullable;

/**
 * Interface that can be implemented by PolicyEnforcementContext users who wish to be notified every time a
 * ServerAssertion finishes executing.
 */
public interface AssertionTraceListener {
    /**
     * Report that a ServerAssertion has just finished executing on the specified PolicyEnforcementContext.
     * The ServerAssertion has either just returned from checkRequest(), or has just thrown AssertionStatusException
     * from checkRequest().
     * 
     * @param assertion the ServerAssertion whose checkRequest() method just returned.  Required.
     * @param status the AssertionStatus returned from the the checkRequest() method.  Never null.
     * @param assertionMetrics the {@link LatencyMetrics} for the specified {@code assertion}.  Optional and can
     *                         be {@code null} if there are no metrics gathered for the assertion
     */
    void assertionFinished(ServerAssertion assertion, AssertionStatus status, @Nullable LatencyMetrics assertionMetrics);
}
