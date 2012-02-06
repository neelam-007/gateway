/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.util.NameValuePair;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;
import static com.l7tech.policy.assertion.AssertionMetadata.*;


/**
 * An assertion that performs a mapping from an input value to an output value.
 */
public class MapValueAssertion extends Assertion implements UsesVariables, SetsVariables {

    private String inputExpr;
    private String outputVar;
    private NameValuePair[] mappings;

    public MapValueAssertion() {}

    public String getInputExpr() {
        return inputExpr;
    }

    public void setInputExpr(String inputExpr) {
        this.inputExpr = inputExpr;
    }

    public String getOutputVar() {
        return outputVar;
    }

    public void setOutputVar(String outputVar) {
        this.outputVar = outputVar;
    }

    public NameValuePair[] getMappings() {
        return mappings;
    }

    public void setMappings( NameValuePair[] mappings) {
        this.mappings = mappings;
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        return outputVar == null ? new VariableMetadata[0] : new VariableMetadata[] { new VariableMetadata(outputVar) };
    }

    @Override
    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    public String[] getVariablesUsed() {
        return Syntax.getReferencedNames(inputExpr);
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(PALETTE_FOLDERS, new String[]{"policyLogic"});

        meta.put(SHORT_NAME, "Map Value");
        meta.put(DESCRIPTION, "Map an input set to an output set.  Match an input value against a list of patterns, and output the first matching result.");

        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/Map16.gif");
        
        meta.put(PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.console.panels.MapValueAssertionPropertiesDialog");

        return meta;
    }
}
