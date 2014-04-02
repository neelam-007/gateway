package com.l7tech.gateway.common.security.rbac;

/**
 * Represents non CRUD operations secured on an Admin interface.
 * These values are used in conjunction with stereotype {@link com.l7tech.gateway.common.security.rbac.MethodStereotype#ENTITY_OPERATION}
 *
 * Use <code>getOperationName()</code> to compare to the value of a {@link Permission#otherOperationName}
 *
 * @see #getOperationName()
 */
public enum OtherOperationName {

    /**Operation which allows the internal audit viewer policy to be invoked.*/
    AUDIT_VIEWER_POLICY("audit-viewer policy"),
    LOG_VIEWER("log-viewer"),
    DEBUGGER("debugger");

    private OtherOperationName( final String operationName ) {
        this.operationName = operationName;
    }

    public String getOperationName() {
        return operationName;
    }

    private final String operationName;
}
