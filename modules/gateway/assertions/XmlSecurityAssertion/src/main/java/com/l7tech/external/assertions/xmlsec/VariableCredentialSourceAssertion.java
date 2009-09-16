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

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put(AssertionMetadata.SHORT_NAME, "Variable Credential Source");
        meta.put(AssertionMetadata.DESCRIPTION, "Gateway retrieves X.509 certificate credentials from context variable");
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"accessControl"});
        meta.put(AssertionMetadata.PALETTE_NODE_SORT_PRIORITY, -1000);
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.xmlsec.console.VariableCredentialSourceAssertionPropertiesDialog");

        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlsignature.gif");
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, new Functions.Unary<String, VariableCredentialSourceAssertion>() {
            @Override
            public String call( final VariableCredentialSourceAssertion ass ) {
                StringBuilder name = new StringBuilder("Credentials from variable: ");
                name.append(ass.getVariableName() == null ? "<Not yet set>" : "${" + ass.getVariableName() + "}");
                return AssertionUtils.decorateName(ass, name);
            }
        });

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }
}
