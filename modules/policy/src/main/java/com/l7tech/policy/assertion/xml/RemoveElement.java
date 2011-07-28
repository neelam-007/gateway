package com.l7tech.policy.assertion.xml;

import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.annotation.RequiresXML;
import com.l7tech.policy.wsp.Java5EnumTypeMapping;
import com.l7tech.policy.wsp.SimpleTypeMappingFinder;
import com.l7tech.policy.wsp.TypeMapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;
import static com.l7tech.policy.assertion.AssertionMetadata.*;

/**
 * Removes an XML element from the target specified through the MessageTargetable interface.
 * The element to be removed is specified through a context variable.
 *
 * @author jbufu
 */
@RequiresXML()
public class RemoveElement extends MessageTargetableAssertion {

    private static final String META_INITIALIZED = RemoveElement.class.getName() + ".metadataInitialized";

    public static enum ElementLocation {
        FIRST_CHILD("First Child"),
        LAST_CHILD("Last Child"),
        PREVIOUS_SIBLING("Previous Sibling"),
        NEXT_SIBLING("Next Sibling")
        ;

        private final String text;

        private ElementLocation(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }

        @Override
        public String toString() {
            return text;
        }
    }

    private String elementFromVariable;
    private String elementToInsertVariable;
    private ElementLocation insertedElementLocation;

    public RemoveElement() {
        super(true);
    }

    /**
     * @return name of variable containing existing element within the document, or null if not configured.
     */
    public String getElementFromVariable() {
        return elementFromVariable;
    }

    /**
     * @param elementFromVariable name of variable containing existing element within the document.  Should be non-null once assertion is fully configured.
     */
    public void setElementFromVariable(String elementFromVariable) {
        this.elementFromVariable = elementFromVariable;
    }

    /**
     * @return name of variable containing new element to insert into document, or null if not configured.
     */
    public String getElementToInsertVariable() {
        return elementToInsertVariable;
    }

    /**
     * @param elementToInsertVariable name of variable conatining new element to insert into document.  Ignored if {@link #getInsertedElementLocation()} is null.
     */
    public void setElementToInsertVariable(String elementToInsertVariable) {
        this.elementToInsertVariable = elementToInsertVariable;
    }

    /**
     * @return location relative to existing element of where to insert new element, or null if removing existing element.
     */
    public ElementLocation getInsertedElementLocation() {
        return insertedElementLocation;
    }

    /**
     * @param insertedElementLocation location relative to existing element of where to insert new element, or null if removing existing element.
     */
    public void setInsertedElementLocation(ElementLocation insertedElementLocation) {
        this.insertedElementLocation = insertedElementLocation;
    }

    private final static String baseName = "Add or Remove XML Element(s)";
    private final static String baseNameRemove = "Remove XML Element(s)";
    private final static String baseNameInsert = "Add XML Element(s)";

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<RemoveElement>(){
        @Override
        public String getAssertionName( final RemoveElement assertion, final boolean decorate) {
            if(!decorate) return baseName;
            final boolean inserting = assertion.getInsertedElementLocation() != null;
            StringBuilder name = new StringBuilder(AssertionUtils.decorateName(assertion, inserting ? baseNameInsert : baseNameRemove));
            if (inserting) {
                name.append(" ").append(assertion.getElementToInsertVariable());
                name.append(" as ").append(assertion.getInsertedElementLocation());
                name.append(" of ").append(assertion.getElementFromVariable());
            } else {
                name.append(" ").append(assertion.getElementFromVariable());
            }
            return name.toString();
        }
    };

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put(SHORT_NAME, baseName);
        meta.put(DESCRIPTION, "Add or remove one or more XML elements from a message.");
        meta.put(POLICY_NODE_NAME_FACTORY, policyNameFactory);
        meta.put(PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.console.panels.RemoveElementPropertiesDialog");
        meta.put(PROPERTIES_ACTION_NAME, "Add or Remove XML Elements Properties");
        meta.put(PALETTE_FOLDERS, new String[] { "xml" });
        meta.put(POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder(Arrays.<TypeMapping>asList(
            new Java5EnumTypeMapping(ElementLocation.class, "insertedElementLocation")
        )));
        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    @Override
    protected VariablesUsed doGetVariablesUsed() {
        return super.doGetVariablesUsed().withVariables(
                elementFromVariable,
                insertedElementLocation != null ? elementToInsertVariable : null
        );
    }
}
