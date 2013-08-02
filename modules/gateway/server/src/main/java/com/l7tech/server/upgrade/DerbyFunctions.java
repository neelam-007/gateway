package com.l7tech.server.upgrade;

import com.l7tech.objectmodel.Goid;
import com.l7tech.util.RandomUtil;

import java.util.HashMap;

/**
 * These are functions that are called from derby. See the ssg_embedded.sql functions
 *
 * @author Victor Kazakov
 */
@SuppressWarnings("UnusedDeclaration")
public final class DerbyFunctions {

    private static final HashMap<String, String> variables = new HashMap<>();

    /**
     * Returns the bytes of a Goid made up from the given high and low values.
     *
     * @param high The high long
     * @param low  The low long
     * @return The goid bytes. This can be set to a 'char(16) for bit data' in derby
     */
    public static byte[] toGoid(Long high, Long low) {
        return high != null && low != null ? new Goid(high, low).getBytes() : null;
    }

    /**
     * Returns a hex string representation of the given goid bytes
     *
     * @param bytes The bytes of the goid
     * @return The hex string representation of the goid
     */
    public static String goidToString(byte[] bytes) {
        return bytes != null ? new Goid(bytes).toHexString() : null;
    }

    /**
     * The if null function returns the first parameter if it is not null. If the first parameter is null the second parameter is returned.
     * @param v1 The first parameter
     * @param v2 The second parameter
     * @return The first parameter if it is not null, otherwise the second parameter.
     */
    public static String ifNull(String v1, String v2) {
        return v1 != null ? v1 : v2;
    }

    /**
     * Returns a random long value using the internal random utils.
     *
     * @return A random long value
     */
    public static long randomLong() {
        return RandomUtil.nextLong();
    }

    /**
     * Returns a random long value that in not in the reserved prefix space for a goid.
     *
     * @return A random long value that in not in the reserved prefix space for a goid.
     */
    public static long randomLongNotReserved() {
        long random;
        do {
            random = RandomUtil.nextLong();
        } while (random >= 0 && random < 65536);
        return random;
    }

    /**
     * Sets a variable to the given value.
     *
     * @param key   The key of the variable
     * @param value The value to set it to
     */
    public static void setVariable(String key, String value) {
        variables.put(key, value);
    }

    /**
     * Retrieves a variables value
     *
     * @param key The key of the variable
     * @return The value of the variable. null if not such variable has been set.
     */
    public static String getVariable(String key) {
        return variables.get(key);
    }
}