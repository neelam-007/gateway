package com.l7tech.server.policy.assertion;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.MapValueAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.NameValuePair;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Server assertion that applies a mapping to an input value to produce an output value.
 */
public class ServerMapValueAssertion extends AbstractServerAssertion<MapValueAssertion> {
    private final List<Pair<Pattern, String>> mappings;
    private final String[] varsUsed;

    public ServerMapValueAssertion(@NotNull final MapValueAssertion assertion) {
        super(assertion);

        List<Pair<Pattern, String>> mappings = new ArrayList<Pair<Pattern, String>>();
        if (assertion.getMappings() != null) {
            for (NameValuePair mapping : assertion.getMappings()) {
                mappings.add(new Pair<Pattern, String>(Pattern.compile(mapping.left), mapping.right));
            }
        }

        this.mappings = mappings;
        this.varsUsed = assertion.getVariablesUsed();
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        String inputStr = ExpandVariables.process(assertion.getInputExpr(), context.getVariableMap(varsUsed, getAudit()), getAudit());

        for (Pair<Pattern, String> mapping : mappings) {
            if (mapping.left.matcher(inputStr).find()) {
                context.setVariable(assertion.getOutputVar(), mapping.right);
                return AssertionStatus.NONE;
            }
        }
        
        return AssertionStatus.FAILED;
    }
}
