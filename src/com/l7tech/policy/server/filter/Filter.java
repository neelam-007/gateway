package com.l7tech.policy.server.filter;

import com.l7tech.identity.User;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

/**
 * A filter that implements an encapsulable rule.
 *
 * <br/><br/>
 * User: flascell<br/>
 * Date: Aug 14, 2003<br/>
 * $Id$
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
    }

    /**
     * Used by subclasses. Removes the passed assertion from its parent and potentially "next" siblings.
     * If the assertion does not have a parent, nothing will be done.
     * @param arg the assertion to remove from its parent
     * @param removeAlsoNextSiblings if true, the siblings coming after this assertion will also be removed from the parent
     */
    protected void removeSelfFromParent(Assertion arg, boolean removeAlsoNextSiblings) {
        if (arg == null) {
            String msg = "null assertion was passed"; // (not the stuff in chineese food)
            logger.severe(msg);
            throw new IllegalArgumentException(msg);
        }
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

    /**
     * removes all assertion children of the parent of the assertion passed. (sets empty children to the parent node)
     * @param arg the assertion whose parent's children will be removed
     */
    protected void removeSelfAndAllSiblings(Assertion arg) {
        if (arg == null) {
            String msg = "null assertion was passed"; // (not the stuff in chineese food)
            logger.severe(msg);
            throw new IllegalArgumentException(msg);
        }
        CompositeAssertion parent = arg.getParent();
        if (parent == null) {
            logger.warning("Filter action resulted in removing all assertions from policy.");
            return;
        }
        parent.setChildren(new LinkedList());
    }

    protected static final Logger logger = Logger.getLogger(Filter.class.getName());
}
