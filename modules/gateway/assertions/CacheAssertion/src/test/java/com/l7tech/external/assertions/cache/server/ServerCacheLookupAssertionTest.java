package com.l7tech.external.assertions.cache.server;

import com.l7tech.external.assertions.cache.CacheAssertionTest;
import com.l7tech.external.assertions.cache.CacheLookupAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.AuditFactory;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.test.BugNumber;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.MockConfig;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 */
public class ServerCacheLookupAssertionTest extends CacheAssertionTest {

    private TestAudit testAudit;

    private PolicyEnforcementContext initPolicyContext() {
        return PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
    }

    private ServerCacheLookupAssertion initServerCacheLookupAssertion(final String a) throws PolicyAssertionException {
        final CacheLookupAssertion assertion = new CacheLookupAssertion();
        assertion.setCacheEntryKey("");
        assertion.setCacheId("");
        assertion.setContentTypeOverride("");
        assertion.setMaxEntryAgeMillis(a);
        final ServerCacheLookupAssertion serverAssertion = new ServerCacheLookupAssertion(assertion, getBeanFactory());
        final AuditFactory auditFactory = new TestAudit().factory();
        testAudit = (TestAudit) auditFactory.newInstance(null, null);
        ApplicationContexts.inject(serverAssertion, CollectionUtils.<String, Object>mapBuilder()
                .put("serverConfig", new MockConfig(Collections.<String, String>emptyMap()))
                .put("auditFactory", auditFactory)
                .unmodifiableMap()
        );
        return serverAssertion;
    }

    @Test
    @BugNumber(12094)
    public void checkEntryAgeLessThanMin() throws Exception {
        PolicyEnforcementContext policyContext = initPolicyContext();
        final ServerCacheLookupAssertion serverAssertion = initServerCacheLookupAssertion("-1");
        AssertionStatus status = serverAssertion.checkRequest(policyContext);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.CACHE_LOOKUP_NOT_VAR_OR_LONG));
        assertEquals(AssertionStatus.FAILED, status);
    }

    @Test
    @BugNumber(12094)
    public void checkEntryAgeAsVariableLessThanMin() throws Exception {
        PolicyEnforcementContext policyContext = initPolicyContext();
        policyContext.setVariable("xyz", "-1");
        final ServerCacheLookupAssertion serverAssertion = initServerCacheLookupAssertion("${xyz}");
        AssertionStatus status = serverAssertion.checkRequest(policyContext);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.CACHE_LOOKUP_VAR_CONTENTS_ILLEGAL));
        assertEquals(AssertionStatus.FAILED, status);
    }

    @Test
    @BugNumber(12094)
    public void checkEntryAgeAsVariableGreaterThanMax() throws Exception {
        PolicyEnforcementContext policyContext = initPolicyContext();
        // The units are seconds so this value is too big when it is processed.
        policyContext.setVariable("xyz", Long.toString(Long.MAX_VALUE));
        final ServerCacheLookupAssertion serverAssertion = initServerCacheLookupAssertion("${xyz}");
        AssertionStatus status = serverAssertion.checkRequest(policyContext);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.CACHE_LOOKUP_VAR_CONTENTS_ILLEGAL));
        assertEquals(AssertionStatus.FAILED, status);
    }

}
