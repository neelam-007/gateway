package com.l7tech.server.policy.assertion;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.Echo;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.common.mime.*;
import com.l7tech.common.message.Message;
import com.l7tech.common.message.MimeKnob;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.context.ApplicationContext;

/**
 * Test assertion that echoes the request into the response
 * @author emil
 * @version 21-Mar-2005
 */
public class ServerEcho extends ServerRoutingAssertion {
    public ServerEcho(Echo ea, ApplicationContext applicationContext) {
        super(applicationContext);
    }

    /**
     * SSG Server-side processing of the given request.
     *
     * @param context
     * @return AssertionStatus.NONE if this Assertion did its business successfully; otherwise, some error code
     * @throws com.l7tech.policy.assertion.PolicyAssertionException
     *          something is wrong in the policy dont throw this if there is an issue with the request or the response
     */
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        Message request = context.getRequest();
        MimeKnob mimeRequestKnob = request.getMimeKnob();

        try {
            InputStream responseStream = mimeRequestKnob.getEntireMessageBodyAsInputStream();
            ContentTypeHeader outerContentType = mimeRequestKnob.getOuterContentType();
            final StashManager stashManager = StashManagerFactory.createStashManager();
            context.getResponse().initialize(stashManager, outerContentType, responseStream);
        } catch (NoSuchPartException e) {
            throw (IOException) new IOException().initCause(e);
        }

        return AssertionStatus.NONE;
    }
}
