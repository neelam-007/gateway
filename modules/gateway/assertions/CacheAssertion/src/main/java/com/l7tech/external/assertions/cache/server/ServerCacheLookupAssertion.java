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
import org.springframework.beans.factory.BeanFactory;

import java.io.IOException;
import java.util.Map;

import static com.l7tech.external.assertions.cache.CacheLookupAssertion.MAX_SECONDS_FOR_MAX_ENTRY_AGE;
import static com.l7tech.external.assertions.cache.CacheLookupAssertion.MIN_SECONDS_FOR_MAX_ENTRY_AGE;
import static com.l7tech.util.ValidationUtils.isValidLong;

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
        final String maxAgeExpression = assertion.getMaxEntryAgeSeconds();
        final String[] refs = Syntax.getReferencedNames(maxAgeExpression);
        if (refs.length > 0) {
            if (!Syntax.isOnlyASingleVariableReferenced(maxAgeExpression)) {
                throw new PolicyAssertionException(assertion, "Invalid value for max age. Only a single variable may be referenced: '" + maxAgeExpression + "'");
            }
        }
    }

    @Override
    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        Map<String, Object> vars = context.getVariableMap(variablesUsed, getAudit());
        final String cacheName = ExpandVariables.process(assertion.getCacheId(), vars, getAudit(), true);
        final String key = ExpandVariables.process(assertion.getCacheEntryKey(), vars, getAudit(), true);

        final String maxAcceptableAge = assertion.getMaxEntryAgeSeconds();
        final String cacheMaxAgeSeconds = ExpandVariables.process(maxAcceptableAge, vars, getAudit(), true);

        if (!isValidLong(cacheMaxAgeSeconds, false, MIN_SECONDS_FOR_MAX_ENTRY_AGE, MAX_SECONDS_FOR_MAX_ENTRY_AGE)) {
            logAndAudit(AssertionMessages.CACHE_LOOKUP_INVALID_MAX_AGE, cacheMaxAgeSeconds, String.valueOf(MIN_SECONDS_FOR_MAX_ENTRY_AGE), String.valueOf(MAX_SECONDS_FOR_MAX_ENTRY_AGE));
            return AssertionStatus.FAILED;
        }

        final long cacheMaxEntryAgeMillis = Long.parseLong(cacheMaxAgeSeconds) * 1000;

        SsgCache cache = cacheManager.getCache(cacheName);
        SsgCache.Entry cachedEntry = cache.lookup(key);
        if (cachedEntry == null || cachedEntry.getTimeStamp() < System.currentTimeMillis() - cacheMaxEntryAgeMillis) {
            logAndAudit(AssertionMessages.CACHE_LOOKUP_MISS, key);
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
