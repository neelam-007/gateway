/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion;

/**
 * Used as a property of assertions that can be configured to operate on requests, responses and/or message-typed
 * variables.
 * 
 * @author alex
*/
public enum TargetMessageType {
    REQUEST,
    RESPONSE,
    OTHER
}
