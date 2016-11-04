package com.l7tech.util;

import org.junit.Test;

import java.util.Arrays;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

public class BuildInfoTest {

    @Test
    public void testParseVersionString() {
        // Test for null, empty, and blank strings
        assertNull(BuildInfo.parseVersionString(null));
        assertNull(BuildInfo.parseVersionString(""));
        assertNull(BuildInfo.parseVersionString(" "));

        // Test where number of subparts != 3 (Note: subpart separator = '.')
        assertNull(BuildInfo.parseVersionString("1"));
        assertNull(BuildInfo.parseVersionString("1.2"));
        assertNotNull(BuildInfo.parseVersionString("1.2.3"));
        assertNull(BuildInfo.parseVersionString("1.2.3.4"));
        assertNull(BuildInfo.parseVersionString("1.2.3.4.5"));
        assertNull(BuildInfo.parseVersionString("1.2.3.4.5.6"));

        // Test for invalid subpart separator (Note: subpart separator = '.')
        assertNull(BuildInfo.parseVersionString("9,2,01"));

        // Test for empty subparts
        assertNull(BuildInfo.parseVersionString(".."));
        assertNull(BuildInfo.parseVersionString("9.."));
        assertNull(BuildInfo.parseVersionString(".7."));
        assertNull(BuildInfo.parseVersionString("..8"));
        assertNull(BuildInfo.parseVersionString("9.2."));
        assertNull(BuildInfo.parseVersionString("9..2"));
        assertNull(BuildInfo.parseVersionString(".9.2"));

        // Test for non-digit subparts
        assertNull(BuildInfo.parseVersionString("x.y.z"));
        assertNull(BuildInfo.parseVersionString("testX.testY.testZ"));
        assertNull(BuildInfo.parseVersionString("9.2.x"));
        assertNull(BuildInfo.parseVersionString("9.x.0"));
        assertNull(BuildInfo.parseVersionString("x.2.0"));

        // Test for digit & character combinations for subparts
        assertNull(BuildInfo.parseVersionString("9x.7y.8z"));
        assertNull(BuildInfo.parseVersionString("x9.y7.z8"));

        // Test for subparts where number of digits > 2
        assertNull(BuildInfo.parseVersionString("999.2.0"));
        assertNull(BuildInfo.parseVersionString("9.222.0"));
        assertNull(BuildInfo.parseVersionString("9.2.000"));

        // Test for major version == 0
        assertNull(BuildInfo.parseVersionString("0.2.0"));
        assertNull(BuildInfo.parseVersionString("00.2.0"));

        // Test for negative numbers in subparts
        assertNull(BuildInfo.parseVersionString("-9.2.1"));
        assertNull(BuildInfo.parseVersionString("9.-2.1"));
        assertNull(BuildInfo.parseVersionString("9.2.-1"));

        // Test for versions that don't make any sense
        assertNull(BuildInfo.parseVersionString("0.0.0"));
        assertNull(BuildInfo.parseVersionString("00.00.00"));
        assertNull(BuildInfo.parseVersionString("-0.-0.-0"));

        // Test that the output of the parsing method returns an int array in the correct format for valid input strings
        assertTrue(Arrays.equals(new int[]{9,2,1}, BuildInfo.parseVersionString("9.2.1")));
        assertTrue(Arrays.equals(new int[]{9,2,11}, BuildInfo.parseVersionString("9.2.11")));
        assertTrue(Arrays.equals(new int[]{9,22,1}, BuildInfo.parseVersionString("9.22.1")));
        assertTrue(Arrays.equals(new int[]{99,2,1}, BuildInfo.parseVersionString("99.2.1")));

        //Test that any 2 digit subparts containing a leading zero gets parsed correctly
        assertTrue(Arrays.equals(new int[]{9,2,0}, BuildInfo.parseVersionString("9.2.00")));
        assertTrue(Arrays.equals(new int[]{9,2,1}, BuildInfo.parseVersionString("9.2.01")));
        assertTrue(Arrays.equals(new int[]{9,0,1}, BuildInfo.parseVersionString("9.00.1")));
        assertTrue(Arrays.equals(new int[]{9,2,1}, BuildInfo.parseVersionString("9.02.1")));
        // For major version subpart, '0' and '00' are not valid
        //assertTrue(Arrays.equals(new int[]{0,2,1}, BuildInfo.parseVersionString("00.2.1")));
        assertTrue(Arrays.equals(new int[] {9,2,1}, BuildInfo.parseVersionString("09.2.1")));

        // Test that any 2 digit subparts ending in '0' get parsed correctly
        assertTrue(Arrays.equals(new int[]{9,2,10}, BuildInfo.parseVersionString("9.2.10")));
        assertTrue(Arrays.equals(new int[]{9,20,1}, BuildInfo.parseVersionString("9.20.1")));
        assertTrue(Arrays.equals(new int[]{90,2,1}, BuildInfo.parseVersionString("90.2.1")));
    }

