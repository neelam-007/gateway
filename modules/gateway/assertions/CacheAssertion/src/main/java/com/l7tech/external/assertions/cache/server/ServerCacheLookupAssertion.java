package com.l7tech.external.assertions.cache.server;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.external.assertions.cache.CacheLookupAssertion;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.audit.LogOnlyAuditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.ExceptionUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server side implementation of the CacheLookupAssertion.
 *
 * @see com.l7tech.external.assertions.cache.CacheLookupAssertion
 */
public class ServerCacheLookupAssertion extends AbstractMessageTargetableServerAssertion<CacheLookupAssertion> {
    private static final Logger logger = Logger.getLogger(ServerCacheLookupAssertion.class.getName());

    private final Auditor auditor;
    private final String[] variablesUsed;
    private final SsgCacheManager cacheManager;

    public ServerCacheLookupAssertion(CacheLookupAssertion assertion, BeanFactory beanFactory) throws PolicyAssertionException {
        super(assertion, assertion);

        this.auditor = beanFactory instanceof ApplicationContext
                       ? new Auditor(this, (ApplicationContext)beanFactory, logger) 
                       : new LogOnlyAuditor(logger);
        this.variablesUsed = assertion.getVariablesUsed();
        this.cacheManager = SsgCacheManager.getInstance(beanFactory);
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        Map<String, Object> vars = context.getVariableMap(variablesUsed, auditor);
        final String cacheName = ExpandVariables.process(assertion.getCacheId(), vars, auditor, true);
        final String key = ExpandVariables.process(assertion.getCacheEntryKey(), vars, auditor, true);
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

            InputStream bodyInputStream = null;
            try {
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
            } finally {
                if (bodyInputStream != null) bodyInputStream.close();
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Message cache error: " + ExceptionUtils.getMessage(e), e);
            return AssertionStatus.FAILED;
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
        logger.log(Level.INFO, "ServerCacheLookupAssertion is preparing itself to be unloaded");
    }
}
