/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy.assertion;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.message.Message;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RequestSizeLimit;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * The Server side Regex Assertion
 */
public class ServerRequestSizeLimit extends AbstractServerAssertion implements ServerAssertion {
    private final Logger logger = Logger.getLogger(getClass().getName());
    private final Auditor auditor;

    private final boolean entireMessage;
    private final long limit;

    public ServerRequestSizeLimit(RequestSizeLimit ass, ApplicationContext springContext) {
        super(ass);
        auditor = new Auditor(this, springContext, logger);
        this.entireMessage = ass.isEntireMessage();
        this.limit = ass.getLimit();
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context)
      throws IOException, PolicyAssertionException
    {
        Message request = context.getRequest();

        final long messlen;
        if (entireMessage) {
            try {
                request.getMimeKnob().setContentLengthLimit(limit);
                messlen = request.getMimeKnob().getContentLength();
            } catch(IOException e) {
                auditor.logAndAudit(AssertionMessages.REQUEST_BODY_TOO_LARGE, null);
                return AssertionStatus.FALSIFIED;
            }
            if (messlen > limit) {
                auditor.logAndAudit(AssertionMessages.REQUEST_BODY_TOO_LARGE);
                return AssertionStatus.FALSIFIED;
            }
            return AssertionStatus.NONE;
        } else {
            try {
                long xmlLen = request.getMimeKnob().getFirstPart().getActualContentLength();
                if (xmlLen > limit) {
                    auditor.logAndAudit(AssertionMessages.REQUEST_FIRST_PART_TOO_LARGE);
                    return AssertionStatus.FALSIFIED;
                }
                return AssertionStatus.NONE;
            } catch (NoSuchPartException e) {
                auditor.logAndAudit(AssertionMessages.EXCEPTION_INFO_WITH_MORE_INFO,
                                    new String[] {"The required attachment " + e.getWhatWasMissing() +
                                            "was not found in the request"}, e);
                return AssertionStatus.FALSIFIED;
            }
        }
    }
}
