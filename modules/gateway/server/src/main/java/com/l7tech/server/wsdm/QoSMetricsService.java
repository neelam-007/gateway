package com.l7tech.server.wsdm;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.GoidUpgradeMapper;
import com.l7tech.util.InetAddressUtil;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.service.MetricsSummaryBin;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.server.service.ServiceCache;
import com.l7tech.server.service.ServiceMetricsServices;
import com.l7tech.server.wsdm.faults.GenericWSRFExceptionFault;
import com.l7tech.server.wsdm.faults.ResourceUnknownFault;
import com.l7tech.server.wsdm.method.GetMultipleResourceProperties;
import com.l7tech.server.wsdm.method.ResourceProperty;
import com.l7tech.server.wsdm.util.EsmUtils;
import com.l7tech.server.wsdm.util.ISO8601Duration;
import com.l7tech.util.ISO8601Date;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.xml.soap.SoapUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.inject.Inject;
import javax.xml.soap.SOAPConstants;
import java.net.URL;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service handling the GetMultipleResourceProperties requests
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Nov 2, 2007<br/>
 */
public class QoSMetricsService {
    private final static Logger logger = Logger.getLogger(QoSMetricsService.class.getName());

    public static final String ESM_QOS_METRICS_SERVICE_NAME = "WSDM QosMetrics Service";
    public static final String ESM_QOS_METRICS_URI_PREFIX = "/wsdm/qosmetrics";
    public static final String ESM_QOS_METRICS_ROOT_WSDL = "qosmw-0.5.wsdl";

    @Inject
    private ServiceMetricsServices serviceMetricsServices;

    @Inject
    private ServiceCache serviceCache;

    @Inject
    private Aggregator aggregator;

