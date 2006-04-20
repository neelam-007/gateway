/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion.xmlsec;

/**
 * 
 */
public interface ResponseWssConfig extends SecurityHeaderAddressable {
    public String getKeyReference();
    public void setKeyReference(String keyReference);
}
