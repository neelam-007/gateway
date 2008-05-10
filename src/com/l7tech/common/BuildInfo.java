/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common;

/**
 * Don't commit this class after doing a build, only when you change it on purpose.
 */
public class BuildInfo {
    // All these fields are replaced by the OFFICIAL-build target of the build script! Don't bother modifying them ever!
    private static final String BUILD_NUMBER = "3507";      // ie, 1014
    private static final String PRODUCT_VERSION = "HEAD";   // ie, 3.4b (human readable)
    private static final String PRODUCT_VERSION_MAJOR = "4"; // ie, 3
    private static final String PRODUCT_VERSION_MINOR = "4"; // ie, 4
    private static final String PRODUCT_NAME = "Layer 7 SecureSpan Suite";
    private static final String BUILD_DATE = "20080509";  // ie, 20030916
    private static final String BUILD_TIME = "182542";  // ie, 153238
    private static final String BUILD_USER = "steve";  // ie, alex
    private static final String BUILD_MACHINE = "fish.l7tech.com"; // ie, locutus.l7tech.com
    private static final String PRODUCT_VERSION_SUBMINOR = "0"; // ie, 5

    public static String getLongBuildString() {
        StringBuffer string = new StringBuffer();
        string.append( getBuildString() );
        string.append( ", built " ).append( getBuildDate() ).append( getBuildTime() );
        string.append( " by " ).append( getBuildUser() ).append(" at ").append( getBuildMachine() );
        return string.toString();
    }

    public static String getBuildString() {
        StringBuffer string = new StringBuffer();
        string.append( getProductName() ).append( " " );
        string.append( getProductVersion() );
        string.append( " build " ).append( getBuildNumber() );
        return string.toString();
    }

    public static void main(String[] args) {
        System.out.println( getLongBuildString() );
        System.out.println( getBuildString() );
    }

    public static String getBuildDate() {
        return BUILD_DATE;
    }

    public static String getBuildNumber() {
        return BUILD_NUMBER;
    }

    /** @return a human readable product version string, ie "3.4b5_p3_banana" */
    public static String getProductVersion() {
        return PRODUCT_VERSION;
    }

    /** @return the strict product major version, ie "3".  NOTE: Licenses bind to this. */
    public static String getProductVersionMajor() {
        return PRODUCT_VERSION_MAJOR;
    }

    /** @return the strict product minor version, ie "4". NOTE: Licenses bind to this. */
    public static String getProductVersionMinor() {
        return PRODUCT_VERSION_MINOR;
    }

    /** @return the strict product subminor version, ie "5" in 3.6.5 NOTE: Licenses DO NOT bind to this. */
    public static String getProductVersionSubMinor() {
        return PRODUCT_VERSION_SUBMINOR;
    }

    /** @return the strict product name, ie "Layer 7 SecureSpan Suite".  NOTE: Licenses bind to this. */
    public static String getProductName() {
        return PRODUCT_NAME;
    }

    public static String getBuildMachine() {
        return BUILD_MACHINE;
    }

    public static String getBuildUser() {
        return stripCvsCrap(BUILD_USER);
    }

    public static String getBuildTime() {
        return BUILD_TIME;
    }

    private static String stripCvsCrap( String crappy ) {
        if ( crappy.startsWith("$") && crappy.endsWith("$") )
            crappy = crappy.substring( 1, crappy.length()-1 );
        else
            return crappy;

        int cpos = crappy.indexOf(":");
        if ( cpos >= 0 )
            return crappy.substring(cpos+1,crappy.length()).trim();
        else
            return "";
    }
}
