package com.l7tech.server.config.commands;

import com.l7tech.common.transport.SsgConnector;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.ResourceUtils;
import com.l7tech.server.config.EndpointActions;
import com.l7tech.server.config.PropertyHelper;
import com.l7tech.server.config.SharedWizardInfo;
import com.l7tech.server.config.beans.ConfigurationBean;
import com.l7tech.server.config.beans.EndpointConfigBean;
import com.l7tech.server.config.db.DBActions;
import com.l7tech.server.config.db.DBInformation;
import com.l7tech.server.config.db.SsgConnectorSql;
import com.l7tech.server.partition.FirewallRules;
import com.l7tech.server.partition.PartitionInformation;
import com.l7tech.server.partition.PartitionManager;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Logger;

/**
 * User: megery
 * Date: Nov 1, 2007
 * Time: 12:22:28 PM
 */
public class EndpointConfigCommand extends BaseConfigurationCommand{
    private static final Logger logger = Logger.getLogger(EndpointConfigCommand.class.getName());
    EndpointConfigBean endpointBean;
    private static final String CLUSTER_PORT_SYSPROP_KEY = "com.l7tech.server.clusterPort";

    public EndpointConfigCommand(ConfigurationBean bean) {
        super(bean);
        endpointBean = (EndpointConfigBean) configBean;
    }

    public boolean execute() {
        boolean success = true;
        try {
            PartitionInformation pinfo = PartitionManager.getInstance().getActivePartition();
            enableDbEndpointsIfNecessary();
            addEndpoints(endpointBean.getEndpointsToAdd(), "Error while adding new endpoints.");
            addEndpoints(endpointBean.getLegacyEndpoints(), "Error while adding legacy endpoints.");

            //generate them once so we have something to test against
            generateFirewallFile(pinfo, 2124);
            //then get the actual rmiPort and generate again, this time for real
            int realRmiPort = getRmiPort(pinfo);
            updateRmiPortClusterProperty(pinfo, realRmiPort);
            generateFirewallFile(pinfo, realRmiPort);
        } catch (Exception e) {
            success = false;
        }
        return success;
    }

    private void updateRmiPortClusterProperty(PartitionInformation pinfo, int rmiPort) throws ClassNotFoundException, SQLException {
        try {
            EndpointActions.updateRmiPortClusterProperty(pinfo, rmiPort);
        } catch (SQLException e) {
            logger.severe("There was an error while trying to update the internode communication port in the database. " + ExceptionUtils.getMessage(e));
            throw e;
        } catch (ClassNotFoundException e) {
            logger.severe("There was an error while trying to update the internode communication port in the database. " + ExceptionUtils.getMessage(e));
            throw e;
        }
    }

    private void generateFirewallFile(PartitionInformation pinfo, int rmiPort) throws IOException {
        Collection<SsgConnector> connectors = EndpointActions.getAllEndpoints(pinfo, true);
        try {
            EndpointActions.doFirewallConfig(pinfo, connectors, rmiPort);
        } catch (IOException e) {
            logger.severe(
                    MessageFormat.format("Error while writing the firewall rules for the {0} partition. {1}",
                                          pinfo.getPartitionId(),
                                          ExceptionUtils.getMessage(e)));
            throw e;
        }
    }

    private int getRmiPort(PartitionInformation pinfo) {
        int thePort = 2124;
        File systemProps = new File(pinfo.getOSSpecificFunctions().getSsgSystemPropertiesFile());
        if (systemProps.exists()) {
            Map<String, String> props = null;
            try {
                props = PropertyHelper.getProperties(systemProps.getPath(), new String[] {CLUSTER_PORT_SYSPROP_KEY});
                String port = props.get(CLUSTER_PORT_SYSPROP_KEY);
                if (port != null)
                    thePort = Integer.parseInt(port);
            } catch (IOException e) {
                logger.warning(MessageFormat.format("Error while trying to determine the internode communication port from system properties. {0}.", ExceptionUtils.getMessage(e)));
            } catch (NumberFormatException nfe) {
                logger.warning(MessageFormat.format("Error while trying to determine the internode communication port from system properties. {0}.", ExceptionUtils.getMessage(nfe)));
            }
        }

        FirewallRules.PortInfo portInfo = FirewallRules.getAllInfo();
        while (portInfo.isPortUsed(thePort, false, null)) {
            thePort++;
        }
        return thePort;
    }

