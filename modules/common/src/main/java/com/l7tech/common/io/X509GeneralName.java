package com.l7tech.common.io;

import com.l7tech.util.InetAddressUtil;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

/**
 * Holds a GeneralName, as used for each component of an X.509 Subject Alternative Name GeneralNames sequence.
 * A GeneralName consists of a type tag and a value.
 */
public class X509GeneralName implements Serializable {
    public enum Type {
        otherName(0, "Other Name"),
        rfc822Name(1, "Email"),
        dNSName(2, "DNS Name"),
        x400Address(3, "X400 Address"),
        directoryName(4, "Directory Name"),
        ediPartyName(5, "EDI Party Name"),
        uniformResourceIdentifier(6, "URI"),
        iPAddress(7, "IP Address"),
        registeredID(8, "Registered ID")
        ;

        private static Type[] BY_TAG = new Type[] {
                otherName,
                rfc822Name,
                dNSName,
                x400Address,
                directoryName,
                ediPartyName,
                uniformResourceIdentifier,
                iPAddress,
                registeredID
        };

        private final int tag;
        private final String userFriendlyName;

        Type(int tag, String name) {
            this.tag = tag;
            this.userFriendlyName = name;
        }

        public int getTag() {
            return tag;
        }

        public String getUserFriendlyName() { return userFriendlyName; }

        public static Type fromTag(int tag) {
            if (tag < 0 || tag >= BY_TAG.length)
                throw new IllegalArgumentException("Unrecognized Type tag");
            return BY_TAG[tag];
        }

        public static Type fromUserName(@NotNull String name) {
            return Arrays.stream(Type.values()).filter(new Predicate<Type>() {
                                                           @Override
                                                           public boolean test(Type type) {
                                                               return type.userFriendlyName.equalsIgnoreCase(name);
                                                           }
                                                       }).findFirst().get();
        }
    }

    private final Type type;
    private final String stringVal;
    private final byte[] derVal;

    public X509GeneralName(Type type, String stringVal) {
        if (type == null || stringVal == null)
            throw new NullPointerException();
        this.type = type;
        this.stringVal = stringVal;
        this.derVal = null;
    }

    public X509GeneralName(Type type, byte[] derVal) {
        if (type == null || derVal == null)
            throw new NullPointerException();
        this.type = type;
        this.derVal = derVal;
        this.stringVal = null;
    }

    /**
     * Create an X509GeneralName from an entry as returned by X509Certificate.getSubjectAlternativeName().
     *
     * @param entry a List with two elements: a tag Integer, and a value which is either a String or a byte[].
     * @throws NullPointerException if entry is null
     * @throws IndexOutOfBoundsException if entry does not contain at least two elements
     * @throws ClassCastException if entry.get(0) is not an Integer or entry.get(1) is not either String or byte[]
     * @throws IllegalArgumentException if the tag Integer is not recognized
     */
    public X509GeneralName(List<?> entry) {
        this.type = Type.fromTag((Integer)entry.get(0));
        Object val = entry.get(1);
        if (val instanceof String) {
            this.stringVal = (String) val;
            this.derVal = null;
        } else if (val instanceof byte[]) {
            this.derVal = (byte[]) val;
            this.stringVal = null;
        } else {
            throw new ClassCastException("GeneralName value must be either String or byte[]; was " + (val == null ? "null" : val.getClass().toString()));
        }
    }

    public Type getType() {
        return type;
    }

    public boolean isString() {
        return stringVal != null;
    }

    public String getStringVal() {
        return stringVal;
    }

    public byte[] getDerVal() {
        return derVal;
    }

    /**
     * Create an X509GeneralName from a DNS hostname
     * @param dnsName
     * @return
     */
    public static X509GeneralName fromDnsName(String dnsName) {
        return new X509GeneralName(Type.dNSName, dnsName);
    }

    /**
     * Create an X509GeneralName from an IPv4 address (dotted decimal) or an IPv6 address (colon delimited).
     *
     * @param ipAddress an IPv4 or IPv6 address in string format.  Required.
     * @return an X509GeneralName instance representing this address.  Never null.
     */
    public static X509GeneralName fromIpAddress(String ipAddress) {
        return new X509GeneralName(Type.iPAddress, ipAddress);
    }

    /**
     * Create an X509GeneralName from a string that may be either a hostname pattern or an IPv4 or IPv6
     * address in string format.
     *
     * @param hostOrIp DNS hostname pattern, IPv4 address in dotted-quad string format,
     *                 or IPv6 address in colon-delimited string format.  Required.
     * @return an X509GeneralName instance for the specified hostname or IP.  Never null.
     */
    public static X509GeneralName fromHostNameOrIp(String hostOrIp) {
        return InetAddressUtil.looksLikeIpAddressV4OrV6(hostOrIp) ? fromIpAddress(hostOrIp) : fromDnsName(hostOrIp);
    }

    /**
     * Produce a list of X509GeneralName instances from an entry list in the format returned by
     * X509Certificate.getSubjectAlternativeName().
     *
     * @param entries the entry array.  May be empty, but must not be null.
     * @return a List of X509GeneralName instances.  May be empty, but never null.
     */
    public static List<X509GeneralName> fromList(Collection<List<?>> entries) {
        List<X509GeneralName> ret = new ArrayList<X509GeneralName>();
        for (List<?> entry : entries) {
            ret.add(new X509GeneralName(entry));
        }
        return ret;
    }

    /**
     * Produce a lsit of X509GeneralName instances from a list of hostname patterns or IP addresses.
     * <P/>
     * For each entry in the list, this method will assume it is an IPv4 or IPv6 address if it contains
     * only hex digits, dots, and colons; otherwise, it will be assumed to be a DNS hostname pattern.
     *
     * @param hostNamesOrIps zero or more DNS hostname patterns, IPv4 addresses in dotted-quad string format,
     *                       or IPv6 addresses in colon-delimited string format.
     * @return a list of X509GeneralName instances corresponding to each entry.  Never null.
     */
    public static List<X509GeneralName> fromHostNamesOrIps(List<String> hostNamesOrIps) {
        List<X509GeneralName> ret = new ArrayList<X509GeneralName>();
        for (String hostOrIp : hostNamesOrIps) {
            ret.add(fromHostNameOrIp(hostOrIp));
        }
        return ret;
    }

    public static Type getTypeFromTag(int tag) {
        return Type.fromTag(tag);
    }
}
