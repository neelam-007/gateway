/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.policy;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.HeaderBasedEntityFinder;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.assertion.credential.http.CookieCredentialSourceAssertion;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.policy.assertion.xmlsec.WssReplayProtection;
import com.l7tech.policy.assertion.xmlsec.RequireWssSaml;
import com.l7tech.policy.assertion.xmlsec.RequireWssX509Cert;
import com.l7tech.policy.assertion.xmlsec.SecureConversation;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.policy.DefaultPolicyPathBuilder;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyPathResult;
import com.l7tech.policy.AssertionPath;
import com.l7tech.objectmodel.GuidBasedEntityManager;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.TestDocuments;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;


import java.util.*;

import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;

/**
 * Test the default policy assertion path builder/analyzer class
 * functionality.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultPolicyPathBuilderTest {
    private ApplicationContext spring;
    @Mock
    private HeaderBasedEntityFinder entityFinder;

    @Test
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
        return new DefaultPolicyPathBuilder((GuidBasedEntityManager<Policy>) spring.getBean("policyManager"), entityFinder){};
    }

    @Before
    public void setUp() throws Exception {
        this.spring = ApplicationContexts.getTestApplicationContext();
    }

    @Test
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


    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
    public void testBug763MonsterPolicy() throws Exception {
        Assertion policy = WspReader.getDefault().parsePermissively(XmlUtil.parse(TestDocuments.getInputStream(TestDocuments.BUG_763_MONSTER_POLICY)).getDocumentElement(), WspReader.INCLUDE_DISABLED);
        DefaultPolicyPathBuilder builder = getPathBuilder();

        PolicyPathResult result = builder.generate(policy);
        int count = result.getPathCount();
        assertTrue("The value received is " + count, count == 5);
    }


    @Test
    public void testBug1022() throws Exception {
        Assertion firstAll = new AllAssertion(Arrays.asList(new Assertion[]{
            new RequireWssX509Cert(),
            new SpecificUser(new Goid(0,-2), "fred", "fred", "fred")
        }));
        Assertion secondAll = new AllAssertion(Arrays.asList(new Assertion[]{
            new HttpBasic(),
            new SpecificUser(new Goid(0,-2), "wilma", "wilma", "wilma")
        }));

        Assertion top = new AllAssertion(Arrays.asList(new Assertion[]{
            new OneOrMoreAssertion(Arrays.asList(new Assertion[]{firstAll, secondAll})),
            new HttpRoutingAssertion("http://wheel")
        }));

        DefaultPolicyPathBuilder builder = getPathBuilder();
        PolicyPathResult result = builder.generate(top);
        assertTrue(result.getPathCount() == 2);
    }

    @Test
    public void testBug1334() throws Exception {
        Assertion firstAll = new AllAssertion(Arrays.asList(new Assertion[]{
            new RequireWssX509Cert(),
            new OneOrMoreAssertion(Arrays.asList(new Assertion[]{
                new SpecificUser(new Goid(0,-2), "fred", "fred", "fred")
            }))
        }));
        Assertion secondAll = new AllAssertion(Arrays.asList(new Assertion[]{
            new HttpBasic(),
            new OneOrMoreAssertion(Arrays.asList(new Assertion[]{
                new SpecificUser(new Goid(0,-2), "wilma", "wilma", "wilma")
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


    @Test
    public void testBug1374() throws Exception {
        List credentials = getCredentialsLocations();
        int nCrendentials = credentials.size();
        for (int i = nCrendentials - 1; i >= 0; i--) {
            List childrenOr = credentials.subList(i, nCrendentials);
            Assertion firstOr = new OneOrMoreAssertion(childrenOr);

            Assertion secondOr = new OneOrMoreAssertion(Arrays.asList(new Assertion[]{
                new SpecificUser(new Goid(0,-2), "wilma", "wilma", "wilma")
            }));

            Assertion top = new AllAssertion(Arrays.asList(new Assertion[]{
                firstOr,
                new WssReplayProtection(),
                secondOr,
                new HttpRoutingAssertion("http://wheel")
            }));

            DefaultPolicyPathBuilder builder = getPathBuilder();
            PolicyPathResult result = builder.generate(top);
            assertTrue(result.getPathCount() == nCrendentials - i);
        }
    }

    @Test
    public void generatePathWithEncapsulatedAssertion() throws Exception {
        final EncapsulatedAssertionConfig config = new EncapsulatedAssertionConfig();
        when(entityFinder.find(any(EntityHeader.class))).thenReturn(config);
        final EncapsulatedAssertion encass = new EncapsulatedAssertion();
        encass.setEncapsulatedAssertionConfigGuid("abc123");
        // config is null initially
        assertNull(encass.config());

        final PolicyPathResult result = getPathBuilder().generate(new AllAssertion(Collections.singletonList(encass)));
        final Set<AssertionPath> paths = result.paths();
        assertEquals(1, paths.size());
        final AssertionPath path = paths.iterator().next();
        assertEquals(2, path.getPathCount());
        final EncapsulatedAssertion encassFromPath = (EncapsulatedAssertion) path.getPathAssertion(1);
        // config should now be set on the encass by the assertion translator
        assertEquals(config, encassFromPath.config());
    }

    static List getCredentialsLocations() {
        List credentialsLocationList = new ArrayList();
        credentialsLocationList.add(new TrueAssertion());

        credentialsLocationList.add(new HttpBasic());
        credentialsLocationList.add(new HttpDigest());
        credentialsLocationList.add(new SslAssertion(true));
        credentialsLocationList.add(new WssBasic());
        credentialsLocationList.add(new RequireWssX509Cert());
        credentialsLocationList.add(new SecureConversation());
        credentialsLocationList.add(new RequireWssSaml());
        credentialsLocationList.add(new CookieCredentialSourceAssertion());

        return credentialsLocationList;
    }


}
