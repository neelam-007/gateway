package com.l7tech.external.assertions.remotecacheassertion.server;

import com.l7tech.external.assertions.remotecacheassertion.RemoteCacheStoreAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.LoggingAudit;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.ExceptionUtils;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server side implementation of the RemoteCacheStoreAssertion.
 *
 * @see com.l7tech.external.assertions.remotecacheassertion.RemoteCacheStoreAssertion
 */
public class ServerRemoteCacheStoreAssertion extends AbstractMessageTargetableServerAssertion<RemoteCacheStoreAssertion> {
    private static final Logger logger = Logger.getLogger(ServerRemoteCacheStoreAssertion.class.getName());

    private final String[] variablesUsed;
    private final StashManagerFactory stashManagerFactory;

    private static final String CLUSTER_PROP_NAME = "remote.cache.servers";

    public ServerRemoteCacheStoreAssertion(RemoteCacheStoreAssertion assertion, ApplicationContext context) throws PolicyAssertionException {
        super(assertion);

        this.variablesUsed = assertion.getVariablesUsed();
        this.stashManagerFactory = context.getBean("stashManagerFactory", StashManagerFactory.class);
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        final LoggingAudit audit = new LoggingAudit(logger);

        try {
            InputStream messageBody = null;
            try {
                Message messageToCache = context.getTargetMessage(assertion, true);
                Map<String, Object> vars = context.getVariableMap(variablesUsed, audit);
                final String key = ExpandVariables.process(assertion.getCacheEntryKey(), vars, audit, true);
                final String expiryAge = ExpandVariables.process(assertion.getMaxEntryAge(), vars, audit, true);
                final String maxEntryByteSize = ExpandVariables.process(assertion.getMaxEntrySizeBytes(), vars, audit, true);
                try {
                    final int expiryAgeInt = Integer.parseInt(expiryAge);
                    final long maxEntryByteSizeInt = Long.parseLong(maxEntryByteSize);
                    final String valueType = assertion.getValueType();
                    if (assertion.isStoreSoapFaults() || !isSoapFault(messageToCache)) {
                        CachedMessageData cacheData = new CachedMessageData(messageToCache, stashManagerFactory, valueType);
                        if (maxEntryByteSizeInt > 0 && cacheData.sizeInBytes(CachedMessageData.ValueType.valueOf(valueType)) > maxEntryByteSizeInt) {
                            audit.logAndAudit(AssertionMessages.EXCEPTION_INFO_WITH_MORE_INFO,
                                    new String[]{"Size of the cache object exceeds the configured max size"});
                            return AssertionStatus.FAILED;
                        }
                        RemoteCache remoteCache = RemoteCachesManagerImpl.getInstance().getRemoteCache(assertion.getRemoteCacheGoid());
                        remoteCache.set(key, cacheData, expiryAgeInt);
                    }
                } catch (NumberFormatException e) {
                    logger.log(Level.FINER, "Cache Expiry Age or Max Cache Entry Size is not a valid number format: " + e.getMessage(), e);
                    return AssertionStatus.FAILED;
                }
            } finally {
                if (messageBody != null) {
                    messageBody.close();
                }
            }
        } catch (Exception e) {
            getAudit().logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                    new String[]{"Unable to store cached value: " + ExceptionUtils.getMessage(e)});
            return AssertionStatus.FAILED;
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

    /*
     * Called reflectively by module class loader when module is unloaded, to ask us to clean up any globals
     * that would otherwise keep our instances from getting collected.
     */
    public static void onModuleUnloaded() {
        // This assertion doesn't have anything to do in response to this, but it implements this anyway
        // since it will be used as an example by future modular assertion authors
        logger.log(Level.INFO, "ServerRemoteCacheStoreAssertion is preparing itself to be unloaded");
    }

    private static class MessageToBigException extends Exception {
    }
}
