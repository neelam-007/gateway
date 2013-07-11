package com.l7tech.server.upgrade;

import com.l7tech.objectmodel.Goid;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.jdbc.Work;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import java.security.SecureRandom;
import java.sql.*;

/**
 * This is used to upgrade the entity oid's to goid's
 *
 * @author Victor Kazakov
 */
public class Upgrade71To80DerbyOids implements UpgradeTask {

    private static final String SQL_GET_OID =
            "select objectid from %s";
    private static final String SQL_UPDATE_GOID =
            "UPDATE %s SET goid = ? where objectid = ?";

    //This is the list of tables to update.
    private static final String[] tables = new String[]{"jdbc_connection", "logon_info"};

    @Override
    public void upgrade(ApplicationContext applicationContext) throws NonfatalUpgradeException, FatalUpgradeException {
        /*
         * This is the state of the database right now:
         * the goid column will have been added but its values will all be null and it does not yet have the not null constraint.
         */

        SessionFactory sessionFactory;
        Session session;
        try {
            sessionFactory = (SessionFactory) applicationContext.getBean("sessionFactory");
            if (sessionFactory == null)
                throw new FatalUpgradeException("Couldn't get required components (sessionFactory)");
            session = sessionFactory.getCurrentSession();
            if (session == null) throw new FatalUpgradeException("Couldn't get required components (session)");
        } catch (BeansException e) {
            throw new FatalUpgradeException("Couldn't get required components");
        }

        SecureRandom random = new SecureRandom();
        for (final String table : tables) {
            //This is the table prefix for the goid
            final Long tablePrefix = random.nextLong();
            session.doWork(new Work() {
                @Override
                public void execute(Connection connection) throws SQLException {
                    Statement listOidStmt = connection.createStatement();
                    //list all the entity oid's
                    ResultSet rs = listOidStmt.executeQuery(String.format(SQL_GET_OID, table));

                    PreparedStatement updateGoidStmt = connection.prepareStatement(String.format(SQL_UPDATE_GOID, table));
                    while (rs.next()) {
                        long oid = rs.getLong(1);
                        updateGoidStmt.setBytes(1, new Goid(tablePrefix, oid).getBytes());
                        updateGoidStmt.setLong(2, oid);
                        //update the goid column to contain the new goid value based on the table prefix and the oid
                        updateGoidStmt.executeUpdate();
                        updateGoidStmt.clearParameters();
                    }
                    connection.commit();
                    listOidStmt.close();
                    updateGoidStmt.close();
                }
            });

            session.doWork(new Work() {
                @Override
                public void execute(Connection connection) throws SQLException {
                    Statement stmt = connection.createStatement();
                    //make the goid column not null
                    stmt.executeUpdate("ALTER TABLE " + table + " ALTER COLUMN goid NOT NULL");
                    //drop the primary key constraint(oid)
                    stmt.executeUpdate("ALTER TABLE " + table + " DROP PRIMARY KEY");
                    //drop the objectid column
                    stmt.executeUpdate("ALTER TABLE " + table + " DROP COLUMN objectid");
                    //make the goid a primary key
                    stmt.executeUpdate("ALTER TABLE " + table + " ADD PRIMARY KEY (goid)");
                    connection.commit();
                    stmt.close();
                }
            });
        }
    }
}
