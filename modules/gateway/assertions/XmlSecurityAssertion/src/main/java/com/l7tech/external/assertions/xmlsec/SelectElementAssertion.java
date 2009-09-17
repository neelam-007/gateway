package com.l7tech.external.assertions.xmlsec;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.util.Functions;

/**
 * An assertion that selects a single DOM element with an XPath, and stores it in an Element context variable.
 */
public class SelectElementAssertion extends NonSoapSecurityAssertionBase implements SetsVariables {
    private static final String META_INITIALIZED = NonSoapDecryptElementAssertion.class.getName() + ".metadataInitialized";

    private String elementVariable = "element";

    public SelectElementAssertion() {
        super(TargetMessageType.REQUEST);
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        return elementVariable == null ? new VariableMetadata[0] :
                new VariableMetadata[] { new VariableMetadata(elementVariable, false, false, elementVariable, true, DataType.ELEMENT) };
    }

    public String getElementVariable() {
        return elementVariable;
    }

    public void setElementVariable(String elementVariable) {
        this.elementVariable = elementVariable;
    }

    private final static String baseName = "Select Single Element";

    @Override
    public String getDisplayName() {
        return baseName;
    }

    private final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<SelectElementAssertion>(){
        @Override
        public String getAssertionName( final SelectElementAssertion assertion, final boolean decorate) {
            final String displayName = assertion.getDisplayName();
            if(!decorate) return displayName;
            StringBuilder name = new StringBuilder(displayName + " ");
            if (assertion.getXpathExpression() == null) {
                name.append("[XPath expression not set]");
            } else {
                name.append(assertion.getXpathExpression().getExpression());
            }
            name.append(" to ${").append(assertion.getElementVariable()).append("}");
            return AssertionUtils.decorateName(assertion, name);
        }
    };

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put(AssertionMetadata.SHORT_NAME, baseName);
        meta.put(META_PROP_VERB, "select");
        meta.put(AssertionMetadata.DESCRIPTION, "Select a single Element with an XPath expression and store it in an Element context variable, perhaps for use with a subsequent Index Lookup by Item.");
        //meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"xmlSecurity"}); // Bug #7789 - do not show this assertion in the palette
        meta.put(AssertionMetadata.PALETTE_NODE_SORT_PRIORITY, -990);
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.xmlsec.console.SelectElementAssertionPropertiesDialog");

        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlsignature.gif");
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, policyNameFactory);

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }
}
