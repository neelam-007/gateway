package com.l7tech.server.config;

import com.l7tech.server.config.exceptions.UnsupportedOsException;

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
//    protected String dbFailDetect;
    protected String keyStorePropertiesFile;
    protected String tomcatServerConfig;
    protected String keystoreDir;

    protected String lunaInstallDir;
    protected String lunaJSPDir;

    protected String pathToJdk;
    protected String pathToJreLibExt;

    protected String ssgLogProperties;
    protected String lunaCmuPath;
    private String pathToJavaSecurityFile;
    private String ssgSystemPropertiesFile;

    public OSSpecificFunctions() {
        installRoot = System.getProperty("com.l7tech.server.home");
        if (installRoot != null && !installRoot.endsWith("/")) {
            installRoot = installRoot + "/";
        }
        makeFilenames();
        makeOSSpecificFilenames();
    }

    public boolean isWindows() {
        return false;
    }

    public boolean isLinux() {
        return false;
    }

    abstract void makeOSSpecificFilenames();

    public void makeFilenames() {
        clusterHostFile = "etc/conf/cluster_hostname";
        databaseConfig = "etc/conf/hibernate.properties";
        ssgLogProperties = "etc/conf/ssglog.properties";
        keyStorePropertiesFile = "etc/conf/keystore.properties";
        tomcatServerConfig = "tomcat/conf/server.xml";
        keystoreDir = "etc/keys/";
        pathToJreLibExt = "jre/lib/ext/";
        pathToJavaSecurityFile = "jre/lib/security/java.security";
        pathToJdk = "jdk1.5.0_02/";
        ssgSystemPropertiesFile = "etc/conf/system.properties";
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
        return installRoot + clusterHostFile;
    }

    public boolean isSsgInstalled() {
        File f = new File(getSsgInstallFilePath());
        return f.exists();
    }

    public String getOSName() {
        return osName;
    }

    public String getSsgInstallRoot() {
        return installRoot;
    }

    String getSsgInstallFilePath() {
        return ssgInstallFilePath;
    }

    public String getHostsFile() {
        return hostsFile;
    }

    public String getDatabaseConfig() {
        return installRoot + databaseConfig;
    }

    public String getClusterHostFile() {
        return installRoot + clusterHostFile;
    }

    public abstract String[] getKeystoreTypes();

    public String getSsgLogPropertiesFile() {
        return installRoot + ssgLogProperties;
    }

    public String getKeyStorePropertiesFile() {
        return installRoot + keyStorePropertiesFile;
    }

    public String getTomcatServerConfig() {
        return installRoot + tomcatServerConfig;
    }

    public String getKeystoreDir() {
        return installRoot + keystoreDir;
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
        return installRoot + pathToJdk;
    }

    public String getPathToJavaSecurityFile() {
        return getPathToJdk() + pathToJavaSecurityFile;
    }

    public String getPathToJreLibExt() {
        return getPathToJdk() + pathToJreLibExt;
    }

    public static void main(String[] args) {
        System.setProperty("SSG_ROOT", "/tmp/testing");
        OSSpecificFunctions functions = null;
        try {
            functions = OSDetector.getOSSpecificActions();
        } catch (UnsupportedOsException e) {
            System.out.println(e.getMessage());
        }
        System.out.println("OS Name = " + functions.getOSName());
        System.out.println("Install root = " + functions.getSsgInstallRoot());
        System.out.println("Cluster Hostname File = " + functions.getClusterHostFile());
        System.out.println("Cluster Hostname = " + functions.getClusterHostName());
    }

    public String getLunaCmuPath() {
        return lunaCmuPath;
    }

    public String getSsgSystemPropertiesFile() {
        return installRoot + ssgSystemPropertiesFile;
    }
}
