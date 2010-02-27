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
 * Extension of the base Accessor interface with Policy support.
 *
 * @see AccessorRuntimeException <code>AccessorRuntimeException</code> Which may be thrown by methods of this interface.
 */
public interface PolicyAccessor<AO extends AccessibleObject> extends Accessor<AO> {

    /**
     * Get the policy for the identified resource.
     *
     * @param identifier The identifier for the resource.
     * @return The policy resource
     * @throws AccessorException If an error occurs
     */
    Resource getPolicy( String identifier ) throws AccessorException;

    /**
     * Set the policy for the identified resource.
     *
     * @param identifier The identifier for the resource.
     * @param resource The policy resource
     * @throws AccessorException If an error occurs
     */
    void putPolicy( String identifier, Resource resource ) throws AccessorException;

    /**
     * Export the policy for the identified resource.
     *
     * @param identifier The identifier for the resource.
     * @return The policy export XML
     * @throws AccessorException If an error occurs
     */
    String exportPolicy( String identifier ) throws AccessorException;

    /**
     * Import a previously exported policy.
     *
     * @param identifier The identifier for the policy import target
     * @param properties The properties associated with the policy import
     * @param export The policy export XML to be imported
     * @param instructions The import instructions
     * @return The results of the policy import.
     * @throws AccessorException If an error occurs
     */
    PolicyImportResult importPolicy( String identifier,
                                     Map<String, Object> properties,
                                     String export,
                                     List<PolicyReferenceInstruction> instructions ) throws AccessorException;

    /**
     * Validate the given policy.
     *
     * @param item The managed object
     * @param resourceSets Additional resources to use when validating (optional)
     * @return The result of the policy validation
     * @throws AccessorException If an error occurs
     */
    PolicyValidationResult validatePolicy( AO item,
                                           List<ResourceSet> resourceSets ) throws AccessorException;

    /**
     * Validate the identified policy.
     *
     * @param identifier The identifier for the resource
     * @return The result of the policy validation
     * @throws AccessorException If an error occurs
     */
    PolicyValidationResult validatePolicy( String identifier ) throws AccessorException;

    /**
     * Policy reference instructions are used to resolve policy dependency
     * issues when importing policies.
     *
     * @see ManagedObjectFactory#createPolicyReferenceInstruction()
     */
    @XmlType(name="PolicyReferenceInstructionType", propOrder="extensions")
    class PolicyReferenceInstruction {
        private PolicyReferenceInstructionType policyReferenceInstructionType;
        private String referenceType;
        private String referenceId;
        private String mappedReferenceId;
        private String mappedName;
        private List<Object> extensions;
        private Map<QName,Object> attributeExtensions;

        PolicyReferenceInstruction() {
        }

        /**
         * Get the type for the instruction (required)
         *
         * @return The type or null if not set
         */
        @XmlAttribute(name="type", required=true)
        public PolicyReferenceInstructionType getPolicyReferenceInstructionType() {
            return policyReferenceInstructionType;
        }

        /**
         * Set the type for the instruction.
         *
         * @param policyReferenceInstructionType The type to use
         */
        public void setPolicyReferenceInstructionType( final PolicyReferenceInstructionType policyReferenceInstructionType ) {
            this.policyReferenceInstructionType = policyReferenceInstructionType;
        }

        /**
         * Get the reference type for the instruction (required)
         *
         * @return The reference type or null if not set
         */
        @XmlAttribute(name="referenceType", required=true)
        public String getReferenceType() {
            return referenceType;
        }

        /**
         * Set the reference type for the instruction.
         *
         * @param referenceType The reference type to use
         */
        public void setReferenceType( final String referenceType ) {
            this.referenceType = referenceType;
        }

        /**
         * Set the reference identifier for the instruction (required)
         *
         * @return The identifier or null if not set.
         */
        @XmlAttribute(name="referenceId", required=true)
        public String getReferenceId() {
            return referenceId;
        }

        /**
         * Set the reference identifier for the instruction.
         *
         * @param referenceId The reference identifier to use.
         */
        public void setReferenceId( final String referenceId ) {
            this.referenceId = referenceId;
        }

        /**
         * Get the mapped reference identifier for the instruction.
         *
         * <p>This is required if the instruction type is {@code MAP}</p>
         *
         * @return The mapped reference identifier or null if not set
         */
        @XmlAttribute(name="mappedReferenceId")
        public String getMappedReferenceId() {
            return mappedReferenceId;
        }

        /**
         * Set the mapped reference identifier for the instruction.
         *
         * @param mappedReferenceId The mapped reference identifier to use
         */
        public void setMappedReferenceId( final String mappedReferenceId ) {
            this.mappedReferenceId = mappedReferenceId;
        }

        /**
         * Get the mapped name for the instruction.
         *
         * <p>This is required if the instruction type is {@code RENAME}</p>
         *
         * @return The mapped name or null if not set
         */
        @XmlAttribute(name="mappedName")
        public String getMappedName() {
            return mappedName;
        }

        /**
         * Set the mapped name for the instruction.
         *
         * @param mappedName The mapped name to use.
         */
        public void setMappedName( final String mappedName ) {
            this.mappedName = mappedName;
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
     * Types for policy reference instructions.
     */
    @XmlEnum(String.class)
    public enum PolicyReferenceInstructionType {
        /**
         * Delete assertions that use the referenced resource.
         */
        @XmlEnumValue("Delete") DELETE,

        /**
         * Ignore (import as-is) assertions that use the referenced resource.
         */
        @XmlEnumValue("Ignore") IGNORE,

        /**
         * Replace the referenced resource with an alternative.
         */
        @XmlEnumValue("Map") MAP,

        /**
         * Rename the referenced resource
         */
        @XmlEnumValue("Rename") RENAME
    }
}
