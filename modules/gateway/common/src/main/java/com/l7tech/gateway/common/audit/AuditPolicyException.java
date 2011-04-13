/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.gateway.common.audit;

/**
 * Exception super class for audit viewer / message filter policy exceptions
 */
public class AuditPolicyException extends Exception{
    public AuditPolicyException(String message) {
        super(message);
    }

    public AuditPolicyException(String message, Throwable cause) {
        super(message, cause);
    }
}
