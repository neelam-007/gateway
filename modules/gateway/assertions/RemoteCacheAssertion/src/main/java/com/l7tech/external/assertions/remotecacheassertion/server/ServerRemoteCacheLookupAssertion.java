package com.l7tech.external.assertions.remotecacheassertion.server;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.external.assertions.remotecacheassertion.RemoteCacheLookupAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.LoggingAudit;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server side implementation of the RemoteCacheLookupAssertion.
 *
 * @see com.l7tech.external.assertions.remotecacheassertion.RemoteCacheLookupAssertion
 */
public class ServerRemoteCacheLookupAssertion extends AbstractMessageTargetableServerAssertion<RemoteCacheLookupAssertion> {
    private static final Logger logger = Logger.getLogger(ServerRemoteCacheLookupAssertion.class.getName());

    private final String[] variablesUsed;

    private static final String CLUSTER_PROP_NAME = "remote.cache.servers";

    public ServerRemoteCacheLookupAssertion(RemoteCacheLookupAssertion assertion, ApplicationContext context) throws PolicyAssertionException {
        super(assertion);

        this.variablesUsed = assertion.getVariablesUsed();
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        final LoggingAudit audit = new LoggingAudit(logger);

        try {
            Map<String, Object> vars = context.getVariableMap(variablesUsed, audit);
            final String key = ExpandVariables.process(assertion.getCacheEntryKey(), vars, audit, true);

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

            RemoteCache remoteCache = RemoteCachesManagerImpl.getInstance().getRemoteCache(assertion.getRemoteCacheGoid());
            CachedMessageData cachedData = remoteCache.get(key);
            message.initialize(ContentTypeHeader.create(cachedData.getContentType()), cachedData.getBodyBytes());
        } catch(TimeoutException te) {
            getAudit().logAndAudit(AssertionMessages.EXCEPTION_INFO_WITH_MORE_INFO,
                new String[]{"Timeout while retrieving the value from remote cache."});
            return AssertionStatus.FAILED;
        } catch (Throwable t) {
            getAudit().logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Unable to lookup cached value: " + t.getMessage());
            return AssertionStatus.FAILED;
        }
        return AssertionStatus.NONE;
    }

    /*
     * Called reflectively by module class loader when module is unloaded, to ask us to clean up any globals
     * that would otherwise keep our instances from getting collected.
     */
    public static void onModuleUnloaded() {
        // This assertion doesn't have anything to do in response to this, but it implements this anyway
        // since it will be used as an example by future modular assertion authors
        logger.log(Level.INFO, "ServerRemoteCacheLookupAssertion is preparing itself to be unloaded");
    }

    private static class MessageToBigException extends Exception {
    }
}
