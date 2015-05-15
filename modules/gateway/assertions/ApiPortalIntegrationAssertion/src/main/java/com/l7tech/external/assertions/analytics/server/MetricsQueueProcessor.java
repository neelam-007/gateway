package com.l7tech.external.assertions.analytics.server;

import com.l7tech.external.assertions.analytics.AnalyticsAssertion;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.jdbc.JdbcConnectionManager;
import com.l7tech.server.jdbc.JdbcConnectionPoolManager;
import com.l7tech.util.Background;
import com.l7tech.util.Config;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.TimeUnit;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author rraquepo, 8/6/14
 */
public class MetricsQueueProcessor {
  private static final Logger logger = Logger.getLogger(MetricsQueueProcessor.class.getName());

  private static ConcurrentLinkedQueue<HitsMetric> HITS_QUEUE = new ConcurrentLinkedQueue<>();

  private static MetricsQueueProcessor instance;
  private final JdbcConnectionPoolManager jdbcConnectionPoolManager;
  private final JdbcConnectionManager jdbcConnectionManager;
  private final Config config;

  private final String INSERT_RAW_ANALYTICS = "INSERT INTO API_METRICS (UUID, SSG_NODE_ID, SSG_REQUEST_ID, RESOLUTION, " +
          "RESOLUTION_TIME_INTERVAL, ROLLUP_START_TIME, SSG_REQUEST_START_TIME, SSG_REQUEST_END_TIME, " +
          "SSG_SERVICE_ID, SSG_PORTAL_API_ID, REQUEST_IP, " +
          "HTTP_METHOD, HTTP_PUT_COUNT, HTTP_POST_COUNT, HTTP_DELETE_COUNT, HTTP_GET_COUNT, HTTP_OTHER_COUNT, " +
          "SERVICE_URI, AUTH_TYPE, HTTP_RESPONSE_STATUS, " +
          "SUCCESS_COUNT, ERROR_COUNT, PROXY_LATENCY, " +
          "BACKEND_LATENCY, TOTAL_LATENCY, " +
          "APPLICATION_UUID, ORGANIZATION_UUID, ACCOUNT_PLAN_UUID, API_PLAN_UUID, " +
          "CUSTOM_TAG1, CUSTOM_TAG2, CUSTOM_TAG3, CUSTOM_TAG4, CUSTOM_TAG5) VALUES" +
          "(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";


  public static MetricsQueueProcessor getInstance(final JdbcConnectionPoolManager jdbcConnectionPoolManager, JdbcConnectionManager jdbcConnectionManager, final Config config) {
    if (instance == null) {
      instance = new MetricsQueueProcessor(jdbcConnectionPoolManager, jdbcConnectionManager, config);
    }
    return instance;
  }

  public static MetricsQueueProcessor getInstance() {
    return instance;

  }

  private MetricsQueueProcessor() {
    throw new IllegalArgumentException("Please use getInstance");
  }

  private MetricsQueueProcessor(final JdbcConnectionPoolManager jdbcConnectionPoolManager, final JdbcConnectionManager jdbcConnectionManager, final Config config) {
    this.jdbcConnectionPoolManager = jdbcConnectionPoolManager;
    this.jdbcConnectionManager = jdbcConnectionManager;
    this.config = config;
    scheduleTask();
  }

  public void add(HitsMetric hits) {
    getQueue().add(hits);
  }

  public ConcurrentLinkedQueue<HitsMetric> getQueue() {
    return HITS_QUEUE;
  }


