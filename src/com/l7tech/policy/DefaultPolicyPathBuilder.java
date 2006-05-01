package com.l7tech.policy;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;

import java.util.*;
import java.util.logging.Level;

/**
 * Default policy path builder. The class builds assertion paths from
 * the assertion tree.
 * <p/>
 * It analyzes the policy using the following :
 * <p/>
 * Given the assertion tree, generate the list of possible assertion
 * paths. Assertion path is a sequence of assertions that is processed
 * by the policy enforcemment point (<i>message processor</i> in L7
 * parlance). The <i>and</i> composite assertion results in a single
 * path, and <i>or</i> composite assertion generates number of paths
 * that is equal to a number of children. This is done by preorder
 * traversal of the policy tree.
 * <p/>
 * <p/>
 * <p/>
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
     *                  path for.
     * @return the result of the build
     */
    public PolicyPathResult generate(Assertion assertion) {
        Set paths = generatePaths(assertion);
        int pathOrder = 0;
        for (Iterator iterator = paths.iterator(); iterator.hasNext(); pathOrder++) {
            AssertionPath path = (AssertionPath)iterator.next();
            path.setPathOrder(pathOrder);
            if (log.isLoggable(Level.FINEST)) {
                log.finest(pathToString(path));
            }
        }
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
        final AssertionPath initPath = new AssertionPath(new Assertion[]{(Assertion)preorder.next()});
        assertionPaths.add(initPath);
        pathStack.push(initPath);

        for (; preorder.hasNext();) {
            Assertion anext = (Assertion)preorder.next();
            if (parentCreatesNewPaths(anext)) {
                AssertionPath cp = (AssertionPath)pathStack.peek();

                Set add = new LinkedHashSet();
                Set remove = new HashSet();

                for (Iterator iterator = assertionPaths.iterator(); iterator.hasNext();) {
                    AssertionPath assertionPath = (AssertionPath)iterator.next();
                    if (assertionPath.contains(anext.getParent()) &&
                      !containsSibling(assertionPath, anext)) {
                        AssertionPath a = assertionPath.addAssertion(anext);
                        add.add(a);
                    }
                }
                AssertionPath stackPath = cp.addAssertion(anext);
                add.add(stackPath);

                assertionPaths.removeAll(remove);
                assertionPaths.addAll(add);

                List siblings = anext.getParent().getChildren();
                // if last sibling and the parent path was pushed on stack, pop the parent
                if (siblings.indexOf(anext) + 1 == siblings.size()) {
                    AssertionPath p = (AssertionPath)pathStack.pop();
                    assertionPaths.remove(p);
                }

                if (isSplitPath(anext)) {
                    pathStack.push(stackPath);
                }

            } else {
                Set add = new LinkedHashSet();
                Set remove = new HashSet();

                for (Iterator iterator = assertionPaths.iterator(); iterator.hasNext();) {
                    AssertionPath assertionPath = (AssertionPath)iterator.next();
                    if (assertionPath.contains(anext.getParent())) {
                        AssertionPath a = assertionPath.addAssertion(anext);
                        add.add(a);
                        remove.add(assertionPath);
                    }
                }
                AssertionPath[] paths = (AssertionPath[])add.toArray(new AssertionPath[]{});
                if (paths.length > 0) {
                    AssertionPath path = paths[paths.length - 1];
                    Assertion lastAssertion = path.lastAssertion();
                    if (lastAssertion instanceof OneOrMoreAssertion) {
                        OneOrMoreAssertion oom = (OneOrMoreAssertion)lastAssertion;
                        List children = oom.getChildren();
                        if (!children.isEmpty())
                            pathStack.push(path);
                    }
                }
                assertionPaths.removeAll(remove);
                assertionPaths.addAll(add);
            }

        }
        return pruneIncompletePaths(assertionPaths);
    }

    /**
     * Test whether the assertion path contains the a assertion's sibling
     *
     * @param assertionPath the assertion path
     * @param a             the assertion to test
     * @return true if the assertion path contiansd a's sibling, false otherwise
     */
    private boolean containsSibling(AssertionPath assertionPath, Assertion a) {
        Assertion[] path = assertionPath.getPath();

        for (int i = 0; i < path.length; i++) {
            Assertion assertion = path[i];
            if (isSibling(assertion, a)) return true;
        }
        return false;
    }

    private boolean isSibling(Assertion assertion, Assertion anext) {
        return assertion.getParent() == anext.getParent() && assertion != anext;
    }

    private Stack pathStack = new Stack();


    /**
     * prune the incomplete assertion paths. The incomplete paths
     * contain composite assertions  where the assertion has children
     * and the child is not immediate assertion in the path. Those
     * (incomplete) paths were kept arround so the correct full paths
     * could get accumulated
     *
     * @param assertionPaths the assertion path set to process
     * @return the new <code>Set</code> without incomplete paths
     */
    private Set pruneIncompletePaths(Set assertionPaths) {
        Set remove = new HashSet();

        // the last node is a composite, it has children but the path
        // is not showing that
        // this could be as well done in the second loop. Doing the 'last assertion'
        // check first is a quickshortcut, and happens often
        for (Iterator iterator = assertionPaths.iterator(); iterator.hasNext();) {
            AssertionPath assertionPath = (AssertionPath)iterator.next();
            final Assertion assertion = assertionPath.lastAssertion();
            if (assertion instanceof CompositeAssertion) {
                CompositeAssertion ca = (CompositeAssertion)assertion;
                if (!ca.getChildren().isEmpty()) {
                    remove.add(assertionPath);
                    continue;
                }
            }
            // there is node that is a composite, it has some children, but the next
            // assertion in path is not in the child set
            Assertion[] assertionArr = assertionPath.getPath();
            for (int i = 0; i < assertionArr.length; i++) {
                Assertion a = assertionArr[i];
                if (a == assertionPath.lastAssertion()) {
                    continue;
                }
                if (a instanceof CompositeAssertion) {
                    CompositeAssertion ca = (CompositeAssertion)a;
                    List children = ca.getChildren();
                    Assertion next = assertionArr[i+1];
                    if (!children.isEmpty() && !children.contains(next)) {
                        remove.add(assertionPath);
                        break;
                    }
                }
            }
        }

        assertionPaths.removeAll(remove);
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
         *
         * @return the number of assertiuon paths
         */
        public int getPathCount() {
            return assertionPaths.size();
        }

        /**
         * return the <code>Set</code> of paths that this path result contains
         *
         * @return the set of assertion paths
         * @see AssertionPath
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
        CompositeAssertion parent = a.getParent();
        if (parent == null) return true;
        return parent instanceof OneOrMoreAssertion && !((OneOrMoreAssertion)parent).getChildren().isEmpty();
    }


    /**
     * is this a split path assertion
     *
     * @param a the assertion to check
     * @return true if split path, false otherwise
     */
    static boolean isSplitPath(Assertion a) {
        return a instanceof OneOrMoreAssertion && !((OneOrMoreAssertion)a).getChildren().isEmpty();
    }

    public static String pathToString(AssertionPath ap) {
        String sp = printPath("** Begin assertion path\n", "** End assertion path", ap);
        return sp;
    }

    private static String printPath(String begin, String end, AssertionPath ap) {
        Assertion[] ass = ap.getPath();
        StringBuffer sb = new StringBuffer(begin);
        for (int i = 0; i < ass.length; i++) {
            if (ass[i] instanceof CompositeAssertion) {
                sb.append("" + (i + 1) + " " + ass[i].getClass().toString() + '@' + System.identityHashCode(ass[i]))
                  .append("\n");
            } else {
                sb.append("" + (i + 1) + " " + ass[i]).append("\n");
            }
        }
        sb.append(end);
        return sb.toString();
    }
}
