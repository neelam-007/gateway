/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.ExactlyOneAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.credential.http.HttpClientCert;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.policy.assertion.credential.wss.WssDigest;
import com.l7tech.policy.assertion.ext.Category;
import com.l7tech.policy.assertion.ext.CustomAssertion;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.policy.assertion.xmlsec.*;

/**
 * @author alex
 * @version $Revision$
 */
public class AllAssertions {

    /**
     * the assertions that the agent (ssa) uses
     */
    public static Assertion[] AGENT_EVERYTHING = new Assertion[]{
        new HttpBasic(),
        new HttpClientCert(),
        new HttpDigest(),
        new WssBasic(),
        new WssDigest(),
        new AllAssertion(),
        new ExactlyOneAssertion(),
        new OneOrMoreAssertion(),
        new FalseAssertion(),
        new SslAssertion(),
        new HttpRoutingAssertion(),
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
        // TODO new TimeOfDayAssertion(),
        // TODO new DateRangeAssertion(),
        // TODO new DayOfWeekAssertion(),
        // TODO new InetAddressAssertion(),
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
        new HttpClientCert(),
        new HttpDigest(),
        new WssBasic(),
        new WssDigest(),
        new AllAssertion(),
        new ExactlyOneAssertion(),
        new OneOrMoreAssertion(),
        new FalseAssertion(),
        new SslAssertion(),
        new HttpRoutingAssertion(),
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
        CUSTOM_ASSERTION_HOLDER,
        new UnknownAssertion()
        // TODO new TimeOfDayAssertion(),
        // TODO new DateRangeAssertion(),
        // TODO new DayOfWeekAssertion(),
        // TODO new InetAddressAssertion(),
    };


    /**
     * all assertions that the serialization must handle
     */
    public static Assertion[] SERIALIZABLE_EVERYTHING = new Assertion[]{
        new HttpBasic(),
        new HttpClientCert(),
        new HttpDigest(),
        new WssBasic(),
        new WssDigest(),
        new AllAssertion(),
        new ExactlyOneAssertion(),
        new OneOrMoreAssertion(),
        new FalseAssertion(),
        new SslAssertion(),
        new HttpRoutingAssertion(),
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
        new CustomAssertionHolder(),
        new UnknownAssertion()
        // TODO new TimeOfDayAssertion(),
        // TODO new DateRangeAssertion(),
        // TODO new DayOfWeekAssertion(),
        // TODO new InetAddressAssertion(),
    };

}
