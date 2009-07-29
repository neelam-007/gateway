package com.l7tech.external.assertions.xmlsec;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.util.Functions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Search a multivalued context variable for a matching value and record its index.
 */
public class IndexLookupByItemAssertion extends Assertion implements SetsVariables, UsesVariables {
    private static final String META_INITIALIZED = IndexLookupByItemAssertion.class.getName() + ".metadataInitialized";

    private String valueToSearchForVariableName;
    private String multivaluedVariableName;
    private String outputVariableName;
    private boolean allowMultipleMatches;

    @Override
    public VariableMetadata[] getVariablesSet() {
        return outputVariableName == null || outputVariableName.trim().length() < 1 ? new VariableMetadata[0] : new VariableMetadata[] {
            new VariableMetadata(outputVariableName, false, allowMultipleMatches, outputVariableName, true, DataType.INTEGER)
        };
    }

    @Override
    public String[] getVariablesUsed() {
        List<String> used = new ArrayList<String>();
        addIfPresent(used, valueToSearchForVariableName);
        addIfPresent(used, multivaluedVariableName);
        return used.toArray(new String[used.size()]);
    }

    private static void addIfPresent(Collection<String> collection, String str) {
        if (str != null && str.trim().length() > 0)
            collection.add(str);
    }

    public String getValueToSearchForVariableName() {
        return valueToSearchForVariableName;
    }

    public void setValueToSearchForVariableName(String valueToSearchForVariableName) {
        this.valueToSearchForVariableName = valueToSearchForVariableName;
    }

    public String getMultivaluedVariableName() {
        return multivaluedVariableName;
    }

    public void setMultivaluedVariableName(String multivaluedVariableName) {
        this.multivaluedVariableName = multivaluedVariableName;
    }

    public String getOutputVariableName() {
        return outputVariableName;
    }

    public void setOutputVariableName(String outputVariableName) {
        this.outputVariableName = outputVariableName;
    }

    public boolean isAllowMultipleMatches() {
        return allowMultipleMatches;
    }

    public void setAllowMultipleMatches(boolean allowMultipleMatches) {
        this.allowMultipleMatches = allowMultipleMatches;
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put(AssertionMetadata.SHORT_NAME, "Index Lookup by Item");
        meta.put(AssertionMetadata.DESCRIPTION, "Search a multivalued context variable for the value of a second variable, " +
                                                "and output the matching index number(s) to a third variable.");
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"policyLogic"});
        meta.put(AssertionMetadata.PALETTE_NODE_SORT_PRIORITY, -100);
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.xmlsec.console.IndexLookupByItemAssertionPropertiesDialog");

        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/external/assertions/xmlsec/console/resources/indexlookup16.gif");
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, new Functions.Unary<String, IndexLookupByItemAssertion>() {
            @Override
            public String call( final IndexLookupByItemAssertion ass ) {
                StringBuilder name = new StringBuilder("Index Lookup by Item");
                if (ass.getMultivaluedVariableName() != null && ass.getValueToSearchForVariableName() != null) {
                    name.append(": find item ${").append(ass.getValueToSearchForVariableName()).append("} within ${").append(ass.getMultivaluedVariableName()).append("}");
                }
                if (ass.getOutputVariableName() != null) {
                    name.append("; output index to ${").append(ass.getOutputVariableName()).append("}");
                }
                return AssertionUtils.decorateName(ass, name);
            }
        });

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }
}