    @Test
    public void testCompareVersionsWithIntArrayParameters() {
        // Test with difference of 00.00.00
        assertEquals(Integer.valueOf(0), BuildInfo.compareVersions(new int[]{9,9,99}, new int[]{9,9,99}));
        assertEquals(Integer.valueOf(0), BuildInfo.compareVersions(new int[]{9,99,99}, new int[]{9,99,99}));
        assertEquals(Integer.valueOf(0), BuildInfo.compareVersions(new int[]{99,99,99}, new int[]{99,99,99}));

        // Test with difference of 00.00.01 (i.e. test diff in subminor version)
        assertEquals(Integer.valueOf(1), BuildInfo.compareVersions(new int[]{9,11,12}, new int[]{9,11,11}));
        assertEquals(Integer.valueOf(-1), BuildInfo.compareVersions(new int[]{9,11,12}, new int[]{9,11,13}));

        // Test with difference of 00.01.00 (i.e. test diff in minor version)
        assertEquals(Integer.valueOf(1), BuildInfo.compareVersions(new int[]{9,12,11}, new int[]{9,11,11}));
        assertEquals(Integer.valueOf(-1), BuildInfo.compareVersions(new int[]{9,12,11}, new int[]{9,13,11}));

        // Test with difference of 01.00.00 (i.e. test diff in major version)
        assertEquals(Integer.valueOf(1), BuildInfo.compareVersions(new int[]{9,11,11}, new int[]{8,11,11}));
        assertEquals(Integer.valueOf(-1), BuildInfo.compareVersions(new int[]{9,11,11}, new int[]{10,11,11}));

        // Test boundaries
        assertEquals(Integer.valueOf(-1), BuildInfo.compareVersions(new int[]{9,99,99}, new int[]{10,0,0}));
        assertEquals(Integer.valueOf(1), BuildInfo.compareVersions(new int[]{10,0,0}, new int[]{9,99,99}));

        // Test for invalid input
        assertEquals(null, BuildInfo.compareVersions(new int[] {}, new int[] {}) );
        assertEquals(null, BuildInfo.compareVersions(new int[] {1,2,3,4,5}, new int[] {1,2,3,4,5}) );
    }

