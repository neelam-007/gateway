package com.l7tech.policy.assertion.ext.cei;

import java.util.Map;

/**
 * Indicates that this Custom Assertion UI or Custom Task Action UI can make use of the Console Context to retrieve custom objects from the Gateway.
 */
public interface UsesConsoleContext<String, Object> {
    /**
     * Sets a map of custom objects from the Gateways that it can use.
     * @param consoleContext a map of custom objects from the Gateway
     */
    void setConsoleContextUsed(Map<String, Object> consoleContext);
}

