package com.l7tech.console.tree.policy;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.RoutingAssertion;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.policy.assertion.xmlsec.XmlEncAssertion;
import com.l7tech.policy.assertion.xmlsec.XmlDsigResAssertion;
import com.l7tech.policy.assertion.xmlsec.XmlDsigReqAssertion;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * Supporting class for assertion descriptions.
 * <p>
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

        Constructor ctor =
          Assertions.findMatchingConstructor(classNode, new Class[]{assertion.getClass()});
        if (ctor != null)
            return (AssertionDescription)ctor.newInstance(new Object[]{assertion});
        throw new RuntimeException("Cannot locate expected he constructor in " + classNode);

    }

    /**
     * the class MemberOfGroupDescription returns the specific assertion
     * description for the MemberOfGroup assertion
     */
    static class MemberOfGroupDescription extends AssertionDescription {

        public MemberOfGroupDescription(MemberOfGroup a) {
            super(a);
        }

        /**
         * The method returns the assertion parameters for MemeberOfGroup
         * to be used in messages.
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
    static class SpecificUserDescription extends AssertionDescription {

        public SpecificUserDescription(SpecificUser a) {
            super(a);
        }
        /**
         * The method returns the assertion parameters for MemeberOfGroup
         * to be used in messages.
         * @return the <CODE>Object[]</CODE> array of assertion parameters
         */
        protected Object[] parameters() {
            return new Object[]{
                ((SpecificUser)assertion).getUserLogin()
            };
        }
    }

    /**
       * the class SpecificUserDescription returns the specific assertion
       * description for the SpecificUser
       */
      static class RoutingDescription extends AssertionDescription {

          public RoutingDescription(RoutingAssertion a) {
              super(a);
          }
          /**
           * The method returns the assertion parameters for MemeberOfGroup
           * to be used in messages.
           * @return the <CODE>Object[]</CODE> array of assertion parameters
           */
          protected Object[] parameters() {
              return new Object[]{
                  ((RoutingAssertion)assertion).getProtectedServiceUrl()
              };
          }
      }

    /**
     * the NoParam assertion description is used for the assertions
     * that do not have any specific (instance) arguments
     */
    static class NoParam extends AssertionDescription {
        /**
         * the subclasses must implement the
         * @param assertion
         */
        public NoParam(Assertion assertion) {
            super(assertion);
        }

        /**
         * The method returns the empty array of assertion parameters.

         * @return the <CODE>Object[]</CODE> array of assertion
         * parameters
         */
        protected Object[] parameters() {
            return new Object[]{};
        }
    };

    /**
     * the class UnknonwnAssertionDescription is a 'null object' description
     */
    static class UnknonwnAssertionDescription extends NoParam {
        /**
         * the subclasses must implement the
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
        descriptionsMap.put(RoutingAssertion.class, RoutingDescription.class);
        descriptionsMap.put(XmlEncAssertion.class, NoParam.class);
        descriptionsMap.put(XmlDsigReqAssertion.class, NoParam.class);
        descriptionsMap.put(XmlDsigResAssertion.class, NoParam.class);
    }
}
