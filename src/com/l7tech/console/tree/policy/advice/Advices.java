package com.l7tech.console.tree.policy.advice;

import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.alert.EmailAlertAssertion;
import com.l7tech.policy.assertion.credential.WsFederationPassiveTokenRequest;
import com.l7tech.policy.assertion.credential.WsTrustCredentialExchange;
import com.l7tech.policy.assertion.credential.XpathCredentialSource;
import com.l7tech.policy.assertion.credential.http.CookieCredentialSourceAssertion;
import com.l7tech.policy.assertion.sla.ThroughputQuota;
import com.l7tech.policy.assertion.xml.SchemaValidation;
import com.l7tech.policy.assertion.xml.XslTransformation;
import com.l7tech.policy.assertion.xmlsec.RequestWssSaml;
import com.l7tech.policy.assertion.xmlsec.SamlBrowserArtifact;

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
            List<Advice> advices = new ArrayList<Advice>();
            for (Class<? extends Assertion> assertionClass : advicesMap.keySet()) {
                if (assertionClass.isAssignableFrom(assertion.getClass())) {
                    Collection<Class<? extends Advice>> adviceClasses = advicesMap.get(assertionClass);
                    for (Class<? extends Advice> adviceClass : adviceClasses) {
                        advices.add(adviceClass.newInstance());
                    }
                }
            }

            Advice advice = (Advice)assertion.meta().get(AssertionMetadata.POLICY_ADVICE_INSTANCE);
            if (advice != null) advices.add(advice);

            if (advices.isEmpty()) advices.add(new UnknownAssertionAdvice());
            
            return advices.toArray(new Advice[0]);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * the class UnknonwnAssertionAdvice is a 'null object' advice
     */
    static class UnknownAssertionAdvice implements Advice {
        public UnknownAssertionAdvice() {
        }

        /**
         * Intercepts a policy change.
         * 
         * @param pc The policy change.
         */
        public void proceed(PolicyChange pc) {
        }
    }

    private static Collection<Class<? extends Advice>> ary(Class<? extends Advice> c) {
        //noinspection RedundantTypeArguments
        return Arrays.<Class<? extends Advice>>asList(c);
    }

    // maping assertions to advices, the Class#isAssignable() is used to determine
    // the advice that applies to the assertion
    private static Map<Class<? extends Assertion>, Collection<Class<? extends Advice>>> advicesMap =
            new HashMap<Class<? extends Assertion>, Collection<Class<? extends Advice>>>() {{
                put(Assertion.class, ary(PolicyValidatorAdvice.class));
                put(RoutingAssertion.class, ary(AddRoutingAssertionAdvice.class));
                put(SchemaValidation.class, ary(AddSchemaValidationAssertionAdvice.class));
                put(XslTransformation.class, ary(AddXslTransformationAssertionAdvice.class));
                put(RemoteIpRange.class, ary(AddRemoteIpRangeAssertionAdvice.class));
                put(TimeRange.class, ary(AddTimeRangeAssertionAdvice.class));
                put(RequestSwAAssertion.class, ary(AddRequestSwAAssertionAdvice.class));
                put(RequestWssSaml.class, ary(AddRequestWssSamlAdvice.class));
                put(WsTrustCredentialExchange.class, ary(AddWsTrustCredentialExchangeAdvice.class));
                put(WsFederationPassiveTokenRequest.class, ary(AddWsFederationPassiveTokenRequestAdvice.class));
                put(XpathCredentialSource.class, ary(AddXpathCredentialSourceAdvice.class));
                put(SamlBrowserArtifact.class, ary(AddSamlBrowserArtifactAdvice.class));
                put(Regex.class, ary(RegexAdvice.class));
                put(HttpFormPost.class, ary(HttpFormPostAdvice.class));
                put(InverseHttpFormPost.class, ary(InverseHttpFormPostAdvice.class));
                put(EmailAlertAssertion.class, ary(AddEmailAlertAssertionAdvice.class));
                put(ThroughputQuota.class, ary(AddThroughputQuotaAssertionAdvice.class));
                put(CommentAssertion.class, ary(CommentAssertionAdvice.class));
                put(SqlAttackAssertion.class, ary(SqlAttackAssertionAdvice.class));
                put(OversizedTextAssertion.class, ary(OversizedTextAssertionAdvice.class));
                put(RequestSizeLimit.class, ary(RequestSizeLimitAdvice.class));
                put(AuditDetailAssertion.class, ary(AddAuditAdviceAssertion.class));
                put(Operation.class, ary(AddWSDLOperationAssertionAdvice.class));
                put(SetVariableAssertion.class, ary(SetVariableAssertionAdvice.class));
                put(CookieCredentialSourceAssertion.class, ary(AddCookieCredentialSourceAssertionAdvice.class));
                put(WsiBspAssertion.class, ary(AddWsiBspAssertionAdvice.class));
                put(WsiSamlAssertion.class, ary(AddWsiSamlAssertionAdvice.class));
                put(HtmlFormDataAssertion.class, ary(HtmlFormDataAssertionAdvice.class));
                put(CodeInjectionProtectionAssertion.class, ary(CodeInjectionProtectionAssertionAdvice.class));
                put(SimpleXpathAssertion.class, ary(AddXPathAssertionAdvice.class));
                put(SamlIssuerAssertion.class, ary(AddSamlIssuerAssertionAdvice.class));
            }};
}
