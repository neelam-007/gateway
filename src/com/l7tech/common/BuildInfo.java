/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common;

/**
 * Don't commit this class when doing an official build, only when you change it on purpose.
 * @author alex
 * @version $Revision$
 */
public class BuildInfo {
    // All these fields are replaced by the OFFICIAL-build target of the build script! Don't bother modifying them ever!
    private static final String BUILD_NUMBER = "11";
    private static final String PRODUCT_VERSION = "0.98b";
    private static final String PRODUCT_NAME = "Layer7 SecureSpan Suite";
    private static final String BUILD_DATE = "20030916";
    private static final String BUILD_TIME = "153238";
    private static final String BUILD_USER = "alex";
    private static final String BUILD_MACHINE = "locutus.l7tech.com";

    // This is maintained by CVS.
    private static final String BUILD_TAG = "$Name$";

    public static String getLongBuildString() {
        StringBuffer string = new StringBuffer();
        string.append( getBuildString() );
        string.append( " (" ).append( getBuildTag() ).append( ")" );
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

    public static String getProductVersion() {
        return PRODUCT_VERSION;
    }

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

    public static String getBuildTag() {
        String tag = stripCvsCrap( BUILD_TAG );
        if ( tag.length() == 0 ) tag = "HEAD";
        return tag;
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
