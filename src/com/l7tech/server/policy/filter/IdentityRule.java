package com.l7tech.server.policy.filter;

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
    private static final int AN_ID_WAS_PASSED = 3;
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
                return AN_ID_WAS_PASSED;
            } else {
                if (parent instanceof AllAssertion) {
                    return ALL_SIBLINGS_SHOULD_BE_DELETED;
                } else {
                    parentIterator.remove();
                }
                return SOMETHING_WAS_DELETED;
            }
        } else if (arg instanceof CompositeAssertion) {
            // apply rules to children
            CompositeAssertion root = (CompositeAssertion)arg;
            Iterator i = root.getChildren().iterator();
            boolean shouldClearTheAll = false;
            boolean somethingWasDeleted = false;
            boolean andIdAssertionWasPassed = false;
            while (i.hasNext()) {
                Assertion kid = (Assertion)i.next();
                int kidres = applyRules(kid, i, root);
                if (kidres == ALL_SIBLINGS_SHOULD_BE_DELETED) {
                    shouldClearTheAll = true;
                    somethingWasDeleted = true;
                    break;
                } else if (kidres == SOMETHING_WAS_DELETED) {
                    somethingWasDeleted = true;
                } else if (kidres == AN_ID_WAS_PASSED) {
                    andIdAssertionWasPassed = true;
                }
            }
            if (shouldClearTheAll) {
                root.getChildren().clear();
            }
            // if this composite is the child of an ALL and something was deleted in it and it is now empty, it
            // means all siblings should be nuked
            if (parent instanceof AllAssertion && somethingWasDeleted && root.getChildren().isEmpty()) {
                if (!andIdAssertionWasPassed) {
                    return ALL_SIBLINGS_SHOULD_BE_DELETED;
                }
            }
            // if all children of this composite were removed, we have to remove it from it's parent
            if (root.getChildren().isEmpty() && parentIterator != null) {
                parentIterator.remove();
                if (andIdAssertionWasPassed) return AN_ID_WAS_PASSED;
                else return SOMETHING_WAS_DELETED;
            }
        }
        return NOTHING_WAS_DELETED;
    }

    /**
     * check whether the user validates this assertion
     */
    private boolean validateIdAssertion(IdentityAssertion idassertion) throws FilteringException {
        return canUserPassIDAssertion(idassertion, requestor, null);
    }

    public static boolean canUserPassIDAssertion(IdentityAssertion idassertion, User user, IdentityProviderConfigManager identityProviderConfigManager) throws FilteringException {
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
                    IdentityProvider prov = identityProviderConfigManager.getIdentityProvider(idprovider);
                    if (prov == null) {
                        logger.warning("IdentityProvider #" + idprovider + " no longer exists");
                        return false;
                    }
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

    private User requestor = null;
    private boolean anIdentityAssertionWasFound = false;
    private boolean userPassedAtLeastOneIdentityAssertion = false;
}
