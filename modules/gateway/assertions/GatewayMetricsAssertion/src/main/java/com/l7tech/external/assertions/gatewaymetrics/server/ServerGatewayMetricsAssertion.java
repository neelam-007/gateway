package com.l7tech.external.assertions.gatewaymetrics.server;

import com.l7tech.external.assertions.gatewaymetrics.GatewayMetricsAssertion;
import com.l7tech.external.assertions.gatewaymetrics.IntervalTimeUnit;
import com.l7tech.gateway.common.cluster.ClusterNodeInfo;
import com.l7tech.gateway.common.cluster.ServiceUsage;
import com.l7tech.gateway.common.service.MetricsBin;
import com.l7tech.gateway.common.service.MetricsSummaryBin;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.cluster.ClusterInfoManager;
import com.l7tech.server.cluster.ServiceUsageManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.server.service.ServiceMetricsManager;
import com.l7tech.server.service.ServiceMetricsServices;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server side implementation of the GatewayMetricsAssertion.
 *
 * @see com.l7tech.external.assertions.gatewaymetrics.GatewayMetricsAssertion
 */
public class ServerGatewayMetricsAssertion extends AbstractServerAssertion<GatewayMetricsAssertion> {
    private static final Logger logger = Logger.getLogger(ServerGatewayMetricsAssertion.class.getName());

    // Metric Summary Bin Comparator. Used to sort MetricsSummaryBin by descending period start time.
    //
    private static Comparator metricsSummaryBinComparator = new Comparator<MetricsSummaryBin> () {

        @Override
        public int compare(MetricsSummaryBin o1, MetricsSummaryBin o2) {
            long o1PeriodStart = o1.getPeriodStart();
            long o2PeriodStart = o2.getPeriodStart();

            if (o1PeriodStart == o2PeriodStart) {
                return 0;
            } else if (o1PeriodStart < o2PeriodStart) {
                return 1;
            } else {
                return -1;
            }
        }
    };

    private final String[] variablesUsed;
    private ApplicationContext springContext;
    private ServiceMetricsManager metricsManager;
    private ServiceMetricsServices metricsServices;
    private ClusterInfoManager clusterInfoManager;
    private ServiceUsageManager statsManager;
    private ServiceManager serviceManager;

    public ServerGatewayMetricsAssertion(final GatewayMetricsAssertion assertion, ApplicationContext context) throws PolicyAssertionException {
        super(assertion);
        this.springContext = context;
        this.variablesUsed = assertion.getVariablesUsed();
    }

    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        String clusterNodeId = null;
        Goid serviceGoid = PublishedService.DEFAULT_GOID;
        int resolution = -1;

        Map<String, Object> vars = context.getVariableMap(variablesUsed, getAudit());

        clusterInfoManager = (ClusterInfoManager) springContext.getBean("clusterInfoManager");
        metricsManager = (ServiceMetricsManager) springContext.getBean("serviceMetricsManager");
        metricsServices = (ServiceMetricsServices) springContext.getBean("serviceMetricsServices");
        statsManager = (ServiceUsageManager) springContext.getBean("serviceUsageManager");
        serviceManager = (ServiceManager)springContext.getBean("serviceManager");

        boolean useVariables = assertion.getUseVariables();

        if (!useVariables) {
            clusterNodeId = assertion.getClusterNodeId();
            serviceGoid = assertion.getPublishedServiceGoid();
            resolution = assertion.getResolution();
        } else {
            // Resolve variables.
            //

            // Cluster Node ID.
            //
            String clusterNodeName = ExpandVariables.process(assertion.getClusterNodeVariable(), vars, getAudit(), true);
            if (clusterNodeName.equalsIgnoreCase("ALL_NODES")) {
                // All clusters.
                //
                clusterNodeId = null;
            } else {
                try {
                    Collection<ClusterNodeInfo> nodes = clusterInfoManager.retrieveClusterStatus();
                    boolean found = false;
                    for (ClusterNodeInfo node : nodes) {
                        if (node.getName().equals(clusterNodeName)) {
                            found = true;
                            clusterNodeId = node.getId();
                            break;
                        }
                    }
                    if (!found) {
                        logger.log(Level.WARNING, "Cluster node with given name cannot be found: " + clusterNodeName);
                        return AssertionStatus.FAILED;
                    }
                } catch (FindException e) {
                    logger.log(Level.WARNING, "Cluster nodes cannot be retrieved.");
                    return AssertionStatus.FAILED;
                }
            }

            // Service GOID.
            //
            String serviceName = ExpandVariables.process(assertion.getPublishedServiceVariable(), vars, getAudit(), true);
            if (serviceName.equalsIgnoreCase("ALL_SERVICES")) {
                // All services.
                //
                serviceGoid = PublishedService.DEFAULT_GOID;
            } else {
                try {
                    PublishedService service = serviceManager.findByUniqueName(serviceName);
                    if (service == null) {
                        logger.log(Level.WARNING, "Published service with given name cannot be found: " + serviceName);
                        return AssertionStatus.FAILED;
                    }
                    serviceGoid = service.getGoid();
                } catch (FindException e1) {
                    logger.log(Level.WARNING, "Published service with given name cannot be found: " + serviceName);
                    return AssertionStatus.FAILED;
                }
            }

            // Resolution.
            // Look for one of three values (case insensitive): DAILY, HOURLY, FINE
            //
            String resolutionName = ExpandVariables.process(assertion.getResolutionVariable(), vars, getAudit(), true);
            if (resolutionName.equalsIgnoreCase("daily")) {
                resolution = MetricsBin.RES_DAILY;
            } else if (resolutionName.equalsIgnoreCase("hourly")) {
                resolution = MetricsBin.RES_HOURLY;
            } else if (resolutionName.equalsIgnoreCase("fine")) {
                resolution = MetricsBin.RES_FINE;
            } else {
                logger.log(Level.WARNING, "An invalid resolution was found: " + resolutionName + ". It must be one of: DAILY, HOURLY, or FINE.");
                return AssertionStatus.FAILED;
            }
        }

