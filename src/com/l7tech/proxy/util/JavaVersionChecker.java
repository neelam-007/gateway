/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.util;

import java.util.StringTokenizer;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Utilities for checking the Java version in use.
 * User: mike
 * Date: Jul 10, 2003
 * Time: 5:26:31 PM
 */
public class JavaVersionChecker {
    private static final Logger log = Logger.getLogger(JavaVersionChecker.class.getName());

    /**
     * Check if the java.version is at least the requested version.
     * @param targetVersion an array of version components, with the major version coming first.
     *                      ie, { 1, 4, 2 } would mean you wanted at least JDK version 1.4.2.
     *                      Given that request, we'd return false if running on these JDK versions:
     *                         (1.1, 1.4, 1.4.1, 1.4.1_9999, 1.bliff.ploink)
     *                      but would return true if running on these JDK version:
     *                         (1.4.2, 1.4.2_01, 1.5, 2.0, 2)
     * @return True iff. the java version appears to be at least the version requested.
     */
    public static boolean isJavaVersionAtLeast(int[] targetVersion) {
        String jv = System.getProperty("java.version");
        boolean isDesiredVersion = false;
        int count = 0;
        for (StringTokenizer tokenizer = new StringTokenizer(jv, "._"); tokenizer.hasMoreTokens(); ++count) {
            String s = tokenizer.nextToken();
            int got;
            try {
                got = Integer.parseInt(s);
            } catch (NumberFormatException e) {
                log.log(Level.WARNING, "Problem while parsing token #" + (count+1) + " of java.version " + jv  + ": ", e);
                break;
            }
            if (got < targetVersion[count])
                break;
            if (got > targetVersion[count]) {
                isDesiredVersion = true;
                break;
            }
            if (count >= targetVersion.length - 1) {
                isDesiredVersion = true;
                break;
            }
        }
        return isDesiredVersion;
    }
}
