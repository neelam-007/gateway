package com.l7tech.server.policy;

import com.l7tech.common.ApplicationContexts;
import com.l7tech.common.message.Message;
import com.l7tech.policy.AllAssertions;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.policy.assertion.xmlsec.RequestWssSaml;
import com.l7tech.server.audit.AuditContextStub;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.policy.assertion.ServerEcho;
import com.l7tech.server.policy.assertion.composite.ServerCompositeAssertion;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.Arrays;

/**
 * @author alex
 * @version $Revision$
 */
public class ServerPolicyFactoryTest extends TestCase {
    private ApplicationContext testApplicationContext = ApplicationContexts.getTestApplicationContext();

    /**
     * test <code>ServerPolicyFactoryTest</code> constructor
     */
    public ServerPolicyFactoryTest(String name) {
        super(name);
    }

    /**
     * create the <code>TestSuite</code> for the
     * ServerPolicyFactoryTest <code>TestCase</code>
     */
    public static Test suite() throws IOException {
        TestSuite suite = new TestSuite(ServerPolicyFactoryTest.class);
        return suite;
    }

    public void setUp() throws Exception {
        // put set up code here
    }

    public void tearDown() throws Exception {
        // put tear down code here
    }

    public void testCompleteness() throws Exception {

        ServerPolicyFactory pfac = (ServerPolicyFactory)testApplicationContext.getBean("policyFactory");

        ServerAssertion foo;
        Assertion[] everything = AllAssertions.GATEWAY_EVERYTHING;
        for (int i = 0; i < everything.length; i++) {
            foo = pfac.makeServerPolicy(everything[i]);
        }
    }

    public void testSimplePolicy() throws Exception {
        AllAssertion allTrue = new AllAssertion(Arrays.asList(new Assertion[]{
            new TrueAssertion(),
            new TrueAssertion(),
            new TrueAssertion()
        }));

        AllAssertion allFalse = new AllAssertion(Arrays.asList(new Assertion[]{
            new FalseAssertion(),
            new FalseAssertion(),
            new FalseAssertion()
        }));

        AllAssertion someFalse = new AllAssertion(Arrays.asList(new Assertion[]{
            new TrueAssertion(),
            new FalseAssertion(),
            new TrueAssertion()
        }));

        AllAssertion falseTree = new AllAssertion(Arrays.asList(new Assertion[]{
            new TrueAssertion(),
            someFalse,
            new TrueAssertion()}));

        AllAssertion trueTree = new AllAssertion(Arrays.asList(new Assertion[]{
            new TrueAssertion(),
            allTrue,
            new TrueAssertion()}));

        AllAssertion real = new AllAssertion(Arrays.asList(new Assertion[]{
            new HttpBasic(),
            new SpecificUser(),
            new RequestWssSaml(),
            new HttpRoutingAssertion()
        }));
        ServerPolicyFactory pfac = (ServerPolicyFactory)testApplicationContext.getBean("policyFactory");

        PolicyEnforcementContext pp = new PolicyEnforcementContext(new Message(), new Message());
        pp.setAuditContext(new AuditContextStub());

        ServerAssertion serverAllTrue = pfac.makeServerPolicy(allTrue);
        assertTrue(serverAllTrue.checkRequest(pp) == AssertionStatus.NONE);

        ServerAssertion serverAllFalse = pfac.makeServerPolicy(allFalse);
        assertTrue(serverAllFalse.checkRequest(pp) != AssertionStatus.NONE);

        ServerAssertion serverSomeFalse = pfac.makeServerPolicy(someFalse);
        assertTrue(serverSomeFalse.checkRequest(pp) != AssertionStatus.NONE);

        ServerAssertion serverFalseTree = pfac.makeServerPolicy(falseTree);
        assertTrue(serverFalseTree.checkRequest(pp) != AssertionStatus.NONE);

        ServerAssertion serverTrueTree = pfac.makeServerPolicy(trueTree);
        assertTrue(serverTrueTree.checkRequest(pp) == AssertionStatus.NONE);

        ServerAssertion serverReal = pfac.makeServerPolicy(real);
    }

    /**
     * Test the Echo individually - this assertion is part of the
     * test source tree only
     *
     * @throws Exception
     */
    public void testInstantiateEchoAssertion() throws Exception {
        AllAssertion echo = new AllAssertion(Arrays.asList(new Assertion[]{
            new Echo()
        }));
        PolicyEnforcementContext pp = new PolicyEnforcementContext(new Message(), new Message());
        pp.setAuditContext(new AuditContextStub());
        ServerPolicyFactory pfac = (ServerPolicyFactory)testApplicationContext.getBean("policyFactory");
        ServerAssertion serverAll = pfac.makeServerPolicy(echo);
        assertTrue(((ServerCompositeAssertion)serverAll).getChildren()[0] instanceof ServerEcho);
    }


    /**
     * Test <code>ServerPolicyFactoryTest</code> main.
     */
    public static void main(String[] args) throws
      Throwable {
        junit.textui.TestRunner.run(suite());
    }
}