package com.l7tech.external.assertions.cache.server;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.message.Message;
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
import java.util.logging.Level;

/**
 * Server side implementation of the CacheStorageAssertion.
 *
 * @see com.l7tech.external.assertions.cache.CacheStorageAssertion
 */
public class ServerCacheStorageAssertion extends AbstractMessageTargetableServerAssertion<CacheStorageAssertion> {
    private final String[] variablesUsed;
    private final SsgCacheManager cacheManager;

    public ServerCacheStorageAssertion(CacheStorageAssertion assertion, BeanFactory beanFactory) throws PolicyAssertionException {
        super(assertion, assertion);
        this.variablesUsed = assertion.getVariablesUsed();
        this.cacheManager = SsgCacheManager.getInstance(beanFactory);
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        try {
            InputStream messageBody = null;
            try {
                Message messageToCache = context.getTargetMessage(assertion, true);
                ContentTypeHeader contentType = messageToCache.getMimeKnob().getOuterContentType();
                messageBody = messageToCache.getMimeKnob().getEntireMessageBodyAsInputStream();
                Map<String, Object> vars = context.getVariableMap(variablesUsed, getAudit());
                final String cacheName = ExpandVariables.process(assertion.getCacheId(), vars, getAudit(), true);
                final String key = ExpandVariables.process(assertion.getCacheEntryKey(), vars, getAudit(), true);

                SsgCache.Config cacheConfig = new SsgCache.Config(cacheName)
                    .maxEntries(assertion.getMaxEntries())
                    .maxAgeMillis(assertion.getMaxEntryAgeMillis())
                    .maxSizeBytes(assertion.getMaxEntrySizeBytes());
                SsgCache cache = cacheManager.getCache(cacheConfig);
                if (assertion.isStoreSoapFaults() || ! isSoapFault(messageToCache)) {
                    cache.store(key, messageBody, contentType.getFullValue());
                    logger.log(Level.FINE, "Stored to cache: " + key);
                }
            } finally {
                if (messageBody != null) messageBody.close();
            }
        } catch (Exception e) {
            logAndAudit(AssertionMessages.EXCEPTION_INFO_WITH_MORE_INFO,
                new String[]{"Unable to store cached value: " + ExceptionUtils.getMessage(e)}, e);
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