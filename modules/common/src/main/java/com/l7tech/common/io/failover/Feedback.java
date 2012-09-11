package com.l7tech.common.io.failover;

/**
 * The feedback data for success/failure for each failover strategy attempt
 */
public class Feedback {

    private long latency;
    private int reason;
    private String route;
    private int status;

    public Feedback(long latency, int reason, String route, int status) {
        this.latency = latency;
        this.reason = reason;
        this.route = route;
        this.status = status;
    }

    public long getLatency() {
        return latency;
    }

    public void setLatency(long latency) {
        this.latency = latency;
    }

    public int getReason() {
        return reason;
    }

    public void setReason(int reason) {
        this.reason = reason;
    }

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

}
