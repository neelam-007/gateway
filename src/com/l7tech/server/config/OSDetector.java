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

    private static String OSName = System.getProperty("os.name");

//    private static OSSpecificFunctions osf_;

    public static OSSpecificFunctions getOSSpecificFunctions() throws UnsupportedOsException {
        return createPartitionAwareOSFunctions("");
    }

    public static OSSpecificFunctions getOSSpecificFunctions(String partitionName) {
        return createPartitionAwareOSFunctions(partitionName);
    }

    private static OSSpecificFunctions createPartitionAwareOSFunctions(String partitionName) {
        if (isWindows()) {
            return new WindowsSpecificFunctions(OSName,partitionName);
        } else if (isLinux()) {
            return new LinuxSpecificFunctions(OSName,partitionName);
        }
        else {
            throw new UnsupportedOsException(OSName + " is not a supported operating system.");
        }
    }

    public static boolean isWindows() {
        return WINDOWS_PATTERN.matcher(OSName).matches();
    }

    public static boolean isLinux() {
        return LINUX_PATTERN.matcher(OSName).matches();
    }

}
