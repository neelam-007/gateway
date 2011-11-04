package com.l7tech.policy.assertion.xmlsec;

import java.io.Serializable;

/**
 * The <code>SamlAttributeStatementAssertion</code> assertion describes
 * the SAML Attribute Statement constraints.
 */
public class SamlAttributeStatement implements Cloneable, Serializable {
   private static final long serialVersionUID = 1L;

    private Attribute[] attributes = new Attribute[]{};

    public Attribute[] getAttributes() {
        return attributes;
    }

    public SamlAttributeStatement() {
    }

    public SamlAttributeStatement(Attribute... attributes) {
        this.attributes = attributes;
    }

    public void setAttributes(Attribute[] attributes) {
        if (attributes == null) {
            this.attributes = new Attribute[]{};

        } else {
            this.attributes = attributes;
        }
    }

    public Object clone() {
        try {
            SamlAttributeStatement copy = (SamlAttributeStatement) super.clone();
            Attribute[] copyAttributes = copy.getAttributes();
            if (copyAttributes != null) {
                for (int i = 0; i < copyAttributes.length; i++) {
                    copyAttributes[i] = (Attribute) copyAttributes[i].clone(); 
                }
            }
            return copy;
        }
        catch(CloneNotSupportedException cnse) {
            throw new RuntimeException("Clone error");
        }
    }

    /**
     * This class is almost identical to {@link com.l7tech.security.saml.Attribute}, but this one is used on the
     * validation side, whereas the other is used on the issuing side.  TODO merge these two classes!
     */
    public static class Attribute implements Cloneable, Serializable {
        private static final long serialVersionUID = 1L;

        private String name;
        private String namespace;
        private String nameFormat;
        private String value;
        private boolean anyValue;
        private boolean repeatIfMulti;

        // additional Value for SAMLP
        private String friendlyName;

        public Attribute() {
        }

        public Attribute(String name, String namespace, String nameFormat, String value, boolean anyValue, boolean repeatIfMulti) {
            if (name == null || (value == null && !anyValue)) {
                throw new IllegalArgumentException();
            }
            this.name = name;
            this.namespace = namespace;
            this.nameFormat = nameFormat;
            this.value = value;
            this.anyValue = anyValue;
            this.repeatIfMulti = repeatIfMulti;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getNamespace() {
            return namespace;
        }

        public void setNamespace(String namespace) {
            this.namespace = namespace;
        }

        public String getNameFormat() {
            return nameFormat;
        }

        public void setNameFormat(String nameFormat) {
            this.nameFormat = nameFormat;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public boolean isAnyValue() {
            return anyValue;
        }

        public void setAnyValue(boolean anyValue) {
            this.anyValue = anyValue;
        }

        public boolean isRepeatIfMulti() {
            return repeatIfMulti;
        }

        public void setRepeatIfMulti(boolean repeatIfMulti) {
            this.repeatIfMulti = repeatIfMulti;
        }

        public String getFriendlyName() {
            return friendlyName;
        }

        public void setFriendlyName(String friendlyName) {
            this.friendlyName = friendlyName;
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("[ ")
              .append("namespace="+ (namespace == null ? "null" : namespace))
              .append(", nameFormat="+ (nameFormat == null ? "null" : nameFormat))
              .append(", name="+ (name == null ? "null" : name))
              .append(", value="+ (value == null ? "null" : value))
              .append(", anyValue=" + anyValue)
              .append(", repeatIfMulti=" + repeatIfMulti);
            if (friendlyName != null) {
                sb.append("friendlyName=").append(friendlyName).append("]");
            } else {
                sb.append(" ]");
            }
            return sb.toString();
        }

        public Object clone() {
            try {
                return super.clone();
            }
            catch(CloneNotSupportedException cnse) {
                throw new RuntimeException("Clone error");
            }
        }
    }

}
