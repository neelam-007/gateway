/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common;

/**
 * Typesafe enumeration for features enabled by the LicenseManager.
 */
public final class Feature {
    public static final Feature MESSAGEPROCESSOR = new Feature("MessageProcessor");
    public static final Feature ADMIN = new Feature("Admin");

    private String name;

    private Feature(String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    }

    public String getName() {
        return name;
    }
}
