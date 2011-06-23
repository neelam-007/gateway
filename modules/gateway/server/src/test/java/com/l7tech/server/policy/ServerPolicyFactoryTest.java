package com.l7tech.server.policy;

import com.l7tech.server.ApplicationContexts;
import com.l7tech.message.Message;
import com.l7tech.policy.AllAssertions;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.policy.assertion.xmlsec.RequireWssSaml;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.policy.assertion.ServerUnknownAssertion;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import org.junit.Test;
import static org.junit.Assert.*;

import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.Arrays;

/**
 * @author alex
 * @version $Revision$
 */
public class ServerPolicyFactoryTest {
    private ApplicationContext testApplicationContext = ApplicationContexts.getTestApplicationContext();

    @Test
    public void testCompleteness() throws Exception {

        ServerPolicyFactory pfac = (ServerPolicyFactory)testApplicationContext.getBean("policyFactory");

        ServerAssertion foo;
        Assertion[] everything = AllAssertions.GATEWAY_EVERYTHING;
        for ( Assertion anEverything : everything ) {
            foo = pfac.compilePolicy( anEverything, false );
            if ( !( anEverything instanceof UnknownAssertion ) ) {
                assertFalse( foo instanceof ServerUnknownAssertion );
            }
        }
    }

    @Test
    public void testSimplePolicy() throws Exception {
        AllAssertion allTrue = new AllAssertion(Arrays.asList(
                new TrueAssertion(),
                new TrueAssertion(),
                new TrueAssertion() ));

        AllAssertion allFalse = new AllAssertion(Arrays.asList(
                new FalseAssertion(),
                new FalseAssertion(),
                new FalseAssertion() ));

        AllAssertion someFalse = new AllAssertion(Arrays.asList(
                new TrueAssertion(),
                new FalseAssertion(),
                new TrueAssertion() ));

        AllAssertion falseTree = new AllAssertion(Arrays.asList(
                new TrueAssertion(),
                someFalse,
                new TrueAssertion() ));

        AllAssertion trueTree = new AllAssertion(Arrays.asList(
                new TrueAssertion(),
                allTrue,
                new TrueAssertion() ));

        AllAssertion real = new AllAssertion(Arrays.asList(
                new HttpBasic(),
                new SpecificUser(),
                new RequireWssSaml(),
                new HttpRoutingAssertion() ));
        ServerPolicyFactory pfac = (ServerPolicyFactory)testApplicationContext.getBean("policyFactory");

        PolicyEnforcementContext pp = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message(), false);

        ServerAssertion serverAllTrue = pfac.compilePolicy(allTrue, true);
        assertTrue(serverAllTrue.checkRequest(pp) == AssertionStatus.NONE);

        ServerAssertion serverAllFalse = pfac.compilePolicy(allFalse, true);
        assertTrue(serverAllFalse.checkRequest(pp) != AssertionStatus.NONE);

        ServerAssertion serverSomeFalse = pfac.compilePolicy(someFalse, true);
        assertTrue(serverSomeFalse.checkRequest(pp) != AssertionStatus.NONE);

        ServerAssertion serverFalseTree = pfac.compilePolicy(falseTree, true);
        assertTrue(serverFalseTree.checkRequest(pp) != AssertionStatus.NONE);

        ServerAssertion serverTrueTree = pfac.compilePolicy(trueTree, true);
        assertTrue(serverTrueTree.checkRequest(pp) == AssertionStatus.NONE);

        pfac.compilePolicy(real, true);
    }

    public static class ServerRunnablesAssertion extends AbstractServerAssertion<RunnablesAssertion> {
        public ServerRunnablesAssertion(RunnablesAssertion assertion) {
            super(assertion);
            if (assertion.ctorRunnable != null) assertion.ctorRunnable.run();
        }

        @Override
        public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
            if (assertion.checkRequestRunnable != null) assertion.checkRequestRunnable.run();
            return AssertionStatus.NONE;
        }
    }

    public static class RunnablesAssertion extends Assertion {
        Runnable ctorRunnable;
        Runnable checkRequestRunnable;

        public RunnablesAssertion() {
        }

        RunnablesAssertion(Runnable ctorRunnable, Runnable checkRequestRunnable) {
            this.ctorRunnable = ctorRunnable;
            this.checkRequestRunnable = checkRequestRunnable;
        }

        @Override
        public AssertionMetadata meta() {
            DefaultAssertionMetadata meta = super.defaultMeta();
            meta.put(AssertionMetadata.SERVER_ASSERTION_CLASSNAME, ServerRunnablesAssertion.class.getName());
            return meta;
        }
    }

    private static final String EXCEPTION_MESS = "this is a RuntimeException";
    private static final Runnable EXCEPTION_THROWER = new Runnable() {
        @Override
        public void run() {
            throw new RuntimeException(EXCEPTION_MESS);
        }
    };

    private static final String ERROR_MESS = "This is an Error";
    private static final Runnable ERROR_THROWER = new Runnable() {
        @Override
        public void run() {
            throw new Error(ERROR_MESS);
        }
    };
    
    @Test
    public void testCtorThrowsException() {
        instantiateThrower(EXCEPTION_THROWER, EXCEPTION_MESS);
    }

    @Test
    public void testCtorThrowsError() {
        instantiateThrower(ERROR_THROWER, ERROR_MESS);
    }

    private void instantiateThrower(Runnable ctorThrower, String expectedExceptionString) {
        ServerPolicyFactory pfac = (ServerPolicyFactory)testApplicationContext.getBean("policyFactory");
        try {
            pfac.compilePolicy(new RunnablesAssertion(ctorThrower, null), false);
            fail("Expected exception was not thrown");
        } catch (ServerPolicyException e) {
            assertTrue("Exception message did not contain expected substring \"" + expectedExceptionString + "\".  Exception message: " + e.getMessage(),
                    e.getMessage().contains(expectedExceptionString));
        } catch (Throwable t) {
            fail("Expected exception was not thrown");
        }
    }

}