package com.l7tech.server.trace;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.AssertionTraceListener;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.ServerPolicyHandle;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.util.ExceptionUtils;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An AssertionTraceListener that invokes the trace policy each time an assertion finishes executing.
 */
public class TracePolicyEvaluator implements AssertionTraceListener {
    private static final Logger logger = Logger.getLogger(TracePolicyEvaluator.class.getName());

    private final TracePolicyEnforcementContext tracePec;
    private final ServerPolicyHandle tracePolicyHandle;

    private TracePolicyEvaluator(TracePolicyEnforcementContext tracePec, ServerPolicyHandle tracePolicyHandle) {
        if (tracePec == null) throw new NullPointerException("tracePec");
        if (tracePolicyHandle == null) throw new NullPointerException("tracePolicyHandle");
        this.tracePec = tracePec;
        this.tracePolicyHandle = tracePolicyHandle;
    }

    /**
     * Create a TracePolicyEvaluator and attach it as the trace listener to the specified context.
     * This will arrange to execute the specified tracePolicyHandle in reponse to each assertionFinished event.
     * <p/>
     * Ownership of the trace policy handle will be given to the contextToTrace: if this constructor
     * succeeds, the tracePolicyHandle will no longer need to be closed as it will be closed when contextToTrace is closed.
     *
     * @param contextToTrace the context that is to be traced.  Required.  Caller retains ownership of the contextToTrace and remains responsible for closing it.
     * @param tracePolicyHandle the server policy to invoke for each trace event.  Required.  This handle will be closed when contextToTrace is closed.
     */
    public static TracePolicyEvaluator createAndAttachToContext(final PolicyEnforcementContext contextToTrace, final ServerPolicyHandle tracePolicyHandle) {
        final TracePolicyEnforcementContext tracePec = new TracePolicyEnforcementContext(contextToTrace);
        final TracePolicyEvaluator traceEvaluator = new TracePolicyEvaluator(tracePec, tracePolicyHandle);
        contextToTrace.runOnClose(new Runnable() {
            @Override
            public void run() {
                Object out = tracePec.getTraceOut();
                if (out != null && logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "Trace output: " + out);
                }

                contextToTrace.setTraceListener(null);
                tracePec.close();
                tracePolicyHandle.close();
            }
        });
        contextToTrace.setTraceListener(traceEvaluator);
        return traceEvaluator;
    }

    TracePolicyEnforcementContext getTraceContext() {
        return tracePec;
    }

    @Override
    public void assertionFinished(ServerAssertion assertion, AssertionStatus status) {
        tracePec.setTracedAssertion(assertion);
        tracePec.setTracedStatus(status);

        try {
            PolicyEnforcementContextFactory.doWithCurrentContext(tracePec, new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    tracePolicyHandle.checkRequest(tracePec);
                    return null;
                }
            });

        } catch (PolicyAssertionException e) {
            throw new RuntimeException("Unable to run trace policy: " + ExceptionUtils.getMessage(e), e);
        } catch (IOException e) {
            throw new RuntimeException("Unable to run trace policy: " + ExceptionUtils.getMessage(e), e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to run trace policy: " + ExceptionUtils.getMessage(e), e);
        }
    }
}
