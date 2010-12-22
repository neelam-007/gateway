/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.util;

import java.net.*;
import java.util.Enumeration;
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

    /** Pattern that matches syntax (but not numeric sematics) of a valid IPv4 network address. */
    private static final Pattern IPV4_PAT = Pattern.compile("\\d{1,3}(?:\\.\\d{1,3}(?:\\.\\d{1,3}(?:\\.\\d{1,3})?)?)?(?:/\\d{1,2})?");
    private static final Pattern validIpAddressPattern = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)\\.(\\d+)$");

    private static final Pattern mightBeIpv6AddressPattern = Pattern.compile("^\\[?[a-fA-F0-9]+\\:[a-fA-F0-9:]+(?:\\d+\\.\\d+\\.\\d+\\.\\d+)?\\]?$");
    private static final int NO_EXPLICIT_IPV6_PREFIX = 129;


    static {
        InetAddress lh;
        try {
            lh = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            // Misconfigured local hostname or DNS -- fall back to 127.0.0.1 or ::1
            lh = getAddress(getLocalHostAddress());
            logger.log(Level.WARNING, "Unable to get localhost: " + e.getMessage() + " falling back to: " + lh, ExceptionUtils.getDebugException(e));
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

    /**
     * Non-throwing version of InetAddress.getLocalHost().getHostName().
     *
     * Returns the configured local host's name, even if it is not resolvable,
     * in which case it is extracted from the underlying error message.
     * This is an implementation-specific hack.
     *
     * @return the local host's name
     */
    public static String getLocalHostName() {
        String localHostName;
        try {
            localHostName = InetAddress.getLocalHost().getCanonicalHostName();
        } catch (UnknownHostException e) {
            localHostName = e.getMessage().split(": ")[0];
        }
        return localHostName;
    }

    public static String getLocalHostAddress() {
        return isUseIpv6() ? getLocalHostAddress6() : getLocalHostAddress4();
    }

    public static String getLocalHostUrlAddress() {
        return isUseIpv6() ? "[" + getLocalHostAddress6() + "]" : getLocalHostAddress4();
    }

    public static String getLocalHostAddress4() {
        return "127.0.0.1";
    }

    public static String getLocalHostAddress6() {
        return "::1";
    }

    public static String getAnyHostAddress() {
        return isUseIpv6() ? getAnyHostAddress6() : getAnyHostAddress4();
    }

    public static String getAnyHostAddress4() {
        return "0.0.0.0";
    }

    public static String getAnyHostAddress6() {
        return "::";
    }

    public static boolean isAnyHostAddress(String address) {
        InetAddress addr = getAddress(address);
        return "*".equals(address) || (addr != null && addr.isAnyLocalAddress());
    }

    /**
     * Verifies if the provided string parameter is a valid IPv4 address.
     *
     * @param address the string to check
     * @return true if the provided string is a valid IPv4 address, false otherwise.
     */
    public static boolean isValidIpv4Address(String address) {

        if (address == null) return false;

        Matcher matcher = validIpAddressPattern.matcher(address);

        if (matcher.matches())
        {
            //at least it's got a sane format.
            int start;
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
     * Non-throwing, no-lookup version of InetAddress.getByName()
     *
     * @param address the address string to parse into a InetAddress object; IPv6 addresses must not include brackets
     * @return the InetAddress, or null if the supplied address string is not a valid IPv4 or IPv6 address
     */
    public static InetAddress getAddress(String address) {
        try {
            return isValidIpv4Address(address) ? Inet4Address.getByName(address):
                   getIpv6Address(address);
        } catch (UnknownHostException e) {
            return null; // should not happen after isValid
        }
    }
    /**
     * @param address an IPv6 address to be tested, in string literal representation, without brackets 
     * @return true if the provided string is a valid IPv6 address, false otherwise
     */
    public static boolean isValidIpv6Address(String address) {
        return hasAtLeastTwoColons(address) && getIpv6Address(address) != null;
    }

    private static boolean hasAtLeastTwoColons(String address) {
        if (address == null) return false;
        int colons = 0;
        for(int i=0; i<address.length(); i++) {
            if (address.charAt(i) == ':') colons++;
            if (colons >= 2) return true;
        }
        return false;
    }

    /**
     * Parse the provided string into a Inet6Address, without performing any lookups.
     *
     * @param address an IPv6 address to be parsed, in string literal representation, without brackets
     * @return a Inet6Address or null if the provided string is not a valid IPv6 one.
     *
     */
    public static Inet6Address getIpv6Address(String address) {
        // prevent InetAddress.getByName from doing a lookup
        try {
            if (address != null && address.length() > 0) {
                InetAddress ipv6 = InetAddress.getByName("[" + address + "]");
                return ipv6 instanceof Inet6Address ? (Inet6Address) ipv6 : null;
            } else {
                return null;
            }
        } catch (UnknownHostException e) {
            return null;
        }
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
     * The final octet of the IPv4 address is mapped to the least significant byte of the long.
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
     * @param pattern a pattern in a form similar to "24", "24.0.0.0/8", or "24.0/8",
     *                or 22:, 22:/64, 22::/64. Required.
     * @param addr  IPv4 or IPv6 address to check.
     * @return true if the specified address matches the specified pattern, false if the pattern is invalid or doesn't match the address
     */
    public static boolean patternMatchesAddress(String pattern, InetAddress addr) {
        if (isValidIpv4Pattern(pattern)) {
            return addr instanceof Inet4Address && patternMatchesAddress4(pattern, (Inet4Address) addr);
        } else if (isValidIpv6Pattern(pattern)) {
            return addr instanceof Inet6Address && patternMatchesAddress6(pattern, (Inet6Address) addr);
        } else {
            logger.log(Level.WARNING, "Invalid IP address pattern: " + pattern);
            return false;
        }
    }

    private static boolean patternMatchesAddress4(String pattern, Inet4Address addr4) {
        int numeric = (int) ipv4ToLong(addr4);

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

    private static boolean patternMatchesAddress6(String pattern, Inet6Address inet6Address) {
        if (pattern == null) return false;

        String patternAddress = getIpv6AddressForPattern(pattern);
        if (patternAddress == null) return false;

        String[] addrAndMask = pattern.split("/", 2);
        int prefix = addrAndMask.length > 1 ? Integer.parseInt(addrAndMask[1]) : NO_EXPLICIT_IPV6_PREFIX;

        try {
            byte[] addrBytes = inet6Address.getAddress();
            byte[] patternBytes = InetAddress.getByName("[" + patternAddress + "]").getAddress();
            if (addrBytes == null || patternBytes == null || addrBytes.length != patternBytes.length )
                return false;

            for(int i = addrBytes.length-1; i>=0; i--) {
                if (prefix == NO_EXPLICIT_IPV6_PREFIX && patternBytes[i] == 0 || prefix / 8 < i)
                    continue;

                byte currentByteMask = (byte) (255 << Math.max( (i+1) * 8 - prefix, 0 ));
                if ( (addrBytes[i] & currentByteMask) != (patternBytes[i] & currentByteMask) )
                    return false;
            }
            return true;

        } catch (UnknownHostException e) {
            logger.log(Level.WARNING, "Invalid network interface address pattern: " + pattern); // shouldn't happen
            return false;
        }
    }

    /**
     * Check whether the specified string looks like it might be an IP address.
     *
     * @param str a string that might be an IPv4 dotted-quad or a colon-delimited IPv6 network address.
     * @return true if the specified string looks like an IPv4 or IPv6 network address.
     */
    public static boolean looksLikeIpAddressV4OrV6(String str) {
        return looksLikeIpv4Address(str) || looksLikeIpv6Address(str);
    }

    public static boolean looksLikeIpv6Address(String str) {
        return mightBeIpv6AddressPattern.matcher(str).matches();
    }

    public static boolean looksLikeIpv4Address(String str) {
        return validIpAddressPattern.matcher(str).matches();
    }

    /**
     * Check if the given hostname resolves to an IP address for the local system.
     *
     * @param hostname The hostname to check
     * @return True if the hostname resolves to a local IP
     */
    public static boolean isLocalSystemAddress( final String hostname ) {
        boolean isLocal = false;

        try {
            InetAddress addr = InetAddress.getByName( hostname );
            if ( addr.isLoopbackAddress() ) {
                isLocal = true;
            } else {
                NetworkInterface ni = NetworkInterface.getByInetAddress( addr );
                isLocal = ni!=null;
            }
        } catch (SocketException e) {
            // assume not local
        } catch (UnknownHostException e) {
            // assume not local
        }

        return isLocal;
    }

    public static boolean isIpv4Enabled() {
        return hasInterfaceWithAddressType(Inet4Address.class);
    }

    public static boolean isIpv6Enabled() {
        return hasInterfaceWithAddressType(Inet6Address.class);
    }

    static boolean hasInterfaceWithAddressType(Class<? extends InetAddress> inetAddressType) {
        Enumeration<NetworkInterface> interfaces;
        try {
            interfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            logger.log(Level.WARNING, "Error getting networkg interfaces: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            return false;
        }

        while(interfaces.hasMoreElements()) {
            NetworkInterface netif = interfaces.nextElement();
            Enumeration<InetAddress> addrs = netif.getInetAddresses();
            while (addrs.hasMoreElements()) {
                if(addrs.nextElement().getClass().isAssignableFrom(inetAddressType)) return true;
            }
        }

        return false;
    }

    public static boolean isUseIpv6() {
        return InetAddressUtil.isIpv6Enabled() && ! SyspropUtil.getBoolean("java.net.preferIPv4Stack");
    }

    public static boolean isLoopbackAddress(String ipAddress) {
        if (! isValidIpv4Address(ipAddress) && ! isValidIpv6Address(ipAddress))
            return false;

        try {
            return InetAddress.getByName(ipAddress).isLoopbackAddress();
        } catch (UnknownHostException e) {
            // shouldn't happen, we already made sure ipAddress is valid
            return false;
        }
    }

    public static boolean isAnyLocalAddress(String ipAddress) {
        if (! isValidIpv4Address(ipAddress) && ! isValidIpv6Address(ipAddress))
            return false;

        try {
            return InetAddress.getByName(ipAddress).isLoopbackAddress();
        } catch (UnknownHostException e) {
            // shouldn't happen, we already made sure ipAddress is valid
            return false;
        }
    }

    public static boolean isValidIpv4Pattern(String pattern) {
        return IPV4_PAT.matcher(pattern).matches();
    }

    public static boolean isValidIpv6Pattern(String pattern) {
        return getIpv6AddressForPattern(pattern) != null;
    }

    public static String getIpv6AddressForPattern(String pattern) {
        if (pattern == null || pattern.indexOf(':') == -1) return null;

        String[] addrAndPrefix = pattern.split("/", 2);

        try {
            if (addrAndPrefix.length > 1) Integer.parseInt(addrAndPrefix[1]);
        } catch (NumberFormatException e) {
            return null;
        }

        if (InetAddressUtil.isValidIpv6Address(addrAndPrefix[0])) return addrAndPrefix[0];

        String patternAddress = addrAndPrefix[0] + (addrAndPrefix[0].charAt(addrAndPrefix[0].length()-1)==':' ? ":" : "::"); // fill with zeroes
        return isValidIpv6Address(patternAddress) ? patternAddress : null;
    }

    public static byte[] getNetworkPrefix(Inet6Address ipv6addr, short prefixLength) {
        byte[] addressBytes = ipv6addr.getAddress();
        byte[] networkBytes = new byte[prefixLength/8 + ((prefixLength % 8) > 0 ? 1 : 0) ];

        for(int i = 0; i < networkBytes.length; i++) {
            networkBytes[i] = addressBytes[i];
        }

        // last byte
        int lastByteMask = prefixLength % 8;
        if (lastByteMask > 0) {
            networkBytes[networkBytes.length -1] = (byte) (addressBytes[networkBytes.length -1] & (0xFF << (8-lastByteMask)) );
        }

        return networkBytes;
    }

    /**
     * If the provided string parameter is a IPv6 address, returns it enclosed in square brackets so that it can be used within URL, per RFC2732.
     * Otherwise the provided host (IPv4 address) is returned back.
     */
    public static String getHostForUrl(String hostMaybeAddress) {
        return isValidIpv6Address(hostMaybeAddress) ? "[" + hostMaybeAddress + "]" : hostMaybeAddress;
    }

    /**
     * Parses the host and port from a "host[:port]" string.
     *
     * If the host is an IPv6 literal, the returned host part is enclosed in square brackets.
     * If the port is not present, the provided default port is returned.
     *
     * @param hostAndPossiblyPort string containing a host and optionally a port (delimited from the host part with ":")
     * @param defaultPort the port to be returned if the first parameter did not contain a port
     * @return the host and port determined as described above
     */
    public static Pair<String,String> getHostAndPort(String hostAndPossiblyPort, String defaultPort) {
        String host;
        String port = defaultPort;

        int lastColon = hostAndPossiblyPort.lastIndexOf(':');
        if (lastColon < 0) {
            host = hostAndPossiblyPort;
        } else if ( hostAndPossiblyPort.startsWith("[")) {
            if (hostAndPossiblyPort.lastIndexOf(']') == lastColon -1 ) {
                host = hostAndPossiblyPort.substring(0, lastColon);
                try {
                    port = hostAndPossiblyPort.substring(lastColon +1, hostAndPossiblyPort.length());
                } catch (ArrayIndexOutOfBoundsException e2) {
                    // use default port
                }
            } else {
                host = hostAndPossiblyPort;
            }
        } else {
            host = hostAndPossiblyPort.substring(0, lastColon);
            try {
                port = hostAndPossiblyPort.substring(lastColon + 1, hostAndPossiblyPort.length());
            } catch (ArrayIndexOutOfBoundsException e2) {
                // use default port
            }
        }
        return new Pair<String, String>(host, port);
    }

    /**
     * Proper, non-blocking URL comparison. Similar to URL.equals
     * but without attempting to resolve host names.
     *
     * Two URL objects are equal if they have the same protocol, host,
     * port number on the host, and the same file and fragment of the file.
     *
     * Different representations of the same IP address literals in the hostname
     * will compare equal. No path normalization is performed.
     *
     * @param url1 the first URL to compare
     * @param url2 the second URL to compare
     * @return true if the URL are equal as described above, false otherwise
     *             (or if either of the provided strings is not a valid URL)
     * @see java.net.URL#equals(Object)
     */
    public static boolean isEqualUrl(String url1, String url2) {
        URL u1, u2;
        try {
            u1 = new URL(url1);
            u2 = new URL(url2);
        } catch (MalformedURLException e) {
            return false;
        }

        if ( ! isEqualIgnoreCaseString(u1.getProtocol(), u2.getProtocol()) )
            return false;

        String host1 = u1.getHost();
        String host2 = u2.getHost();
        String host1address = getHostAddress(host1);
        if (host1address != null) {
            if ( ! getAddress(host1address).equals(getAddress(getHostAddress(host2))) )
                return false;
        } else {
            if ( ! isEqualIgnoreCaseString(host1, host2) )
                return false;
        }

        int port1 = u1.getPort();
        if (port1 == -1) port1 = u1.getDefaultPort();
        int port2 = u2.getPort();
        if (port2 == -1) port2 = u2.getDefaultPort();
        if (port1 != port2) return false;

        if ( ! isEqualString(u1.getFile(), u2.getFile()) )
            return false;

        if ( ! isEqualString(u1.getRef(), u2.getRef()) )
            return false;

        return true;
    }

    /**
     * @param url1 the first URL to compare
     * @param url2 the second URL to compare
     * @return true if second URL's file is a subcomponent of the first URL's file
     */
    public static boolean isSubUrl(String url1, String url2) {
        URL u1, u2;
        try {
            u1 = new URL(url1);
            u2 = new URL(url2);
        } catch (MalformedURLException e) {
            return false;
        }


        if (u1.getFile() != null) {
            if (u2.getFile() == null || ! u2.getFile().startsWith(u1.getFile())) {
                return false;
            }
        }

        try {
            return isEqualUrl(url1, (new URL(u2.getProtocol(), u2.getHost(), u2.getPort(), u1.getFile())).toString());
        } catch (MalformedURLException e) {
            return false;
        }
    }

    public static String formatMac( final byte[] macAddr ) {
        String hex = HexUtils.hexDump(macAddr).toUpperCase();
        StringBuilder hexBuilder = new StringBuilder();
        for ( int i=0; i < hex.length(); i++ ) {
            if ( i>0 && i%2==0 ) {
                hexBuilder.append(':');
            }
            hexBuilder.append(hex.charAt(i));
        }
        return hexBuilder.toString();
    }

    private static String getHostAddress(String maybeIpAddress) {
        if (isValidIpv4Address(maybeIpAddress)) return maybeIpAddress;
        if (maybeIpAddress != null && maybeIpAddress.length() > 2 &&
            maybeIpAddress.charAt(0) == '[' && maybeIpAddress.charAt(maybeIpAddress.length()-1) == ']' &&
            isValidIpv6Address(maybeIpAddress.substring(1, maybeIpAddress.length() - 1)) ) {
            return maybeIpAddress.substring(1, maybeIpAddress.length() - 1);
        }
        return null;
    }

    private static boolean isEqualString(String s1, String s2) {
        return s1 == null ? s2 == null : s1.equals(s2);
    }

    private static boolean isEqualIgnoreCaseString(String s1, String s2) {
        return s1 == null ? s2 == null : s1.equalsIgnoreCase(s2);
    }

}
