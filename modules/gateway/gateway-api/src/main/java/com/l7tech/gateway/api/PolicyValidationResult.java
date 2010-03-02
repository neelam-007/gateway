package com.l7tech.gateway.api;

import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.namespace.QName;
import java.util.List;
import java.util.Map;

/**
 * Encapsulates the result of policy validation.
 */
@XmlRootElement(name="PolicyValidationResult")
@XmlType(name="PolicyValidationResultType", propOrder={"status", "policyValidationMessages", "extensions"})
public class PolicyValidationResult extends ManagedObject {

    //- PUBLIC

    /**
     * Get the policy validation status (required)
     *
     * @return The validation status or null
     */
    @XmlElement(name="ValidationStatus", required=true)
    public ValidationStatus getStatus() {
        return status;
    }

    /**
     * Set the policy validation status.
     *
     * @param status The status to use.
     */
    public void setStatus( final ValidationStatus status ) {
        this.status = status;
    }

    /**
     * Get the policy validation messages.
     *
     * @return The validation messages or null.
     */
    @XmlElementWrapper(name="ValidationMessages")
    @XmlElement(name="ValidationMessage", required=true)
    public List<PolicyValidationMessage> getPolicyValidationMessages() {
        return policyValidationMessages;
    }

    /**
     * Set the policy validation messages.
     *
     * @param policyValidationMessages The policy validation messages to use.
     */
    public void setPolicyValidationMessages( final List<PolicyValidationMessage> policyValidationMessages ) {
        this.policyValidationMessages = policyValidationMessages;
    }

    /**
     * Status for policy validation.
     */
    public enum ValidationStatus {
        /**
         * Policy validated without issue.
         */
        @XmlEnumValue("OK") OK,

        /**
         * Policy validated with warnings.
         */
        @XmlEnumValue("Warning") WARNING,

        /**
         * Policy validated with errors.
         */
        @XmlEnumValue("Error") ERROR
    }

    /**
     * Represents a policy validation message.
     *
     * <p>A validation message consists of a level, message and information to
     * identify the policy assertion to which the message relates.</p>
     */
    @XmlType(name="PolicyValidationMessageType", propOrder={"assertionDetails", "message", "extensions"})
    public static class PolicyValidationMessage {
        private String level;
        private int assertionOrdinal;
        private List<AssertionDetail> assertionDetails;
        private String message;
        private List<Object> extensions;
        private Map<QName,Object> attributeExtensions;

        PolicyValidationMessage() {
        }

        /**
         * Get the level for the message (required)
         *
         * @return The level or null.
         */
        @XmlAttribute(name="level", required=true)
        public String getLevel() {
            return level;
        }

        /**
         * Set the level for the message.
         *
         * @param level The level to use.
         */
        public void setLevel( final String level ) {
            this.level = level;
        }

        /**
         * Get the ordinal for the assertion (required)
         *
         * @return The assertion ordinal.
         */
        @XmlAttribute(name="assertionOrdinal", required=true)
        public int getAssertionOrdinal() {
            return assertionOrdinal;
        }

        /**
         * Set the ordinal for the assertion.
         *
         * @param assertionOrdinal The ordinal to use.
         */
        public void setAssertionOrdinal( final int assertionOrdinal ) {
            this.assertionOrdinal = assertionOrdinal;
        }

        /**
         * Get the path for the assertion (required)
         *
         * @return The path or null.
         */
        @XmlElementWrapper(name="AssertionPath", required=true)
        @XmlElement(name="Assertion", required=true)
        public List<AssertionDetail> getAssertionDetails() {
            return assertionDetails;
        }

        /**
         * Set the path for the assertion.
         *
         * @param assertionDetails The path to use.
         */
        public void setAssertionDetails( final List<AssertionDetail> assertionDetails ) {
            this.assertionDetails = assertionDetails;
        }

        /**
         * Get the message (required)
         *
         * @return The message or null.
         */
        @XmlElement(name="Message", required=true)
        public String getMessage() {
            return message;
        }

        /**
         * Set the message.
         *
         * @param message The message to use.
         */
        public void setMessage( final String message ) {
            this.message = message;
        }

        @SuppressWarnings({ "RedundantIfStatement" })
        @Override
        public boolean equals( final Object o ) {
            if ( this == o ) return true;
            if ( o == null || getClass() != o.getClass() ) return false;

            final PolicyValidationMessage that = (PolicyValidationMessage) o;

            if ( assertionOrdinal != that.assertionOrdinal ) return false;
            if ( level != null ? !level.equals( that.level ) : that.level != null ) return false;
            if ( message != null ? !message.equals( that.message ) : that.message != null ) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = level != null ? level.hashCode() : 0;
            result = 31 * result + assertionOrdinal;
            result = 31 * result + (message != null ? message.hashCode() : 0);
            return result;
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
     * Details for an assertions location relative to it's parent.
     */
    @XmlType(name="AssertionDetailType")
    public static class AssertionDetail {
        private int position;
        private String description;
        private Map<QName,Object> attributeExtensions;

        AssertionDetail() {            
        }

        /**
         * Get the position for the assertion (required)
         *
         * @return The position.
         */
        @XmlAttribute(name="position", required=true)
        public int getPosition() {
            return position;
        }

        /**
         * Set the position for the assertion.
         *
         * @param position The position to use.
         */
        public void setPosition( final int position ) {
            this.position = position;
        }

        /**
         * Get the description for the assertion.
         *
         * @return The assertion description or null.
         */
        @XmlValue
        public String getDescription() {
            return description;
        }

        /**
         * Set the description for the assertion.
         *
         * @param description The description to use.
         */
        public void setDescription( final String description ) {
            this.description = description;
        }

        /**
         * NOTE this does not work (related JAXB bug https://jaxb.dev.java.net/issues/show_bug.cgi?id=738) 
         */
        @XmlAnyAttribute
        protected Map<QName, Object> getAttributeExtensions() {
            return attributeExtensions;
        }

        protected void setAttributeExtensions( final Map<QName, Object> attributeExtensions ) {
            this.attributeExtensions = attributeExtensions;
        }
    }

    //- PROTECTED

    @Override
    @XmlAnyElement(lax=true)
    protected List<Object> getExtensions() {
        return super.getExtensions();
    }

    @Override
    protected void setExtensions( final List<Object> extensions ) {
        super.setExtensions( extensions );
    }

    //- PACKAGE

    PolicyValidationResult() {
    }

    //- PRIVATE

    private ValidationStatus status;
    private List<PolicyValidationMessage> policyValidationMessages;
}
