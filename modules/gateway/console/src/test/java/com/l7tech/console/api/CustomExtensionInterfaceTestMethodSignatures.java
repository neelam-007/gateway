package com.l7tech.console.api;

import org.junit.Assert;

import java.util.List;
import java.util.Map;

/**
 * Combinations of method signatures (return and parameter data types) supported by the Custom Extension Interface.
 */

public interface CustomExtensionInterfaceTestMethodSignatures {
    public final static String FAIL_ARGS = "failArgs";
    public final static String FAIL_RETURN = "failReturn";
    public final static String FAIL_RETURN_AND_ARGS = "failReturnAndArgs";

    // supported: primitives, String (arrays okay)
    @SuppressWarnings("unused")
    public long successTypes(boolean booleanArg, byte byteArg,  short shortArg, int intArg, long longArg);

    @SuppressWarnings("unused")
    public double[] successTypes(float[] floatArrayArg, double[] doubleArrayArg, char[] charArrayArg, String[] stringArrayArg);

    @SuppressWarnings("unused")
    public String[] successTypes(String stringArg, String[] stringArrayArg, boolean booleanArg);

    @SuppressWarnings("unused")
    public void successTypes(String[] stringArrayArg);

    @SuppressWarnings("unused")
    public void successTypes();

    // unsupported: anything else (e.g. Assert), unit test will look for "failArgs", or "failReturn", or "failReturnAndArgs" to start the method name
    @SuppressWarnings("unused")
    public Assert failReturnAndArgsWithUnsupportedTypes(int intArg, Assert assertArg);

    @SuppressWarnings("unused")
    public Assert failReturnWithUnsupportedTypes(String stringArg, short shortArg);

    @SuppressWarnings("unused")
    public String failArgsWithUnsupportedTypes(float floatArg, Assert assertArg);

    @SuppressWarnings("unused")
    public List failReturnWithUnsupportedTypes();

    @SuppressWarnings("unused")
    public void failArgsWithUnsupportedTypes(String stringArg, Map<String, String> mapStringStringArg, short shortArg);

    @SuppressWarnings("unused")
    public List<String> failReturnAndArgsWithUnsupportedGenericsAndTypes(List<String> listStringArg);
}
