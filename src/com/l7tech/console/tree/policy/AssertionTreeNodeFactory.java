package com.l7tech.console.tree.policy;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;

import java.util.Map;
import java.util.HashMap;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;


/**
 * The class <code>AssertionTreeNodeFactory</code> is a factory
 * class that creates <code>TreeNode</code> instances based on
 * <code>Assertion</code> instances.
 *
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.1
 */
class AssertionTreeNodeFactory {
    static Map assertionMap = new HashMap();

    // maping assertion to tree nodes to
    static {
        assertionMap.put(SslAssertion.class, SslAssertionTreeNode.class);
        assertionMap.put(SpecificUser.class, SpecificUserAssertionTreeNode.class);
        assertionMap.put(MemberOfGroup.class, MemberOfGroupAssertionTreeNode.class);
        assertionMap.put(OneOrMoreAssertion.class, OneOrMoreAssertionTreeNode.class);
        assertionMap.put(AllAssertion.class, AllAssertionTreeNode.class);
        assertionMap.put(HttpBasic.class, HttpBasicAuthAssertionTreeNode.class);
    }

    /**
     * private constructor, this class cannot be instantiated
     */
    private AssertionTreeNodeFactory() {
    }

    /**
     * Returns the corresponding TreeNode instance for
     * an directory <code>Entry</code>
     *
     * @return the TreeNode for a given Entry
     */
    static AssertionTreeNode asTreeNode(Assertion assertion) {
        if (assertion == null) {
            throw new IllegalArgumentException();
        }

        Class classNode = (Class) assertionMap.get(assertion.getClass());
        if (null == classNode) {
            return new UnknownAssertionTreeNode(assertion);
        }

        try {
            return makeAssertionNode(classNode, assertion);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static AssertionTreeNode makeAssertionNode(Class classNode, Assertion assertion)
      throws InstantiationException, InvocationTargetException, IllegalAccessException {

        Constructor ctor = findMatchingConstructor(classNode, new Class[]{assertion.getClass()});
        if (ctor != null)
            return (AssertionTreeNode) ctor.newInstance(new Object[]{assertion});
        throw new RuntimeException("Cannot locate expected he constructor in " + classNode);

    }

    private static Constructor findMatchingConstructor(Class cls, Class[] params) {
        Constructor[] constructors = cls.getConstructors();
        for (int i = 0; i < constructors.length; i++) {
            if (isAssignable(constructors[i].getParameterTypes(), params)) {
                return constructors[i];
            }
        }
        return null;
    }

    /**
     * special assertion tree node that describesd unknown assertion
     */
    static class UnknownAssertionTreeNode extends LeafAssertionTreeNode {

        public UnknownAssertionTreeNode(Assertion assertion) {
            super(assertion);
        }

        /**
         * subclasses override this method specifying the resource name
         *
         * @param open for nodes that can be opened, can have children
         */
        protected String iconResource(boolean open) {
            return "com/l7tech/console/resources/unknown.gif";
        }

        public String toStirng() {
            return "Unknown assertion " + getUserObject().getClass();
        }
    }


    /**
     * Determine whether the assignTo array accepts assignFrom classes in
     * the given order.
     *
     * {@link Class#isAssignableFrom(Class) is used to determine if the
     * assignTo accepts the parameter from the assignFrom.
     *
     * @param assignTo the array receiving
     * @param assignFrom the class array to check
     * @return true if assignable, false otherwise
     */
    private static boolean isAssignable(Class[] assignTo, Class[] assignFrom) {
        if (assignTo == null) {
            return assignFrom == null || assignFrom.length == 0;
        }

        if (assignFrom == null) {
            return assignTo.length == 0;
        }

        if (assignTo.length != assignFrom.length) {
            return false;
        }

        for (int i = 0; i < assignTo.length; i++) {
            if (!(assignTo[i].isAssignableFrom(assignFrom[i]))) {
                return false;
            }
        }
        return true;
    }
}
