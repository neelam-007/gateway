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

import java.sql.Connection;
import java.sql.SQLException;
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
            addEndpoints(endpointBean.getEndpointsToAdd(), "Error while adding new endpoints.");
            addEndpoints(endpointBean.getLegacyEndpoints(), "Error while adding legacy endpoints.");
        } catch (Exception e) {
            success = false;
        } finally {
            ResourceUtils.closeQuietly(connection);
        }
        return success;
    }

    private void addEndpoints(Collection<SsgConnector> whichOnes, String errorMessage) throws SQLException, ClassNotFoundException {
        if(whichOnes != null) {
            boolean shouldRollback = false;
            Connection conn = null;
            try {
                conn = getConnection();
                conn.setAutoCommit(false);
                for (SsgConnector endpoint : whichOnes) {
                    logger.info("Adding endpoint (" + endpoint.getName() + ")") ;
                    new SsgConnectorSql(endpoint).save(conn);
                }
                conn.commit();
                logger.info("Added all endpoints successfully");
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
