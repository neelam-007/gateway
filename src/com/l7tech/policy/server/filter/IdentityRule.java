package com.l7tech.policy.server.filter;

import com.l7tech.identity.User;
import com.l7tech.identity.Group;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.assertion.identity.IdentityAssertion;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Level;

/**
 * User: flascell
 * Date: Aug 14, 2003
 * Time: 3:11:22 PM
 * $Id$
 *
 * If there is at least one identity assertion, and the user does not "pass" any of them, the result will be null
 * meaning that this user cannot consume this service and therefore has no business seeing it.
 *
 * Inside a composite assertion, all chidren following an identity assertion are removed from the tree IF the assertion
 * is not passed AND if the parent is an ALL composite.
 *
 * If the id assertion is passed, it is removed from the composite parent but not the following siblings.
 *
 * CompositeAssertion that become child-less as a result of processing this tree are removed from their parents -
 * except if this results in the root assertion to be deleted.
 */
public class IdentityRule extends Filter {
    public IdentityRule() {
        super();
    }
    public Assertion filter(User policyRequestor, Assertion assertionTree) throws FilteringException {
        requestor = policyRequestor;
        if (assertionTree == null) return null;
        applyRules(assertionTree);
        if (anIdentityAssertionWasFound && !userPassedAtLeastOneIdentityAssertion) {
            logger.severe("This user is not authorized to consume this service. Policy filter returning null.");
            return null;
        }
        return assertionTree;
    }

    /**
     * returns true if one or more assertion was deleted amoungs the siblings of this assertion
     */
    private boolean applyRules(Assertion arg) throws FilteringException {
        // apply rules on this one
        if (arg instanceof IdentityAssertion) {
            anIdentityAssertionWasFound = true;
            if (validateIdAssertion((IdentityAssertion)arg)) {
                userPassedAtLeastOneIdentityAssertion = true;
                removeSelfFromParent(arg, false);
            }
            else {
                Assertion parent = arg.getParent();
                if (parent == null) return true;
                else if (parent instanceof AllAssertion) {
                    removeSelfAndAllSiblings(arg);
                    //removeSelfFromParent(arg, true);
                }
                else removeSelfFromParent(arg, false);
            }
            return true;
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
            // else unknown
        }
        return false;
    }

    /**
     * check whether the user validates this assertion
     */
    private boolean validateIdAssertion(IdentityAssertion idassertion) throws FilteringException {
        return canUserPassIDAssertion(idassertion, requestor);
    }

    public static boolean canUserPassIDAssertion(IdentityAssertion idassertion, User user) throws FilteringException {
        if (user == null) return false;
        // check what type of assertion we have
        if (idassertion instanceof SpecificUser) {
            SpecificUser userass = (SpecificUser)idassertion;
            if (userass.getIdentityProviderOid() == user.getProviderId())
                if (userass.getUserLogin().equals(user.getLogin())) return true;
            return false;
        } else if (idassertion instanceof MemberOfGroup) {
            MemberOfGroup grpmemship = (MemberOfGroup)idassertion;
            if (grpmemship.getIdentityProviderOid() == user.getProviderId()) {
                Iterator i = user.getGroups().iterator();
                while (i.hasNext()) {
                    Group grp = (Group)i.next();
                    if (grp.getName().equals(grpmemship.getGroupName())) return true;
                }
            }
            return false;
        } else throw new FilteringException("unsupported IdentityAssertion type " + idassertion.getClass().getName());
    }

    private User requestor = null;
    private boolean anIdentityAssertionWasFound = false;
    private boolean userPassedAtLeastOneIdentityAssertion = false;
}
