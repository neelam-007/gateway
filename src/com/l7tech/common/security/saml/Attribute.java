/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.common.security.saml;

import java.io.Serializable;

/**
 * Information necessary to <em>issue</em> an individual Attribute value within a SAML AttributeStatement.
 * Note that {@link com.l7tech.policy.assertion.xmlsec.SamlAttributeStatement.Attribute} is very similar, but is used on
 * the <em>validation</em> side.  TODO merge these two classes!
 * @author alex
*/
public class Attribute implements Serializable {
    private String name;
    private String namespace;
    private String value;

    public Attribute() { }

    public Attribute(String name, String nameSpace, String value) {
        this.name = name;
        this.namespace = nameSpace;
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

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Attribute attribute = (Attribute) o;

        if (name != null ? !name.equals(attribute.name) : attribute.name != null) return false;
        if (namespace != null ? !namespace.equals(attribute.namespace) : attribute.namespace != null) return false;
        if (value != null ? !value.equals(attribute.value) : attribute.value != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (name != null ? name.hashCode() : 0);
        result = 31 * result + (namespace != null ? namespace.hashCode() : 0);
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }
}
