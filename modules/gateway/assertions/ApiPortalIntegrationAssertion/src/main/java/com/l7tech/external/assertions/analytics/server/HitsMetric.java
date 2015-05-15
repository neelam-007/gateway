package com.l7tech.external.assertions.analytics.server;

/**
 * Represents One hits metrics
 *
 * @author rraquepo, 8/1/14
 */
public class HitsMetric {
    private String connectionName;//the connection name at the time of capture
    private String uuid;
    private String ssgNodeId;
    private String ssgRequestId;
    private String resolution;
    private String resolutionTimeInterval;
    private String rollupStartTime;
    private String ssgRequestStartTime;
    private Long ssgRequestEndTime;
    private String ssgServiceId;
    private String ssgPortalApiId;
    private String apiName;//Portal API Name
    private String requestIp;
    private String httpMethod;
    private Integer httpPutCount;
    private Integer httpPostCount;
    private Integer httpDeleteCount;
    private Integer httpGetCount;
    private Integer httpOtherCount;
    private String serviceUri;
    private String authType;
    private Integer httpResponseStatus;
    private Integer successCount;
    private Integer errorCount;
    private Integer proxyLatency;
    private Integer backendLatency;
    private Integer totalLatency;
    private String applicationUuid;
    private String applicationName;//Portal Application Name
    private String organizationUuid;
    private String organizationName;//Portal Organization Name
    private String accountPlanUuid;
    private String accountPlanName;//Portal Account Plan Name
    private String apiPlanUuid;
    private String apiPlanName;//Portal Api Plan Name
    private String customTag1;
    private String customTag2;
    private String customTag3;
    private String customTag4;
    private String customTag5;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getSsgNodeId() {
        return ssgNodeId;
    }

    public void setSsgNodeId(String ssgNodeId) {
        this.ssgNodeId = ssgNodeId;
    }

    public String getSsgRequestId() {
        return ssgRequestId;
    }

