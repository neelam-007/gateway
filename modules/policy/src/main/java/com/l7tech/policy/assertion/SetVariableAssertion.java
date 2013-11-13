package com.l7tech.policy.assertion;

import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.assertion.annotation.Base64Value;
import com.l7tech.policy.variable.*;
import com.l7tech.util.Charsets;
import com.l7tech.util.HexUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Calendar;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;
import static com.l7tech.policy.assertion.AssertionMetadata.*;
import static com.l7tech.policy.assertion.VariableUseSupport.expressions;

/**
 * Assertion to set a context variable, either built-in or user-defined.
 *
 * <p>Related function specifications:
 * <ul>
 *  <li><a href="http://sarek.l7tech.com/mediawiki/index.php?title=XML_Variables">XML Variables</a> (4.3)
 * </ul>
 */
public class SetVariableAssertion extends Assertion implements SetsVariables, UsesVariables {

    /**
     * Enum wrapper for Calendar int fields. Not serialized, just used for convenience.
     */
    public static enum CalendarFields {
        MILLISECONDS(Calendar.MILLISECOND, "Milliseconds"),
        SECONDS(Calendar.SECOND, "Seconds"),
        MINUTES(Calendar.MINUTE, "Minutes"),
        HOURS(Calendar.HOUR_OF_DAY, "Hours"),
        DAYS(Calendar.DAY_OF_MONTH, "Days"),
        WEEKS(Calendar.WEEK_OF_YEAR, "Weeks"),
        MONTHS(Calendar.MONTH, "Months")
        ;

        CalendarFields(int calendarField, String displayName) {
            this.calendarField = calendarField;
            this.displayName = displayName;
        }

        public int getCalendarField() {
            return calendarField;
        }

        public String getDisplayName() {
            return displayName;
        }

        public static CalendarFields getCalendarField(final int calendarField) {
            final CalendarFields[] values = CalendarFields.values();
            for (CalendarFields value : values) {
                if (value.getCalendarField() == calendarField) {
                    return value;
                }
            }
            throw new IllegalArgumentException("Unknown calendar field : " + calendarField);
        }

        private final int calendarField;
        private final String displayName;
    }

    private String _variableToSet;

    /**
     * This field is introducted in 4.3.
     * Defaults to {@link DataType#STRING} for pre-4.3 compatibility.
     * @since SecureSpan 4.3
     */
    private DataType _dataType = DataType.STRING;

    /**
     * This field replaces expression since 4.3.
     * @since SecureSpan 4.3
     */
    private String _base64Expression;   // Base64-encoded to workaround serializer not turning CR, LF into entity characters

    /**
     * This field is introducted in 4.3.
     * Defaults to {@link LineBreak#CRLF} for pre-4.3 compatibility.
     * @since SecureSpan 4.3
     */
    private LineBreak _lineBreak = LineBreak.CRLF;

    /**
     * Used only if {@link #_dataType} == {@link DataType#MESSAGE}.
     * This field is introducted in 4.3.
     * @since SecureSpan 4.3
     */
    private String _contentType;

    /**
     * Used only if {@link #_dataType} == {@link DataType#DATE_TIME}
     *
     * @since SecureSpan 7.0
     */
    private String dateFormat;

    /**
     * Used only if {@link #_dataType} == {@link DataType#DATE_TIME}
     *
     * @since SecureSpan 7.0
     */
    private int dateOffsetField = CalendarFields.SECONDS.getCalendarField();

    /**
     * Used only if {@link #_dataType} == {@link DataType#DATE_TIME}
     *
     * @since SecureSpan 7.0
     */
    private String dateOffsetExpression;

    private transient VariableMetadata _meta;   // just for caching

    public SetVariableAssertion() {
    }

    public SetVariableAssertion(String variableToSet, String stringValue) {
        this._variableToSet = variableToSet;
        setDataType(DataType.STRING);
        setExpression(stringValue);
    }

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

    @Base64Value(decodeMethodName = "expression")
    public String getBase64Expression() {
        return _base64Expression;
    }

    public void setBase64Expression(String base64Expression) {
        expressionString = null;
        _base64Expression = base64Expression;
    }

    public String expression() {
        if (expressionString != null)
            return expressionString;
        try {
            return expressionString = new String(HexUtils.decodeBase64(_base64Expression, true), Charsets.UTF8);
        } catch (Exception e) {
            return _base64Expression;
        }
    }

    /**
     * Only use when deserializing pre-4.3 policy XML.
     * @param expression not encoded in Base64
     */
    public void setExpression(String expression) {
        setBase64Expression(HexUtils.encodeBase64(HexUtils.encodeUtf8(expression), true));
    }

    public LineBreak getLineBreak() {
        return _lineBreak;
    }

    public void setLineBreak(LineBreak lineBreak) {
        _lineBreak = lineBreak;
    }


    public String getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(@Nullable String dateFormat) {
        this.dateFormat = dateFormat;
    }

    public int getDateOffsetField() {
        return dateOffsetField;
    }

    public void setDateOffsetField(int dateOffsetField) {
        this.dateOffsetField = dateOffsetField;
    }

    public String getDateOffsetExpression() {
        return dateOffsetExpression;
    }

    public void setDateOffsetExpression(@Nullable String dateOffsetExpression) {
        this.dateOffsetExpression = dateOffsetExpression;
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        if (_variableToSet == null) return new VariableMetadata[0];
        return new VariableMetadata[] { getMetadata() };
    }

    @Override
    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    public String[] getVariablesUsed() {
        return expressions(expression(), getDateFormat(), getDateOffsetExpression()).asArray();
    }

    private VariableMetadata getMetadata() {
        if (_meta == null) {
            _meta = BuiltinVariables.getMetadata(_variableToSet);
            if (_meta == null) _meta = new VariableMetadata(_variableToSet, false, false, null, true, _dataType);
        }
        return _meta;
    }

    private transient String expressionString;
    private final static String baseName = "Set Context Variable";
    private static final int MAX_DISPLAY_LENGTH = 60;
    
    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<SetVariableAssertion>(){
        @Override
        public String getAssertionName( final SetVariableAssertion assertion, final boolean decorate) {
            if(!decorate) return baseName;

            StringBuffer name = new StringBuffer(baseName + " ");
            name.append(assertion.getVariableToSet());
            name.append(" as ");
            name.append(assertion.getDataType().getName());
            name.append(" to");
            final String expression = assertion.expression();
            if (expression.length() == 0) {
                name.append(" empty");
            } else if (expression.length() <= MAX_DISPLAY_LENGTH) {
                name.append(": ");
                name.append(expression);
            } else {
                name.append(": ");
                name.append(expression, 0, MAX_DISPLAY_LENGTH - 1);
                name.append("...");
            }
            return name.toString();
        }
    };

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(PALETTE_FOLDERS, new String[]{"policyLogic"});

        meta.put(SHORT_NAME, baseName);
        meta.put(DESCRIPTION, "Create custom context variables.");

        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/check16.gif");

        meta.put(POLICY_NODE_NAME_FACTORY, policyNameFactory);

        meta.put(PROPERTIES_ACTION_CLASSNAME, "com.l7tech.console.action.SetVariableAssertionPropertiesAction");
        meta.put(PROPERTIES_ACTION_NAME, "Context Variable Properties");
        meta.put(PROPERTIES_ACTION_ICON, "com/l7tech/console/resources/About16.gif");
        meta.put(POLICY_VALIDATOR_CLASSNAME, "com.l7tech.policy.validator.SetVariableAssertionValidator");

        return meta;
    }

}
