package com.l7tech.policy;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.composite.ExactlyOneAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Default policy path builder. The class builds assertion paths from
 * the assertion tree.
 *
 * It analyzes the policy using the following :
 * <p>
 * <ul>
 * <li>Given the assertion tree, generate the list of possible policy
 * paths. Policy path is a sequence of assertions that is processed
 * by the policy enforcemment point (<i>message processor</i> in L7
 * parlance). The <i>and</i> composite assertion results in a single
 * path, and <i>or</i> composite assertion generates number of paths
 * that is equal to a number of children. This is done by preorder
 * traversal of the policy tree.
 * <li>Processes each policy path and determine if the sequence of
 * assertions is in expected order.
 * </ul>
 * <p>

 *
 * the result is returned in an object of <code>PolicyPathResult</code> type.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.0
 */
public class DefaultPolicyPathBuilder extends PolicyPathBuilder {
    /**
     * Protected constructor, the class cannot be instantiated
     * directly
     */
    protected DefaultPolicyPathBuilder() {
    }

    /**
     * Generate the policy path result (policy assertion paths for
     * the <code>Assertion</code> tree.
     *
     * @param assertion the assertion tree to attempt the build
     * path for.
     * @return the result of the build
     */
    public PolicyPathResult generate(Assertion assertion) {
        return new DefaultPolicyPathResult(generatePaths(assertion));
    }


    /**
     * generate possible assertion paths
     *
     * @param assertion the root assertion
     */
    private Set generatePaths(Assertion assertion) {
        Set assertionPaths = new HashSet();
        assertionPaths.add(new AssertionPath(assertion));
        for (Iterator preorder = assertion.preorderIterator(); preorder.hasNext();) {
            Assertion anext = (Assertion)preorder.next();
            if (createsNewPaths(anext)) {
                CompositeAssertion ca = (CompositeAssertion)anext;
                Iterator children = ca.getChildren().iterator();
                Set newPaths = new HashSet();
                Set removePaths = new HashSet();
                for (; children.hasNext();) {
                    Assertion child = (Assertion)children.next();
                    for (Iterator ipaths = assertionPaths.iterator(); ipaths.hasNext();) {
                        AssertionPath parentPath = (AssertionPath)ipaths.next();
                        AssertionPath newPath = new AssertionPath(parentPath);
                        newPath.addAssertion(child);
                        newPaths.add(newPath);
                        removePaths.add(parentPath);
                    }
                }
                assertionPaths.addAll(newPaths);
                assertionPaths.removeAll(removePaths);
            } else {
                for (Iterator ipaths = assertionPaths.iterator(); ipaths.hasNext();) {
                    AssertionPath one = (AssertionPath)ipaths.next();
                    one.addAssertion(anext);
                }
            }
        }
        return assertionPaths;
    }

    /**
     * default assertion path result holder
     */
    static class DefaultPolicyPathResult implements PolicyPathResult {
        private Set assertionPaths;
        public DefaultPolicyPathResult(Set paths) {
            assertionPaths = Collections.unmodifiableSet(paths);
        }

        /**
         * returns the number of paths in this result
         * @return the number of assertiuon paths
         */
        public int getPathCount() {
            return assertionPaths.size();
        }

        /**
         * return the <code>Set</code> of paths that this path result contains
         *
         * @see AssertionPath
         * @return the set of assertion paths
         */
        public Set paths() {
            return assertionPaths;
        }
    }

    /**
     * does this assertion creates new paths
     * @param a the assertion to check
     * @return true if new paths, false otherwise
     */
    static boolean createsNewPaths(Assertion a) {
        return
          a instanceof ExactlyOneAssertion ||
          a instanceof OneOrMoreAssertion;
    }

    static boolean isValidRoot(Assertion a) {
        return
          a instanceof AllAssertion ||
          a instanceof OneOrMoreAssertion;
    }
}
