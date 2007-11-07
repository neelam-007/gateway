package com.l7tech.server.config.commands;

import com.l7tech.common.transport.SsgConnector;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.ResourceUtils;
import com.l7tech.server.config.SharedWizardInfo;
import com.l7tech.server.config.beans.ConfigurationBean;
import com.l7tech.server.config.beans.EndpointConfigBean;
import com.l7tech.server.config.db.DBActions;
import com.l7tech.server.config.db.DBInformation;
import com.l7tech.server.config.db.SsgConnectorSql;
import com.l7tech.server.partition.PartitionInformation;
import com.l7tech.server.partition.PartitionManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.logging.Logger;

/**
 * User: megery
 * Date: Nov 1, 2007
 * Time: 12:22:28 PM
 */
public class EndpointConfigCommand extends BaseConfigurationCommand{
    private static final Logger logger = Logger.getLogger(EndpointConfigCommand.class.getName());
    EndpointConfigBean endpointBean;
    public EndpointConfigCommand(ConfigurationBean bean) {
        super(bean);
        endpointBean = (EndpointConfigBean) configBean;
    }

    public boolean execute() {
        boolean success = true;
        Connection connection = null;
        try {
            enableDbEndpointsIfNecessary();
            addEndpoints(endpointBean.getEndpointsToAdd(), "Error while adding new endpoints.");
            addEndpoints(endpointBean.getLegacyEndpoints(), "Error while adding legacy endpoints.");
        } catch (Exception e) {
            success = false;
        } finally {
            ResourceUtils.closeQuietly(connection);
        }
        return success;
    }

    private void enableDbEndpointsIfNecessary() throws ClassNotFoundException, SQLException {
        if (shouldEnableEndpoints()) {
            logger.info("This is a new database and is the default_ partition. The default endpoints will be enabled.");
            enableDbEndpoints();
        } else {
            logger.warning("The default endpoints will not be enabled. Enable them using the SecureSpan Manager if there are no conflicts.");
        }
    }

    private boolean shouldEnableEndpoints() {
        PartitionInformation pinfo = PartitionManager.getInstance().getActivePartition();
        DBInformation dbinfo = SharedWizardInfo.getInstance().getDbinfo();
        return (dbinfo != null &&
                dbinfo.isNew() &&
                pinfo.getPartitionId().equals(PartitionInformation.DEFAULT_PARTITION_NAME));
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
