/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.logging.Logger;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.TrueAssertion;
import com.l7tech.policy.assertion.AssertionError;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.proxy.datamodel.Ssg;
import org.apache.axis.message.SOAPEnvelope;

/**
 *
 * User: mike
 * Date: Jun 16, 2003
 * Time: 11:23:25 AM
 */
public class ClientPolicyTest extends TestCase {
    private static Logger log = Logger.getLogger(ClientPolicyTest.class.getName());

    public ClientPolicyTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(ClientPolicyTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    /** Decorate a message with an empty policy. */
    public void testNullPolicy() throws Exception {
        Ssg ssg = new Ssg(1, "Foo Ssg", "/foo", "http://foo");
        PendingRequest req = new PendingRequest(new SOAPEnvelope(), ssg);

        Assertion policy = new TrueAssertion();

        AssertionError result = policy.decorateRequest(req);

        assertTrue(AssertionError.NONE.equals(result));
    }

    /** Test decoration of a message with an HTTP Basic policy. */
    public void testHttpBasicPolicy() throws Exception {
        Assertion policy = new HttpBasic();
        Ssg ssg = new Ssg(1, "Foo ssg", "/foo", "http://foo");
        SOAPEnvelope env = new SOAPEnvelope();
        PendingRequest req;
        AssertionError result;

        ssg.setUsername(null);
        ssg.setPassword("");
        result = policy.decorateRequest(req = new PendingRequest(env, ssg));
        assertTrue(!AssertionError.NONE.equals(result));
        assertFalse(req.isBasicAuthRequired());

        ssg.setUsername("");
        result = policy.decorateRequest(req = new PendingRequest(env, ssg));
        assertTrue(!AssertionError.NONE.equals(result));
        assertFalse(req.isBasicAuthRequired());

        final String USER = "fbunky";
        ssg.setUsername(USER);
        result = policy.decorateRequest(req = new PendingRequest(env, ssg));
        assertTrue(AssertionError.NONE.equals(result));
        assertTrue(req.isBasicAuthRequired());
        assertTrue(USER.equals(req.getHttpBasicUsername()));
        assertTrue("".equals(req.getHttpBasicPassword()));

        final String PASS = "s3cr3t";
        ssg.setPassword(PASS);
        result = policy.decorateRequest(req = new PendingRequest(env, ssg));
        assertTrue(AssertionError.NONE.equals(result));
        assertTrue(req.isBasicAuthRequired());
        assertTrue(USER.equals(req.getHttpBasicUsername()));
        assertTrue(PASS.equals(req.getHttpBasicPassword()));
    }

    /** Test decoration of a message with an SSL policy (specifying no certificates in particular). */
    public void testAnonymousSslPolicy() throws Exception {
        Assertion policy = new SslAssertion();
        Ssg ssg = new Ssg(1, "Foo ssg", "/foo", "http://foo");
        SOAPEnvelope env = new SOAPEnvelope();
        PendingRequest req;
        AssertionError result;

        result = policy.decorateRequest(req = new PendingRequest(env, ssg));;
        assertTrue(AssertionError.NONE.equals(result));
        assertTrue(req.isSslRequired());
    }
}
