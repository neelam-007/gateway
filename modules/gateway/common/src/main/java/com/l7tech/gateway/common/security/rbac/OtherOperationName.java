/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.gateway.common.security.rbac;

/**
 * Represents non CRUD operations secured on an Admin interface.
 * These values are used in conjunction with stereotype {@link com.l7tech.gateway.common.security.rbac.MethodStereotype#ENTITY_OPERATION}
 *
 * The toString() values of these enum values are used to compare to the value of a {@link Permission#otherOperationName}
 * 
 */
public enum OtherOperationName {

    /**Operation which allows the internal audit viewer policy to be invoked.*/
    AUDIT_VIEWER_POLICY("audit-viewer policy");

    private OtherOperationName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    private final String name;
}
