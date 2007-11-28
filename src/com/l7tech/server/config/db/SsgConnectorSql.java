package com.l7tech.server.config.db;

import com.l7tech.common.transport.SsgConnector;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.ResourceUtils;

import java.lang.reflect.Method;
import java.sql.*;
import java.sql.Date;
import java.util.*;
import java.util.logging.Logger;

/**
 * Temporary kludge utility class that generates INSERT statements for connectors and their properties
 */
public class SsgConnectorSql {
    protected static final Logger logger = Logger.getLogger(SsgConnectorSql.class.getName());

    private final SsgConnector connector;
    private static final Method[] EMPTY_METHOD_ARRAY = new Method[0];

    public SsgConnectorSql(SsgConnector connector) {
        this.connector = connector;
    }

    /**
     * Loads all connectors from the database.
     *
     * @param c the DB connection from which to load the connectors
     * @return a collection of all SsgConnector instances.  May be empty but won't ever be null.
     * @throws SQLException if there is a problem loading connectors from the database
     */
    public static Collection<SsgConnector> loadAll(Connection c) throws SQLException {
        Collection<SsgConnector> connectors =
                doLoadAll(c, "connector", SsgConnector.class,
                        "objectid", "enabled", "name", "port", "scheme", "secure", "endpoints", "client_auth", "keystore_oid", "key_alias");

        // Fill in properties
        for (final SsgConnector connector : connectors) {
            long oid = connector.getOid();
            query(c, "select name,value from connector_property where connector_oid=" + oid, new ResultVisitor() {
                public void visit(ResultSet rs) throws SQLException {
                    String name = rs.getString(1);
                    String value = rs.getString(2);
                    connector.putProperty(name, value);
                }
            });
        }

        return connectors;
    }

    private static <T> Collection<T> doLoadAll(Connection c, String table, final Class<T> clazz, final String... columnNames) throws SQLException {
        StringBuffer sql = HexUtils.join(new StringBuffer("select "), ",", columnNames).append(" from ").append(table);
        final List<T> ret = new ArrayList<T>();
        final Method[] methods = getSetterNames(clazz, columnNames);
        query(c, sql.toString(), new ResultVisitor() {
            public void visit(ResultSet rs) throws SQLException {
                ret.add(instantiate(clazz, rs, methods));
            }
        });
        return ret;
    }

    private static String toMethodName(String columnName) {
        if (columnName.equals("objectid"))
            return "Oid"; // TODO express this nonstandard mapping declaratively

        StringBuilder ret = new StringBuilder();
        char[] chars = columnName.toCharArray();
        boolean ucNext = true;
        for (char c : chars) {
            if (c == '_') {
                ucNext = true;
            } else {
                if (ucNext) {
                    ret.append(Character.toUpperCase(c));
                    ucNext = false;
                } else {
                    ret.append(c);
                }
            }
        }

        return ret.toString();
    }

    private static <T> Method[] getSetterNames(Class<T> clazz, String[] columnNames) {
        List<Method> ret = new ArrayList<Method>();
        Map<String, Method> setterMap = findSetters(clazz);
        for (String col : columnNames) {
            String methName = "set" + toMethodName(col);
            Method method = setterMap.get(methName);
            if (method == null)
                throw new RuntimeException("Unable to find setter in class " + clazz + " named " + methName);
            ret.add(method);
        }
        return ret.toArray(EMPTY_METHOD_ARRAY);
    }

    private static Map<String, Method> findSetters(Class clazz) {
        Map<String, Method> ret = new LinkedHashMap<String, Method>();
        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            if (method.getName().startsWith("set") && method.getParameterTypes().length == 1)
                ret.put(method.getName(), method);
        }
        return ret;
    }

    private static <T> T instantiate(Class<T> clazz, ResultSet row, Method[] setters) throws SQLException {
        T target = null;
        try {
            target = clazz.newInstance();
        } catch (Exception e) {
            throw new SQLException("Unable to instantiate object of class " + clazz + " from row in DB: " +
                                   "unable to instantiate bean: " + ExceptionUtils.getMessage(e), e);
        }
        for (int i = 0; i < setters.length; i++) {
            Method method = setters[i];
            Object value = getColumnValue(row, i + 1, setterType(method));

            try {
                method.invoke(target, value);
            } catch (Exception e) {
                throw new SQLException("Unable to instantiate object of class " + clazz.getSimpleName() + " from row in DB (" +
                                       "column name " + row.getMetaData().getColumnName(i) +
                                       " gave type " + typeOf(value) + "; setter " + method.getName() + " wants type " + setterType(method) +
                                       "): " + ExceptionUtils.getMessage(e), e);
            }
        }
        return target;
    }

    private static Object getColumnValue(ResultSet row, int i, Class type) throws SQLException {
        if (Boolean.class.equals(type) || boolean.class.equals(type)) {
            return row.getBoolean(i);
        } else if (String.class.equals(type)) {
            return row.getString(i);
        } else if (Long.class.isAssignableFrom(type) || long.class.equals(type)) {
            return row.getLong(i);
        } else if (Integer.class.isAssignableFrom(type) || int.class.equals(type)) {
            return row.getInt(i);
        } else if (Short.class.isAssignableFrom(type) || short.class.equals(type)) {
            return row.getShort(i);
        } else if (Float.class.isAssignableFrom(type) || float.class.equals(type)) {
            return row.getFloat(i);
        } else if (Double.class.isAssignableFrom(type) || double.class.equals(type)) {
            return row.getDouble(i);
        } else if (Timestamp.class.isAssignableFrom(type)) {
            return row.getTimestamp(i);
        } else if (Time.class.isAssignableFrom(type)) {
            return row.getTime(i);
        } else if (Date.class.isAssignableFrom(type)) {
            return row.getDate(i);
        }
        return row.getObject(i);
    }

    private static Class setterType(Method value) {
        return value.getParameterTypes()[0];
    }

    private static String typeOf(Object value) {
        return value == null ? "<null>" : value.getClass().toString();        
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

    private static void query(Connection c, String sql, ResultVisitor visitor) throws SQLException {
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
