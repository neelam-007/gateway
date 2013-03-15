package com.l7tech.external.assertions.cache.server;

import com.l7tech.external.assertions.cache.CacheAssertionTest;
import com.l7tech.external.assertions.cache.CacheStorageAssertion;
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

import java.text.MessageFormat;
import java.util.Collections;

import static com.l7tech.external.assertions.cache.CacheStorageAssertion.kMAX_ENTRY_AGE_SECONDS;
import static com.l7tech.gateway.common.audit.AssertionMessages.CACHE_STORAGE_INVALID_VALUE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 */
public class ServerCacheStorageAssertionTest extends CacheAssertionTest {

    private final Long maxEntryAgeSeconds = 100000000L;
    private TestAudit testAudit;

    private PolicyEnforcementContext initPolicyContext() {
        return PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
    }

    private ServerCacheStorageAssertion initServerCacheStorageAssertion(final String maxEntries, final String maxEntryAgeSeconds, final String maxEntrySizeBytes) throws PolicyAssertionException {
        final CacheStorageAssertion assertion = new CacheStorageAssertion();
        assertion.setCacheEntryKey("a");
        assertion.setCacheId("b");
        assertion.setMaxEntries(maxEntries);
        assertion.setMaxEntryAgeSeconds(maxEntryAgeSeconds);
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
        assertTrue(testAudit.isAuditPresent(AssertionMessages.CACHE_STORAGE_INVALID_VALUE));
        assertEquals(AssertionStatus.FAILED, status);
    }

