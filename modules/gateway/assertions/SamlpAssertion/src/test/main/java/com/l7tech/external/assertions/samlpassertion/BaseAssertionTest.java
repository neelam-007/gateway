package com.l7tech.external.assertions.samlpassertion;

import com.l7tech.common.ApplicationContexts;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.wsp.WspConstants;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.server.policy.ServerPolicyException;
import com.l7tech.server.policy.ServerPolicyFactory;
import junit.framework.TestCase;
import org.springframework.context.ApplicationContext;

import java.io.IOException;

/**
 * User: vchan
 */
public abstract class BaseAssertionTest<A extends Assertion> extends TestCase {

    protected ApplicationContext appCtx;
    protected WspReader policyReader;
    protected ServerPolicyFactory factory;

    protected void setUp() throws Exception {

        // get the spring app context
        if (appCtx == null) {
            appCtx = ApplicationContexts.getTestApplicationContext();
            assertNotNull("Fail - Unable to get applicationContext instance", appCtx);
        }

        // grab the wspReader bean from spring
        if (policyReader == null) {
//            policyReader = (WspReader) appCtx.getBean("wspReader", WspReader.class);

            final AssertionRegistry tmf = new AssertionRegistry();
            tmf.setApplicationContext(appCtx);
            tmf.registerAssertion(SamlpRequestBuilderAssertion.class);
            tmf.registerAssertion(SamlpResponseEvaluationAssertion.class);
            WspConstants.setTypeMappingFinder(tmf);
            policyReader = new WspReader(tmf);

            assertNotNull("Fail - Unable to obtain the WspReader bean from the application context.", policyReader);
        }

        // grab the policyFactory
        if (factory == null) {
            factory = (ServerPolicyFactory) appCtx.getBean("policyFactory", ServerPolicyFactory.class);
            assertNotNull("Fail - Unable to obtain \"policyFactory\" from the application context", factory);
        }
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }


    protected A parseAssertionFromXml(String policyXml) throws IOException, ServerPolicyException {

        Assertion as = policyReader.parsePermissively(policyXml);
        assertNotNull(as);
        assertTrue(as instanceof AllAssertion);
        AllAssertion all = (AllAssertion) as;

        for (Object obj : all.getChildren()) {

            if (isAssertionClass(obj)) {
                return castAssertionClass(obj);
            }
        }
        return null;
    }


    protected abstract boolean isAssertionClass(Object obj);

    protected abstract A castAssertionClass(Object obj);

}
