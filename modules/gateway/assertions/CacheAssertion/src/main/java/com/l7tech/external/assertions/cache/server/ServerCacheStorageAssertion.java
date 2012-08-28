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
            if (CacheStorageAssertion.isSingleVariableOrIntegerWithinRange(assertion.getMaxEntries(), 0, CacheStorageAssertion.kMAX_ENTRIES)) {
                if (Syntax.isOnlyASingleVariableReferenced(assertion.getMaxEntries())) {
                    final String value = ExpandVariables.process(assertion.getMaxEntries(), vars, getAudit(), true);
                    if (CacheStorageAssertion.isIntegerWithinRange(value, 0, CacheStorageAssertion.kMAX_ENTRIES)) {
                        cacheMaxEntries = Integer.parseInt(value);
                    } else {
                        logAndAudit(AssertionMessages.CACHE_STORAGE_VAR_CONTENTS_NOT_INTEGER, assertion.getMaxEntries(), value, Integer.toString(0), Integer.toString(CacheStorageAssertion.kMAX_ENTRIES));
                        return AssertionStatus.FAILED;
                    }
                } else {
                    cacheMaxEntries = Integer.parseInt(assertion.getMaxEntries());
                }
            } else {
                logAndAudit(AssertionMessages.CACHE_STORAGE_NOT_VAR_OR_INTEGER, assertion.getMaxEntries(), Integer.toString(0), Integer.toString(CacheStorageAssertion.kMAX_ENTRIES));
                return AssertionStatus.FAILED;
            }

            final long minMillis = 0;
            final long maxMillis = CacheStorageAssertion.kMAX_ENTRY_AGE_MILLIS;
            final String maxEntryAgeString = assertion.getMaxEntryAgeMillis();
            final long cacheMaxEntryAgeMillis;
            if (CacheLookupAssertion.isSingleVariableOrLongWithinRange(maxEntryAgeString, minMillis, maxMillis)) {
                if (Syntax.isOnlyASingleVariableReferenced(maxEntryAgeString)) {
                    final String cacheMaxEntryAgeSecondsString = ExpandVariables.process(maxEntryAgeString, vars, getAudit(), true);
                    final long minSeconds = 0;
                    final long maxSeconds = CacheStorageAssertion.kMAX_ENTRY_AGE_SECONDS;
                    if (CacheLookupAssertion.isLongWithinRange(cacheMaxEntryAgeSecondsString, minSeconds, maxSeconds)) {
                        cacheMaxEntryAgeMillis = Long.parseLong(cacheMaxEntryAgeSecondsString) * 1000L;
                    } else {
                        logAndAudit(AssertionMessages.CACHE_STORAGE_VAR_CONTENTS_NOT_LONG, maxEntryAgeString, cacheMaxEntryAgeSecondsString, Long.toString(minSeconds), Long.toString(maxSeconds));
                        return AssertionStatus.FAILED;
                    }
                } else {
                    cacheMaxEntryAgeMillis = Long.parseLong(maxEntryAgeString);
                }
            } else {
                logAndAudit(AssertionMessages.CACHE_STORAGE_NOT_VAR_OR_LONG, maxEntryAgeString, Long.toString(minMillis), Long.toString(maxMillis));
                return AssertionStatus.FAILED;
            }

            final String maxEntrySizeString = assertion.getMaxEntrySizeBytes();
            final long cacheMaxEntrySizeBytes;
            if (CacheLookupAssertion.isSingleVariableOrLongWithinRange(maxEntrySizeString, 0, CacheStorageAssertion.kMAX_ENTRY_SIZE)) {
                if (Syntax.isOnlyASingleVariableReferenced(maxEntrySizeString)) {
                    final String value = ExpandVariables.process(maxEntrySizeString, vars, getAudit(), true);
                    if (CacheLookupAssertion.isLongWithinRange(value, 0, CacheStorageAssertion.kMAX_ENTRY_SIZE)) {
                        cacheMaxEntrySizeBytes = Long.parseLong(value);
                    } else {
                        logAndAudit(AssertionMessages.CACHE_STORAGE_VAR_CONTENTS_NOT_LONG, maxEntrySizeString, value, Long.toString(0), Long.toString(CacheStorageAssertion.kMAX_ENTRY_SIZE));
                        return AssertionStatus.FAILED;
                    }
                } else {
                    cacheMaxEntrySizeBytes = Long.parseLong(maxEntrySizeString);
                }
            } else {
                logAndAudit(AssertionMessages.CACHE_STORAGE_NOT_VAR_OR_LONG, maxEntrySizeString, Long.toString(0), Long.toString(CacheStorageAssertion.kMAX_ENTRY_SIZE));
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