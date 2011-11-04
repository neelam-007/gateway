package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.util.Option;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

/**
 * The <code>SamlAttributeStatement</code> assertion describes
 * the SAML Attribute Statement constraints.
 *
 * This bean stores Attribute configuration for various purposes:
 * Validating an AttributeStatement in Require SAML Token
 * Validating a protocol response in SamlpAssertion.
 * Note: these use cases have the same core functionality except they differ on the location of where a SAML
 * token should be found.
 */
public class SamlAttributeStatement implements Cloneable, Serializable {
   private static final long serialVersionUID = 1L;

    private Attribute[] attributes = new Attribute[]{};

    /**
     * An expression that may reference any number of variables. Only variables of types 'Element' or 'Message' are
     * considered. Schema type should be an SAML Attribute. Used in the issuing use cases, where the set of
     * Attributes issued may need to be filtered by elements from an Attribute Query request.
     */
    private String filterExpression = "";

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

    public String getFilterExpression() {
        return filterExpression;
    }

    public void setFilterExpression(String filterExpression) {
        this.filterExpression = filterExpression;
    }

    @Override
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
            StringBuilder sb = new StringBuilder();
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

        @Override
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
