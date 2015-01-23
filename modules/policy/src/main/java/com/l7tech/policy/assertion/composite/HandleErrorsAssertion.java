package com.l7tech.policy.assertion.composite;


import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.SetsVariables;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;

import java.util.List;

import static com.l7tech.policy.assertion.AssertionMetadata.*;

public class HandleErrorsAssertion extends CompositeAssertion implements SetsVariables {

    public static final String VARIABLE_PREFIX = "handleError";

    private static final String META_INITIALIZED = HandleErrorsAssertion.class.getName() + ".metadataInitialized";

    private String variablePrefix = VARIABLE_PREFIX;

    private boolean includeIOException;

    public HandleErrorsAssertion() {

    }

    public HandleErrorsAssertion(List<? extends Assertion> children) {
        super(children);
    }

    public boolean isRoot() {
        return getParent() == null;
    }

    @Override
    public boolean permitsEmpty() {
        return true;
    }

    public String getVariablePrefix() {
        return variablePrefix;
    }

    public void setVariablePrefix(String variablePrefix) {
        this.variablePrefix = variablePrefix;
    }

    public boolean isIncludeIOException() {
        return includeIOException;
    }

    public void setIncludeIOException(boolean includeIOException) {
        this.includeIOException = includeIOException;
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;
        meta.put(PALETTE_FOLDERS, new String[]{"policyLogic"});
        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/folder.gif");
        meta.put(POLICY_NODE_ICON_OPEN, "com/l7tech/console/resources/folderOpen.gif");

        meta.put(SHORT_NAME, "Handle Errors");
        meta.put(DESCRIPTION, "Handle errors raised by child assertions.");

        meta.put(POLICY_NODE_CLASSNAME, "com.l7tech.console.tree.policy.HandleErrorsAssertionTreeNode");

        meta.put(PROPERTIES_ACTION_NAME, "Handle Errors Properties");
        meta.put(PROPERTIES_ACTION_ICON, "com/l7tech/console/resources/folder.gif");

        meta.put(PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.console.panels.HandleErrorsPropertiesDialog");
        meta.put(WSP_TYPE_MAPPING_CLASSNAME, "com.l7tech.policy.assertion.composite.HandleErrorsAssertionTypeMapping");

        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        meta.put(FEATURE_SET_NAME, "(fromClass)");
        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    public VariableMetadata[] getVariablesSet() {
        return variablePrefix == null ? new VariableMetadata[0] : new VariableMetadata[] {
                new VariableMetadata(getVariablePrefix() + ".message", false, false, null, false, DataType.STRING)
        };
    }

    @Override
    public HandleErrorsAssertion clone() {
        HandleErrorsAssertion copy = (HandleErrorsAssertion) super.clone();
        copy.setVariablePrefix(variablePrefix);
        return copy;
    }

}
