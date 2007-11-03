package com.l7tech.server.config.db;

import com.l7tech.common.transport.SsgConnector;
import com.l7tech.common.util.ResourceUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;

/**
 * Temporary kludge utility class that generates INSERT statements for connectors and their properties
 */
public class SsgConnectorSql {
    protected static final Logger logger = Logger.getLogger(SsgConnectorSql.class.getName());

    private final SsgConnector connector;

    public SsgConnectorSql(SsgConnector connector) {
        this.connector = connector;
    }

    /**
     * Delete any old data from the database for this connector and replace it with current data.
     * <p/>
     * Caller must ensure that this call happens inside a transaction.
     *
     * @param c  a JDBC connection pointed at the SSG database
     * @throws java.sql.SQLException if there is a problem saving the connector. caller must ensure that
     *         transaction gets rolled back
     */
    public void save(Connection c) throws SQLException {

        long oid = connector.getOid();
        if (oid == SsgConnector.DEFAULT_OID) {
            oid = allocateOid(c, -1200, "connector");
            connector.setOid(oid);
        }

        delete(c, "delete from connector_property where connector_oid=" + oid);
        delete(c, "delete from connector where objectid=" + oid);

        insert(c, "connector",
               oid,
               connector.getVersion(),
               connector.getName(),
               connector.isEnabled(),
               connector.getPort(),
               connector.getScheme(),
               connector.getEndpoints(),
               connector.isSecure(),
               connector.getClientAuth(),
               connector.getKeystoreOid(),
               connector.getKeyAlias());

        List<String> propNames = connector.getPropertyNames();
        for (String propName : propNames) {
            String value = connector.getProperty(propName);
            long poid = allocateOid(c, -1250, "connector_property");
            insert(c, "connector_property",
                   poid,
                   1,
                   oid,
                   propName,
                   value);
        }
    }

    /**
     * Allocate an unused OID that is less than def from the table named table.
     *
     * @param c  open DB connection
     * @param def default oid, ie -1200
     * @param table table name, ie "connector"
     * @return an OID currently unused by this table and less than def
     * @throws SQLException if db troubles
     */
    private long allocateOid(Connection c, int def, String table) throws SQLException {
        final long[] smallestUsed = { def };
        query(c, "select objectid from " + table + " where objectid < 0", new ResultVisitor() {
            public void visit(ResultSet rs) throws SQLException {
                long used = rs.getLong(1);
                if (used <= smallestUsed[0]) smallestUsed[0] = used;
            }
        });
        return --smallestUsed[0];
    }

    // Can't just use Callable since we take an arg; can't use Functions since we need to throw :/
    private interface ResultVisitor {
        void visit(ResultSet rs) throws SQLException;
    }

    final ResultVisitor nullVisitor = new ResultVisitor() {
        public void visit(ResultSet rs) throws SQLException {
        }
    };

    private void query(Connection c, String sql, ResultVisitor visitor) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = c.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) visitor.visit(rs);
        } finally {
            ResourceUtils.closeQuietly(rs);
            ResourceUtils.closeQuietly(ps);
        }
    }

    private void delete(Connection c, String sql) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = c.prepareStatement(sql);
            ps.execute(sql);
        } finally {
            ResourceUtils.closeQuietly(rs);
            ResourceUtils.closeQuietly(ps);
        }
    }

    private void insert(Connection c, String tablename, Object... properties) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            StringBuilder sql = new StringBuilder("insert into " + tablename + " values(");
            boolean first = true;
            //noinspection UnusedDeclaration
            for (Object property : properties) {
                if (!first) sql.append(",");
                first = false;
                sql.append("?");
            }
            sql.append(")");

            ps = c.prepareStatement(sql.toString());
            for (int i = 0; i < properties.length; i++)
                ps.setObject(i + 1, properties[i]);
            ps.execute();

        } finally {
            ResourceUtils.closeQuietly(rs);
            ResourceUtils.closeQuietly(ps);
        }
    }
}
