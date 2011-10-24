/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.security.saml;

import java.io.Serializable;

/**
 * Information necessary to <em>issue</em> an individual Attribute value within a SAML AttributeStatement.
 * Note that {@link com.l7tech.policy.assertion.xmlsec.SamlAttributeStatement.Attribute} is very similar, but is used on
 * the <em>validation</em> side.  TODO merge these two classes!
 *
 * This class is used as configuration for the SamlAssertionGeneratorSaml classes.
 * @author alex
*/
public final class Attribute implements Serializable {
    private final String name;
    /**
     * Pre 2.0 this is the namespace attribute, 2.0 it's the name format attribute.
     */
    private final String namespace;
    private final Object value;

    /**
     * Constructor which allows any type of value. toString will be called for all types apart from Element and Message.
     * Type <code>List</code> is supported as a value to allow for mixed content.
     *
     * @param name The value of the saml:Attribute's Name or AttributeName attribute, depending on saml version.
     * @param nameSpaceOrNameFormat The value of the NameFormat or NameSpace attribute, depending on saml version.
     * @param value The supported AttributeValue value.
     */
    public Attribute(String name, String nameSpaceOrNameFormat, Object value) {
        this.name = name;
        this.namespace = nameSpaceOrNameFormat;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getNamespace() {
        return namespace;
    }

    public Object getValue() {
        return value;
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
