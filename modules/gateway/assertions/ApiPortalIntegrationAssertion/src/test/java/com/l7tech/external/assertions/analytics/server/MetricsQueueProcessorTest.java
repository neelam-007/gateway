package com.l7tech.external.assertions.analytics.server;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.server.jdbc.JdbcConnectionManager;
import com.l7tech.server.jdbc.JdbcConnectionPoolManager;
import com.l7tech.util.Config;
import java.sql.Connection;
import org.hsqldb.jdbc.JDBCDataSource;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * @author rraquepo, 8/19/14
 */
@Ignore
public class MetricsQueueProcessorTest {
  @Mock
  private JdbcConnectionManager jdbcConnectionManager;
  @Mock
  private JdbcConnectionPoolManager jdbcConnectionPoolManager;
  @Mock
  private JdbcConnection jdbcConnection;
  @Mock
  private Connection connection;
  @Mock
  private JDBCDataSource dataSource;
  @Mock
  private Config config;
  final String connectionName = "hsqldb";

  final int REC_COUNT = 45;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    when(jdbcConnectionManager.getJdbcConnection(Matchers.eq(connectionName))).thenReturn(jdbcConnection);
    when(jdbcConnectionPoolManager.getDataSource(Matchers.eq(connectionName))).thenReturn(dataSource);
    when(dataSource.getConnection()).thenReturn(connection);
  }

  @Test
  public void testQueue_and_connection_close() throws Exception {
    final MetricsQueueProcessor queueProcessor = MetricsQueueProcessor.getInstance(jdbcConnectionPoolManager, jdbcConnectionManager, config);
    for (int i = 0; i < REC_COUNT; i++) {
      HitsMetric metric = new HitsMetric();
      metric.setConnectionName(connectionName);
      queueProcessor.add(metric);
    }
    int execNum = (int) Math.ceil(REC_COUNT / 10) + 1;
    Thread.sleep((execNum * 3 * 1000) + 100);//introduce some delay to give the schedule some time to kick-in and complete the queue
    verify(connection, times(execNum)).close();
    assertEquals(queueProcessor.getQueue().size(), 0);
  }

}