        //check to see if cluster node still exists
        if (clusterNodeId != null) {
            try {
                Collection<ClusterNodeInfo> nodes = clusterInfoManager.retrieveClusterStatus();
                boolean found = false;
                for ( ClusterNodeInfo node : nodes) {
                    if ( clusterNodeId.equals(node.getId())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    logger.log(Level.WARNING, "Cluster node with given ID cannot be found: " + clusterNodeId);
                    return AssertionStatus.FAILED;
                }
            } catch (FindException e) {
                logger.log(Level.WARNING, "Cluster nodes cannot be retrieved.");
                return AssertionStatus.FAILED;
            }
        }

        //check to see if service still exists
        if (!serviceGoid.equals(PublishedService.DEFAULT_GOID)) {
            try {
                if (serviceManager.findByPrimaryKey(serviceGoid) == null ) {
                    logger.log(Level.WARNING, "Published service with given GOID cannot be found: " + serviceGoid);
                    return AssertionStatus.FAILED;
                }
            } catch (FindException e) {
                logger.log(Level.WARNING, "Published service with given GOID cannot be found: " + serviceGoid);
                return AssertionStatus.FAILED;
            }
        }

        try {
            ClusterNodeInfo[] clusterNodeInfos = this.getClusterData();
            ServiceUsage[] serviceUsages = this.getServiceStatistics(serviceGoid);
            List<MetricsSummaryBin> metricsSummaryBins = this.getMetricsSummaryBins(clusterNodeId, serviceGoid, resolution, vars);

            GatewayMetricsMessage metricsMessage = new GatewayMetricsMessage(
                clusterNodeId,
                serviceGoid,
                resolution,
                assertion.getIntervalType(),
                clusterNodeInfos,
                serviceUsages,
                metricsSummaryBins,
                serviceManager);

            this.setContextVariables(context, metricsMessage);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unable to retrieve gateway metrics data.", e);
            return AssertionStatus.FAILED;
        }

        return AssertionStatus.NONE;
    }

    private ClusterNodeInfo[] getClusterData() throws FindException {
        Collection<ClusterNodeInfo> c = clusterInfoManager.retrieveClusterStatus();

        return c.toArray(new ClusterNodeInfo[c.size()] );
    }

    private ServiceUsage[] getServiceStatistics(Goid serviceGoid) throws FindException {
        ServiceUsage[] serviceUsages;

        if (!serviceGoid.equals(PublishedService.DEFAULT_GOID)) {
            serviceUsages = statsManager.findByServiceGoid(serviceGoid);
        } else {
            serviceUsages = statsManager.getAll().toArray(new ServiceUsage[statsManager.getAll().size()]);
        }

        return serviceUsages;
    }

