package com.l7tech.policy.assertion;

import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.variable.Syntax;

import java.util.logging.Level;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;
import static com.l7tech.policy.assertion.AssertionMetadata.*;

/**
 * Allows administrator to add audit details to the audit context of a message. Detail record
 * is added using the record id AssertionMessages.USERDETAIL.
 *
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jan 19, 2006<br/>
 */
public class AuditDetailAssertion extends Assertion implements UsesVariables {
    public static final String CUSTOM_LOGGER_NAME_PATTERN = "^[A-Za-z][A-Za-z0-9]*(?:\\.[A-Za-z][A-Za-z0-9]*)*$";
    private String level = Level.INFO.toString();
    private String detail;
    private boolean loggingOnly = false;
    private String customLoggerSuffix = null;

    public AuditDetailAssertion() {
    }

    public AuditDetailAssertion(final String detail) {
        setDetail(detail);
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    /**
     * @return  true if this details message is to be sent immediately to the log sinks, bypassing the audit subsystem.
     */
    public boolean isLoggingOnly() {
        return loggingOnly;
    }

    /**
     * @param loggingOnly  true if this detail message should be sent immediately to the log sinks, bypassing the audit subsystem.
     */
    public void setLoggingOnly(boolean loggingOnly) {
        this.loggingOnly = loggingOnly;
    }

    /**
     * @return suffix for custom logger name, or null if custom logger is disabled.
     */
    public String getCustomLoggerSuffix() {
        return customLoggerSuffix;
    }

    /**
     * A custom suffix to use for a custom logger name, or null to use the default logger name.
     * <p/>
     * The custom logger prefix is always "com.l7tech.log.custom.".
     *
     * @param customLoggerSuffix suffix for custom logger name, or null to disable use of a custom logger name.
     */
    public void setCustomLoggerSuffix(String customLoggerSuffix) {
        this.customLoggerSuffix = customLoggerSuffix;
    }

    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    public String[] getVariablesUsed() {
        return Syntax.getReferencedNames(detail, customLoggerSuffix);
    }

    private final static String baseName = "Add Audit Details";

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<AuditDetailAssertion>(){
        @Override
        public String getAssertionName( final AuditDetailAssertion assertion, final boolean decorate) {
            if(!decorate) return baseName;
            return baseName + ": \"" + assertion.getDetail() + "\"";
        }
    };

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(PALETTE_FOLDERS, new String[]{"audit"});

        meta.put(SHORT_NAME, baseName);
        meta.put(DESCRIPTION, "Audit a custom message with a selected level.");

        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/Edit16.gif");

        meta.put(POLICY_NODE_NAME_FACTORY, policyNameFactory);

        meta.put(PROPERTIES_ACTION_CLASSNAME, "com.l7tech.console.action.AuditDetailsPropertiesAction");
        meta.put(PROPERTIES_ACTION_NAME, "Audit Detail Properties");
        return meta;
    }
}