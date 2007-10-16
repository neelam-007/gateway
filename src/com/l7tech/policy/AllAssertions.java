/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */

package com.l7tech.policy;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.alert.EmailAlertAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.ExactlyOneAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.assertion.credential.WsFederationPassiveTokenExchange;
import com.l7tech.policy.assertion.credential.WsFederationPassiveTokenRequest;
import com.l7tech.policy.assertion.credential.WsTrustCredentialExchange;
import com.l7tech.policy.assertion.credential.XpathCredentialSource;
import com.l7tech.policy.assertion.credential.http.CookieCredentialSourceAssertion;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.policy.assertion.credential.http.HttpNegotiate;
import com.l7tech.policy.assertion.credential.wss.EncryptedUsernameTokenAssertion;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.policy.assertion.identity.AuthenticationAssertion;
import com.l7tech.policy.assertion.identity.MappingAssertion;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.policy.assertion.sla.ThroughputQuota;
import com.l7tech.policy.assertion.xml.SchemaValidation;
import com.l7tech.policy.assertion.xml.XslTransformation;
import com.l7tech.policy.assertion.xmlsec.*;

import java.util.Arrays;

/**
 * The canonical list of legacy assertions known to a new AssertionRegistry instance on startup.
 *
 * @author alex
 */
public class AllAssertions {

    /**
     * the assertions that the agent (ssa) uses
     */
    public static Assertion[] BRIDGE_EVERYTHING = new Assertion[]{
        new HttpBasic(),
        new HttpDigest(),
        new WssBasic(),
        new EncryptedUsernameTokenAssertion(),
        new AllAssertion(),
        new ExactlyOneAssertion(),
        new OneOrMoreAssertion(),
        new FalseAssertion(),
        new SslAssertion(),
        new TrueAssertion(),
        new RequestWssX509Cert(),
        new SecureConversation(),
        new RequestWssIntegrity(),
        new RequestWssConfidentiality(),
        new ResponseWssIntegrity(),
        new ResponseWssConfidentiality(),
        // new RequestXpathAssertion(),  // support removed from Bridge by Franco: fla, please leave commented - this is causing mucho problems
        // new ResponseXpathAssertion(), // support removed from Bridge by Franco: fla, please leave commented - this is causing mucho problems
        new RequestWssReplayProtection(),
        new RequestWssKerberos(),
        new CookieCredentialSourceAssertion(),
        new RequestWssSaml(),
        new RequestWssSaml2(),
    };

    /**
     * all assertions that the gateway must handle
     */
    public static Assertion[] GATEWAY_EVERYTHING = new Assertion[]{
        new TrueAssertion(),
        new FalseAssertion(),
        new HttpBasic(),
        new HttpDigest(),
        new HttpNegotiate(),
        new WssBasic(),
        new AllAssertion(),
        new ExactlyOneAssertion(),
        new OneOrMoreAssertion(),
        new SslAssertion(),
        new HttpRoutingAssertion(),
        new BridgeRoutingAssertion(),
        new JmsRoutingAssertion(),
        new MemberOfGroup(),
        new SpecificUser(),
        new RequestWssX509Cert(),
        new RequestWssIntegrity(),
        new RequestWssConfidentiality(),
        new ResponseWssIntegrity(),
        new ResponseWssConfidentiality(),
        new RequestXpathAssertion(),
        new ResponseXpathAssertion(),
        new RequestWssReplayProtection(),
        new RequestSwAAssertion(),
        new RequestWssSaml(),
        new RequestWssTimestamp(),
        new ResponseWssTimestamp(),
        new AuditAssertion(),
        new WsTrustCredentialExchange(),
        new WsFederationPassiveTokenExchange(),
        new WsFederationPassiveTokenRequest(),
        new EncryptedUsernameTokenAssertion(),
        new Regex(),
        new UnknownAssertion(),
        new ThroughputQuota(),
        new EmailAlertAssertion(),
        new CommentAssertion(),
        new SqlAttackAssertion(),
        new OversizedTextAssertion(),
        new RequestSizeLimit(),
        new RequestWssKerberos(),
        new WsiBspAssertion(),
        new WsiSamlAssertion(),
        new WsspAssertion(),
        new CookieCredentialSourceAssertion(),
        new XpathCredentialSource(),
        new AuthenticationAssertion(),
        new HtmlFormDataAssertion(),
        new CodeInjectionProtectionAssertion(),
        new SamlIssuerAssertion(),
    };


    /**
     * all assertions that the serialization must handle
     */
    public static Assertion[] SERIALIZABLE_EVERYTHING = new Assertion[]{
        new HttpBasic(),
        new HttpDigest(),
        new HttpNegotiate(),
        new WssBasic(),
        new AllAssertion(Arrays.asList(new FalseAssertion())),    // Empty composites are not valid
        new ExactlyOneAssertion(Arrays.asList(new FalseAssertion())), // Empty composites are not valid
        new OneOrMoreAssertion(Arrays.asList(new FalseAssertion())), // Empty composites are not valid
        new FalseAssertion(),
        new SslAssertion(),
        new HttpRoutingAssertion(),
        new BridgeRoutingAssertion(),
        new JmsRoutingAssertion(),
        new TrueAssertion(),
        new MemberOfGroup(),
        new SpecificUser(),
        new RequestWssX509Cert(),
        new SecureConversation(),
        new RequestWssIntegrity(),
        new RequestWssConfidentiality(),
        new ResponseWssIntegrity(),
        new ResponseWssConfidentiality(),
        new RequestXpathAssertion(),
        new ResponseXpathAssertion(),
        new RequestWssReplayProtection(),
        new RequestSwAAssertion(),
        new RequestWssSaml(),
        new RequestWssTimestamp(),
        new ResponseWssTimestamp(),
        new AuditAssertion(),
        new CustomAssertionHolder(),
        new WsTrustCredentialExchange(),
        new WsFederationPassiveTokenExchange(),
        new WsFederationPassiveTokenRequest(),
        new Regex(),
        new UnknownAssertion(),
        new ThroughputQuota(),
        new EmailAlertAssertion(),
        new CommentAssertion(),
        new SqlAttackAssertion(),
        new OversizedTextAssertion(),
        new RequestSizeLimit(),
        new RequestWssKerberos(),
        new EncryptedUsernameTokenAssertion(),
        new WsiBspAssertion(),
        new WsspAssertion(),
        new FaultLevel(),
        new RequestWssSaml2(),
        new SchemaValidation(),
        new XslTransformation(),
        new TimeRange(),
        new RemoteIpRange(),
        new AuditDetailAssertion(),
        new XpathCredentialSource(),
        new SamlBrowserArtifact(),
        new HttpFormPost(),
        new InverseHttpFormPost(),
        new Operation(),
        new HardcodedResponseAssertion(),
        new ResponseWssSecurityToken(),
        new MappingAssertion(),
        new SetVariableAssertion(),
        new CookieCredentialSourceAssertion(),
        new HtmlFormDataAssertion(),
        new CodeInjectionProtectionAssertion(),
        new AuthenticationAssertion(),
        new WsiSamlAssertion(),
        new SamlIssuerAssertion(),
    };
}
