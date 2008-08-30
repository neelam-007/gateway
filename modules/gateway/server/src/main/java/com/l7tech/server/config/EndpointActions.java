package com.l7tech.server.config;

import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ResourceUtils;
import com.l7tech.server.config.db.DBActions;
import com.l7tech.server.config.db.DBInformation;
import com.l7tech.server.config.db.SsgConnectorSql;
import com.l7tech.server.partition.FirewallRules;
import com.l7tech.server.partition.PartitionInformation;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * User: megery
 * Date: Nov 1, 2007
 * Time: 4:26:18 PM
 */
public class EndpointActions {
    private static final Logger logger = Logger.getLogger(EndpointActions.class.getName());
    public static final String FIREWALL_RULES_DROPFILE = "firewall_rules";

    public static boolean isEndpointsInDb(PartitionInformation pInfo) {
        Connection conn = null;

        boolean found = false;
        try {
            conn = getConnection(pInfo);
            found = !(SsgConnectorSql.loadAll(conn).isEmpty());
        } catch (ClassNotFoundException e) {
            logger.severe("Error while reading endpoints from the database. Cannot proceed." + ExceptionUtils.getMessage(e));
            throw new RuntimeException(e);
        } catch (SQLException e) {
            logger.severe("Error while reading endpoints from the database. Cannot proceed." + ExceptionUtils.getMessage(e));
            throw new RuntimeException(e);
        } finally {
            ResourceUtils.closeQuietly(conn);
        }
        return found;
    }

    public static Collection<SsgConnector> getExistingAdminEndpoints(PartitionInformation pInfo) {
        return filterForAdminOnly(getAllEndpoints(pInfo, false));
    }

    private static Collection<SsgConnector> filterForAdminOnly(Collection<SsgConnector> allOfThem) {
        Collection<SsgConnector> filtered = new ArrayList<SsgConnector>();
        for (SsgConnector ssgConnector : allOfThem) {
            if (ssgConnector.getScheme().equals(SsgConnector.SCHEME_HTTPS) && ssgConnector.getEndpoints().toUpperCase().contains("ADMIN")) {
                filtered.add(ssgConnector);
            }
        }
        return filtered;
    }

    public static Collection<SsgConnector> getLegacyEndpoints(PartitionInformation pinfo) {
        if (isEndpointsInDb(pinfo)) return Collections.emptyList();

        Collection<SsgConnector> allEndpoints = new ArrayList<SsgConnector>();

        // TODO server.xml parsing happened here, in case we decide to care again about upgrades from pre-4.3

        Collection<SsgConnector> legacyFtp = pinfo.parseFtpEndpointsAsSsgConnectors();
        if (legacyFtp != null && !legacyFtp.isEmpty()) allEndpoints.addAll(legacyFtp);
                
        return allEndpoints;
    }

    public static void doFirewallConfig(PartitionInformation pInfo, Collection<SsgConnector> connectors, int rmiPort) throws IOException {
        OSSpecificFunctions osf = pInfo.getOSSpecificFunctions();
        if ( !(osf instanceof UnixSpecificFunctions) ) {
            return;
        } else {
            FirewallRules.writeFirewallDropfile(new File(osf.getConfigurationBase(), FIREWALL_RULES_DROPFILE).getPath(), rmiPort, connectors);
        }
    }

    public static Collection<SsgConnector> getAllEndpoints(PartitionInformation pInfo, boolean enabledOnly) {

        List<SsgConnector> found = new ArrayList<SsgConnector>();

        Connection conn = null;
        try {
            conn = getConnection(pInfo);
            Collection<SsgConnector> connInDb = SsgConnectorSql.loadAll(conn);

            //if none were found in the DB, then we should try and get the legacy ones from server.xml et al
            if (connInDb.isEmpty()) {
                Collection<SsgConnector> legacyOnes = EndpointActions.getLegacyEndpoints(pInfo);
                for (SsgConnector legacyOne : legacyOnes) {
                    addIfEnabled(legacyOne, found, enabledOnly);
                }
            } else {
                for (SsgConnector ssgConnector : connInDb) {
                    addIfEnabled(ssgConnector, found, enabledOnly);
                }
            }
        } catch (SQLException e) {
            logger.severe("Error while retrieving endpoints. " + ExceptionUtils.getMessage(e));
        } catch (ClassNotFoundException e) {
            logger.severe("Error while retrieving endpoints. " + ExceptionUtils.getMessage(e));
        } finally {
            ResourceUtils.closeQuietly(conn);
        }

        return found;
    }

    private static void addIfEnabled(SsgConnector connector, Collection<SsgConnector> connectors, boolean enabledOnly) {
        if (enabledOnly) {
            if (connector.isEnabled()) connectors.add(connector);
        } else {
            connectors.add(connector);
        }
    }

    public static void updateRmiPortClusterProperty(PartitionInformation pinfo, int rmiPort) throws SQLException, ClassNotFoundException {
        Connection conn = null;
        Statement stmt = null;
        PreparedStatement pStmt = null;
        ResultSet rs = null;

        try {
            logger.info("Updating the internode communication port in the database.");
            conn = getConnection(pinfo);
            stmt = conn.createStatement();

            rs = stmt.executeQuery("select version,propvalue from cluster_properties where propkey = 'cluster.internodePort'");
            if (rs.next()) {
                int version = rs.getInt("version");
                int existingPort = rs.getInt("propvalue");
                if (existingPort == rmiPort) {
                    logger.info("No need to update the internode communication port in the database since it has not changed.");
                } else {
                    pStmt = conn.prepareStatement("update cluster_properties SET version=?,propvalue=? WHERE propkey='cluster.internodePort'");
                    pStmt.setInt(1, version+1);
                    pStmt.setString(2, String.valueOf(rmiPort));
                }
            } else {
                pStmt = conn.prepareStatement("insert into cluster_properties (objectid, version, propkey, propvalue) VALUES (?,?,?,?)");
                pStmt.setLong(1, -2124);
                pStmt.setInt(2, 1);
                pStmt.setString(3, "cluster.internodePort");
                pStmt.setString(4, String.valueOf(rmiPort));
            }
            if (pStmt != null) {
                pStmt.execute();
                logger.info("Successfully updated the internode communication port in the database.");
            }
        } finally {
            ResourceUtils.closeQuietly(rs);
            ResourceUtils.closeQuietly(stmt);
            ResourceUtils.closeQuietly(pStmt);
            ResourceUtils.closeQuietly(conn);
        }
    }

    private static Connection getConnection(PartitionInformation pinfo) throws ClassNotFoundException, SQLException {
        OSSpecificFunctions osf = pinfo.getOSSpecificFunctions();
        DBInformation dbinfo = SharedWizardInfo.getInstance().getDbinfo();
        DBActions dba = new DBActions(osf);

        String hostname = dbinfo.getHostname();
        if (hostname.equalsIgnoreCase(SharedWizardInfo.getInstance().getRealHostname())) {
            hostname = "localhost";
        }
        return dba.getConnection(hostname, dbinfo.getDbName(), dbinfo.getUsername(), dbinfo.getPassword());
    }
}
