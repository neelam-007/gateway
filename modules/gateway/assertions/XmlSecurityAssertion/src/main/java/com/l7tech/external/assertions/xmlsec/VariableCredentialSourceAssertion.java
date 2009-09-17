package com.l7tech.external.assertions.xmlsec;

import com.l7tech.policy.assertion.*;
import com.l7tech.util.Functions;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * An assertion that can gather arbitrary context variables as credentials, if they are X509Certificates
 * or SecurityTokens.
 */
public class VariableCredentialSourceAssertion extends MessageTargetableAssertion {
    private static final String META_INITIALIZED = VariableCredentialSourceAssertion.class.getName() + ".metadataInitialized";

    private String variableName;

    public VariableCredentialSourceAssertion() {
        super(TargetMessageType.REQUEST);
    }

    public String getVariableName() {
        return variableName;
    }

    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    @Override
    public String[] getVariablesUsed() {
        List<String> variables = new ArrayList<String>();
        variables.addAll( Arrays.asList( super.getVariablesUsed() ) );
        if (variableName != null)
            variables.add(variableName);
        return variables.toArray( new String[variables.size()] );
    }

    @Override
    public boolean isCredentialSource() {
        return true;
    }

    private final static String baseName = "Variable Credential Source";

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<VariableCredentialSourceAssertion>(){
        @Override
        public String getAssertionName( final VariableCredentialSourceAssertion assertion, final boolean decorate) {
            if(!decorate) return baseName;
            StringBuilder name = new StringBuilder("Credentials from variable: ");
            name.append(assertion.getVariableName() == null ? "<Not yet set>" : "${" + assertion.getVariableName() + "}");
            return AssertionUtils.decorateName(assertion, name);
        }
    };

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put(AssertionMetadata.SHORT_NAME, baseName);
        meta.put(AssertionMetadata.DESCRIPTION, "Gateway retrieves X.509 certificate credentials from context variable");
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"accessControl"});
        meta.put(AssertionMetadata.PALETTE_NODE_SORT_PRIORITY, -1000);
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.xmlsec.console.VariableCredentialSourceAssertionPropertiesDialog");

        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlsignature.gif");
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, policyNameFactory);

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }
}
