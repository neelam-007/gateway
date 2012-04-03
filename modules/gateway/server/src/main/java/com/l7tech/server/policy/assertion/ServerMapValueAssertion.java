package com.l7tech.server.policy.assertion;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.AuditFactory;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.MapValueAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.Either;
import com.l7tech.util.Functions;
import com.l7tech.util.NameValuePair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Server assertion that applies a mapping to an input value to produce an output value.
 */
public class ServerMapValueAssertion extends AbstractServerAssertion<MapValueAssertion> {
    private static final int PATTERN_FLAGS = Pattern.DOTALL | Pattern.MULTILINE;

    private final List<MapValueMapping> mappings;
    private final String[] varsUsed;

    public ServerMapValueAssertion(@NotNull final MapValueAssertion assertion) {
        this(assertion, null);
    }

    public ServerMapValueAssertion(@NotNull final MapValueAssertion assertion, @Nullable final AuditFactory auditFactory) {
        super(assertion, auditFactory);

        List<MapValueMapping> mappings = new ArrayList<MapValueMapping>();
        if (assertion.getMappings() != null) {
            for (NameValuePair mapping : assertion.getMappings()) {
                mappings.add(new MapValueMapping(mapping));
            }
        }

        this.mappings = mappings;
        this.varsUsed = assertion.getVariablesUsed();
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        final Map<String,Object> variableMap = context.getVariableMap(varsUsed, getAudit());
        String inputStr = ExpandVariables.process(assertion.getInputExpr(), variableMap, getAudit());

        for (MapValueMapping mapping : mappings) {
            final Pattern pattern = mapping.getPattern(variableMap, getAudit());
            final Matcher matcher = pattern.matcher(inputStr);
            if (matcher.find()) {
                // Add capture groups to variable map prior to expansion:  ${0} (entire match),  ${1} (first capture group), etc
                int groupCount = matcher.groupCount();
                for (int i = 0; i <= groupCount; ++i) {
                    String captureValue = matcher.group(i);
                    variableMap.put(String.valueOf(i), captureValue); // no need to copy variableMap first
                }

                String result = ExpandVariables.process(mapping.outputTemplate, variableMap, getAudit());
                
                context.setVariable(assertion.getOutputVar(), result);
                getAudit().logAndAudit(AssertionMessages.MAP_VALUE_PATTERN_MATCHED, pattern.toString());
                return AssertionStatus.NONE;
            }
            getAudit().logAndAudit(AssertionMessages.MAP_VALUE_PATTERN_NOT_MATCHED, pattern.toString());
        }

        getAudit().logAndAudit(AssertionMessages.MAP_VALUE_NO_PATTERNS_MATCHED);
        return AssertionStatus.FAILED;
    }

    private static final class MapValueMapping {
        final Either<Pattern,String> patternOrTemplate;
        final String outputTemplate;

        private MapValueMapping(NameValuePair mapping) {
            this.patternOrTemplate = makePatternOrTemplate(mapping.left);
            this.outputTemplate = mapping.right;
        }

        private static Either<Pattern,String> makePatternOrTemplate(String pat) {
            if (Syntax.getReferencedNames(pat).length > 0) {
                return Either.right(pat);
            } else {
                return Either.left(Pattern.compile(pat, PATTERN_FLAGS));
            }
        }

        private Pattern getPattern(Map<String, ?> variableMap, Audit audit) {
            if (patternOrTemplate.isLeft())
                return patternOrTemplate.left();

            final String patternTemplate = patternOrTemplate.right();
            final String expandedAndQuotedPattern = ExpandVariables.process(patternTemplate, variableMap, audit, new Functions.Unary<String, String>() {
                @Override
                public String call(String s) {
                    return Pattern.quote(s);
                }
            });

            return Pattern.compile(expandedAndQuotedPattern, PATTERN_FLAGS);
        }
    }
}
