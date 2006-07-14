/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.common.security.rbac;

/**
 * @author alex
 */
public enum OperationType {
    CREATE("Create"),
    READ("Read"),
    WRITE("Write"),
    DELETE("Delete"),
    OTHER("<other>"),
    ;

    private OperationType(String name) {
        this.opName = name;
    }

    private final String opName;

    public Object getName() {
        return opName;
    }

    public String toString() {
        return opName;
    }
}
