/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy;

import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.User;
import com.l7tech.identity.Group;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.RoutingAssertion;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.ExactlyOneAssertion;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.policy.wsp.WspWriter;
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
    Group groupStaff = null;

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
        // Simplest possible policy.  No special requirements at all; unconditionally route to default ProtServ.
        Assertion simplestPolicy = new RoutingAssertion();
    }

    public void testBasicAuth() {
        // Require HTTP Basic auth.
        Assertion basicAuthPolicy = new AllAssertion(Arrays.asList(new Assertion[] {
            // Identify:
            new HttpBasic(),

            // Authorize:
            new SpecificUser( identityProvider.getConfig().getOid(), userAlice.getLogin() ),

            // Route:
            new RoutingAssertion()
        }));
    }

    public void testDigestAuth() {
        // Require HTTP Digest auth.  Allow Alice in, but nobody else.
        Assertion basicAuthPolicy = new AllAssertion(Arrays.asList(new Assertion[] {
            // Identify:
            new HttpDigest(),

            // Authorize:
            new SpecificUser( identityProvider.getConfig().getOid(), userAlice.getLogin() ),

            // Route:
            new RoutingAssertion()
        }));
    }

    public void testBasicSslAuth() {
        // Require HTTP Basic auth and SSL for link-level confidentiality.  Allow Bob or Alice in.
        Assertion basicAuthPolicy = new AllAssertion(Arrays.asList(new Assertion[] {
            // Preconditions:
            new SslAssertion(),

            // Identify:
            new HttpBasic(),

            // Authorize:
            new ExactlyOneAssertion(Arrays.asList(new Assertion[] {
                new SpecificUser( identityProvider.getConfig().getOid(), userAlice.getLogin() ),
                new SpecificUser( identityProvider.getConfig().getOid(), userBob.getLogin() )
            })),

            // Route:
            new RoutingAssertion() // will use default URL for the PublishedService using this policy
        }));
    }

    public void testDigestGroup() {
        // Require HTTP Digest auth with group.  All staff get to use this service.
        Assertion basicAuthPolicy = new AllAssertion(Arrays.asList(new Assertion[] {
            // Identify:
            new HttpDigest(),

            // Authorize:
            new MemberOfGroup( identityProvider.getConfig().getOid(), groupStaff.getName() ),

            // Route:
            new RoutingAssertion()
        }));
    }

    public void testPerUserRouting() {
        // Require HTTP Digest auth.  Alice goes to service1, Bob goes to service2
        Assertion perUserRoutingPolicy = new AllAssertion(Arrays.asList(new Assertion[] {
            // Identify:
            new HttpBasic(),

            // Route:
            new ExactlyOneAssertion(Arrays.asList(new Assertion[] {
                new AllAssertion(Arrays.asList(new Assertion[] {
                    new SpecificUser( identityProvider.getConfig().getOid(), userAlice.getLogin() ),
                    new RoutingAssertion("http://backend.example.com/service1/soap")
                })),
                new AllAssertion(Arrays.asList(new Assertion[] {
                    new SpecificUser( identityProvider.getConfig().getOid(), userBob.getLogin() ),
                    new RoutingAssertion("http://backend.example.com/service2/soap")
                })),
            })),
        }));

        // Display this policy for Jay
        log.info(WspWriter.getPolicyXml(perUserRoutingPolicy));
    }
}
