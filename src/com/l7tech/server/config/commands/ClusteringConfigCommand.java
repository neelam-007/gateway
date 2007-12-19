package com.l7tech.server.config.commands;

import com.l7tech.common.util.CausedIOException;
import com.l7tech.common.util.ResourceUtils;
import com.l7tech.server.config.ConfigurationType;
import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.server.config.PropertyHelper;
import com.l7tech.server.config.beans.ClusteringConfigBean;
import com.l7tech.server.config.beans.ConfigurationBean;
import com.l7tech.server.partition.PartitionInformation;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.util.Date;
import java.util.logging.Logger;

/**
 * User: megery
 * Date: Aug 16, 2005
 */
public class ClusteringConfigCommand extends BaseConfigurationCommand {
    static Logger logger = Logger.getLogger(ClusteringConfigCommand.class.getName());


    private String hostname;
    private String clusterHostname;
    private static final String BACKUP_FILE_NAME = "cluster_config_backups";
    private static final String PROP_RMI_HOSTNAME = "java.rmi.server.hostname";

    public ClusteringConfigCommand() {
        super();
    }

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
        PartitionInformation pinfo = clusterBean.getPartitionInformation();

        ConfigurationType confType = clusterBean.getConfigType();

        boolean configureCluster = confType != ConfigurationType.CONFIG_STANDALONE;

        OSSpecificFunctions osf = pinfo.getOSSpecificFunctions();
        File clusterHostNameFile = null;

        boolean hasDifferentName = !StringUtils.equalsIgnoreCase(clusterBean.getClusterHostname(),clusterBean.getLocalHostName());

        if (configureCluster || hasDifferentName)
            clusterHostNameFile = new File(osf.getClusterHostFile());

        File systemPropertiesFile = new File(osf.getSsgSystemPropertiesFile());

        File[] files = new File[]
        {   clusterHostNameFile,
            systemPropertiesFile,
        };


        backupFiles(files, BACKUP_FILE_NAME);


        String hostname = clusterBean.getClusterHostname();
        try {
            updateSystemPropertiesFile(hostname, systemPropertiesFile);
            if (configureCluster || hasDifferentName) {
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
            ClusteringConfigBean clusterBean = (ClusteringConfigBean) configBean;
            OSSpecificFunctions osf = clusterBean.getPartitionInformation().getOSSpecificFunctions();
            PropertiesConfiguration props = PropertyHelper.mergeProperties(systemPropertiesFile,
                    new File(systemPropertiesFile.getAbsolutePath() + "." + osf.getUpgradedNewFileExtension()),
                    true, true);

            props.setProperty(PROP_RMI_HOSTNAME, hostname);

            fos =   new FileOutputStream(systemPropertiesFile);
            props.setHeader("Updated by the SSG Configuration Tool\n" + new Date());
            props.save(fos, "iso-8859-1");
            logger.info("Setting " + PROP_RMI_HOSTNAME + "=" + hostname + " in system.properties file");
        } catch (FileNotFoundException e) {
            logger.severe("There was an error while updating the file: " + systemPropertiesFile.getAbsolutePath());
            logger.severe(e.getMessage());
            throw e;
        } catch (ConfigurationException e) {
            logger.severe("There was an error while updating the file: " + systemPropertiesFile.getAbsolutePath());
            logger.severe(e.getMessage());
            throw new CausedIOException(e);
        } catch (IOException e) {
            logger.severe("There was an error while updating the file: " + systemPropertiesFile.getAbsolutePath());
            logger.severe(e.getMessage());
            throw e;
        } finally {
            ResourceUtils.closeQuietly(fos);
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
