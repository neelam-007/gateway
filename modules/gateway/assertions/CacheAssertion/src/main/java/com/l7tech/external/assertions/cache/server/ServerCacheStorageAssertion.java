package com.l7tech.external.assertions.cache.server;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.message.Message;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.external.assertions.cache.CacheStorageAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.audit.LogOnlyAuditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.gateway.common.audit.AssertionMessages;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server side implementation of the CacheStorageAssertion.
 *
 * @see com.l7tech.external.assertions.cache.CacheStorageAssertion
 */
public class ServerCacheStorageAssertion extends AbstractMessageTargetableServerAssertion<CacheStorageAssertion> {
    private static final Logger logger = Logger.getLogger(ServerCacheStorageAssertion.class.getName());

    private final Auditor auditor;
    private final String[] variablesUsed;
    private final SsgCacheManager cacheManager;

    public ServerCacheStorageAssertion(CacheStorageAssertion assertion, BeanFactory beanFactory) throws PolicyAssertionException {
        super(assertion, assertion);

        this.auditor = beanFactory instanceof ApplicationContext
                       ? new Auditor(this, (ApplicationContext)beanFactory, logger)
                       : new LogOnlyAuditor(logger);
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
                Map<String, Object> vars = context.getVariableMap(variablesUsed, auditor);
                final String cacheName = ExpandVariables.process(assertion.getCacheId(), vars, auditor, true);
                final String key = ExpandVariables.process(assertion.getCacheEntryKey(), vars, auditor, true);

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
            auditor.logAndAudit(AssertionMessages.EXCEPTION_INFO_WITH_MORE_INFO,
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

    @Override
    protected Audit getAuditor() {
        return auditor;
    }

    /*
     * Called reflectively by module class loader when module is unloaded, to ask us to clean up any globals
     * that would otherwise keep our instances from getting collected.
     */
    @SuppressWarnings({"UnusedDeclaration"})
    public static void onModuleUnloaded() {
        // This assertion doesn't have anything to do in response to this, but it implements this anyway
        // since it will be used as an example by future modular assertion authors
        logger.log(Level.INFO, "ServerCacheStorageAssertion is preparing itself to be unloaded");
    }
}