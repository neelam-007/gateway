package com.l7tech.server.policy.assertion.composite;

import com.l7tech.gateway.common.LicenseException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.variable.BuiltinVariables;
import com.l7tech.policy.variable.PolicyVariableUtils;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.ServerPolicyFactory;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.TimeSource;
import org.springframework.beans.factory.BeanFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class ServerCompositeAssertion<CT extends CompositeAssertion>
    extends AbstractServerAssertion<CT>
{
    private static final Logger logger = Logger.getLogger(ServerCompositeAssertion.class.getName());
    private List<ServerAssertion> children;
    private final boolean[] recordLatency;
    private final TimeSource timeSource;

    public ServerCompositeAssertion(CT composite, BeanFactory beanFactory) throws PolicyAssertionException, LicenseException {
        super(composite);
        if (beanFactory == null)
            throw new IllegalArgumentException("The Application Context is required");

        final ServerPolicyFactory pf = beanFactory.getBean("policyFactory", ServerPolicyFactory.class);

        final List<ServerAssertion> result = new ArrayList<ServerAssertion>(composite.getChildren().size());

        timeSource = getTimeSource();
        recordLatency = new boolean[composite.getChildren().size()];
        try {
            Assertion prevChild = null;
            int index = 0;
            for ( final Iterator<Assertion> i = composite.children(); i.hasNext(); ) {
                final Assertion child = i.next();
                recordLatency[index] = false;
                if (prevChild != null) {
                    if (PolicyVariableUtils.usesAnyVariable(child, BuiltinVariables.ASSERTION_LATENCY_MS, BuiltinVariables.ASSERTION_LATENCY_NS)) {
                        recordLatency[index-1] = true;
                    }
                }
                prevChild = child;
                index++;
                final ServerAssertion sass = pf.compileSubtree(child);
                assert sass != null;
                result.add(sass);
            }
        } catch ( final Exception e ) {
            // Close partially created policy
            for ( final ServerAssertion serverAssertion : result ) {
                ResourceUtils.closeQuietly( serverAssertion );
            }

            if ( e instanceof PolicyAssertionException ) {
                throw (PolicyAssertionException) e;
            } else if ( e instanceof LicenseException ) {
                throw (LicenseException) e;
            } else {
                throw ExceptionUtils.wrap( e );
            }
        }
        this.children = Collections.unmodifiableList(result);
    }
    
    protected TimeSource getTimeSource() {
        return new TimeSource();
    }
            

    public List<ServerAssertion> getChildren() {
        return children;
    }

    public String toString() {
        return "<" + this.getClass().getName() + " children=" + children + ">";
    }

    protected void seenAssertionStatus(PolicyEnforcementContext context, AssertionStatus assertionStatus) {
        if(context != null) {
            context.addSeenAssertionStatus(assertionStatus);
        }
    }

    protected void rollbackDeferredAssertions(PolicyEnforcementContext context) {
        if (context != null)
            context.removeDeferredAssertion(this);
        for (ServerAssertion child : children) {
            if (child instanceof ServerCompositeAssertion)
                ((ServerCompositeAssertion)child).rollbackDeferredAssertions(context);
            if (context != null)
                context.removeDeferredAssertion(child);
        }
    }

    /**
     * Iterate the children of composite assertion, notify the context that an assertion
     * is about to begin evaluation, evaluate the child assertion, notify the context that
     * an assertion is finished, notify the AssertionResultListener that an assertion is finished
     * and if necessary, capture the assertion latency.
     *
     * @param context The PolicyEnforcementContext attached to the request
     * @param listener The listener which will be notified when a child assertion is finished.
     * @return The assertion status
     * @throws IOException if there is a problem reading a request or response
     * @throws PolicyAssertionException as an alternate mechanism to return an assertion status other than AssertionStatus.NONE.
     */
    protected AssertionStatus iterateChildren(PolicyEnforcementContext context, AssertionResultListener listener) throws IOException, PolicyAssertionException {
        final List<ServerAssertion> kids = getChildren();
        AssertionStatus result = AssertionStatus.NONE;

        int i = 0;
        long startTime = 0;
        for ( final ServerAssertion kid : kids ) {

            context.assertionStarting(kid);

            if (recordLatency[i]) {
                startTime = timeSource.nanoTime();
            }
            try {
                result = kid.checkRequest(context);
            } catch (AssertionStatusException e) {
                result = e.getAssertionStatus();
            }
            if (recordLatency[i]) {
                context.setAssertionLatencyNanos(timeSource.nanoTime() - startTime);
            }
            i++;
            context.assertionFinished(kid, result);

            if (listener != null) {
                boolean proceed = listener.assertionFinished(context, result);
                if (!proceed) {
                    return result;
                }
            }
        }

        return result;
    }

    @Override
    public void close() {
        for (ServerAssertion child : children) {
            try {
                child.close();
            } catch (Exception e) {
                logger.log(Level.WARNING, "Child assertion did not close cleanly", e);
            }
        }
    }

    /**
     * Interface that can be implemented by ServerCompositeAssertion users who wish to be notified every time a
     * child ServerAssertion finishes executing in a Composite Assertion.
     */
    protected static interface AssertionResultListener {

        /**
         * Report that a child ServerAssertion has just finished executing on the Composite Assertion.
         *
         * @param context the PolicyEnforementContext attached to the request
         * @param result the AssertionStatus returned from the the checkRequest() method.  Never null.
         * @return True to proceed to the next child assertion. False to terminate iteration of composite assertion.
         */

        boolean assertionFinished(PolicyEnforcementContext context, AssertionStatus result);

    }
}
