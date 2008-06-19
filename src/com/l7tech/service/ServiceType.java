/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.service;

/** @author alex */
public enum ServiceType {
    PUBLISHED_SERVICE("Published Service"),
    TOKEN_SERVICE("WS-Trust Token Service"),
    POLICY_SERVICE("Policy Service"),
    OTHER_INTERNAL_SERVICE("Other Internal Service");

    private final String description;

    private ServiceType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}