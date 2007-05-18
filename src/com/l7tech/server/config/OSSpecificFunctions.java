package com.l7tech.server.config;

import com.l7tech.server.partition.PartitionInformation;

import java.io.*;
import java.util.Map;
import java.util.HashMap;

/**
 * A class that encapsulates the configuraration locations for an SSG on a supported platform.
 *
 * If the class is instantiated with a partition ID then it will provide configurations for THAT partition.
 * If no partition name is given then the default partition configuration will be given.
 *
 * Written By: megery
 * Date: Aug 12, 2005
 * Time: 3:06:33 PM
 */
public abstract class OSSpecificFunctions {
    protected String osName;

    // configuration files/directories to be queried, modified or created
    protected String installRoot;
    protected String ssgInstallFilePath;
    protected String hostsFile;
    protected String clusterHostFile;
    protected String databaseConfig;
    protected String keyStorePropertiesFile;
    protected String tomcatServerConfig;
    protected String ftpServerConfig;
    protected String keystoreDir;

    protected String pathToJdk;
    protected String pathToJreLibExt;

    protected String ssgLogProperties;

    protected String pathToJavaSecurityFile;
    protected String ssgSystemPropertiesFile;
    protected String pathToJavaLibPath;

    private String pathToDBCreateFile;
    private String partitionName;
    public static final String PARTITION_BASE = "etc/conf/partitions";
    public static final String NOPARTITION_BASE = "etc/conf/";
    private boolean hasPartitions;

    String partitionControlScriptName;

    protected String networkConfigDir;
    protected String upgradeFileNewExt;
    protected String upgradeFileOldExt;

    protected String timeZonesDir;

    protected KeystoreInfo[] keystoreInfos;

    public OSSpecificFunctions(String OSName) {
        this(OSName, "");
    }

    public OSSpecificFunctions(String OSName, String partitionName) {
        installRoot = System.getProperty("com.l7tech.server.home");

        if (installRoot != null && !installRoot.endsWith("/")) {
            installRoot = installRoot + "/";
        }
        this.osName = OSName;


        if (!isEmptyString(partitionName))
            this.partitionName = partitionName;
        else
            this.partitionName = "";
        
        doOsSpecificSetup();
        makeFilenames();
        hasPartitions = checkIsPartitioned();
    }

    private boolean checkIsPartitioned() {
        return new File(getPartitionBase()).exists();
    }

    public boolean isWindows() {
        return false;
    }

    public boolean isUnix() {
        return false;    
    }

    abstract void doOsSpecificSetup();

    public void makeFilenames() {
        clusterHostFile = "cluster_hostname";
        databaseConfig = "hibernate.properties";
        ssgLogProperties = "ssglog.properties";
        keyStorePropertiesFile = "keystore.properties";
        keystoreDir = "keys/";
        ssgSystemPropertiesFile = "system.properties";

        tomcatServerConfig = "server.xml";
        ftpServerConfig = "ftpserver.properties";
        pathToJreLibExt = "jre/lib/ext/";
        pathToJavaLibPath = "lib/";
        pathToJavaSecurityFile = "jre/lib/security/java.security";
        pathToDBCreateFile = "etc/sql/ssg.sql";
    }

