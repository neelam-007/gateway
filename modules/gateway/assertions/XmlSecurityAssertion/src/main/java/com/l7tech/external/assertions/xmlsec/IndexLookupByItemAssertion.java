package com.l7tech.external.assertions.xmlsec;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.variable.Syntax;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Arrays;

/**
 * Search a multivalued context variable for a matching value and record its index.
 */
public class IndexLookupByItemAssertion extends Assertion implements SetsVariables, UsesVariables {
    private static final String META_INITIALIZED = IndexLookupByItemAssertion.class.getName() + ".metadataInitialized";

    private String valueToSearchForVariableName; // May be a variable expression (omitting surrounding ${ })
    private String multivaluedVariableName;       // Must be a single variable name
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
        addExprIfPresent(used, valueToSearchForVariableName);
        addVarIfPresent(used, multivaluedVariableName);
        return used.toArray(new String[used.size()]);
    }

    private static void addVarIfPresent(Collection<String> collection, String str) {
        if (str != null && str.trim().length() > 0)
            collection.add(str);
    }

    private static void addExprIfPresent(Collection<String> collection, String expr) {
        if (expr == null || expr.trim().length() < 1)
            return;
        String[] names = Syntax.getReferencedNames("${" + expr + "}");
        collection.addAll(Arrays.asList(names));
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

    private final static String baseName = "Index Lookup by Item";

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<IndexLookupByItemAssertion>(){
        @Override
        public String getAssertionName( final IndexLookupByItemAssertion assertion, final boolean decorate) {
            if(!decorate) return baseName;
            StringBuilder name = new StringBuilder(baseName);
            if (assertion.getMultivaluedVariableName() != null && assertion.getValueToSearchForVariableName() != null) {
                name.append(": find item ${").append(assertion.getValueToSearchForVariableName()).append("} within ${").append(assertion.getMultivaluedVariableName()).append("}");
            }
            if (assertion.getOutputVariableName() != null) {
                name.append("; output index to ${").append(assertion.getOutputVariableName()).append("}");
            }
            return AssertionUtils.decorateName(assertion, name);
        }
    };

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put(AssertionMetadata.SHORT_NAME, baseName);
        meta.put(AssertionMetadata.DESCRIPTION, "Search a multivalued context variable for the value of a second variable, " +
                                                "and output the matching index number(s) to a third variable.");
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"policyLogic"});
        meta.put(AssertionMetadata.PALETTE_NODE_SORT_PRIORITY, -100);
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.xmlsec.console.IndexLookupByItemAssertionPropertiesDialog");

        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/external/assertions/xmlsec/console/resources/indexlookup16.gif");
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, policyNameFactory);

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }
}
