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
import java.util.regex.Matcher;

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
            int flags = Pattern.DOTALL | Pattern.MULTILINE;
            if (regexAssertion.isCaseInsensitive()) {
                flags |= Pattern.CASE_INSENSITIVE;
            }
            regexPattern = Pattern.compile(regExExpression, flags);
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

            final String replacement = regexAssertion.getReplacement();
            final boolean haveReplacement = replacement != null;

            byte[] message = HexUtils.slurpStream(firstPart.getInputStream(haveReplacement));
            final String encoding = firstPart.getContentType().getEncoding();
            Matcher matcher = regexPattern.matcher(new String(message, encoding));
            if (haveReplacement) {
                String result = matcher.replaceAll(replacement);
                firstPart.setBodyBytes(result.getBytes(encoding));
            } else {
                logger.fine("No replace has been requested. Verifying match for pattern "+regexAssertion.getRegex());
                if (!matcher.find())  {
                    logger.fine("No match for "+regexAssertion.getRegex());
                    return AssertionStatus.FALSIFIED;
                }
            }
            return AssertionStatus.NONE;
        } catch (NoSuchPartException e) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE, null, e);
            return AssertionStatus.FAILED;
        }
    }


    private boolean isPostRouting(PolicyEnforcementContext context) {
        return RoutingStatus.ROUTED.equals(context.getRoutingStatus()) || RoutingStatus.ATTEMPTED.equals(context.getRoutingStatus());
    }

}
