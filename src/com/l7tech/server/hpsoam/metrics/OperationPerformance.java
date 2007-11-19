package com.l7tech.server.hpsoam.metrics;

/**
 * Maps http://openview.hp.com/xmlns/soa/1/perf:OperationPerformance
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Sep 19, 2007<br/>
 */
public class  OperationPerformance {
    private String localName;
    private String ns;
    private PerfMetrics metrics = new PerfMetrics();

    public OperationPerformance(String localName, String ns) {
        this.localName = localName;
        this.ns = ns;
    }

    public String getLocalName() {
        return localName;
    }

    public void setLocalName(String localName) {
        this.localName = localName;
    }

    public String getNs() {
        return ns;
    }

    public void setNs(String ns) {
        this.ns = ns;
    }

    public PerfMetrics getMetrics() {
        return metrics;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append(" name " + localName);
        buf.append(" ns " + ns);
        buf.append(" " + metrics.getFailureAverageResponseTime() + ", " + metrics.getFailureMaximumResponseTime() +
                   ", " + metrics.getFailureMessageCount() + ", " + metrics.getFailureMinimumResponseTime() + ", "
                   + metrics.getSecurityViolationMessageCount() + ", " + metrics.getSuccessAverageResponseTime() +
                   ", " + metrics.getSuccessMaximumResponseTime() + ", " + metrics.getSuccessMessageCount() + ", "
                   + metrics.getSuccessMinimumResponseTime());
        return buf.toString();
    }
}
