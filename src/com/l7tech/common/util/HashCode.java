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
       NOT FINISHED!!!
    */
    public static int compute(int [] intArr) {
	return 0;
    }
}
