package com.l7tech.server.config;

import com.l7tech.server.config.exceptions.UnsupportedOsException;

import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Aug 15, 2005
 * Time: 2:13:11 PM
 * To change this template use File | Settings | File Templates.
 */
public class OSDetector {
    private static Pattern LINUX_PATTERN = Pattern.compile("Linux.*");
    private static Pattern WINDOWS_PATTERN = Pattern.compile("Windows.*");

    private static String OSName;

    public static OSSpecificFunctions getOSSpecificActions() throws UnsupportedOsException {
        OSName = System.getProperty("os.name");
        if (isWindows()) {
            return new WindowsSpecificFunctions(OSName);
        }
        if (isLinux()) {
            return new LinuxSpecificFunctions(OSName);
        }
        else {
            throw new UnsupportedOsException(OSName + " is not a supported operating system.");
        }
    }

    private static boolean isWindows() {
        return WINDOWS_PATTERN.matcher(OSName).matches();
    }

    private static boolean isLinux() {
        return LINUX_PATTERN.matcher(OSName).matches();
    }
}