    @Test
    public void testDiffBetweenMajorVersions() {
        // Test with difference of 00.00.01 (i.e. test diff in subminor version)
        assertEquals(Integer.valueOf(0), BuildInfo.diffBetweenMajorVersions("9.02.01", "9.02.00"));
        assertEquals(Integer.valueOf(0), BuildInfo.diffBetweenMajorVersions("9.02.00", "9.02.01"));
        assertEquals(Integer.valueOf(0), BuildInfo.diffBetweenMajorVersions("9.02.01", "9.02.01"));

        // Test with difference of 00.01.00 (i.e. test diff in minor version)
        assertEquals(Integer.valueOf(0), BuildInfo.diffBetweenMajorVersions("9.02.01", "9.01.01"));
        assertEquals(Integer.valueOf(0), BuildInfo.diffBetweenMajorVersions("9.02.01", "9.03.01"));
        assertEquals(Integer.valueOf(0), BuildInfo.diffBetweenMajorVersions("9.2.01", "9.2.01"));

        // Test edge cases for major version difference of 0
        assertEquals(Integer.valueOf(0), BuildInfo.diffBetweenMajorVersions("9.0.0", "9.99.99"));
        assertEquals(Integer.valueOf(0), BuildInfo.diffBetweenMajorVersions("9.99.99", "9.0.0"));

        // Test with difference of 01.00.00 (i.e. test diff in major version = +/- 1)
        assertEquals(Integer.valueOf(-1), BuildInfo.diffBetweenMajorVersions("9.02.01", "10.02.01"));
        assertEquals(Integer.valueOf(1), BuildInfo.diffBetweenMajorVersions("10.02.01", "9.02.01"));
        assertEquals(Integer.valueOf(1), BuildInfo.diffBetweenMajorVersions("9.99.99", "8.99.99"));
        assertEquals(Integer.valueOf(-1), BuildInfo.diffBetweenMajorVersions("8.99.99", "9.99.99"));

        // Test edge cases for major version difference = +/- 1
        assertEquals(Integer.valueOf(-1), BuildInfo.diffBetweenMajorVersions("9.99.99", "10.00.00"));
        assertEquals(Integer.valueOf(1), BuildInfo.diffBetweenMajorVersions("10.00.00", "9.99.99"));

        // Test with difference of 02.00.00 (i.e. test diff in major version = +/- 2)
        assertEquals(Integer.valueOf(-2), BuildInfo.diffBetweenMajorVersions("9.2.01", "11.2.01"));
        assertEquals(Integer.valueOf(2), BuildInfo.diffBetweenMajorVersions("11.2.01", "9.2.01"));
        assertEquals(Integer.valueOf(2), BuildInfo.diffBetweenMajorVersions("9.99.99", "7.99.99"));
        assertEquals(Integer.valueOf(-2), BuildInfo.diffBetweenMajorVersions("7.99.99", "9.99.99"));

        // Test edge cases for major version difference = +/- 2
        assertEquals(Integer.valueOf(-2), BuildInfo.diffBetweenMajorVersions("9.99.99", "11.00.00"));
        assertEquals(Integer.valueOf(2), BuildInfo.diffBetweenMajorVersions("11.00.00", "9.99.99"));

        // Test with difference of 03.00.00 or greater (i.e. test diff in major version > +/- 2)
        assertEquals(Integer.valueOf(-3), BuildInfo.diffBetweenMajorVersions("9.2.01", "12.2.1"));
        assertEquals(Integer.valueOf(3), BuildInfo.diffBetweenMajorVersions("12.2.01", "9.2.01"));
        assertEquals(Integer.valueOf(3), BuildInfo.diffBetweenMajorVersions("9.99.99", "6.99.99"));
        assertEquals(Integer.valueOf(-3), BuildInfo.diffBetweenMajorVersions("9.99.99", "12.99.99"));

        // Test edge cases for major version difference > +/- 2
        assertEquals(Integer.valueOf(-3), BuildInfo.diffBetweenMajorVersions("9.99.99", "12.0.0"));
        assertEquals(Integer.valueOf(3), BuildInfo.diffBetweenMajorVersions("11.00.00", "8.99.99"));

        // Test for invalid input
        assertEquals(null, BuildInfo.diffBetweenMajorVersions("9.2.1", "blahblah"));
        assertEquals(null, BuildInfo.diffBetweenMajorVersions("this is an invalid version string", "9.2.1"));
    }

    @Test
    public void testCompareVersionsWithStringParameters() {
        // Test with difference of 00.00.00
        assertEquals(Integer.valueOf(0), BuildInfo.compareVersions("9.9.99", "9.9.99"));
        assertEquals(Integer.valueOf(0), BuildInfo.compareVersions("9.99.99", "9.99.99"));
        assertEquals(Integer.valueOf(0), BuildInfo.compareVersions("99.99.99", "99.99.99"));

        // Test with difference of 00.00.01 (i.e. test diff in subminor version)
        assertEquals(Integer.valueOf(1), BuildInfo.compareVersions("9.02.01", "9.02.00"));
        assertEquals(Integer.valueOf(-1), BuildInfo.compareVersions("9.02.01", "9.02.02"));

        // Test with difference of 00.01.00 (i.e. test diff in minor version)
        assertEquals(Integer.valueOf(1), BuildInfo.compareVersions("9.02.01", "9.01.01"));
        assertEquals(Integer.valueOf(-1), BuildInfo.compareVersions("9.02.01", "9.03.01"));

        // Test with difference of 01.00.00 (i.e. test diff in major version)
        assertEquals(Integer.valueOf(1), BuildInfo.compareVersions("9.02.01", "8.01.01"));
        assertEquals(Integer.valueOf(-1), BuildInfo.compareVersions("9.02.01", "10.02.01"));

        // Test boundaries
        assertEquals(Integer.valueOf(-1), BuildInfo.compareVersions("9.99.99", "10.00.00"));
        assertEquals(Integer.valueOf(1), BuildInfo.compareVersions("10.00.00", "9.99.99"));

        // Test 2-digit version subparts with leading zero
        assertEquals(Integer.valueOf(0), BuildInfo.compareVersions("9.2.1", "9.2.01"));
        assertEquals(Integer.valueOf(0), BuildInfo.compareVersions("9.2.1", "9.02.1"));
        assertEquals(Integer.valueOf(0), BuildInfo.compareVersions("9.2.1", "09.2.1"));

        // Test for invalid input
        assertEquals(null, BuildInfo.compareVersions("9.2.1", "blahblah"));
        assertEquals(null, BuildInfo.compareVersions("this is an invalid version string", "9.2.1"));
    }

