/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.server.policy.assertion;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.ExpandVariables;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.util.HexUtils;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.Level;
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
    private final ExpandVariables expandVariables = new ExpandVariables();

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

            String replacement = regexAssertion.getReplacement();
            final boolean isReplacement = regexAssertion.isReplace();

            // replacement set and replacement null
            if (isReplacement && replacement == null) {
                final String msg = "Replace requested, and no replace string specified (null).";
                logger.log(Level.FINE, msg);
                throw new PolicyAssertionException(msg);
            }

            byte[] message = HexUtils.slurpStream(firstPart.getInputStream(isReplacement));
            final String encoding = firstPart.getContentType().getEncoding();
            Matcher matcher = regexPattern.matcher(new String(message, encoding));
            if (isReplacement) {
                logger.log(Level.FINE, "Replace requested: Match pattern '{0}', replace pattern '{1}'", new Object[]{regexAssertion.getRegex(), replacement});
                replacement = expandVariables.process(replacement, context.getVariables());
                String result = matcher.replaceAll(replacement);
                firstPart.setBodyBytes(result.getBytes(encoding));
            } else {
                logger.fine("Verifying match for pattern " + regexAssertion.getRegex());
                if (!matcher.find()) {
                    logger.fine("No match for " + regexAssertion.getRegex());
                    return AssertionStatus.FALSIFIED;
                }
            }
            return AssertionStatus.NONE;
        } catch (NoSuchPartException e) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE, null, e);
            return AssertionStatus.FAILED;
        } catch (ExpandVariables.VariableNotFoundException e) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE, null, e);
            return AssertionStatus.FAILED;
        }
    }


    private boolean isPostRouting(PolicyEnforcementContext context) {
        return RoutingStatus.ROUTED.equals(context.getRoutingStatus()) || RoutingStatus.ATTEMPTED.equals(context.getRoutingStatus());
    }

}
