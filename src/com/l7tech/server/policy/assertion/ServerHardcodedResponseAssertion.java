/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy.assertion;

import com.l7tech.common.audit.Auditor;
import com.l7tech.common.audit.Messages;
import com.l7tech.common.message.HttpResponseKnob;
import com.l7tech.common.message.Message;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.StashManager;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.HardcodedResponseAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import org.springframework.context.ApplicationContext;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * The Server side Hardcoded Response.
 */
public class ServerHardcodedResponseAssertion implements ServerAssertion {
    private final Logger logger = Logger.getLogger(getClass().getName());
    private final Auditor auditor;

    private final String message;
    private final int status;
    private final ContentTypeHeader contentType;

    public ServerHardcodedResponseAssertion(HardcodedResponseAssertion ass, ApplicationContext springContext) {
        auditor = new Auditor(this, springContext, logger);
        ContentTypeHeader ctype;
        try {
            ctype = ContentTypeHeader.parseValue(ass.getResponseContentType());
        } catch (IOException e) {
            contentType = null;
            message = null;
            status = 500;
            return;
        }

        contentType = ctype;
        message = ass.getResponseBody();
        status = ass.getResponseStatus();
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context)
      throws IOException, PolicyAssertionException
    {
        // Create a real stash manager, rather than making a RAM-only one, in case later assertions replace the
        // response with one that is huge (and hence needs the real hybrid stashing strategy).
        StashManager stashManager = StashManagerFactory.createStashManager(); // TODO use Spring for this instead

        Message response = context.getResponse();
        response.close();
        try {
            response.initialize(stashManager, contentType, new ByteArrayInputStream(message.getBytes(contentType.getEncoding())));
        } catch (NoSuchPartException e) {
            auditor.logAndAudit(Messages.EXCEPTION_WARNING_WITH_MORE_INFO,
                    new String[] {"Unable to produce hardcoded response"},
                    e);
            return AssertionStatus.FAILED;
        }

        HttpResponseKnob hrk = (HttpResponseKnob) response.getKnob(HttpResponseKnob.class);
        if (hrk != null)
            hrk.setStatus(status);
        context.setRoutingStatus(RoutingStatus.ROUTED);
        return AssertionStatus.NONE;
    }
}
