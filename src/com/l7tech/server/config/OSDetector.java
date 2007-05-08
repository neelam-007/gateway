package com.l7tech.server.config;

import com.l7tech.server.config.exceptions.UnsupportedOsException;

import java.util.regex.Pattern;

public class OSDetector {
    private static Pattern LINUX_PATTERN = Pattern.compile("Linux.*");
    private static Pattern SOLARIS_PATTERN = Pattern.compile("SunOS.*");
    private static Pattern WINDOWS_PATTERN = Pattern.compile("Windows.*");

    private static String OSName = System.getProperty("os.name");

    public static OSSpecificFunctions getOSSpecificFunctions() throws UnsupportedOsException {
        return getOSSpecificFunctions("");
    }

    public static OSSpecificFunctions getOSSpecificFunctions(String partitionName) {
        return createPartitionAwareOSFunctions(partitionName);
    }

    private static OSSpecificFunctions createPartitionAwareOSFunctions(String partitionName) {
        if (!isWindows() && !isUnix())
            throw new UnsupportedOsException(OSName + " is not a supported operating system.");

        OSSpecificFunctions osf = null;
        if (isWindows()) {
            osf = new WindowsSpecificFunctions(OSName,partitionName);
        } else {
            if (isLinux())
                osf = new LinuxSpecificFunctions(OSName,partitionName);
            else if (isSolaris())
                osf = new SolarisSpecificFunctions(OSName, partitionName);
        }

        return osf;
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

    public static void main(String[] args) {
        //this should return EITHER the default partition information or the original, pre SSG configuration
        OSSpecificFunctions osf = getOSSpecificFunctions("");

        //now list all the stuff that it returns
        System.out.println("SSG Root = " + osf.getSsgInstallRoot());
        System.out.println("Configuration Directory = " + osf.getConfigurationBase());
        System.out.println("Partitition Base = " + osf.getPartitionBase());
        System.out.println("Partitition Name = " + osf.getPartitionName());
        System.out.println("DB Configuration = " + osf.getDatabaseConfig());
        System.out.println("Logging Configuration = " + osf.getSsgLogPropertiesFile());
        System.out.println("System Properties = " + osf.getSsgSystemPropertiesFile());
        System.out.println("Keystore Properties = " + osf.getKeyStorePropertiesFile());
        System.out.println("Keystore Directory = " + osf.getKeystoreDir());
        System.out.println("Tomcat Server Configuratioin = " + osf.getTomcatServerConfig());
    }
}