    public Document handleMultipleResourcePropertiesRequest(GetMultipleResourceProperties method)
        throws GenericWSRFExceptionFault, ResourceUnknownFault {
        StringBuffer output = new StringBuffer();
        output.append(
                "<soap:Envelope xmlns:soap=\"" + SOAPConstants.URI_NS_SOAP_ENVELOPE + "\" xmlns:wsa=\"" + Namespaces.WSA + "\">\n" +
                "    <soap:Header>\n" +
                "        <wsa:Action>\n" +
                "            http://docs.oasis-open.org/wsrf/rpw-2/GetMultipleResourceProperties/GetMultipleResourcePropertiesResponse\n" +
                "        </wsa:Action>\n" +
                "        <wsa:RelatesTo>\n" +
                "            " + method.getMessageId() + "\n" +
                "        </wsa:RelatesTo>\n" +
                "    </soap:Header>\n" +
                "    <soap:Body xmlns:wsrf-rp=\"" + Namespaces.WSRF_RP + "\" " +
                               "xmlns:muws2=\""   + Namespaces.MUWS2   + "\" " +
                               "xmlns:wsnt=\""    + Namespaces.WSNT   + "\" " +
                               "xmlns:mows=\""    + Namespaces.MOWS   + "\" " +
                               "xmlns:qosm=\""    + Namespaces.QOSM   + "\" " +
                               "xmlns:muws1=\""   + Namespaces.MUWS1   + "\">\n" +
                "        <wsrf-rp:GetMultipleResourcePropertiesResponse>\n");

        String serviceId = EsmUtils.determineServiceFromUrl(method.getIncomingUrl().toString(), serviceCache);
        if (serviceId == null) {
            Element header;
            try {
                header = SoapUtil.getHeaderElement(method.getReqestDoc());
            } catch (InvalidDocumentFormatException e) {
                logger.log(Level.WARNING, "error getting header", e);
                header = null;
            }
            if (header != null) {
                Element residel = XmlUtil.findFirstChildElementByName(header, new String[] {Namespaces.MUWS1, Namespaces.MUWS2}, "ResourceId");
                if (residel != null) {
                    String residval = XmlUtil.getTextValue(residel);
                    serviceId = EsmUtils.determineServiceFromUrl(residval, serviceCache);
                    if (serviceId == null) {
                        logger.warning("could not extract a service id from the pattern " + residval);
                    }
                } else {
                    logger.info("No ResourceId element in header");
                }
            }
            if (serviceId == null) {
               logger.warning("cannot deduce service whose resource id this is meant for");
            }
        }

        Goid goid = null;
        boolean operational = false;
        if (serviceId != null) {
            try {
                goid = GoidUpgradeMapper.mapId(EntityType.SERVICE, serviceId);
                PublishedService ps = serviceCache.getCachedService(goid);
                if (ps == null) throw new ResourceUnknownFault("No service by the ");
                operational = !ps.isDisabled();
            } catch (IllegalArgumentException e) {
                logger.log(Level.WARNING, "Improperly formatted Service ID", e);
                throw new ResourceUnknownFault("Improperly formatted Service ID");
            } catch (Exception e) {
                logger.log(Level.WARNING, "error getting service from cache", e);
            }
        }


        final MetricsRequestContext context;
        if (goid != null) {
            MetricsSummaryBin bin = aggregator.getMetricsForService(goid);
            if (bin == null) {
                throw new GenericWSRFExceptionFault("No metrics information available");
            }

            context = new MetricsRequestContext(bin, operational, method.getIncomingUrl(), System.currentTimeMillis() - bin.getStartTime()); // TODO downtime
        } else {
            context = null;
        }

        for (ResourceProperty rp : method.getRequestedProperties()) {
            if (serviceId == null && rp.serviceSpecific()) {
                throw new ResourceUnknownFault("ResourceID Not Specified");
            }
            if (rp == ResourceProperty.AVG_RESPONSETIME) {
                output.append(outputAvgResponseTime(context));
            } else if (rp == ResourceProperty.CURRENT_TIME) {
                output.append(outputCurrentTime());
            } else if (rp == ResourceProperty.FAULTRATE) {
                output.append(outputFaultRate(context));
            } else if (rp == ResourceProperty.LAST_RESPONSETIME) {
                output.append(outputLastResponseTime(context));
            } else if (rp == ResourceProperty.MANAGEABILITY_CAPABILITY) {
                output.append(outputManageabilityCapabilities());
            } else if (rp == ResourceProperty.MAX_RESPONSETIME) {
                output.append(outputMaxResponseTime(context));
            } else if (rp == ResourceProperty.NUMBER_OF_FAILEDREQUESTS) {
                output.append(outputNrFailedRequests(context));
            } else if (rp == ResourceProperty.NUMBER_OF_REQUESTS) {
                output.append(outputNrRequests(context));
            } else if (rp == ResourceProperty.NUMBER_OF_SUCCESSFULREQUESTS) {
                output.append(outputNrSuccessRequests(context));
            } else if (rp == ResourceProperty.OPERATIONAL_STATUS) {
                output.append(outputOperationalStatus(context));
            } else if (rp == ResourceProperty.RESOURCE_ID) {
                output.append(outputResourceID(context));
            } else if (rp == ResourceProperty.SERVICE_TIME) {
                output.append(outputServiceTime(context));
            } else if (rp == ResourceProperty.THROUGHPUT) {
                output.append(outputThroughput(context));
            } else if (rp == ResourceProperty.TOPIC) {
                output.append(outputTopicCapabilities());
            }
        }

        output.append(
                "        </wsrf-rp:GetMultipleResourcePropertiesResponse>\n" +
                "    </soap:Body>\n" +
                "</soap:Envelope>");

        try {
            return XmlUtil.stringToDocument(output.toString());
        } catch (SAXException e) {
            logger.log(Level.WARNING, "error constructing response", e);
            throw new GenericWSRFExceptionFault("error constructing response " + e.getMessage());
        }
    }

