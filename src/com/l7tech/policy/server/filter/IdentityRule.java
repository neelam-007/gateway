package com.l7tech.policy.server.filter;

import com.l7tech.common.util.Locator;
import com.l7tech.identity.*;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.identity.IdentityAssertion;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.policy.assertion.identity.SpecificUser;

import java.util.Iterator;
import java.util.logging.Level;

/**
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
 *
 * <br/><br/>
 * User: flascell<br/>
 * Date: Aug 14, 2003<br/>
 * $Id$
 */
public class IdentityRule extends Filter {
    public IdentityRule() {
        super();
    }
    public Assertion filter(User policyRequestor, Assertion assertionTree) throws FilteringException {
        requestor = policyRequestor;
        if (assertionTree == null) return null;
        applyRules(assertionTree, null, null);
        if (anIdentityAssertionWasFound && !userPassedAtLeastOneIdentityAssertion && requestor != null) {
            logger.severe("This user is not authorized to consume this service. Policy filter returning null.");
            return null;
        }
        return assertionTree;
    }

    // results for apply rule
    private static final int NOTHING_WAS_DELETED = 0;
    private static final int SOMETHING_WAS_DELETED = 1;
    private static final int ALL_SIBLINGS_SHOULD_BE_DELETED = 2;
    /**
     * @return see NOTHING_WAS_DELETED, SOMETHING_WAS_DELETED, or ALL_SIBLINGS_SHOULD_BE_DELETED
     */
    private int applyRules(Assertion arg, Iterator parentIterator, CompositeAssertion parent) throws FilteringException {
        // apply rules on this one
        if (arg instanceof IdentityAssertion) {
            if (parent == null || parentIterator == null) {
                throw new RuntimeException("ID assertions must have a parent. This is not a valid policy.");
            }
            anIdentityAssertionWasFound = true;
            if (validateIdAssertion((IdentityAssertion)arg)) {
                userPassedAtLeastOneIdentityAssertion = true;
                if (parentIterator == null) {
                    throw new RuntimeException("Invalid policy, all policies must have a composite assertion at the root");
                }
                parentIterator.remove();
            } else {
                if (parent instanceof AllAssertion) {
                    return ALL_SIBLINGS_SHOULD_BE_DELETED;
                } else {
                    parentIterator.remove();
                }
            }
            return SOMETHING_WAS_DELETED;
        } else if (arg instanceof CompositeAssertion) {
            // apply rules to children
            CompositeAssertion root = (CompositeAssertion)arg;
            Iterator i = root.getChildren().iterator();
            boolean shouldClearTheAll = false;
            while (i.hasNext()) {
                Assertion kid = (Assertion)i.next();
                if (applyRules(kid, i, root) == ALL_SIBLINGS_SHOULD_BE_DELETED) {
                    shouldClearTheAll = true;
                    break;
                }
            }
            if (shouldClearTheAll) {
                root.getChildren().clear();
            }
            // if all children of this composite were removed, we have to remove it from it's parent
            if (root.getChildren().isEmpty() && parentIterator != null) {
                parentIterator.remove();
                return SOMETHING_WAS_DELETED;
            }
        }
        return NOTHING_WAS_DELETED;
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
                if (userass.getUserLogin().equals(user.getLogin())) {
                    return true;
                }
            return false;
        } else if (idassertion instanceof MemberOfGroup) {
            MemberOfGroup grpmemship = (MemberOfGroup)idassertion;
            long idprovider = grpmemship.getIdentityProviderOid();
            if (idprovider == user.getProviderId()) {
                try {
                    IdentityProvider prov = getIdentityProviderConfigManager().getIdentityProvider(idprovider);
                    GroupManager gman = prov.getGroupManager();
                    Group grp = gman.findByPrimaryKey(grpmemship.getGroupId());
                    if (grp == null) {
                        logger.warning("The group " + grpmemship.getGroupId() + " does not exist.");
                        return false;
                    }
                    return gman.isMember(user, grp);
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
