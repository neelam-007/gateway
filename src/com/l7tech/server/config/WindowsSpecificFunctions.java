package com.l7tech.server.config;

import com.l7tech.server.config.systemconfig.NetworkingConfigurationBean;

import java.io.File;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WindowsSpecificFunctions extends OSSpecificFunctions {

    //- PUBLIC

    public WindowsSpecificFunctions(String osname) {
        this(osname, null);
    }

    public WindowsSpecificFunctions(String osname, String partitionName) {
        super(osname, partitionName);
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

    public List<NetworkingConfigurationBean.NetworkConfig> getNetworkConfigs(boolean getLoopBack, boolean getIPV6) throws SocketException {
        List<NetworkingConfigurationBean.NetworkConfig> networkConfigs = new ArrayList<NetworkingConfigurationBean.NetworkConfig>();
        List<NetworkInterface> allInterfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
        for (NetworkInterface networkInterface : allInterfaces) {
            if (!networkInterface.isLoopback())
                networkConfigs.add(NetworkingConfigurationBean.makeNetworkConfig(networkInterface, null, getIPV6));
        }
        return networkConfigs;
    }

    public boolean isWindows() {
        return true;
    }

    //- PACKAGE

    void doOsSpecificSetup() {
        if (isEmptyString(installRoot)) {
            installRoot = detectInstallRoot();

            if (isEmptyString(installRoot)) {
                installRoot = "C:/Program Files/Layer 7 Technologies/SecureSpan Gateway/";
            }
        }
        configWizardLauncher = installRoot + "configwizard/ssgconfig.cmd";

        List<KeystoreInfo> infos = new ArrayList<KeystoreInfo>();
        infos.add(new KeystoreInfo(KeystoreType.DEFAULT_KEYSTORE_NAME));
        if (KeystoreInfo.isLunaEnabled()) {
            KeystoreInfo lunaInfo = new KeystoreInfo(KeystoreType.LUNA_KEYSTORE_NAME);
            lunaInfo.addMetaInfo("INSTALL_DIR", "C:/Program Files/LunaSA/");
            lunaInfo.addMetaInfo("JSP_DIR", "C:/Program Files/LunaSA/JSP");
            lunaInfo.addMetaInfo("CMU_PATH", "cmu.exe");
            infos.add(lunaInfo);
        }

        pathToJdk = "jdk/";
        partitionControlScriptName= "service.cmd";

        keystoreInfos = infos.toArray(new KeystoreInfo[0]);
        upgradeFileNewExt = "new";

        timeZonesDir = null;

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