    public static String outputResourceID(MetricsRequestContext context) {
        URL url = context.getIncomingURL();
        if (url == null) {
            logger.warning("Unexpected null incoming URL");
            return "";
        }
        String resourceID = url.getProtocol() + "://" + InetAddressUtil.getHostForUrl(url.getHost()) + ":" + url.getPort() + "/service/" + context.getServiceId();
        return  "           <muws1:ResourceId>\n" +
                "             " + resourceID + "\n" +
                "           </muws1:ResourceId>\n";
    }

    public static String outputOperationalStatus(MetricsRequestContext context) {
        String status;
        if (context.isOperational()) {
            status = "Available";
        } else {
            status = "Unavailable";
        }
        return  "           <muws2:OperationalStatus>\n" +
                "             " + status + "\n" +
                "           </muws2:OperationalStatus>\n";
    }

    public static String outputNrSuccessRequests(MetricsRequestContext context) {
        return  "           <mows:NumberOfSuccessfulRequests ResetAt=\"" + ISO8601Date.format(new Date(context.getPeriodStart())) +
                                              "\" LastUpdated=\"" + ISO8601Date.format(new Date(context.getLastUpdated())) +
                                              "\" Duration=\"" + context.getDuration() + "\">\n" +
                "             " + context.getNrSuccessRequests() + "\n" +
                "           </mows:NumberOfSuccessfulRequests>\n";
    }

    public static String outputNrRequests(MetricsRequestContext context) {
        return  "           <mows:NumberOfRequests ResetAt=\"" + ISO8601Date.format(new Date(context.getPeriodStart())) +
                                              "\" LastUpdated=\"" + ISO8601Date.format(new Date(context.getLastUpdated())) +
                                              "\" Duration=\"" + context.getDuration() + "\">\n" +
                "             " + context.getNrRequests() + "\n" +
                "           </mows:NumberOfRequests>\n";
    }

    public static String outputNrFailedRequests(MetricsRequestContext context) {
        return  "           <mows:NumberOfFailedRequests ResetAt=\"" + ISO8601Date.format(new Date(context.getPeriodStart())) +
                                              "\" LastUpdated=\"" + ISO8601Date.format(new Date(context.getLastUpdated())) +
                                              "\" Duration=\"" + context.getDuration() + "\">\n" +
                "             " + context.getNrFailedRequests() + "\n" +
                "           </mows:NumberOfFailedRequests>\n";
    }

    public static String outputThroughput(MetricsRequestContext context) {
        return  "           <qosm:Throughput ResetAt=\"" + ISO8601Date.format(new Date(context.getPeriodStart())) +
                                              "\" LastUpdated=\"" + ISO8601Date.format(new Date(context.getLastUpdated())) +
                                              "\" Duration=\"" + context.getThrouputPeriod() + "\">\n" +
                "             " + context.getThroughput() + "\n" +
                "           </qosm:Throughput>\n";
    }

    public static String outputAvgResponseTime(MetricsRequestContext context) {
        double val = context.getAvgResponseTime();
        val /= 1000.0;
        String output = "PT" + val + "S";
        return  "           <qosm:AvgResponseTime ResetAt=\"" + ISO8601Date.format(new Date(context.getPeriodStart())) +
                                              "\" LastUpdated=\"" + ISO8601Date.format(new Date(context.getLastUpdated())) +
                                              "\" Duration=\"" + context.getDuration() + "\">\n" +
                "             " + output + "\n" +
                "           </qosm:AvgResponseTime>\n";
    }

    public static String outputServiceTime(MetricsRequestContext context) {
        long val = context.getServiceTime();
        val /= 1000;
        String output = ISO8601Duration.durationFromSecs(val);
        //String output = "PT" + val + "S";
        return  "           <mows:ServiceTime ResetAt=\"" + ISO8601Date.format(new Date(context.getPeriodStart())) +
                                              "\" LastUpdated=\"" + ISO8601Date.format(new Date(context.getLastUpdated())) +
                                              "\" Duration=\"" + context.getDuration() + "\">\n" +
                "             " + output + "\n" +
                "           </mows:ServiceTime>\n";
    }

