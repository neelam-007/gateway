package com.l7tech.external.assertions.gatewaymetrics.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.gatewaymetrics.IntervalType;
import com.l7tech.gateway.common.cluster.ClusterNodeInfo;
import com.l7tech.gateway.common.cluster.GatewayStatus;
import com.l7tech.gateway.common.cluster.ServiceUsage;
import com.l7tech.gateway.common.logging.StatisticsRecord;
import com.l7tech.gateway.common.service.MetricsBin;
import com.l7tech.gateway.common.service.MetricsSummaryBin;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.service.ServiceManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: cirving
 * Date: 7/19/12
 * Time: 12:38 PM
 * To change this template use File | Settings | File Templates.
 */
public class GatewayMetricsMessage {
    private static final Logger logger = Logger.getLogger(GatewayMetricsMessage.class.getName());
    private static final String MISSING_SERVICE = "<MISSING SERVICE>";
    private static final String MESSAGE_MISSING_SERVICE = "ServiceUsage record GOID={0} points to nonexistent " +
            "published service by primary key {1}";
    private static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

    private String clusterNodeId;
    private Goid serviceGoid;
    private int resolution;
    private IntervalType intervalType;
    private ClusterNodeInfo[] clusterNodeInfos;
    private ServiceUsage[] serviceUsages;
    private List<MetricsSummaryBin> metricsSummaryBins;
    private ServiceManager serviceManager;
    private Map<String, String> clusterNodes = new ConcurrentHashMap(); // Key = node ID, value = node name
    private Document document;
    private Element rootElement;

    public GatewayMetricsMessage(String clusterNodeId,
                                 Goid serviceGoid,
                                 int resolution,
                                 IntervalType intervalType,
                                 ClusterNodeInfo[] clusterNodeInfos,
                                 ServiceUsage[] serviceUsages,
                                 List<MetricsSummaryBin> metricsSummaryBins,
                                 ServiceManager serviceManager) throws SAXException, GatewayMetricsException {

        this.clusterNodeId = clusterNodeId;
        this.serviceGoid = serviceGoid;
        this.resolution = resolution;
        this.intervalType = intervalType;
        this.clusterNodeInfos = clusterNodeInfos;
        this.serviceUsages = serviceUsages;
        this.metricsSummaryBins = metricsSummaryBins;
        this.serviceManager = serviceManager;

        this.createDocument();
    }

    public String getFormattedMessage() throws IOException{
        return XmlUtil.nodeToFormattedString(document);
    }

    private void createDocument() throws SAXException, GatewayMetricsException {
        document = XmlUtil.createEmptyDocument("gatewayMetrics", null, null);

        rootElement = (Element) document.getFirstChild();
        rootElement.setAttribute("time", this.formatTime(System.currentTimeMillis()));

        this.addServiceMetricsBinsElements();
        this.addGatewayStatusElements();
        try {
            this.addServiceStatisticElements();
        } catch (FindException e) {
            logger.log(Level.WARNING, "Unable to lookup service name by id");
            throw new GatewayMetricsException("Unable to lookup service name by id");
        }
    }

