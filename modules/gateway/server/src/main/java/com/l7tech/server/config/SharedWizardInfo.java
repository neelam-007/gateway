package com.l7tech.server.config;

import com.l7tech.server.config.db.DBInformation;

/**
 * User: megery
 * Date: May 15, 2007
 * Time: 4:15:46 PM
 */
public class SharedWizardInfo {
    private ClusteringType clusterType = ClusteringType.UNDEFINED;
    private ConfigurationType configType = ConfigurationType.UNDEFINED;
    private KeystoreType keystoreType = KeystoreType.UNDEFINED;
    private DBInformation dbinfo;
    private String hostname;
    private String realHostname;

    private static SharedWizardInfo instance;


    private SharedWizardInfo() {
    }

    public ClusteringType getClusterType() {
        return clusterType;
    }

    public void setClusterType(ClusteringType clusterType) {
        this.clusterType = clusterType;
    }

    public ConfigurationType getConfigType() {
        return configType;
    }

    public void setConfigType(ConfigurationType configType) {
        this.configType = configType;
    }

    public KeystoreType getKeystoreType() {
        return keystoreType;
    }

    public void setKeystoreType(KeystoreType ksType) {
        this.keystoreType = ksType;
    }

    public DBInformation getDbinfo() {
        if (dbinfo == null) dbinfo = new DBInformation();
        return dbinfo;
    }

    public void setDbinfo(DBInformation dbinfo) {
        this.dbinfo = dbinfo;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getHostname() {
        return hostname;
    }

    public String getRealHostname() {
        return realHostname;
    }

    public void setRealHostname(String realHostname) {
        this.realHostname = realHostname;
    }

    public static SharedWizardInfo getInstance() {
        if (instance == null)
            instance = new SharedWizardInfo();
        return instance;
    }
}
