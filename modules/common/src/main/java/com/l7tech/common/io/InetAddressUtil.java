/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.io;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.StringTokenizer;

/**
 * Utility methods for {@link java.net.InetAddress}.
 */
public class InetAddressUtil {
    private static final Logger logger = Logger.getLogger(InetAddressUtil.class.getName());

    private static final InetAddress localHost;
    public static final Pattern validIpAddressPattern = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)\\.(\\d+)$");

    static {
        InetAddress lh;
        try {
            lh = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            // Misconfigured local hostname or DNS -- fall back to hardcoded 127.0.0.1
            logger.log(Level.WARNING, "Unable to get localhost: " + e.getMessage(), e);
            try {
                lh = InetAddress.getByAddress(new byte[] {127, 0, 0, 1});
            } catch (UnknownHostException e1) {
                throw new RuntimeException(e1); // can't happen
            }
        }
        localHost = lh;
    }

    /**
     * Non-throwing version of InetAddress.getLocalHost().  This version will return 127.0.0.1 if the local hostname
     * is bogus or can't be resolved.  The value returned by this method is <em>not</em> guaranteed to be up-to-date
     * if the system hostname changes after the JVM process has started.
     * <p/>
     * Since this method might return 127.0.0.1, it should not be used when ambiguity about which machine's
     * loopback address is itended exists and such ambiguity cannot be tolerated (for example, when this address will be
     * sent to a remote host, which may attempt to connect back to it).
     *
     * @return A reasonable InetAddress to use representing "this here host"; 127.0.0.1 as a last resort.  Never null.
     */
    public static InetAddress getLocalHost() {
        return localHost;
    }

    public static boolean isValidIpAddress(String address) {

        if (address == null) return false;

        Matcher matcher = validIpAddressPattern.matcher(address);

        if (matcher.matches())
        {
            //at least it's got a sane format.
            int start = 0;
            int end = 255;

            for (int i = 1; i <= matcher.groupCount(); ++i) {
                String octetString = matcher.group(i);
                try {
                    int octet = Integer.parseInt(octetString);
                    if (i == 1)
                        start = 1;
                    else
                        start = 0;

                    if (octet < start || octet > end)
                        return false;

                } catch (NumberFormatException e) {
                    return false;
                }

            }
            return true;
        }

        return false;
    }

    /**
     * Pack an IPv4 address into the least significant DWORD of a long.
     * The final octet of the IPv4 address is mapped to least significant byte of the long.
     *
     * @param addr an IP address to pack into a long.  Must be an IPv4 address.
     * @return a long containing the packed IP address.
     * @throws IllegalArgumentException if the address is an IPv6 address.
     */
    public static long ipv4ToLong(InetAddress addr) throws IllegalArgumentException {        
        byte[] a = addr.getAddress();
        return ipv4ToLong(a);
    }

    /**
     * Pack an IPv4 address into the least significant DWORD of a long.
     * The final octet of the IPv4 address is mapped to the least significant byte of ht elong.
     *
     * @param addr an IP address to pack into a long.  Must be an IPv4 address.
     * @return a long containing the packed IP address.
     * @throws IllegalArgumentException if the length of the array is not exactly 4.
     */
    public static long ipv4ToLong(byte[] addr) {
        if (addr.length > 4)
            throw new IllegalArgumentException("IPv6 address not supported");
        if (addr.length < 4)
            throw new IllegalArgumentException("Not enough octets for IPv4 address");
        return ((0xFFL & addr[0]) << 24) |
               ((0xFFL & addr[1]) << 16) |
               ((0xFFL & addr[2]) <<  8) |
                (0xFFL & addr[3]);
    }

    /**
     * Create a 32-bit netmask with the specified number of network bits set.  For example, calling
     * with a bit count of 24 will result in a netmask of -256 (0xFFFFFF00).
     * @param bitcount the number of network bits that should be set in the IPv4 netmask.
     *                 Must be a nonnegative integer less than or equal to 32.
     * @return the requested IPv4 netmask.
     * @throws IllegalArgumentException if bitcount is negative or greater than 32
     */
    public static int makeNetmaskFromBitcount(int bitcount) {
        if (bitcount < 0 || bitcount > 32)
            throw new IllegalArgumentException("Invalid netmask bit count: " + bitcount);
        if (bitcount == 32)
            return 0xFFFFFFFF;
        int netmask = -1;
        netmask >>>= bitcount;
        netmask ^= -1;
        return netmask;
    }

    /**
     * Parse a dotted-decimal IP pattern into bytes.
     * For example, "24.14" should result in { 24, 14, 0, 0 },
     * "192.168.13" should result in { 192, 168, 13, 0 }, and
     * "1.2.3.4.5.6.7.8.9" should result in { 1, 2, 3, 4 }.
     *
     * @param pattern
     * @return
     */
    public static byte[] parseDottedDecimalPrefix(String pattern) {
        byte[] result = new byte[4];
        StringTokenizer st = new StringTokenizer(pattern, ".");
        int idx = 0;
        while (st.hasMoreElements() && idx < 4) {
            String elm = st.nextElement().toString().trim();
            result[idx++] = (byte)(Integer.parseInt(elm) & 0xFF);
        }
        return result;
    }

    /**
     * Check if the specified InetAddress matches the specified address pattern.
     *
     * @param pattern a pattern in a form similar to "24", "24.0.0.0/8", or "24.0/8".  Required.
     * @param addr  the address to check.
     * @return true if the specified address matches the specified pattern.
     */
    public static boolean patternMatchesAddress(String pattern, InetAddress addr) {
        int numeric = (int) ipv4ToLong(addr);

        String[] addrAndMask = pattern.split("\\/", 2);
        byte[] pataddr = parseDottedDecimalPrefix(addrAndMask[0]);
        int netmask;
        if (addrAndMask.length > 1) {
            // Explicit netmask in CIDR format
            netmask = makeNetmaskFromBitcount(Integer.parseInt(addrAndMask[1]));
        } else {
            // Guess implicit netmask based on contiguous octets in address
            if (pataddr[3] == 0 && pataddr[2] == 0 && pataddr[1] == 0)
                netmask = 0xFF000000;
            else if (pataddr[3] == 0 && pataddr[2] == 0)
                netmask = 0xFFFF0000;
            else if (pataddr[3] == 0)
                netmask = 0xFFFFFF00;
            else
                netmask = 0xFFFFFFFF;
        }

        int patint = (int) ipv4ToLong(pataddr);
        return (numeric & netmask) == (patint & netmask);
    }
}
