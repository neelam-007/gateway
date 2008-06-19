package com.l7tech.policy;

import com.l7tech.common.policy.IncludeAssertionDereferenceTranslator;
import com.l7tech.common.policy.Policy;
import com.l7tech.objectmodel.ReadOnlyEntityManager;
import com.l7tech.objectmodel.PolicyHeader;
import com.l7tech.objectmodel.GuidBasedEntityManager;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionTranslator;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.Include;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.policy.wsp.WspWriter;

import java.util.*;
import java.util.logging.Level;
import java.io.IOException;

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
    private final GuidBasedEntityManager<Policy> policyFinder;

    /**
     * Protected constructor, the class cannot be instantiated
     * directly
     */
    protected DefaultPolicyPathBuilder(final GuidBasedEntityManager<Policy> policyFinder) {
        this.policyFinder = policyFinder;
    }

    /**
     * Put includes inline
     */
    @Override
    public Assertion inlineIncludes(final Assertion assertion, final Set<String> includedPolicyGuids) throws  PolicyAssertionException, InterruptedException {
        final Assertion rootWithIncludes;

        if ( Assertion.contains(assertion, Include.class) ) {
            Set<String> guids = includedPolicyGuids==null ? new HashSet<String>() : includedPolicyGuids;
            final AssertionTranslator translator = new IncludeAssertionDereferenceTranslator(policyFinder, guids, false);
            try {
                rootWithIncludes = translate(WspReader.getDefault().parsePermissively(WspWriter.getPolicyXml(assertion)), translator);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            rootWithIncludes = assertion;
        }

        return rootWithIncludes;
    }    

    /**
     * Generate the policy path result (policy assertion paths for
     * the <code>Assertion</code> tree.
     *
     * @param assertion the assertion tree to attempt the build
     *                  path for.
     * @return the result of the build
     */
    @Override
    public PolicyPathResult generate(Assertion assertion, boolean processIncludes) throws InterruptedException, PolicyAssertionException {
        Set<AssertionPath> paths = generatePaths(assertion,processIncludes);
        int pathOrder = 0;
        for (Iterator iterator = paths.iterator(); iterator.hasNext(); pathOrder++) {
            AssertionPath path = (AssertionPath)iterator.next();
            path.setPathOrder(pathOrder);
            if (log.isLoggable(Level.FINEST)) {
                log.finest(pathToString(path));
            }
            if (Thread.interrupted()) throw new InterruptedException();
        }
        return new DefaultPolicyPathResult(paths);
    }

    /**
     * Essentially a helper method that can pre-proecess the assertion for any include fragments problems.  If there
     * were any PolicyAssertionException found, then it will accumuldate into a list then returned back to the caller
     * to decide what to do with the exceptions.
     *
     * @param assertion The assertion to process for Include fragments
     * @return  Returns a list of PolicyAssertionException, if any.  Will never return NULL.
     */
    public List<PolicyAssertionException> preProcessIncludeFragments(Assertion assertion) {

        //list of PolicyAssertionExceptions collected so far
        List<PolicyAssertionException> policyExceptions = new ArrayList<PolicyAssertionException>();
        IncludeAssertionDereferenceTranslator iadt = new IncludeAssertionDereferenceTranslator(policyFinder, new HashSet<String>(), false);

        //determine if this is a composite assertion, if so, then there will be more assertions to traverse.  Recursively
        //call this method on the composite assertion children
        if (assertion instanceof CompositeAssertion) {
            final CompositeAssertion compositeAssertion = (CompositeAssertion) assertion;
            final List children = compositeAssertion.getChildren();
            int numOfChildren = children.size();

            //recursive call on the children assertion
            if (numOfChildren > 0) {
                for (int i = 0; i < numOfChildren; i++) {
                    List<PolicyAssertionException> paeList = preProcessIncludeFragments((Assertion) children.get(i));
                    policyExceptions.addAll(paeList);   //add the policy assertion exception to the list so far
                }
            }
        }

        //if we are at the leaf (the only kid), then we'll try to process this kid
        try {
            iadt.translate(assertion);
        }
        catch (PolicyAssertionException pae) {
            //got policy assertion exception problem, so we'll need to add it to the list
            policyExceptions.add(pae);
        }

        return policyExceptions;
    }

    /**
     * generate possible assertion paths
     *
     * @param assertion the root assertion
     */
    private Set<AssertionPath> generatePaths(Assertion assertion, boolean processIncludes) throws InterruptedException, PolicyAssertionException {
        Set<AssertionPath> assertionPaths = new LinkedHashSet<AssertionPath>();
        Iterator preorder = processIncludes ?
                assertion.preorderIterator(new IncludeAssertionDereferenceTranslator(policyFinder, new HashSet<String>(), false)) :
                assertion.preorderIterator();
        final AssertionPath initPath = new AssertionPath(new Assertion[]{(Assertion)preorder.next()});
        assertionPaths.add(initPath);
        pathStack.push(initPath);

        for (; preorder.hasNext();) {
            if (Thread.interrupted()) throw new InterruptedException();
            Assertion anext = (Assertion)preorder.next();
            if (parentCreatesNewPaths(anext)) {
                AssertionPath cp = pathStack.peek();

                Set<AssertionPath> add = new LinkedHashSet<AssertionPath>();
                Set<AssertionPath> remove = new HashSet<AssertionPath>();

                for( AssertionPath assertionPath : assertionPaths ) {
                    if( assertionPath.contains( anext.getParent() ) &&
                            !containsSibling( assertionPath, anext ) ) {
                        AssertionPath a = assertionPath.addAssertion( anext );
                        add.add( a );
                    }
                    if( Thread.interrupted() ) throw new InterruptedException();
                }
                AssertionPath stackPath = cp.addAssertion(anext);
                add.add(stackPath);

                assertionPaths.removeAll(remove);
                assertionPaths.addAll(add);

                List siblings = anext.getParent().getChildren();
                // if last sibling and the parent path was pushed on stack, pop the parent
                if (siblings.indexOf(anext) + 1 == siblings.size()) {
                    AssertionPath p = pathStack.pop();
                    assertionPaths.remove(p);
                }

                if (isSplitPath(anext)) {
                    pathStack.push(stackPath);
                }

            } else {
                Set<AssertionPath> add = new LinkedHashSet<AssertionPath>();
                Set<AssertionPath> remove = new HashSet<AssertionPath>();

                for( AssertionPath assertionPath : assertionPaths ) {
                    if( assertionPath.contains( anext.getParent() ) ) {
                        AssertionPath a = assertionPath.addAssertion( anext );
                        add.add( a );
                        remove.add( assertionPath );
                    }
                    if( Thread.interrupted() ) throw new InterruptedException();
                }
                AssertionPath[] paths = add.toArray(new AssertionPath[add.size()]);
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

        for( Assertion assertion : path ) {
            if( isSibling( assertion, a ) ) return true;
        }
        return false;
    }

    private boolean isSibling(Assertion assertion, Assertion anext) {
        return assertion.getParent() == anext.getParent() && assertion != anext;
    }

    private Stack<AssertionPath> pathStack = new Stack<AssertionPath>();


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
    private Set<AssertionPath> pruneIncompletePaths(Set<AssertionPath> assertionPaths) {
        Set<AssertionPath> remove = new HashSet<AssertionPath>();

        // the last node is a composite, it has children but the path
        // is not showing that
        // this could be as well done in the second loop. Doing the 'last assertion'
        // check first is a quickshortcut, and happens often
        for( AssertionPath assertionPath : assertionPaths ) {
            final Assertion assertion = assertionPath.lastAssertion();
            if( assertion instanceof CompositeAssertion ) {
                CompositeAssertion ca = (CompositeAssertion) assertion;
                if( !ca.getChildren().isEmpty() ) {
                    remove.add( assertionPath );
                    continue;
                }
            }
            // there is node that is a composite, it has some children, but the next
            // assertion in path is not in the child set
            Assertion[] assertionArr = assertionPath.getPath();
            for( int i = 0; i < assertionArr.length; i++ ) {
                Assertion a = assertionArr[i];
                if( a == assertionPath.lastAssertion() ) {
                    continue;
                }
                if( a instanceof CompositeAssertion ) {
                    CompositeAssertion ca = (CompositeAssertion) a;
                    List children = ca.getChildren();
                    Assertion next = assertionArr[i + 1];
                    if( !children.isEmpty() && !children.contains( next ) ) {
                        remove.add( assertionPath );
                        break;
                    }
                }
            }
        }

        assertionPaths.removeAll(remove);
        return assertionPaths;
    }

    /**
     * Run the given translator on the given assertion and all children.
     */
    private Assertion translate(final Assertion assertion, final AssertionTranslator translator) throws PolicyAssertionException {
        Assertion translated = translator.translate( assertion );

        if ( translated instanceof CompositeAssertion) {
            CompositeAssertion ca = (CompositeAssertion) translated;

            for ( int c=0; c<ca.getChildren().size(); c++ ) {
                Assertion childAssertion = (Assertion) ca.getChildren().get(c);
                Assertion transAssertion = translate(childAssertion, translator);
                if ( childAssertion != transAssertion ) {
                    ca.replaceChild( childAssertion, transAssertion );
                }
            }
        }

        translator.translationFinished(assertion);

        return translated;
    }

    /**
     * default assertion path result holder
     */
    static class DefaultPolicyPathResult implements PolicyPathResult {
        private Set<AssertionPath> assertionPaths;

        public DefaultPolicyPathResult(Set<AssertionPath> paths) {
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
        public Set<AssertionPath> paths() {
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
        return parent == null || parent instanceof OneOrMoreAssertion && !parent.getChildren().isEmpty();
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
        return printPath("** Begin assertion path\n", "** End assertion path", ap);
    }

    private static String printPath(String begin, String end, AssertionPath ap) {
        Assertion[] ass = ap.getPath();
        StringBuffer sb = new StringBuffer(begin);
        for (int i = 0; i < ass.length; i++) {
            if (ass[i] instanceof CompositeAssertion) {
                sb.append("").append(i + 1).append(" ").append(ass[i].getClass().toString()).append('@').append(System.identityHashCode(ass[i]))
                  .append("\n");
            } else {
                sb.append("").append(i + 1).append(" ").append(ass[i]).append("\n");
            }
        }
        sb.append(end);
        return sb.toString();
    }
}