    public static String outputMaxResponseTime(MetricsRequestContext context) {
        double val = context.getMaxResponseTime();
        val /= 1000.0;
        String output = "PT" + val + "S";
        return  "           <mows:MaxResponseTime ResetAt=\"" + ISO8601Date.format(new Date(context.getPeriodStart())) +
                                              "\" LastUpdated=\"" + ISO8601Date.format(new Date(context.getLastUpdated())) +
                                              "\" Duration=\"" + context.getDuration() + "\">\n" +
                "             " + output + "\n" +
                "           </mows:MaxResponseTime>\n";
    }

    public static String outputLastResponseTime(MetricsRequestContext context) {
        double val = context.getLastResponseTime();
        val /= 1000.0;
        String output = "PT" + val + "S";
        return  "           <mows:LastResponseTime ResetAt=\"" + ISO8601Date.format(new Date(context.getPeriodStart())) +
                                              "\" LastUpdated=\"" + ISO8601Date.format(new Date(context.getLastUpdated())) +
                                              "\" Duration=\"" + context.getDuration() + "\">\n" +
                "             " + output + "\n" +
                "           </mows:LastResponseTime>\n";
    }

    private String outputFaultRate(MetricsRequestContext context) {
        // i'm not sure where i got this from, it's no longer in the spec documents v 0.5
        logger.severe("received request for currently unsupported fault rate (gone from 0.5 spec)");
        return "";
    }

    private static String outputCurrentTime() {
        String currentTime = ISO8601Date.format(new Date(System.currentTimeMillis()));
        return  "           <muws2:CurrentTime>\n" +
                "               " + currentTime + "\n" +
                "           </muws2:CurrentTime>\n";
    }

    private String outputTopicCapabilities() {
        return  "            <wsnt:Topic Dialect=\"http://docs.oasis-open.org/wsn/t-1/TopicExpression/Simple\">\n" +
                "                muws-ev:OperationalStatusCapability\n" +
                "            </wsnt:Topic>\n" +
                "            <wsnt:Topic Dialect=\"http://docs.oasis-open.org/wsn/t-1/TopicExpression/Simple\">\n" +
                "                mowse:MetricsCapability\n" +
                "            </wsnt:Topic>\n";
    }

    private String outputManageabilityCapabilities() {
        return  "            <muws1:ManageabilityCapability>\n" +
                "                http://docs.oasis-open.org/wsdm/muws/capabilities/Identity\n" +
                "            </muws1:ManageabilityCapability>\n" +
                "            <muws1:ManageabilityCapability>\n" +
                "                http://docs.oasis-open.org/wsdm/muws/capabilities/ManageabilityCharacteristics\n" +
                "            </muws1:ManageabilityCapability>\n" +
                "            <muws1:ManageabilityCapability>\n" +
                "                http://docs.oasis-open.org/wsdm/muws/capabilities/OperationalStatus\n" +
                "            </muws1:ManageabilityCapability>\n" +
                "            <muws1:ManageabilityCapability>\n" +
                "                http://docs.oasis-open.org/mows-2/capabilities/Metrics\n" +
                "            </muws1:ManageabilityCapability>\n" +
                "            <muws1:ManageabilityCapability>\n" +
                "                http://docs.oasis-open.org/wsdm/muws/capabilities/Metrics\n" +
                "            </muws1:ManageabilityCapability>\n";
    }

//    private String getOIDFromString(String url) {
//        Matcher matcher = oidPattern.matcher(url);
//        if (matcher.find() && matcher.groupCount() == 1) {
//            return matcher.group(1);
//        }
//
//        return getOIDFromString2(url);
//    }
//
//    private String getOIDFromString2(String url) {
//        Matcher matcher = oidPattern2.matcher(url);
//        if (matcher.find() && matcher.groupCount() == 1) {
//            return matcher.group(1);
//        }
//
//        return null;
//    }
}