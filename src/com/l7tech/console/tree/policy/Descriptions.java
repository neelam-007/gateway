package com.l7tech.console.tree.policy;

import com.l7tech.common.util.ConstructorInvocation;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.policy.assertion.xmlsec.RequestWssIntegrity;
import com.l7tech.policy.assertion.xmlsec.ResponseWssIntegrity;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * Supporting class for assertion descriptions.
 * <p/>
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class Descriptions {

    /**
     * the class cannot be instantiated
     */
    private Descriptions() {
    }

    /**
     * Returns the corresponding AssertionDescription instance for
     * an assertion
     *
     * @return the <code>AssertionDescription</code> for a given
     *         assertion
     */
    public static AssertionDescription getDescription(Assertion assertion) {
        if (assertion == null) {
            throw new IllegalArgumentException();
        }
        Class classNode = (Class)descriptionsMap.get(assertion.getClass());
        if (null == classNode) {
            return new UnknonwnAssertionDescription(assertion);
        }

        try {
            return makeDescription(classNode, assertion);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static AssertionDescription makeDescription(Class classNode, Assertion assertion)
      throws InstantiationException, InvocationTargetException, IllegalAccessException {

        ConstructorInvocation ci = new ConstructorInvocation(classNode, new Object[]{assertion});
        return (AssertionDescription)ci.invoke();
    }

    /**
     * the class MemberOfGroupDescription returns the specific assertion
     * description for the MemberOfGroup assertion
     */
    public static class MemberOfGroupDescription extends AssertionDescription {

        public MemberOfGroupDescription(MemberOfGroup a) {
            super(a);
        }

        /**
         * The method returns the assertion parameters for MemeberOfGroup
         * to be used in messages.
         *
         * @return the <CODE>Object[]</CODE> array of assertion parameters
         */
        protected Object[] parameters() {
            return new Object[]{
                ((MemberOfGroup)assertion).getGroupName()
            };
        }
    }

    /**
     * the class SpecificUserDescription returns the specific assertion
     * description for the SpecificUser
     */
    public static class SpecificUserDescription extends AssertionDescription {

        public SpecificUserDescription(SpecificUser a) {
            super(a);
        }

        /**
         * The method returns the assertion parameters for MemeberOfGroup
         * to be used in messages.
         *
         * @return the <CODE>Object[]</CODE> array of assertion parameters
         */
        protected Object[] parameters() {
            SpecificUser userassertion = (SpecificUser)assertion;
            String param = userassertion.getUserLogin();
            if (param == null || param.length() < 1) param = userassertion.getUserName();
            if (param == null || param.length() < 1) param = userassertion.getUserUid();
            return new Object[]{
                param
            };
        }
    }

    /**
     * the class SpecificUserDescription returns the specific assertion
     * description for the SpecificUser
     */
    public static class RoutingDescription extends AssertionDescription {

        public RoutingDescription(RoutingAssertion a) {
            super(a);
        }

        /**
         * The method returns the assertion parameters for MemberOfGroup
         * to be used in messages.
         *
         * @return the <CODE>Object[]</CODE> array of assertion parameters
         */
        protected Object[] parameters() {
            if (assertion instanceof HttpRoutingAssertion) {
                String suffix = "";
                if (assertion instanceof BridgeRoutingAssertion)
                    suffix = " using SecureSpan Bridge";
                return new Object[]{
                    ((HttpRoutingAssertion)assertion).getProtectedServiceUrl() + suffix
                };
            } else if (assertion instanceof JmsRoutingAssertion) {
                JmsRoutingAssertion ass = (JmsRoutingAssertion)assertion;
                String s;
                if (ass.getEndpointOid() == null) {
                    s = " (undefined)";
                } else {
                    String name = ass.getEndpointName();
                    if (name == null)
                        name = "(unnamed)";
                    s = name;
                }
                return new Object[]{s};
            } else {
                return new Object[]{"? using unknown protocol"};
            }
        }
    }

    /**
     * the NoParam assertion description is used for the assertions
     * that do not have any specific (instance) arguments
     */
    public static class NoParam extends AssertionDescription {
        /**
         * the subclasses must implement the
         *
         * @param assertion
         */
        public NoParam(Assertion assertion) {
            super(assertion);
        }

        /**
         * The method returns the empty array of assertion parameters.
         *
         * @return the <CODE>Object[]</CODE> array of assertion
         *         parameters
         */
        protected Object[] parameters() {
            return new Object[]{};
        }
    };

    /**
     * the class UnknonwnAssertionDescription is a 'null object' description
     */
    public static class UnknonwnAssertionDescription extends NoParam {
        /**
         * the subclasses must implement the
         *
         * @param assertion
         */
        public UnknonwnAssertionDescription(Assertion assertion) {
            super(assertion);
        }
    }

    private static Map descriptionsMap = new HashMap();


    // maping assertions to assertion tree nodes
    static {
        descriptionsMap.put(SslAssertion.class, NoParam.class);
        descriptionsMap.put(SpecificUser.class, SpecificUserDescription.class);
        descriptionsMap.put(MemberOfGroup.class, MemberOfGroupDescription.class);
        descriptionsMap.put(OneOrMoreAssertion.class, NoParam.class);
        descriptionsMap.put(AllAssertion.class, NoParam.class);
        descriptionsMap.put(HttpBasic.class, NoParam.class);
        descriptionsMap.put(HttpDigest.class, NoParam.class);
        descriptionsMap.put(HttpRoutingAssertion.class, RoutingDescription.class);
        descriptionsMap.put(JmsRoutingAssertion.class, RoutingDescription.class);
        descriptionsMap.put(RequestWssIntegrity.class, NoParam.class);
        descriptionsMap.put(ResponseWssIntegrity.class, NoParam.class);
    }
}
