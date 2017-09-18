/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
     * <code>CA API Gateway X.Y.Z build 1234, built 20001231235959 by user at host.l7tech.com</code>
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
     * <code>CA API Gateway X.Y.Z build 1234</code>
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
     * Get the human readable formal product version string, i.e. "4.7.0"
     *
     *  @return The 3 digit, dot separated version string.
     */
    public static String getFormalProductVersion() {
        return getProductVersionMajor() + "." + getProductVersionMinor() + "." + getProductVersionSubMinor();
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
     * Get the product name, ie "CA API Gateway".  NOTE: Licenses bind to this.
     *
     * @return The strict product name
     */
    public static String getProductName() {
        return getBuildProperty(PROP_BUILD_PRODUCT, productName);
    }

    /**
     * Get the legacy product name, ie "Layer 7 SecureSpan Suite".  NOTE: Old Licenses are bind to this so
     * we need to support legacy product name in order to support upgrade.
     *
     * @return The strict product name
     */
    public static String getLegacyProductName() {
        return getBuildProperty(PROP_BUILD_PRODUCT, legacy_productName);
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
     * Set the package for the product and the default name.
     *
     * @param productPackage The package whose version info should be used.
     * @param productName The name of the product
     */
    public static void setProduct( final Package productPackage, final String productName ) {
        if ( packageInfo == null || BuildInfo.class.getPackage().equals(packageInfo) ) {
            BuildInfo.packageInfo = productPackage;
            BuildInfo.productName = productName;
        } else {
            throw new IllegalStateException( "Product already initialized." );
        }
    }

    /**
     * Parse a string (which should be in the format X.Y.Z, where X,Y,Z are positive 1 or 2 digit numbers)
     * into its subparts, where X = major version, Y = minor version, Z = subminor version
     * Note: (1) for invalid strings, the returned value is null
     *       (2) for subparts containing leading a leading 0, the leading zero is stripped out
     *           Example: "01" = "1", "00" = "0"
     *       (3) X (major version) cannot be "0" or "00".  This is treated as invalid
     *
     * @param  versionString  a string representing the version of db or gateway
     * @return int[]  the string subparts as an int array {X.Y.Z}, where X,Y,Z are positive 1-or-2 digit numbers
     *                OR null on error (for parsing errors when the version string is in an incorrect format)
     */
    @Nullable
    public static int[] parseVersionString(final String versionString) {
        if (versionString == null || versionString.trim().isEmpty()) {
            logger.log(Level.WARNING, VERSION_PARSING_ERR_MSG_START + versionString + VERSION_PARSING_ERR_MSG_END);
            return null;
        }

        // check if version format matches X.Y.Z where X,Y,Z are positive numbers with max length=2
        if (!versionString.matches(VERSION_FORMAT_REGEX)) {
            logger.log(Level.WARNING, VERSION_PARSING_ERR_MSG_START + versionString + VERSION_PARSING_ERR_MSG_END);
            return null;
        }

        String[] versionStringSubparts = versionString.split(VERSION_SUBPART_SEPARATOR_REGEX);

        int[] versionSubparts = {0,0,0};
        for (int i = 0; i < versionStringSubparts.length; i++) {
            versionSubparts[i] = Integer.parseInt(versionStringSubparts[i]);
        }

        if (versionSubparts[0] == 0) {
            logger.log(Level.WARNING, VERSION_PARSING_ERR_MSG_START + versionString + VERSION_PARSING_ERR_MSG_END);
            return null;
        }
        return versionSubparts;
    }

    /**
     * Compare 2 versions and return the difference (-1, 0, or +1) between them
     *
     * @param v1Subparts  int array in the format {X.Y.Z} where X,Y,Z are positive 2-digit numbers.
     * @param v2Subparts  int array in the format {X.Y.Z} where X,Y,Z are positive 2-digit numbers.
     * @precondition v1Subparts and v2Subparts must be in the correct format for this method to work
     *
     * @return  (a) if v1Subparts == v2Subparts, return 0
     *          (b) if v1Subparts > v2Subparts, return +1
     *          (c) if v1Subparts < v2Subparts, return -1
     *          (d) null on error (when either input argument is null or in the incorrect format)
     */
    static Integer compareVersions(final int[] v1Subparts, final int[] v2Subparts) {

        if (v1Subparts == null || v2Subparts == null || v1Subparts.length != 3 || v2Subparts.length != 3) {
            return null;
        }

        int v1Major = v1Subparts[0];
        int v1Minor = v1Subparts[1];
        int v1SubMinor = v1Subparts[2];
        int v2Major = v2Subparts[0];
        int v2Minor = v2Subparts[1];
        int v2SubMinor = v2Subparts[2];

        // if versions are equal, return 0
        if (v1Major == v2Major && v1Minor == v2Minor && v1SubMinor == v2SubMinor) {
            return 0;
        }

        // else return the difference between v1 - v2 (+1 if v1 is larger, -1 if v2 is larger)
        if ( (v1Major < v2Major) ||
                (v1Major == v2Major && v1Minor < v2Minor) ||
                (v1Major == v2Major && v1Minor == v2Minor && v1SubMinor < v2SubMinor) ) {
            return -1;
        } else {
            return 1;
        }
    }

    /**
     * Compare 2 versions and return the difference (-ve, 0, or +ve) between their Major version
     *
     * @param versionA  string in the format X.Y.Z where X,Y,Z are positive 2-digit numbers.
     * @param versionB  string in the format X.Y.Z where X,Y,Z are positive 2-digit numbers.
     *
     * @return  (a) if versionA.X == versionB.X, return 0
     *          (b) if versionA.X > versionB.X, return the +ve difference between their major versions
     *          (c) if versionA.X < versionB.X, return the -ve difference between their major versions
     *          (d) null on error (for parsing errors when the version string is in an incorrect format)
     */
    @Nullable
    static Integer diffBetweenMajorVersions(final String versionA, final String versionB) {
        final int[] v1Subparts = parseVersionString(versionA);
        final int[] v2Subparts = parseVersionString(versionB);

        if (v1Subparts == null || v2Subparts == null) {
            return null;
        } else {
            return v1Subparts[0] - v2Subparts[0];
        }
    }

    /**
     * Compare 2 versions and return the difference (-1, 0, or +1) between versionA and VersionB
     *
     * @param versionA  string in the format X.Y.Z where X,Y,Z are positive 2-digit numbers.
     * @param versionB  string in the format X.Y.Z where X,Y,Z are positive 2-digit numbers.
     *
     * @return  (a) if versionA == versionB, return 0
     *          (b) if versionA > versionB, return +1
     *          (c) if versionA < versionB, return -1
     *          (d) null on error (for parsing errors when the version string is in an incorrect format)
     */
    @Nullable
    public static Integer compareVersions(@NotNull final String versionA, @NotNull final String versionB) {
        final int[] v1Subparts = parseVersionString(versionA);
        final int[] v2Subparts = parseVersionString(versionB);

        if (v1Subparts == null || v2Subparts == null) {
            return null;
        } else {
            return compareVersions(v1Subparts, v2Subparts);
        }
    }

    /**
     * The Gateway version is compatible with the database version if:
     * (a) the database is the same version as the Gateway or higher AND
     * (b) the database is at most "maxMjorVersionRange" major versions ahead
     * Gateway and database versions are in the format X.Y.Z,
     * where X = major version, Y = minor version, Z = subminor version
     *
     * @param gatewayVersion  string representing the gateway version
     * @param dbVersion  string representing the db version
     * @param maxMajorVersionDiff  the allowable max Major Version difference by which the db version can be higher than the gateway version
     * @return boolean  true if (gateway Version <= db Version) AND (db version <= gateway version + maxMajorVersionRange),
     *                  false otherwise
     */
    public static boolean isGatewayVersionCompatibleWithDBVersion(@NotNull String gatewayVersion, @NotNull String dbVersion, int maxMajorVersionDiff) {
        // if gatewayVersion == dbVersion, diff = 0;
        // if gatewayVersion < dbVersion, diff = gatewayVersion - dbVersion (-ve)
        // if gatewayVersion > dbVersion, diff = gatewayVersion - dbVersion (+ve)
        Integer diff = diffBetweenMajorVersions(gatewayVersion, dbVersion);
        Integer lessThan = compareVersions(gatewayVersion, dbVersion);
        return  (lessThan != null) && (lessThan <= 0) && (diff != null) && (-maxMajorVersionDiff <= diff && diff <= 0);
    }

    /**
     * The Gateway version is compatible with the database version if:
     * (a) the database is the same version as the Gateway or higher AND
     * (b) the database is at most 2 major versions ahead
     * Gateway and database versions are in the format X.Y.Z,
     * where X = major version, Y = minor version, Z = subminor version
     *
     * @param gatewayVersion  string representing the gateway version
     * @param dbVersion  string representing the db version
     * @return boolean  true if (gateway Version <= db Version) AND (db version <= gateway version + 2),
     *                  false otherwise
     */
    public static boolean isGatewayVersionCompatibleWithDBVersion(@NotNull final String gatewayVersion, @NotNull final String dbVersion) {
        return isGatewayVersionCompatibleWithDBVersion(gatewayVersion, dbVersion, MAX_ACCEPTABLE_MAJOR_VERSION_DIFF);
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

    private static final String VERSION_FORMAT_REGEX = "[0-9]{1,2}[.][0-9]{1,2}[.][0-9]{1,2}";
    private static final String VERSION_SUBPART_SEPARATOR_REGEX = "[.]";
    private static final String VERSION_PARSING_ERR_MSG_START = "Error parsing version string \"";
    private static final String VERSION_PARSING_ERR_MSG_END = "\". Acceptable format: X.Y.Z where X,Y,Z are positive 1-or-2 digit numbers.";
    private static final int MAX_ACCEPTABLE_MAJOR_VERSION_DIFF = 2;


    private static Package packageInfo = BuildInfo.class.getPackage();
    private static String productName = "CA API Gateway";
    private static String legacy_productName = "Layer 7 SecureSpan Suite";

    private static String getPackageImplementationVersion() {
        String version = "0";

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
