/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy;

import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.User;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.RoutingAssertion;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.IfThenAssertion;
import com.l7tech.policy.assertion.composite.ExactlyOneAssertion;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.policy.assertion.identity.SpecificUser;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.Arrays;
import java.util.logging.Logger;

/**
 * Test the ability to express some sample policies.
 * User: mike
 * Date: Jun 25, 2003
 * Time: 10:33:00 AM
 */
public class SamplePolicyTest extends TestCase {
    private static Logger log = Logger.getLogger(SamplePolicyTest.class.getName());
    IdentityProvider identityProvider = null;
    User userAlice = null;
    User userBob = null;

    public SamplePolicyTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(SamplePolicyTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testSimple() {
        // Simplest possible policy.  No special requirements at all
        Assertion simplestPolicy = new RoutingAssertion("http://backend.example.com/soap");
    }


    public void testBasicAuth() {
        // Require HTTP Basic auth.
        Assertion basicAuthPolicy = new IfThenAssertion(Arrays.asList(new Assertion[] {
            // Identify:
            new HttpBasic(),

            // Authorize:
            new SpecificUser(identityProvider, userAlice),

            // Route:
            new RoutingAssertion("http://backend.example.com/soap")
        }));
    }

    public void testDigestAuth() {
        // Require HTTP Digest auth.  Allow Bob in.
        Assertion basicAuthPolicy = new IfThenAssertion(Arrays.asList(new Assertion[] {
            // Identify:
            new HttpDigest(),

            // Authorize:
            new SpecificUser(identityProvider, userAlice),

            // Route:
            new RoutingAssertion("http://backend.example.com/soap")
        }));
    }

    public void testBasicSslAuth() {
        // Require HTTP Basic auth and SSL for link-level confidentiality.  Allow Bob or Alice in.
        Assertion basicAuthPolicy = new IfThenAssertion(Arrays.asList(new Assertion[] {
            // Preconditions:
            new SslAssertion(),

            // Identify:
            new HttpBasic(),

            // Authorize:
            new ExactlyOneAssertion(Arrays.asList(new Assertion[] {
                new SpecificUser(identityProvider, userAlice),
                new SpecificUser(identityProvider, userBob)
            })),

            // Route:
            new RoutingAssertion("http://backend.example.com/soap")
        }));
    }

    public void testPerUserRouting() {
        // Require HTTP Digest auth.  Alice goes to service1, Bob goes to service2
        Assertion perUserRoutingPolicy = new AllAssertion(Arrays.asList(new Assertion[] {
            // Identify:
            new HttpBasic(),

            // Route:
            new IfThenAssertion(Arrays.asList(new Assertion[] {
                new SpecificUser(identityProvider, userAlice),
                new RoutingAssertion("http://backend.example.com/service1/soap")
            })),
            new IfThenAssertion(Arrays.asList(new Assertion[] {
                new SpecificUser(identityProvider, userAlice),
                new RoutingAssertion("http://backend.example.com/service2/soap")
            })),
        }));
    }
}
