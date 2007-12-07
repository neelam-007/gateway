package com.l7tech.policy.assertion;

import com.l7tech.policy.variable.*;

/**
 * Assertion to set a context variable, either built-in or user-defined.
 *
 * <p>Related function specifications:
 * <ul>
 *  <li><a href="http://sarek.l7tech.com/mediawiki/index.php?title=XML_Variables">XML Variables</a> (4.3)
 * </ul>
 */
public class SetVariableAssertion extends Assertion implements SetsVariables, UsesVariables {
    private String _variableToSet;
    private DataType _dataType;
    private String _expression;

    /** Used only if {@link #_dataType} == {@link DataType#MESSAGE}. */
    private String _contentType;

    private transient VariableMetadata _meta;   // just for caching

    public String getVariableToSet() {
        return _variableToSet;
    }

    public void setVariableToSet(String variableToSet) throws VariableNotSettableException {
        final VariableMetadata meta = BuiltinVariables.getMetadata(variableToSet);
        if (meta != null) { // It's built-in variable.
            if (!meta.isSettable()) throw new VariableNotSettableException(variableToSet);
            _dataType = meta.getType(); // forces data type for built-in variable.
        }

        _variableToSet = variableToSet;
        _meta = null;   // need to refresh cache
    }

    public DataType getDataType() {
        return _dataType;
    }

    public void setDataType(DataType dataType) {
        if (_variableToSet != null) {
            final VariableMetadata meta = BuiltinVariables.getMetadata(_variableToSet);
            if (meta != null) { // It's built-in variable.
                if (meta.getType() != dataType) throw new VariableDataTypeNotChangeableException(_variableToSet);
            }
        }

        _dataType = dataType;
        _meta = null;   // need to refresh cache
    }

    public void setContentType(String contentType) {
        _contentType = contentType;
    }

    public String getContentType() {
        return _contentType;
    }

    public String getExpression() {
        return _expression;
    }

    public void setExpression(String expression) {
        _expression = expression;
    }

    public VariableMetadata[] getVariablesSet() {
        if (_variableToSet == null) return new VariableMetadata[0];
        return new VariableMetadata[] { getMetadata() };
    }

    public String[] getVariablesUsed() {
        if (_expression == null) return new String[0];
        return Syntax.getReferencedNames(_expression);
    }

    private VariableMetadata getMetadata() {
        if (_meta == null) {
            _meta = BuiltinVariables.getMetadata(_variableToSet);
            if (_meta == null) _meta = new VariableMetadata(_variableToSet, false, false, null, true, _dataType);
        }
        return _meta;
    }
}
