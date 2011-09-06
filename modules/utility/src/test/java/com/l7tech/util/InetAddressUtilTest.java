package com.l7tech.util;

import static com.l7tech.util.InetAddressUtil.patternMatchesAddress;
import static junit.framework.Assert.*;

import com.l7tech.test.BugNumber;
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

    @BugNumber(9683)
    @Test
    public void testGetHostAndPort() {
        // IPv4 host tests
        assertEquals( "IPv4 address 0.0.0.0", "0.0.0.0", InetAddressUtil.getHostAndPort("0.0.0.0", "8443").left );
        assertEquals( "IPv4 address 0.0.0.0", "255.255.255.255", InetAddressUtil.getHostAndPort("255.255.255.255", "8443").left );

        // IPv4 host:port tests
        assertEquals( "IPv4 address 0.0.0.0:8443", "0.0.0.0", InetAddressUtil.getHostAndPort("0.0.0.0:8443", "8443").left );
        assertEquals( "IPv4 address 255.255.255.255:8443", "255.255.255.255", InetAddressUtil.getHostAndPort("255.255.255.255:8443", "8443").left );

        // IPv6 host only tests
        assertEquals( "IPv6 address ::", "[::]", InetAddressUtil.getHostAndPort("::", "8443").left );
        assertEquals( "IPv6 address ::1", "[::1]", InetAddressUtil.getHostAndPort("::1", "8443").left );
        assertEquals( "IPv6 address 2222::f", "[2222::f]", InetAddressUtil.getHostAndPort("2222::f", "8443").left );
        assertEquals( "IPv6 address abf3:FF2:0::00:23", "[abf3:FF2:0::00:23]", InetAddressUtil.getHostAndPort("abf3:FF2:0::00:23", "8443").left );
        assertEquals( "IPv6 address cafe:babe:0000::4343:1.2.3.4", "[cafe:babe:0000::4343:1.2.3.4]", InetAddressUtil.getHostAndPort("cafe:babe:0000::4343:1.2.3.4", "8443").left );
        assertEquals( "IPv6 address 2001:0db8:85a3:08d3:1319:8a2e:0370:7348", "[2001:0db8:85a3:08d3:1319:8a2e:0370:7348]", InetAddressUtil.getHostAndPort("2001:0db8:85a3:08d3:1319:8a2e:0370:7348", "8443").left );

        // IPv6 host:port tests
        assertEquals( "IPv6 address [::]:8443", "[::]", InetAddressUtil.getHostAndPort("[::]:8443", "8443").left );
        assertEquals( "IPv6 address [::1]:8443", "[::1]", InetAddressUtil.getHostAndPort("[::1]:8443", "8443").left );
        assertEquals( "IPv6 address [2222::f]:8443", "[2222::f]", InetAddressUtil.getHostAndPort("[2222::f]:8443", "8443").left );
        assertEquals( "IPv6 address [abf3:FF2:0::00:23]:8443", "[abf3:FF2:0::00:23]", InetAddressUtil.getHostAndPort("[abf3:FF2:0::00:23]:8443", "8443").left );
        assertEquals( "IPv6 address [cafe:babe:0000::4343:1.2.3.4]:8443", "[cafe:babe:0000::4343:1.2.3.4]", InetAddressUtil.getHostAndPort("[cafe:babe:0000::4343:1.2.3.4]:8443", "8443").left );
        assertEquals( "IPv6 address [2001:0db8:85a3:08d3:1319:8a2e:0370:7348]:8443", "[2001:0db8:85a3:08d3:1319:8a2e:0370:7348]", InetAddressUtil.getHostAndPort("[2001:0db8:85a3:08d3:1319:8a2e:0370:7348]:8443", "8443").left );
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

    private void ipv4(String in) {
        assertTrue(InetAddressUtil.looksLikeIpv4Address(in));
    }

    private void ipv6(String in) {
        assertTrue(InetAddressUtil.looksLikeIpv6Address(in));
    }

    private void notIp(String in) {
        assertFalse(InetAddressUtil.looksLikeIpAddressV4OrV6(in));
    }

    @Test
    public void testLooksLikeIpAddressV4OrV6() throws Exception {
        ipv4("1.2.3.4");
        ipv4("234.555.222.334");
        ipv4("2344.222.1111.0");
        ipv4("000.000.000.000");
        ipv4("0000.0000.0000.0000");
        ipv6("2001:0db8:85a3:08d3:1319:8a2e:0370:7348");
        ipv6("abf3:FF2:0::00:23");
        ipv6("cafe:babe:0000::4343:1.2.3.4");
        notIp("foo.bar.com");
        notIp("cafe.babe");
        notIp("cafe:babe:food::344");
    }

    @Test
    public void testUrlCompare() throws Exception {
        assertTrue(InetAddressUtil.isEqualUrl("http://bla", "http://bla:80"));
        assertTrue(InetAddressUtil.isEqualUrl("https://bla", "https://bla:443"));
        assertTrue(InetAddressUtil.isEqualUrl("ftp://bla", "ftp://bla:21"));
        assertFalse(InetAddressUtil.isEqualUrl("http://example.com/path#f1", "http://example.com/path#f2"));
        assertFalse(InetAddressUtil.isEqualUrl("HTTP://EXAMPLE.COM/%63", "http://example.com/c"));
        assertTrue(InetAddressUtil.isEqualUrl("http://[::1]", "http://[0:0::000:1]"));
    }

    @Test
    public void testIsAnyHostAddress() {
        assertTrue( "*", InetAddressUtil.isAnyHostAddress("*") );
        assertTrue( "0.0.0.0", InetAddressUtil.isAnyHostAddress("0.0.0.0") );
        assertTrue( "::", InetAddressUtil.isAnyHostAddress("::") );
        assertTrue( "0000:0000:0000:0000:0000:0000:0000:0000", InetAddressUtil.isAnyHostAddress("0000:0000:0000:0000:0000:0000:0000:0000") );

        assertFalse( "1.2.3.4", InetAddressUtil.isAnyHostAddress("1.2.3.4") );
        assertFalse( "0.0.0.1", InetAddressUtil.isAnyHostAddress("0.0.0.1") );
        assertFalse( "::1", InetAddressUtil.isAnyHostAddress("::1") );
        assertFalse( "0000:0000:0000:0000:0000:0000:0000:0001", InetAddressUtil.isAnyHostAddress("0000:0000:0000:0000:0000:0000:0000:0001") );
    }

    @Test
    public void testStripIPv6Brackets() {
        assertNull( "Null address", InetAddressUtil.stripIpv6Brackets( null ) );
        assertEquals( "Localhost address", "::1", InetAddressUtil.stripIpv6Brackets( "::1" ) );
        assertEquals( "Localhost address with brackets", "::1", InetAddressUtil.stripIpv6Brackets( "[::1]" ) );
        assertEquals( "Full address with brackets", "2001:0db8:85a3:08d3:1319:8a2e:0370:7348", InetAddressUtil.stripIpv6Brackets( "[2001:0db8:85a3:08d3:1319:8a2e:0370:7348]" ) );
    }
}
