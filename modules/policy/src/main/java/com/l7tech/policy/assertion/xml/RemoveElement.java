package com.l7tech.policy.assertion.xml;

import com.l7tech.policy.assertion.annotation.RequiresXML;
import com.l7tech.policy.assertion.*;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;

import java.util.*;

/**
 * Removes an XML element from the target specified through the MessageTargetable interface.
 * The element to be removed is specified through a context variable.
 *
 * @author jbufu
 */
@RequiresXML()
public class RemoveElement extends MessageTargetableAssertion {

    private String elementFromVariable;

    public RemoveElement() {
    }

    public String getElementFromVariable() {
        return elementFromVariable;
    }

    public void setElementFromVariable(String elementFromVariable) {
        this.elementFromVariable = elementFromVariable;
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();
        meta.put(AssertionMetadata.SHORT_NAME, "Remove XML Element(s)");
        meta.put(AssertionMetadata.DESCRIPTION, "Remove one or more XML elements from a message.");
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.console.panels.RemoveElementPropertiesDialog");
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "xml" });
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");
        return meta;
    }

    @Override
    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    public String[] getVariablesUsed() {
        List<String> vars = new ArrayList<String>(Arrays.asList(super.getVariablesUsed()));
        if (elementFromVariable != null)
            vars.add(elementFromVariable);
        return vars.toArray(new String[vars.size()]);
    }
}
