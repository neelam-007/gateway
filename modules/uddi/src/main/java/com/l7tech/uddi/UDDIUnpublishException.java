/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.uddi;

/**
 * UDDIException representing that it's not possible to unpublish from UDDI and that a retry will not fix the issue.
 */
public class UDDIUnpublishException extends UDDIException{
    public UDDIUnpublishException(String message) {
        super(message);
    }
}
