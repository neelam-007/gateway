package com.l7tech.external.assertions.cache.server;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.external.assertions.cache.CacheLookupAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ValidationUtils;
import org.springframework.beans.factory.BeanFactory;

import java.io.IOException;
import java.util.Map;

/**
 * Server side implementation of the CacheLookupAssertion.
 *
 * @see com.l7tech.external.assertions.cache.CacheLookupAssertion
 */
public class ServerCacheLookupAssertion extends AbstractMessageTargetableServerAssertion<CacheLookupAssertion> {
    private final String[] variablesUsed;
    private final SsgCacheManager cacheManager;

    public ServerCacheLookupAssertion(final CacheLookupAssertion assertion, final BeanFactory beanFactory) throws PolicyAssertionException {
        super(assertion);
        this.variablesUsed = assertion.getVariablesUsed();
        this.cacheManager = SsgCacheManager.getInstance(beanFactory);
    }

    @Override
    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        Map<String, Object> vars = context.getVariableMap(variablesUsed, getAudit());
        final String cacheName = ExpandVariables.process(assertion.getCacheId(), vars, getAudit(), true);
        final String key = ExpandVariables.process(assertion.getCacheEntryKey(), vars, getAudit(), true);

        final long minMillis = CacheLookupAssertion.MIN_MILLIS_FOR_MAX_ENTRY_AGE;
        final long maxMillis = CacheLookupAssertion.MAX_MILLIS_FOR_MAX_ENTRY_AGE;
        final String value = assertion.getMaxEntryAgeMillis();
        final long cacheMaxEntryAgeMillis;

        if (Syntax.isOnlyASingleVariableReferenced(value) || ValidationUtils.isValidLong(value, false, minMillis, maxMillis)) {
            if (Syntax.isOnlyASingleVariableReferenced(value)) {
                final String cacheMaxEntryAgeSecondsString = ExpandVariables.process(value, vars, getAudit(), true);
                final long minSeconds = CacheLookupAssertion.MIN_SECONDS_FOR_MAX_ENTRY_AGE;
                final long maxSeconds = CacheLookupAssertion.MAX_SECONDS_FOR_MAX_ENTRY_AGE;
                if (ValidationUtils.isValidLong(cacheMaxEntryAgeSecondsString, false, minSeconds, maxSeconds)) {
                    cacheMaxEntryAgeMillis = Long.parseLong(cacheMaxEntryAgeSecondsString) * 1000L;
                } else {
                    logAndAudit(AssertionMessages.CACHE_LOOKUP_VAR_CONTENTS_ILLEGAL, value, cacheMaxEntryAgeSecondsString, Long.toString(minSeconds), Long.toString(maxSeconds));
                    return AssertionStatus.FAILED;
                }
            } else {
                cacheMaxEntryAgeMillis = Long.parseLong(value);
            }
        } else {
            logAndAudit(AssertionMessages.CACHE_LOOKUP_NOT_VAR_OR_LONG, value, Long.toString(minMillis), Long.toString(maxMillis));
            return AssertionStatus.FAILED;
        }

        SsgCache cache = cacheManager.getCache(cacheName);
        SsgCache.Entry cachedEntry = cache.lookup(key);
        if (cachedEntry == null || cachedEntry.getTimeStamp() < System.currentTimeMillis() - cacheMaxEntryAgeMillis) {
            return AssertionStatus.FALSIFIED;
        }

        logAndAudit(AssertionMessages.CACHE_LOOKUP_RETRIEVED, key);

        Message message = null;
        switch (assertion.getTarget()) {
            case REQUEST:
                message = context.getRequest();
                break;
            case RESPONSE:
                message = context.getResponse();
                break;
            case OTHER:
                message = new Message();
                context.setVariable(assertion.getOtherTargetMessageVariable(), message);
        }

        try {
            if (message == null) {
                logAndAudit(AssertionMessages.USERDETAIL_WARNING, "Null message target: " + assertion.getTarget());
                return AssertionStatus.FAILED;
            }
            String cachedContentType = cachedEntry.getContentType();
            String contentTypeOverride = assertion.getContentTypeOverride();
            ContentTypeHeader contentType = contentTypeOverride != null && !contentTypeOverride.isEmpty() ? ContentTypeHeader.create(contentTypeOverride) :
                cachedContentType != null ? ContentTypeHeader.create(cachedContentType) :
                    ContentTypeHeader.XML_DEFAULT;

            byte[] bodyBytes;
            try {
                bodyBytes = new byte[ cachedEntry.getDataSize() ];
                cachedEntry.putData( bodyBytes );
            } catch (IOException e) {
                logAndAudit(AssertionMessages.USERDETAIL_WARNING, "Exception while retrieving cached information: " + ExceptionUtils.getMessage(e));
                return AssertionStatus.FAILED;
            } catch (NoSuchPartException e) {
                logAndAudit(AssertionMessages.USERDETAIL_WARNING, "Exception while retrieving cached information: " + ExceptionUtils.getMessage(e)); // can't happen
                return AssertionStatus.FAILED;
            }
            // TODO use proper hybrid stash manager, making arrangements to have it closed when context is closed,
            // instead of just using two-arg initialize() which just uses ByteArrayStashManager
            message.initialize(contentType, bodyBytes);
            return AssertionStatus.NONE;
        } catch (IOException e) {
            logAndAudit(AssertionMessages.USERDETAIL_WARNING, "Message cache error: " + ExceptionUtils.getMessage(e));
            return AssertionStatus.FAILED;
        }
    }

}
