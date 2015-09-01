package com.l7tech.external.assertions.analytics.server;

import com.l7tech.external.assertions.analytics.AnalyticsAssertion;
import com.l7tech.gateway.common.LicenseException;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.jdbc.JdbcConnectionManager;
import com.l7tech.server.jdbc.JdbcConnectionPoolManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.composite.ServerCompositeAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.Config;
import com.l7tech.util.ExceptionUtils;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.springframework.context.ApplicationContext;

/**
 * @author rraquepo, 7/4/14
 */
public class ServerAnalyticsAssertion extends ServerCompositeAssertion<AnalyticsAssertion> {
  private static final Logger logger = Logger.getLogger(ServerAnalyticsAssertion.class.getName());
  private final JdbcConnectionPoolManager jdbcConnectionPoolManager;
  private final JdbcConnectionManager jdbcConnectionManager;
  private final MetricsQueueProcessor metricsQueueProcessor;
  private final Config config;

  private final AssertionResultListener assertionResultListener = new AssertionResultListener() {
    @Override
    public boolean assertionFinished(PolicyEnforcementContext context, AssertionStatus result) {
      if (result != AssertionStatus.NONE) {
        seenAssertionStatus(context, result);
        rollbackDeferredAssertions(context);
        return false;
      }
      return true;
    }
  };

  public ServerAnalyticsAssertion(AnalyticsAssertion data, ApplicationContext context) throws PolicyAssertionException, LicenseException {
    super(data, context);
    if (context == null) {
      throw new IllegalStateException("Application context cannot be null.");
    }
    jdbcConnectionPoolManager = context.getBean("jdbcConnectionPoolManager", JdbcConnectionPoolManager.class);
    jdbcConnectionManager = context.getBean("jdbcConnectionManager", JdbcConnectionManager.class);
    config = context.getBean("serverConfig", Config.class);
    metricsQueueProcessor = MetricsQueueProcessor.getInstance(jdbcConnectionPoolManager, jdbcConnectionManager, config);
    if (assertion.getConnectionName() == null) {
      throw new PolicyAssertionException(assertion, "Assertion must supply a connection name");
    }
  }

  @Override
  public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
    if (context == null) {
      throw new IllegalStateException("Policy Enforcement Context cannot be null.");
    }
    long assertion_start_time = context.getStartTime();
    final String[] initialVariablesUsed = Syntax.getReferencedNames(assertion.getConnectionName(), "${gateway.time}");
    final Map<String, Object> variableMap = context.getVariableMap(initialVariablesUsed, getAudit());
    final String connName = ExpandVariables.process(assertion.getConnectionName(), variableMap, getAudit());
    final String gatewayTimeStart = ExpandVariables.process("${gateway.time.millis}", variableMap, getAudit());
    //validate that the connection exists.
    final JdbcConnection connection;
    try {
      connection = jdbcConnectionManager.getJdbcConnection(connName);
      if (connection == null) {
        throw new FindException();
      }
    } catch (Exception e) {
      String errorMsg = "Could not find JDBC connection: " + connName;
      logAndAudit(AssertionMessages.EXCEPTION_INFO_WITH_MORE_INFO, errorMsg);
      return AssertionStatus.FAILED;
    }

    final DataSource dataSource;
    try {
      dataSource = jdbcConnectionPoolManager.getDataSource(connName);
      if (dataSource == null) {
        throw new FindException();
      }
    } catch (Exception e) {
      String errorMsg1 = "Count not get a DataSource from the pool: " + connName;
      String errorMsg2 = ExceptionUtils.getMessage(ExceptionUtils.unnestToRoot(e));
      logAndAudit(AssertionMessages.EXCEPTION_INFO_WITH_MORE_INFO, new String[]{errorMsg1, errorMsg2});
      return AssertionStatus.FAILED;
    }
    //run the actual assertions
    AssertionStatus status = iterateChildren(context, assertionResultListener);

    //for latency computation
    long latency_start = context.getRoutingStartTime();
        if (latency_start <= 0) latency_start = System.currentTimeMillis();
    long latency_end = context.getRoutingEndTime();
        if (latency_end <= 0) latency_end = latency_start;

