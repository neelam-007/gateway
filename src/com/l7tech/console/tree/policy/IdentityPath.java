package com.l7tech.console.tree.policy;

import com.l7tech.identity.Group;
import com.l7tech.identity.GroupBean;
import com.l7tech.identity.User;
import com.l7tech.identity.UserBean;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyPathBuilder;
import com.l7tech.policy.PolicyPathResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.assertion.ext.Category;
import com.l7tech.policy.assertion.ext.CustomAssertion;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.policy.assertion.identity.SpecificUser;

import java.security.Principal;
import java.util.*;

/**
 * Class <code>IdentityPath</code> represents a collection of
 * assertion paths for an identity within given policy.
 * <p/>
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class IdentityPath {
    protected static final Comparator DEFAULT_COMPARATOR = new Comparator() {
        public int compare(Object o1, Object o2) {
            if (!(o1 instanceof IdentityPath && o2 instanceof IdentityPath)) {
                throw new ClassCastException();
            }
            Principal p1 = ((IdentityPath)o1).getPrincipal();
            Principal p2 = ((IdentityPath)o2).getPrincipal();

            if (p1.equals(p2)) return 0;
            if (p1 instanceof User && p2 instanceof User) {
                User u1 = (User)p1;
                User u2 = (User)p2;
                long ret = u1.getProviderId() - u2.getProviderId();
                if (ret != 0) return (int)ret;
                if ( u2.getLogin() != null ) return u2.getLogin().compareTo(u1.getLogin());
                if ( u2.getName() != null ) return u2.getName().compareTo(u1.getName());
                return 0;
            } else if (p1 instanceof Group && p2 instanceof Group) {
                Group g1 = (Group)p1;
                Group g2 = (Group)p2;
                long ret = g1.getProviderId() - g2.getProviderId();
                if (ret != 0) return (int)ret;
                return g2.getName().compareTo(g1.getName());
            }
            return p1 instanceof Group ? 1 : -1;
        }
    };

    /** the anonymous path label */
    public static final String ANONYMOUS = "Anonymous";
    public static final String CUSTOM_ACCESS_CONTROL = "Custom Access Control:";

    /**
     * Extract the identities from the assertion tree.
     *
     * @param root the root of assertion tree
     * @return the <code>Set</code> of identities that
     *         are specified in this assertion tree.
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
        return getPaths(root, DEFAULT_COMPARATOR);
    }

    /**
     * Create the set of <code>IdentityPaths</code> that exist in this
     * assertion tree. This returns all the meniotned identities with
     * their paths.
     * The identities are sorted according to the specified comparator.
     *
     * @param root the assertion root
     * @param c    the the comparator that will be used to sort the identities
     *             set.
     * @return the set of identity paths that exist in thi policy tree
     */
    public static Set getPaths(Assertion root, Comparator c) {
        Set identities = getIdentities(root);
        Set paths = new TreeSet(c);
        for (Iterator i = identities.iterator(); i.hasNext();) {
            Principal principal = (Principal)i.next();
            paths.add(forIdentity(principal, root));
        }
        IdentityPath anonPath = anonymousPaths(root);
        if (!anonPath.getPaths().isEmpty()) {
            paths.add(anonPath);
        }
        IdentityPath customAccesControlPath = customAccessControlPaths(root);
        if (!customAccesControlPath.getPaths().isEmpty()) {
            paths.add(customAccesControlPath);
        }
        return paths;
    }


    /**
     * Determine the set of paths - <code>IdentityPath</code> that exist
     * in the policy rooted at the <code>Assertion</code> for the the given
     * principal.
     *
     * @param p    the principal
     * @param root the assertion root
     * @return the identity path with the collection of assertion paths
     *         for the given principal
     * @see IdentityPath
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
                        ipath.identityPaths.add(ap);
                        continue outer;
                    }
                }
            }
        }
        return ipath;
    }


    /**
     * Determine the set of paths - <code>IdentityPath</code> that exist
     * in the policy rooted at the <code>Assertion</code> that are anonymous
     * Anonymous paths are paths that do not contain any identity check,
     * group membership check, and custom access control check.
     *
     * @param root the assertion root
     * @return the collection of aanonymous assertion paths
     */
    private static IdentityPath anonymousPaths(Assertion root) {
        UserBean anon = new UserBean();
        anon.setLogin(ANONYMOUS);
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
                if (isIdentity(assertion) ||
                  isCustomAccessControl(assertion)) {
                    continue outer;
                }
            }
            ipath.identityPaths.add(ap);
        }
        return ipath;
    }


    /**
     * Determine the set of paths - <code>IdentityPath</code> that exist
     * in the policy rooted at the <code>Assertion</code> that contain the
     * access control custom assertion.
     * Those paths are considered to have the identity check delegated to
     * the custom assertion
     *
     * @param root the assertion root
     * @return the collection of aanonymous assertion paths
     */
    private static IdentityPath customAccessControlPaths(Assertion root) {
        UserBean anon = new UserBean();
        StringBuffer sb = new StringBuffer(CUSTOM_ACCESS_CONTROL);

        IdentityPath ipath = new IdentityPath(anon);
        PolicyPathBuilder pb = PolicyPathBuilder.getDefault();
        PolicyPathResult ppr = pb.generate(root);
        outer:
        for (Iterator i = ppr.paths().iterator(); i.hasNext();) {
            AssertionPath ap = (AssertionPath)i.next();
            Assertion[] path = ap.getPath();
            boolean found = false;
            for (int j = path.length - 1; j >= 0; j--) {
                Assertion assertion = path[j];
                if (isCustomAccessControl(assertion)) {
                    if (found) {
                        sb.append(", "); //multiple delegate assertions
                    }
                    CustomAssertionHolder cah = (CustomAssertionHolder)assertion;
                    CustomAssertion customAssertion = cah.getCustomAssertion();
                    if (customAssertion != null) {
                        sb.append(customAssertion.getName());
                    } else {
                        sb.append("Bad or misconfigured access control assertion");
                    }
                    found = true;
                }
            }
            if (found) {
                ipath.identityPaths.add(ap);
            }
        }
        anon.setLogin(sb.toString());
        anon.setName(anon.getLogin());

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
     * return a set of assertion instances from this identity path
     * that are equal to the given type.
     *
     * @param cl the assertion type
     * @return the <code>Set</code> of the assertion instances.
     */
    public Set getEqualAssertions(Class cl) {
        Set resultSet = new HashSet();
        if (cl == null || !Assertion.class.isAssignableFrom(cl)) {
            return resultSet;
        }

        for (Iterator iterator = identityPaths.iterator(); iterator.hasNext();) {
            Assertion[] assertions = ((AssertionPath)iterator.next()).getPath();
            for (int i = 0; i < assertions.length; i++) {
                Assertion assertion = assertions[i];
                if (assertion.getClass().equals(cl)) {
                    resultSet.add(assertion);
                }
            }
        }
        return resultSet;
    }

    /**
     * return a set of assertion instances from this identity path
     * that are assignable to the given type.
     *
     * @param cl the assertion type
     * @return the <code>Set</code> of the assertion instances.
     */
    public Set getAssignableAssertions(Class cl) {
        Set resultSet = new HashSet();
        if (cl == null || !Assertion.class.isAssignableFrom(cl)) {
            return resultSet;
        }

        for (Iterator iterator = identityPaths.iterator(); iterator.hasNext();) {
            Assertion[] assertions = ((AssertionPath)iterator.next()).getPath();
            for (int i = 0; i < assertions.length; i++) {
                Assertion assertion = assertions[i];
                if (cl.isAssignableFrom(assertion.getClass())) {
                    resultSet.add(assertion);
                }
            }
        }
        return resultSet;
    }

    /**
     * is the object passed an identity assertion
     *
     * @param assertion
     * @return whether the assertion is an identity
     */
    private static boolean isIdentity(Object assertion) {
        return
          assertion instanceof SpecificUser ||
          assertion instanceof MemberOfGroup;
    }

    /**
     * is the object passed an custom access control assertion
     *
     * @param assertion
     * @return whether the assertion is an custom asertion access control
     */
    private static boolean isCustomAccessControl(Object assertion) {
        return
          assertion instanceof CustomAssertionHolder &&
          Category.ACCESS_CONTROL.equals(((CustomAssertionHolder)assertion).getCategory());
    }


    /**
     * Extract the user or from the object. The expected
     * object must be assertion of
     *
     * @param assertion
     * @return whether the assertion is an identity
     */
    public static Principal extractIdentity(Object assertion) {
        if (assertion instanceof SpecificUser) {
            SpecificUser su = ((SpecificUser)assertion);
            UserBean u = new UserBean();
            u.setUniqueIdentifier(su.getUserUid());
            u.setProviderId(su.getIdentityProviderOid());
            u.setLogin(su.getUserLogin());
            u.setName(su.getUserName());
            return u;
        } else if (assertion instanceof MemberOfGroup) {
            MemberOfGroup mog = ((MemberOfGroup)assertion);
            GroupBean g = new GroupBean();
            g.setUniqueIdentifier(mog.getGroupId());
            g.setProviderId(mog.getIdentityProviderOid());
            g.setName(mog.getGroupName());
            return g;
        }
        throw new IllegalArgumentException("Unknown assertion type " + assertion);
    }


    /**
     * user or group
     */
    private Principal principal;
    private Set identityPaths = new HashSet();

    private static final Comparator IDCOMPARATOR = new Comparator() {
        /**
         * Compares its two arguments for order.  This compares users and groups
         * by provider and name. Two users or groups are considered equal if they
         * have the same provider, and name.
         *
         * @param o1 the first object to be compared (user or group).
         * @param o2 the second object to be compared (user or group).
         * @return a negative integer, zero, or a positive integer as the
         *         first argument is less than, equal to, or greater than the
         *         second.
         * @throws ClassCastException if the arguments' types prevent them from
         *                            being compared by this Comparator.
         */
        public int compare(Object o1, Object o2) {
            if (o1.getClass() != o2.getClass()) {
                return -1;
            }
            if (o1 instanceof User) {
                return compareUsers((User)o1, (User)o2);
            } else if (o1 instanceof Group) {
                return compareGroup((Group)o1, (Group)o2);
            }
            throw new ClassCastException("Don't know how to handle " + o1.getClass());

        }
    };

    private static int compareUsers(User u1, User u2) {
        int n = (int)(u1.getProviderId() - u2.getProviderId());
        if (n != 0) return n;
        if (u2.getLogin() != null) return u2.getLogin().compareTo(u1.getLogin());
        if (u2.getName() != null) return u2.getName().compareTo(u1.getName());
        return 0;
    }


    private static int compareGroup(Group g1, Group g2) {
        int n = (int)(g1.getProviderId() - g2.getProviderId());
        if (n != 0) return n;
        return g2.getName().compareTo(g1.getName());
    }
}
