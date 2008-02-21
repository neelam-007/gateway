package com.l7tech.external.assertions.cache.server;

import com.l7tech.common.message.Message;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.external.assertions.cache.CacheLookupAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.audit.LogOnlyAuditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
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
public class ServerCacheLookupAssertion extends AbstractServerAssertion<CacheLookupAssertion> {
    private static final Logger logger = Logger.getLogger(ServerCacheLookupAssertion.class.getName());

    private final Auditor auditor;
    private final String[] variablesUsed;
    private final SsgCacheManager cacheManager;
    private final StashManagerFactory stashManagerFactory;

    public ServerCacheLookupAssertion(CacheLookupAssertion assertion, BeanFactory beanFactory) throws PolicyAssertionException {
        super(assertion);

        this.auditor = beanFactory instanceof ApplicationContext
                       ? new Auditor(this, (ApplicationContext)beanFactory, logger) 
                       : new LogOnlyAuditor(logger);
        this.variablesUsed = assertion.getVariablesUsed();
        this.cacheManager = SsgCacheManager.getInstance(beanFactory);
        this.stashManagerFactory = (StashManagerFactory)beanFactory.getBean("stashManagerFactory", StashManagerFactory.class);
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        Map<String,Object> vars = context.getVariableMap(variablesUsed, auditor);
        final String cacheName = ExpandVariables.process(assertion.getCacheId(), vars, auditor, true);
        final String key = ExpandVariables.process(assertion.getCacheEntryKey(), vars, auditor, true);
        SsgCache cache = cacheManager.getCache(cacheName);

        InputStream body = cache.lookup(key);
        if (body == null)
            return AssertionStatus.FALSIFIED;

        try {
            String varname = assertion.getTargetVariableName();
            Message message;
            ContentTypeHeader ctype;
            if (varname == null) {
                message = assertion.isUseRequest() ? context.getRequest() : context.getResponse();
                ctype = message.getMimeKnob().getOuterContentType(); // TODO use cached mime type
            } else {
                message = new Message();
                ctype = context.getRequest().getMimeKnob().getOuterContentType(); // TODO use cached mime type
            }

            // TODO use proper hybrid stash manager, making arrangements to have it closed when context is closed,
            // instead of just using two-arg initialize() which just uses ByteArrayStashManager
            if (ctype == null) ctype = ContentTypeHeader.OCTET_STREAM_DEFAULT;
            message.initialize(ctype, HexUtils.slurpStream(body));

            return AssertionStatus.NONE;
        } finally {
            body.close();
        }
    }

    /*
     * Called reflectively by module class loader when module is unloaded, to ask us to clean up any globals
     * that would otherwise keep our instances from getting collected.
     */
    public static void onModuleUnloaded() {
        // This assertion doesn't have anything to do in response to this, but it implements this anyway
        // since it will be used as an example by future modular assertion authors
        logger.log(Level.INFO, "ServerCacheLookupAssertion is preparing itself to be unloaded");
    }
}