    @Test
    @BugNumber(12094)
    public void checkMaxEntriesAsVariableLessThanMin() throws Exception {
        PolicyEnforcementContext policyContext = initPolicyContext();
        policyContext.setVariable("xyz", "-1");
        final ServerCacheStorageAssertion serverAssertion = initServerCacheStorageAssertion("${xyz}", "2", "3");
        AssertionStatus status = serverAssertion.checkRequest(policyContext);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.CACHE_STORAGE_INVALID_VALUE));
        assertEquals(AssertionStatus.FAILED, status);
    }

    @Test
    @BugNumber(12094)
    public void checkEntryAgeLessThanMin() throws Exception {
        PolicyEnforcementContext policyContext = initPolicyContext();
        final ServerCacheStorageAssertion serverAssertion = initServerCacheStorageAssertion("2", "-1", "3");
        AssertionStatus status = serverAssertion.checkRequest(policyContext);
        for (String s : testAudit) {
            System.out.println(s);
        }

        assertTrue(testAudit.isAuditPresent(AssertionMessages.CACHE_STORAGE_INVALID_VALUE));

        final String format = MessageFormat.format(CACHE_STORAGE_INVALID_VALUE.getMessage(), "Resolved maximum entry age value is invalid '-1'. Value must be seconds between '0' and '" + String.valueOf(kMAX_ENTRY_AGE_SECONDS) + "' inclusive.");
        System.out.println(format);
        assertTrue(testAudit.isAuditPresentContaining(format));

        assertEquals(AssertionStatus.FAILED, status);
    }

    @Test
    @BugNumber(12094)
    public void checkEntryAgeAsVariableLessThanMin() throws Exception {
        PolicyEnforcementContext policyContext = initPolicyContext();
        policyContext.setVariable("xyz", "-1");
        final ServerCacheStorageAssertion serverAssertion = initServerCacheStorageAssertion("2", "${xyz}", "3");
        AssertionStatus status = serverAssertion.checkRequest(policyContext);
        for (String s : testAudit) {
            System.out.println(s);
        }
        assertTrue(testAudit.isAuditPresent(AssertionMessages.CACHE_STORAGE_INVALID_VALUE));

        final String format = MessageFormat.format(CACHE_STORAGE_INVALID_VALUE.getMessage(), "Resolved maximum entry age value is invalid '-1'. Value must be seconds between '0' and '" + String.valueOf(kMAX_ENTRY_AGE_SECONDS) + "' inclusive.");
        System.out.println(format);
        assertTrue(testAudit.isAuditPresentContaining(format));

        assertEquals(AssertionStatus.FAILED, status);
    }

    @Test
    @BugNumber(12094)
    public void checkEntryAgeAsVariableGreaterThanMax() throws Exception {
        PolicyEnforcementContext policyContext = initPolicyContext();
        // The units are seconds so this value is too big when it is processed.
        policyContext.setVariable("xyz", Long.toString(kMAX_ENTRY_AGE_SECONDS + 1));
        final ServerCacheStorageAssertion serverAssertion = initServerCacheStorageAssertion("2", "${xyz}", "3");
        AssertionStatus status = serverAssertion.checkRequest(policyContext);
        for (String s : testAudit) {
            System.out.println(s);
        }
        assertTrue(testAudit.isAuditPresent(CACHE_STORAGE_INVALID_VALUE));
        final String format = MessageFormat.format(CACHE_STORAGE_INVALID_VALUE.getMessage(), "Resolved maximum entry age value is invalid '" + Long.toString(kMAX_ENTRY_AGE_SECONDS + 1) + "'. Value must be seconds between '0' and '" + String.valueOf(kMAX_ENTRY_AGE_SECONDS) + "' inclusive.");
        System.out.println(format);
        assertTrue(testAudit.isAuditPresentContaining(format));
        assertEquals(AssertionStatus.FAILED, status);
    }

    @Test
    @BugNumber(12094)
    public void checkMaxEntrySizeLessThanMin() throws Exception {
        PolicyEnforcementContext policyContext = initPolicyContext();
        final ServerCacheStorageAssertion serverAssertion = initServerCacheStorageAssertion("2", "3", "-1");
        AssertionStatus status = serverAssertion.checkRequest(policyContext);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.CACHE_STORAGE_INVALID_VALUE));
        assertEquals(AssertionStatus.FAILED, status);
    }

    @Test
    @BugNumber(12094)
    public void checkMaxEntrySizeAsVariableLessThanMin() throws Exception {
        PolicyEnforcementContext policyContext = initPolicyContext();
        policyContext.setVariable("xyz", "-1");
        final ServerCacheStorageAssertion serverAssertion = initServerCacheStorageAssertion("2", "3", "${xyz}");
        AssertionStatus status = serverAssertion.checkRequest(policyContext);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.CACHE_STORAGE_INVALID_VALUE));
        assertEquals(AssertionStatus.FAILED, status);
    }

    @Test
    @BugNumber(13194)
    public void testCheckRequestWithInvalidContextVariableContentType_AssertionFailed() throws Exception {
        PolicyEnforcementContext policyContext = initPolicyContext();
        policyContext.setVariable("xyz", new Message());

        final ServerCacheStorageAssertion serverAssertion = initServerCacheStorageAssertion("1", "1", "${xyz}");

        AssertionStatus status = serverAssertion.checkRequest(policyContext);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.CACHE_STORAGE_INVALID_VALUE));
        assertEquals(AssertionStatus.FAILED, status);
    }

    /**
     * The maximum second value that may be entered is 100000000L
     * @throws Exception
     */
    @Test
    public void testMaxSecondValue() throws Exception {
        PolicyEnforcementContext policyContext = initPolicyContext();
        final ServerCacheStorageAssertion serverAssertion = initServerCacheStorageAssertion("2", String.valueOf(maxEntryAgeSeconds), "34323");
        AssertionStatus status = serverAssertion.checkRequest(policyContext);
        for (String s : testAudit) {
            System.out.println(s);
        }

        assertEquals(AssertionStatus.NONE, status);
    }

    /**
     * Tests the boundary condition of the maximum entry age milliseconds.
     * @throws Exception
     */
    @Test
    public void testMaxSecondValueBoundaryCondition() throws Exception {
        PolicyEnforcementContext policyContext = initPolicyContext();
        final ServerCacheStorageAssertion serverAssertion = initServerCacheStorageAssertion("2", String.valueOf(maxEntryAgeSeconds + 1), "3");
        AssertionStatus status = serverAssertion.checkRequest(policyContext);
        for (String s : testAudit) {
            System.out.println(s);
        }
        assertTrue(testAudit.isAuditPresent(CACHE_STORAGE_INVALID_VALUE));
        final String format = MessageFormat.format(CACHE_STORAGE_INVALID_VALUE.getMessage(), "Resolved maximum entry age value is invalid '" + String.valueOf(maxEntryAgeSeconds + 1) + "'. Value must be seconds between '0' and '" + String.valueOf(kMAX_ENTRY_AGE_SECONDS) + "' inclusive.");
        System.out.println(format);
        assertTrue(testAudit.isAuditPresentContaining(format));

        assertEquals(AssertionStatus.FAILED, status);
    }

    @Test(expected = PolicyAssertionException.class)
    @BugNumber(13188)
    public void testMoreThanOneVariableForMaxEntryAgeThrowsInConstructor() throws Exception {
        initServerCacheStorageAssertion("20", "${var1}${var2}", "2000");
    }

    @Test(expected = PolicyAssertionException.class)
    @BugNumber(13188)
    public void testMoreThanOneVariableForMaxSizeThrowsInConstructor() throws Exception {
        initServerCacheStorageAssertion("20", "${var1}", "${var1}${var2}");
    }

    @Test(expected = Exception.class)
    @BugNumber(13188)
    public void testMoreThanOneVariableForMaxEntriesThrowsInConstructor() throws Exception {
        initServerCacheStorageAssertion("${var1}${var2}", "${var1}", "${var1}");
    }

}
