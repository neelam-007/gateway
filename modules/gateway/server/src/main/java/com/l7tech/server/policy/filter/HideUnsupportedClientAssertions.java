package com.l7tech.server.policy.filter;

import com.l7tech.identity.User;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.util.ArrayUtils;

import java.util.Iterator;

/**
 * Hide all non-whitelisted assertions.
 */
public class HideUnsupportedClientAssertions implements Filter {
    
    public HideUnsupportedClientAssertions() {
    }

    @Override
    public Assertion filter( final User policyRequestor,
                             final Assertion assertionTree ) throws FilteringException {
        if (assertionTree == null) return null;
        applyRules(assertionTree, null);
        return assertionTree;
    }

    /**
     * returns true if one or more assertion was deleted amoungs the siblings of this assertion
     */
    private boolean applyRules( final Assertion arg,
                                final Iterator parentIterator ) throws FilteringException {
        // apply rules on this one
        if (arg instanceof CompositeAssertion) {
            // apply rules to children
            CompositeAssertion root = (CompositeAssertion)arg;
            Iterator i = root.getChildren().iterator();
            while (i.hasNext()) {
                Assertion kid = (Assertion)i.next();
                if (kid.isEnabled()) {
                    applyRules(kid, i);
                }
                else {
                    // If it is disabled, then ignore it.
                    i.remove();
                }
            }
            // if all children of this composite were removed, we have to remove it from it's parent
            if (root.getChildren().isEmpty() && parentIterator != null) {
                parentIterator.remove();
                return true;
            }
        } else {
            if ( ( !arg.isEnabled() ||
                   (!Assertion.isRequest(arg) && !Assertion.isResponse(arg)) ||
                   (Assertion.isRequest(arg) && !ArrayUtils.contains((String[])arg.meta().get(AssertionMetadata.CLIENT_ASSERTION_TARGETS), "request")) ||
                   (Assertion.isResponse(arg) && !ArrayUtils.contains((String[])arg.meta().get(AssertionMetadata.CLIENT_ASSERTION_TARGETS), "response")))
                 && parentIterator != null) {
                parentIterator.remove();
                return true;
            }

            if (Boolean.TRUE.equals(arg.meta().get(AssertionMetadata.USED_BY_CLIENT)))
                return false;

            if (parentIterator == null) {
                throw new RuntimeException("Invalid policy, all policies must have a composite assertion at the root");
            }

            parentIterator.remove();
            return true;
        }
        return false;
    }
}
