/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.server.policy.assertion;

import com.l7tech.policy.assertion.*;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.util.HexUtils;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.springframework.context.ApplicationContext;

/**
 * The Server side Regex Assertion
 */
public class ServerRegex implements ServerAssertion {
    private final Logger logger = Logger.getLogger(getClass().getName());
    private final Auditor auditor;

    private Pattern regexPattern;
    private Exception compileException;
    private Regex regexAssertion;


    public ServerRegex(Regex ass, ApplicationContext springContext) {
        regexAssertion = ass;
        auditor = new Auditor(this, springContext, logger);
        String regExExpression = ass.getRegex();
        try {
            regexPattern = Pattern.compile(regExExpression);
        } catch (Exception e) {
            compileException = e;
        }
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context)
      throws IOException, PolicyAssertionException {

        if (regexPattern == null) {
            if (compileException != null) {
                auditor.logAndAudit(AssertionMessages.REGEX_PATTERN_INVALID,
                                    new String[]{regexAssertion.getRegex(),
                                        compileException.getMessage()}, compileException);
            } else {
                auditor.logAndAudit(AssertionMessages.REGEX_PATTERN_INVALID,
                                    new String[]{regexAssertion.getRegex(), "unknown error"});
            }
            return AssertionStatus.FALSIFIED;
        }


        try {
            PartInfo firstPart = isPostRouting(context) ?
                                 context.getResponse().getMimeKnob().getFirstPart() :
                                 context.getRequest().getMimeKnob().getFirstPart();
            byte[] message = HexUtils.slurpStream(firstPart.getInputStream(true));
            final String encoding = firstPart.getContentType().getEncoding();
            String result = regexPattern.matcher(new String(message, encoding)).replaceAll(regexAssertion.getReplacement());
            firstPart.setBodyBytes(result.getBytes(encoding));
        } catch (NoSuchPartException e) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE, null, e);
            return AssertionStatus.FAILED;
        }
        return AssertionStatus.NONE;
    }


    private boolean isPostRouting(PolicyEnforcementContext context) {
        return RoutingStatus.ROUTED.equals(context.getRoutingStatus()) || RoutingStatus.ATTEMPTED.equals(context.getRoutingStatus());
    }

}
