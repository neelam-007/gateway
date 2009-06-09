package com.l7tech.server.policy.filter;

import com.l7tech.identity.User;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.util.EmptyIterator;

import java.util.Iterator;
import java.util.List;

/**
 * A Filter that processes a policy to remove any assertions that are not enabled.
 * This filter should run before the other filters are run so the other filters will
 * not have to worry about disabled assertions.
 */
public class HideDisabledAssertions implements Filter {
    public Assertion filter(User policyRequestor, Assertion assertionTree) {
        applyRules(assertionTree, new EmptyIterator() {
            public void remove() { }
        });
        return assertionTree;
    }

    private void applyRules(Assertion arg, Iterator parentIterator) {
        if (arg == null || !arg.isEnabled()) {
            parentIterator.remove();
            return;
        }

        if (arg instanceof CompositeAssertion) {
            CompositeAssertion comp = (CompositeAssertion)arg;
            List kids = comp.getChildren();
            Iterator i = kids.iterator();
            //noinspection WhileLoopReplaceableByForEach
            while (i.hasNext())
                applyRules((Assertion)i.next(), i);
            if (kids.isEmpty())
                parentIterator.remove();
        }
    }
}
