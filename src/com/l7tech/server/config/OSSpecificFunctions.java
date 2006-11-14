package com.l7tech.server.config;

import com.l7tech.server.partition.PartitionInformation;
import org.apache.commons.lang.StringUtils;

import java.io.*;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Aug 12, 2005
 * Time: 3:06:33 PM
 * To change this template use File | Settings | File Templates.
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
    protected String keystoreDir;

    protected String lunaInstallDir;
    protected String lunaJSPDir;

    protected String pathToJdk;
    protected String pathToJreLibExt;

    protected String ssgLogProperties;
    protected String lunaCmuPath;
    protected String pathToJavaSecurityFile;
    protected String ssgSystemPropertiesFile;
    protected String pathToJavaLibPath;

    private String pathToDBCreateFile;
    private String partitionName;
    public static final String PARTITION_BASE = "etc/conf/partitions";
    public static final String NOPARTITION_BASE = "etc/conf/";

    public OSSpecificFunctions(String OSName) {
        this(OSName, "");
    }

    public OSSpecificFunctions(String OSName, String partitionName) {
        installRoot = System.getProperty("com.l7tech.server.home");

        if (installRoot != null && !installRoot.endsWith("/")) {
            installRoot = installRoot + "/";
        }
        this.osName = OSName;

        if (StringUtils.isNotEmpty(partitionName))
            this.partitionName = partitionName;
        else
            this.partitionName = "";
        
        makeOSSpecificFilenames();
        makeFilenames();
    }

    public boolean isWindows() {
        return false;
    }

    public boolean isLinux() {
        return false;
    }

    abstract void makeOSSpecificFilenames();

    public void makeFilenames() {
        clusterHostFile = "cluster_hostname";
        databaseConfig = "hibernate.properties";
        ssgLogProperties = "ssglog.properties";
        keyStorePropertiesFile = "keystore.properties";
        keystoreDir = "keys/";
        ssgSystemPropertiesFile = "system.properties";

        tomcatServerConfig = "tomcat/conf/server.xml";
        pathToJreLibExt = "jre/lib/ext/";
        pathToJavaLibPath = "lib/";
        pathToJavaSecurityFile = "jre/lib/security/java.security";
        pathToDBCreateFile = "etc/sql/ssg.sql";
    }

    public String getClusterHostName() {
        String hostname = null;
        File f = new File(getClusterHostNamePath());
        if (f.exists()) {
            BufferedReader reader = null;
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

    protected String getClusterHostNamePath() {
        return getSsgInstallRoot() + clusterHostFile;
    }

    public String getOSName() {
        return osName;
    }


    public String getPartitionName() {
        return partitionName;
    }

    public String getConfigurationBase() {
        if (StringUtils.isNotEmpty(getPartitionName()))
            return getSsgInstallRoot() + PartitionInformation.PARTITIONS_BASE + getPartitionName() + "/";


        return getSsgInstallRoot() + NOPARTITION_BASE;

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

    public abstract String[] getKeystoreTypes();

    public String getSsgLogPropertiesFile() {
        return getConfigurationBase() + ssgLogProperties;
    }

    public String getKeyStorePropertiesFile() {
        return getConfigurationBase() + keyStorePropertiesFile;
    }

    public String getTomcatServerConfig() {
        return getSsgInstallRoot() + tomcatServerConfig;
    }

    public String getKeystoreDir() {
        return getConfigurationBase() + keystoreDir;
    }

    public String getLunaJSPDir() {
        return lunaJSPDir;
    }

    public void setLunaJSPDir(String lunaJSPDir) {
        this.lunaJSPDir = lunaJSPDir;
    }

    public String getLunaInstallDir() {
        return lunaInstallDir;
    }

    public void setLunaInstallDir(String lunaInstallDir) {
        this.lunaInstallDir = lunaInstallDir;
    }

    public String getPathToJdk() {
        return getSsgInstallRoot() + pathToJdk;
    }

    public String getPathToJavaSecurityFile() {
        return getPathToJdk() + pathToJavaSecurityFile;
    }

    public String getPathToJreLibExt() {
        return getPathToJdk() + pathToJreLibExt;
    }

    public String getLunaCmuPath() {
        return lunaCmuPath;
    }

    public String getSsgSystemPropertiesFile() {
        return getConfigurationBase() + ssgSystemPropertiesFile;
    }

    public String getPathToJavaLibPath() {
        return getSsgInstallRoot() + pathToJavaLibPath;
    }

    public String getPathToDBCreateFile() {
        return getSsgInstallRoot() + pathToDBCreateFile;
    }

    public abstract String getNetworkConfigurationDirectory();

    public abstract String getUpgradedFileExtension();

    public static class MissingPropertyException extends RuntimeException
    {
        private MissingPropertyException(String message) {
            super(message);
        }
    }
}