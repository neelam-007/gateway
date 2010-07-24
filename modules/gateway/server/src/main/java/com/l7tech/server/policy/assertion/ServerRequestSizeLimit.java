/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy.assertion;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RequestSizeLimit;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.variable.ExpandVariables;
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
    private final String limitString;

    public ServerRequestSizeLimit(RequestSizeLimit ass, ApplicationContext springContext) {
        super(ass);
        auditor = new Auditor(this, springContext, logger);
        this.entireMessage = ass.isEntireMessage();
        this.limitString = ass.getLimit();
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context)
      throws IOException, PolicyAssertionException
    {
        Message request = context.getRequest();

        long limit;
        try {
            limit = getLimit(context);
        } catch (NumberFormatException e) {
            auditor.logAndAudit(AssertionMessages.VARIABLE_INVALID_VALUE, limitString, "Long");
            return AssertionStatus.FAILED;
        }

        final long messlen;
        if (entireMessage) {
            try {
                request.getMimeKnob().setContentLengthLimit(limit);
                messlen = request.getMimeKnob().getContentLength();
            } catch(IOException e) {
                auditor.logAndAudit(AssertionMessages.REQUEST_BODY_TOO_LARGE);
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

    /**
     * Gets the request size limit in bytes.
     */
    private long getLimit(PolicyEnforcementContext context) throws NumberFormatException {
        final String[] referencedVars = Syntax.getReferencedNames(limitString);
        long longValue;
        if(referencedVars.length > 0){
            final String stringValue = ExpandVariables.process(limitString, context.getVariableMap(referencedVars, auditor), auditor);
            longValue = Long.parseLong(stringValue) * 1024;
        }else{
            longValue = Long.parseLong(limitString) * 1024;
        }

        return longValue;
    }
}
