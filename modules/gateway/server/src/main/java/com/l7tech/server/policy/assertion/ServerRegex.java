package com.l7tech.server.policy.assertion;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.Regex;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.message.ContextVariableBackedMessageUtils;
import com.l7tech.server.message.ContextVariableKnob;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.*;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * The Server side Regex Assertion
 */
public class ServerRegex extends AbstractServerAssertion<Regex> {
    private Exception compileException;
    private final Either<Pattern, String> regexPatternOrTemplate;
    private final String[] varNames;
    private final boolean isReplacement;
    private final int replaceRepeatCount;
    private final boolean includeEntireExpressionCapture;
    private final boolean findAll;
    private final String captureVar;
    private final boolean caseInsensitive;

    public ServerRegex(Regex ass) {
        super(ass);
        String regExExpression = ass.getRegex();
        caseInsensitive = assertion.isCaseInsensitive();
        isReplacement = assertion.isReplace();
        varNames = ass.getVariablesUsed();
        replaceRepeatCount = Math.abs(ass.getReplaceRepeatCount());
        includeEntireExpressionCapture = ass.isIncludeEntireExpressionCapture();
        captureVar = assertion.getCaptureVar();
        findAll = assertion.isFindAll();

        Either<Pattern, String> pattern;
        try {
            if (assertion.isPatternContainsVariables()) {
                pattern = Either.right(regExExpression);
            } else {
                pattern = Either.left(Pattern.compile(regExExpression, patternFlags()));
            }
        } catch (Exception e) {
            compileException = e;
            pattern = null;
        }
        this.regexPatternOrTemplate = pattern;
    }

