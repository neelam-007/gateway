/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy.assertion;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.server.audit.Auditor;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.BufferPool;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.Regex;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.policy.variable.ExpandVariables;
import com.l7tech.server.message.PolicyEnforcementContext;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Server side Regex Assertion
 */
public class ServerRegex extends AbstractServerAssertion<Regex> implements ServerAssertion {
    private final Logger logger = Logger.getLogger(getClass().getName());
    private final Auditor auditor;

    private Pattern regexPattern;
    private Exception compileException;
    private Regex regexAssertion;
    private final String[] varNames;
    public static final String ENCODING = "UTF-8";

    public ServerRegex(Regex ass, ApplicationContext springContext) {
        super(ass);
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
        varNames = ass.getVariablesUsed();
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
                                    regexAssertion.getRegex(), "unknown error");
            }
            return AssertionStatus.FALSIFIED;
        }

        int whichPart = regexAssertion.getMimePart();
        InputStream is = null;
        byte[] returnBuffer = null;
        try {

            PartInfo messagePart = isPostRouting(context) ?
              context.getResponse().getMimeKnob().getPart(whichPart) :
              context.getRequest().getMimeKnob().getPart(whichPart);

            String replacement = regexAssertion.getReplacement();
            final boolean isReplacement = regexAssertion.isReplace();

            // replacement set and replacement null
            if (isReplacement && replacement == null) {
                auditor.logAndAudit(AssertionMessages.REGEX_NO_REPLACEMENT);
                throw new PolicyAssertionException(regexAssertion, AssertionMessages.REGEX_NO_REPLACEMENT.getMessage());
            }

            byte[] messageBytes = messagePart.getBytesIfAlreadyAvailable();
            final int messageBytesLen;
            if (messageBytes != null) {
                messageBytesLen = messageBytes.length;
            } else {
                // Need to slurp it up
                long messageLen = messagePart.getActualContentLength();
                if (messageLen > Integer.MAX_VALUE) {
                    // Note--this is just to keep the array index from overflowing; actual limit is enforced elsewhere
                    auditor.logAndAudit(AssertionMessages.REGEX_TOO_BIG);
                    return AssertionStatus.FAILED;
                }
                messageBytes = returnBuffer = BufferPool.getBuffer((int)messageLen);
                is = messagePart.getInputStream(false);
                messageBytesLen = HexUtils.slurpStream(is, messageBytes);
                assert messageBytesLen == messageLen;
            }

            String encoding = regexAssertion.getEncoding();
            if (encoding != null && encoding.length() > 0) {
                auditor.logAndAudit(AssertionMessages.REGEX_ENCODING_OVERRIDE, encoding);
            } else if (encoding == null || encoding.length() == 0) {
                encoding = messagePart.getContentType().getEncoding();
            }

            if (encoding == null) {
                auditor.logAndAudit(AssertionMessages.REGEX_NO_ENCODING, ENCODING);
                encoding = ENCODING;
            }

            Matcher matcher = regexPattern.matcher(new String(messageBytes, 0, messageBytesLen, encoding));
            AssertionStatus assertionStatus = AssertionStatus.FAILED;
            if (isReplacement) {
                logger.log(Level.FINE, "Replace requested: Match pattern ''{0}'', replace pattern ''{1}''", new Object[]{regexAssertion.getRegex(), replacement});
                replacement = ExpandVariables.process(replacement, context.getVariableMap(varNames, auditor));
                String result = matcher.replaceAll(replacement);
                messagePart.setBodyBytes(result.getBytes(encoding));
                assertionStatus = AssertionStatus.NONE;
            } else {
                final boolean matched = matcher.find();
                if (matched && regexAssertion.isProceedIfPatternMatches()) {
                    logger.fine("Proceeding : Matched " + regexAssertion.getRegex());
                    assertionStatus = AssertionStatus.NONE;
                } else if (!matched && regexAssertion.isProceedIfPatternMatches()) {
                    auditor.logAndAudit(AssertionMessages.REGEX_NO_MATCH_FAILURE, regexAssertion.getRegex());
                    assertionStatus = AssertionStatus.FAILED;
                } else if (!matched && !regexAssertion.isProceedIfPatternMatches()) {
                    logger.fine("Proceeding : Not matched and proceed if no match requested " + regexAssertion.getRegex());
                    assertionStatus = AssertionStatus.NONE;
                } else if (matched && !regexAssertion.isProceedIfPatternMatches()) {
                    auditor.logAndAudit(AssertionMessages.REGEX_MATCH_FAILURE, regexAssertion.getRegex());
                    assertionStatus = AssertionStatus.FAILED;
                }
            }
            return assertionStatus;
        } catch (NoSuchPartException e) {
            auditor.logAndAudit(AssertionMessages.REGEX_NO_SUCH_PART, Integer.toString(whichPart));
            return AssertionStatus.FAILED;
        } finally {
            if (returnBuffer != null) BufferPool.returnBuffer(returnBuffer);
            if (is != null) try { is.close(); } catch (IOException e) { /* IGNORE */ }
        }
    }

    /**
     * Check if this regex would consider the specified policy enforcment context to be post-routing.
     *
     * @param context  the context to check.  Must not be null.
     * @return true iff. this ServerRegex would consider the specified context to be post-routing.
     */
    public static boolean isPostRouting(PolicyEnforcementContext context) {
        return RoutingStatus.ROUTED.equals(context.getRoutingStatus()) || RoutingStatus.ATTEMPTED.equals(context.getRoutingStatus());
    }

}
