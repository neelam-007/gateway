package com.l7tech.console.tree.policy.advice;

import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.tree.policy.PolicyException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.RequestXpathAssertion;
import com.l7tech.policy.assertion.RoutingAssertion;
import com.l7tech.policy.assertion.RemoteIpRange;
import com.l7tech.policy.assertion.xml.SchemaValidation;
import com.l7tech.policy.assertion.xml.XslTransformation;
import com.l7tech.policy.assertion.xmlsec.RequestWssIntegrity;
import com.l7tech.policy.assertion.xmlsec.ResponseWssIntegrity;

import java.util.*;

/**
 * Supporting class for assertion advices.
 * <p/>
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class Advices {

    /**
     * the class cannot be instantiated
     */
    private Advices() {
    }

    /**
     * Returns the corresponding <code>Advice</code> array for an assertion
     * 
     * @return the <code>AssertionDescription</code> for a given
     *         assertion
     */
    public static Advice[] getAdvices(Assertion assertion) {
        if (assertion == null) {
            throw new IllegalArgumentException();
        }

        try {
            Iterator it = advicesMap.keySet().iterator();
            List advices = new ArrayList();
            for (; it.hasNext();) {
                Class assertionClass = (Class)it.next();
                if (assertionClass.isAssignableFrom(assertion.getClass())) {
                    Class[] adviceClasses = (Class[])advicesMap.get(assertionClass);
                    for (int i = 0; i < adviceClasses.length; i++) {
                        Class adviceClass = adviceClasses[i];
                        advices.add(adviceClass.newInstance());
                    }
                }
            }
            if (advices.isEmpty()) {
                advices.add(new UnknonwnAssertion());
            }
            return (Advice[])advices.toArray(new Advice[]{});
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * the class UnknonwnAssertionAdvice is a 'null object' advice
     */
    static class UnknonwnAssertion implements Advice {
        public UnknonwnAssertion() {
        }

        /**
         * Intercepts a policy change.
         * 
         * @param pc The policy change.
         */
        public void proceed(PolicyChange pc) throws PolicyException {
        }
    }

    private static Map advicesMap = new HashMap();


    // maping assertions to advices, the Class#isAssignable() is used to determine
    // the advice that applies to the assertion
    static {
        advicesMap.put(Assertion.class, new Class[]{PolicyValidatorAdvice.class});
        advicesMap.put(RequestXpathAssertion.class, new Class[]{AddXpathAssertionAdvice.class});
        advicesMap.put(RoutingAssertion.class, new Class[]{AddRoutingAssertionAdvice.class});
        advicesMap.put(SchemaValidation.class, new Class[]{AddSchemaValidationAssertionAdvice.class});
        advicesMap.put(XslTransformation.class, new Class[]{AddXslTransformationAssertionAdvice.class});
        advicesMap.put(RemoteIpRange.class, new Class[]{AddRemoteIpRangeAssertionAdvice.class});
    }
}
