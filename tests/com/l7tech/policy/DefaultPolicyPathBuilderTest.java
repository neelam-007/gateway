/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy;

import com.l7tech.common.ApplicationContexts;
import com.l7tech.common.policy.Policy;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.TestDocuments;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.assertion.credential.http.CookieCredentialSourceAssertion;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.policy.assertion.xmlsec.RequestWssReplayProtection;
import com.l7tech.policy.assertion.xmlsec.RequestWssSaml;
import com.l7tech.policy.assertion.xmlsec.RequestWssX509Cert;
import com.l7tech.policy.assertion.xmlsec.SecureConversation;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.ReadOnlyEntityManager;
import com.l7tech.objectmodel.PolicyHeader;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.springframework.context.ApplicationContext;

/**
 * Test the default policy assertion path builder/analyzer class
 * functionality.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class DefaultPolicyPathBuilderTest extends TestCase {
    private ApplicationContext spring;
    private ReadOnlyEntityManager<Policy, PolicyHeader> policyFinder;

    public DefaultPolicyPathBuilderTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(DefaultPolicyPathBuilderTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testSingleDepthPolicyPathWithConjunctionOr() throws Exception {
        final List kids =
          Arrays.asList(new Assertion[]{
              new TrueAssertion(),
              new FalseAssertion(),
              new TrueAssertion()
          });

        Assertion oom = new OneOrMoreAssertion(kids);
        DefaultPolicyPathBuilder builder = getPathBuilder();

        assertTrue(builder.generate(oom).getPathCount() == 3);
    }

    private DefaultPolicyPathBuilder getPathBuilder() {
        return new DefaultPolicyPathBuilder((ReadOnlyEntityManager<Policy, PolicyHeader>) spring.getBean("policyManager"));
    }

    @Override
    protected void setUp() throws Exception {
        this.spring = ApplicationContexts.getTestApplicationContext();
        this.policyFinder = (ReadOnlyEntityManager<Policy, PolicyHeader>) spring.getBean("policyManager");
    }

    public void testAllAssertionSingleDepthWithConjunctionOr() throws Exception {
        final List kids =
          Arrays.asList(new Assertion[]{
              new TrueAssertion(),
              new TrueAssertion()
          });

        final List kids2 =
          Arrays.asList(new Assertion[]{
              new FalseAssertion()
          });

        Assertion one = new OneOrMoreAssertion(kids);
        Assertion two = new OneOrMoreAssertion(kids2);
        Assertion oom = new AllAssertion(Arrays.asList(new Assertion[]{one, two}));
        DefaultPolicyPathBuilder builder = getPathBuilder();

        final int pathCount = builder.generate(oom).getPathCount();
        assertTrue("The path count value received is " + pathCount, pathCount == 2);
    }


    public void testSingleDepthPolicyPathWithConjunctionAnd() throws Exception {
        final List kids =
          Arrays.asList(new Assertion[]{
              new TrueAssertion(),
              new FalseAssertion(),
              new TrueAssertion()
          });

        Assertion oom = new AllAssertion(kids);
        DefaultPolicyPathBuilder builder = getPathBuilder();

        assertTrue(builder.generate(oom).getPathCount() == 1);
    }

    public void testSingleDepthPolicyPathWithConjunctionAnd2() throws Exception {
        final List kids =
          Arrays.asList(new Assertion[]{
              new TrueAssertion(),
              new OneOrMoreAssertion(),
              new TrueAssertion()
          });

        Assertion oom = new AllAssertion(kids);
        DefaultPolicyPathBuilder builder = getPathBuilder();

        assertTrue(builder.generate(oom).getPathCount() == 1);
    }

    public void testTwoDepthPolicyPathWithConjunctionOr() throws Exception {
        final List kids =
          Arrays.asList(new Assertion[]{
              new TrueAssertion(),
              new FalseAssertion(),
              new FalseAssertion()
          });

        final List kids2 =
          Arrays.asList(new Assertion[]{
              new TrueAssertion(),
              new FalseAssertion(),
              new TrueAssertion()
          });

        final List kids3 =
          Arrays.asList(new Assertion[]{
              new TrueAssertion(),
              new FalseAssertion(),
              new FalseAssertion()
          });

        Assertion one = new OneOrMoreAssertion(kids);
        Assertion two = new OneOrMoreAssertion(kids2);
        Assertion three = new OneOrMoreAssertion(kids3);

        Assertion oom = new OneOrMoreAssertion(Arrays.asList(new Assertion[]{one, two, three}));
        DefaultPolicyPathBuilder builder = getPathBuilder();
        int count = builder.generate(oom).getPathCount();
        assertTrue("The value received is " + count, count == 9);
    }

    public void testTwoDepthPolicyPathWithConjunctionAnd() throws Exception {
        final List kids =
          Arrays.asList(new Assertion[]{
              new TrueAssertion(),
              new FalseAssertion(),
              new TrueAssertion()
          });

        final List kids2 =
          Arrays.asList(new Assertion[]{
              new TrueAssertion(),
              new FalseAssertion(),
              new TrueAssertion()
          });

        Assertion one = new OneOrMoreAssertion(kids);
        Assertion two = new OneOrMoreAssertion(kids2);
        Assertion three = new AllAssertion(Arrays.asList(new Assertion[]{new TrueAssertion()}));
        Assertion oom = new OneOrMoreAssertion(Arrays.asList(new Assertion[]{one, two, three}));
        DefaultPolicyPathBuilder builder = getPathBuilder();

        int count = builder.generate(oom).getPathCount();
        assertTrue("The value received is " + count, count == 7);
    }

    public void testBug763MonsterPolicy() throws Exception {
        Assertion policy = WspReader.getDefault().parsePermissively(XmlUtil.parse(TestDocuments.getInputStream(TestDocuments.BUG_763_MONSTER_POLICY)).getDocumentElement());
        DefaultPolicyPathBuilder builder = getPathBuilder();

        PolicyPathResult result = builder.generate(policy);
        int count = result.getPathCount();
        assertTrue("The value received is " + count, count == 5);
    }


    public void testBug1022() throws Exception {
        Assertion firstAll = new AllAssertion(Arrays.asList(new Assertion[]{
            new RequestWssX509Cert(),
            new SpecificUser(-2, "fred", "fred", "fred")
        }));
        Assertion secondAll = new AllAssertion(Arrays.asList(new Assertion[]{
            new HttpBasic(),
            new SpecificUser(-2, "wilma", "wilma", "wilma")
        }));

        Assertion top = new AllAssertion(Arrays.asList(new Assertion[]{
            new OneOrMoreAssertion(Arrays.asList(new Assertion[]{firstAll, secondAll})),
            new HttpRoutingAssertion("http://wheel")
        }));

        DefaultPolicyPathBuilder builder = getPathBuilder();
        PolicyPathResult result = builder.generate(top);
        assertTrue(result.getPathCount() == 2);
    }

    public void testBug1334() throws Exception {
        Assertion firstAll = new AllAssertion(Arrays.asList(new Assertion[]{
            new RequestWssX509Cert(),
            new OneOrMoreAssertion(Arrays.asList(new Assertion[]{
                new SpecificUser(-2, "fred", "fred", "fred")
            }))
        }));
        Assertion secondAll = new AllAssertion(Arrays.asList(new Assertion[]{
            new HttpBasic(),
            new OneOrMoreAssertion(Arrays.asList(new Assertion[]{
                new SpecificUser(-2, "wilma", "wilma", "wilma")
            }))
        }));

        Assertion top = new AllAssertion(Arrays.asList(new Assertion[]{
            new OneOrMoreAssertion(Arrays.asList(new Assertion[]{firstAll, secondAll})),
            new HttpRoutingAssertion("http://wheel")
        }));

        DefaultPolicyPathBuilder builder = getPathBuilder();
        PolicyPathResult result = builder.generate(top);
        assertTrue(result.getPathCount() == 2);
        Iterator it = result.paths().iterator();
        while (it.hasNext()) {
            AssertionPath path = (AssertionPath)it.next();
            System.out.println(DefaultPolicyPathBuilder.pathToString(path));
        }
    }


    public void testBug1374() throws Exception {
        List credentials = getCredentialsLocations();
        int nCrendentials = credentials.size();
        for (int i = nCrendentials - 1; i >= 0; i--) {
            List childrenOr = credentials.subList(i, nCrendentials);
            Assertion firstOr = new OneOrMoreAssertion(childrenOr);

            Assertion secondOr = new OneOrMoreAssertion(Arrays.asList(new Assertion[]{
                new SpecificUser(-2, "wilma", "wilma", "wilma")
            }));

            Assertion top = new AllAssertion(Arrays.asList(new Assertion[]{
                firstOr,
                new RequestWssReplayProtection(),
                secondOr,
                new HttpRoutingAssertion("http://wheel")
            }));

            DefaultPolicyPathBuilder builder = getPathBuilder();
            PolicyPathResult result = builder.generate(top);
            assertTrue(result.getPathCount() == nCrendentials - i);
        }
    }

    static List getCredentialsLocations() {
        List credentialsLocationList = new ArrayList();
        credentialsLocationList.add(new TrueAssertion());

        credentialsLocationList.add(new HttpBasic());
        credentialsLocationList.add(new HttpDigest());
        credentialsLocationList.add(new SslAssertion(true));
        credentialsLocationList.add(new WssBasic());
        credentialsLocationList.add(new RequestWssX509Cert());
        credentialsLocationList.add(new SecureConversation());
        credentialsLocationList.add(new RequestWssSaml());
        credentialsLocationList.add(new CookieCredentialSourceAssertion());

        return credentialsLocationList;
    }


}