    int routingLatency = (int) (latency_end - latency_start);
    long assertion_end_time = System.currentTimeMillis();
    int assertion_total_time = (int) (assertion_end_time - assertion_start_time);
    //create background service to insert the API Analytics record
    try {
            final String[] dataValuesVariable = Syntax.getReferencedNames(assertion.getConnectionName(),
                    AnalyticsAssertion.SSG_NODE_ID, AnalyticsAssertion.REQUEST_ID,
                    AnalyticsAssertion.RESPONSE_CODE,
                    AnalyticsAssertion.REQUEST_REMOTE_IP, AnalyticsAssertion.REQUEST_METHOD,
                    AnalyticsAssertion.ROUTING_LATENCY, AnalyticsAssertion.ROUTING_REASON_CODE,
                    AnalyticsAssertion.REQUEST_URI, AnalyticsAssertion.GATEWAY_TIME_MILLIS,
                    AnalyticsAssertion.PORTALMAN_API_ID, AnalyticsAssertion.PORTALMAN_API_KEY,
                    AnalyticsAssertion.PORTALMAN_ACCOUNT_PLAN_MAPPING_ID, AnalyticsAssertion.ANALYTICS_AUTH_TYPE,
                    AnalyticsAssertion.ANALYTICS_RESPONSE_CODE,
                    AnalyticsAssertion.ANALYTICS_APP_UUID, AnalyticsAssertion.ANALYTICS_APP_NAME,
                    AnalyticsAssertion.ANALYTICS_API_UUID, AnalyticsAssertion.ANALYTICS_API_NAME,
                    AnalyticsAssertion.ANALYTICS_ORG_UUID, AnalyticsAssertion.ANALYTICS_ORG_NAME,
                    AnalyticsAssertion.ANALYTICS_ACCOUNT_PLAN_UUID, AnalyticsAssertion.ANALYTICS_ACCOUNT_PLAN_NAME,
                    AnalyticsAssertion.ANALYTICS_API_PLAN_UUID, AnalyticsAssertion.ANALYTICS_API_PLAN_NAME,
                    AnalyticsAssertion.ANALYTICS_CUSTOM_TAG1, AnalyticsAssertion.ANALYTICS_CUSTOM_TAG2,
                    AnalyticsAssertion.ANALYTICS_CUSTOM_TAG3, AnalyticsAssertion.ANALYTICS_CUSTOM_TAG4,
                    AnalyticsAssertion.ANALYTICS_CUSTOM_TAG5);
      final Map<String, Object> dataValuesMap = context.getVariableMap(dataValuesVariable, getAudit());
      final String ssgNodeId = ExpandVariables.process(AnalyticsAssertion.SSG_NODE_ID, dataValuesMap, getAudit());
      final String requestId = ExpandVariables.process(AnalyticsAssertion.REQUEST_ID, dataValuesMap, getAudit());
      final String httpMethod = ExpandVariables.process(AnalyticsAssertion.REQUEST_METHOD, dataValuesMap, getAudit());
      final String requestIp = ExpandVariables.process(AnalyticsAssertion.REQUEST_REMOTE_IP, dataValuesMap, getAudit());
      final String requestUri = ExpandVariables.process(AnalyticsAssertion.REQUEST_URI, dataValuesMap, getAudit());
      //final String gatewayTimeEnd = ExpandVariables.process("${gateway.time.millis}", dataValuesMap, getAudit());
      final String authType = ExpandVariables.process(AnalyticsAssertion.ANALYTICS_AUTH_TYPE, dataValuesMap, getAudit());
      String accountPlanMappingId = ExpandVariables.process(AnalyticsAssertion.PORTALMAN_ACCOUNT_PLAN_MAPPING_ID, dataValuesMap, getAudit());
      String serviceApiId = ExpandVariables.process(AnalyticsAssertion.PORTALMAN_API_ID, dataValuesMap, getAudit());
      String apiKey = ExpandVariables.process(AnalyticsAssertion.PORTALMAN_API_KEY, dataValuesMap, getAudit());
      final String analyticsResponseCode = ExpandVariables.process(AnalyticsAssertion.ANALYTICS_RESPONSE_CODE, dataValuesMap, getAudit());
      final String apiIdUuid = ExpandVariables.process(AnalyticsAssertion.ANALYTICS_API_UUID, dataValuesMap, getAudit());
      final String applicationUuid = ExpandVariables.process(AnalyticsAssertion.ANALYTICS_APP_UUID, dataValuesMap, getAudit());
      final String organizationUuid = ExpandVariables.process(AnalyticsAssertion.ANALYTICS_ORG_UUID, dataValuesMap, getAudit());
      final String accountPlanUuid = ExpandVariables.process(AnalyticsAssertion.ANALYTICS_ACCOUNT_PLAN_UUID, dataValuesMap, getAudit());
      final String apiPlanUuid = ExpandVariables.process(AnalyticsAssertion.ANALYTICS_API_PLAN_UUID, dataValuesMap, getAudit());
      final String customTag1 = ExpandVariables.process(AnalyticsAssertion.ANALYTICS_CUSTOM_TAG1, dataValuesMap, getAudit());
      final String customTag2 = ExpandVariables.process(AnalyticsAssertion.ANALYTICS_CUSTOM_TAG2, dataValuesMap, getAudit());
      final String customTag3 = ExpandVariables.process(AnalyticsAssertion.ANALYTICS_CUSTOM_TAG3, dataValuesMap, getAudit());
      final String customTag4 = ExpandVariables.process(AnalyticsAssertion.ANALYTICS_CUSTOM_TAG4, dataValuesMap, getAudit());
      final String customTag5 = ExpandVariables.process(AnalyticsAssertion.ANALYTICS_CUSTOM_TAG5, dataValuesMap, getAudit());
      String responseHttpStatus = ExpandVariables.process(AnalyticsAssertion.RESPONSE_CODE, dataValuesMap, getAudit());
      String httpRoutingReasonCodeStr = ExpandVariables.process(AnalyticsAssertion.ROUTING_REASON_CODE, dataValuesMap, getAudit());
      int httpStatuCode = 500;
      if (!isEmpty(analyticsResponseCode)) {
        try {
          httpStatuCode = Integer.parseInt(analyticsResponseCode);
        } catch (Exception e) {
          httpStatuCode = 500;
        }
      } else if (isEmpty(responseHttpStatus)) { //if there is no response status, try grabbing the routing status code
        if (isEmpty(httpRoutingReasonCodeStr)) {
          //by default it's 200, TODO: if we can tap in to message processing to get the real service output
          httpRoutingReasonCodeStr = "500";
        }
        try {
          httpStatuCode = Integer.parseInt(httpRoutingReasonCodeStr);
        } catch (Exception e) {
          httpStatuCode = 500;
        }
      } else {
        try {
          httpStatuCode = Integer.parseInt(responseHttpStatus);
        } catch (Exception e) {
          httpStatuCode = 500;
        }
      }
      if (isEmpty(serviceApiId) || !isEmpty(apiIdUuid)) {
        serviceApiId = apiIdUuid;
      }
      if (isEmpty(apiKey) || !isEmpty(applicationUuid)) {
        apiKey = applicationUuid;
      }
      if (isEmpty(accountPlanMappingId) || !isEmpty(organizationUuid)) {
        accountPlanMappingId = organizationUuid;
      }
            /*
            INSERT INTO API_METRICS (1=UUID, 2=SSG_NODE_ID, 3=SSG_REQUEST_ID, 4=RESOLUTION, " +
            "5=RESOLUTION_TIME_INTERVAL, 6=ROLLUP_START_TIME, 7=SSG_REQUEST_START_TIME, 8=SSG_REQUEST_END_TIME, " +
            "9=SSG_SERVICE_ID, 10=SSG_PORTAL_API_ID, " +
            "11=HTTP_METHOD, 12=HTTP_PUT_COUNT, 13=HTTP_POST_COUNT, 14=HTTP_DELETE_COUNT, 15=HTTP_GET_COUNT, 16=HTTP_OTHER_COUNT, " +
            "17=SERVICE_URI, 18=AUTH_TYPE, 19=HTTP_RESPONSE_STATUS, " +
            "20=SUCCESS_COUNT, 21=ERROR_COUNT, 22=PROXY_LATENCY, " +
            "23=BACKEND_LATENCY, 24=TOTAL_LATENCY, " +
            "25=APPLICATION_UUID, 26=ORGANIZATION_UUID, 27=ACCOUNT_PLAN_UUID, 28=API_PLAN_UUID, " +
            "29=CUSTOM_TAG1, 30=CUSTOM_TAG2, 31=CUSTOM_TAG3, 32=CUSTOM_TAG4, 33=CUSTOM_TAG5) VALUES" +
             */
      HitsMetric metric = new HitsMetric();
      metric.setConnectionName(connName);
      //pstmt.setString(1, uuid);//UUID
      //metric.setUuid(uuid);
      //pstmt.setString(2, ssgNodeId);//SSG_NODE_ID
      metric.setSsgNodeId(ssgNodeId);
      //pstmt.setString(3, requestId);//SSG_REQUEST_ID
      metric.setSsgRequestId(requestId);
      //pstmt.setInt(4, 0);//RESOLUTION
      //pstmt.setInt(5, 0);//RESOLUTION_TIME_INTERVAL
      //pstmt.setInt(6, 0);//ROLLUP_START_TIME
      //pstmt.setString(7, gatewayTimeStart);//SSG_REQUEST_START_TIME
      metric.setSsgRequestStartTime(gatewayTimeStart);
      //pstmt.setLong(8, assertion_end_time);//SSG_REQUEST_END_TIME
      metric.setSsgRequestEndTime(assertion_end_time);
      //pstmt.setString(9, "");//SSG_SERVICE_ID
      //pstmt.setString(10, serviceApiId);//SSG_PORTAL_API_ID
      metric.setSsgPortalApiId(serviceApiId);
      //pstmt.setString(11, requestIp);//REQUEST_IP
      metric.setRequestIp(requestIp);
      //pstmt.setString(12, httpMethod);//HTTP_METHOD
      metric.setHttpMethod(httpMethod);
      int HTTP_PUT = 0, HTTP_POST = 0, HTTP_DELETE = 0, HTTP_GET = 0, HTTP_OTHER = 0;
      switch (httpMethod) {
        case "PUT":
          HTTP_PUT++;
          break;
        case "POST":
          HTTP_POST++;
          break;
        case "DELETE":
          HTTP_DELETE++;
          break;
        case "GET":
          HTTP_GET++;
          break;
        default:
          HTTP_OTHER++;
          break;
      }
      int ERROR_COUNT, SUCCESS_COUNT;
      if (httpStatuCode >= 200 && httpStatuCode <= 299) {
        ERROR_COUNT = 0;
        SUCCESS_COUNT = 1;
      } else {
        ERROR_COUNT = 1;
        SUCCESS_COUNT = 0;
      }
      //pstmt.setInt(13, HTTP_PUT);//HTTP_PUT_COUNT
      metric.setHttpPutCount(HTTP_PUT);
      //pstmt.setInt(14, HTTP_POST);//HTTP_POST_COUNT
      metric.setHttpPostCount(HTTP_POST);
      //pstmt.setInt(15, HTTP_DELETE);//HTTP_DELETE_COUNT
      metric.setHttpDeleteCount(HTTP_DELETE);
      //pstmt.setInt(16, HTTP_GET);//HTTP_GET_COUNT
      metric.setHttpGetCount(HTTP_GET);
      //pstmt.setInt(17, HTTP_OTHER);//HTTP_OTHER_COUNT
      metric.setHttpOtherCount(HTTP_OTHER);
      //pstmt.setString(18, requestUri);//SERVICE_URI
      metric.setServiceUri(requestUri);
      //pstmt.setString(19, authType);//AUTH_TYPE
      metric.setAuthType(authType);
      //pstmt.setInt(20, httpStatuCode);//HTTP_RESPONSE_STATUS
      metric.setHttpResponseStatus(httpStatuCode);
      //pstmt.setInt(21, SUCCESS_COUNT);//SUCCESS_COUNT
      metric.setSuccessCount(SUCCESS_COUNT);
      //pstmt.setInt(22, ERROR_COUNT);//ERROR_COUNT
      metric.setErrorCount(ERROR_COUNT);
      //pstmt.setInt(23, routingLatency);//PROXY_LATENCY
      metric.setProxyLatency(routingLatency);
      //pstmt.setInt(24, assertion_total_time - routingLatency);//BACKEND_LATENCY
      metric.setBackendLatency(assertion_total_time - routingLatency);
      //pstmt.setInt(25, assertion_total_time);//TOTAL_LATENCY
      metric.setTotalLatency(assertion_total_time);
      //pstmt.setString(26, apiKey);//APPLICATION_UUID
      metric.setApplicationUuid(apiKey);
      //pstmt.setString(27, accountPlanMappingId);//ORGANIZATION_UUID
      metric.setOrganizationUuid(accountPlanMappingId);
      //pstmt.setString(28, accountPlanUuid);//ACCOUNT_PLAN_UUID
      metric.setAccountPlanUuid(accountPlanUuid);
      //pstmt.setString(29, apiPlanUuid);//API_PLAN_UUID
      metric.setApiPlanUuid(apiPlanUuid);
      //pstmt.setString(30, customTag1);//CUSTOM_TAG1
      metric.setCustomTag1(customTag1);
      //pstmt.setString(31, customTag2);//CUSTOM_TAG2
      metric.setCustomTag2(customTag2);
      //pstmt.setString(32, customTag3);//CUSTOM_TAG3
      metric.setCustomTag3(customTag3);
      //pstmt.setString(33, customTag4);//CUSTOM_TAG4
      metric.setCustomTag4(customTag4);
      //pstmt.setString(34, customTag5);//CUSTOM_TAG5
      metric.setCustomTag5(customTag5);
      metricsQueueProcessor.add(metric);
    } catch (Exception e) {
      logger.warning(ExceptionUtils.getMessageWithCause(e));
      String errorMsg = ExceptionUtils.getMessage(ExceptionUtils.unnestToRoot(e));
      logAndAudit(AssertionMessages.EXCEPTION_INFO, errorMsg);
    }
    return status;
  }


  /**
   * checks if a string is empty
   */
  private boolean isEmpty(final String var) {
    if (var == null || "".equals(var.trim())) {
      return true;
    }
    return false;
  }
}