    private List<MetricsSummaryBin> getMetricsSummaryBins (
        String clusterNodeId,
        Goid serviceGoid,
        int resolution,
        Map<String, Object> vars) throws Exception {

        List<MetricsSummaryBin> metricsSummaryBins = null;

        Goid[] serviceGoids = null;
        if (!serviceGoid.equals(PublishedService.DEFAULT_GOID)) {
            serviceGoids = new Goid[]{serviceGoid};
        }

        switch (assertion.getIntervalType()) {
            case MOST_RECENT: {
                MetricsSummaryBin latestBin = getLatestBin(clusterNodeId, serviceGoids, resolution);
                metricsSummaryBins = new ArrayList<MetricsSummaryBin>(1);
                metricsSummaryBins.add(latestBin);
            }
            break;

            case RECENT_NUMBER_OF_INTERVALS: {
                // Get number of intervals.
                //
                String numberOfIntervalsStrVal = ExpandVariables.process(assertion.getNumberOfRecentIntervals(), vars, getAudit(), true);
                long numberOfIntervals = Long.parseLong(numberOfIntervalsStrVal); // NumberFormatException is caught by caller of this method.

                // Convert number of intervals to millis.
                //
                long intervalTimeMillis = this.convertNumberOfIntervalsToMillis(numberOfIntervals, resolution);

                // Get period start time to include the interval.
                //
                long minStartPeriod = MetricsSummaryBin.periodStartFor(
                    resolution,
                    metricsServices.getFineInterval(),
                    System.currentTimeMillis() - intervalTimeMillis);

                metricsSummaryBins = getRecentBins(clusterNodeId, serviceGoids, resolution, minStartPeriod);
            }
            break;

            case RECENT_INTERVALS_WITHIN_TIME_PERIOD: {
                // Get interval time.
                //
                String intervalTimeStrVal = ExpandVariables.process(assertion.getNumberOfRecentIntervalsWithinTimePeriod(), vars, getAudit(), true);
                long intervalTime = Long.parseLong(intervalTimeStrVal); // NumberFormatException is caught by caller of this method.

                // Convert interval time to millis.
                //
                IntervalTimeUnit intervalTimeUnit = assertion.getIntervalTimeUnit();
                long intervalTimeMillis = this.convertIntervalTimeToMillis(intervalTime, intervalTimeUnit);

                // Get period start time to include the interval.
                //
                long minStartPeriod = MetricsSummaryBin.periodStartFor(
                    resolution,
                    metricsServices.getFineInterval(),
                    System.currentTimeMillis() - intervalTimeMillis);

                metricsSummaryBins = getRecentBins(clusterNodeId, serviceGoids, resolution, minStartPeriod);
            }
            break;

            default:
                throw new RuntimeException("Unexpected interval type.");
        }

        return metricsSummaryBins;
    }

    private MetricsSummaryBin getLatestBin (String clusterNodeId, Goid[] serviceGoids, int resolution) throws Exception {
        boolean includeEmpty = true;
        int durationMillis = -1;

        switch (resolution) {
            case MetricsBin.RES_FINE:
                durationMillis = metricsServices.getFineInterval(); // Latest "fine interval" amount.
                break;

            case MetricsBin.RES_HOURLY:
                durationMillis = 60 * 60 * 1000; // Latest hour.
                break;

            case MetricsBin.RES_DAILY:
                durationMillis = 24 * 60 * 60 * 1000; // Latest day.
                break;

            default:
                throw new RuntimeException("Unexpected resolution.");
        }

        return metricsManager.summarizeLatest(clusterNodeId, serviceGoids, MetricsBin.RES_FINE, durationMillis, includeEmpty);
    }

    private List<MetricsSummaryBin> getRecentBins (String clusterNodeId, Goid[] serviceGoids, int resolution, long minStartPeriod) throws Exception {
        boolean includeEmpty = true;
        Long maxStartPeriod = null;

        Collection<MetricsSummaryBin> unsortedBins =
            metricsManager.summarizeByPeriod(clusterNodeId, serviceGoids, resolution, minStartPeriod, maxStartPeriod, includeEmpty);
        List<MetricsSummaryBin> sortedBins = new LinkedList<MetricsSummaryBin>(unsortedBins);
        Collections.sort(sortedBins, metricsSummaryBinComparator);

        return sortedBins;
    }

    private long convertIntervalTimeToMillis (long intervalTime, IntervalTimeUnit intervalTimeUnit) {
        long millis = -1L;

        switch (intervalTimeUnit) {
            case SECONDS:
                millis = intervalTime * 1000L;
                break;

            case MINUTES:
                millis = intervalTime * 60L * 1000L;
                break;

            case HOURS:
                millis = intervalTime * 60L* 60L * 1000L;
                break;

            case DAYS:
                millis = intervalTime * 24L * 60L* 60L * 1000L;
                break;

            default:
                throw new RuntimeException("Unexpected interval time unit.");
        }

        return millis;
    }

    private long convertNumberOfIntervalsToMillis (long numberOfIntervals, int resolution) {
        long millis = -1L;

        switch (resolution) {
            case MetricsBin.RES_FINE:
                millis = numberOfIntervals * metricsServices.getFineInterval(); // getFineInterval() already returns in millis.
                break;

            case MetricsBin.RES_HOURLY:
                millis = numberOfIntervals * 60L * 60L * 1000L;
                break;

            case MetricsBin.RES_DAILY:
                millis = numberOfIntervals * 24L * 60L* 60L * 1000L;
                break;

            default:
                throw new RuntimeException("Unexpected resolution.");
        }

        return millis;
    }

    private void setContextVariables(PolicyEnforcementContext context, GatewayMetricsMessage metricsMessage) throws IOException {
        context.setVariable(assertion.getDocumentVariable(), metricsMessage.getFormattedMessage());
    }
}