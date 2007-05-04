package com.l7tech.server.config;

import java.io.File;

public class WindowsSpecificFunctions extends OSSpecificFunctions {

    //- PUBLIC

    public WindowsSpecificFunctions(String osname) {
        super(osname);
    }

    public WindowsSpecificFunctions(String osname, String partitionName) {
        super(osname, partitionName);
    }

    public String[] getKeystoreTypes() {
        return new String[]
        {
            KeystoreType.DEFAULT_KEYSTORE_NAME.getName(),
            KeystoreType.LUNA_KEYSTORE_NAME.getName()
        };
    }

    public String getOriginalPartitionControlScriptName() {
        return getSsgInstallRoot() + "bin/" + partitionControlScriptName;
    }

    public String getSpecificPartitionControlScriptName() {
        return getPartitionBase() + getPartitionName() + "/" + partitionControlScriptName;
    }

    public String getNetworkConfigurationDirectory() {
        throw new IllegalStateException("Cannot Configure the network on Windows.");
    }

    public String getUpgradedFileExtension() {
        return "new";
    }

    public boolean isWindows() {
        return true;
    }

    public boolean isLinux() {
        return false;
    }

    //- PACKAGE

    void makeOSSpecificFilenames() {
        if (isEmptyString(installRoot)) {
            installRoot = detectInstallRoot();

            if (isEmptyString(installRoot)) {
                installRoot = "C:/Program Files/Layer 7 Technologies/SecureSpan Gateway/";
            }
        }
        lunaInstallDir = "C:/Program Files/LunaSA/";
        lunaJSPDir = "C:/Program Files/LunaSA/JSP";
        lunaCmuPath = "cmu.exe";
        pathToJdk = "jdk/";
        partitionControlScriptName= "service.cmd";
    }
    
    //- PRIVATE

    private static final String SYSPROP_JAVA_HOME = "java.home";
    private static final String DEFAULT_JAVA_HOME = "C:/";
    private static final String KNOWN_HOME_CHILD_DIR = "configwizard";

    /**
     * Detect the SSG install root based based on the java home directory
     *
     * <p>This looks up to 2 levels above java home to find the ssg root.</p>
     *
     * @return The path to the installRoot or null if not found
     */
    private String detectInstallRoot() {
        String installRootPath = null;

        File javaHome = new File(System.getProperty(SYSPROP_JAVA_HOME, DEFAULT_JAVA_HOME));

        if (javaHome.isDirectory() && javaHome.getParentFile()!=null) {
            File installRootFile = javaHome.getParentFile();

            if (isHome(installRootFile)) {
                installRootPath = installRootFile.getAbsolutePath();
            } else if (installRootFile.getParentFile()!=null) {
                installRootFile = installRootFile.getParentFile();

                if (isHome(installRootFile)) {
                    installRootPath = installRootFile.getAbsolutePath();
                }
            }

            if (installRootPath != null && !installRootPath.endsWith("/")) {
                installRootPath = installRootPath + "/";
            }
        }

        return installRootPath;
    }

    /**
     * Check if the given directory is the home directory.
     *
     * @param homeDirectory The directory to check.
     * @return true if home
     */
    private boolean isHome(File homeDirectory) {
        return new File(homeDirectory, KNOWN_HOME_CHILD_DIR).isDirectory();    
    }
}