    private void addServiceMetricsBinsElements() {
        Element serviceMetricsElement = this.addElement("serviceMetricsBin", rootElement);

        serviceMetricsElement.setAttribute("clusterNode", this.getClusterAsString(clusterNodeId));
        serviceMetricsElement.setAttribute("service", getServiceAsString(serviceGoid));
        serviceMetricsElement.setAttribute("resolution", getResolutionAsString(resolution));

        for (MetricsSummaryBin currentBin : metricsSummaryBins) {
            if (null != currentBin) {
                String periodStart = this.formatTime(currentBin.getPeriodStart());
                String periodEnd = this.formatTime(currentBin.getPeriodEnd());

                String minFrontendResponseTime = Integer.toString(currentBin.getMinFrontendResponseTime() == null ? 0 : currentBin.getMinFrontendResponseTime());
                double avgFrontendResponseTime = currentBin.getAverageFrontendResponseTime();
                String maxFrontendResponseTime = Integer.toString(currentBin.getMaxFrontendResponseTime() == null ? 0 : currentBin.getMaxFrontendResponseTime());

                String minBackendResponseTime = Integer.toString(currentBin.getMinBackendResponseTime() == null ? 0 : currentBin.getMinBackendResponseTime());
                double avgBackendResponseTime = currentBin.getAverageBackendResponseTime();
                String maxBackendResponseTime = Integer.toString(currentBin.getMaxBackendResponseTime() == null ? 0 : currentBin.getMaxBackendResponseTime());

                int numberRoutingFailure = currentBin.getNumRoutingFailure();
                int numberPolicyViolation = currentBin.getNumPolicyViolation();
                int totalNumberOfSuccessfulRequests = currentBin.getNumSuccess();
                int totalNumberOfRequest = currentBin.getNumTotal();

                Element serviceMetricElement = this.addElement("serviceMetrics", serviceMetricsElement);
                serviceMetricElement.setAttribute("startTime", periodStart);
                serviceMetricElement.setAttribute("endTime", periodEnd);

                Element currentElement = null;
                currentElement = this.addElement("minFrontendResponseTime", serviceMetricElement);
                currentElement.setAttribute("rate", "milliseconds");
                currentElement.setTextContent(minFrontendResponseTime);

                currentElement = this.addElement("avgFrontendResponseTime", serviceMetricElement);
                currentElement.setAttribute("rate", "milliseconds");
                currentElement.setTextContent(this.formatNumber(avgFrontendResponseTime));

                currentElement = this.addElement("maxFrontendResponseTime", serviceMetricElement);
                currentElement.setAttribute("rate", "milliseconds");
                currentElement.setTextContent(maxFrontendResponseTime);

                currentElement = this.addElement("minBackendResponseTime", serviceMetricElement);
                currentElement.setAttribute("rate", "milliseconds");
                currentElement.setTextContent(minBackendResponseTime);

                currentElement = this.addElement("avgBackendResponseTime", serviceMetricElement);
                currentElement.setAttribute("rate", "milliseconds");
                currentElement.setTextContent(this.formatNumber(avgBackendResponseTime));

                currentElement = this.addElement("maxBackendResponseTime", serviceMetricElement);
                currentElement.setAttribute("rate", "milliseconds");
                currentElement.setTextContent(maxBackendResponseTime);

                this.addElement("numberRoutingFailure", serviceMetricElement).setTextContent(String.valueOf(numberRoutingFailure));
                this.addElement("numberPolicyViolation", serviceMetricElement).setTextContent(String.valueOf(numberPolicyViolation));
                this.addElement("totalNumberOfSuccessfulRequests", serviceMetricElement).setTextContent(String.valueOf(totalNumberOfSuccessfulRequests));
                this.addElement("totalNumberOfRequest", serviceMetricElement).setTextContent(String.valueOf(totalNumberOfRequest));

                if (intervalType.equals(IntervalType.RECENT_NUMBER_OF_INTERVALS) ||
                        intervalType.equals(IntervalType.RECENT_INTERVALS_WITHIN_TIME_PERIOD)) {
                    // The rates only apply to metrics already collected for a nominal period.
                    //
                    double routingFailureRate = currentBin.getNominalRoutingFailureRate();
                    double policyViolationRate = currentBin.getNominalPolicyViolationRate();
                    double successfulRequestRate = currentBin.getNominalSuccessRate();

                    this.addElement("routingFailureRate", serviceMetricElement).setTextContent(this.formatNumber(routingFailureRate));
                    this.addElement("policyViolationRate", serviceMetricElement).setTextContent(this.formatNumber(policyViolationRate));
                    this.addElement("successfulRequestRate", serviceMetricElement).setTextContent(this.formatNumber(successfulRequestRate));
                }
            }
        }
    }

    private void addGatewayStatusElements() {
        Element gatewayStatusElement = this.addElement("gatewayStatus", rootElement);

        for (ClusterNodeInfo currentNode : clusterNodeInfos) {
            // If a specific node is requested for, only add data for that node
            if(null != clusterNodeId && !clusterNodeId.equals(currentNode.getId())) {
                continue;
            }
            clusterNodes.put(currentNode.getId(), currentNode.getName());

            GatewayStatus gatewayStatus = new GatewayStatus(currentNode);
            String nodeName = gatewayStatus.getName();
            int loadSharing = gatewayStatus.getLoadSharing();
            int requestRouted = gatewayStatus.getRequestRouted();
            double avgLoad = gatewayStatus.getAvgLoad();
            long upTime = gatewayStatus.getUptime();
            String ipAddress = gatewayStatus.getAddress();

            Element clusterNodeElement = this.addElement("clusterNode", gatewayStatusElement);
            clusterNodeElement.setAttribute("name", nodeName);

            this.addElement("loadSharingPercentage", clusterNodeElement).setTextContent(String.valueOf(loadSharing));
            this.addElement("requestRoutedPercentage", clusterNodeElement).setTextContent(String.valueOf(requestRouted));
            this.addElement("loadAvg", clusterNodeElement).setTextContent(String.valueOf(avgLoad));
            Element currentElement= this.addElement("uptime", clusterNodeElement);
            currentElement.setAttribute("rate", "milliseconds");
            currentElement.setTextContent(String.valueOf(upTime));
            this.addElement("IP", clusterNodeElement).setTextContent(ipAddress);
        }
    }

