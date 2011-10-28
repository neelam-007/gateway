/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.security.saml;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

/**
 * Information necessary to <em>issue</em> an individual Attribute value within a SAML AttributeStatement.
 * Note that {@link com.l7tech.policy.assertion.xmlsec.SamlAttributeStatement.Attribute} is very similar, but is used on
 * the <em>validation</em> side.
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
    private final NullBehavior nullBehavior;

    public enum NullBehavior{
        /**
         * When null, do not add an AttributeValue element.
         */
        NO_ATTRIBUTE_VALUE,
        /**
         * When null, add an AttributeValue with a null type.
         */
        NULL_TYPE
    }

    public Attribute(@NotNull final String name,
                     @NotNull final String nameSpaceOrNameFormat,
                     @Nullable final Object value) {
        this(name, nameSpaceOrNameFormat, value, NullBehavior.NULL_TYPE);
    }

    /**
     * Constructor which allows any type of value. toString will be called for all types apart from Element and Message.
     * Type <code>List</code> is supported as a value to allow for mixed content.
     *
     * @param name The value of the saml:Attribute's Name or AttributeName attribute, depending on saml version.
     * @param nameSpaceOrNameFormat The value of the NameFormat or NameSpace attribute, depending on saml version.
     * @param value The supported AttributeValue value. May be null. If so nullBehavior parameter is consulted for behavior.
     * @param nullBehavior behavior for AttributeValue element when the value is null.
     */
    public Attribute(String name, String nameSpaceOrNameFormat, @Nullable Object value, NullBehavior nullBehavior) {
        this.name = name;
        this.namespace = nameSpaceOrNameFormat;
        this.value = value;
        this.nullBehavior = nullBehavior;
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

    public NullBehavior getNullBehavior() {
        return nullBehavior;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Attribute attribute = (Attribute) o;

        if (!name.equals(attribute.name)) return false;
        if (!namespace.equals(attribute.namespace)) return false;
        if (nullBehavior != attribute.nullBehavior) return false;
        if (value != null ? !value.equals(attribute.value) : attribute.value != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + namespace.hashCode();
        result = 31 * result + (value != null ? value.hashCode() : 0);
        result = 31 * result + nullBehavior.hashCode();
        return result;
    }
}
