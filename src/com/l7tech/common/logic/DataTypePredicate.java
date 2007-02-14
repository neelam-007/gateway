/**
 * Copyright (C) 2006-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.common.logic;

import com.l7tech.policy.variable.DataType;

/**
 * Evaluates whether the rvalue is, or can be converted to, one of the classes described by {@link DataType#getValueClasses()}.
 * @author alex
 */
public class DataTypePredicate extends Predicate {
    private DataType type;

    public DataTypePredicate() {
    }

    public DataTypePredicate(DataType type) {
        if (type == DataType.UNKNOWN)
            throw new IllegalArgumentException("Can't create a DataTypePredicate with type == UNKNOWN");
        this.type = type;
    }

    public DataType getType() {
        return type;
    }

    public void setType(DataType type) {
        this.type = type;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (negated) sb.append("!");
        sb.append("IS(");
        sb.append(type.getShortName());
        sb.append(")");
        return sb.toString();
    }

    public String getSimpleName() {
        return "dataType";
    }
}
