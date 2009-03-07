package com.l7tech.util;

import java.io.PrintStream;

/**
 *
 */
public interface CodegenTestThingPrinter {
    /**
     * Pring a greeting to the specified PrintStream, including the given suffix.
     *
     * @param out a PrintStream to send the greeting to.  Required.
     * @param suffix the suffix to attach.  Required.
     * @return the String that was printed to the PrintStream.
     */
    String printThing(PrintStream out, String suffix);
}
