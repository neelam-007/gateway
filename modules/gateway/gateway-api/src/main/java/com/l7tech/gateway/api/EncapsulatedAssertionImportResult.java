package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.AttributeExtensibleType;
import com.l7tech.gateway.api.impl.ElementExtendableManagedObject;
import com.l7tech.gateway.api.impl.EncassImportContext;
import com.l7tech.util.Functions;

import javax.xml.bind.annotation.*;
import java.util.List;

import static com.l7tech.gateway.api.impl.AttributeExtensibleType.get;
import static com.l7tech.gateway.api.impl.AttributeExtensibleType.set;

/**
 * The results of importing a encapsulated assertion.
 */
@XmlRootElement(name="EncapsulatedAssertionImportResult")
@XmlType(name="EncapsulatedAssertionImportResultType", propOrder={"warningsValue", "importedEncapsulatedAssertion", "importedPolicyReferences", "extension", "extensions"})
@XmlSeeAlso( EncassImportContext.class)
public class EncapsulatedAssertionImportResult extends ElementExtendableManagedObject {
    //- PUBLIC

    /**
     * The warnings generated during the import.
     *
     * @return The warnings or null.
     */
    public List<String> getWarnings() {
        return warnings==null ? null : Functions.map(warnings, new Functions.Unary<String,AttributeExtensibleType.AttributeExtensibleString>() {
            @Override
            public String call(final AttributeExtensibleType.AttributeExtensibleString attributeExtensibleString) {
                return get(attributeExtensibleString);
            }
        });
    }

    /**
     * Set the warnings generated during the import.
     *
     * @param warnings The warnings to use.
     */
    public void setWarnings( final List<String> warnings ) {
        this.warnings = warnings == null ? null : Functions.map( warnings, new Functions.Unary<AttributeExtensibleType.AttributeExtensibleString,String>(){
            @Override
            public AttributeExtensibleType.AttributeExtensibleString call( final String s ) {
                return set(null, s);
            }
        } );
    }

    /**
     * Get the imported policy references.
     *
     * @return The imported policy references or null.
     */
    @XmlElementWrapper(name="ImportedPolicyReferences")
    @XmlElement(name="ImportedPolicyReference", required=true)
    public List<PolicyImportResult.ImportedPolicyReference> getImportedPolicyReferences() {
        return importedPolicyReferences;
    }

    /**
     * Set the imported policy references.
     *
     * @param importedPolicyReferences The references to use.
     */
    public void setImportedPolicyReferences( final List<PolicyImportResult.ImportedPolicyReference> importedPolicyReferences ) {
        this.importedPolicyReferences = importedPolicyReferences;
    }


    @XmlElement(name="ImportedEncapsulatedAssertion", required=true)
    public EncapsulatedAssertionMO getImportedEncapsulatedAssertion() {
        return importedEncapsulatedAssertion;
    }

    /**
     * Set the imported encapsulated assertion.
     *
     * @param importedEncapsulatedAssertion The references to use.
     */
    public void setImportedEncapsulatedAssertion( final EncapsulatedAssertionMO importedEncapsulatedAssertion ) {
        this.importedEncapsulatedAssertion = importedEncapsulatedAssertion;
    }

    //- PROTECTED

    @XmlElementWrapper(name="Warnings")
    @XmlElement(name="Warning", required=true)
    protected List<AttributeExtensibleType.AttributeExtensibleString> getWarningsValue() {
        return warnings;
    }

    protected void setWarningsValue( final List<AttributeExtensibleType.AttributeExtensibleString> warnings ) {
        this.warnings = warnings;
    }

    //- PACKAGE

    EncapsulatedAssertionImportResult() {
    }

    //- PRIVATE

    private List<AttributeExtensibleType.AttributeExtensibleString> warnings;
    private List<PolicyImportResult.ImportedPolicyReference> importedPolicyReferences;
    private EncapsulatedAssertionMO importedEncapsulatedAssertion;

}
