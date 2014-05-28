/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree.policy;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.alert.EmailAlertAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.ExactlyOneAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.assertion.credential.WsFederationPassiveTokenExchange;
import com.l7tech.policy.assertion.credential.WsFederationPassiveTokenRequest;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.policy.assertion.sla.ThroughputQuota;
import com.l7tech.policy.assertion.xml.SchemaValidation;
import com.l7tech.policy.assertion.xml.XslTransformation;
import com.l7tech.util.ConstructorInvocation;
import com.l7tech.util.Functions;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * The class <code>AssertionTreeNodeFactory</code> is a factory
 * class that creates policy <code>TreeNode</code> instances based on
 * <code>Assertion</code> instances.
 */
public class AssertionTreeNodeFactory {
    protected static final Logger logger = Logger.getLogger(AssertionTreeNodeFactory.class.getName());

    private static Map<Class<? extends Assertion>, Class<? extends AssertionTreeNode>> assertionMap = new HashMap<Class<? extends Assertion>, Class<? extends AssertionTreeNode>>();

    // mapping assertions to assertion tree nodes
    static {
        assertionMap.put(SpecificUser.class, SpecificUserAssertionTreeNode.class);
        assertionMap.put(MemberOfGroup.class, MemberOfGroupAssertionTreeNode.class);

        assertionMap.put(RoutingAssertion.class, HttpRoutingAssertionTreeNode.class);
        assertionMap.put(HttpRoutingAssertion.class, HttpRoutingAssertionTreeNode.class);
        assertionMap.put(JmsRoutingAssertion.class, JmsRoutingAssertionTreeNode.class);

        assertionMap.put(RequestXpathAssertion.class, RequestXpathPolicyTreeNode.class);
        assertionMap.put(ResponseXpathAssertion.class, ResponseXpathPolicyTreeNode.class);
        assertionMap.put(CustomAssertionHolder.class, CustomAssertionTreeNode.class);
        assertionMap.put(WsFederationPassiveTokenExchange.class, WsFederationPassiveTokenRequestTreeNode.class);
        assertionMap.put(WsFederationPassiveTokenRequest.class, WsFederationPassiveTokenRequestTreeNode.class);
        assertionMap.put(FaultLevel.class, FaultLevelTreeNode.class);
        assertionMap.put(Include.class, IncludeAssertionPolicyNode.class);
    }

    /**
     * private constructor, this class cannot be instantiated
     */
    private AssertionTreeNodeFactory() {
    }

    /**
     * Returns the corresponding <code>AssertionTreeNode</code> instance
     * for an <code>Assertion</code><br>
     * In case there is no corresponding <code>AssertionTreeNode</code>
     * the <code>UnknownAssertionTreeNode</code> is returned
     * 
     * @param assertion  the assertion for which to create the tree node.  Must not be null.
     * @return the AssertionTreeNode for a given assertion
     */
    public static <AT extends Assertion> AssertionTreeNode<AT> asTreeNode(AT assertion) {
        if (assertion == null) {
            throw new IllegalArgumentException();
        }
        // assertion lookup, find the  assertion tree node
        Class classNode = assertionMap.get(assertion.getClass());
        if (null == classNode) {
            // See if the assertion declares its own tree node factory
            //noinspection unchecked
            Functions.Unary< AssertionTreeNode<AT>, Assertion > factory =
                    (Functions.Unary<AssertionTreeNode<AT>, Assertion>)
                            assertion.meta().get(AssertionMetadata.POLICY_NODE_FACTORY);
            if (factory != null)
                return factory.call(assertion);

            return new UnknownAssertionTreeNode<AT>(assertion);
        }
        try {
            return makeAssertionNode(classNode, assertion);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create the assertion tree node <code>classNode</code> using reflection.
     * The target class is searched for the constructor that accepts the assertion
     * as a parameter.
     * That is, the <code>AssertionTreeNode</code> subclass is searched for
     * the constructor accepting the <code>Assertion</code> parameter.
     * 
     * @param classNode the class that is a subclass of AssertionTreeNode
     * @param assertion the assertion constructor parameter
     * @return the corresponding assertion tree node
     * @throws InstantiationException    thrown on error instantiating the assertion
     *                                   tree node
     * @throws InvocationTargetException thrown on error invoking the constructor
     * @throws IllegalAccessException    thrown if there is no access to the desired
     *                                   constructor
     */
    private static <AT extends Assertion> AssertionTreeNode<AT> makeAssertionNode(Class classNode, AT assertion)
      throws InstantiationException, InvocationTargetException, IllegalAccessException {

        ConstructorInvocation ci = new ConstructorInvocation(classNode, new Object[]{assertion});
        //noinspection unchecked
        return (AssertionTreeNode<AT>)ci.invoke();
    }

    /**
     * special assertion tree node that describes the unknown assertion
     */
    static class UnknownAssertionTreeNode<AT extends Assertion> extends LeafAssertionTreeNode<AT> {
        String name = null;

        public UnknownAssertionTreeNode(AT assertion) {
            super(assertion);
            if (assertion instanceof UnknownAssertion) {
                UnknownAssertion unknownAssertion = (UnknownAssertion)assertion;
                name = unknownAssertion.getDetailMessage();
            }
            if (name == null) {
                name = "Unknown assertion class '" + assertion.getClass() + "'";
            }
        }

        /**
         * subclasses override this method specifying the resource name
         * 
         * @param open for nodes that can be opened, can have children
         */
        @Override
        protected String iconResource(boolean open) {
            return "com/l7tech/console/resources/unknown.gif";
        }

        @Override
        public String getName(final boolean decorate) {
            return name;
        }
    }
}
