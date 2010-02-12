package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.PolicyImportContext;

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
 * 
 */
@XmlRootElement(name="PolicyImportResult")
@XmlType(name="PolicyImportResultType", propOrder={"warnings", "importedPolicyReferences", "extensions"})
@XmlSeeAlso( PolicyImportContext.class)
public class PolicyImportResult extends ManagedObject {

    //- PUBLIC

    @XmlElementWrapper(name="Warnings")
    @XmlElement(name="Warning")
    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings( final List<String> warnings ) {
        this.warnings = warnings;
    }

    @XmlElementWrapper(name="ImportedPolicyReferences")
    @XmlElement(name="ImportedPolicyReference")
    public List<ImportedPolicyReference> getImportedPolicyReferences() {
        return importedPolicyReferences;
    }

    public void setImportedPolicyReferences( final List<ImportedPolicyReference> importedPolicyReferences ) {
        this.importedPolicyReferences = importedPolicyReferences;
    }

    @XmlType(name="ImportedPolicyReferenceType")
    public static class ImportedPolicyReference {
        private ImportedPolicyReferenceType type;
        private String referenceType;
        private String referenceId;
        private String id;
        private String guid;
        private String name;
        private List<Object> extensions;
        private Map<QName,Object> attributeExtensions;

        ImportedPolicyReference(){            
        }

        @XmlAttribute(name="type", required=true)
        public ImportedPolicyReferenceType getType() {
            return type;
        }

        public void setType( final ImportedPolicyReferenceType type ) {
            this.type = type;
        }

        @XmlAttribute(name="referenceType", required=true)
        public String getReferenceType() {
            return referenceType;
        }

        public void setReferenceType( final String referenceType ) {
            this.referenceType = referenceType;
        }

        @XmlAttribute(name="referenceId", required=true)
        public String getReferenceId() {
            return referenceId;
        }

        public void setReferenceId( final String referenceId ) {
            this.referenceId = referenceId;
        }

        @XmlAttribute(name="id", required=true)
        public String getId() {
            return id;
        }

        public void setId( final String id ) {
            this.id = id;
        }

        @XmlAttribute(name="guid")
        public String getGuid() {
            return guid;
        }

        public void setGuid( final String guid ) {
            this.guid = guid;
        }

        @XmlAttribute(name="name")
        public String getName() {
            return name;
        }

        public void setName( final String name ) {
            this.name = name;
        }

        @XmlAnyAttribute
        public Map<QName, Object> getAttributeExtensions() {
            return attributeExtensions;
        }

        public void setAttributeExtensions( final Map<QName, Object> attributeExtensions ) {
            this.attributeExtensions = attributeExtensions;
        }

        @XmlTransient
        public List<Object> getExtensions() {
            return extensions;
        }

        public void setExtensions( final List<Object> extensions ) {
            this.extensions = extensions;
        }
    }

    @XmlEnum(String.class)
    public enum ImportedPolicyReferenceType {
        @XmlEnumValue("Created") CREATED,
        @XmlEnumValue("Mapped") MAPPED
    }

    //- PROTECTED

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

    private List<String> warnings;
    private List<ImportedPolicyReference> importedPolicyReferences;

}
