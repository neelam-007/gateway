/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.alert.EmailAlertAssertion;
import com.l7tech.policy.assertion.alert.SnmpTrapAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.ExactlyOneAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.assertion.credential.WsTrustCredentialExchange;
import com.l7tech.policy.assertion.credential.XpathCredentialSource;
import com.l7tech.policy.assertion.credential.WsFederationPassiveTokenRequest;
import com.l7tech.policy.assertion.credential.WsFederationPassiveTokenExchange;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.policy.assertion.credential.wss.EncryptedUsernameTokenAssertion;
import com.l7tech.policy.assertion.ext.Category;
import com.l7tech.policy.assertion.ext.CustomAssertion;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.policy.assertion.sla.ThroughputQuota;
import com.l7tech.policy.assertion.xmlsec.*;

import java.util.Arrays;

/**
 * @author alex
 * @version $Revision$
 */
public class AllAssertions {

    /**
     * the assertions that the agent (ssa) uses
     */
    public static Assertion[] BRIDGE_EVERYTHING = new Assertion[]{
        new HttpBasic(),
        new HttpDigest(),
        new WssBasic(),
        new AllAssertion(),
        new ExactlyOneAssertion(),
        new OneOrMoreAssertion(),
        new FalseAssertion(),
        new SslAssertion(),
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
        new RequestWssKerberos(),
    };

    private static CustomAssertionHolder CUSTOM_ASSERTION_HOLDER = new CustomAssertionHolder();

    static {
        CUSTOM_ASSERTION_HOLDER.setCustomAssertion(new CustomAssertion() {
            public String getName() {
                return "test custom assertion";
            }
        });
        CUSTOM_ASSERTION_HOLDER.setCategory(Category.ACCESS_CONTROL);
    }

    /**
     * all assertions that the gateway must handle
     */
    public static Assertion[] GATEWAY_EVERYTHING = new Assertion[]{
        new HttpBasic(),
        new HttpDigest(),
        new WssBasic(),
        new AllAssertion(),
        new ExactlyOneAssertion(),
        new OneOrMoreAssertion(),
        new FalseAssertion(),
        new SslAssertion(),
        new HttpRoutingAssertion(),
        new BridgeRoutingAssertion(),
        new JmsRoutingAssertion(),
        new TrueAssertion(),
        new MemberOfGroup(),
        new SpecificUser(),
        new RequestWssIntegrity(),
        new RequestWssConfidentiality(),
        new ResponseWssIntegrity(),
        new ResponseWssConfidentiality(),
        new RequestXpathAssertion(),
        new ResponseXpathAssertion(),
        new RequestWssReplayProtection(),
        new RequestSwAAssertion(),
        new RequestWssSaml(),
        new AuditAssertion(),
        new WsTrustCredentialExchange(),
        new WsFederationPassiveTokenExchange(),
        new WsFederationPassiveTokenRequest(),
        new EncryptedUsernameTokenAssertion(),
        CUSTOM_ASSERTION_HOLDER,
        new Regex(),
        new UnknownAssertion(),
        new SnmpTrapAssertion(),
        new ThroughputQuota(),
        new EmailAlertAssertion(),
        new CommentAssertion(),
        new ComparisonAssertion(),
        new StealthFault(),
        new SqlAttackAssertion(),
        new OversizedTextAssertion(),
        new RequestSizeLimit(),
        new RequestWssKerberos(),
        new WsiBspAssertion(),
        new WsiSamlAssertion(),
        new EchoRoutingAssertion(),
    };


    /**
     * all assertions that the serialization must handle
     */
    public static Assertion[] SERIALIZABLE_EVERYTHING = new Assertion[]{
        new HttpBasic(),
        new HttpDigest(),
        new WssBasic(),
        new AllAssertion(Arrays.asList(new Assertion[] {new FalseAssertion()})),    // Empty composites are not valid
        new ExactlyOneAssertion(Arrays.asList(new Assertion[] {new FalseAssertion()})), // Empty composites are not valid
        new OneOrMoreAssertion(Arrays.asList(new Assertion[] {new FalseAssertion()})), // Empty composites are not valid
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
        new AuditAssertion(),
        new CustomAssertionHolder(),
        new WsTrustCredentialExchange(),
        new WsFederationPassiveTokenExchange(),
        new WsFederationPassiveTokenRequest(),
        new Regex(),
        new UnknownAssertion(),
        new SnmpTrapAssertion(),
        new ThroughputQuota(),
        new EmailAlertAssertion(),
        new CommentAssertion(),
        new ComparisonAssertion(),
        new StealthFault(),
        new SqlAttackAssertion(),
        new OversizedTextAssertion(),
        new RequestSizeLimit(),
        new RequestWssKerberos(),
        new EncryptedUsernameTokenAssertion(),
        new WsiBspAssertion(),
        new WsiSamlAssertion(),
        new EchoRoutingAssertion(),
    };

    public static Assertion[] CREDENTIAL_ASSERTIONS = new Assertion[] {
        new HttpBasic(),
        new HttpDigest(),
        new WssBasic(),
        new EncryptedUsernameTokenAssertion(),            
        new RequestWssX509Cert(),
        new SecureConversation(),
        new RequestWssSaml(),
        new WsTrustCredentialExchange(),
        new WsFederationPassiveTokenExchange(),
        new WsFederationPassiveTokenRequest(),
        new XpathCredentialSource(),
        new RequestWssKerberos(),
    };

}
