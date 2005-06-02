/**
 * @author emil
 * @version 17-Apr-2005
 */
package com.l7tech.common.security.xml;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 * The enumeration type for XML encryption algorithms.
 */
public final class XencAlgorithm implements Serializable {
    private static int index = 0;
    public static final XencAlgorithm TRIPLE_DES_CBC = new XencAlgorithm(index++, "tripledes-cbc", "http://www.w3.org/2001/04/xmlenc#tripledes-cbc");
    public static final XencAlgorithm AES_128_CBC = new XencAlgorithm(index++, "aes128-cbc", "http://www.w3.org/2001/04/xmlenc#aes128-cbc");
    public static final XencAlgorithm AES_192_CBC = new XencAlgorithm(index++, "aes192-cbc", "http://www.w3.org/2001/04/xmlenc#aes192-cbc");
    public static final XencAlgorithm AES_256_CBC = new XencAlgorithm(index++, "aes256-cbc", "http://www.w3.org/2001/04/xmlenc#aes256-cbc");
    // for readResolve
    private static final XencAlgorithm[] ALGORITHMS = {TRIPLE_DES_CBC, AES_128_CBC, AES_192_CBC, AES_256_CBC};

    private int val;
    private final String shortName;
    private final String xencName;

    /**
     * Private constructor, to support instantiating only the constant
     */
    private XencAlgorithm(int index, String shortName, String xencName) {
        val = index;
        this.shortName = shortName;
        this.xencName = xencName;
    }

    /**
     * @return the short algorithm name (tripledes-cbc, aes128-cbc etc)
     */
    public String getShortName() {
        return shortName;
    }

    /**
     * @return the xml encryption algorithm name such as http://www.w3.org/2001/04/xmlenc#tripledes-cbc
     */
    public String getXencName() {
        return xencName;
    }

    /**
     * @return the array of all algorithms
     */
    public static List getAllAlgorithms() {
        return Arrays.asList(ALGORITHMS);
    }

    /**
     * Serialization support for correct enum resolving.
     */
    private Object readResolve() throws ObjectStreamException {
        return ALGORITHMS[val];
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final XencAlgorithm xencAlgorithm = (XencAlgorithm)o;

        if (val != xencAlgorithm.val) return false;
        if (shortName != null ? !shortName.equals(xencAlgorithm.shortName) : xencAlgorithm.shortName != null) return false;
        if (xencName != null ? !xencName.equals(xencAlgorithm.xencName) : xencAlgorithm.xencName != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = val;
        result = 29 * result + (shortName != null ? shortName.hashCode() : 0);
        result = 29 * result + (xencName != null ? xencName.hashCode() : 0);
        return result;
    }


    public String toString() {
        return "XencAlgorithm{" +
                 "val=" + val +
                 ", shortName='" + shortName + "'" +
                 ", xencName='" + xencName + "'" +
                 "}";
    }
}
