package com.l7tech.external.assertions.cache.server;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.message.Message;
import com.l7tech.message.MimeKnob;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;
import com.l7tech.external.assertions.cache.CacheStorageAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.audit.LogOnlyAuditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.gateway.common.audit.AssertionMessages;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;

import java.io.ByteArrayInputStream;
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
public class ServerCacheStorageAssertion extends AbstractServerAssertion<CacheStorageAssertion> {
    private static final Logger logger = Logger.getLogger(ServerCacheStorageAssertion.class.getName());

    private final Auditor auditor;
    private final String[] variablesUsed;
    private final SsgCacheManager cacheManager;

    public ServerCacheStorageAssertion(CacheStorageAssertion assertion, BeanFactory beanFactory) throws PolicyAssertionException {
        super(assertion);

        this.auditor = beanFactory instanceof ApplicationContext
                       ? new Auditor(this, (ApplicationContext)beanFactory, logger)
                       : new LogOnlyAuditor(logger);
        this.variablesUsed = assertion.getVariablesUsed();
        this.cacheManager = SsgCacheManager.getInstance(beanFactory);
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        final Pair<ContentTypeHeader, InputStream> source;
        try {
            source = getSource(context);
        } catch (Exception e) {
            auditor.logAndAudit( AssertionMessages.EXCEPTION_INFO_WITH_MORE_INFO,
                                new String[] { "Unable to store cached value: " + ExceptionUtils.getMessage(e) }, e);
            return AssertionStatus.FAILED;
        }

        try {
            Map<String,Object> vars = context.getVariableMap(variablesUsed, auditor);
            final String cacheName = ExpandVariables.process(assertion.getCacheId(), vars, auditor, true);
            final String key = ExpandVariables.process(assertion.getCacheEntryKey(), vars, auditor, true);

            SsgCache cache = cacheManager.getCache(cacheName);
            // TODO store mime type
            cache.store(key, source.right);

            return AssertionStatus.NONE;

        } catch (IllegalArgumentException e) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_INFO_WITH_MORE_INFO,
                                new String[] { "Unable to store cached value: " + ExceptionUtils.getMessage(e) }, e);
            return AssertionStatus.FAILED;
        } finally {
            source.right.close();
        }
    }

    private Pair<ContentTypeHeader, InputStream> getSource(PolicyEnforcementContext context) throws IOException, NoSuchVariableException, NoSuchPartException {
        final String varname = assertion.getSourceVariableName();
        if (varname == null) {
            final MimeKnob mk = assertion.isUseRequest()
                                ? context.getRequest().getMimeKnob()
                                : context.getResponse().getMimeKnob();
            return new Pair<ContentTypeHeader, InputStream>(mk.getOuterContentType(),
                                                            mk.getEntireMessageBodyAsInputStream());
        } else {
            Object val = context.getVariable(varname);
            if (val instanceof Message) {
                Message message = (Message)val;
                return new Pair<ContentTypeHeader, InputStream>(message.getMimeKnob().getOuterContentType(),
                                                                message.getMimeKnob().getEntireMessageBodyAsInputStream());
            } else if (val instanceof byte[]) {
                byte[] bytes = (byte[])val;
                return new Pair<ContentTypeHeader, InputStream>(ContentTypeHeader.OCTET_STREAM_DEFAULT,
                                                                new ByteArrayInputStream(bytes));
            } else {
                return new Pair<ContentTypeHeader, InputStream>(ContentTypeHeader.TEXT_DEFAULT,
                                                                new ByteArrayInputStream(val.toString().getBytes("UTF-8")));
            }
        }
    }

    /*
     * Called reflectively by module class loader when module is unloaded, to ask us to clean up any globals
     * that would otherwise keep our instances from getting collected.
     */
    public static void onModuleUnloaded() {
        // This assertion doesn't have anything to do in response to this, but it implements this anyway
        // since it will be used as an example by future modular assertion authors
        logger.log(Level.INFO, "ServerCacheStorageAssertion is preparing itself to be unloaded");
    }
}