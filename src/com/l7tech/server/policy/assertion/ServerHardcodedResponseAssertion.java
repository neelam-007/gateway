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
import com.l7tech.policy.variable.ExpandVariables;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import org.springframework.context.ApplicationContext;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * The Server side Hardcoded Response.
 */
public class ServerHardcodedResponseAssertion implements ServerAssertion {
    private final Logger logger = Logger.getLogger(getClass().getName());
    private final Auditor auditor;

    private final String message;
    private final int status;
    private final ContentTypeHeader contentType;
    private final String[] variablesUsed;

    public ServerHardcodedResponseAssertion(HardcodedResponseAssertion ass, ApplicationContext springContext) {
        auditor = new Auditor(this, springContext, logger);
        ContentTypeHeader ctype;
        variablesUsed = ass.getVariablesUsed();
        try {
            ctype = ContentTypeHeader.parseValue(ass.getResponseContentType());
        } catch (IOException e) {
            // fla bugfix, instead of breaking policy completly, log the problem (which was not done before)
            // as warning and fallback on a safe value
            logger.log(Level.WARNING, "Error parsing content type. Falling back on text/plain", e);
            try {
                contentType = ContentTypeHeader.parseValue("text/plain");
            } catch (IOException e1) {
                // can't happen
                throw new RuntimeException(e);
            }
            message = ass.responseBodyString();
            status = ass.getResponseStatus();
            return;
        }
        contentType = ctype;
        message = ass.responseBodyString();
        status = ass.getResponseStatus();
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context)
      throws IOException, PolicyAssertionException
    {
        // Create a real stash manager, rather than making a RAM-only one, in case later assertions replace the
        // response with one that is huge (and hence needs the real hybrid stashing strategy).
        StashManager stashManager = StashManagerFactory.createStashManager(); // TODO use Spring for this instead

        Message response = context.getResponse();
        // fla bugfix attach the status before closing otherwise, it's lost
        HttpResponseKnob hrk = null;
        try {
            hrk = response.getHttpResponseKnob();
        } catch (IllegalStateException e) {
            hrk = null;
            logger.warning("there is no httpresponseknob to attach the status code to");
        }
        if (hrk != null) {
            logger.fine("setting status " + status + " to existing httpresponseknob");
            hrk.setStatus(status);
        }
        response.close();
        try {
            String msg = message;
            if (variablesUsed.length > 0) {
                msg = ExpandVariables.process(msg, context.getVariableMap(variablesUsed, auditor));
            }
            response.initialize(stashManager, contentType, new ByteArrayInputStream(msg.getBytes(contentType.getEncoding())));
        } catch (NoSuchPartException e) {
            auditor.logAndAudit(Messages.EXCEPTION_WARNING_WITH_MORE_INFO,
                    new String[] {"Unable to produce hardcoded response"},
                    e);
            return AssertionStatus.FAILED;
        }
        context.setRoutingStatus(RoutingStatus.ROUTED);
        return AssertionStatus.NONE;
    }
}