    private void addServiceStatisticElements() throws FindException {
        long numRoutingFailuresTotal = 0;
        long numPolicyViolationsTotal = 0;
        long numSuccessesTotal = 0;

        Element serviceStatsElement = this.addElement("serviceStatistics", rootElement);

        for (ServiceUsage currentServiceUsage : serviceUsages) {
            // If a specific node is requested for, only add data for that node
            if(null != clusterNodeId && !clusterNodeId.equals(currentServiceUsage.getNodeid())) {
                continue;
            }
            PublishedService publishedService = serviceManager.findByPrimaryKey(currentServiceUsage.getServiceid());
            if (publishedService == null) {
                logger.log(Level.WARNING, MESSAGE_MISSING_SERVICE,
                        new Object[] {currentServiceUsage.getGoid(), currentServiceUsage.getServiceid()});
            }

            StatisticsRecord statsRecord = new StatisticsRecord(
                getServiceIdOrName(currentServiceUsage, publishedService),
                currentServiceUsage.getRequests(),
                currentServiceUsage.getAuthorized(),
                currentServiceUsage.getCompleted(),
                0);

            String serviceName = statsRecord.getServiceName();
            Goid serviceId = currentServiceUsage.getServiceid();
            String nodeId = currentServiceUsage.getNodeid();
            String nodeName = clusterNodes.get(nodeId);
            String routingUri = getServiceNameOrRoutingUri(currentServiceUsage, publishedService);
            long numRoutingFailures = statsRecord.getNumRoutingFailure();
            long numPolicyViolations = statsRecord.getNumPolicyViolation();
            long numSuccesses = statsRecord.getCompletedCount();

            Element serviceStatElement = this.addElement("serviceStat", serviceStatsElement);
            serviceStatElement.setAttribute("name", serviceName);
            serviceStatElement.setAttribute("serviceId", String.valueOf(serviceId));
            serviceStatElement.setAttribute("clusterNode", nodeName);
            serviceStatElement.setAttribute("routingUri", routingUri);

            this.addElement("routingFailures", serviceStatElement).setTextContent(String.valueOf(numRoutingFailures));
            this.addElement("policyViolations", serviceStatElement).setTextContent(String.valueOf(numPolicyViolations));
            this.addElement("successCount", serviceStatElement).setTextContent(String.valueOf(numSuccesses));

            numRoutingFailuresTotal += numRoutingFailures;
            numPolicyViolationsTotal += numPolicyViolations;
            numSuccessesTotal += numSuccesses;
        }

        Element serviceTotalsElement = this.addElement("serviceTotals", serviceStatsElement);
        this.addElement("routingFailuresTotal", serviceTotalsElement).setTextContent(String.valueOf(numRoutingFailuresTotal));
        this.addElement("policyViolationsTotal", serviceTotalsElement).setTextContent(String.valueOf(numPolicyViolationsTotal));
        this.addElement("successCountTotal", serviceTotalsElement).setTextContent(String.valueOf(numSuccessesTotal));
    }

    private Element addElement (final String tagName, final Element parentElement) {
        Element element = document.createElement(tagName);
        parentElement.appendChild(element);
        return element;
    }

    private static String getServiceIdOrName(ServiceUsage serviceUsage, PublishedService publishedService) {
        return publishedService == null ? MISSING_SERVICE + serviceUsage.getServiceid() : publishedService.getName();
    }

    private static String getServiceNameOrRoutingUri(ServiceUsage serviceUsage, PublishedService publishedService) {
        return publishedService == null ? MISSING_SERVICE + serviceUsage.getServiceid() : publishedService.getRoutingUri();
    }

    private String formatTime(long time) {
        String date = dateFormatter.format(new Date(time));
        // Convert to ISO 8601 format.
        // from "2013-02-25T10:34:31-0800" to "2013-02-25T10:34:31-08:00"
        date = new StringBuffer(date).insert(date.length()-2, ':').toString();
        return date;
    }

    private String formatNumber(double number) {
        // Display 5 decimal places.
        return String.format("%.5f", number);
    }

    private String getClusterAsString(String clusterNodeId) {
        if (clusterNodeId == null) {
            return "All Nodes";
        }

        for (int ix = 0; ix < clusterNodeInfos.length; ix++) {
            if (clusterNodeInfos[ix].getId().equals(clusterNodeId)) {
                return clusterNodeInfos[ix].getName();
            }
        }

        return clusterNodeId;
    }

    private String getServiceAsString(Goid serviceGoid) {
        if (serviceGoid.equals(Goid.DEFAULT_GOID)) {
            return "All Services";
        }

        try {
            PublishedService service = serviceManager.findByPrimaryKey(serviceGoid);
            if (service != null) {
                return service.getName();
            } else {
                return String.valueOf(serviceGoid);
            }
        } catch (FindException e) {
            return String.valueOf(serviceGoid);
        }
    }

    private String getResolutionAsString(int resolution) {
        return MetricsBin.describeResolution(resolution);
    }
}