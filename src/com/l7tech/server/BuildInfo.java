/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server;

/**
 * @author alex
 * @version $Revision$
 */
public class BuildInfo {
    private static final String BUILD_NUMBER = "9";
    private static final String PRODUCT_VERSION = "0.98";
    private static final String PRODUCT_NAME = "ssg";
    private static final String BUILD_DATE = "20030916";
    private static final String BUILD_TIME = "150820";
    private static final String BUILD_TAG = "$Name$";
    private static final String BUILD_USER = "$Author$";

    public static String getBuildString() {
        StringBuffer string = new StringBuffer();
        string.append( PRODUCT_NAME ).append( " " );
        string.append( "version " ).append( PRODUCT_VERSION ).append( ", " );
        string.append( "build " ).append( BUILD_NUMBER ).append( ", " );
        string.append( "built on " ).append( BUILD_DATE ).append( " at " );
        string.append( BUILD_TIME ).append( ", tag " ).append( getBuildTag() );
        return string.toString();
    }

    public static void main(String[] args) {
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

        int cpos = crappy.indexOf(":");
        if ( cpos >= 0 )
            return crappy.substring(cpos+1,crappy.length()).trim();
        else
            return "";
    }
}