    public void setSsgRequestId(String ssgRequestId) {
        this.ssgRequestId = ssgRequestId;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public String getResolutionTimeInterval() {
        return resolutionTimeInterval;
    }

    public void setResolutionTimeInterval(String resolutionTimeInterval) {
        this.resolutionTimeInterval = resolutionTimeInterval;
    }

    public String getRollupStartTime() {
        return rollupStartTime;
    }

    public void setRollupStartTime(String rollupStartTime) {
        this.rollupStartTime = rollupStartTime;
    }

    public String getSsgRequestStartTime() {
        return ssgRequestStartTime;
    }

    public void setSsgRequestStartTime(String ssgRequestStartTime) {
        this.ssgRequestStartTime = ssgRequestStartTime;
    }

    public Long getSsgRequestEndTime() {
        return ssgRequestEndTime;
    }

    public void setSsgRequestEndTime(Long ssgRequestEndTime) {
        this.ssgRequestEndTime = ssgRequestEndTime;
    }

    public String getSsgServiceId() {
        return ssgServiceId;
    }

    public void setSsgServiceId(String ssgServiceId) {
        this.ssgServiceId = ssgServiceId;
    }

    public String getSsgPortalApiId() {
        return ssgPortalApiId;
    }

    public void setSsgPortalApiId(String ssgPortalApiId) {
        this.ssgPortalApiId = ssgPortalApiId;
    }

    public String getApiName() {
        return apiName;
    }

    public void setApiName(String apiName) {
        this.apiName = apiName;
    }

    public String getRequestIp() {
        return requestIp;
    }

    public void setRequestIp(String requestIp) {
        this.requestIp = requestIp;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public Integer getHttpPutCount() {
        return httpPutCount;
    }

    public void setHttpPutCount(Integer httpPutCount) {
        this.httpPutCount = httpPutCount;
    }

    public Integer getHttpPostCount() {
        return httpPostCount;
    }

    public void setHttpPostCount(Integer httpPostCount) {
        this.httpPostCount = httpPostCount;
    }

    public Integer getHttpDeleteCount() {
        return httpDeleteCount;
    }

    public void setHttpDeleteCount(Integer httpDeleteCount) {
        this.httpDeleteCount = httpDeleteCount;
    }

    public Integer getHttpGetCount() {
        return httpGetCount;
    }

    public void setHttpGetCount(Integer httpGetCount) {
        this.httpGetCount = httpGetCount;
    }

    public Integer getHttpOtherCount() {
        return httpOtherCount;
    }

    public void setHttpOtherCount(Integer httpOtherCount) {
        this.httpOtherCount = httpOtherCount;
    }

    public String getServiceUri() {
        return serviceUri;
    }

    public void setServiceUri(String serviceUri) {
        this.serviceUri = serviceUri;
    }

    public String getAuthType() {
        return authType;
    }

    public void setAuthType(String authType) {
        this.authType = authType;
    }

    public Integer getHttpResponseStatus() {
        return httpResponseStatus;
    }

    public void setHttpResponseStatus(Integer httpResponseStatus) {
        this.httpResponseStatus = httpResponseStatus;
    }

    public Integer getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(Integer successCount) {
        this.successCount = successCount;
    }

    public Integer getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(Integer errorCount) {
        this.errorCount = errorCount;
    }

    public Integer getProxyLatency() {
        return proxyLatency;
    }

    public void setProxyLatency(Integer proxyLatency) {
        this.proxyLatency = proxyLatency;
    }

    public Integer getBackendLatency() {
        return backendLatency;
    }

    public void setBackendLatency(Integer backendLatency) {
        this.backendLatency = backendLatency;
    }

    public Integer getTotalLatency() {
        return totalLatency;
    }

    public void setTotalLatency(Integer totalLatency) {
        this.totalLatency = totalLatency;
    }

    public String getApplicationUuid() {
        return applicationUuid;
    }

    public void setApplicationUuid(String applicationUuid) {
        this.applicationUuid = applicationUuid;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getOrganizationUuid() {
        return organizationUuid;
    }

    public void setOrganizationUuid(String organizationUuid) {
        this.organizationUuid = organizationUuid;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public String getAccountPlanUuid() {
        return accountPlanUuid;
    }

    public void setAccountPlanUuid(String accountPlanUuid) {
        this.accountPlanUuid = accountPlanUuid;
    }

    public String getAccountPlanName() {
        return accountPlanName;
    }

    public void setAccountPlanName(String accountPlanName) {
        this.accountPlanName = accountPlanName;
    }

    public String getApiPlanUuid() {
        return apiPlanUuid;
    }

    public void setApiPlanUuid(String apiPlanUuid) {
        this.apiPlanUuid = apiPlanUuid;
    }

    public String getApiPlanName() {
        return apiPlanName;
    }

    public void setApiPlanName(String apiPlanName) {
        this.apiPlanName = apiPlanName;
    }

    public String getCustomTag1() {
        return customTag1;
    }

    public void setCustomTag1(String customTag1) {
        this.customTag1 = customTag1;
    }

    public String getCustomTag2() {
        return customTag2;
    }

    public void setCustomTag2(String customTag2) {
        this.customTag2 = customTag2;
    }

    public String getCustomTag3() {
        return customTag3;
    }

    public void setCustomTag3(String customTag3) {
        this.customTag3 = customTag3;
    }

    public String getCustomTag4() {
        return customTag4;
    }

    public void setCustomTag4(String customTag4) {
        this.customTag4 = customTag4;
    }

    public String getCustomTag5() {
        return customTag5;
    }

    public void setCustomTag5(String customTag5) {
        this.customTag5 = customTag5;
    }

    public String getConnectionName() {
        return connectionName;
    }

    public void setConnectionName(String connectionName) {
        this.connectionName = connectionName;
    }
}
