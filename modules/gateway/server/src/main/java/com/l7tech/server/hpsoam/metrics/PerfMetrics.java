package com.l7tech.server.hpsoam.metrics;

/**
 * Corresponds to the http://openview.hp.com/xmlns/soa/1/perf:PerformanceMetrics element
 * in the OperationPerformance element
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Sep 19, 2007<br/>
 */
public class PerfMetrics {
    private long successMessageCount;
    private long successCombinedResponseTime;
    private long successMinimumResponseTime;
    private long successMaximumResponseTime;
    private long failureMessageCount;
    private long failureCombinedResponseTime;
    private long failureMinimumResponseTime;
    private long failureMaximumResponseTime;
    private long securityViolationMessageCount;


    public long getSuccessMessageCount() {
        return successMessageCount;
    }

    public long getSuccessAverageResponseTime() {
        if (successMessageCount == 0) return 0;
        return successCombinedResponseTime / successMessageCount;
    }

    public long getSuccessMinimumResponseTime() {
        return successMinimumResponseTime;
    }

    public long getSuccessMaximumResponseTime() {
        return successMaximumResponseTime;
    }

    public long getFailureMessageCount() {
        return failureMessageCount;
    }

    public long getFailureAverageResponseTime() {
        if (failureMessageCount == 0) return 0;
        return failureCombinedResponseTime /failureMessageCount;
    }

    public long getFailureMinimumResponseTime() {
        return failureMinimumResponseTime;
    }

    public long getFailureMaximumResponseTime() {
        return failureMaximumResponseTime;
    }

    public long getSecurityViolationMessageCount() {
        return securityViolationMessageCount;
    }

    public void addSuccessCombinedResponseTime(long successResponseTime) {
        this.successCombinedResponseTime += successResponseTime;
        ++successMessageCount;
        if (successMinimumResponseTime == 0 || successResponseTime < successMinimumResponseTime) {
            successMinimumResponseTime = successResponseTime;
        }
        if (successResponseTime > successMaximumResponseTime) {
            successMaximumResponseTime = successResponseTime;
        }
    }

    public void addFailureCombinedResponseTime(long failureResponseTime) {
        ++failureMessageCount;
        failureCombinedResponseTime += failureResponseTime;
        if (failureMinimumResponseTime == 0 || failureResponseTime < failureMinimumResponseTime) {
            failureMinimumResponseTime = failureResponseTime;
        }
        if (failureResponseTime > failureMaximumResponseTime) {
            failureMaximumResponseTime = failureResponseTime;
        }
    }

    public void addSecurityViolationMessageCount() {
        ++securityViolationMessageCount;
    }
}
