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
    public static final Feature AUXILIARY_SERVLETS = new Feature("AuxiliaryServices");

    private final String name;

    private Feature(String name) {
        if (name == null) throw new IllegalArgumentException("Name must not be null");
        this.name = name;
    }

    public String toString() {
        return name;
    }

    public String getName() {
        return name;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final Feature feature = (Feature)o;

        if (name != null ? !name.equals(feature.name) : feature.name != null) return false;

        return true;
    }

    public int hashCode() {
        return (name != null ? name.hashCode() : 0);
    }
}
