package com.l7tech.common.util;
/**
 * @author Henry Chan
 * May 2/2004
 *
 * This class is intended to be used as a helper
 * to compute hashCode's for hiberate composited id classes
 * compute
 * should be the primary method here with various polymorphs of it
 *
 * NOTE: This class should eventually be deprecated ... after using
 * hibernate's CodeGenerator
 */
public class HashCode {
    private static final int RANDOM_INT = 29;
    private static final int LONG_SIZE = 32;

    public static int compute(long l, String str) {
        int result;
        result = (int) (l ^ (l >>> LONG_SIZE));
        result = RANDOM_INT * result +
                (int) (str != null ? str.hashCode() : 0);
        return result;
    }

    public static int compute(String[] strArr) {
        int result = 0;
        for (int i = 0; i < strArr.length; i++) {
            if (strArr[i] != null) {
                result += RANDOM_INT * strArr[i].hashCode();
            }
        }
        return result;
    }

    /**
     * Compute a hash code for the specified byte array.  The hashcode is computed using the same algorithm used
     * by String.hashCode():
     * <blockquote><pre>
     * s[0]*31^(n-1) + s[1]*31^(n-2) + ... + s[n-1]
     * </pre></blockquote>
     * @return a hashCode for the specified byte array.
     */
    public static int compute(byte[] b) {
        int h = 0;
        for (int i = 0; i < b.length; i++)
            h = 31*h + b[i];
        return h;
    }
}
