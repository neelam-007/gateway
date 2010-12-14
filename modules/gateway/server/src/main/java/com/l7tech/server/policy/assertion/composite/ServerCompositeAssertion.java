package com.l7tech.server.policy.assertion.composite;

import com.l7tech.gateway.common.LicenseException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.ServerPolicyFactory;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ResourceUtils;
import org.springframework.beans.factory.BeanFactory;

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

    public ServerCompositeAssertion(CT composite, BeanFactory beanFactory) throws PolicyAssertionException, LicenseException {
        super(composite);
        if (beanFactory == null)
            throw new IllegalArgumentException("The Application Context is required");

        final ServerPolicyFactory pf = beanFactory.getBean("policyFactory", ServerPolicyFactory.class);

        final List<ServerAssertion> result = new ArrayList<ServerAssertion>(composite.getChildren().size());
        try {
            for ( final Iterator<Assertion> i = composite.children(); i.hasNext(); ) {
                final Assertion child = i.next();
                if ( !child.isEnabled() ) continue;

                final ServerAssertion sass = pf.compileSubtree(child);
                if ( sass != null )
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
}
