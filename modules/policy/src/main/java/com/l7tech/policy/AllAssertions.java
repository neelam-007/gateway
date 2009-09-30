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
import com.l7tech.policy.assertion.credential.wss.WssDigest;
import com.l7tech.policy.assertion.identity.AuthenticationAssertion;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.policy.assertion.sla.ThroughputQuota;
import com.l7tech.policy.assertion.transport.PreemptiveCompression;
import com.l7tech.policy.assertion.transport.RemoteDomainIdentityInjection;
import com.l7tech.policy.assertion.xml.SchemaValidation;
import com.l7tech.policy.assertion.xml.XslTransformation;
import com.l7tech.policy.assertion.xml.RemoveElement;
import com.l7tech.policy.assertion.xmlsec.*;

import java.util.Collections;

/**
 * The canonical list of legacy assertions known to a new AssertionRegistry instance on startup.
 *
 * @author alex
 */
public class AllAssertions {

    /**
     * all assertions that the gateway must handle
     * TODO wrap and protect this currently-mutable array
     */
    public static Assertion[] GATEWAY_EVERYTHING = new Assertion[]{
        new TrueAssertion(),
        new FalseAssertion(),
        new HttpBasic(),
        new HttpDigest(),
        new HttpNegotiate(),
        new WssBasic(),
        new WssDigest(),
        new AllAssertion(Collections.singletonList(new FalseAssertion())),
        new ExactlyOneAssertion(Collections.singletonList(new FalseAssertion())),
        new OneOrMoreAssertion(Collections.singletonList(new FalseAssertion())),
        new SslAssertion(),
        new HttpRoutingAssertion(),
        new BridgeRoutingAssertion(),
        new JmsRoutingAssertion(),
        new MemberOfGroup(),
        new SpecificUser(),
        new RequireWssX509Cert(),
        new RequireWssSignedElement(),
        new RequireWssEncryptedElement(),
        new WssSignElement(),
        new WssEncryptElement(),
        new RequestXpathAssertion(),
        new ResponseXpathAssertion(),
        new WssReplayProtection(),
        new RequestSwAAssertion(),
        new RequireWssSaml(),
        new RequireWssTimestamp(),
        new AddWssTimestamp(),
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
        new Include(),
        new WssVersionAssertion(),
        new WsSecurity(),
        new RemoveElement(),
        new AddWssUsernameToken(),
            new AuditRecordToXmlAssertion(),
    };


    /**
     * all assertions that the serialization must handle
     * TODO wrap and protect this currently-mutable array
     */
    public static Assertion[] SERIALIZABLE_EVERYTHING = new Assertion[]{
        new HttpBasic(),
        new HttpDigest(),
        new HttpNegotiate(),
        new WssBasic(),
        new WssDigest(),
        new AllAssertion(Collections.singletonList(new FalseAssertion())),    // Empty composites are not valid
        new ExactlyOneAssertion(Collections.singletonList(new FalseAssertion())), // Empty composites are not valid
        new OneOrMoreAssertion(Collections.singletonList(new FalseAssertion())), // Empty composites are not valid
        new FalseAssertion(),
        new SslAssertion(),
        new HttpRoutingAssertion(),
        new BridgeRoutingAssertion(),
        new JmsRoutingAssertion(),
        new TrueAssertion(),
        new MemberOfGroup(),
        new SpecificUser(),
        new RequireWssX509Cert(),
        new SecureConversation(),
        new RequireWssSignedElement(),
        new RequireWssEncryptedElement(),
        new WssSignElement(),
        new WssEncryptElement(),
        new RequestXpathAssertion(),
        new ResponseXpathAssertion(),
        new WssReplayProtection(),
        new RequestSwAAssertion(),
        new RequireWssSaml(),
        new RequireWssTimestamp(),
        new AddWssTimestamp(),
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
        new RequireWssSaml2(),
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
        new AddWssSecurityToken(),
        new SetVariableAssertion(),
        new CookieCredentialSourceAssertion(),
        new HtmlFormDataAssertion(),
        new CodeInjectionProtectionAssertion(),
        new AuthenticationAssertion(),
        new WsiSamlAssertion(),
        new SamlIssuerAssertion(),
        new Include(),
        new WssVersionAssertion(),
        new PreemptiveCompression(),
        new RemoteDomainIdentityInjection(),
        new WsSecurity(),
        new RemoveElement(),
        new AddWssUsernameToken(),
        new AuditRecordToXmlAssertion(),
    };
}
