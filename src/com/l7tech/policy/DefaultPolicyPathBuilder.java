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
        Set paths = generatePaths(assertion);

//        for (Iterator iterator = paths.iterator(); iterator.hasNext();) {
//                printPath((AssertionPath)iterator.next());
//        }

        return new DefaultPolicyPathResult(paths);
    }


    /**
     * generate possible assertion paths
     *
     * @param assertion the root assertion
     */
    private Set generatePaths(Assertion assertion) {
        Set assertionPaths = new LinkedHashSet();
        Iterator preorder = assertion.preorderIterator();
        assertionPaths.add(new AssertionPath(new Assertion[]{(Assertion)preorder.next()}));
        pathStack.push(lastPath(assertionPaths));

        for (; preorder.hasNext();) {
            Assertion anext = (Assertion)preorder.next();

            if (parentCreatesNewPaths(anext)) {
                AssertionPath cp = (AssertionPath)pathStack.peek();
                assertionPaths.remove(cp);
                AssertionPath assertionPath = cp.addAssertion(anext);
                assertionPaths.add(assertionPath);
                if (isSplitPath(anext)) pathStack.push(assertionPath);
            } else {
                Set add = new HashSet();
                Set remove = new HashSet();

                for (Iterator iterator = assertionPaths.iterator(); iterator.hasNext();) {
                    AssertionPath assertionPath = (AssertionPath)iterator.next();
                    if (assertionPath.contains(anext.getParent())) {
                        AssertionPath a = assertionPath.addAssertion(anext);
                        add.add(a);
                        remove.add(assertionPath);
                    }
                }
                AssertionPath[] paths = (AssertionPath[])add.toArray(new AssertionPath[] {});
                if (paths.length > 0) {
                    AssertionPath path = paths[paths.length - 1];
                    if (path.lastAssertion() instanceof OneOrMoreAssertion)
                        pathStack.push(path);
                }
                assertionPaths.removeAll(remove);
                assertionPaths.addAll(add);
            }
        }
        pathStack.pop();
        // return pruneEmptyComposites(assertionPaths);
        return assertionPaths; 
    }
    private Stack pathStack = new Stack();


    private AssertionPath lastPath(Set set) {
        AssertionPath[] apaths =
          (AssertionPath[])set.toArray(new AssertionPath[]{});
        if (apaths.length == 0) {
            throw new IllegalArgumentException("path size is " + 0);
        }
        return apaths[apaths.length - 1];
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
        return a.getParent() instanceof OneOrMoreAssertion;
    }


    /**
     * is this a split path assertion
     *
     * @param a the assertion to check
     * @return true if split path, false otherwise
     */
    static boolean isSplitPath(Assertion a) {
        return a instanceof OneOrMoreAssertion;
    }

    private static void printPath(AssertionPath ap) {
        Assertion[] ass = ap.getPath();
System.out.println("** Begin assertion path");
        for (int i = 0; i < ass.length; i++) {
System.out.println(ass[i].getClass());
        }
System.out.println("** End assertion path");

    }

}
