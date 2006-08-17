package com.l7tech.policy.assertion;

import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.variable.BuiltinVariables;
import com.l7tech.policy.variable.ExpandVariables;
import com.l7tech.policy.variable.VariableNotSettableException;

public class SetVariableAssertion extends Assertion implements SetsVariables, UsesVariables {
    private String variableToSet;
    private String expression;

    private transient VariableMetadata meta;

    public String getVariableToSet() {
        return variableToSet;
    }

    public void setVariableToSet(String variableToSet) throws VariableNotSettableException {
        VariableMetadata meta = getMetadata(variableToSet);
        if (meta != null && !meta.isSettable()) throw new VariableNotSettableException(variableToSet);
        this.variableToSet = variableToSet;
        this.meta = null;
    }

    private VariableMetadata getMetadata(String variableToSet) {
        if (meta == null) {
            meta = BuiltinVariables.getMetadata(variableToSet);
            if (meta == null) meta = new VariableMetadata(variableToSet, false, false, null, true);
        }
        return meta;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
        this.meta = null;
    }

    public VariableMetadata[] getVariablesSet() {
        if (variableToSet == null) return new VariableMetadata[0];
        return new VariableMetadata[] { getMetadata(variableToSet) };
    }

    public String[] getVariablesUsed() {
        if (expression == null) return new String[0];
        return ExpandVariables.getReferencedNames(expression);
    }
}
