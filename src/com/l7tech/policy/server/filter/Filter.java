package com.l7tech.policy.server.filter;

import com.l7tech.identity.User;
import com.l7tech.logging.LogManager;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

/**
 * User: flascell
 * Date: Aug 14, 2003
 * Time: 2:54:54 PM
 * $Id$
 *
 * A filter that implements an encapsulable rule.
 */
public abstract class Filter {
    /**
     * Applies a rule that affects how an external requestor sees a policy.
     *
     * @param policyRequestor may be null if the requestor is anonymous
     * @param assertionTree
     * @return a filtered policy. may return null if the result of the filter is that the requestor may not
     * consume this service at all.
     */
    public abstract Assertion filter(User policyRequestor, Assertion assertionTree) throws FilteringException;

    public Filter() {
        logger = LogManager.getInstance().getSystemLogger();
    }

    protected void removeSelfFromParent(Assertion arg, boolean removeAlsoNextSiblings) throws FilteringException {
        CompositeAssertion parent = arg.getParent();
        // special case, empty root assertion (anonymous access?)
        if (parent == null) {
            logger.warning("Filter action resulted in removing all assertions from policy.");
            return;
        }
        List newKids = new LinkedList();
        Iterator i = parent.getChildren().iterator();
        boolean pastSelf = false;
        while (i.hasNext()) {
            Assertion kid = (Assertion)i.next();
            if (kid == arg) {
                pastSelf = true;
            } else if (!pastSelf) newKids.add(kid);
            else if (!removeAlsoNextSiblings ) newKids.add(kid);
        }
        parent.setChildren(newKids);
    }

    protected void removeSelfAndAllSiblings(Assertion arg)  throws FilteringException {
        CompositeAssertion parent = arg.getParent();
        if (parent == null) {
            logger.warning("Filter action resulted in removing all assertions from policy.");
            return;
        }
        parent.setChildren(new LinkedList());
    }

    protected Logger logger = null;
}
