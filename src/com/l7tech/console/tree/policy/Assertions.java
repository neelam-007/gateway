package com.l7tech.console.tree.policy;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.RoutingAssertion;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.credential.CredentialSourceAssertion;
import com.l7tech.policy.assertion.identity.IdentityAssertion;

import java.util.*;

/**
 * Supporting class for Assertions.
 * <p>
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a> 
 */
public class Assertions {
    /**
     * Sort the children of the composite assertion in the policy
     * processing order
     * @param cn
     * @return the sorted children colleciton
     */
    public static Collection sort(CompositeAssertionTreeNode cn) {
        List l = Collections.list(cn.children());
        if (l.isEmpty()) return l;
        Collections.sort(l, new AssertionsComparator());

        return l;
    }



    private static boolean isRouting(Assertion a) {
        return a instanceof RoutingAssertion;
    }

    private static boolean isAccessControl(Assertion a) {
        return a instanceof IdentityAssertion;
    }

    private static boolean isCrendentialSource(Assertion a) {
        return a instanceof CredentialSourceAssertion;
    }

    private static boolean isPreconditionAssertion(Assertion a) {
        return a instanceof SslAssertion;
    }

    private static boolean isComposite(Assertion a) {
        return a instanceof CompositeAssertion;
    }

    /**
     * Assertions comparator. compare the assertions in th policy
     * porcessing order
     */
    private static class AssertionsComparator implements Comparator {
        /**
         * Compares its two arguments (asssertion tree nodes )for order.
         * @param o1 the first object to be compared.
         * @param o2 the second object to be compared.
         * @return a negative integer, zero, or a positive integer as the
         * 	       first argument is less than, equal to, or greater than the
         *	       second.
         * @throws ClassCastException if the arguments' types prevent them from
         * 	       being compared by this Comparator.
         */
        public int compare(Object o1, Object o2) {
            AssertionTreeNode a1 = (AssertionTreeNode)o1;
            AssertionTreeNode a2 = (AssertionTreeNode)o2;
            if (isComposite(a1.asAssertion()) || isComposite(a2.asAssertion())) return 0;
            int n1 = getCompareValue(a1);
            int n2 = getCompareValue(a2);
            return n1 - n2;
        }

        private int getCompareValue(AssertionTreeNode o1) {
            Assertion a = o1.asAssertion();
            if (isPreconditionAssertion(a)) return 0;
            if (isCrendentialSource(a)) return 1;
            if (isAccessControl(a)) return 2;
            if (isRouting(a)) return 3;

            return 5;
        }
    }
}
