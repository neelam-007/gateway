package com.l7tech.gateway.api;

import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;
import java.util.List;
import java.util.Map;

/**
 * 
 */
public interface PolicyAccessor<MO extends ManagedObject> extends Accessor<MO> {

    /**
     *
     */
    String exportPolicy( String identifier ) throws AccessorException;

    /**
     * Import a previously exported policy.
     *
     * @param identifier The identifier for the policy import target
     * @param properties The properties associated with the policy import
     * @param export The policy export to be imported
     * @param instructions The import instructions
     * @return The results of the policy import.
     */
    PolicyImportResult importPolicy( String identifier, Map<String, Object> properties, String export, List<PolicyReferenceInstruction> instructions ) throws AccessorException;

    /**
     *
     * @see PolicyDetail#getProperties()
     * @see ServiceDetail#getProperties()
     */
    PolicyValidationResult validatePolicy( MO item ) throws AccessorException;

    @XmlType(name="PolicyReferenceInstructionType", propOrder="extensions")
    class PolicyReferenceInstruction {
        private PolicyReferenceInstructionType policyReferenceInstructionType;
        private String referenceType;
        private String referenceId;
        private String mappedReferenceId;
        private List<Object> extensions;
        private Map<QName,Object> attributeExtensions;

        PolicyReferenceInstruction() {
        }

        @XmlAttribute(name="type", required=true)
        public PolicyReferenceInstructionType getPolicyReferenceInstructionType() {
            return policyReferenceInstructionType;
        }

        public void setPolicyReferenceInstructionType( final PolicyReferenceInstructionType policyReferenceInstructionType ) {
            this.policyReferenceInstructionType = policyReferenceInstructionType;
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

        @XmlAttribute(name="mappedReferenceId")
        public String getMappedReferenceId() {
            return mappedReferenceId;
        }

        public void setMappedReferenceId( final String mappedReferenceId ) {
            this.mappedReferenceId = mappedReferenceId;
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

    @XmlEnum(String.class)
    public enum PolicyReferenceInstructionType {
        @XmlEnumValue("Delete") DELETE,
        @XmlEnumValue("Ignore") IGNORE,
        @XmlEnumValue("Map") MAP
    }

}
