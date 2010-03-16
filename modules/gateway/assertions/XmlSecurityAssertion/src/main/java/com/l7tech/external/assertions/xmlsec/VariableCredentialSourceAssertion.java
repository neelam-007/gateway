package com.l7tech.external.assertions.xmlsec;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.validator.ValidatorFlag;
import com.l7tech.util.Functions;

import java.util.*;

/**
 * An assertion that can gather arbitrary context variables as credentials, if they are X509Certificates
 * or SecurityTokens.
 */
public class VariableCredentialSourceAssertion extends MessageTargetableAssertion {
    private static final String META_INITIALIZED = VariableCredentialSourceAssertion.class.getName() + ".metadataInitialized";

    private String variableName;

    public VariableCredentialSourceAssertion() {
        super(TargetMessageType.REQUEST, false); // Changing a Message's authentication context doesn't count as changing a Message variable, even though there's a 1-1 mapping
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

    private final static String baseName = "Retrieve Credentials from Context Variable";

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<VariableCredentialSourceAssertion>(){
        @Override
        public String getAssertionName( final VariableCredentialSourceAssertion assertion, final boolean decorate) {
            if(!decorate) return baseName;
            StringBuilder name = new StringBuilder(baseName + ": ");
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
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "Credentials from Context Variable Properties");

        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlsignature.gif");
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, policyNameFactory);

        meta.put(AssertionMetadata.POLICY_VALIDATOR_CLASSNAME, VariableCredentialSourceValidator.class.getName());
        meta.put(AssertionMetadata.POLICY_VALIDATOR_FLAGS_FACTORY, new Functions.Unary<Set<ValidatorFlag>, VariableCredentialSourceAssertion>() {
            @Override
            public Set<ValidatorFlag> call(VariableCredentialSourceAssertion assertion) {
                return assertion == null ? EnumSet.noneOf(ValidatorFlag.class) : EnumSet.of(ValidatorFlag.GATHERS_X509_CREDENTIALS);
            }
        });
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }
}
