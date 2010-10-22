package com.l7tech.console.tree.policy.advice;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.alert.EmailAlertAssertion;
import com.l7tech.policy.assertion.credential.WsFederationPassiveTokenRequest;
import com.l7tech.policy.assertion.credential.WsTrustCredentialExchange;
import com.l7tech.policy.assertion.credential.XpathCredentialSource;
import com.l7tech.policy.assertion.credential.http.CookieCredentialSourceAssertion;
import com.l7tech.policy.assertion.sla.ThroughputQuota;
import com.l7tech.policy.assertion.xml.SchemaValidation;
import com.l7tech.policy.assertion.xml.XslTransformation;
import com.l7tech.policy.assertion.xmlsec.SamlBrowserArtifact;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;

/**
 *
 */
public class AdvicesTest {

    private static Collection<Class> assertionAdviceWhitelist = Arrays.<Class>asList(
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
            XpathBasedAssertion.class,
            MessageTargetable.class
            );

    /**
     * Test to ensure that any new advice classes are registered using assertion metadata
     */
    @Test
    public void testNoNewAdvices() {
        for ( Class assClass : Advices.getAdviceAssertionClasses() ) {
            Assert.assertTrue(
                    "Advice for '"+assClass+"' should use assertion metadata (see AssertionMetadata.POLICY_ADVICE_CLASSNAME)",
                    assertionAdviceWhitelist.contains( assClass ) );
        }
    }
}
