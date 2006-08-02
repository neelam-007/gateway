package com.l7tech.console.tree.policy.advice;

import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.tree.policy.PolicyException;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.identity.MappingAssertion;
import com.l7tech.policy.assertion.sla.ThroughputQuota;
import com.l7tech.policy.assertion.alert.EmailAlertAssertion;
import com.l7tech.policy.assertion.alert.SnmpTrapAssertion;
import com.l7tech.policy.assertion.credential.WsTrustCredentialExchange;
import com.l7tech.policy.assertion.credential.XpathCredentialSource;
import com.l7tech.policy.assertion.credential.WsFederationPassiveTokenRequest;
import com.l7tech.policy.assertion.credential.http.CookieCredentialSourceAssertion;
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
     * @return the <code>AssertionDescription</code> for a given
     *         assertion
     */
    public static Advice[] getAdvices(Assertion assertion) {
        if (assertion == null) {
            throw new IllegalArgumentException();
        }

        try {
            List advices = new ArrayList();
            for (Class<? extends Assertion> assertionClass : advicesMap.keySet()) {
                if (assertionClass.isAssignableFrom(assertion.getClass())) {
                    Class[] adviceClasses = advicesMap.get(assertionClass);
                    for (Class adviceClass : adviceClasses) {
                        advices.add(adviceClass.newInstance());
                    }
                }
            }

            if (advices.isEmpty()) {
                advices.add(new UnknonwnAssertion()); // TODO what the hell?
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

    private static Map<Class<? extends Assertion>, Class[]> advicesMap = new HashMap<Class<? extends Assertion>, Class[]>();


    // maping assertions to advices, the Class#isAssignable() is used to determine
    // the advice that applies to the assertion
    static {
        advicesMap.put(Assertion.class, new Class[]{PolicyValidatorAdvice.class});
        advicesMap.put(RoutingAssertion.class, new Class[]{AddRoutingAssertionAdvice.class});
        advicesMap.put(SchemaValidation.class, new Class[]{AddSchemaValidationAssertionAdvice.class});
        advicesMap.put(XslTransformation.class, new Class[]{AddXslTransformationAssertionAdvice.class});
        advicesMap.put(RemoteIpRange.class, new Class[]{AddRemoteIpRangeAssertionAdvice.class});
        advicesMap.put(TimeRange.class, new Class[]{AddTimeRangeAssertionAdvice.class});
        advicesMap.put(RequestSwAAssertion.class, new Class[] {AddRequestSwAAssertionAdvice.class});
        advicesMap.put(RequestWssSaml.class, new Class[] {AddRequestWssSamlAdvice.class});
        advicesMap.put(WsTrustCredentialExchange.class, new Class[] {AddWsTrustCredentialExchangeAdvice.class});
        advicesMap.put(WsFederationPassiveTokenRequest.class, new Class[] {AddWsFederationPassiveTokenRequestAdvice.class});
        advicesMap.put(XpathCredentialSource.class, new Class[] {AddXpathCredentialSourceAdvice.class});
        advicesMap.put(SamlBrowserArtifact.class, new Class[] {AddSamlBrowserArtifactAdvice.class});
        advicesMap.put(Regex.class, new Class[] {RegexAdvice.class});
        advicesMap.put(HttpFormPost.class, new Class[] {HttpFormPostAdvice.class});
        advicesMap.put(SnmpTrapAssertion.class, new Class[] {AddSnmpTrapAssertionAdvice.class});
        advicesMap.put(InverseHttpFormPost.class, new Class[] {InverseHttpFormPostAdvice.class});
        advicesMap.put(EmailAlertAssertion.class, new Class[] {AddEmailAlertAssertionAdvice.class});
        advicesMap.put(ThroughputQuota.class, new Class[]{AddThroughputQuotaAssertionAdvice.class});
        advicesMap.put(CommentAssertion.class, new Class[]{CommentAssertionAdvice.class});
        advicesMap.put(ComparisonAssertion.class, new Class[]{ComparisonAssertionAdvice.class});
        advicesMap.put(SqlAttackAssertion.class, new Class[]{SqlAttackAssertionAdvice.class});
        advicesMap.put(OversizedTextAssertion.class, new Class[]{OversizedTextAssertionAdvice.class});
        advicesMap.put(RequestSizeLimit.class, new Class[]{RequestSizeLimitAdvice.class});
        advicesMap.put(HardcodedResponseAssertion.class, new Class[]{HardcodedResponseAssertionAdvice.class});
        advicesMap.put(MappingAssertion.class, new Class[] {MappingAssertionAdvice.class});
        advicesMap.put(AuditDetailAssertion.class, new Class[] {AddAuditAdviceAssertion.class});
        advicesMap.put(Operation.class, new Class[]{AddWSDLOperationAssertionAdvice.class});
        advicesMap.put(SetVariableAssertion.class, new Class[]{SetVariableAssertionAdvice.class});
        advicesMap.put(CookieCredentialSourceAssertion.class, new Class[]{AddCookieCredentialSourceAssertionAdvice.class});
        advicesMap.put(WsiBspAssertion.class, new Class[]{AddWsiBspAssertionAdvice.class});
        advicesMap.put(WsiSamlAssertion.class, new Class[]{AddWsiSamlAssertionAdvice.class});
        advicesMap.put(EchoRoutingAssertion.class, new Class[]{AddEchoRoutingAssertionAdvice.class});
    }
}
