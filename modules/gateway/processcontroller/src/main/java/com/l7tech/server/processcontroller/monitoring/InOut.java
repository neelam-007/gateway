/*
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller.monitoring;

public enum InOut {
    IN("normal", "in tolerance"),
    OUT("alert", "out of tolerance");

    private final String shortName;
    private final String description;

    private InOut(String shortName, String description) {
        this.shortName = shortName;
        this.description = description;
    }

    public String getShortName() {
        return shortName;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return description;
    }
}
