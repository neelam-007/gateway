/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy;

import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.ExactlyOneAssertion;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.policy.assertion.ext.Category;
import com.l7tech.policy.assertion.ext.CustomAssertion;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.policy.wsp.WspWriter;
import org.junit.Test;
import static org.junit.Assert.*;


import java.util.Arrays;
import java.util.Iterator;
import java.util.logging.Logger;

/**
 * Test the ability to express some sample policies.
 * User: mike
 * Date: Jun 25, 2003
 * Time: 10:33:00 AM
 */
public class SamplePolicyTest {
    private static Logger log = Logger.getLogger(SamplePolicyTest.class.getName());
    Goid identityProvider = new Goid(0,111);
    String userAlice = "alice";
    String userBob = "bob";

    @Test
    public void testSimple() {
        // Simplest possible policy.  No special requirements at all; unconditionally route to default ProtServ.
        Assertion simplestPolicy = new HttpRoutingAssertion();
    }

    @Test
    public void testBasicAuth() {
        // Require HTTP Basic auth.
        Assertion basicAuthPolicy = new AllAssertion(Arrays.asList(new Assertion[]{
            // Identify:
            new HttpBasic(),

            // Authorize:
            new SpecificUser(identityProvider, userAlice, null, null),

            // Route:
            new HttpRoutingAssertion()
        }));
    }

    @Test
    public void testDigestAuth() {
        String userAlice = "alice";
        // Require HTTP Digest auth.  Allow Alice in, but nobody else.
        Assertion basicAuthPolicy = new AllAssertion(Arrays.asList(new Assertion[]{
            // Identify:
            new HttpDigest(),

            // Authorize:
            new SpecificUser(identityProvider, userAlice, null, null),

            // Route:
            new HttpRoutingAssertion()
        }));
    }

    @Test
    public void testBasicSslAuth() {
        // Require HTTP Basic auth and SSL for link-level confidentiality.  Allow Bob or Alice in.
        Assertion basicAuthPolicy = new AllAssertion(Arrays.asList(new Assertion[]{
            // Preconditions:
            new SslAssertion(),

            // Identify:
            new HttpBasic(),

            // Authorize:
            new ExactlyOneAssertion(Arrays.asList(new Assertion[]{
                new SpecificUser(identityProvider, userAlice, null, null),
                new SpecificUser(identityProvider, userBob, null, null)
            })),

            // Route:
            new HttpRoutingAssertion() // will use default URL for the PublishedService using this policy
        }));
    }

    @Test
    public void testDigestGroup() {
        String groupStaff = "staff";
        // Require HTTP Digest auth with group.  All staff get to use this service.
        Assertion basicAuthPolicy = new AllAssertion(Arrays.asList(new Assertion[]{
            // Identify:
            new HttpDigest(),

            // Authorize:
            new MemberOfGroup(identityProvider, groupStaff, "666"),

            // Route:
            new HttpRoutingAssertion()
        }));
    }
    
    @Test
    public void testPerUserRouting() {
        // Require HTTP Digest auth.  Alice goes to service1, Bob goes to service2
        Assertion perUserRoutingPolicy = new AllAssertion(Arrays.asList(new Assertion[]{
            // Identify:
            new HttpBasic(),

            // Route:
            new ExactlyOneAssertion(Arrays.asList(new Assertion[]{
                new AllAssertion(Arrays.asList(new Assertion[]{
                    new SpecificUser(identityProvider, userAlice, null, null),
                    new HttpRoutingAssertion("http://backend.example.com/service1/soap")
                })),
                new AllAssertion(Arrays.asList(new Assertion[]{
                    new SpecificUser(identityProvider, userBob, null, null),
                    new HttpRoutingAssertion("http://backend.example.com/service2/soap")
                })),
            })),
        }));

        // Display this policy for Jay
        log.info(WspWriter.getPolicyXml(perUserRoutingPolicy));
    }

    static public class OneBean implements CustomAssertion {
        Object value;

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }

        public String getName() {
            return "test name";
        }

    }

    @Test
    public void testCustomAssertion() throws Exception {
        CustomAssertionHolder customAssertion = new CustomAssertionHolder();
        OneBean oneBean = new OneBean();
        oneBean.setValue("bu!");
        customAssertion.setCustomAssertion(oneBean);
        customAssertion.setCategories(Category.ACCESS_CONTROL);
        Assertion testin = new AllAssertion(Arrays.asList(new Assertion[]{
            new HttpBasic(),
            customAssertion
        }));

        String sxml = WspWriter.getPolicyXml(testin);
        log.info("Serialized to: " + sxml);
        Assertion testout = WspReader.getDefault().parsePermissively(sxml, WspReader.INCLUDE_DISABLED);

        Iterator it = testout.preorderIterator();
        while (it.hasNext()) {
            Assertion a = (Assertion)it.next();
            if (a instanceof CustomAssertionHolder) {
                CustomAssertionHolder ca = (CustomAssertionHolder)a;
                final OneBean cb = (OneBean)ca.getCustomAssertion();
                assertTrue(cb.getValue().equals(oneBean.getValue()));
            }
        }

    }

}
