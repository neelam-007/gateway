package com.l7tech.external.assertions.remotecacheassertion.server;

import com.l7tech.external.assertions.remotecacheassertion.RemoteCacheRemoveAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.LoggingAudit;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server side implementation of the RemoteCacheRemoveAssertion.
 *
 * @see com.l7tech.external.assertions.remotecacheassertion.RemoteCacheRemoveAssertion
 */
public class ServerRemoteCacheRemoveAssertion extends AbstractServerAssertion<RemoteCacheRemoveAssertion> {
    private static final Logger logger = Logger.getLogger(ServerRemoteCacheRemoveAssertion.class.getName());

    private final String[] variablesUsed;

    public ServerRemoteCacheRemoveAssertion(RemoteCacheRemoveAssertion assertion, ApplicationContext context) throws PolicyAssertionException {
        super(assertion);

        this.variablesUsed = assertion.getVariablesUsed();
    }

    /**
     * Check Request
     *
     * @param context the PolicyEnforcementContext.  Never null.
     * @return AssertionStatus.NONE if the entry is removed from cache
     *         AssertionStatus.FAILED if an error occurs
     * @throws IOException
     * @throws PolicyAssertionException
     */
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        final LoggingAudit audit = new LoggingAudit(logger);

        try {
            Map<String, Object> vars = context.getVariableMap(variablesUsed, audit);
            final String key = ExpandVariables.process(assertion.getCacheEntryKey(), vars, audit, true);

            RemoteCache remoteCache = RemoteCachesManagerImpl.getInstance().getRemoteCache(assertion.getRemoteCacheGoid());
            remoteCache.remove(key);
        } catch (Throwable t) {
            getAudit().logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Unable to remove cached entry: " + t.getMessage());
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
        logger.log(Level.INFO, "ServerRemoteCacheRemoveAssertion is preparing itself to be unloaded");
    }
}
