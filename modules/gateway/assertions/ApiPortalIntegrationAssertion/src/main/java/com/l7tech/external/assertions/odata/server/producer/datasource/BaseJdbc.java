package com.l7tech.external.assertions.odata.server.producer.datasource;

import com.l7tech.external.assertions.odata.server.producer.jdbc.Jdbc;
import com.l7tech.external.assertions.odata.server.producer.jdbc.Util;
import org.odata4j.core.Throwables;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Our BaseJdbc class
 *
 * @author rraquepo, 11/25/14
 */
public abstract class BaseJdbc extends Jdbc {
    private static final Logger log = Logger.getLogger(BaseJdbc.class.getName());
    private DataSource dataSource;
    private String databaseName;
    private Connection conn;

    public BaseJdbc(final DataSource dataSource, final boolean preserveConn) {
        super(null, null, null, null);
        this.dataSource = dataSource;
        conn = getConnection();
        this.databaseName = Util.getDatabaseTypeName(conn);
        if (!preserveConn) {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    log.log(Level.SEVERE, e.getMessage(), e);
                    throw Throwables.propagate(e);
                }
            }
        }
    }


    public String getDatabaseName() {
        return databaseName;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void close() {
        if (this.conn != null) {
            try {
                this.conn.close();
                this.conn = null;
            } catch (SQLException e) {
                log.log(Level.SEVERE, e.getMessage(), e);
            }
        }
    }

    protected Connection getConnection() {
        if (this.conn == null) {
            try {
                this.conn = dataSource.getConnection();
            } catch (SQLException e) {
                log.log(Level.SEVERE, e.getMessage(), e);
                throw Throwables.propagate(e);
            }
        }
        return conn;
    }


}
