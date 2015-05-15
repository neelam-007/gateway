package com.l7tech.external.assertions.odata.server.producer.datasource;

import org.core4j.ThrowingFunc1;
import org.odata4j.core.Throwables;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author rraquepo, 11/25/14
 */
public class TransactionalJdbcDataSource extends BaseJdbc {
    private static final Logger log = Logger.getLogger(TransactionalJdbcDataSource.class.getName());
    private final Connection conn;

    public TransactionalJdbcDataSource(final DataSource dataSource) {
        super(dataSource, true);
        try {
            conn = getConnection();
            conn.setAutoCommit(false);
        } catch (SQLException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public <T> T execute(final ThrowingFunc1<Connection, T> execute) {
        final long startTime = System.nanoTime();
        try {
            T result = execute.apply(conn);
            return result;
        } catch (Exception e) {
            log.log(Level.SEVERE, e.getMessage(), e);
            throw Throwables.propagate(e);
        } finally {
            final long endTime = System.nanoTime();
            final double elapseTime = (endTime - startTime) / (double) 1000 / (double) 1000;
            log.info("jdbc execute/apply time " + elapseTime + " msec");
        }
    }

    public void commit() {
        if (conn == null) {
            log.warning("Connection object is null");
            return;
        }
        try {
            conn.commit();
        } catch (SQLException e) {
            log.log(Level.SEVERE, e.getMessage(), e);
            throw Throwables.propagate(e);
        } finally {
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

    public void rollback() {
        if (conn == null) {
            log.warning("Connection object is null");
            return;
        }
        try {
            conn.rollback();
        } catch (SQLException e) {
            log.log(Level.SEVERE, e.getMessage(), e);
            throw Throwables.propagate(e);
        } finally {
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
}
