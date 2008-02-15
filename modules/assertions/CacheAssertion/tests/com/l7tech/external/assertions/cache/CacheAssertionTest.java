package com.l7tech.external.assertions.cache;

import com.l7tech.common.message.Message;
import com.l7tech.external.assertions.cache.server.ServerCacheLookupAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.ServerConfigStub;
import com.l7tech.server.message.PolicyEnforcementContext;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.springframework.context.ApplicationContext;

import java.util.logging.Logger;

/**
 * Test the CacheStorageAssertion and the CacheLookupAssertion.
 */
public class CacheAssertionTest extends TestCase {

    private static final Logger log = Logger.getLogger(CacheAssertionTest.class.getName());
    private static ApplicationContext applicationContext;
    private static ServerConfigStub serverConfig;

    public CacheAssertionTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(CacheAssertionTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testFeatureNames() throws Exception {
        assertEquals("assertion:CacheStorage", new CacheStorageAssertion().getFeatureSetName());
        assertEquals("assertion:CacheLookup", new CacheLookupAssertion().getFeatureSetName());
    }
    
    public void testSomething() throws Exception {
        ServerCacheLookupAssertion smcass = new ServerCacheLookupAssertion(new CacheLookupAssertion(), null);
        PolicyEnforcementContext ctx = makeContext();
        AssertionStatus result = smcass.checkRequest(ctx);
        //assertEquals(result, AssertionStatus.NONE);
    }

    private PolicyEnforcementContext makeContext() {
        Message request = new Message();
        Message response = new Message();
        PolicyEnforcementContext context = new PolicyEnforcementContext(request, response);
        return context;
    }
}
