package com.l7tech.policy.server.filter;

import com.l7tech.identity.User;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.RoutingAssertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import java.util.Iterator;

/**
 * User: flascell
 * Date: Aug 14, 2003
 * Time: 3:13:44 PM
 * $Id$
 *
 * Removes all routing assertions.
 *
 * If, as a result, a composite is left with no children, this composite assertion is also removed from it's parent
 * except if it's the root assertion in the tree.
 */
public class HideRoutingTarget extends Filter {
    public Assertion filter(User policyRequestor, Assertion assertionTree) throws FilteringException {
        if (assertionTree == null) return null;
        applyRules(assertionTree);
        return assertionTree;
    }

    public HideRoutingTarget() {
        super();
    }

    /**
     * returns true if one or more assertion was deleted amoungs the siblings of this assertion
     */
    private boolean applyRules(Assertion arg) throws FilteringException {
        // apply rules on this one
        if (arg instanceof RoutingAssertion) {
            removeSelfFromParent(arg, false);
        } else if (arg instanceof CompositeAssertion) {
            // apply rules to children
            CompositeAssertion root = (CompositeAssertion)arg;
            Iterator i = root.getChildren().iterator();
            while (i.hasNext()) {
                Assertion kid = (Assertion)i.next();
                boolean res = applyRules(kid);
                // the children were affected
                if (res) {
                    // if all children of this composite were removed, we have to remove it from it's parent
                    if (root.getChildren().isEmpty()) {
                        removeSelfFromParent(root, false);
                        return true;
                    }
                    // otherwise continue, but reget the iterator because the list of children is affected
                    else i = root.getChildren().iterator();
                }
            }
        }
        return false;
    }
}
