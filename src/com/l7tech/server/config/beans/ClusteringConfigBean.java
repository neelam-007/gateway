package com.l7tech.server.config.beans;

import com.l7tech.server.config.commands.ClusteringConfigCommand;
import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.server.config.commands.ClusteringConfigCommand;

import java.util.ArrayList;

import org.apache.commons.lang.StringUtils;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Aug 12, 2005
 * Time: 11:26:07 AM
 * To change this template use File | Settings | File Templates.
 */
public class ClusteringConfigBean extends BaseConfigurationBean {
    private boolean isNewHostName;
    private String hostname;
    private String localHostName;
    private int clusterType;

    private String cloneHostname;
    private String cloneUsername;
    private char[] clonePassword;


    private final static String NAME = "Clustering Configuration";
    private final static String DESCRIPTION = "Configures the cluster properties for an SSG";

    private final static String NOTHING_TO_DO_INFO = "Nothing to do";
    private final static String CLUSTER_HOSTFILE_UPDATE_INFO = "Update cluster hostname: ";
    private final static String CLUSTER_HOSTFILE_DELETE_INFO = "Delete cluster hostname file";

    public static final int CLUSTER_NONE = 0;
    public static final int CLUSTER_NEW = 1;
    public static final int CLUSTER_JOIN = 2;



    public ClusteringConfigBean(OSSpecificFunctions osFunctions) {
        super(NAME, DESCRIPTION, osFunctions);
        ELEMENT_KEY = this.getClass().getName();
        init();
    }

    private void init() {
        setClusterHostname(null);
        setNewHostName(false);
    }

    public String[] explain() {
        ArrayList explanations = new ArrayList();

        explanations.add(getName() + " - " + getDescription());

        if (clusterType == CLUSTER_NONE )  {
            explanations.add(insertTab + NOTHING_TO_DO_INFO);
        } else {
            explanations.add(insertTab + CLUSTER_HOSTFILE_UPDATE_INFO + getClusterHostname());
        }


        return (String[]) explanations.toArray(new String[explanations.size()]);
    }

    public void reset() {
        isNewHostName = false;
        hostname = "";
        localHostName  = "";
        clusterType = CLUSTER_NONE;
        cloneHostname = "";
        cloneUsername = "";
        clonePassword = new char[0];
    }

    public boolean apply() {
        return false;
    }

    public void setClusterHostname(String hostName) {
        this.hostname = hostName;
    }

    public String getClusterHostname() {
        return this.hostname;
    }

    public boolean isNewHostName() {
        return isNewHostName;
    }

    public void setNewHostName(boolean newHostName) {
        isNewHostName = newHostName;
    }

    public String getLocalHostName() {
        return localHostName;
    }

    public void setLocalHostName(String localHostName) {
        this.localHostName = localHostName;
    }

    public void putAffectedObject(String key, Object affectedObject) {
        affectedObjects.put(key, affectedObject);
    }

    public void removeAffectedObject(String key) {
        affectedObjects.remove(key);
    }

    public void setDoClusterType(int clusterType) {
        this.clusterType= clusterType;
    }

    public void setClonePassword(char[] password) {
        this.clonePassword = password;
    }

    public void setCloneUsername(String username) {
        this.cloneUsername = username;
    }

    public void setCloneHostname(String hostname) {
        this.cloneHostname = hostname;
    }

    public char[] getClonePassword() {
        return clonePassword;
    }

    public String getCloneUsername() {
        return cloneUsername;
    }

    public String getCloneHostname() {
        return cloneHostname;
    }

    public int getClusterType() {
        return clusterType;
    }
}
