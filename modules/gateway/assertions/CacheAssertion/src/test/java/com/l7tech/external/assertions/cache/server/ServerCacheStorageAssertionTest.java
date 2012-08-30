package com.l7tech.external.assertions.cache.server;

import com.l7tech.external.assertions.cache.CacheAssertionTest;
import com.l7tech.external.assertions.cache.CacheLookupAssertion;
import com.l7tech.external.assertions.cache.CacheStorageAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.AuditFactory;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.message.Message;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.wsp.WspConstants;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.test.BugNumber;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.MockConfig;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 */
public class ServerCacheStorageAssertionTest extends CacheAssertionTest {

    private TestAudit testAudit;

    private PolicyEnforcementContext initPolicyContext() {
        return PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
    }

    private ServerCacheStorageAssertion initServerCacheStorageAssertion(final String maxEntries, final String maxEntryAgeMillis, final String maxEntrySizeBytes) throws PolicyAssertionException {
        final CacheStorageAssertion assertion = new CacheStorageAssertion();
        assertion.setCacheEntryKey("a");
        assertion.setCacheId("b");
        assertion.setMaxEntries(maxEntries);
        assertion.setMaxEntryAgeMillis(maxEntryAgeMillis);
        assertion.setMaxEntrySizeBytes(maxEntrySizeBytes);
        final ServerCacheStorageAssertion serverAssertion = new ServerCacheStorageAssertion(assertion, getBeanFactory());
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
    public void checkMaxEntriesLessThanMin() throws Exception {
        PolicyEnforcementContext policyContext = initPolicyContext();
        final ServerCacheStorageAssertion serverAssertion = initServerCacheStorageAssertion("-1", "2", "3");
        AssertionStatus status = serverAssertion.checkRequest(policyContext);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.CACHE_STORAGE_ERROR));
        assertEquals(AssertionStatus.FAILED, status);
    }

    @Test
    @BugNumber(12094)
    public void checkMaxEntriesAsVariableLessThanMin() throws Exception {
        PolicyEnforcementContext policyContext = initPolicyContext();
        policyContext.setVariable("xyz", "-1");
        final ServerCacheStorageAssertion serverAssertion = initServerCacheStorageAssertion("${xyz}", "2", "3");
        AssertionStatus status = serverAssertion.checkRequest(policyContext);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.CACHE_STORAGE_ERROR));
        assertEquals(AssertionStatus.FAILED, status);
    }

    @Test
    @BugNumber(12094)
    public void checkEntryAgeLessThanMin() throws Exception {
        PolicyEnforcementContext policyContext = initPolicyContext();
        final ServerCacheStorageAssertion serverAssertion = initServerCacheStorageAssertion("2", "-1", "3");
        AssertionStatus status = serverAssertion.checkRequest(policyContext);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.CACHE_STORAGE_ERROR));
        assertEquals(AssertionStatus.FAILED, status);
    }

    @Test
    @BugNumber(12094)
    public void checkEntryAgeAsVariableLessThanMin() throws Exception {
        PolicyEnforcementContext policyContext = initPolicyContext();
        policyContext.setVariable("xyz", "-1");
        final ServerCacheStorageAssertion serverAssertion = initServerCacheStorageAssertion("2", "${xyz}", "3");
        AssertionStatus status = serverAssertion.checkRequest(policyContext);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.CACHE_STORAGE_ERROR));
        assertEquals(AssertionStatus.FAILED, status);
    }

    @Test
    @BugNumber(12094)
    public void checkEntryAgeAsVariableGreaterThanMax() throws Exception {
        PolicyEnforcementContext policyContext = initPolicyContext();
        // The units are seconds so this value is too big when it is processed.
        policyContext.setVariable("xyz", Long.toString(CacheStorageAssertion.kMAX_ENTRY_AGE_MILLIS));
        final ServerCacheStorageAssertion serverAssertion = initServerCacheStorageAssertion("2", "${xyz}", "3");
        AssertionStatus status = serverAssertion.checkRequest(policyContext);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.CACHE_STORAGE_ERROR));
        assertEquals(AssertionStatus.FAILED, status);
    }

    @Test
    @BugNumber(12094)
    public void checkMaxEntrySizeLessThanMin() throws Exception {
        PolicyEnforcementContext policyContext = initPolicyContext();
        final ServerCacheStorageAssertion serverAssertion = initServerCacheStorageAssertion("2", "3", "-1");
        AssertionStatus status = serverAssertion.checkRequest(policyContext);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.CACHE_STORAGE_ERROR));
        assertEquals(AssertionStatus.FAILED, status);
    }

    @Test
    @BugNumber(12094)
    public void checkMaxEntrySizeAsVariableLessThanMin() throws Exception {
        PolicyEnforcementContext policyContext = initPolicyContext();
        policyContext.setVariable("xyz", "-1");
        final ServerCacheStorageAssertion serverAssertion = initServerCacheStorageAssertion("2", "3", "${xyz}");
        AssertionStatus status = serverAssertion.checkRequest(policyContext);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.CACHE_STORAGE_ERROR));
        assertEquals(AssertionStatus.FAILED, status);
    }

}
