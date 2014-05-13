package com.l7tech.console.tree.policy.advice;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.alert.EmailAlertAssertion;
import com.l7tech.policy.assertion.credential.WsFederationPassiveTokenRequest;
import com.l7tech.policy.assertion.credential.http.CookieCredentialSourceAssertion;
import com.l7tech.policy.assertion.sla.ThroughputQuota;
import com.l7tech.policy.assertion.xml.SchemaValidation;
import com.l7tech.policy.assertion.xml.XslTransformation;

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
     * @param assertion the assertion whose advice to find
     * @return the <code>AssertionDescription</code> for a given
     *         assertion
     */
    public static Advice[] getAdvices(Assertion assertion) {
        if (assertion == null) {
            throw new IllegalArgumentException();
        }

        try {
            List<Advice> advices = new ArrayList<>();
            for (Class<?> assertionClass : advicesMap.keySet()) {
                if (assertionClass.isAssignableFrom(assertion.getClass())) {
                    Collection<Class<? extends Advice>> adviceClasses = advicesMap.get(assertionClass);
                    for (Class<? extends Advice> adviceClass : adviceClasses) {
                        advices.add(adviceClass.newInstance());
                    }
                }
            }

            Advice advice = assertion.meta().get(AssertionMetadata.POLICY_ADVICE_INSTANCE);
            if (advice != null) advices.add(advice);

            if (Boolean.TRUE.equals(assertion.meta().get(AssertionMetadata.POLICY_VALIDATION_ADVICE_ENABLED))) {
                advices.add(new PolicyValidatorAdvice());
            }

            return advices.toArray(new Advice[advices.size()]);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static Set<Class<?>> getAdviceAssertionClasses() {
        return Collections.unmodifiableSet( advicesMap.keySet() );    
    }

    private static Collection<Class<? extends Advice>> ary(Class<? extends Advice> c) {
        //noinspection RedundantTypeArguments
        return Arrays.<Class<? extends Advice>>asList(c);
    }

    // maping assertions to advices, the Class#isAssignable() is used to determine
    // the advice that applies to the assertion
    private static Map<Class<?>, Collection<Class<? extends Advice>>> advicesMap =
            new HashMap<Class<?>, Collection<Class<? extends Advice>>>() {{
                put(RoutingAssertion.class, ary(AddRoutingAssertionAdvice.class));
                put(SchemaValidation.class, ary(AddSchemaValidationAssertionAdvice.class));
                put(XslTransformation.class, ary(AddXslTransformationAssertionAdvice.class));
                put(RemoteIpRange.class, ary(AddRemoteIpRangeAssertionAdvice.class));
                put(TimeRange.class, ary(AddTimeRangeAssertionAdvice.class));
                put(RequestSwAAssertion.class, ary(AddRequestSwAAssertionAdvice.class));
                put(WsFederationPassiveTokenRequest.class, ary(AddWsFederationPassiveTokenRequestAdvice.class));
                put(Regex.class, ary(RegexAdvice.class));
                put(HttpFormPost.class, ary(HttpFormPostAdvice.class));
                put(InverseHttpFormPost.class, ary(InverseHttpFormPostAdvice.class));
                put(EmailAlertAssertion.class, ary(AddEmailAlertAssertionAdvice.class));
                put(ThroughputQuota.class, ary(AddThroughputQuotaAssertionAdvice.class));
                put(CommentAssertion.class, ary(CommentAssertionAdvice.class));
                put(RequestSizeLimit.class, ary(RequestSizeLimitAdvice.class));
                put(AuditDetailAssertion.class, ary(AddAuditAdviceAssertion.class));
                put(Operation.class, ary(AddWSDLOperationAssertionAdvice.class));
                put(SetVariableAssertion.class, ary(SetVariableAssertionAdvice.class));
                put(CookieCredentialSourceAssertion.class, ary(AddCookieCredentialSourceAssertionAdvice.class));
                put(WsiBspAssertion.class, ary(AddWsiBspAssertionAdvice.class));
                put(WsiSamlAssertion.class, ary(AddWsiSamlAssertionAdvice.class));
                put(HtmlFormDataAssertion.class, ary(HtmlFormDataAssertionAdvice.class));
                put(XpathBasedAssertion.class, ary(AddXPathAssertionAdvice.class));
                put(MessageTargetable.class, ary(MessageTargetableAdvice.class));
            }};
}
