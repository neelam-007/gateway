package com.l7tech.common.io;

import static com.l7tech.common.io.InetAddressUtil.patternMatchesAddress;
import static junit.framework.Assert.*;
import org.junit.*;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Unit tests for {@link InetAddressUtil}.
 */
public class InetAddressUtilTest {

    @Test
    public void testToLong() throws UnknownHostException {
        assertEquals(3872534927L, InetAddressUtil.ipv4ToLong(InetAddress.getByName("230.210.49.143")));
        assertEquals(2130706433L, InetAddressUtil.ipv4ToLong(InetAddress.getByName("127.0.0.1")));
    }

    @Test
    public void testMakeNetmaskFromBitcount() {
        assertEquals(0x00000000, InetAddressUtil.makeNetmaskFromBitcount(0));
        assertEquals(0x80000000, InetAddressUtil.makeNetmaskFromBitcount(1));
        assertEquals(0xFFFFFF00, InetAddressUtil.makeNetmaskFromBitcount(24));
        assertEquals(0xFFFFFF80, InetAddressUtil.makeNetmaskFromBitcount(25));
        assertEquals(0xFFFFFFF0, InetAddressUtil.makeNetmaskFromBitcount(28));
        assertEquals(0xFFFFFFF8, InetAddressUtil.makeNetmaskFromBitcount(29));
        assertEquals(0xFFFFFFFE, InetAddressUtil.makeNetmaskFromBitcount(31));
        assertEquals(0xFFFFFFFF, InetAddressUtil.makeNetmaskFromBitcount(32));
    }

    @Test(expected=IllegalArgumentException.class)
    public void testMakeNetmaskNegativeBitcount() {
        InetAddressUtil.makeNetmaskFromBitcount(-1);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testMakeNetmaskOutOfRangeBitcount() {
        InetAddressUtil.makeNetmaskFromBitcount(33);
    }

    @Test
    public void testParseDottedDecimalPrefix() {
        assertMatches(new int[] { 255, 255, 255, 255 }, InetAddressUtil.parseDottedDecimalPrefix("255.255.255.255"));
        assertMatches(new int[] { 0, 0, 0, 0 }, InetAddressUtil.parseDottedDecimalPrefix("0.0.0.0"));
        assertMatches(new int[] { 0, 0, 255, 255 }, InetAddressUtil.parseDottedDecimalPrefix("0.0.255.255"));
        assertMatches(new int[] { 192, 168, 4, 253 }, InetAddressUtil.parseDottedDecimalPrefix("192.168.4.253"));
        assertMatches(new int[] { 1, 2, 3, 4 }, InetAddressUtil.parseDottedDecimalPrefix("1.2.3.4.5.6.7.8.9"));
        assertMatches(new int[] { 0, 1, 2, 3 }, InetAddressUtil.parseDottedDecimalPrefix("256.257.258.259"));
    }

    private void assertMatches(int[] expect, byte[] got) {
        for (int i = 0; i < expect.length; i++)
            assertEquals(((byte)(expect[i] & 0xFF)), got[i]);
    }

    private byte[] bytes(int... values) {
        byte[] ret = new byte[values.length];
        for (int i = 0; i < values.length; i++)
            ret[i] = (byte)(values[i] & 0xFF);
        return ret;
    }

    private InetAddress addr(int... values) throws UnknownHostException {
        return InetAddress.getByAddress(bytes(values));
    }
    
    @Test
    public void testPatternMatchesAddress() throws Exception {
        assertTrue(patternMatchesAddress("255", addr(255, 255, 255, 255)));
        assertTrue(patternMatchesAddress("255", addr(255, 255, 0, 255)));
        assertTrue(patternMatchesAddress("255", addr(255, 0, 0, 0)));
        assertTrue(patternMatchesAddress("255", addr(255, 255, 0, 0)));
        assertTrue(patternMatchesAddress("255", addr(255, 121, 0, 0)));
        assertTrue(patternMatchesAddress("43.55", addr(43, 55, 66, 33)));
        assertFalse(patternMatchesAddress("43.55", addr(43, 56, 0, 0)));
        assertTrue(patternMatchesAddress("1.2.3", addr(1, 2, 3, 4)));
        assertFalse(patternMatchesAddress("1.2.3", addr(1, 3, 2, 4)));
        assertTrue(patternMatchesAddress("1.2.3.4", addr(1, 2, 3, 4)));
        assertFalse(patternMatchesAddress("1.2.3.4", addr(1, 3, 2, 5)));
        assertTrue(patternMatchesAddress("32.14.0.0/16", addr(32, 14, 43, 240)));
        assertTrue(patternMatchesAddress("232.255.109.186/31", addr(232, 255, 109, 186)));
        assertTrue(patternMatchesAddress("232.255.109.186/31", addr(232, 255, 109, 187)));
        assertFalse(patternMatchesAddress("232.255.109.186/31", addr(232, 255, 109, 188)));
    }
}
