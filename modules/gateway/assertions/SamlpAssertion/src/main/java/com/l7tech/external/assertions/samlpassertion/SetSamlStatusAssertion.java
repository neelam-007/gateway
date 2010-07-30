package com.l7tech.external.assertions.samlpassertion;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.AssertionNodeNameFactory;
import com.l7tech.policy.assertion.AssertionUtils;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.SetsVariables;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.wsp.Java5EnumTypeMapping;
import com.l7tech.policy.wsp.SimpleTypeMappingFinder;
import com.l7tech.policy.wsp.TypeMapping;

import java.util.Arrays;

import static com.l7tech.policy.assertion.AssertionMetadata.PALETTE_NODE_ICON;

/**
 * Set SAML Status assertion
 */
public class SetSamlStatusAssertion extends Assertion implements SetsVariables {

    //- PUBLIC

    public String getVariableName() {
        return variableName;
    }

    public void setVariableName( final String variableName ) {
        this.variableName = variableName;
    }

    public SamlStatus getSamlStatus() {
        return samlStatus;
    }

    public void setSamlStatus( final SamlStatus samlStatus ) {
        this.samlStatus = samlStatus;
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "xml" });

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, baseName);
        meta.put(AssertionMetadata.DESCRIPTION, "Set SAML Web SSO Response Status.");
        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/check16.gif");
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "SAML Web SSO Response Status Properties");
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.samlpassertion.console.SetSamlStatusPropertiesDialog");
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, new AssertionNodeNameFactory<SetSamlStatusAssertion>(){
            @Override
            public String getAssertionName( final SetSamlStatusAssertion assertion, boolean decorate) {
                if(!decorate) return baseName;
                StringBuilder sb = new StringBuilder(baseName);
                sb.append( ": ${" );
                sb.append( assertion.getVariableName() );
                sb.append( "}" );
                return AssertionUtils.decorateName(assertion, sb);
            }
        });

        meta.put(AssertionMetadata.WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder( Arrays.<TypeMapping>asList(
            new Java5EnumTypeMapping( SamlStatus.class, "samlStatus")
        )));
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "set:modularAssertions"); //TODO [steve] licensing

        return meta;
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        return new VariableMetadata[] { new VariableMetadata(variableName, false, false, null, false, DataType.STRING) };
    }

    //- PRIVATE

    private static final String baseName = "Set SAML Web SSO Response Status ";

    private String variableName = "responseStatus";
    private SamlStatus samlStatus;
}
