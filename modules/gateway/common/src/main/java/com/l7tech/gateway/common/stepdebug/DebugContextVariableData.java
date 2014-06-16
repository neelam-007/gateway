package com.l7tech.gateway.common.stepdebug;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Set;
import java.util.TreeSet;

/**
 * Represents a context variable and its properties.
 */
public class DebugContextVariableData implements Serializable, Comparable<DebugContextVariableData> {

    final private String name;
    final private String value;
    final private String dataType;
    final private boolean isUserAdded;
    final private Set<DebugContextVariableData> children;
    private String parentName;
    private int childIndex = -1; // Only applicable for multi-value.
    private boolean isDisplayNullValue = true;
    private boolean isDisplayDoubleQuotes = true;

    /**
     * Creates <code>DebugContextVariableData</code>.
     *
     * @param name the name of the context variable
     * @param value the value of the context variable
     * @param dataType the data type of the context variable
     * @param isUserAdded indicates whether or not the context variable is added by user
     */
    public DebugContextVariableData(@NotNull String name,
                                    @Nullable String value,
                                    @Nullable String dataType,
                                    boolean isUserAdded) {
        this.name = name;
        this.value = value;
        this.dataType = dataType;
        this.isUserAdded = isUserAdded;
        this.children = new TreeSet<>();
    }

    /**
     * Gets the name of the context variable.
     *
     * @return the name of the context variable
     */
    @NotNull
    public String getName() {
        return name;
    }

    /**
     * Sets whether or not to display double quotes around value.
     *
     * @param isDisplayDoubleQuotes whether or not to display double quotes around value
     */
    public void setIsDisplayDoubleQuotes (boolean isDisplayDoubleQuotes) {
        this.isDisplayDoubleQuotes = isDisplayDoubleQuotes;
    }

    /**
     * Sets whether or not to display 'null' text for null value.
     *
     * @param isDisplayNullValue whether or not to display 'null' text for null value
     */
    public void setIsDisplayNullValue (boolean isDisplayNullValue) {
        this.isDisplayNullValue = isDisplayNullValue;
    }

    /**
     * Sets the parent context variable name.
     *
     * @param parentName the parent context variable name
     */
    public void setParentName(@NotNull String parentName) {
        this.parentName = parentName;
    }

    /**
     * Sets the child index.
     *
     * @param childIndex the child index
     */
    public void setChildIndex(int childIndex) {
        this.childIndex = childIndex;
    }

    /**
     * Gets whether or not context variable data is added by user.
     *
     * @return true if the context variable data is added by user, false otherwise
     */
    public boolean getIsUserAdded() {
        return isUserAdded;
    }

    /**
     * Add child context variable.
     * For example for "request" message, "request.mainpart", "request.contentType", etc can be
     * added.
     *
     * @param child the child context variable data
     */
    public void addChild(@NotNull DebugContextVariableData child) {
        children.add(child);
    }

    /**
     * Returns child context variables.
     *
     * @return child context variables. Never null.
     */
    @NotNull
    public Set<DebugContextVariableData> getChildren() {
        return children;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name);
        sb.append(" = ");

        if (dataType != null) {
            sb.append("{");
            sb.append(dataType);
            sb.append("} ");
        }

        if (value != null) {
            if (isDisplayDoubleQuotes) {
                sb.append("\"");
                sb.append(value);
                sb.append("\"");
            } else {
                sb.append(value);
            }
        } else {
            if (isDisplayNullValue) {
                sb.append("null");
            }
        }

        return sb.toString();
    }

    @Override
    public int compareTo(@NotNull DebugContextVariableData o) {
        int result;

        // Compare fields that uniquely identify the debug context variable.
        //
        result = compareStringNullSafe(this.parentName, o.parentName);
        if (result != 0) {
            return result;
        }

        result = Integer.compare(this.childIndex, o.childIndex);
        if (result != 0) {
            return result;
        }

        result = compareStringNullSafe(this.name, o.name);
        if (result != 0) {
            return result;
        }

        result = Boolean.compare(this.isUserAdded, o.isUserAdded);
        if (result != 0) {
            return result;
        }

        return result;
    }

    private static int compareStringNullSafe (String str1, String str2) {
        int result;

        if (str1 != null && str2 != null) {
            result = str1.compareToIgnoreCase(str2);
        } else if (str1 != null) {
            result = 1;
        } else if (str2 != null) {
            result = -1;
        } else {
            result = 0;
        }

        return result;
    }
}