    private int patternFlags() {
        int flags = Pattern.DOTALL | Pattern.MULTILINE;
        if (caseInsensitive) {
            flags |= Pattern.CASE_INSENSITIVE;
        }
        return flags;
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
        Pair<RegexInput, RegexOutput> inputAndOutput = getInputAndOutput(context);
        final Pattern pattern = getPattern(context);
        if(assertion.getRegexVar()!=null)
            context.setVariable(assertion.getRegexVar(),pattern.pattern());

        return isReplacement
                ? doReplace(context, pattern, inputAndOutput.left, inputAndOutput.right)
                : doMatch(context, pattern.matcher(inputAndOutput.left.getInput()));
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
            logAndAudit(AssertionMessages.REGEX_NO_SUCH_PART, Integer.toString(whichPart));
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
                logAndAudit(AssertionMessages.REGEX_ENCODING_OVERRIDE, encodingName);
            } catch (UnsupportedCharsetException e) {
                encoding = null;
            }
        }

        if (encoding == null) {
            logAndAudit(AssertionMessages.REGEX_NO_ENCODING, Charsets.UTF8.name());
            encoding = Charsets.UTF8;
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

            return TargetMessageType.OTHER.equals(assertion.getTarget())
                    ? createBackwardCompatibleContextVariableBackedMessage(context)
                    : context.getTargetMessage(assertion, true);

        } catch (NoSuchVariableException e) {
            logAndAudit(AssertionMessages.NO_SUCH_VARIABLE, e.getVariable());
            throw new AssertionStatusException(AssertionStatus.SERVER_ERROR);
        }
    }

    private Message createBackwardCompatibleContextVariableBackedMessage(PolicyEnforcementContext context) throws NoSuchVariableException {
        // Bug #12148 - For compatibility with pre-Fangtooth, detect a multivalued target message value and convert it into AbstractCollection.toString() format
        // TODO in the future we should make it possible to disable this behavior, since it is really pretty terrible, and only desirable if someone has accidentally written a policy to rely on it
        final String variableName = assertion.getOtherTargetMessageVariable();
        if (variableName == null)
            throw new IllegalArgumentException("Target is OTHER but no variable name was set");

        final Object value = context.getVariable(variableName);
        if (value == null)
            throw new NoSuchVariableException(variableName, "Target is OTHER but variable value is null");

        if (value instanceof Message)
            return (Message) value;

        final String stringVal;
        if (value instanceof CharSequence) {
            // The common case, no special behavior required
            stringVal = value.toString();
        } else if (value instanceof AbstractCollection) {
            // Pre-Fangtooth we'd match against a Message containing "[foo, bar, baz]" if targeted at a multivalued variable containing an AbstractCollection
            stringVal = value.toString();
        } else if (value instanceof Collection) {
            // Make sure hand-rolled Collections that don't inherit from AbstractCollection still use its output format
            //noinspection unchecked
            stringVal = new ArrayList((Collection)value).toString();
        } else if (value instanceof Object[]) {
            // We'll make arrays behave like Lists did pre-Fangtooth, in case an assertion changes its output from array to a list in the future
            Object[] objects = (Object[]) value;
            stringVal = Arrays.asList(objects).toString();
        } else {
            // For maximum backward compat with pre-Fangtooth behavior, we'll just call toString() for other types
            // even though this is extremely unlikely to accomplish anything useful in most cases
            stringVal = value.toString();
        }

        return ContextVariableBackedMessageUtils.createContextVariableBackedMessage(context, variableName, ContentTypeHeader.TEXT_DEFAULT, stringVal);
    }

    private AssertionStatus doMatch(PolicyEnforcementContext context, Matcher matcher) {
        final boolean matched = matcher.find();

        if (matched && captureVar != null) {
            List<String> captured = new ArrayList<String>();
            collectMatchGroups(captured, matcher);

            if (findAll) {
                boolean found;
                StringBuffer sb = new StringBuffer();
                for (;;) {
                    found = matcher.find();
                    if (!found)
                        break;
                    collectMatchGroups(captured, matcher);
                }
                matcher.appendTail(sb);
            }

            context.setVariable(captureVar, captured.toArray(new String[captured.size()]));
        }

        if (assertion.isProceedIfPatternMatches()) {
            if (matched) {
                if (logger.isLoggable(Level.FINE))
                    logger.fine("Proceeding : Matched " + assertion.getRegex());
                return AssertionStatus.NONE;
            } else {
                logAndAudit(AssertionMessages.REGEX_NO_MATCH_FAILURE, assertion.getRegex());
                return AssertionStatus.FALSIFIED;
            }
        } else { // !proceedIfPatternMatches
            if (matched) {
                logAndAudit(AssertionMessages.REGEX_MATCH_FAILURE, assertion.getRegex());
                return AssertionStatus.FALSIFIED;
            } else {
                if (logger.isLoggable(Level.FINE))
                    logger.fine("Proceeding : Not matched and proceed if no match requested " + assertion.getRegex());
                return AssertionStatus.NONE;
            }
        }
    }

    private void collectMatchGroups(List<String> captured, Matcher matcher) {
        if (includeEntireExpressionCapture)
            captured.add(matcher.group(0));
        for (int i = 1; i <= matcher.groupCount(); ++i)
            captured.add(matcher.group(i)); // may be null if it's an unmatched optional group (Bug #9395)
    }

    private AssertionStatus doReplace(PolicyEnforcementContext context, Pattern pattern, RegexInput input, RegexOutput out) throws IOException {
        String replacement = assertion.getReplacement();
        if (logger.isLoggable(Level.FINE))
            logger.log(Level.FINE, "Replace requested: Match pattern ''{0}'', replace pattern ''{1}''",
                    new Object[] { assertion.getRegex(), replacement });
        replacement = ExpandVariables.process(replacement, context.getVariableMap(varNames, getAudit()), getAudit());

        List<String> captured = new ArrayList<String>();
        final CharSequence origIn = input.getInput();
        CharSequence in = origIn;
        Matcher matcher;
        for (int iteration = 0; iteration <= replaceRepeatCount; ++iteration) {
            matcher = pattern.matcher(in);

            boolean found = matcher.find();
            if (found) {
                if (captureVar != null)
                    collectMatchGroups(captured, matcher);

                StringBuffer sb = new StringBuffer();
                for (;;) {
                    try {
                        matcher.appendReplacement(sb, replacement);
                    } catch (IndexOutOfBoundsException e) {
                        //noinspection ThrowableResultOfMethodCallIgnored
                        logAndAudit(AssertionMessages.REGEX_REPLACEMENT_INVALID, new String[]{ "unable to append replacement expression (possible nonexistent backreference)" }, ExceptionUtils.getDebugException(e));
                        throw new AssertionStatusException(AssertionStatus.SERVER_ERROR);
                    }
                    found = matcher.find();
                    if (!found)
                        break;
                    if (captureVar != null)
                        collectMatchGroups(captured, matcher);
                }
                matcher.appendTail(sb);
                in = sb.toString();
            } else {
                // No match, stop re-applying
                break;
            }
        }

        if (in != origIn)
            out.setOutput(in);
        if (captureVar != null)
            context.setVariable(captureVar, captured.toArray(new String[captured.size()]));

        return AssertionStatus.NONE;
    }

    private Pattern getPattern(PolicyEnforcementContext context) throws PolicyAssertionException {
        checkPattern();
        if (regexPatternOrTemplate.isLeft()) {
            return regexPatternOrTemplate.left();
        } else {
            String patstr = ExpandVariables.process(regexPatternOrTemplate.right(), context.getVariableMap(varNames, getAudit()), getAudit(), new Functions.Unary<String, String>() {
                @Override
                public String call(String s) {
                    // patterns resolved from context variables are treated literally
                    return Pattern.quote(s);
                }
            });
            try {
                if (caseInsensitive) {
                    return Pattern.compile(patstr, Pattern.CASE_INSENSITIVE);
                } else {
                    return Pattern.compile(patstr);
                }
            } catch (PatternSyntaxException e) {
                //noinspection ThrowableResultOfMethodCallIgnored
                logAndAudit(AssertionMessages.REGEX_PATTERN_INVALID, new String[] { patstr, ExceptionUtils.getMessage(e) }, ExceptionUtils.getDebugException(e));
                throw new AssertionStatusException(AssertionStatus.SERVER_ERROR, e);
            }
        }
    }

    private void checkPattern() throws AssertionStatusException, PolicyAssertionException {
        if (regexPatternOrTemplate == null) {
            if (compileException != null) {
                logAndAudit(AssertionMessages.REGEX_PATTERN_INVALID,
                                    new String[]{assertion.getRegex(),
                                        compileException.getMessage()}, compileException);
            } else {
                logAndAudit(AssertionMessages.REGEX_PATTERN_INVALID,
                                    assertion.getRegex(), "unknown error");
            }
            throw new AssertionStatusException(AssertionStatus.SERVER_ERROR);
        }

        if (isReplacement && assertion.getReplacement() == null) {
            logAndAudit(AssertionMessages.REGEX_NO_REPLACEMENT);
            throw new PolicyAssertionException(assertion, AssertionMessages.REGEX_NO_REPLACEMENT.getMessage());
        }
    }
}
