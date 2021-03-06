package com.l7tech.gateway.config.backuprestore;

/**
 * Copyright (C) 2009, Layer 7 Technologies Inc.
 *  
 * Represents a single command line argument. Defines whether it expects a value or not. If the value represents
 * a file, then the client should enforce this
 *
 * This class is immutable
 */
final class CommandLineOption {
    private final String name;
    private final String description;
    private final boolean hasValue;

    CommandLineOption(final String name, final String desc, final boolean hasValue) {
        this.name = name;
        this.description = desc;
        this.hasValue = hasValue;
    }

    public String getDescription() {
        return description;
    }

    public boolean isHasValue() {
        return hasValue;
    }

    public String getName() {
        return name;
    }
}