/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.filter;

import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.composite.ExactlyOneAssertion;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.credential.http.HttpClientCert;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.common.ApplicationContexts;
import com.l7tech.common.security.Keys;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.extensions.TestSetup;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.springframework.context.ApplicationContext;

/**
 * Test of some policy filter scenarios.
 * User: mike
 * Date: Aug 15, 2003
 * Time: 11:45:21 AM
 */
public class FilterTest extends TestCase {
    private static Logger log = Logger.getLogger(FilterTest.class.getName());
    private static ApplicationContext applicationContext;
    private static FilterManager filterManager;

    public FilterTest(String name) throws Exception {
        super(name);
    }

    public static Test suite() {
        final TestSuite suite = new TestSuite(FilterTest.class);
        TestSetup wrapper = new TestSetup(suite) {

            protected void setUp() throws Exception {
                Keys.createTestSsgKeystoreProperties();
                applicationContext = createApplicationContext();
                filterManager = (FilterManager)applicationContext.getBean("policyFilterManager");

            }

            protected void tearDown() throws Exception {
                ;
            }

            private ApplicationContext createApplicationContext() {
                return ApplicationContexts.getTestApplicationContext();
            }
        };
        return wrapper;
    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }

    public void testSimpleFilter() throws Exception {

        long providerid = IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID;
        Assertion pol = new AllAssertion(Arrays.asList(new Assertion[] {
            new SslAssertion(),
            new HttpBasic(),
            new ExactlyOneAssertion(Arrays.asList(new Assertion[] {
                new SpecificUser(providerid, "alice", null, null),
                new SpecificUser(providerid, "bob", null, null)
                // you cant test this because member of group requires access to the id provider and you
                // dont have access to the actual provider here
                //new MemberOfGroup(providerid, "sales", "666")
            })),
            new HttpRoutingAssertion()
        }));

        InternalUser alice = new InternalUser();
        alice.setLogin("alice");
        alice.setProviderId(providerid);
        Assertion filtered = filterManager.applyAllFilters(alice, pol);
        //log.info("Policy filtered for alice: " + WspWriter.getPolicyXml(filtered));

        assertTrue(filtered instanceof AllAssertion);
        List kids = ((CompositeAssertion) filtered).getChildren();
        assertTrue(kids != null);
        assertTrue(kids.size() == 2);
        assertTrue(kids.get(0) instanceof SslAssertion);
        assertTrue(kids.get(1) instanceof HttpBasic);
    }

    public void testUserSpecificFilter() throws Exception {
        long providerid = IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID;
        //URL policyUrl = getClass().getClassLoader().getResource(POLICY_USERSPECIFIC);
        //Assertion policy = WspReader.parse(policyUrl.openStream());
        Assertion policy = new AllAssertion(Arrays.asList(new Assertion[] {
            new ExactlyOneAssertion(Arrays.asList(new Assertion[] {
                new AllAssertion(Arrays.asList(new Assertion[] {
                    new HttpDigest(),
                    new SpecificUser(providerid, "bob", null, null),
                    new HttpRoutingAssertion()
                })),
                new AllAssertion(Arrays.asList(new Assertion[] {
                    new SslAssertion(),
                    new HttpClientCert(),
                    new SpecificUser(providerid, "alice", null, null),
                    new HttpRoutingAssertion()
                })),
            })),
        }));

        log.info("Starting policy: " + policy);


        Assertion forAnon = filterManager.applyAllFilters(null, policy.getCopy());
        log.info("Policy forAnon = " + forAnon);
        assertTrue("Filtered policy for invalid user is null", forAnon == null);

        InternalUser alice = new InternalUser();
        alice.setProviderId(providerid);
        alice.setLogin("alice");
        Assertion forAlice = filterManager.applyAllFilters(alice, policy.getCopy());
        log.info("Policy forAlice = " + forAlice);
        assertTrue("Filtered policy for valid user alice is not null", forAlice != null);

        InternalUser bob = new InternalUser();
        bob.setProviderId(providerid);
        bob.setLogin("bob");
        Assertion forBob = filterManager.applyAllFilters(bob, policy.getCopy());
        log.info("Policy forBob = " + forBob);
        assertTrue("Filtered policy for valid user bob is not null", forBob != null);

    }
}
