/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.policy;

import com.l7tech.policy.PolicyFactory;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.policy.assertion.credential.wss.EncryptedUsernameTokenAssertion;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.policy.assertion.xmlsec.RequestWssIntegrity;
import com.l7tech.policy.assertion.xmlsec.RequestWssKerberos;
import com.l7tech.policy.assertion.xmlsec.ResponseWssIntegrity;
import com.l7tech.proxy.policy.assertion.*;
import com.l7tech.proxy.policy.assertion.credential.http.ClientHttpBasic;
import com.l7tech.proxy.policy.assertion.credential.http.ClientHttpDigest;
import com.l7tech.proxy.policy.assertion.credential.wss.ClientEncryptedUsernameTokenAssertion;
import com.l7tech.proxy.policy.assertion.credential.wss.ClientWssBasic;
import com.l7tech.proxy.policy.assertion.xmlsec.ClientRequestWssIntegrity;
import com.l7tech.proxy.policy.assertion.xmlsec.ClientRequestWssKerberos;
import com.l7tech.proxy.policy.assertion.xmlsec.ClientResponseWssIntegrity;

/**
 * @author alex
 * @version $Revision$
 */
public class ClientPolicyFactory extends PolicyFactory {
    public ClientAssertion makeClientPolicy( Assertion rootAssertion ) throws PolicyAssertionException {
        return (ClientAssertion)makeSpecificPolicy( rootAssertion );
    }

    public static ClientPolicyFactory getInstance() {
        if ( _instance == null ) _instance = new ClientPolicyFactory();
        return _instance;
    }

    protected String getProductRootPackageName() {
        return "com.l7tech.proxy.policy.assertion";
    }

    protected String getProductClassnamePrefix() {
        return "Client";
    }

    protected Object makeSpecificPolicy(Assertion genericAssertion) throws PolicyAssertionException {
        if (genericAssertion instanceof CommentAssertion) return null;

        return super.makeSpecificPolicy(genericAssertion);
    }

    protected Object makeUnknownAssertion(Assertion genericAssertion) {
        return new ClientUnknownAssertion(genericAssertion);
    }

    private static ClientPolicyFactory _instance;

    // Insert references to dynamically loaded classes that will be used by this factory,
    // so the closure finder will notice that we need them in the JAR
    public static ClientAssertion[] EVERYTHING = new ClientAssertion[] {
        new ClientHttpBasic(new HttpBasic()),
        new ClientHttpDigest(new HttpDigest()),
        new ClientWssBasic(new WssBasic()),
        new ClientEncryptedUsernameTokenAssertion(new EncryptedUsernameTokenAssertion()),
        //new ClientAllAssertion(new AllAssertion()),
        //new ClientExactlyOneAssertion(new ExactlyOneAssertion()),
        //new ClientOneOrMoreAssertion(new OneOrMoreAssertion()),
        new ClientFalseAssertion(new FalseAssertion()),
        new ClientSslAssertion(new SslAssertion()),
        new ClientTrueAssertion(new TrueAssertion()),
        new ClientResponseWssIntegrity(new ResponseWssIntegrity()),
        new ClientRequestWssIntegrity(new RequestWssIntegrity()),
        new ClientRequestXpathAssertion(new RequestXpathAssertion()),
        new ClientRequestWssKerberos(new RequestWssKerberos())
        // TODO new TimeOfDayAssertion(),
        // TODO new DateRangeAssertion(),
        // TODO new DayOfWeekAssertion(),
        // TODO new InetAddressAssertion(),
        // TODO new XmlDsigAssertion(),
        // TODO new XmlEncAssertion()
    };
}
