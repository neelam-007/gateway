package com.l7tech.console.tree.policy;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.identity.User;
import com.l7tech.identity.Group;

import java.security.Principal;
import java.util.*;

/**
 * Class <code>IdentityPath</code> represents a path in the .
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class IdentityPath {

    /**
     * Extract the identities from the assertion tree.
     *
     * @param root the root of assertion tree
     * @return the <code>Set</code> of identities that
     * are specified in this assertion tree.
     */
    public static Set getIdentities(Assertion root) {
        Set identities = new TreeSet(IDCOMPARATOR);
        for (Iterator it = root.preorderIterator(); it.hasNext();) {
            Object o = it.next();
            if (isIdentity(o)) {
                identities.add(extractIdentity(o));
            }
        }
        return identities;
    }


    public static IdentityPath forIdentity(Principal p, Assertion root) {
        return null;
    }

    /**
     * protected constructor accepting principal.
     *
     * @param p the principal this identity path represents
     */
    protected IdentityPath(Principal p) {
        principal = p;
    }

    /**
     * the <code>Principal</code> that this identity path describes.
     * This is <code>User</code> or <code>Group</code> instance.
     *
     * @return the <code>Principal</code> user or group
     */
    public Principal getPrincipal() {
        return principal;
    }

    /**
     * returns the unmdifiable <code>List</code> of assertion paths
     * for the identoty (user or group) represnetewd by this path.
     *
     * @return the identity paths
     */
    public List getPaths() {
        return Collections.unmodifiableList(identityPaths);
    }

    /**
     * is the object passed an identity
     * @param assertion
     * @return whether the assertion is an identity
     */
    private static boolean isIdentity(Object assertion) {
        return
          assertion instanceof SpecificUser ||
          assertion instanceof MemberOfGroup;
    }

    /**
     * Extract the user or from the object. The expected
     * object must be assertion of
     * @param assertion
     * @return whether the assertion is an identity
     */
    private static Principal extractIdentity(Object assertion) {
        if (assertion instanceof SpecificUser) {
            SpecificUser su = ((SpecificUser)assertion);
            User u = new User();
            u.setLogin(su.getUserLogin());
            u.setProviderId(su.getIdentityProviderOid());
            return u;
        } else if (assertion instanceof MemberOfGroup) {
            MemberOfGroup mog = ((MemberOfGroup)assertion);
            Group g = new Group();
            g.setName(mog.getGroupOid());
            g.setProviderId(mog.getIdentityProviderOid());
            return g;
        }
        throw new IllegalArgumentException("Unknown assertion type " + assertion);
    }


    /** user or group */
    private Principal principal;
    private List identityPaths = new ArrayList();

    private static final Comparator IDCOMPARATOR = new Comparator() {
        /**
         * Compares its two arguments for order.  This compares users and groups
         * by provider and name. Two users or groups are considered equal if they
         * have the same provider, and name.

         * @param o1 the first object to be compared (user or group).
         * @param o2 the second object to be compared (user or group).
         * @return a negative integer, zero, or a positive integer as the
         * 	       first argument is less than, equal to, or greater than the
         *	       second.
         * @throws ClassCastException if the arguments' types prevent them from
         * 	       being compared by this Comparator.
         */
        public int compare(Object o1, Object o2) {
            if (o1.getClass() != o2.getClass()) {
                return -1;
            }
            if (User.class.equals(o1.getClass())) {
                return compareUsers((User)o1, (User)o2);
            }
            return 0;

        }
    };

    private static int compareUsers(User u1, User u2) {
        int n = (int)(u1.getProviderId() - u2.getProviderId());
        if (n != 0) return n;
        return u2.getName().compareTo(u1.getName());
    }


    private static int compareGroup(Group g1, Group g2) {
        int n = (int)(g1.getProviderId() - g2.getProviderId());
        if (n != 0) return n;
        return g2.getName().compareTo(g1.getName());
    }
}
