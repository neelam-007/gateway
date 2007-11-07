package com.l7tech.server.config;

import com.l7tech.common.transport.SsgConnector;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.ResourceUtils;
import com.l7tech.server.config.db.DBActions;
import com.l7tech.server.config.db.DBInformation;
import com.l7tech.server.partition.PartitionInformation;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

/**
 * User: megery
 * Date: Nov 1, 2007
 * Time: 4:26:18 PM
 */
public class EndpointActions {
    private static final Logger logger = Logger.getLogger(EndpointActions.class.getName());

    public static boolean isEndpointsInDb(PartitionInformation pInfo) {
        boolean found = false;
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            DBActions dba = new DBActions(pInfo.getOSSpecificFunctions());
            conn = dba.getConnection(SharedWizardInfo.getInstance().getDbinfo());
            stmt = conn.createStatement();
            rs = stmt.executeQuery("select * from connector");
            if(rs.next()) {
                found = true;
            }
        } catch (ClassNotFoundException e) {
            logger.severe("Error while reading endpoints from the database. Cannot proceed." + ExceptionUtils.getMessage(e));
            throw new RuntimeException(e);
        } catch (SQLException e) {
            logger.severe("Error while reading endpoints from the database. Cannot proceed." + ExceptionUtils.getMessage(e));
            throw new RuntimeException(e);
        } finally {
            ResourceUtils.closeQuietly(rs);
            ResourceUtils.closeQuietly(stmt);
            ResourceUtils.closeQuietly(conn);
        }
        return found;
    }

    public static List<SsgConnector> getExistingAdminEndpoints(PartitionInformation pInfo) {
        List<SsgConnector> found = new ArrayList<SsgConnector>();
        DBInformation dbinfo = SharedWizardInfo.getInstance().getDbinfo();

        //if there are no endpoints in the db, then look in server.xml for more
        if (!isEndpointsInDb(pInfo)) {
            Collection<SsgConnector> legacyOnes = EndpointActions.getLegacyEndpoints(pInfo);
            for (SsgConnector legacyOne : legacyOnes) {
                if (legacyOne.getScheme().equals(SsgConnector.SCHEME_HTTPS) && legacyOne.getEndpoints().toUpperCase().contains("ADMIN")) {
                    found.add(legacyOne);
                }
            }
        } else {
            Connection conn = null;
            Statement stmt = null;
            ResultSet rs = null;
            try {
                OSSpecificFunctions osf = pInfo.getOSSpecificFunctions();
                DBActions dba = new DBActions(osf);
                conn = dba.getConnection(dbinfo);
                stmt = conn.createStatement();
                rs = stmt.executeQuery("select * from connector where scheme = 'https' and endpoints like '%admin%'");

                while(rs.next()) {
                    SsgConnector connector = new SsgConnector();
                    connector.setName(rs.getString("name"));
                    connector.setPort(rs.getInt("port"));
                    connector.setEndpoints(rs.getString("endpoints"));
                    connector.setEnabled(rs.getInt("enabled") ==1);
                    found.add(connector);
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                ResourceUtils.closeQuietly(rs);
                ResourceUtils.closeQuietly(stmt);
                ResourceUtils.closeQuietly(conn);
            }
        }
        return found;
    }

    public static Collection<SsgConnector> getLegacyEndpoints(PartitionInformation pinfo) {
        if (isEndpointsInDb(pinfo)) return null;

        Collection<SsgConnector> allEndpoints = new ArrayList<SsgConnector>();

        Collection<SsgConnector> legacyHttp = pinfo.getConnectorsFromServerXml();
        if (legacyHttp != null && !legacyHttp.isEmpty()) allEndpoints.addAll(legacyHttp);

        Collection<SsgConnector> legacyFtp = pinfo.parseFtpEndpointsAsSsgConnectors();
        if (legacyFtp != null && !legacyFtp.isEmpty()) allEndpoints.addAll(legacyFtp);
                
        return allEndpoints;
    }
}
