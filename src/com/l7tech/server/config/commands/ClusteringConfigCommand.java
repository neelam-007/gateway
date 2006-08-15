package com.l7tech.server.config.commands;

import com.l7tech.server.config.beans.ClusteringConfigBean;
import com.l7tech.server.config.beans.ConfigurationBean;
import com.l7tech.server.config.PropertyHelper;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.util.Properties;
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
    private static final String PROP_RMI_HOSTNAME = "java.rmi.server.hostname";

    public ClusteringConfigCommand(ConfigurationBean bean) {
        super(bean);
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
        boolean success = true;
        ClusteringConfigBean clusterBean = (ClusteringConfigBean) configBean;

        boolean configureCluster = clusterBean.getClusterType() != ClusteringConfigBean.CLUSTER_NONE;

        File clusterHostNameFile = configureCluster? new File(osFunctions.getClusterHostFile()):null;
        File systemPropertiesFile = new File(osFunctions.getSsgSystemPropertiesFile());

        File[] files = new File[]
        {   clusterHostNameFile,
            systemPropertiesFile,
        };


        backupFiles(files, BACKUP_FILE_NAME);


        String hostname = clusterBean.getClusterHostname();
        try {
            updateSystemPropertiesFile(hostname, systemPropertiesFile);
            if (configureCluster) {
                try {
                    writeClusterHostname(clusterHostNameFile, hostname);
                    success = true;
                } catch (IOException e) {
                    success = false;
                }
            }
        } catch (IOException e) {
            success = false;
        }

        return success;
    }

    private void updateSystemPropertiesFile(String hostname, File systemPropertiesFile) throws IOException {
        OutputStream fos = null;
        try {
            Properties props = PropertyHelper.mergeProperties(systemPropertiesFile,
                    new File(systemPropertiesFile.getAbsolutePath() + "." + osFunctions.getUpgradedFileExtension()),
                    true);

            props.setProperty(PROP_RMI_HOSTNAME, hostname);

            fos =   new FileOutputStream(systemPropertiesFile);
            props.store(fos, "Updated by the SSG Configuration Tool");
            logger.info("Setting " + PROP_RMI_HOSTNAME + "=" + hostname + " in system.properties file");
        } catch (FileNotFoundException e) {
            logger.severe("There was an error while updating the file: " + systemPropertiesFile.getAbsolutePath());
            logger.severe(e.getMessage());
            throw e;
        } catch (IOException e) {
            logger.severe("There was an error while updating the file: " + systemPropertiesFile.getAbsolutePath());
            logger.severe(e.getMessage());
            throw e;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private void writeClusterHostname(File clusterHostNameFile, String clusterHostname) throws IOException {
        if (clusterHostNameFile == null) {
            logger.severe("Cannot write cluster host name file");
        }

        if (StringUtils.isEmpty(clusterHostname)) {
            throw new IllegalArgumentException("cluster hostname needs to be provided");
        }

        PrintStream ps = null;
        try {
            ps = new PrintStream(new FileOutputStream(clusterHostNameFile));
            logger.info("Writing " + clusterHostname + " to cluster_hostname file");
            ps.print(clusterHostname);

        } catch (FileNotFoundException e) {
            logger.severe("error while updating the cluster host name file");
            logger.severe(e.getMessage());
            throw e;
        } catch (IOException e) {
            logger.severe("error while updating the cluster host name file");
            logger.severe(e.getMessage());
            throw e;
        } finally {
            if (ps != null) {
               ps.close();
            }
        }
    }
}
