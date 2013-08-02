package com.l7tech.server.wsdm;

import com.l7tech.objectmodel.Goid;
import com.l7tech.server.wsdm.util.ISO8601Duration;
import com.l7tech.gateway.common.service.MetricsSummaryBin;

import java.net.URL;

/**
 * Holds lazily populated data for the handling of metrics related requests.
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Nov 26, 2007<br/>
 */
public class MetricsRequestContext {
    private final long avgResponseTime;
    private final long lastResponseTime;
    private final long minResponseTime;
    private final long maxResponseTime;
    private final long nrFailedRequests;
    private final long nrSuccessRequests;
    private final long nrRequests;
    private final boolean operational;
    private final Goid serviceId;
    private final long serviceTime;
    private final long throughput;
    private final long periodStart;
    private final long lastUpdated;
    private final float availability;
    private final String duration;
    private final URL incomingURL;
    private final String throuputPeriod;

    public MetricsRequestContext(MetricsSummaryBin bin, boolean operational, URL url, long uptime) {
        this.operational = operational;
        this.serviceId = bin.getServiceGoid();
        this.incomingURL = url;

        avgResponseTime = (long)bin.getAverageFrontendResponseTime();

        duration = ISO8601Duration.durationFromSecs(uptime / 1000);

        lastResponseTime = (long)bin.getLastAverageFrontendResponseTime();
        lastUpdated = bin.getLastAttemptedRequest();
        minResponseTime = bin.getMinFrontendResponseTime()==null?0:bin.getMinFrontendResponseTime();
        maxResponseTime = bin.getMaxFrontendResponseTime()==null?0:bin.getMaxFrontendResponseTime();
        nrFailedRequests = bin.getNumPolicyViolation() + bin.getNumRoutingFailure();
        nrRequests = bin.getNumAttemptedRequest();
        nrSuccessRequests = bin.getNumCompletedRequest();
        periodStart = bin.getPeriodStart();
        availability = bin.getNumAttemptedRequest()==0 ?
                100f :
                (1f - (((float)bin.getNumRoutingFailure())/((float)bin.getNumAttemptedRequest()))) *100f;

        // From MOWS 1.1 Specification , Section 5.2.3.2:
        // "ServiceTime is a counter of the total elapsed time that the Web service
        // endpoint has taken to process all requests (successfully or not)."
        serviceTime = bin.getSumFrontendResponseTime();

        double thptval = 0.0;
        double durationinsecs = (double)uptime / 1000.0;
        if (durationinsecs > 0.0) {
            thptval = (double)(bin.getNumAttemptedRequest()) / durationinsecs;
        }

        String tperiod;
        double tval;
        tperiod = "PT1S";
        tval = thptval;
        if (thptval <= 1.0 && durationinsecs > 60) {
            thptval = (double)bin.getNumAttemptedRequest() / (durationinsecs/60);
            tperiod = "PT1M";
            tval = thptval;
            if (thptval <= 1 && durationinsecs > 3600) {
                thptval = (double)bin.getNumAttemptedRequest() / (durationinsecs/3600);
                tperiod = "PT1H";
                tval= thptval;
            }
        }

        this.throuputPeriod = tperiod;
        this.throughput = (long)tval;
    }

    public long getAvgResponseTime() {
        return avgResponseTime;
    }

    public long getLastResponseTime() {
        return lastResponseTime;
    }

    public long getMinResponseTime() {
        return minResponseTime;
    }

    public long getMaxResponseTime() {
        return maxResponseTime;
    }

    public long getNrFailedRequests() {
        return nrFailedRequests;
    }

    public long getNrSuccessRequests() {
        return nrSuccessRequests;
    }

    public long getNrRequests() {
        return nrRequests;
    }

    public boolean isOperational() {
        return operational;
    }

    public Goid getServiceId() {
        return serviceId;
    }

    public long getServiceTime() {
        return serviceTime;
    }

    public long getThroughput() {
        return throughput;
    }

    public long getPeriodStart() {
        return periodStart;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public String getDuration() {
        // "PT5M" ?
        return duration;
    }

    public URL getIncomingURL() {
        return incomingURL;
    }

    public String getThrouputPeriod() {
        return throuputPeriod;
    }

    public float getAvailability() {
        return availability;
    }
}
