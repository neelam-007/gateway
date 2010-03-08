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
 * Policy reference instructions are used to resolve policy dependency
 * issues when importing policies.
 *
 * @see ManagedObjectFactory#createPolicyReferenceInstruction()
 */
@XmlType(name="PolicyReferenceInstructionType", propOrder="extensions")
public class PolicyReferenceInstruction {

    //- PUBLIC

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

    /**
     * Types for policy reference instructions.
     */
    @XmlEnum(String.class)
    public static enum PolicyReferenceInstructionType {
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

    //- PROTECTED

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

    //- PACKAGE

    PolicyReferenceInstruction() {
    }

    //- PRIVATE

    private PolicyReferenceInstructionType policyReferenceInstructionType;
    private String referenceType;
    private String referenceId;
    private String mappedReferenceId;
    private String mappedName;
    private List<Object> extensions;
    private Map<QName,Object> attributeExtensions;

}
