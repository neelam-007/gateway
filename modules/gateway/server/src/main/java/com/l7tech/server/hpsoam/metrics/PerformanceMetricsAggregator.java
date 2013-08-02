package com.l7tech.server.hpsoam.metrics;

import com.l7tech.util.Background;
import com.l7tech.util.ISO8601Date;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.util.DomUtils;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.TrafficMonitor;
import com.l7tech.server.hpsoam.ServiceManagedObject;
import com.l7tech.server.hpsoam.WSMFService;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.gateway.common.service.PublishedService;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Aggregates performance metrics for container MO to return to HP SOAM NS when asked for
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Sep 19, 2007<br/>
 */
public class PerformanceMetricsAggregator implements TrafficMonitor {
    private final Logger logger = Logger.getLogger(PerformanceMetricsAggregator.class.getName());
    private final ArrayList<PerfWindow> performanceWindows = new ArrayList<PerfWindow>();
    private long lastWindowIndex = -1;
    private final ServiceManager serviceManager;

    public PerformanceMetricsAggregator(MessageProcessor mp, ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
        mp.registerTrafficMonitorCallback(this);

        Background.scheduleRepeated(new TimerTask() {
            public void run() {
                maintenance();
            }
        }, 10000, 30000);
    }

    private void maintenance() {
        if (WSMFService.isEnabled()) {
            synchronized (performanceWindows) {
                // create missing windows
                long now = System.currentTimeMillis();
                PerfWindow active;
                if (performanceWindows.isEmpty()) {
                    active = createNewWindow(now);
                    ++lastWindowIndex;
                    performanceWindows.add(active);
                    active.setWindowIndex(lastWindowIndex);
                } else {
                    active = performanceWindows.get(performanceWindows.size()-1);
                    // add new window when last one is older than 30 seconds
                    while (now > active.getWindowEnd()) {
                        active = createNewWindow(active.getWindowEnd());
                        ++lastWindowIndex;
                        active.setWindowIndex(lastWindowIndex);
                        performanceWindows.add(active);
                    }
                }

                // delete ones that are too old
                while (performanceWindows.size() > 100) {
                    performanceWindows.remove(0);
                }
            }
        }
    }

    private PerfWindow createNewWindow(long tstamp) {
        PerfWindow output = new PerfWindow(tstamp);
        // populate with existing services to avoid holes in the metrics between windows
        Collection<PublishedService> services;
        try {
            services = serviceManager.findAll();
        } catch (FindException e) {
            logger.log(Level.SEVERE, "cannot search for services", e);
            throw new RuntimeException(e);
        }
        for (PublishedService svc : services) {
            if (svc.isSoap()) {
                output.getSvcPerfs().add(new ServicePerformance(svc));
            }
        }
        return output;
    }

    public void recordTransactionStatus(PolicyEnforcementContext context, AssertionStatus status, long processingTime) {
        if (!WSMFService.isEnabled()) return;

        if (context.getService() == null) {
            logger.finest("Ignoring transactions for non resolved service");
            return;
        }
        if (!context.getService().isSoap()) {
            logger.finest("Ignoring transactions for non-soap service");
            return;
        }
        synchronized (performanceWindows) {
            // select currently relevant window
            PerfWindow active;
            long now = System.currentTimeMillis();
            // look at last window
            if (performanceWindows.isEmpty()) {
                active = createNewWindow(now);
                ++lastWindowIndex;
                performanceWindows.add(active);
                active.setWindowIndex(lastWindowIndex);
            } else {
                active = performanceWindows.get(performanceWindows.size()-1);
                // add new window when last one is older than 30 seconds
                while (now > active.getWindowEnd()) {
                    active = createNewWindow(active.getWindowEnd());
                    ++lastWindowIndex;
                    active.setWindowIndex(lastWindowIndex);
                    performanceWindows.add(active);
                }
            }

            // select servicePerformance object relevant
            ServicePerformance sp = active.getOrMakeServicePerformance(context);

            // select operation object relevant
            OperationPerformance op = sp.getOrMakeOperationPerformance(context);

            // add data
            // success case
            if (status.getNumeric() == AssertionStatus.NONE.getNumeric()) {
                op.getMetrics().addSuccessCombinedResponseTime(processingTime);
            } else if(context.isRequestPolicyViolated()) {// violation case
                op.getMetrics().addSecurityViolationMessageCount();
            } else { // failure case
                op.getMetrics().addFailureCombinedResponseTime(processingTime);
            }
        }
    }

