/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.test.performance;

/**
 * Extensions to Japex configuration parameters.
 *
 * @author rmak
 */
public class Constants extends com.sun.japex.Constants {

    // ------------------------ Global input parameters ------------------------

    /** Name of global parameter with the SecureSpan version. */
    public static final String NAME = "layer7.name";

    /** Name of global parameter with the SecureSpan version. */
    public static final String SS_VERSION = "layer7.version";

    /** Name of global parameter that contains additional notes about this Japex report. */
    public static final String NOTES = "layer7.notes";

    // ------------------------ Driver input parameters ------------------------

    /** Name of driver parameter that specifies static methods to invoke during
        {@link com.sun.japex.JapexDriver#initializeDriver initializeDriver}.
        Parameter value is a space-separated list of no-argument static method
        in the form <i>class</i>.<i>method</i>.*/
    public static final String RUN_IN_INITIALIZE_DRIVER = "layer7.runInInitializeDriver";

    /** Name of driver parameter that specifies static methods to invoke during
        {@link com.sun.japex.JapexDriver#initializeDriver terminateDriver}.
        Parameter value is a space-separated list of no-argument static method
        in the form <i>class</i>.<i>method</i>.*/
    public static final String RUN_IN_TERMINATE_DRIVER = "layer7.runInTerminateDriver";

    // ----------------------- Test case input parameters ----------------------

    /** Name of testCase parameter that specifies the class to be tested. */
    public static final String CLASS_NAME = "layer7.className";

    /** Name of testCase parameter that specifies the method to be tested. */
    public static final String METHOD_NAME = "layer7.methodName";

    /** Name of test case parameter specifying the methods to invoke during
        {@link com.sun.japex.JapexDriver#prepare prepare}.
        Parameter value is a space-separated list of no-argument method names. */
    public static final String RUN_IN_PREPARE = "layer7.runInPrepare";

    /** Name of test case parameter specifying the methods to invoke during
        {@link com.sun.japex.JapexDriver#finish finish}.
        Parameter value is a space-separated list of no-argument method names. */
    public static final String RUN_IN_FINISH = "layer7.runInFinish";
}
