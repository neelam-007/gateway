package com.l7tech.console.tree.policy;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.xmlsec.*;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.policy.assertion.credential.http.HttpClientCert;
import com.l7tech.policy.assertion.credential.wss.WssDigest;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.policy.assertion.identity.SpecificUser;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;


/**
 * The class <code>AssertionTreeNodeFactory</code> is a factory
 * class that creates <code>TreeNode</code> instances based on
 * <code>Assertion</code> instances.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.1
 */
public class AssertionTreeNodeFactory {
    private static Map assertionMap = new HashMap();

    // maping assertions to assertion tree nodes
    static {
        assertionMap.put(SslAssertion.class, SslAssertionTreeNode.class);
        assertionMap.put(SpecificUser.class, SpecificUserAssertionTreeNode.class);
        assertionMap.put(MemberOfGroup.class, MemberOfGroupAssertionTreeNode.class);
        assertionMap.put(OneOrMoreAssertion.class, OneOrMoreAssertionTreeNode.class);
        assertionMap.put(AllAssertion.class, AllAssertionTreeNode.class);

        assertionMap.put(HttpBasic.class, HttpBasicAuthAssertionTreeNode.class);
        assertionMap.put(HttpDigest.class, HttpDigestAuthAssertionTreeNode.class);
        assertionMap.put(WssBasic.class, WssBasicAuthAssertionTreeNode.class);
        assertionMap.put(WssDigest.class, WssDigestAuthAssertionTreeNode.class);

        assertionMap.put(HttpClientCert.class, HttpClientCertAssertionTreeNode.class);
        assertionMap.put(RoutingAssertion.class, RoutingAssertionTreeNode.class);
        assertionMap.put(TrueAssertion.class, AnonymousAssertionTreeNode.class);
        assertionMap.put(XmlRequestSecurity.class, XmlRequestSecurityTreeNode.class);
        assertionMap.put(XmlResponseSecurity.class, XmlResponseSecurityTreeNode.class);
        assertionMap.put(RequestXpathAssertion.class, RequestXpathPolicyTreeNode.class);
        assertionMap.put(SamlSecurity.class, SamlTreeNode.class);
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
     * @return the AssertionTreeNode for a given assertion
     */
    public static AssertionTreeNode asTreeNode(Assertion assertion) {
        if (assertion == null) {
            throw new IllegalArgumentException();
        }
        // assertion lookup, find the  assertion tree node
        Class classNode = (Class)assertionMap.get(assertion.getClass());
        if (null == classNode) {
            return new UnknownAssertionTreeNode(assertion);
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
     * the constructor accepting the <code>Aseertion</code> parameter.
     * 
     * @param classNode the class that is a subclass of AssertionTreeNode
     * @param assertion the assertion constructor parameter
     * @return the corresponding assertion tree node
     * @throws InstantiationException    thrown on error instantiating the assertion
     *                                   tree node
     * @throws InvocationTargetException thrown on error invoking the constructor
     * @throws IllegalAccessException    thrown if there is no access to the desired
     *                                   constructor
     * @see Assertions#findMatchingConstructor(Class, Class[])
     */
    private static AssertionTreeNode makeAssertionNode(Class classNode, Assertion assertion)
      throws InstantiationException, InvocationTargetException, IllegalAccessException {

        Constructor ctor =
          Assertions.findMatchingConstructor(classNode, new Class[]{assertion.getClass()});
        if (ctor != null)
            return (AssertionTreeNode)ctor.newInstance(new Object[]{assertion});
        throw new RuntimeException("Cannot locate expected he constructor in " + classNode);

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

        public String getName() {
            return "Unknown assertion " + getUserObject().getClass();
        }
    }
}
