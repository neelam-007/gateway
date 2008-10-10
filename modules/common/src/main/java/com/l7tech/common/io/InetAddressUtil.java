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
     * @throws IllegalArgumentException if the addres is an IPv6 address.
     */
    public static long ipv4ToLong(InetAddress addr) throws IllegalArgumentException {        
        byte[] a = addr.getAddress();
        if (a.length > 4)
            throw new IllegalArgumentException("IPv6 address not supported");
        return ((0xFFL & a[0]) << 24) |
               ((0xFFL & a[1]) << 16) |
               ((0xFFL & a[2]) <<  8) |
                (0xFFL & a[3]);
    }
}
