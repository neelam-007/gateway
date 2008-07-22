/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.common.security.rbac;

import java.util.EnumSet;

/**
 * @author alex
 */
public enum OperationType {
    CREATE("Create"),
    READ("Read"),
    UPDATE("Update"),
    DELETE("Delete"),
    OTHER("<other>"),
    NONE("<none>"),
    ;

    public static final EnumSet<OperationType> ALL_CRUD = EnumSet.of(CREATE, READ, UPDATE, DELETE);

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
