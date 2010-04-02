package com.l7tech.gateway.api;

import static com.l7tech.gateway.api.impl.AttributeExtensibleType.*;

import com.l7tech.gateway.api.impl.Extension;
import com.l7tech.gateway.api.impl.PolicyImportContext;
import com.l7tech.util.Functions;

import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;
import java.util.List;
import java.util.Map;

/**
 * The results of importing a policy document.
 */
@XmlRootElement(name="PolicyImportResult")
@XmlType(name="PolicyImportResultType", propOrder={"warningsValue", "importedPolicyReferences", "extension", "extensions"})
@XmlSeeAlso( PolicyImportContext.class)
public class PolicyImportResult extends ManagedObject {

    //- PUBLIC

    /**
     * The warnings generated during the import.
     *
     * @return The warnings or null.
     */
    @XmlTransient
    public List<String> getWarnings() {
        return warnings==null ? null : Functions.map( warnings, new Functions.Unary<String,AttributeExtensibleString>(){
            @Override
            public String call( final AttributeExtensibleString attributeExtensibleString ) {
                return get(attributeExtensibleString);
            }
        } );
    }

    /**
     * Set the warnings generated during the import.
     *
     * @param warnings The warnings to use.
     */
    public void setWarnings( final List<String> warnings ) {
        this.warnings = warnings == null ? null : Functions.map( warnings, new Functions.Unary<AttributeExtensibleString,String>(){
            @Override
            public AttributeExtensibleString call( final String s ) {
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
    public List<ImportedPolicyReference> getImportedPolicyReferences() {
        return importedPolicyReferences;
    }

    /**
     * Set the imported policy references.
     *
     * @param importedPolicyReferences The references to use.
     */
    public void setImportedPolicyReferences( final List<ImportedPolicyReference> importedPolicyReferences ) {
        this.importedPolicyReferences = importedPolicyReferences;
    }

    /**
     * Description of an imported policy reference.
     *
     * <p>Any automatically created or mapped policy dependencies are included
     * in the policy import result.</p>
     */
    @XmlType(name="ImportedPolicyReferenceType")
    public static class ImportedPolicyReference {
        private ImportedPolicyReferenceType type;
        private String referenceType;
        private String referenceId;
        private String id;
        private String guid;
        private List<Object> extensions;
        private Map<QName,Object> attributeExtensions;

        ImportedPolicyReference(){            
        }

        /**
         * Get the type of imported policy reference (required)
         *
         * @return The type or null
         */
        @XmlAttribute(name="type", required=true)
        public ImportedPolicyReferenceType getType() {
            return type;
        }

        /**
         * Set the type of the imported policy reference.
         *
         * @param type The type to use.
         */
        public void setType( final ImportedPolicyReferenceType type ) {
            this.type = type;
        }

        /**
         * Get the type of the referenced dependency (required)
         *
         * @return The dependency type or null
         */
        @XmlAttribute(name="referenceType", required=true)
        public String getReferenceType() {
            return referenceType;
        }

        /**
         * Set the type of the referenced dependency.
         *
         * @param referenceType The dependency type.
         */
        public void setReferenceType( final String referenceType ) {
            this.referenceType = referenceType;
        }

        /**
         * Get the identifier of the referenced dependency (required)
         *
         * <p>This identifier is a value from the policy export document.</p>
         *
         * @return The dependency identifier or null.
         */
        @XmlAttribute(name="referenceId", required=true)
        public String getReferenceId() {
            return referenceId;
        }

        /**
         * Set the identifier of the referenced dependency.
         *
         * @param referenceId The dependency identifier to use.
         */
        public void setReferenceId( final String referenceId ) {
            this.referenceId = referenceId;
        }

        /**
         * Get the identifier of the dependency on the target Gateway (required)
         *
         * @return The identifier or null.
         */
        @XmlAttribute(name="id", required=true)
        public String getId() {
            return id;
        }

        /**
         * Set the identifier of the dependency on the target Gateway.
         *
         * @param id The identifier to use.
         */
        public void setId( final String id ) {
            this.id = id;
        }

        /**
         * Get the GUID of the dependency on the target Gateway.
         *
         * @return The GUID or null.
         */
        @XmlAttribute(name="guid")
        public String getGuid() {
            return guid;
        }

        /**
         * Set the GUID of the dependency on the target Gateway.
         *
         * @param guid The GUID to use.
         */
        public void setGuid( final String guid ) {
            this.guid = guid;
        }

        @XmlAnyAttribute
        protected Map<QName, Object> getAttributeExtensions() {
            return attributeExtensions;
        }

        protected void setAttributeExtensions( final Map<QName, Object> attributeExtensions ) {
            this.attributeExtensions = attributeExtensions;
        }

        @XmlAnyElement(lax=true)
        protected List<Object> getExtensions() {
            return extensions;
        }

        protected void setExtensions( final List<Object> extensions ) {
            this.extensions = extensions;
        }
    }

    /**
     * Type for imported policy references.
     */
    @XmlEnum(String.class)
    @XmlType(name="ImportedPolicyReferenceTypeType")
    public enum ImportedPolicyReferenceType {
        /**
         * The referenced dependency was created during the import.
         */
        @XmlEnumValue("Created") CREATED,

        /**
         * The referenced dependency was automatically mapped to an alternative on the target Gateway.
         */
        @XmlEnumValue("Mapped") MAPPED
    }

    //- PROTECTED

    @XmlElementWrapper(name="Warnings")
    @XmlElement(name="Warning", required=true)
    protected List<AttributeExtensibleString> getWarningsValue() {
        return warnings;
    }

    protected void setWarningsValue( final List<AttributeExtensibleString> warnings ) {
        this.warnings = warnings;
    }

    @XmlElement(name="Extension")
    @Override
    protected Extension getExtension() {
        return super.getExtension();
    }

    @Override
    protected void setExtension( final Extension extension ) {
        super.setExtension( extension );
    }

    @XmlAnyElement(lax=true)
    @Override
    protected List<Object> getExtensions() {
        return super.getExtensions();
    }

    @Override
    protected void setExtensions( final List<Object> extensions ) {
        super.setExtensions( extensions );
    }

    //- PACKAGE

    PolicyImportResult() {        
    }

    //- PRIVATE

    private List<AttributeExtensibleString> warnings;
    private List<ImportedPolicyReference> importedPolicyReferences;

}
