/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.variable;

import java.io.Serializable;

/**
 * Metadata describing a variable.  Applies mainly to built-in variables at this time.
 */
public class VariableMetadata implements Serializable {
    private final String name;
    private final boolean prefixed;
    private final boolean multivalued;
    private final String canonicalName;
    private final boolean settable;
    private final DataType type;

    public VariableMetadata(String name, boolean prefixed, boolean multivalued, String canonicalName, boolean settable, DataType type) {
        ExpandVariables.validateName(name);
        this.name = name;
        this.prefixed = prefixed;
        this.multivalued = multivalued;
        this.canonicalName = canonicalName == null ? name : canonicalName;
        this.settable = settable;
        this.type = type;
    }

    public VariableMetadata(String name, boolean prefixed, boolean multivalued, String canonicalName, boolean settable) {
        this(name, prefixed, multivalued, canonicalName, settable, DataType.STRING);
    }

    public VariableMetadata(String name) {
        this(name, false, false, null, false);
    }

    /**
     * The name of the variable. If {@link #isPrefixed} is true, this name is just a prefix:
     * the names of the actual variables are accessible using this prefix followed by a period
     * and the sub-name.
     * <p>
     * For example, this method will return <code>request.http.header</code>, but when you're looking for
     * the Host header, you should ask for <code>request.http.header.host</code>.
     * @return the name of the variable, or the prefix used in constructing names if {@link #isPrefixed()} is true.
     */
    public String getName() {
        return name;
    }

    /**
     * True if this variable is really a prefixed "meta-variable".  For example, there is no
     * such variable as "request.http.header", but "request.http.header.host" is likely to work
     * for HTTP requests.
     * @return true if this variable's names are prefixed, false otherwise.
     */
    public boolean isPrefixed() {
        return prefixed;
    }

    /**
     * @return true if this variable returns multiple values, false otherwise.
     */
    public boolean isMultivalued() {
        return multivalued;
    }

    /**
     * @return the canonical name for this variable (used for finding this variable's description in a properties file).
     */
    public String getCanonicalName() {
        return canonicalName;
    }

    /**
     * @return true if this variable is settable, false otherwise.
     */
    public boolean isSettable() {
        return settable;
    }

    /**
     * @return the declared type of this variable (may be {@link DataType#UNKNOWN})
     */
    public DataType getType() {
        return type;
    }

    public String toString() {
        if (prefixed) {
            return name + ".*";
        } else {
            return name;
        }
    }
}
