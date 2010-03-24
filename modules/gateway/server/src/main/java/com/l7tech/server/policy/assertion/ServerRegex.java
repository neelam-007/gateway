/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy.assertion;

import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.Regex;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.audit.LogOnlyAuditor;
import com.l7tech.server.message.ContextVariableKnob;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.Charsets;
import com.l7tech.util.IOUtils;
import com.l7tech.util.Pair;
import com.l7tech.util.ResourceUtils;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Server side Regex Assertion
 */
public class ServerRegex extends AbstractServerAssertion<Regex> {
    private final Logger logger = Logger.getLogger(getClass().getName());
    private final Auditor auditor;

    private Pattern regexPattern;
    private Exception compileException;
    private final String[] varNames;
    public static final Charset ENCODING = Charsets.UTF8;
    private final boolean isReplacement;

    public ServerRegex(Regex ass, ApplicationContext springContext) {
        super(ass);
        auditor = springContext != null ? new Auditor(this, springContext, logger) : new LogOnlyAuditor(logger);
        String regExExpression = ass.getRegex();
        try {
            int flags = Pattern.DOTALL | Pattern.MULTILINE;
            if (assertion.isCaseInsensitive()) {
                flags |= Pattern.CASE_INSENSITIVE;
            }
            regexPattern = Pattern.compile(regExExpression, flags);
        } catch (Exception e) {
            compileException = e;
        }
        isReplacement = assertion.isReplace();
        varNames = ass.getVariablesUsed();
    }

    private static interface RegexInput {
        CharSequence getInput() throws IOException;
    }

    private static interface RegexOutput {
        void setOutput(CharSequence output) throws IOException;
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException
    {
        checkPattern();

        Pair<RegexInput, RegexOutput> inputAndOutput = getInputAndOutput(context);

        Matcher matcher = regexPattern.matcher(inputAndOutput.left.getInput());

        return isReplacement
                ? doReplace(context, matcher, inputAndOutput.right)
                : doMatch(context, matcher);
    }

    private Pair<RegexInput, RegexOutput> getInputAndOutput(PolicyEnforcementContext context) throws IOException {
        RegexInput input;
        RegexOutput output = null;
        Message target = getTarget(context);
        int whichPart = assertion.getMimePart();


        try {
            final PartInfo part = target.getMimeKnob().getPart(whichPart);
            final Charset encoding = getEncoding(part);

            ContextVariableKnob cvk = target.getKnob(ContextVariableKnob.class);
            if (cvk != null)
                cvk.setOverrideEncoding(encoding);

            input = makeInput(part, encoding);

            if (isReplacement) output = makeOutput(part, encoding);

        } catch (NoSuchPartException e) {
            auditor.logAndAudit(AssertionMessages.REGEX_NO_SUCH_PART, Integer.toString(whichPart));
            throw new AssertionStatusException(e);
        }

        return new Pair<RegexInput, RegexOutput>(input, output);
    }

    private RegexInput makeInput(final PartInfo part, final Charset encoding) {
        return new RegexInput() {

            @Override
            public CharSequence getInput() throws IOException {
                InputStream is = null;
                try {
                    close();
                    byte[] bytes = part.getBytesIfAlreadyAvailable();
                    if (bytes == null)
                        bytes = IOUtils.slurpStream(is = part.getInputStream(false));
                    return new String(bytes, encoding);

                } catch (NoSuchPartException e) {
                    throw new IOException(e);
                } finally {
                    ResourceUtils.closeQuietly(is);
                }
            }
        };
    }

    private RegexOutput makeOutput(final PartInfo part, final Charset encoding) {
        return new RegexOutput() {
            @Override
            public void setOutput(CharSequence output) throws IOException {
                part.setBodyBytes(output.toString().getBytes(encoding));
            }
        };
    }

