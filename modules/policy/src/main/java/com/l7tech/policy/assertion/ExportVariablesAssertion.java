package com.l7tech.policy.assertion;

/**
 * A utility assertion that can mark one or more variables as in use.
 */
public class ExportVariablesAssertion extends Assertion implements UsesVariables {
    private String[] exportedVars = new String[0];

    public String[] getExportedVars() {
        return exportedVars;
    }

    public void setExportedVars(String[] exportedVars) {
        if (exportedVars == null)
            exportedVars = new String[0];
        this.exportedVars = exportedVars;
    }

    @Override
    public String[] getVariablesUsed() {
        return exportedVars;
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();
        meta.put(AssertionMetadata.SHORT_NAME, "Export Variables from Fragment");
        meta.put(AssertionMetadata.DESCRIPTION, "Designate variables that will be used by other policies that include this policy.");
        meta.put(AssertionMetadata.PALETTE_FOLDERS,  new String[] { "policyLogic" });
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "none"); // Can't open properties dialog until assertion is embedded in a policy
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.console.panels.ExportVariablesAssertionPropertiesDialog");
        meta.put(AssertionMetadata.SERVER_ASSERTION_CLASSNAME, "com.l7tech.server.policy.assertion.ServerTrueAssertion");
        return meta;
    }
}
