package com.l7tech.policy.assertion.xmlsec;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import static com.l7tech.policy.assertion.AssertionMetadata.DESCRIPTION;
import static com.l7tech.policy.assertion.AssertionMetadata.PALETTE_FOLDERS;
import static com.l7tech.policy.assertion.AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME;
import static com.l7tech.policy.assertion.AssertionMetadata.SHORT_NAME;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.SetsVariables;
import com.l7tech.policy.assertion.UsesVariables;
import static com.l7tech.policy.assertion.VariableUseSupport.expressions;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;

/**
 * Assertion for trusted certificate lookup
 */
public class LookupTrustedCertificateAssertion extends Assertion implements SetsVariables, UsesVariables {

    private static final String META_INITIALIZED = LookupTrustedCertificateAssertion.class.getName() + ".metadataInitialized";
    private String trustedCertificateName;
    private boolean allowMultipleCertificates;
    private String variableName = "certificates";

    public LookupTrustedCertificateAssertion(){
    }

    public String getTrustedCertificateName() {
        return trustedCertificateName;
    }

    public void setTrustedCertificateName( final String trustedCertificateName ) {
        this.trustedCertificateName = trustedCertificateName;
    }

    public boolean isAllowMultipleCertificates() {
        return allowMultipleCertificates;
    }

    public void setAllowMultipleCertificates( final boolean allowMultipleCertificates ) {
        this.allowMultipleCertificates = allowMultipleCertificates;
    }

    public String getVariableName() {
        return variableName;
    }

    public void setVariableName( final String variableName ) {
        this.variableName = variableName;
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        return new VariableMetadata[]{ new VariableMetadata( variableName, false, true, variableName, true, DataType.CERTIFICATE ) };
    }

    @Override
    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    public String[] getVariablesUsed() {
        return expressions( trustedCertificateName ).asArray();
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put(SHORT_NAME, "Look Up Trusted Certificate");
        meta.put(DESCRIPTION, "Look up trusted certificates for later use in policy");
        meta.put(PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.console.panels.LookupTrustedCertificateAssertionPropertiesDialog");
        meta.put(PALETTE_FOLDERS, new String[] { "xmlSecurity" });
        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }
}
