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
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.policy.assertion.xmlsec.XmlRequestSecurity;
import com.l7tech.policy.assertion.xmlsec.XmlResponseSecurity;

/**
 * @author alex
 * @version $Revision$
 */
public class AllAssertions {
    public static Assertion[] EVERYTHING = new Assertion[]{
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
        new XmlResponseSecurity(),
        new XmlRequestSecurity(),
        new RequestXpathAssertion()
        // new CustomAssertionHolder()
        // TODO new TimeOfDayAssertion(),
        // TODO new DateRangeAssertion(),
        // TODO new DayOfWeekAssertion(),
        // TODO new InetAddressAssertion(),
    };
}
