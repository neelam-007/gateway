package com.l7tech.external.assertions.xmlsec;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.util.Functions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 *
 */
public class ItemLookupByIndexAssertion extends Assertion implements SetsVariables, UsesVariables {
    private static final String META_INITIALIZED = ItemLookupByIndexAssertion.class.getName() + ".metadataInitialized";

    private String multivaluedVariableName;
    private String indexValue = "0";
    private String outputVariableName = "output";
    private boolean allowMultipleMatches = false;

    @Override
    public VariableMetadata[] getVariablesSet() {
        return outputVariableName == null || outputVariableName.trim().length() < 1 ? new VariableMetadata[0] : new VariableMetadata[] {
            new VariableMetadata(outputVariableName, false, allowMultipleMatches, outputVariableName, true, DataType.INTEGER)
        };
    }

    @Override
    public String[] getVariablesUsed() {
        List<String> used = new ArrayList<String>();
        if (indexValue != null)
            used.addAll(Arrays.asList(Syntax.getReferencedNames(indexValue)));
        addIfPresent(used, multivaluedVariableName);
        return used.toArray(new String[used.size()]);
    }

    private static void addIfPresent(Collection<String> collection, String str) {
        if (str != null && str.trim().length() > 0)
            collection.add(str);
    }

    public String getIndexValue() {
        return indexValue;
    }

    /** Index value, allowing variable interpolation.  Should be either a number or an interpolated name of a variable that contains a number. */
    public void setIndexValue(String indexValue) {
        this.indexValue = indexValue;
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

        meta.put(AssertionMetadata.SHORT_NAME, "Item Lookup by Index");
        meta.put(AssertionMetadata.DESCRIPTION, "Copy a single item out of a multivalued context variable, identified by its index, into a single-valued context variable.");
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"policyLogic"});
        meta.put(AssertionMetadata.PALETTE_NODE_SORT_PRIORITY, -101);
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.xmlsec.console.ItemLookupByIndexAssertionPropertiesDialog");

        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/external/assertions/xmlsec/console/resources/itemlookup16.gif");
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, new Functions.Unary<String, ItemLookupByIndexAssertion>() {
            @Override
            public String call( final ItemLookupByIndexAssertion ass ) {
                StringBuilder name = new StringBuilder("Item Lookup by Index");
                if (ass.getMultivaluedVariableName() != null && ass.getIndexValue() != null) {
                    name.append(": find index ").append(ass.getIndexValue()).append(" within ${").append(ass.getMultivaluedVariableName()).append("}");
                }
                if (ass.getOutputVariableName() != null) {
                    name.append("; output value to ${").append(ass.getOutputVariableName()).append("}");
                }
                return AssertionUtils.decorateName(ass, name);
            }
        });

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }
}
