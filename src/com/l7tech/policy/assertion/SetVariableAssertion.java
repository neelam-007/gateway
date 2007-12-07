package com.l7tech.policy.assertion;

import com.l7tech.common.util.HexUtils;
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
    private String _base64Expression;   // Base64-encoded to workaround serializer not turning CR, LF into entity characters
    private LineBreak _lineBreak;

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

    public String getBase64Expression() {
        return _base64Expression;
    }

    public void setBase64Expression(String base64Expression) {
        _base64Expression = base64Expression;
    }

    public String expression() {
        try {
            return new String(HexUtils.decodeBase64(_base64Expression, true), "UTF-8");
        } catch (Exception e) {
            return _base64Expression;
        }
    }

    public void expression(String expression) {
        setBase64Expression(HexUtils.encodeBase64(HexUtils.encodeUtf8(expression), true));
    }

    public LineBreak getLineBreak() {
        return _lineBreak;
    }

    public void setLineBreak(LineBreak lineBreak) {
        _lineBreak = lineBreak;
    }

    public VariableMetadata[] getVariablesSet() {
        if (_variableToSet == null) return new VariableMetadata[0];
        return new VariableMetadata[] { getMetadata() };
    }

    public String[] getVariablesUsed() {
        if (_base64Expression == null) return new String[0];
        return Syntax.getReferencedNames(_base64Expression);
    }

    private VariableMetadata getMetadata() {
        if (_meta == null) {
            _meta = BuiltinVariables.getMetadata(_variableToSet);
            if (_meta == null) _meta = new VariableMetadata(_variableToSet, false, false, null, true, _dataType);
        }
        return _meta;
    }
}
