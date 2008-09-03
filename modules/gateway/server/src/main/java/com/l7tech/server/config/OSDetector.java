package com.l7tech.server.config;

import com.l7tech.server.config.exceptions.UnsupportedOsException;

import java.util.regex.Pattern;

public class OSDetector {
    private static Pattern LINUX_PATTERN = Pattern.compile("Linux.*");
    private static Pattern SOLARIS_PATTERN = Pattern.compile("SunOS.*");
    private static Pattern WINDOWS_PATTERN = Pattern.compile("Windows.*");

    private static String OSName = System.getProperty("os.name");

    public static OSSpecificFunctions getOSSpecificFunctions() throws UnsupportedOsException {
        if (!isLinux())
            throw new UnsupportedOsException(OSName + " is not a supported operating system.");

        return new LinuxSpecificFunctions(OSName);
    }

    public static boolean isWindows() {
        return WINDOWS_PATTERN.matcher(OSName).matches();
    }

    public static boolean isUnix() {
        return (isLinux() || isSolaris());
    }

    public static boolean isSolaris() {
        return SOLARIS_PATTERN.matcher(OSName).matches();
    }

    public static boolean isLinux() {
        return LINUX_PATTERN.matcher(OSName).matches();
    }
}
