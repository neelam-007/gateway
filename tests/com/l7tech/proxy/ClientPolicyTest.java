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
import java.util.Arrays;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.TrueAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.composite.ExactlyOneAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
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

        AssertionStatus result = policy.decorateRequest(req);

        assertTrue(AssertionStatus.NONE.equals(result));
    }

    /** Test decoration of a message with an HTTP Basic policy. */
    public void testHttpBasicPolicy() throws Exception {
        Assertion policy = new HttpBasic();
        Ssg ssg = new Ssg(1, "Foo ssg", "/foo", "http://foo");
        SOAPEnvelope env = new SOAPEnvelope();
        PendingRequest req;
        AssertionStatus result;

        ssg.setUsername(null);
        ssg.setPassword("");
        result = policy.decorateRequest(req = new PendingRequest(env, ssg));
        assertTrue(!AssertionStatus.NONE.equals(result));
        assertFalse(req.isBasicAuthRequired());

        ssg.setUsername("");
        result = policy.decorateRequest(req = new PendingRequest(env, ssg));
        assertTrue(!AssertionStatus.NONE.equals(result));
        assertFalse(req.isBasicAuthRequired());

        final String USER = "fbunky";
        ssg.setUsername(USER);
        result = policy.decorateRequest(req = new PendingRequest(env, ssg));
        assertTrue(AssertionStatus.NONE.equals(result));
        assertTrue(req.isBasicAuthRequired());
        assertTrue(USER.equals(req.getHttpBasicUsername()));
        assertTrue("".equals(req.getHttpBasicPassword()));

        final String PASS = "s3cr3t";
        ssg.setPassword(PASS);
        result = policy.decorateRequest(req = new PendingRequest(env, ssg));
        assertTrue(AssertionStatus.NONE.equals(result));
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
        AssertionStatus result;

        result = policy.decorateRequest(req = new PendingRequest(env, ssg));;
        assertTrue(AssertionStatus.NONE.equals(result));
        assertTrue(req.isSslRequired());
    }

    /** Test a composite policy. */
    public void testCompositePolicy() throws Exception {
        Ssg ssg = new Ssg(1, "Foo ssg", "/foo", "http://foo");
        SOAPEnvelope env = new SOAPEnvelope();
        PendingRequest req;
        AssertionStatus result;

        {
            // Test (SSL + Basic) || Digest
            Assertion policy = new ExactlyOneAssertion(Arrays.asList(new Assertion[] {
                new AllAssertion(Arrays.asList(new Assertion[] {
                    new SslAssertion(),
                    new HttpBasic()
                })),
                new HttpDigest(),
            }));

            ssg.setUsername("");
            ssg.setPassword("");
            result = policy.decorateRequest(req = new PendingRequest(env, ssg));
            assertFalse(AssertionStatus.NONE.equals(result));

            final String USER = "fbunky";
            final String PASS = "asdfjkal";
            ssg.setUsername(USER);
            ssg.setPassword(PASS);
            result = policy.decorateRequest(req = new PendingRequest(env, ssg));
            assertTrue(AssertionStatus.NONE.equals(result));
            assertTrue(req.isSslRequired());
            assertFalse(req.isDigestAuthRequired());
            assertTrue(req.isBasicAuthRequired());
            assertTrue(USER.equals(req.getHttpBasicUsername()));
            assertTrue(PASS.equals(req.getHttpBasicPassword()));
        }

        {
            // Test Digest || (SSL + Basic)
            Assertion policy = new ExactlyOneAssertion(Arrays.asList(new Assertion[] {
                new HttpDigest(),
                new AllAssertion(Arrays.asList(new Assertion[] {
                    new SslAssertion(),
                    new HttpBasic()
                })),
            }));

            ssg.setUsername("");
            ssg.setPassword("");
            result = policy.decorateRequest(req = new PendingRequest(env, ssg));
            assertFalse(AssertionStatus.NONE.equals(result));

            final String USER = "fbunky";
            final String PASS = "asdfjkal";
            ssg.setUsername(USER);
            ssg.setPassword(PASS);
            result = policy.decorateRequest(req = new PendingRequest(env, ssg));
            assertTrue(AssertionStatus.NONE.equals(result));
            assertFalse(req.isSslRequired());
            assertFalse(req.isBasicAuthRequired());
            assertTrue(req.isDigestAuthRequired());
            assertTrue(USER.equals(req.getHttpDigestUsername()));
            assertTrue(PASS.equals(req.getHttpDigestPassword()));
        }
    }
}
