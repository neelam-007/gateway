package com.l7tech.policy;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.ExactlyOneAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;

import java.util.*;

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
        Set assertionPaths = new LinkedHashSet();
        AssertionPath lastPath = null;
        for (Iterator preorder = assertion.preorderIterator(); preorder.hasNext();) {
            Assertion anext = (Assertion)preorder.next();
            AssertionPath newPath = null;
            if (parentCreatesNewPaths(anext)) {
                if (anext.getParent() == null) {
                    newPath = new AssertionPath(new Assertion[]{anext});
                } else {
                    newPath = new AssertionPath(anext.getPath());
                }
                lastPath = newPath;
            } else {
                newPath = lastPath.addAssertion(anext);
                assertionPaths.remove(lastPath);
                lastPath = newPath;
            }
            assertionPaths.add(newPath);
        }
        return pruneEmptyComposites(assertionPaths);
    }

    /**
     * prune the assertion paths that end with emty composite
     * assertions.
     *
     * @param assertionPaths the assertion path set to process
     * @return the new <code>Set</code> without assertion paths
     * that end with empty composites.
     */
    private Set pruneEmptyComposites(Set assertionPaths) {
        Set result = new HashSet();
        for (Iterator iterator = assertionPaths.iterator(); iterator.hasNext();) {
            AssertionPath assertionPath = (AssertionPath)iterator.next();
            if (!(assertionPath.lastAssertion() instanceof CompositeAssertion)) {
                result.add(assertionPath);
            }
        }
        return result;
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
     * does the immediate composite parent of this assertion
     * creates new paths
     *
     * @param a the assertion to check
     * @return true if new paths, false otherwise
     */
    static boolean parentCreatesNewPaths(Assertion a) {
        if (a.getParent() == null) return true;

        Assertion[] path = a.getPath();
        if (path == null || path.length == 0)
            throw new IllegalArgumentException("path null or empty");

        for (int i = path.length - 2; i >= 0; i--) {
            if (path[i] instanceof AllAssertion) return false;
        }
        return true;
    }
}