    private Charset getEncoding(PartInfo part) {
        String encodingName = assertion.getEncoding();
        Charset encoding;
        if (encodingName == null || encodingName.length() < 1) {
            encoding = part.getContentType().getEncoding();
        } else {
            try {
                encoding = Charset.forName(encodingName);
                auditor.logAndAudit(AssertionMessages.REGEX_ENCODING_OVERRIDE, encodingName);
            } catch (UnsupportedCharsetException e) {
                encoding = null;
            }
        }

        if (encoding == null) {
            auditor.logAndAudit(AssertionMessages.REGEX_NO_ENCODING, ENCODING.name());
            encoding = ENCODING;
        }
        return encoding;
    }

    private Message getTarget(PolicyEnforcementContext context) {
        try {
            if (assertion.isAutoTarget()) {
                // Preserve old behavior for old policies that didn't specify a message target
                return context.isPostRouting()
                        ? context.getResponse()
                        : context.getRequest();
            }

            return context.getTargetMessage(assertion, true);
        } catch (NoSuchVariableException e) {
            auditor.logAndAudit(AssertionMessages.NO_SUCH_VARIABLE, e.getVariable());
            throw new AssertionStatusException(AssertionStatus.SERVER_ERROR);
        }
    }

    private AssertionStatus doMatch(PolicyEnforcementContext context, Matcher matcher) {
        final boolean matched = matcher.find();

        final String captureVar = assertion.getCaptureVar();
        if (matched && captureVar != null) {
            List<String> captured = new ArrayList<String>();
            captured.add(matcher.group(0));
            int groupCount = matcher.groupCount();
            for (int i = 1; i <= groupCount; ++i) { // note 1-based
                captured.add(matcher.group(i));
            }
            context.setVariable(captureVar, captured.toArray(new String[captured.size()]));
        }

        if (assertion.isProceedIfPatternMatches()) {
            if (matched) {
                if (logger.isLoggable(Level.FINE))
                    logger.fine("Proceeding : Matched " + assertion.getRegex());
                return AssertionStatus.NONE;
            } else {
                auditor.logAndAudit(AssertionMessages.REGEX_NO_MATCH_FAILURE, assertion.getRegex());
                return AssertionStatus.FALSIFIED;
            }
        } else { // !proceedIfPatternMatches
            if (matched) {
                auditor.logAndAudit(AssertionMessages.REGEX_MATCH_FAILURE, assertion.getRegex());
                return AssertionStatus.FALSIFIED;
            } else {
                if (logger.isLoggable(Level.FINE))
                    logger.fine("Proceeding : Not matched and proceed if no match requested " + assertion.getRegex());
                return AssertionStatus.NONE;
            }
        }
    }

    private AssertionStatus doReplace(PolicyEnforcementContext context, Matcher matcher, RegexOutput out) throws IOException {
        String replacement = assertion.getReplacement();
        if (logger.isLoggable(Level.FINE))
            logger.log(Level.FINE, "Replace requested: Match pattern ''{0}'', replace pattern ''{1}''",
                    new Object[] { assertion.getRegex(), replacement });
        replacement = ExpandVariables.process(replacement, context.getVariableMap(varNames, auditor), auditor);
        String result = matcher.replaceAll(replacement);
        out.setOutput(result);
        return AssertionStatus.NONE;
    }

    private void checkPattern() throws AssertionStatusException, PolicyAssertionException {
        if (regexPattern == null) {
            if (compileException != null) {
                auditor.logAndAudit(AssertionMessages.REGEX_PATTERN_INVALID,
                                    new String[]{assertion.getRegex(),
                                        compileException.getMessage()}, compileException);
            } else {
                auditor.logAndAudit(AssertionMessages.REGEX_PATTERN_INVALID,
                                    assertion.getRegex(), "unknown error");
            }
            throw new AssertionStatusException(AssertionStatus.SERVER_ERROR);
        }

        if (isReplacement && assertion.getReplacement() == null) {
            auditor.logAndAudit(AssertionMessages.REGEX_NO_REPLACEMENT);
            throw new PolicyAssertionException(assertion, AssertionMessages.REGEX_NO_REPLACEMENT.getMessage());
        }
    }
}
