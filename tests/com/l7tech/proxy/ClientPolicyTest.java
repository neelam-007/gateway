/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.TrueAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.ExactlyOneAssertion;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;
import com.l7tech.proxy.policy.ClientPolicyFactory;
import com.l7tech.proxy.policy.assertion.ClientAssertion;
import com.l7tech.proxy.policy.assertion.ClientSslAssertion;
import com.l7tech.proxy.policy.assertion.ClientTrueAssertion;
import com.l7tech.proxy.policy.assertion.credential.http.ClientHttpBasic;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.w3c.dom.Document;

import java.util.Arrays;
import java.util.logging.Logger;

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
        Ssg ssg = new Ssg(1, "foo");
        PendingRequest req = new PendingRequest(null, ssg, NullRequestInterceptor.INSTANCE, null);

        ClientAssertion policy = new ClientTrueAssertion( TrueAssertion.getInstance() );

        AssertionStatus result = policy.decorateRequest(req);

        assertTrue(AssertionStatus.NONE.equals(result));
    }

    /** Test decoration of a message with an HTTP Basic policy. */
    public void testHttpBasicPolicy() throws Exception {
        ClientAssertion policy = new ClientHttpBasic( new HttpBasic() );
        Ssg ssg = new Ssg(1, "foo");
        Document env = null;
        PendingRequest req;
        AssertionStatus result;

        ssg.setUsername(null);
        ssg.cmPassword("".toCharArray());
        try {
            result = policy.decorateRequest(req = new PendingRequest(env, ssg, NullRequestInterceptor.INSTANCE, null));
            fail("HttpBasic was provided null username, and didn't throw");
        } catch (OperationCanceledException e) {
            // Ok
        }

        ssg.setUsername("");
        try {
            result = policy.decorateRequest(req = new PendingRequest(env, ssg, NullRequestInterceptor.INSTANCE, null));
            fail("HttpBasic was provided empty username, and didn't throw");
        } catch (OperationCanceledException e) {
            // Ok
        }

        final String USER = "fbunky";
        ssg.setUsername(USER);
        result = policy.decorateRequest(req = new PendingRequest(env, ssg, NullRequestInterceptor.INSTANCE, null));
        assertTrue(AssertionStatus.NONE.equals(result));
        assertTrue(req.isBasicAuthRequired());

        final String PASS = "s3cr3t";
        ssg.cmPassword(PASS.toCharArray());
        result = policy.decorateRequest(req = new PendingRequest(env, ssg, NullRequestInterceptor.INSTANCE, null));
        assertTrue(AssertionStatus.NONE.equals(result));
        assertTrue(req.isBasicAuthRequired());
    }

    /** Test decoration of a message with an SSL policy (specifying no certificates in particular). */
    public void testAnonymousSslPolicy() throws Exception {
        ClientAssertion policy = new ClientSslAssertion( new SslAssertion() );
        Ssg ssg = new Ssg(1, "foo");
        Document env = null;
        PendingRequest req;
        AssertionStatus result;

        result = policy.decorateRequest(req = new PendingRequest(env, ssg, NullRequestInterceptor.INSTANCE, null));;
        assertTrue(AssertionStatus.NONE.equals(result));
        assertTrue(req.isSslRequired());
    }

    /** Test a composite policy. */
    public void testCompositePolicy() throws Exception {
        Ssg ssg = new Ssg(1, "foo");
        Document env = null;
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

            ClientAssertion clientPolicy = ClientPolicyFactory.getInstance().makeClientPolicy( policy );

            ssg.setUsername("");
            ssg.cmPassword("".toCharArray());
            try {
                result = clientPolicy.decorateRequest(req = new PendingRequest(env, ssg, NullRequestInterceptor.INSTANCE, null));
                fail("Policy was given empty username, but failed to throw");
            } catch (OperationCanceledException e) {
                // Ok
            }

            final String USER = "fbunky";
            final String PASS = "asdfjkal";
            ssg.setUsername(USER);
            ssg.cmPassword(PASS.toCharArray());
            result = clientPolicy.decorateRequest(req = new PendingRequest(env, ssg, NullRequestInterceptor.INSTANCE, null));
            assertTrue(AssertionStatus.NONE.equals(result));
            assertTrue(req.isSslRequired());
            assertFalse(req.isDigestAuthRequired());
            assertTrue(req.isBasicAuthRequired());
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

            ClientAssertion clientPolicy = ClientPolicyFactory.getInstance().makeClientPolicy( policy );

            ssg.setUsername("");
            ssg.cmPassword("".toCharArray());
            try {
                result = clientPolicy.decorateRequest(req = new PendingRequest(env, ssg, NullRequestInterceptor.INSTANCE, null));
                fail("Policy was given empty username, and failed to throw");
            } catch (OperationCanceledException e) {
                // Ok
            }

            final String USER = "fbunky";
            final String PASS = "asdfjkal";
            ssg.setUsername(USER);
            ssg.cmPassword(PASS.toCharArray());
            result = clientPolicy.decorateRequest(req = new PendingRequest(env, ssg, NullRequestInterceptor.INSTANCE, null));
            assertTrue(AssertionStatus.NONE.equals(result));
            assertFalse(req.isSslRequired());
            assertFalse(req.isBasicAuthRequired());
            assertTrue(req.isDigestAuthRequired());
        }
    }
}