    @Test
    public void testisGatewayVersionCompatibleWithDBVersion() {
        // Test for when gateway version = db version
        assertEquals(true, BuildInfo.isGatewayVersionCompatibleWithDBVersion("9.2.01", "9.2.01"));

        // Test: when gateway version > db version for each subpart (x.y.z)
        assertEquals(false, BuildInfo.isGatewayVersionCompatibleWithDBVersion("9.2.01", "9.2.00"));
        assertEquals(false, BuildInfo.isGatewayVersionCompatibleWithDBVersion("9.2.01", "9.1.01"));
        assertEquals(false, BuildInfo.isGatewayVersionCompatibleWithDBVersion("9.2.01", "8.2.01"));

        // Test: when gateway version < db version for each subpart (x.y.z)
        assertEquals(true, BuildInfo.isGatewayVersionCompatibleWithDBVersion("9.2.01", "9.2.02"));
        assertEquals(true, BuildInfo.isGatewayVersionCompatibleWithDBVersion("9.2.01", "9.3.01"));
        assertEquals(true, BuildInfo.isGatewayVersionCompatibleWithDBVersion("9.2.01", "10.2.01"));
        //assertEquals(true, BuildInfo.isGatewayVersionCompatibleWithDBVersion("9.2.01", "11.9.99"));

        // Test: when db major version > (gateway major version + 2) for each subpart (x.y.z)
        assertEquals(false, BuildInfo.isGatewayVersionCompatibleWithDBVersion("9.2.01", "12.0.00"));
        assertEquals(false, BuildInfo.isGatewayVersionCompatibleWithDBVersion("9.2.01", "12.2.02"));
        assertEquals(false, BuildInfo.isGatewayVersionCompatibleWithDBVersion("9.2.01", "12.3.01"));
        //assertEquals(false, BuildInfo.isGatewayVersionCompatibleWithDBVersion("9.2.01", "13.2.01"));

        // Test: edge cases
        assertEquals(true, BuildInfo.isGatewayVersionCompatibleWithDBVersion("9.2.00", "9.2.01"));
        assertEquals(false, BuildInfo.isGatewayVersionCompatibleWithDBVersion("9.2.00", "9.1.99"));
        assertEquals(true, BuildInfo.isGatewayVersionCompatibleWithDBVersion("9.2.00", "11.99.99"));
        assertEquals(false, BuildInfo.isGatewayVersionCompatibleWithDBVersion("9.2.00", "12.0.00"));

        // Test the boundaries for "database version > (gateway version + 2)"
        assertEquals(true, BuildInfo.isGatewayVersionCompatibleWithDBVersion("9.0.00", "11.99.99"));
        assertEquals(true, BuildInfo.isGatewayVersionCompatibleWithDBVersion("9.99.99", "11.99.99"));
        assertEquals(false, BuildInfo.isGatewayVersionCompatibleWithDBVersion("9.0.00", "12.0.0"));
        assertEquals(false, BuildInfo.isGatewayVersionCompatibleWithDBVersion("9.99.99", "12.0.0"));

        // Test the boundaries for "database version < gateway version"
        assertEquals(false, BuildInfo.isGatewayVersionCompatibleWithDBVersion("9.0.00", "8.99.99"));
        assertEquals(false, BuildInfo.isGatewayVersionCompatibleWithDBVersion("9.99.99", "9.99.98"));

        // Test for invalid input
        assertEquals(false, BuildInfo.isGatewayVersionCompatibleWithDBVersion("9.2.1", "blahblah"));
        assertEquals(false, BuildInfo.isGatewayVersionCompatibleWithDBVersion("this is an invalid version string", "9.2.1"));
    }
}
