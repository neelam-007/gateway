package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This lists the different rest man versions.
 */
public enum RestManVersion implements Comparable<RestManVersion> {
    //Note these must be listed in proper order! Starting with the first version and ending with the last.
    VERSION_1_0(1, 0, 0),
    VERSION_1_0_1(1, 0, 1),
    VERSION_1_0_2(1, 0, 2);

    private final int major;
    private final int minor;
    private final int subMinor;

    private RestManVersion(int major, int minor, int subMinor) {
        this.major = major;
        this.minor = minor;
        this.subMinor = subMinor;
    }

    public String getStringRepresentation() {
        return major + "." + minor + "." + subMinor;
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getSubMinor() {
        return subMinor;
    }

    @Nullable
    public static RestManVersion fromString(@NotNull String version) {
        for (RestManVersion restManVersion : RestManVersion.values()) {
            if (restManVersion.getStringRepresentation().equals(version)) {
                return restManVersion;
            }
        }
        return null;
    }
}
