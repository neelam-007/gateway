package com.l7tech.console.tree.policy;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyPathBuilder;
import com.l7tech.policy.PolicyPathResult;
import com.l7tech.identity.User;
import com.l7tech.identity.Group;

import java.security.Principal;
import java.util.*;

/**
 * Class <code>IdentityPath</code> represents a collection of
 * assertion paths for an identity within given policy.
 * <p>
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

    /**
     * Create the set of <code>IdentityPaths</code> that exist in this
     * assertion tree. This returns all the meniotned identities with
     * their paths.
     *
     * @param root the assertion root
     * @return the set of identity paths that exist in thi policy tree
     */
    public static Set getPaths(Assertion root) {
        Set identities = getIdentities(root);
        Set paths = new HashSet();
        for (Iterator i = identities.iterator(); i.hasNext();) {
            Principal principal = (Principal)i.next();
            paths.add(forIdentity(principal, root));
        }
        IdentityPath anonPath = anonymousPaths(root);
        if (!anonPath.getPaths().isEmpty()) {
            paths.add(anonPath);
        }
        return paths;
    }

    /**
     * Create the <code>IdentityPath</code> from the assertion for the
     * the given principal.
     *
     * @param p the principal
     * @param root the assertion root
     * @return the identity path with the collection of assertion paths
     *         for the
     */
    public static IdentityPath forIdentity(Principal p, Assertion root) {
        if (!(p instanceof User || p instanceof Group)) {
            throw new IllegalArgumentException("unknown type");
        }
        IdentityPath ipath = new IdentityPath(p);
        PolicyPathBuilder pb = PolicyPathBuilder.getDefault();
        PolicyPathResult ppr = pb.generate(root);
        outer:
        for (Iterator i = ppr.paths().iterator(); i.hasNext();) {
            AssertionPath ap = (AssertionPath)i.next();
            Assertion[] path = ap.getPath();
            for (int j = path.length - 1; j >= 0; j--) {
                Assertion assertion = path[j];
                if (isIdentity(assertion)) {
                    Principal principal = extractIdentity(assertion);
                    if (IDCOMPARATOR.compare(principal, p) == 0) {
                        ipath.identityPaths.add(path);
                        continue outer;
                    }
                }
            }
        }
        return ipath;
    }


    private static IdentityPath anonymousPaths(Assertion root) {
        User anon = new User();
        anon.setLogin("Anonymous");
        anon.setName(anon.getLogin());
        IdentityPath ipath = new IdentityPath(anon);
        PolicyPathBuilder pb = PolicyPathBuilder.getDefault();
        PolicyPathResult ppr = pb.generate(root);
        outer:
        for (Iterator i = ppr.paths().iterator(); i.hasNext();) {
            AssertionPath ap = (AssertionPath)i.next();
            Assertion[] path = ap.getPath();
            for (int j = path.length - 1; j >= 0; j--) {
                Assertion assertion = path[j];
                if (isIdentity(assertion)) {
                    continue outer;
                }
            }
            ipath.identityPaths.add(path);
        }
        return ipath;

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
     * returns the unmdifiable <code>Set</code> of assertion paths
     * for the identity (user or group) represented by this path
     * collection.
     *
     * @return the identity paths
     */
    public Set getPaths() {
        return Collections.unmodifiableSet(identityPaths);
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
    static Principal extractIdentity(Object assertion) {
        if (assertion instanceof SpecificUser) {
            SpecificUser su = ((SpecificUser)assertion);
            User u = new User();
            u.setLogin(su.getUserLogin());
            u.setName(su.getUserLogin());
            u.setProviderId(su.getIdentityProviderOid());
            return u;
        } else if (assertion instanceof MemberOfGroup) {
            MemberOfGroup mog = ((MemberOfGroup)assertion);
            Group g = new Group();
            g.setName(mog.getGroupName());
            g.setProviderId(mog.getIdentityProviderOid());
            return g;
        }
        throw new IllegalArgumentException("Unknown assertion type " + assertion);
    }


    /** user or group */
    private Principal principal;
    private Set identityPaths = new HashSet();

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
            } else if (Group.class.equals(o1.getClass())) {
                return compareGroup((Group)o1, (Group)o2);
            }
            throw new ClassCastException("Don't know how to handle " + o1.getClass());

        }
    };

    private static int compareUsers(User u1, User u2) {
        int n = (int)(u1.getProviderId() - u2.getProviderId());
        if (n != 0) return n;
        return u2.getLogin().compareTo(u1.getLogin());
    }


    private static int compareGroup(Group g1, Group g2) {
        int n = (int)(g1.getProviderId() - g2.getProviderId());
        if (n != 0) return n;
        return g2.getName().compareTo(g1.getName());
    }
}
