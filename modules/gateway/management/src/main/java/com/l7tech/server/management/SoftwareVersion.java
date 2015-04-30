/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management;

import com.l7tech.util.SyspropUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** @author alex */
public class SoftwareVersion {
    private static final String PROP_LAX_VERSION_CHECK = "com.l7tech.server.management.laxVersionCheck";

    private final int major;
    private final int minor;
    private final Integer revision;
    private final String revisionSuffix;
    private final Integer build;

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public Integer getRevision() {
        return revision;
    }

    public String getRevisionSuffix() {
        return revisionSuffix;
    }

    public Integer getBuild() {
        return build;
    }

    private static final Pattern PARSER = Pattern.compile("(\\d+)\\.(\\d+)(?:\\.(\\d+))?(\\w+)?(?:-[bB](\\d+))?");

    public static SoftwareVersion fromString(String s) throws NumberFormatException {
        final Matcher mat = PARSER.matcher(s);
        if (!mat.matches() || mat.groupCount() != 5) {
            if ( SyspropUtil.getBoolean( PROP_LAX_VERSION_CHECK, false ) )
                return new SoftwareVersion( 1, 0, 0, null, null );
            throw new IllegalArgumentException("Illegal version number format");
        }

        final int major = Integer.parseInt(mat.group(1));
        final int minor = Integer.parseInt(mat.group(2));

        final String srev = mat.group(3);
        final Integer revision = srev != null ? Integer.parseInt(srev) : null;
        final String ssuf = mat.group(4);
        final String suffix = ssuf != null ? ssuf.trim() : null;
        final String sbuild = mat.group(5);
        final Integer build = sbuild != null ? Integer.parseInt(sbuild) : null;

        return new SoftwareVersion(major, minor, revision, suffix, build);
    }

    private SoftwareVersion(int major, int minor, Integer revision, String suffix, Integer build) {
        this.major = major;
        this.minor = minor;
        this.revision = revision;
        this.build = build;
        this.revisionSuffix = suffix;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(major).append(".").append(minor);
        if (revision != null) sb.append(".").append(revision);
        if (revisionSuffix != null) sb.append(revisionSuffix);
        if (build != null) sb.append("-b").append(build);
        return sb.toString();
    }
}