  void processHitsMetrics() {
    Connection conn = null;
    try {
      int DEFAULT_BATCH_SIZE;
      try {
        DEFAULT_BATCH_SIZE = config.getIntProperty(AnalyticsAssertion.PARAM_ANALYTICS_CAPTURE_BATCH_SIZE, AnalyticsAssertion.ANALYTICS_CAPTURE_BATCH_SIZE_DEFAULT);
      } catch (Exception e) {
        logger.warning(ExceptionUtils.getMessageWithCause(e));
        DEFAULT_BATCH_SIZE = 10;
      }
      final List<HitsMetric> hits;
      if (getQueue().isEmpty()) {
        logger.log(Level.FINE, "no captured metrics to write");
        return;
      } else {
        int ctr = 0;
        hits = new ArrayList<>();
        while (ctr < DEFAULT_BATCH_SIZE) {
          HitsMetric hit = HITS_QUEUE.poll();
          if (hit != null) {
            hits.add(hit);
          } else {
            break;
          }
          ctr++;
        }
      }
      long start = System.currentTimeMillis();
      String connName = hits.get(0).getConnectionName();

      try {
        final JdbcConnection l7connection;
        try {
          l7connection = jdbcConnectionManager.getJdbcConnection(connName);
          if (l7connection == null) {
            throw new FindException();
          }
        } catch (FindException e) {
          String errorMsg = "Could not find JDBC connection: " + connName;
          logger.log(Level.INFO, errorMsg);
          return;
        }

        final DataSource dataSource;
        try {
          dataSource = jdbcConnectionPoolManager.getDataSource(connName);
          if (dataSource == null) {
            throw new FindException();
          }
          conn = dataSource.getConnection();
          conn.setAutoCommit(false);
        } catch (FindException e) {
          String errorMsg1 = "Count not get a DataSource from the pool: " + connName;
          String errorMsg2 = ExceptionUtils.getMessage(ExceptionUtils.unnestToRoot(e));
          logger.log(Level.INFO, errorMsg1 + errorMsg2);
          return;
        }
      } catch (Exception e) {
        String errorMsg = ExceptionUtils.getMessage(ExceptionUtils.unnestToRoot(e));
        logger.log(Level.INFO, errorMsg);
        return;
      }
      PreparedStatement pstmt = conn.prepareStatement(INSERT_RAW_ANALYTICS);
      logger.log(Level.INFO, "Processing " + hits.size() + " metrics... " + HITS_QUEUE.size());
      for (HitsMetric metric : hits) {
        String uuid = UUID.randomUUID().toString();
        pstmt.setString(1, uuid);
        pstmt.setString(2, metric.getSsgNodeId());//SSG_NODE_ID
        pstmt.setString(3, metric.getSsgRequestId());//SSG_REQUEST_ID
        pstmt.setInt(4, 0);//RESOLUTION
        pstmt.setInt(5, 0);//RESOLUTION_TIME_INTERVAL
        pstmt.setInt(6, 0);//ROLLUP_START_TIME
        pstmt.setString(7, metric.getSsgRequestStartTime());//SSG_REQUEST_START_TIME
        pstmt.setLong(8, metric.getSsgRequestEndTime());//SSG_REQUEST_END_TIME
        pstmt.setString(9, "");//SSG_SERVICE_ID
        pstmt.setString(10, metric.getSsgPortalApiId());//SSG_PORTAL_API_ID
        pstmt.setString(11, metric.getRequestIp());//REQUEST_IP
        pstmt.setString(12, metric.getHttpMethod());//HTTP_METHOD
        pstmt.setInt(13, metric.getHttpPutCount());//HTTP_PUT_COUNT
        pstmt.setInt(14, metric.getHttpPostCount());//HTTP_POST_COUNT
        pstmt.setInt(15, metric.getHttpDeleteCount());//HTTP_DELETE_COUNT
        pstmt.setInt(16, metric.getHttpGetCount());//HTTP_GET_COUNT
        pstmt.setInt(17, metric.getHttpOtherCount());//HTTP_OTHER_COUNT
        pstmt.setString(18, metric.getServiceUri());//SERVICE_URI
        pstmt.setString(19, metric.getAuthType());//AUTH_TYPE
        pstmt.setInt(20, metric.getHttpResponseStatus());//HTTP_RESPONSE_STATUS
        pstmt.setInt(21, metric.getSuccessCount());//SUCCESS_COUNT
        pstmt.setInt(22, metric.getErrorCount());//ERROR_COUNT
        pstmt.setInt(23, metric.getProxyLatency());//PROXY_LATENCY
        pstmt.setInt(24, metric.getBackendLatency());//BACKEND_LATENCY
        pstmt.setInt(25, metric.getTotalLatency());//TOTAL_LATENCY
        pstmt.setString(26, metric.getApplicationUuid());//APPLICATION_UUID
        pstmt.setString(27, metric.getOrganizationUuid());//ORGANIZATION_UUID
        pstmt.setString(28, metric.getAccountPlanUuid());//ACCOUNT_PLAN_UUID
        pstmt.setString(29, metric.getApiPlanUuid());//API_PLAN_UUID
        pstmt.setString(30, metric.getCustomTag1());//CUSTOM_TAG1
        pstmt.setString(31, metric.getCustomTag2());//CUSTOM_TAG2
        pstmt.setString(32, metric.getCustomTag3());//CUSTOM_TAG3
        pstmt.setString(33, metric.getCustomTag4());//CUSTOM_TAG4
        pstmt.setString(34, metric.getCustomTag5());//CUSTOM_TAG5
        pstmt.addBatch();
      }
      int[] count = pstmt.executeBatch();
      conn.commit();
      long total = System.currentTimeMillis() - start;
      logger.log(Level.INFO, "Processed " + count.length + " metrics in " + (total / 1000) + "ms");

    } catch (Exception e) {
      logger.warning(ExceptionUtils.getMessageWithCause(e));
      String errorMsg = ExceptionUtils.getMessage(ExceptionUtils.unnestToRoot(e));
      logger.log(Level.INFO, errorMsg);
    } finally {
      if (conn != null) {
        try {
          conn.close(); //explicitly close the connection, so that datasource can be returned to the pool.
        } catch (SQLException e) {
          logger.warning(ExceptionUtils.getMessageWithCause(e));
        }
        conn = null;
      }
    }

  }

  //exposed as protected only for testing purposes
  protected void scheduleTask() {
    // Clear cache daily to prevent it from growing without bound
    long intervalTime = ConfigFactory.getLongProperty("com.l7tech.analytics.capture.writeInterval", TimeUnit.SECONDS.toMillis(3));
    Background.scheduleRepeated(new TimerTask() {
      @Override
      public void run() {
        logger.log(Level.FINE, "MetricsQueueProcessor triggered");
        processHitsMetrics();
      }
    }, intervalTime, intervalTime);
  }

  //exposed as protected only for testing purposes
  protected void setQueue(ConcurrentLinkedQueue<HitsMetric> queue) {
    this.HITS_QUEUE = queue;
  }
}
