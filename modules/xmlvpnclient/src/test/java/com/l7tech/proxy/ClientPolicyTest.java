/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy;

import com.l7tech.message.Message;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.TrueAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.ExactlyOneAssertion;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;
import com.l7tech.proxy.datamodel.exceptions.HttpChallengeRequiredException;
import com.l7tech.proxy.message.PolicyApplicationContext;
import com.l7tech.proxy.policy.ClientPolicyFactory;
import com.l7tech.proxy.policy.assertion.*;
import com.l7tech.proxy.policy.assertion.credential.http.ClientHttpBasic;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.Arrays;

/**
 *
 * User: mike
 * Date: Jun 16, 2003
 * Time: 11:23:25 AM
 */
public class ClientPolicyTest extends TestCase {
    public ClientPolicyTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(ClientPolicyTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    protected void setUp() throws Exception {
        AssertionRegistry.installEnhancedMetadataDefaults();
    }

    /** Decorate a message with an empty policy. */
    public void testNullPolicy() throws Exception {
        Ssg ssg = new Ssg(1, "foo");
        Document env = XmlUtil.stringToDocument("<foo/>");
        PolicyApplicationContext context = new PolicyApplicationContext(ssg, new Message(env), null, null, null, null);

        ClientAssertion policy = new ClientTrueAssertion( TrueAssertion.getInstance() );

        AssertionStatus result = policy.decorateRequest(context);

        assertTrue(AssertionStatus.NONE.equals(result));
    }

    /** Test decoration of a message with an HTTP Basic policy. */
    public void testHttpBasicPolicy() throws Exception {
        ClientAssertion policy = new ClientHttpBasic( new HttpBasic() );
        Ssg ssg = new Ssg(1, "foo");
        Document env = XmlUtil.stringToDocument("<foo/>");
        PolicyApplicationContext context = new PolicyApplicationContext(ssg, new Message(env), null, null, null, null);
        AssertionStatus result;

        ssg.setUsername(null);
        ssg.getRuntime().setCachedPassword("".toCharArray());
        context.getCredentialsForTrustedSsg();
        result = policy.decorateRequest(context);
        assertTrue(AssertionStatus.NONE != result);

        ssg.setUsername("");
        context.getCredentialsForTrustedSsg();
        result = policy.decorateRequest(context);
        assertTrue(AssertionStatus.NONE != result);

        final String USER = "fbunky";
        ssg.setUsername(USER);
        context.getCredentialsForTrustedSsg();
        result = policy.decorateRequest(context);
        assertTrue(AssertionStatus.NONE.equals(result));
        assertTrue(context.isBasicAuthRequired());

        final String PASS = "s3cr3t";
        ssg.getRuntime().setCachedPassword(PASS.toCharArray());
        context.getCredentialsForTrustedSsg();
        result = policy.decorateRequest(context);
        assertTrue(AssertionStatus.NONE.equals(result));
        assertTrue(context.isBasicAuthRequired());
    }

    /** Test decoration of a message with an SSL policy (specifying no certificates in particular). */
    public void testAnonymousSslPolicy() throws Exception {
        ClientAssertion policy = new ClientSslAssertion( new SslAssertion() );
        Ssg ssg = new Ssg(1, "foo");
        Document env = XmlUtil.stringToDocument("<foo/>");
        PolicyApplicationContext context = new PolicyApplicationContext(ssg, new Message(env), null, null, null, null);
        AssertionStatus result;

        result = policy.decorateRequest(context);;
        assertTrue(AssertionStatus.NONE.equals(result));
        assertTrue(context.isSslRequired());
    }

    private PolicyApplicationContext context;
    private PolicyApplicationContext makeContext(Ssg ssg,Document env) throws IOException, SAXException, OperationCanceledException, HttpChallengeRequiredException {
        context = new PolicyApplicationContext(ssg, new Message(env), null, null, null, null);
        // preheat credentials
        context.getCredentialsForTrustedSsg();
        return context;
    }

    /** Test a composite policy. */
    public void testCompositePolicy() throws Exception {
        Ssg ssg = new Ssg(1, "foo");
        Document env = XmlUtil.stringToDocument("<foo/>");
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

            if (clientPolicy != null) {
                assertTrue(noUnknowns(clientPolicy));

                // Test empty username
                ssg.setUsername("");
                ssg.getRuntime().setCachedPassword("".toCharArray());
                result = clientPolicy.decorateRequest(makeContext(ssg, env));
                assertTrue(AssertionStatus.NONE != result);

                final String USER = "fbunky";
                final String PASS = "asdfjkal";
                ssg.setUsername(USER);
                ssg.getRuntime().setCachedPassword(PASS.toCharArray());
                result = clientPolicy.decorateRequest(makeContext(ssg, env));
                assertTrue(AssertionStatus.NONE.equals(result));
                assertTrue(context.isSslRequired());
                assertFalse(context.isDigestAuthRequired());
                assertTrue(context.isBasicAuthRequired());
            }
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
            if (clientPolicy != null) {
                // Test empty username
                ssg.setUsername("");
                ssg.getRuntime().setCachedPassword("".toCharArray());
                result = clientPolicy.decorateRequest(makeContext(ssg, env));
                assertTrue(AssertionStatus.NONE != result);

                final String USER = "fbunky";
                final String PASS = "asdfjkal";
                ssg.setUsername(USER);
                ssg.getRuntime().setCachedPassword(PASS.toCharArray());
                result = clientPolicy.decorateRequest(makeContext(ssg, env));
                assertTrue(AssertionStatus.NONE.equals(result));
                assertFalse(context.isSslRequired());
                assertFalse(context.isBasicAuthRequired());
                assertTrue(context.isDigestAuthRequired());
            }
        }
    }

    private boolean noUnknowns(ClientAssertion clientPolicy) {
        final boolean[] sawUnknown = new boolean[] { false };
        clientPolicy.visit(new ClientAssertion.ClientAssertionVisitor() {
            public void visit(ClientAssertion clientAssertion) {
                if (clientAssertion instanceof ClientUnknownAssertion || clientAssertion instanceof UnimplementedClientAssertion) {
                    System.err.println("Unimplemented assertion: " + clientAssertion.getName());
                    sawUnknown[0] = true;
                }
            }
        });
        return !sawUnknown[0];
    }
}
