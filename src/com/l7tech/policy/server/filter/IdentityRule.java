package com.l7tech.policy.server.filter;

import com.l7tech.identity.User;
import com.l7tech.identity.Group;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.policy.assertion.identity.IdentityAssertion;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.common.util.Locator;
import com.l7tech.objectmodel.FindException;
import com.l7tech.logging.LogManager;

import java.util.Iterator;
import java.util.logging.Logger;
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
        Logger logger = LogManager.getInstance().getSystemLogger();
        // check what type of assertion we have
        if (idassertion instanceof SpecificUser) {
            SpecificUser userass = (SpecificUser)idassertion;
            if (userass.getIdentityProviderOid() == user.getProviderId())
                if (userass.getUserLogin().equals(user.getLogin())) return true;
            return false;
        } else if (idassertion instanceof MemberOfGroup) {
            MemberOfGroup grpmemship = (MemberOfGroup)idassertion;
            long idprovider = grpmemship.getIdentityProviderOid();
            if (idprovider == user.getProviderId()) {
                // try to match user to group
                Iterator i = user.getGroups().iterator();
                while (i.hasNext()) {
                    Group grp = (Group)i.next();
                    if (grp.getName().equals(grpmemship.getGroupName())) return true;
                }
                // try to match group to user
                try {
                    IdentityProvider prov = getIdentityProviderConfigManager().getIdentityProvider(idprovider);
                    Group grp = prov.getGroupManager().findByName(grpmemship.getGroupName());
                    for (Iterator jj = grp.getMembers().iterator(); jj.hasNext();) {
                        User memberx = (User)jj.next();
                        if (memberx.getLogin().equals(user.getLogin())) {
                            return true;
                        }
                    }
                } catch (FindException e) {
                    logger.log(Level.WARNING, "Cannot get group from provider", e);
                    return false;
                } catch (IllegalStateException e) {
                    logger.log(Level.WARNING, "Cannot get group from provider", e);
                    return false;
                }
            }
            return false;
        } else throw new FilteringException("unsupported IdentityAssertion type " + idassertion.getClass().getName());
    }

    private static IdentityProviderConfigManager getIdentityProviderConfigManager() {
        return (IdentityProviderConfigManager)Locator.getDefault().
                                                lookup(IdentityProviderConfigManager.class);
    }

    private User requestor = null;
    private boolean anIdentityAssertionWasFound = false;
    private boolean userPassedAtLeastOneIdentityAssertion = false;
}
