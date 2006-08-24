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
    UPDATE("Write"),
    DELETE("Delete"),
    OTHER("<other>"),
    NONE("<none>"),
    ;

    private OperationType(String name) {
        this.opName = name;
    }

    private final String opName;

    public String getName() {
        return opName;
    }

    public String toString() {
        return opName;
    }
}
