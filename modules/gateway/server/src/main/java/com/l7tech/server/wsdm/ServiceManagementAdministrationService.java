package com.l7tech.server.wsdm;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.event.ServiceEnablementEvent;
import com.l7tech.server.service.ServiceCache;
import com.l7tech.server.wsdm.faults.FaultMappableException;
import com.l7tech.server.wsdm.faults.ResourceUnknownFault;
import com.l7tech.server.wsdm.method.*;
import com.l7tech.server.wsdm.subscription.ServiceStateMonitor;
import com.l7tech.server.wsdm.subscription.Subscription;
import com.l7tech.server.wsdm.subscription.SubscriptionNotifier;
import com.l7tech.server.wsdm.util.EsmUtils;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.message.Message;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.IOException;

/**
 * Entry point for the ESM subsystem
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Nov 2, 2007<br/>
 */
public class ServiceManagementAdministrationService implements ApplicationListener {
    private static final Logger logger = Logger.getLogger(ServiceManagementAdministrationService.class.getName());

    @Resource
    private QoSMetricsService qosService;
    @Resource
    private SubscriptionNotifier subscriptionNotifier;
    @Resource
    private ServiceCache serviceCache;

    private final ArrayList<ServiceStateMonitor> serviceStateMonitors = new ArrayList<ServiceStateMonitor>();
    private final List<ServiceStateMonitor> monitors;

    public ServiceManagementAdministrationService(List<ServiceStateMonitor> monitors) {
        this.monitors = monitors;
    }

    @PostConstruct
    public void start() {
        serviceStateMonitors.add(subscriptionNotifier);
        serviceStateMonitors.addAll(monitors);
    }

    /**
     * Entry point for ESM subsystem. This could be called by a servlet or a special ESM assertion.
     *
     * @param incomingURL      the URL for that request
     * @param incomingRequest  the incoming request message
     * @return a response xml
     * @throws FaultMappableException in case the service should return a soap fault
     * @throws SAXException     if there's an error processing the request
     */
    public Document handleESMRequest(URL incomingURL, Message incomingRequest) throws FaultMappableException, IOException, SAXException {
        // method classification
        ESMMethod method = ESMMethod.resolve(incomingRequest);

        if (method instanceof GetMultipleResourceProperties) {
            return qosService.handleMultipleResourcePropertiesRequest(incomingURL, (GetMultipleResourceProperties)method);
        } else {
            throw new FaultMappableException( buildUnknownMethodMessage(method) );
        }
    }

    /**
     * Entry point for ESM subsystem. This could be called by a servlet or a special ESM assertion.
     *
     * @param incomingURL      the URL for that request
     * @param incomingRequest  the incoming request message
     * @return a response xml
     * @throws FaultMappableException in case the service should return a soap fault
     * @throws SAXException     if there's an error processing the request
     */
    public Document handleSubscriptionRequest(URL incomingURL, Message incomingRequest, String policyGuid)
        throws FaultMappableException, IOException, SAXException {
        // method classification
        ESMMethod method = ESMMethod.resolve(incomingRequest);

        try {
            if (method instanceof Renew) {
                return respondToRenew((Renew)method, incomingURL, policyGuid);
            } else if (method instanceof Subscribe) {
                return respondToSubscribe((Subscribe)method, incomingURL, policyGuid);
            } else if (method instanceof Unsubscribe) {
                return respondToUnsubscribe((Unsubscribe)method, incomingURL);
            } else {
                throw new FaultMappableException( buildUnknownMethodMessage(method) );
            }
        } catch (SAXException e) {
            logger.log(Level.WARNING, "Problem constructing response to " + method, e);
            throw new FaultMappableException(e.getMessage());
        }
    }

    private String buildUnknownMethodMessage( final ESMMethod method ) {
        String message;
        if ( method == null ) {
            message = "Unsupported operation.";
        } else {
            message = "Method not supported: " + method.toString();
        }

        return message;
    }

    private Document respondToUnsubscribe(Unsubscribe unsubscribe, URL incomingURL)
        throws SAXException, ResourceUnknownFault {
        subscriptionNotifier.unsubscribe(unsubscribe.getSubscriptionIdValue());
        String output = unsubscribe.respond(incomingURL.toString());
        return XmlUtil.stringToDocument(output);
    }

    private Document respondToSubscribe(Subscribe subscribe, URL incomingURL, String policyGuid)
        throws SAXException, FaultMappableException {
        // look for a resource id
        String serviceID = identifyService(incomingURL.toString(), subscribe.getReqestDoc());
        if (serviceID != null) {
            subscribe.setServiceId(serviceID);
        } else {
            logger.warning("Subscription to a service without any resource id specified");
            throw new ResourceUnknownFault("Resource ID required");
        }
        Subscription res = subscriptionNotifier.subscribe(subscribe, policyGuid);

        String output = subscribe.respond(incomingURL.toString(), res.getUuid());
        return XmlUtil.stringToDocument(output);
    }

