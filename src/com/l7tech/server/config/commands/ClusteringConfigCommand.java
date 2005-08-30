package com.l7tech.server.config.commands;

import com.l7tech.server.config.beans.ClusteringConfigBean;
import com.l7tech.server.config.beans.ConfigurationBean;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.text.DateFormat;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Aug 16, 2005
 * Time: 2:52:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class ClusteringConfigCommand extends BaseConfigurationCommand {
    static Logger logger = Logger.getLogger(ClusteringConfigCommand.class.getName());


    private String hostname;
    private String clusterHostname;
    private static final String BACKUP_FILE_NAME = "cluster_config_backups";
    DateFormat formatter;

    public ClusteringConfigCommand(ConfigurationBean bean) {
        super(bean, bean.getOSFunctions());
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getClusterHostname() {
        return clusterHostname;
    }

    public void setClusterHostname(String clusterHostname) {
        this.clusterHostname = clusterHostname;
    }

    public boolean execute() {
//        printPlans();
        boolean success = true;
        ClusteringConfigBean clusterBean = (ClusteringConfigBean) configBean;

        String clusterHostname = clusterBean.getClusterHostname();
        boolean configureCluster = (!(clusterHostname == null || clusterHostname.equalsIgnoreCase(clusterBean.getLocalHostName())));


        File clusterHostNameFile = configureCluster? new File(osFunctions.getClusterHostFile()):null;
        File hostsFile = new File(osFunctions.getHostsFile());

        File[] files = new File[]
        {   clusterHostNameFile,
            hostsFile
        };


        try {
            backupFiles(files, BACKUP_FILE_NAME);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (configureCluster) {
            try {
                writeClusterHostname(clusterHostNameFile, clusterHostname);
            } catch (IOException e) {
                success = false;
            }
        }

//        updateHostsFile(hostsFile, clusterHostname);

        return success;
    }

//    private void updateHostsFile(File hostsFile, String clusterHostname) {
//        if (hostsFile == null || StringUtils.isEmpty(clusterHostname)) {
//            throw new IllegalArgumentException("hosts file and cluster hostname need to be provided");
//        }
//    }

    private void writeClusterHostname(File clusterHostNameFile, String clusterHostname) throws IOException {
        if (clusterHostNameFile == null || StringUtils.isEmpty(clusterHostname)) {
            throw new IllegalArgumentException("cluster hostname file and cluster hostname need to be provided");
        }

        try {
            FileOutputStream fos = new FileOutputStream(clusterHostNameFile);
            fos.write((clusterHostname + "\n").getBytes("UTF-8"));
            fos.close();
        } catch (FileNotFoundException e) {
            logger.severe("error while updating the cluster host name file");
            logger.severe(e.getMessage());
            throw e;
        } catch (UnsupportedEncodingException e) {
            logger.severe("error while updating the cluster host name file");
            logger.severe(e.getMessage());
            throw e;
        } catch (IOException e) {
            logger.severe("error while updating the cluster host name file");
            logger.severe(e.getMessage());
            throw e;
        }
    }

    private void printPlans() {
        ClusteringConfigBean clusterBean = (ClusteringConfigBean) configBean;

        System.out.println("editing the following files");
        System.out.println(osFunctions.getHostsFile());
        System.out.println(osFunctions.getClusterHostFile());

        System.out.println("update clustering configuration as follows:");

        StringBuffer buffer = new StringBuffer();
        if (clusterBean.isNewHostName()) {
            buffer.append("Configure a new hostname - ").append(clusterBean.getClusterHostname()).append("\n");
        } else {
            buffer.append("Use the existing hostname - ").append(clusterBean.getClusterHostname()).append("\n");
        }

        switch (clusterBean.getClusterType()) {
            case ClusteringConfigBean.CLUSTER_NONE:
                buffer.append("Don't configure a cluster\n");
                break;
            case ClusteringConfigBean.CLUSTER_NEW:
                buffer.append("Create a new cluster using the above name: ").append(clusterBean.getClusterHostname()).append("\n");
                break;
            case ClusteringConfigBean.CLUSTER_JOIN:
                buffer.append("Join a cluster using the above name: ").append(clusterBean.getClusterHostname()).append("\n");
                buffer.append("the master of the cluster is: \n");
                buffer.append(clusterBean.getCloneUsername()).append(":").append(clusterBean.getClonePassword()).append("@").append(clusterBean.getCloneHostname()).append("\n");
                break;
        }
        System.out.println(buffer.toString());
    }

}
