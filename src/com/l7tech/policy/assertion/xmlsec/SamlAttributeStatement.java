package com.l7tech.policy.assertion.xmlsec;

/**
 * The <code>SamlAttributeStatementAssertion</code> assertion describes
 * the SAML Attribute Statement constraints.
 */
public class SamlAttributeStatement extends SamlStatementAssertion {
    private Attribute[] attributes = new Attribute[]{};

    public Attribute[] getAttributes() {
        return attributes;
    }

    public void setAttributes(Attribute[] attributes) {
        if (attributes == null) {
            this.attributes = new Attribute[]{};

        } else {
            this.attributes = attributes;
        }
    }

    public static class Attribute {
        private String name;
        private String namespace;
        private String value;

        public Attribute() {
        }

        public Attribute(String name, String namespace, String value) {
            if (name == null || value == null) {
                throw new IllegalArgumentException();
            }
            this.name = name;
            this.namespace = namespace;
            this.value = value;
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

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

    }
}
