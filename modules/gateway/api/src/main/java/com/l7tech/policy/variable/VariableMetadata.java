/**
 * Copyright (C) 2006-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.variable;

import java.io.Serializable;
import java.util.regex.Pattern;

/**
 * Metadata describing a variable.  Applies mainly to built-in variables at this time.
 */
public class VariableMetadata implements Serializable {
    private static final Pattern VARIABLE_NAME_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9._\\-]*$");

    /**
     * Currently the syntax pattern only permits array syntax, not multivalued since the multivalued delimiter can be changed ...
     */
    private static final Pattern VARIABLE_NAME_SYNTAX_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9._\\-]*(?:\\[\\d{1,7}\\])?$");

    private final String name;
    private final boolean prefixed;
    private final boolean multivalued;
    private final String canonicalName;
    private final boolean settable;
    private final DataType type;
    private final String replacedBy;

    public VariableMetadata(String name, boolean prefixed, boolean multivalued, String canonicalName, boolean settable, DataType type, String replacedBy)
        throws VariableNameSyntaxException
    {
        assertNameIsValid(name,false);
        this.name = name;
        this.prefixed = prefixed;
        this.multivalued = multivalued;
        this.canonicalName = canonicalName == null ? name : canonicalName;
        this.settable = settable;
        this.type = type;
        this.replacedBy = replacedBy;
    }

    public VariableMetadata(String name, boolean prefixed, boolean multivalued, String canonicalName, boolean settable, DataType type)
        throws VariableNameSyntaxException {
        this(name, prefixed, multivalued, canonicalName, settable, type, null);
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

    /**
     * @return true if the variable is deprecated (should trigger a warning in policies when used), false otherwise
     */
    public boolean isDeprecated() {
        return replacedBy != null;
    }

    /**
     * @return the name of the new variable that deprecates this variable
     */
    public String getReplacedBy() {
        return replacedBy;
    }

    public String toString() {
        if (prefixed) {
            return name + ".*";
        } else {
            return name;
        }
    }

    public static void assertNameIsValid(String name, boolean permitSyntax) throws VariableNameSyntaxException {
        if (!isNameValid(name, permitSyntax))
            throw new VariableNameSyntaxException("Variable name must start with a letter or underscore, and contains optional letters, digits, underscores, dashes, or periods.");
    }

    public static boolean isNameValid(String name) {
        return isNameValid( name, false );
    }

    /**
     * Check if the the given variable name is valid.
     *
     * @param name The name to check
     * @param permitSyntax True to allow syntax (e.g. an array index)
     * @return true if valid
     */
    public static boolean isNameValid(String name, boolean permitSyntax) {
        return permitSyntax?
                VARIABLE_NAME_SYNTAX_PATTERN.matcher( name ).matches() :
                VARIABLE_NAME_PATTERN.matcher(name).matches();
    }

    /**
     * Check if the specified name is valid as a context variable name.
     *
     * @param name the name to examine.  Required.
     * @return an error message describing why the specified name is not valid, or null if the name is valid.
     */
    public static String validateName(String name) {
        return validateName( name, false );
    }

    /**
     * Check if the specified name is valid as a context variable name.
     *
     * @param name the name to examine.  Required.
     * @param permitSyntax true to permit syntax (e.g. an array index)
     * @return an error message describing why the specified name is not valid, or null if the name is valid.
     */
    public static String validateName(String name, boolean permitSyntax) {
        try {
            assertNameIsValid(name,permitSyntax);
            return null;
        } catch (VariableNameSyntaxException e) {
            return e.getMessage();
        }
    }

    /**
     * Prefix the given name.
     *
     * @param prefix The prefix to use (may be null)
     * @param name The name being prefixed (must not be null)
     * @return The name with prefix appended and a separator inserted if required.
     * @throws IllegalArgumentException if name is null
     */
    public static String prefixName( final String prefix, final String name ) {
        if ( name == null ) throw new IllegalArgumentException( "name must not be null" );
        String prefixedName = "";

        if ( prefix != null ) {
            prefixedName = prefix;
        }

        if ( !prefixedName.endsWith(".") ) {
            prefixedName += ".";
        }

        if ( name.startsWith(".") ) {
            prefixedName += name.substring(1);
        } else {
            prefixedName += name;
        }

        return prefixedName;
    }
}
