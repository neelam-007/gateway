/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.util;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Date;
import java.util.Properties;
import java.io.InputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.text.ParseException;

/**
 * Product build information.
 */
public class BuildInfo {

    //- PUBLIC

    /**
     * Get the long form of the build string.
     *
     * <code>Layer 7 SecureSpan Suite X.Y.Z build 1234, built 20001231235959 by user at host.l7tech.com</code>
     *
     * @return The build string
     */
    public static String getLongBuildString() {
        StringBuilder string = new StringBuilder();
        string.append( getBuildString() );
        string.append( ", built " ).append( getBuildDate() ).append( getBuildTime() );
        string.append( " by " ).append( getBuildUser() ).append(" at ").append( getBuildMachine() );
        return string.toString();
    }

    /**
     * Get the long form of the build string.
     *
     * <code>Layer 7 SecureSpan Suite X.Y.Z build 1234</code>
     *
     * @return The build string
     */
    public static String getBuildString() {
        StringBuilder string = new StringBuilder();
        string.append( getProductName() ).append( " " );
        string.append( getProductVersion() );
        string.append( " build " ).append( getBuildNumber() );
        return string.toString();
    }

    /**
     * Get the build date.
     *
     * @return The date string
     */
    public static String getBuildDate() {
        return getFormattedBuildTimestamp(BUILD_DATE_FORMAT);
    }

    /**
     * Get the build time.
     *
     * @return The time string
     */
    public static String getBuildTime() {
        return getFormattedBuildTimestamp(BUILD_TIME_FORMAT);
    }
    
    /**
     * Get the build number.
     *
     * @return The build number string
     */
    public static String getBuildNumber() {
        return getBuildProperty(PROP_BUILD_NUMBER, "0000");
    }

    /**
     * Get the human readable product version string, ie "3.4b5_p3_banana"
     *
     *  @return The full version string.
     */
    public static String getProductVersion() {
        return getPackageImplementationVersion();
    }

    /**
     * Get the strict product major version, ie "3".  NOTE: Licenses bind to this.
     *
     * @return The major version string
     */
    public static String getProductVersionMajor() {
        return getPackageImplementationVersionField(0);
    }

    /**
     * Get the strict product minor version, ie "4". NOTE: Licenses bind to this.
     *
     * @return The minor version string  
     */
    public static String getProductVersionMinor() {
        return getPackageImplementationVersionField(1);
    }

    /**
     * Get the strict product subminor version, ie "5" in 3.6.5 NOTE: Licenses DO NOT bind to this.
     *
     * @return The subminor version string
     */
    public static String getProductVersionSubMinor() {
        return getPackageImplementationVersionField(2);
    }

    /**
     * Get the product name, ie "Layer 7 SecureSpan Suite".  NOTE: Licenses bind to this.
     *
     * @return The strict product name
     */
    public static String getProductName() {
        return getBuildProperty(PROP_BUILD_PRODUCT, "Layer 7 SecureSpan Suite");
    }

    /**
     * Get the build hostname.
     *
     * @return The build hostname
     */
    public static String getBuildMachine() {
        return getBuildProperty(PROP_BUILD_HOST, "build.l7tech.com");
    }

    /**
     * Get the name of the build user.
     *
     * @return The build username
     */
    public static String getBuildUser() {
        return getBuildProperty(PROP_BUILD_USER, "build");        
    }

    /**
     * Outputs version information to the console.
     */
    public static void main(String[] args) {
        System.out.println( getLongBuildString() );
        System.out.println( getBuildString() );
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( BuildInfo.class.getName() );

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String BUILD_DATE_FORMAT = "yyyyMMdd";
    private static final String BUILD_TIME_FORMAT = "HHmmss";
    private static final String BUILD_INFO_PROPERTIES = "resources/BuildInfo.properties";
    private static final String BUILD_VERSION_FALLBACK_PROPERTY = BuildInfo.class.getPackage().getName() + ".buildVersion";
    private static final String PROP_INVALID_MARKER = "@@@";

    private static final String PROP_BUILD_PRODUCT = "Build-Product";
    private static final String PROP_BUILD_TIMESTAMP = "Build-Timestamp";
    private static final String PROP_BUILD_HOST = "Build-Host";
    private static final String PROP_BUILD_USER = "Built-By";
    private static final String PROP_BUILD_NUMBER = "Build-Number";    

    private static String getPackageImplementationVersion() {
        String version = "0";
        Package packageInfo = BuildInfo.class.getPackage();
        if ( packageInfo != null ) {
            String packageVersion = packageInfo.getImplementationVersion();
            if ( packageVersion != null ) {
                version = packageVersion;                
            } else {
                version = SyspropUtil.getString(BUILD_VERSION_FALLBACK_PROPERTY, version);
            }
        }

        return version;
    }

    private static String getPackageImplementationVersionField( final int index ) {
        String versionField = "0";
        String version = getPackageImplementationVersion();

        // strip off non-numeric version information
        StringBuilder builder = new StringBuilder();
        for ( char character : version.toCharArray() ) {
            if ( Character.isDigit( character ) || character == '.' ) {
                builder.append( character );
            } else {
                break;
            }
        }
        version = builder.toString();

        // break into version parts
        String[] versions = version.split( "\\." );
        if ( index > -1 && index < versions.length && versions[index] != null && versions[index].length() > 0 ) {
            versionField = versions[index];
        }

        return versionField;
    }

    private static String getBuildProperty( final String name, final String defaultValue ) {
        String value = defaultValue; 

        InputStream in = null;
        try {
            in = BuildInfo.class.getResourceAsStream( BUILD_INFO_PROPERTIES );
            if ( in != null ) {
                Properties buildProperties = new Properties();
                buildProperties.load( in );
                String propValue = buildProperties.getProperty( name, defaultValue );
                if ( propValue != null && propValue.indexOf( PROP_INVALID_MARKER ) < 0 ) {
                    value = propValue;
                }
            }
        } catch ( IllegalArgumentException iae ) {
            logger.log( Level.WARNING, "Error reading build properties.", iae );
        } catch ( IOException ioe ) {
            logger.log( Level.WARNING, "Error reading build properties.", ioe );   
        } finally {
            ResourceUtils.closeQuietly( in );
        }

        return value;
    }

    private static String getFormattedBuildTimestamp( final String formatString ) {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        SimpleDateFormat outFormat = new SimpleDateFormat(formatString);
        try {
            Date buildDate = sdf.parse( getBuildProperty( PROP_BUILD_TIMESTAMP, "2000-01-01 00:00:00" ) );

            return outFormat.format( buildDate );
        } catch ( ParseException pe ) {
            logger.log( Level.WARNING, "Error processing JAR manifest build timestamp.", pe );
            return outFormat.format( new Date(0) );
        }
    }
}
