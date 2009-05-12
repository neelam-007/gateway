package com.l7tech.console.tree.policy.advice;

import org.junit.Test;
import org.junit.Assert;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.RoutingAssertion;
import com.l7tech.policy.assertion.CommentAssertion;
import com.l7tech.policy.assertion.SetVariableAssertion;
import com.l7tech.policy.assertion.SqlAttackAssertion;
import com.l7tech.policy.assertion.HtmlFormDataAssertion;
import com.l7tech.policy.assertion.HttpFormPost;
import com.l7tech.policy.assertion.WsiSamlAssertion;
import com.l7tech.policy.assertion.CodeInjectionProtectionAssertion;
import com.l7tech.policy.assertion.RemoteIpRange;
import com.l7tech.policy.assertion.WsiBspAssertion;
import com.l7tech.policy.assertion.TimeRange;
import com.l7tech.policy.assertion.SimpleXpathAssertion;
import com.l7tech.policy.assertion.InverseHttpFormPost;
import com.l7tech.policy.assertion.AuditDetailAssertion;
import com.l7tech.policy.assertion.RequestSizeLimit;
import com.l7tech.policy.assertion.Operation;
import com.l7tech.policy.assertion.RequestSwAAssertion;
import com.l7tech.policy.assertion.Regex;
import com.l7tech.policy.assertion.OversizedTextAssertion;
import com.l7tech.policy.assertion.xmlsec.SamlBrowserArtifact;
import com.l7tech.policy.assertion.sla.ThroughputQuota;
import com.l7tech.policy.assertion.xml.XslTransformation;
import com.l7tech.policy.assertion.xml.SchemaValidation;
import com.l7tech.policy.assertion.credential.http.CookieCredentialSourceAssertion;
import com.l7tech.policy.assertion.credential.WsFederationPassiveTokenRequest;
import com.l7tech.policy.assertion.credential.XpathCredentialSource;
import com.l7tech.policy.assertion.credential.WsTrustCredentialExchange;
import com.l7tech.policy.assertion.alert.EmailAlertAssertion;

import java.util.*;

/**
 *
 */
public class AdvicesTest {

    private static Collection<Class<? extends Assertion>> assertionAdviceWhitelist = Arrays.asList(
            Assertion.class,
            RoutingAssertion.class,
            SchemaValidation.class,
            XslTransformation.class,
            RemoteIpRange.class,
            TimeRange.class,
            RequestSwAAssertion.class,
            WsTrustCredentialExchange.class,
            WsFederationPassiveTokenRequest.class,
            XpathCredentialSource.class,
            SamlBrowserArtifact.class,
            Regex.class,
            HttpFormPost.class,
            InverseHttpFormPost.class, 
            EmailAlertAssertion.class,
            ThroughputQuota.class,
            CommentAssertion.class,
            SqlAttackAssertion.class,
            OversizedTextAssertion.class,
            RequestSizeLimit.class,
            AuditDetailAssertion.class,
            Operation.class,
            SetVariableAssertion.class,
            CookieCredentialSourceAssertion.class,
            WsiBspAssertion.class,
            WsiSamlAssertion.class,
            HtmlFormDataAssertion.class,
            CodeInjectionProtectionAssertion.class,
            SimpleXpathAssertion.class
            );

    /**
     * Test to ensure that any new advice classes are registered using assertion metadata
     */
    @Test
    public void testNoNewAdvices() {
        for ( Class<? extends Assertion> assClass : Advices.getAdviceAssertionClasses() ) {
            Assert.assertTrue(
                    "Advice for '"+assClass+"' should use assertion metadata (see AssertionMetadata.POLICY_ADVICE_CLASSNAME)",
                    assertionAdviceWhitelist.contains( assClass ) );
        }
    }
}