    private void enableDbEndpointsIfNecessary() throws ClassNotFoundException, SQLException {
        PartitionInformation pinfo = PartitionManager.getInstance().getActivePartition();
        DBInformation dbinfo = SharedWizardInfo.getInstance().getDbinfo();

        if (isDefaultPartition(pinfo)) {
            if (isNewDatabase(dbinfo)) {
                logger.info("This is a new database and is the default_ partition. The default endpoints will be enabled.");
                enableDbEndpoints();
            }
        } else {
            logger.warning("The default endpoints will not be enabled. Enable them using the SecureSpan Manager if there are no conflicts.");
        }
    }

    private boolean isNewDatabase(DBInformation dbInfo) {
        return dbInfo.isNew();
    }

    private boolean isDefaultPartition(PartitionInformation pinfo) {
        return pinfo.getPartitionId().equals(PartitionInformation.DEFAULT_PARTITION_NAME);
    }

    private void enableDbEndpoints() throws SQLException, ClassNotFoundException {
        Connection conn = null;
        Statement stmt = null;
        logger.info("Enabling the default endpoints (8080, 8443 and 9443)");
        try {
            DBActions dba = new DBActions(getOsFunctions());
            conn = dba.getConnection(SharedWizardInfo.getInstance().getDbinfo());
            stmt = conn.createStatement();
            conn.setAutoCommit(false);
            stmt.execute("UPDATE connector SET enabled = 1 WHERE port = 8080");
            stmt.execute("UPDATE connector SET enabled = 1 WHERE port = 8443");
            stmt.execute("UPDATE connector SET enabled = 1 WHERE port = 9443");
            conn.commit();
            logger.info("Succesfully enabled the default endpoints");
        } catch (ClassNotFoundException e) {
            logger.severe("Error while enabling the default endpoints. " + ExceptionUtils.getMessage(e));
            throw e;
        } catch (SQLException e) {
            logger.severe("Error while enabling the default endpoints. " + ExceptionUtils.getMessage(e));
            throw e;
        } finally {
            ResourceUtils.closeQuietly(stmt);
            ResourceUtils.closeQuietly(conn);
        }

    }

    private void addEndpoints(Collection<SsgConnector> whichOnes, String errorMessage) throws SQLException, ClassNotFoundException {
        if(whichOnes != null) {
            boolean shouldRollback = false;
            Connection conn = null;
            try {
                if (!whichOnes.isEmpty()) {
                    conn = getConnection();
                    conn.setAutoCommit(false);
                    for (SsgConnector endpoint : whichOnes) {
                        String name = endpoint.getName();
                        String port = String.valueOf(endpoint.getPort());
                        logger.info("Adding endpoint (" + name + ", port " + port + ")") ;
                        new SsgConnectorSql(endpoint).save(conn);
                    }
                    conn.commit();
                    logger.info("Added all endpoints successfully");
                }
            } catch (SQLException sqlex) {
                logger.severe(errorMessage + ExceptionUtils.getMessage(sqlex));
                if (conn != null ) shouldRollback = true;
                throw sqlex;
            } catch (ClassNotFoundException e) {
                logger.severe(errorMessage + ExceptionUtils.getMessage(e));
                throw e;
            } finally {
                if (shouldRollback) conn.rollback();
                ResourceUtils.closeQuietly(conn);
            }
        }
    }

    private Connection getConnection() throws SQLException, ClassNotFoundException {
        DBActions dba = new DBActions(getOsFunctions());
        DBInformation dbinfo = SharedWizardInfo.getInstance().getDbinfo();
        return dba.getConnection(dbinfo);
    }


}
