/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy.assertion;

import com.l7tech.common.audit.Auditor;
import com.l7tech.common.audit.Messages;
import com.l7tech.common.http.HttpCookie;
import com.l7tech.common.message.AbstractHttpResponseKnob;
import com.l7tech.common.message.HttpResponseKnob;
import com.l7tech.common.message.Message;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.StashManager;
import com.l7tech.common.util.ExceptionUtils;
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
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The Server side Hardcoded Response.
 */
public class ServerHardcodedResponseAssertion extends AbstractServerAssertion implements ServerAssertion {
    private final Logger logger = Logger.getLogger(getClass().getName());
    private final Auditor auditor;

    private final String message;
    private final byte[] messageBytesNoVar;
    private final int status;
    private final ContentTypeHeader contentType;
    private final String[] variablesUsed;

    public ServerHardcodedResponseAssertion(HardcodedResponseAssertion ass, ApplicationContext springContext)
            throws PolicyAssertionException
    {
        super(ass);
        auditor = new Auditor(this, springContext, logger);

        ContentTypeHeader ctype;
        try {
            ctype = ContentTypeHeader.parseValue(ass.getResponseContentType());
        } catch (IOException e) {
            // fla bugfix, instead of breaking policy completly, log the problem (which was not done before)
            // as warning and fallback on a safe value
            logger.log(Level.WARNING, "Error parsing content type. Falling back on text/plain", e);
            try {
                ctype = ContentTypeHeader.parseValue("text/plain");
            } catch (IOException e1) {
                // can't happen
                throw new RuntimeException(e);
            }
        }
        this.contentType = ctype;

        variablesUsed = ass.getVariablesUsed();
        this.message = ass.responseBodyString();
        this.status = ass.getResponseStatus();
        try {
            messageBytesNoVar = this.message.getBytes(contentType.getEncoding());
        } catch (UnsupportedEncodingException e) {
            throw new PolicyAssertionException(assertion, "Invalid encoding for hardcoded response: " + ExceptionUtils.getMessage(e), e);
        }
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context)
      throws IOException, PolicyAssertionException
    {
        // Create a real stash manager, rather than making a RAM-only one, in case later assertions replace the
        // response with one that is huge (and hence needs the real hybrid stashing strategy).
        StashManager stashManager = StashManagerFactory.createStashManager(); // TODO use Spring for this instead

        Message response = context.getResponse();
        // fla bugfix attach the status before closing otherwise, it's lost
        HttpResponseKnob hrk = (HttpResponseKnob)response.getKnob(HttpResponseKnob.class);
        if (hrk == null) {
            hrk = new AbstractHttpResponseKnob() {
                public void addCookie(HttpCookie cookie) {
                    // This was probably not an HTTP request, so cookies are meaningless anyway.
                }
            };
        }

        hrk.setStatus(status);
        response.close();
        try {
            final byte[] bytes;
            if (variablesUsed.length > 0) {
                String msg = message;
                msg = ExpandVariables.process(msg, context.getVariableMap(variablesUsed, auditor));
                bytes = msg.getBytes(contentType.getEncoding());
            } else {
                bytes = this.messageBytesNoVar;
            }
            response.initialize(stashManager, contentType, new ByteArrayInputStream(bytes));
            response.attachHttpResponseKnob(hrk);
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
