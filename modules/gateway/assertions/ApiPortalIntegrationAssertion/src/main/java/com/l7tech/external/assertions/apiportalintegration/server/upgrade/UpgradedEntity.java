package com.l7tech.external.assertions.apiportalintegration.server.upgrade;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * Represents an entity that was upgraded.
 */
public class UpgradedEntity implements Serializable {
    public static final String API = "API";
    public static final String KEY = "API KEY";
    public static final String SERVICE = "SERVICE";

    private final String id;
    private final String type;
    private final String description;

    public UpgradedEntity(@NotNull final String id, @NotNull final String type, @NotNull final String description) {
        this.id = id;
        this.type = type;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }
}