    public String getClusterHostName() {
        String hostname = null;
        File f = new File(getClusterHostNamePath());
        if (f.exists()) {
            BufferedReader reader;
            try {
                reader = new BufferedReader(new FileReader(f));
                if (reader.ready()) {
                    hostname = reader.readLine();
                }
                reader.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace(); //won't happen since this is inside a if f.exists() block
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return hostname;
    }

    public String getOSName() {
        return osName;
    }

    public String getPartitionName() {
        return partitionName;
    }


    public void setPartitionName(String partitionName) {
        this.partitionName = partitionName;
    }

    protected String getClusterHostNamePath() {
        return getConfigurationBase() + clusterHostFile;
    }

    public String getPartitionBase() {
        return getSsgInstallRoot() + PartitionInformation.PARTITIONS_BASE;
    }

    public String getConfigurationBase() {
        String pname = getPartitionName();
        if (!isEmptyString(pname))
             return getPartitionBase() + partitionName + "/";

        return getSsgInstallRoot() + NOPARTITION_BASE;

    }

    //Just like StringUtils.isEmpty() without needing to import StringUtils. Since this is a utility class I wanted to
    //keep the number of imports down.
    protected boolean isEmptyString(String string) {
        return (string == null) || "".equals(string);
    }

    public String getSsgInstallRoot() {
        return installRoot;
    }

    public String getDatabaseConfig() {
        return getConfigurationBase() + databaseConfig;
    }

    public String getClusterHostFile() {
        return getConfigurationBase() + clusterHostFile;
    }

    public String getSsgLogPropertiesFile() {
        return getConfigurationBase() + ssgLogProperties;
    }

    public String getKeyStorePropertiesFile() {
        return getConfigurationBase() + keyStorePropertiesFile;
    }

    public String getTomcatServerConfig() {
        if (!hasPartitions) {
            return getSsgInstallRoot() + "tomcat/conf/server.xml";
        }                                                        
        return getConfigurationBase() + tomcatServerConfig;
    }

    public String getFtpServerConfig() {
        if (!hasPartitions) {
            return getSsgInstallRoot() + "etc/conf/" + ftpServerConfig;
        }
        return getConfigurationBase() + ftpServerConfig;
    }

    public String getKeystoreDir() {
        if (!hasPartitions) {
            return getSsgInstallRoot() + "etc/keys/";
        }
        return getConfigurationBase() + keystoreDir;
    }

//    public String getLunaJSPDir() {
//        return lunaJSPDir;
//    }

//    public void setLunaJSPDir(String lunaJSPDir) {
//        this.lunaJSPDir = lunaJSPDir;
//    }

//    public String getLunaInstallDir() {
//        return lunaInstallDir;
//    }

//    public void setLunaInstallDir(String lunaInstallDir) {
//        this.lunaInstallDir = lunaInstallDir;
//    }

    public String getPathToJdk() {
        return getSsgInstallRoot() + pathToJdk;
    }

    public String getPathToJavaSecurityFile() {
        return getPathToJdk() + pathToJavaSecurityFile;
    }

    public String getPathToJreLibExt() {
        return getPathToJdk() + pathToJreLibExt;
    }

//    public String getLunaCmuPath() {
//        return lunaCmuPath;
//    }

    public String getSsgSystemPropertiesFile() {
        return getConfigurationBase() + ssgSystemPropertiesFile;
    }

    public String getPathToJavaLibPath() {
        return getSsgInstallRoot() + pathToJavaLibPath;
    }

    public String getPathToDBCreateFile() {
        return getSsgInstallRoot() + pathToDBCreateFile;
    }

    public KeystoreInfo[] getAvailableKeystores() {
        return keystoreInfos;
    }

    public abstract String getOriginalPartitionControlScriptName();
    public abstract String getSpecificPartitionControlScriptName();

    public String getNetworkConfigurationDirectory() {
        return networkConfigDir;
    }

    public String getUpgradedNewFileExtension() {
        return upgradeFileNewExt;
    }

    public String getUpgradedOldFileExtension() {
        return upgradeFileOldExt;
    }

    public KeystoreInfo getKeystore(KeystoreType keystoreType) {
        if (keystoreInfos == null)
            return null;

        for (KeystoreInfo keystoreInfo : keystoreInfos) {
            if (keystoreInfo.getType() == keystoreType)
                return keystoreInfo;
        }

        return null;
    }

    public String getTimeZonesDir() {
        return timeZonesDir;
    }

    public static class KeystoreInfo {
        KeystoreType type;
        Map<String, String> metaInfo;
        private static final String ENABLE_HSM = "com.l7tech.server.keystore.enablehsm";
        private static final String ENABLE_LUNA = "com.l7tech.server.keystore.enableluna";

        public KeystoreInfo(KeystoreType type) {
            this.type = type;
            metaInfo = new HashMap<String, String>();
        }

        public void addMetaInfo(String key, String data) {
            metaInfo.put(key, data);
        }

        public void removeMetaInfo(String key) {
            metaInfo.remove(key);
        }

        public Map<String, String> getAllMetaInfo() {
            return metaInfo;   
        }

        public String getMetaInfo(String key) {
            return getAllMetaInfo().get(key);
        }

        public KeystoreType getType() {
            return type;
        }

        public static boolean isHSMEnabled() {
            return Boolean.getBoolean(ENABLE_HSM) ;
        }

        public static boolean isLunaEnabled() {
            return Boolean.getBoolean(ENABLE_LUNA) ;
        }


    }
}