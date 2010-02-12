package com.l7tech.gateway.api;

import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.namespace.QName;
import java.util.List;
import java.util.Map;

/**
 * 
 */
@XmlRootElement(name="PolicyValidationResult")
@XmlType(name="PolicyValidationResultType", propOrder={"policyValidationMessages", "extensions"})
public class PolicyValidationResult extends ManagedObject {

    //- PUBLIC

    @XmlElementWrapper(name="ValidationMessages")
    @XmlElement(name="ValidationMessage")
    public List<PolicyValidationMessage> getPolicyValidationMessages() {
        return policyValidationMessages;
    }

    public void setPolicyValidationMessages( final List<PolicyValidationMessage> policyValidationMessages ) {
        this.policyValidationMessages = policyValidationMessages;
    }

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

        @XmlAttribute(name="level", required=true)
        public String getLevel() {
            return level;
        }

        public void setLevel( final String level ) {
            this.level = level;
        }

        @XmlAttribute(name="assertionOrdinal", required=true)
        public int getAssertionOrdinal() {
            return assertionOrdinal;
        }

        public void setAssertionOrdinal( final int assertionOrdinal ) {
            this.assertionOrdinal = assertionOrdinal;
        }

        @XmlElementWrapper(name="AssertionPath")
        @XmlElement(name="Assertion", required=true)
        public List<AssertionDetail> getAssertionDetails() {
            return assertionDetails;
        }

        public void setAssertionDetails( final List<AssertionDetail> assertionDetails ) {
            this.assertionDetails = assertionDetails;
        }

        @XmlElement(name="Message", required=true)
        public String getMessage() {
            return message;
        }

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

    @XmlType(name="AssertionDetailType")
    public static class AssertionDetail {
        private int position;
        private String description;
        private Map<QName,Object> attributeExtensions;

        AssertionDetail() {            
        }

        @XmlAttribute(name="position")
        public int getPosition() {
            return position;
        }

        public void setPosition( final int position ) {
            this.position = position;
        }

        @XmlValue
        public String getDescription() {
            return description;
        }

        public void setDescription( final String description ) {
            this.description = description;
        }

        /**
         * TODO [steve] this does not work (due to jaxb bug with XmlValue annotation?) 
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

    private List<PolicyValidationMessage> policyValidationMessages;
}
