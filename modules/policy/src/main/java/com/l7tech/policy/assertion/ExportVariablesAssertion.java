package com.l7tech.policy.assertion;

import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import org.jetbrains.annotations.Nullable;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;

/**
 * A utility assertion that can mark one or more variables as in use.
 * <p/>
 * Also, as of 7.0, copies the value back to a parent PolicyEnforcementContext if one is present.
 */
public class ExportVariablesAssertion extends Assertion implements UsesVariables {
    private String[] exportedVars = new String[0];

    public String[] getExportedVars() {
        return exportedVars;
    }

    public void setExportedVars(@Nullable String[] exportedVars) {
        if (exportedVars == null)
            exportedVars = new String[0];
        this.exportedVars = exportedVars;
    }

    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    @Override
    public String[] getVariablesUsed() {
        return exportedVars;
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();
        meta.put(AssertionMetadata.SHORT_NAME, "Export Variables from Fragment");
        meta.put(AssertionMetadata.DESCRIPTION, "Designate variables that will be used by other policies that include this policy.\nThis assertion should be used within an included policy, near the end.");
        meta.put(AssertionMetadata.PALETTE_FOLDERS,  new String[] { "policyLogic" });
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "none"); // Can't open properties dialog until assertion is embedded in a policy
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.console.panels.ExportVariablesAssertionPropertiesDialog");
        return meta;
    }
}
