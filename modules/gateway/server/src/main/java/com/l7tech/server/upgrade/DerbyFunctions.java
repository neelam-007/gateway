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
    public static byte[] toGoid(long high, long low) {
        return new Goid(high, low).getBytes();
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