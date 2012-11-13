package com.l7tech.external.assertions.cache.server;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.external.assertions.cache.CacheLookupAssertion;
import com.l7tech.message.Message;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.external.assertions.cache.CacheStorageAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.util.ValidationUtils;
import org.springframework.beans.factory.BeanFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Server side implementation of the CacheStorageAssertion.
 *
 * @see com.l7tech.external.assertions.cache.CacheStorageAssertion
 */
public class ServerCacheStorageAssertion extends AbstractMessageTargetableServerAssertion<CacheStorageAssertion> {

    private final String[] variablesUsed;
    private final SsgCacheManager cacheManager;
    private final static String zeroString = Integer.toString(0);

    public ServerCacheStorageAssertion(CacheStorageAssertion assertion, BeanFactory beanFactory) throws PolicyAssertionException {
        super(assertion);
        this.variablesUsed = assertion.getVariablesUsed();
        this.cacheManager = SsgCacheManager.getInstance(beanFactory);
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        InputStream messageBody = null;
        final Map<String, Object> vars = context.getVariableMap(variablesUsed, getAudit());

        try {
            final int cacheMaxEntries;
            final String processedCacheMaxEntries = ExpandVariables.process(assertion.getMaxEntries(), vars, getAudit());
            if (ValidationUtils.isValidInteger(processedCacheMaxEntries, false, 0, CacheStorageAssertion.kMAX_ENTRIES)) {
                cacheMaxEntries = Integer.parseInt(processedCacheMaxEntries);
            } else {
                logAndAudit(AssertionMessages.CACHE_STORAGE_ERROR, "''" + processedCacheMaxEntries + "'' is not an integer between ''" + zeroString + "'' and ''" + Integer.toString(CacheStorageAssertion.kMAX_ENTRIES) + "'' inclusive");
                return AssertionStatus.FAILED;
            }

            final LongAndAssertionStatus result = determineMaxEntryAgeMillis(vars);
            if (result.status != null) return result.status;
            final long cacheMaxEntryAgeMillis = result.value;

            final long cacheMaxEntrySizeBytes;
            final String processedCacheMaxEntrySizeBytes = ExpandVariables.process(assertion.getMaxEntrySizeBytes(), vars, getAudit());
            if (ValidationUtils.isValidLong(processedCacheMaxEntrySizeBytes, false, 0, CacheStorageAssertion.kMAX_ENTRY_SIZE)) {
                cacheMaxEntrySizeBytes = Long.parseLong(processedCacheMaxEntrySizeBytes);
            } else {
                logAndAudit(AssertionMessages.CACHE_STORAGE_ERROR, "''" + processedCacheMaxEntrySizeBytes + "'' is not a long between ''" + zeroString + "'' and ''" + Long.toString(CacheStorageAssertion.kMAX_ENTRY_SIZE) + "'' inclusive");
                return AssertionStatus.FAILED;
            }

            final Message messageToCache = context.getTargetMessage(assertion, true);
            final ContentTypeHeader contentType = messageToCache.getMimeKnob().getOuterContentType();
            final String cacheId = ExpandVariables.process(assertion.getCacheId(), vars, getAudit());
            final String cacheEntryKey = ExpandVariables.process(assertion.getCacheEntryKey(), vars, getAudit());
            messageBody = messageToCache.getMimeKnob().getEntireMessageBodyAsInputStream();

            final SsgCache.Config cacheConfig = new SsgCache.Config(cacheId)
                    .maxEntries(cacheMaxEntries)
                    .maxAgeMillis(cacheMaxEntryAgeMillis)
                    .maxSizeBytes(cacheMaxEntrySizeBytes);
            final SsgCache cache = cacheManager.getCache(cacheConfig);
            if (assertion.isStoreSoapFaults() || ! isSoapFault(messageToCache)) {
                cache.store(cacheEntryKey, messageBody, contentType.getFullValue());
                logAndAudit(AssertionMessages.CACHE_STORAGE_STORED, cacheEntryKey);
            }
        } catch (NoSuchVariableException e) {
            logAndAudit(AssertionMessages.NO_SUCH_VARIABLE_WARNING, e.getVariable());
        } catch (Exception e) {
            logAndAudit(AssertionMessages.EXCEPTION_INFO_WITH_MORE_INFO, new String[] {"Unable to store cached value: " + ExceptionUtils.getMessage(e)}, e);
        } finally {
            if (messageBody != null) messageBody.close();
        }
        return AssertionStatus.NONE;
    }

    private boolean isSoapFault(Message messageToCache) {
        try {
            return messageToCache.isSoap() && messageToCache.getSoapKnob().isFault();
        } catch (Exception e) {
            return false;
        }
    }

    private LongAndAssertionStatus determineMaxEntryAgeMillis(final Map<String, Object> vars) {
        final String maxEntryAgeString = assertion.getMaxEntryAgeMillis();
        if (Syntax.isAnyVariableReferenced(maxEntryAgeString)) {
            // The units are seconds.
            final String processedCacheMaxEntryAgeSeconds = ExpandVariables.process(maxEntryAgeString, vars, getAudit(), true);
            if (ValidationUtils.isValidLong(processedCacheMaxEntryAgeSeconds, false, 0, CacheStorageAssertion.kMAX_ENTRY_AGE_SECONDS)) {
                return new LongAndAssertionStatus(Long.parseLong(processedCacheMaxEntryAgeSeconds) * 1000L);
            } else {
                logAndAudit(AssertionMessages.CACHE_STORAGE_ERROR, "''" + processedCacheMaxEntryAgeSeconds + "'' is not a long between ''" + zeroString + "'' and ''" + Long.toString(CacheStorageAssertion.kMAX_ENTRY_AGE_SECONDS) + "'' inclusive");
                return new LongAndAssertionStatus(AssertionStatus.FAILED);
            }
        } else {
            // The units are milliseconds.
            if (ValidationUtils.isValidLong(maxEntryAgeString, false, 0, CacheStorageAssertion.kMAX_ENTRY_AGE_MILLIS)) {
                return new LongAndAssertionStatus(Long.parseLong(maxEntryAgeString));
            } else {
                logAndAudit(AssertionMessages.CACHE_STORAGE_ERROR, "''" + maxEntryAgeString + "'' is not a long between ''" + zeroString + "'' and ''" + Long.toString(CacheStorageAssertion.kMAX_ENTRY_AGE_MILLIS) + "'' inclusive");
                return new LongAndAssertionStatus(AssertionStatus.FAILED);
            }
        }
    }

    private static class LongAndAssertionStatus {
        private final AssertionStatus status;
        private final long value;

        private LongAndAssertionStatus(final AssertionStatus status) {
            this.status = status;
            this.value = 0;
        }

        private LongAndAssertionStatus(final long value) {
            this.status = null;
            this.value = value;
        }
    }
}