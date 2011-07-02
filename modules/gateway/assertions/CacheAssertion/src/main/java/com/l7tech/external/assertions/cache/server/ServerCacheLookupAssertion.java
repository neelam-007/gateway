package com.l7tech.external.assertions.cache.server;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.external.assertions.cache.CacheLookupAssertion;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.ExceptionUtils;
import org.springframework.beans.factory.BeanFactory;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;

/**
 * Server side implementation of the CacheLookupAssertion.
 *
 * @see com.l7tech.external.assertions.cache.CacheLookupAssertion
 */
public class ServerCacheLookupAssertion extends AbstractMessageTargetableServerAssertion<CacheLookupAssertion> {
    private final String[] variablesUsed;
    private final SsgCacheManager cacheManager;

    public ServerCacheLookupAssertion(CacheLookupAssertion assertion, BeanFactory beanFactory) throws PolicyAssertionException {
        super(assertion, assertion);
        this.variablesUsed = assertion.getVariablesUsed();
        this.cacheManager = SsgCacheManager.getInstance(beanFactory);
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        Map<String, Object> vars = context.getVariableMap(variablesUsed, getAudit());
        final String cacheName = ExpandVariables.process(assertion.getCacheId(), vars, getAudit(), true);
        final String key = ExpandVariables.process(assertion.getCacheEntryKey(), vars, getAudit(), true);
        SsgCache cache = cacheManager.getCache(cacheName);

        SsgCache.Entry cachedEntry = cache.lookup(key);
        if (cachedEntry == null || cachedEntry.getTimeStamp() < System.currentTimeMillis() - assertion.getMaxEntryAgeMillis())
            return AssertionStatus.FALSIFIED;

        logger.log(Level.FINE, "Retrieved from cache: " + key);

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
                logger.log(Level.WARNING, "Null message target: " + assertion.getTarget());
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
                logger.log(Level.WARNING, "Exception while retrieving cached information: " + ExceptionUtils.getMessage(e), e);
                return AssertionStatus.FAILED;
            } catch (NoSuchPartException e) {
                logger.log(Level.WARNING, "Exception while retrieving cached information: " + ExceptionUtils.getMessage(e), e); // can't happen
                return AssertionStatus.FAILED;
            }
            // TODO use proper hybrid stash manager, making arrangements to have it closed when context is closed,
            // instead of just using two-arg initialize() which just uses ByteArrayStashManager
            message.initialize(contentType, bodyBytes);
            return AssertionStatus.NONE;
        } catch (IOException e) {
            logger.log(Level.WARNING, "Message cache error: " + ExceptionUtils.getMessage(e), e);
            return AssertionStatus.FAILED;
        }
    }
}
