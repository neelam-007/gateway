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

        if (! CacheStorageAssertion.isSingleVariableOrIntegerWithinRange(assertion.getMaxEntries(), 0, CacheStorageAssertion.kMAX_ENTRIES)) {
            throw new PolicyAssertionException(assertion, "maxEntries value of ''" + assertion.getMaxEntries() + "'' is not single variable reference or an integer between ''" + zeroString + "'' and ''" + Integer.toString(CacheStorageAssertion.kMAX_ENTRIES) + "'' inclusive");
        }

        if (! CacheLookupAssertion.isSingleVariableOrLongWithinRange(assertion.getMaxEntryAgeMillis(), 0, CacheStorageAssertion.kMAX_ENTRY_AGE_MILLIS)) {
            throw new PolicyAssertionException(assertion, "maxEntryAgeMillis value of ''" + assertion.getMaxEntryAgeMillis() + "'' is not single variable reference or a long between ''" + zeroString + "'' and ''" + Long.toString(CacheStorageAssertion.kMAX_ENTRY_AGE_MILLIS) + "'' inclusive");
        }

        if (! CacheLookupAssertion.isSingleVariableOrLongWithinRange(assertion.getMaxEntrySizeBytes(), 0, CacheStorageAssertion.kMAX_ENTRY_SIZE)) {
            throw new PolicyAssertionException(assertion, "maxEntrySizeBytes value of ''" + assertion.getMaxEntrySizeBytes() + "'' is not single variable reference or a long between ''" + zeroString + "'' and ''" + Long.toString(CacheStorageAssertion.kMAX_ENTRY_SIZE) + "'' inclusive");
        }
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        InputStream messageBody = null;
        final Map<String, Object> vars = context.getVariableMap(variablesUsed, getAudit());

        try {
            final int cacheMaxEntries;
            final String processedCacheMaxEntries = ExpandVariables.process(assertion.getMaxEntries(), vars, getAudit(), true);
            if (CacheStorageAssertion.isIntegerWithinRange(processedCacheMaxEntries, 0, CacheStorageAssertion.kMAX_ENTRIES)) {
                cacheMaxEntries = Integer.parseInt(processedCacheMaxEntries);
            } else {
                logAndAudit(AssertionMessages.CACHE_STORAGE_ERROR, "''" + processedCacheMaxEntries + "'' is not an integer between ''" + zeroString + "'' and ''" + Integer.toString(CacheStorageAssertion.kMAX_ENTRIES) + "'' inclusive");
                return AssertionStatus.FAILED;
            }

            final long cacheMaxEntryAgeMillis;
            final String maxEntryAgeString = assertion.getMaxEntryAgeMillis();
            if (Syntax.isOnlyASingleVariableReferenced(maxEntryAgeString)) {
                final String processedCacheMaxEntryAgeSeconds = ExpandVariables.process(maxEntryAgeString, vars, getAudit(), true);
                if (CacheLookupAssertion.isLongWithinRange(processedCacheMaxEntryAgeSeconds, 0, CacheStorageAssertion.kMAX_ENTRY_AGE_SECONDS)) {
                    cacheMaxEntryAgeMillis = Long.parseLong(processedCacheMaxEntryAgeSeconds) * 1000L;
                } else {
                    logAndAudit(AssertionMessages.CACHE_STORAGE_ERROR, "''" + processedCacheMaxEntryAgeSeconds + "'' is not a long between ''" + zeroString + "'' and ''" + Long.toString(CacheStorageAssertion.kMAX_ENTRY_AGE_SECONDS) + "'' inclusive");
                    return AssertionStatus.FAILED;
                }
            } else {
                cacheMaxEntryAgeMillis = Long.parseLong(maxEntryAgeString);
            }

            final long cacheMaxEntrySizeBytes;
            final String processedCacheMaxEntrySizeBytes = ExpandVariables.process(assertion.getMaxEntrySizeBytes(), vars, getAudit(), true);
            if (CacheLookupAssertion.isLongWithinRange(processedCacheMaxEntrySizeBytes, 0, CacheStorageAssertion.kMAX_ENTRY_SIZE)) {
                cacheMaxEntrySizeBytes = Long.parseLong(processedCacheMaxEntrySizeBytes);
            } else {
                logAndAudit(AssertionMessages.CACHE_STORAGE_ERROR, "''" + processedCacheMaxEntrySizeBytes + "'' is not a long between ''" + zeroString + "'' and ''" + Long.toString(CacheStorageAssertion.kMAX_ENTRY_SIZE) + "'' inclusive");
                return AssertionStatus.FAILED;
            }

            final Message messageToCache = context.getTargetMessage(assertion, true);
            final ContentTypeHeader contentType = messageToCache.getMimeKnob().getOuterContentType();
            final String cacheId = ExpandVariables.process(assertion.getCacheId(), vars, getAudit(), true);
            final String cacheEntryKey = ExpandVariables.process(assertion.getCacheEntryKey(), vars, getAudit(), true);
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
}