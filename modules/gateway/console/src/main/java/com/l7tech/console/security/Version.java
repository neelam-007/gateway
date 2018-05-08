package com.l7tech.console.security;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Version {
    private final String version;
    private final int major;
    private final int minor;
    private final int subMinor;

    public Version(final int major, final int minor, final int subMinor) {
        this.major = major;
        this.minor = minor;
        this.subMinor = subMinor;
        version = major + "." + minor + "." + (subMinor < 10 ? ("0" + subMinor) : subMinor);
    }

    public Version(@NotNull final String version) {
        final @Nullable int[] splitVersion = PolicyManagerBuildInfo.parseVersionString(version);
        if (splitVersion == null) {
            throw new IllegalStateException("Unable to parse version: " + version);
        }
        this.major = splitVersion[0];
        this.minor = splitVersion[1];
        this.subMinor = splitVersion[2];
        this.version = version;
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

    @Override
    public String toString() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Version version = (Version) o;

        if (major != version.major) return false;
        if (minor != version.minor) return false;
        return subMinor == version.subMinor;
    }

    @Override
    public int hashCode() {
        int result = major;
        result = 31 * result + minor;
        result = 31 * result + subMinor;
        return result;
    }
}
