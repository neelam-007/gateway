/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy.assertion;

import com.l7tech.common.audit.Auditor;
import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.common.util.HexUtils;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.Regex;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.policy.ExpandVariables;
import com.l7tech.server.message.PolicyEnforcementContext;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

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
    public static final String ENCODING = "UTF-8";

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

        int whichPart = regexAssertion.getMimePart();
        try {

            PartInfo messagePart = isPostRouting(context) ?
              context.getResponse().getMimeKnob().getPart(whichPart) :
              context.getRequest().getMimeKnob().getPart(whichPart);

            String replacement = regexAssertion.getReplacement();
            final boolean isReplacement = regexAssertion.isReplace();

            // replacement set and replacement null
            if (isReplacement && replacement == null) {
                auditor.logAndAudit(AssertionMessages.REGEX_NO_REPLACEMENT);
                throw new PolicyAssertionException(AssertionMessages.REGEX_NO_REPLACEMENT.getMessage());
            }

            InputStream is = messagePart.getInputStream(false);
            byte[] messageBytes = HexUtils.slurpStream(is, Regex.MAX_LENGTH);
            if (messageBytes.length == Regex.MAX_LENGTH) {
                auditor.logAndAudit(AssertionMessages.REGEX_TOO_BIG);
                return AssertionStatus.FAILED;
            }

            String encoding = regexAssertion.getEncoding();
            if (encoding != null && encoding.length() > 0) {
                auditor.logAndAudit(AssertionMessages.REGEX_ENCODING_OVERRIDE, new String[] { encoding });
            } else if (encoding == null || encoding.length() == 0) {
                encoding = messagePart.getContentType().getEncoding();
            }

            if (encoding == null) {
                auditor.logAndAudit(AssertionMessages.REGEX_NO_ENCODING, new String[] { ENCODING });
                encoding = ENCODING;
            }

            Matcher matcher = regexPattern.matcher(new String(messageBytes, encoding));
            AssertionStatus assertionStatus = AssertionStatus.FAILED;
            if (isReplacement) {
                logger.log(Level.FINE, "Replace requested: Match pattern '{0}', replace pattern '{1}'", new Object[]{regexAssertion.getRegex(), replacement});
                replacement = expandVariables.process(replacement, context.getVariables());
                String result = matcher.replaceAll(replacement);
                messagePart.setBodyBytes(result.getBytes(encoding));
                assertionStatus = AssertionStatus.NONE;
            } else {
                final boolean matched = matcher.find();
                if (matched && regexAssertion.isProceedIfPatternMatches()) {
                    logger.fine("Proceeding : Matched " + regexAssertion.getRegex());
                    assertionStatus = AssertionStatus.NONE;
                } else if (!matched && regexAssertion.isProceedIfPatternMatches()) {
                    logger.fine("Failing : Not matched " + regexAssertion.getRegex());
                    assertionStatus = AssertionStatus.FAILED;
                } else if (!matched && !regexAssertion.isProceedIfPatternMatches()) {
                    logger.fine("Proceeding : Not matched and proceed if no match requested " + regexAssertion.getRegex());
                    assertionStatus = AssertionStatus.NONE;
                } else if (matched && !regexAssertion.isProceedIfPatternMatches()) {
                    logger.fine("Failing : Matched and proceed if no match requested " + regexAssertion.getRegex());
                     assertionStatus = AssertionStatus.FAILED;
                }
            }
            return assertionStatus;
        } catch (NoSuchPartException e) {
            auditor.logAndAudit(AssertionMessages.REGEX_NO_SUCH_PART, new String[] { Integer.toString(whichPart) });
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
