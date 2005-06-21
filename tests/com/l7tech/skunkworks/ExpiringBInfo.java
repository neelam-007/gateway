/*
* Copyright (C) 2005 Layer 7 Technologies Inc.
*
*/

package com.l7tech.skunkworks;

/**
 * This is a version of the class B u i l d I n f o that has been hacked to cause the software to expire.
 */
public class ExpiringBInfo {
    private static final long JUN_21_2005 = 1119381999000L;
    private static final long JUN_19_2005 = 1119181999000L;
    private static final long SEP_1_2005 = 1125557999000L;
    static {
        expiryCheck();
    }

    private static void expiryCheck() {
        long now = System.currentTimeMillis();
        if (now > SEP_1_2005) {
            final String msg = "This copy of the software has expired.  http://l7tech.com";
            System.err.println(msg);
            System.exit(253);
            throw new LinkageError(msg);
        }
    }

    // All these fields are replaced by the OFFICIAL-build target of the build script! Don't bother modifying them ever!
    private static final String BUILD_NUMBER = "3148ev1";      // ie, 1014
    private static final String PRODUCT_VERSION = "3.1ev1";   // ie, 0.98b
    private static final String PRODUCT_NAME = "Layer 7 SecureSpan Evaluation";
    private static final String BUILD_DATE = "20050606";  // ie, 20030916
    private static final String BUILD_TIME = "154648";  // ie, 153238
    private static final String BUILD_USER = "releng";  // ie, alex
    private static final String BUILD_MACHINE = "buildmachine.l7tech.com"; // ie, locutus.l7tech.com

    public static String getLongBuildString() {
        expiryCheck();
        StringBuffer string = new StringBuffer();
        string.append( getBuildString() );
        string.append( ", built " ).append( getBuildDate() ).append( getBuildTime() );
        string.append( " by " ).append( getBuildUser() ).append(" at ").append( getBuildMachine() );
        return string.toString();
    }

    public static String getBuildString() {
        expiryCheck();
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
        expiryCheck();
        return BUILD_NUMBER;
    }

    public static String getProductVersion() {
        expiryCheck();
        return PRODUCT_VERSION;
    }

    public static String getProductName() {
        expiryCheck();
        return PRODUCT_NAME;
    }

    public static String getBuildMachine() {
        expiryCheck();
        return BUILD_MACHINE;
    }

    public static String getBuildUser() {
        expiryCheck();
        return stripCvsCrap(BUILD_USER);
    }

    public static String getBuildTime() {
        expiryCheck();
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

