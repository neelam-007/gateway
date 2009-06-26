package com.l7tech.gateway.config.backuprestore;

/**
 * Copyright (C) 2009, Layer 7 Technologies Inc.
 *  
 * Represents a single command line argument. Defines whether it expects a value, and if that value represents a file
 * which should exist
 *
 * This class is immutable
 */
public final class CommandLineOption {
    private final String name;
    private final String description;
    private final boolean isValuePath;
    private final boolean hasNoValue;

    CommandLineOption(final String name, final String desc) {
        this.name = name;
        this.description = desc;
        isValuePath = false;
        hasNoValue = false;
    }
    CommandLineOption(final String name, final String desc, final boolean isValuePath, final boolean hasNoValue) {
        this.name = name;
        this.description = desc;
        this.isValuePath = isValuePath;
        this.hasNoValue = hasNoValue;
    }

    public String getDescription() {
        return description;
    }

    public boolean isHasNoValue() {
        return hasNoValue;
    }

    public boolean isValuePath() {
        return isValuePath;
    }

    public String getName() {
        return name;
    }
}