    public String handlePerformanceWindowRequest(WSMFService.RequestContext context) {
        long windowIndexRequested;
        try {
            Element body = SoapUtil.getBodyElement(context.payloadXml);
            Element getel = DomUtils.findFirstChildElementByName(body, "http://openview.hp.com/xmlns/mip/2005/03/Wsee", "Get");
            Element param0 = DomUtils.findFirstChildElementByName(getel, "http://openview.hp.com/xmlns/mip/2005/03/Wsee", "param0");
            String param0Value = DomUtils.getTextValue(param0);
            windowIndexRequested = Long.parseLong(param0Value);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Could not establish requested window index from " + context.payload, e);
            return emptyResponse();
        }

        StringBuilder logMsg = null;
        if (logger.isLoggable(Level.FINE)) {
            logMsg = new StringBuilder("Received request for window " + windowIndexRequested + "; responding with windows:");
        }

        StringBuffer output = new StringBuffer();
        output.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        output.append("<soapenv:Envelope\n");
        output.append("        xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"\n");
        output.append("        xmlns:Wsee=\"http://openview.hp.com/xmlns/mip/2005/03/Wsee\"\n");
        output.append("        xmlns:perf=\"http://openview.hp.com/xmlns/soa/1/PerformanceWindow.xsd\">\n");
        output.append("    <soapenv:Body>\n");
        output.append("        <Wsee:GetResponse>\n");
        output.append("            <perf:PerformanceWindowList>\n");
        for (PerfWindow pw : performanceWindows) {
            if (pw.getWindowIndex() >= windowIndexRequested) {
                if (logger.isLoggable(Level.FINE)) logMsg.append(" ").append(pw.getWindowIndex());
                output.append("                <perf:PerformanceWindow>\n");
                output.append("                    <perf:WindowIndex>" + pw.getWindowIndex() + "</perf:WindowIndex>\n");
                output.append("                    <perf:StartOfWindow>" + ISO8601Date.format(new Date(pw.getWindowStart())) + "</perf:StartOfWindow>\n");
                output.append("                    <perf:EndOfWindow>" + ISO8601Date.format(new Date(pw.getWindowEnd())) + "</perf:EndOfWindow>\n");
                output.append("                    <perf:ServicePerformanceList>\n");
                for (ServicePerformance svcPerf : pw.getSvcPerfs()) {
                    output.append("                        <perf:ServicePerformance>\n");
                    output.append("                            <perf:ServiceNamespace>" + svcPerf.getNs() + "</perf:ServiceNamespace>\n");
                    output.append("                            <perf:ServiceLocalName>" + svcPerf.getLocalName() + "</perf:ServiceLocalName>\n");
                    output.append("                            <perf:ServiceWsdlUrl>" + ServiceManagedObject.generateContainedWsdlUrl(context.req, svcPerf.getServiceGOID().toHexString()) + "</perf:ServiceWsdlUrl>\n");
                    output.append("                            <perf:OperationPerformanceList>\n");
                    for (OperationPerformance operf : svcPerf.getOpPerfs()) {
                        output.append("                                <perf:OperationPerformance>\n");
                        output.append("                                    <perf:OperationNamespace>" + operf.getNs() + "</perf:OperationNamespace>\n");
                        output.append("                                    <perf:OperationLocalName>" + operf.getLocalName() + "</perf:OperationLocalName>\n");
                        output.append("                                    <perf:PerformanceMetrics>\n");
                        output.append("                                        <perf:SuccessMessageCount>" + operf.getMetrics().getSuccessMessageCount() + "</perf:SuccessMessageCount>\n");
                        output.append("                                        <perf:SuccessAverageResponseTime>" + operf.getMetrics().getSuccessAverageResponseTime() + "</perf:SuccessAverageResponseTime>\n");
                        output.append("                                        <perf:SuccessMinimumResponseTime>" + operf.getMetrics().getSuccessMinimumResponseTime() + "</perf:SuccessMinimumResponseTime>\n");
                        output.append("                                        <perf:SuccessMaximumResponseTime>" + operf.getMetrics().getSuccessMaximumResponseTime() + "</perf:SuccessMaximumResponseTime>\n");

                        output.append("                                        <perf:FailureMessageCount>" + operf.getMetrics().getFailureMessageCount() + "</perf:FailureMessageCount>\n");
                        output.append("                                        <perf:FailureAverageResponseTime>" + operf.getMetrics().getFailureAverageResponseTime() + "</perf:FailureAverageResponseTime>\n");
                        output.append("                                        <perf:FailureMinimumResponseTime>" + operf.getMetrics().getFailureMinimumResponseTime() + "</perf:FailureMinimumResponseTime>\n");
                        output.append("                                        <perf:FailureMaximumResponseTime>" + operf.getMetrics().getFailureMaximumResponseTime() + "</perf:FailureMaximumResponseTime>\n");
                        output.append("                                        <perf:SecurityViolationMessageCount>" + operf.getMetrics().getSecurityViolationMessageCount() + "</perf:SecurityViolationMessageCount>\n");
                        output.append("                                    </perf:PerformanceMetrics>\n");
                        output.append("                                </perf:OperationPerformance>\n");
                    }
                    output.append("                            </perf:OperationPerformanceList>\n");
                    output.append("                        </perf:ServicePerformance>\n");
                }                        
                output.append("                    </perf:ServicePerformanceList>\n");
                output.append("                </perf:PerformanceWindow>\n");
            }
        }
        output.append("            </perf:PerformanceWindowList>\n");
        output.append("        </Wsee:GetResponse>\n");
        output.append("    </soapenv:Body>\n");
        output.append("</soapenv:Envelope>");
        if (logger.isLoggable(Level.FINE)) { logger.fine(logMsg.toString()); }
        return output.toString();
    }

    private String emptyResponse() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<soapenv:Envelope\n" +
                "        xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
                "        xmlns:Wsee=\"http://openview.hp.com/xmlns/mip/2005/03/Wsee\"\n" +
                "        xmlns:perf=\"http://openview.hp.com/xmlns/soa/1/PerformanceWindow.xsd\">\n" +
                "    <soapenv:Body>\n" +
                "        <Wsee:GetResponse>\n" +
                "            <perf:PerformanceWindowList>\n" +
                "            </perf:PerformanceWindowList>\n" +
                "        </Wsee:GetResponse>\n" +
                "    </soapenv:Body>\n" +
                "</soapenv:Envelope>";
    }

    /*

    Let us assume that a web services container has 2 web services, WebServ1 and WebServ2. WebServ1 has Oper1 and Oper2 operations and WebServ2 has Oper3 operation.
    The SOAM agent must collect the time taken for completing the operations Oper1, Oper2 and Oper3 when they are invoked. This can be done in any of the following ways:
    � Intercepting through a handler, plug in, or a filter
    � Collecting through some of the already available APIs provided by the web services container
    After the performance metrics are collected, the SOAM agent must aggregate metrics at operation level for a window at every 30 seconds. The agent must also implement a rolling window of size 60 to store 30 minutes of data at any point of time.
    In each window, there must be separate aggregates for each operation, Oper1, Oper2, Oper3, and the performance metrics are stored to easily expose the metrics.

    The request schema in this section contains a window index in element param0. The response for this request contains all the windows with an index value greater than or equal to the index value specified in the request.

    The response schema in this section contains windows with an index value greater than or equal to the index value specified in the request. The response also contains metrics for all the web services managed by the SOAM agent. The web service metrics are displayed based on the operations they perform.

     */

}
