package com.l7tech.server.util;

import com.l7tech.server.RuntimeLifecycleException;
import com.l7tech.util.BuildInfo;
import com.l7tech.util.ResourceUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.jdbc.Work;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ApplicationObjectSupport;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

/**
 * Extremely simple bean with no dependencies on any Hibernate managers (not even cluster property manager)
 * that checks the Gateway database version number and aborts startup if it is incorrect.
 */
public class GatewayDbVersionChecker extends ApplicationObjectSupport implements InitializingBean {

    private static final Logger logger = Logger.getLogger(GatewayDbVersionChecker.class.getName());

    public static final String SSG_VERSION_TABLE="ssg_version";
    public static final String CURRENT_VERSION_COLUMN="current_version";
    public static final String CHECK_VERSION_STMT="SELECT " + CURRENT_VERSION_COLUMN + " FROM " + SSG_VERSION_TABLE;

    @Override
    public void afterPropertiesSet() throws Exception {
        ApplicationContext appCtx = getApplicationContext();

        //Check if the DB is the right version. This will only work for newer (5.0+) gateways, but it's at least a good start.
        SessionFactory sf = ((SessionFactory)appCtx.getBean("sessionFactory"));

        Session session;
        session = sf.openSession();

        final String myVersion = BuildInfo.getFormalProductVersion();
        session.doWork(new Work() {
            @Override
            public void execute(Connection connection) throws SQLException {
                Statement st =  null;
                ResultSet rs = null;

                try {
                    st = connection.createStatement();
                    String errMsg = null;
                    try {
                        rs = st.executeQuery(CHECK_VERSION_STMT);
                        if (rs.next()) {
                            //we have a table, now check the contents
                            String dbVersion = rs.getString(CURRENT_VERSION_COLUMN);

                            // Log the Gateway and Database versions to the logger
                            logger.info("Gateway version: " + myVersion);
                            logger.info("Database version: " + dbVersion);

                            // Check if the gateway and database version are compatible
                            // Compatibility between the Gateway and Database is defined as follows:
                            // Rule: (1) database Version must be greater than or equal to the gateway version
                            //       (2) database Version can be at most 2 major versions higher than the gateway version
                            if (!BuildInfo.isGatewayVersionCompatibleWithDBVersion(myVersion, dbVersion)) {
                                errMsg = "The version mismatch between the database (version " + dbVersion +
                                        ") and Gateway (version " + myVersion + ") is too great." +
                                        "The database version must be within two major versions of the Gateway and cannot be lower." +
                                        "To resolve, either upgrade the database or the Gateway.";
                            }
                        } else {
                            errMsg = "Could not find a correct version (" + myVersion + ") in the database. Please check or upgrade the database before starting the gateway.";
                        }
                    } catch (SQLException e) {
                        errMsg = "The database is not an Gateway Database or is not the right version (" + myVersion + "). Please check or upgrade the database before starting the gateway.";
                    }
                    if (errMsg != null) throw new RuntimeLifecycleException(errMsg);
                } finally {
                    ResourceUtils.closeQuietly(rs);
                    ResourceUtils.closeQuietly(st);
                }
            }
        });
    }


}
