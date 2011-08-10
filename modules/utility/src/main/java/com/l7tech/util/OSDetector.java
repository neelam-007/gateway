package com.l7tech.util;

import java.util.regex.Pattern;

public class OSDetector {
    private static Pattern LINUX_PATTERN = Pattern.compile("Linux.*");
    private static Pattern SOLARIS_PATTERN = Pattern.compile("SunOS.*");
    private static Pattern WINDOWS_PATTERN = Pattern.compile("Windows.*");

    private static String OSName = SyspropUtil.getProperty( "os.name" );

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

    public static String getOSName() {
        return OSName;
    }

}