    private String identifyService(String incomingURL, Document fullDoc) throws ResourceUnknownFault {

        String serviceid = EsmUtils.determineServiceFromUrl(incomingURL, serviceCache);
        if (serviceid != null) {
            return serviceid;
        } else {
            Element header = null;
            try {
                header = SoapUtil.getHeaderElement(fullDoc);
            } catch (InvalidDocumentFormatException e) {
                logger.log(Level.WARNING, "error getting header", e);
            }
            if (header != null) {
                Element residel = XmlUtil.findFirstChildElementByName(header, new String[]{ Namespaces.MUWS1, Namespaces.MUWS2 }, "ResourceId");
                if (residel != null) {
                    String residval = XmlUtil.getTextValue(residel);
                    serviceid = EsmUtils.determineServiceFromUrl(residval, serviceCache);
                    if (serviceid != null) {
                        return serviceid;
                    } else {
                        logger.warning("could not extract a service id from the pattern " + residval);
                    }
                } else {
                    logger.info("No ResourceId element in header");
                }
            }
            logger.warning("cannot deduce service whose resource id this is meant for");
        }
        return null;
    }

    private Document respondToRenew(Renew renew, URL incomingURL, String policyGuid) throws SAXException, FaultMappableException {
        subscriptionNotifier.renewSubscription(renew.getSubscriptionIdValue(), renew.getTermination(), policyGuid);
        String output = renew.respond(incomingURL.toString());
        return XmlUtil.stringToDocument(output);
    }

    /*
    private Document respondToGetResourceProperty(GetResourceProperty method, URL incomingURL) throws FaultableException {
        String output;
        if (method.getPropertyRequested() == ResourceProperty.TOPIC) {
            output = method.respondToTopicPropertyRequest();
        } else if (method.getPropertyRequested() == ResourceProperty.MANAGEABILITY_CAPABILITY) {
            output = method.responseToManageabilityCapability();
        } else {
            throw new FaultableException("This property cannot be queried that way");
        }
        return XmlUtil.stringAsDocument(output);
    }

    private Document respondToGetManageabilityReferences(GetManageabilityReferences method,
                                                         URL incomingURL) throws FaultableException {
        String outgoingReourceId = method.getResourceIdRequested();
        if (outgoingReourceId == null || outgoingReourceId.length() < 1) {
            // look for pattern for service id in the incoming URL
            String oid = getOIDFromURL(incomingURL);
            if (oid == null || oid.length() < 1) {
                throw new FaultableException("ResourceId unspecified");
            }
            outgoingReourceId = incomingURL.getProtocol() + ":" + "//" + incomingURL.getHost() + ":" + incomingURL.getPort() + "/serviceoid=" + oid;

        }
        String serviceid = determineServiceFromUrl(outgoingReourceId);
        String metricsServiceAddress = incomingURL.getProtocol() + ":" + "//" + incomingURL.getHost() + ":" + incomingURL.getPort() + "/ssg/wsdm/QoSMetrics?serviceoid=" + serviceid;
        //http://ssg.acme.com:8080/ssg/wsdm/QoSMetrics?serviceoid=1234567
        String output = method.generateResponseDocument(metricsServiceAddress, outgoingReourceId);
        return XmlUtil.stringAsDocument(output);
    }

    private String getOIDFromURL(URL in) {
        String url = in.toString();
        return determineServiceFromUrl(url);
    }

    private String determineServiceFromUrl(String url) {
        Matcher matcher = oidPattern.matcher(url);
        if (matcher.find() && matcher.groupCount() == 1) {
            return matcher.group(1);
        }

        return null;
    }*/

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (!(event instanceof EntityInvalidationEvent)) return;
        try {
            EntityInvalidationEvent eie = (EntityInvalidationEvent)event;
            if (!PublishedService.class.isAssignableFrom(eie.getEntityClass())) return;

            for (int i = 0; i < eie.getEntityIds().length; i++) {
                long oid = eie.getEntityIds()[i];
                char op = eie.getEntityOperations()[i];
                switch (op) {
                    case EntityInvalidationEvent.CREATE:
                        for (ServiceStateMonitor ssm : serviceStateMonitors) {
                            ssm.onServiceCreated(oid);
                        }
                        break;
                    case EntityInvalidationEvent.UPDATE:
                        if (eie instanceof ServiceEnablementEvent) {
                            ServiceEnablementEvent see = (ServiceEnablementEvent)eie;
                            for (ServiceStateMonitor ssm : serviceStateMonitors) {
                                if (see.isEnabled())
                                    ssm.onServiceEnabled(oid);
                                else
                                    ssm.onServiceDisabled(oid);
                            }
                        } // else other kinds of updates are irrelevant to metrics
                        break;
                    case EntityInvalidationEvent.DELETE:
                        for (ServiceStateMonitor ssm : serviceStateMonitors) {
                            ssm.onServiceDeleted(oid);
                        }
                        break;
                }
            }
        } catch (Throwable e) {
            logger.log(Level.SEVERE, "Unexpected error handling application event", e);
        }
    }

}
