package com.l7tech.policy.assertion;

import com.l7tech.policy.variable.Syntax;
import static com.l7tech.policy.assertion.AssertionMetadata.*;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;

import java.util.logging.Level;

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
    private String level = Level.INFO.toString();
    private String detail;

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

    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    public String[] getVariablesUsed() {
        return Syntax.getReferencedNames(detail);
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
