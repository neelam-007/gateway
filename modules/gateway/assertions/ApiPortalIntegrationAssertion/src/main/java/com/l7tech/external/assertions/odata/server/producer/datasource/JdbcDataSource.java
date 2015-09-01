package com.l7tech.external.assertions.odata.server.producer.datasource;

import org.core4j.ThrowingFunc1;
import org.odata4j.core.Throwables;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A Jdbc class implementation that works with DataSource
 *
 * @author rraquepo, 8/15/13
 */
public class JdbcDataSource extends BaseJdbc {

    private static final Logger log = Logger.getLogger(JdbcDataSource.class.getName());
    private boolean reuseConnection;

    public JdbcDataSource(final DataSource dataSource, final boolean reuseConnection) {
        super(dataSource, reuseConnection);
        this.reuseConnection = reuseConnection;
    }

    @Override
    public <T> T execute(final ThrowingFunc1<Connection, T> execute) {
        Connection conn = null;
        final long startTime = System.nanoTime();
        try {
            conn = getDataSource().getConnection();
            T result = execute.apply(conn);
            return result;
        } catch (Exception e) {
            log.log(Level.SEVERE, e.getMessage(), e);
            throw Throwables.propagate(e);
        } finally {
            final long endTime = System.nanoTime();
            final double elapseTime = (endTime - startTime) / (double) 1000 / (double) 1000;
            log.info("jdbc execute/apply time " + elapseTime + " msec");
            if (conn != null && !reuseConnection) {
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
