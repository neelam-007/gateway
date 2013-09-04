package com.l7tech.server.hpsoam;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.*;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.service.ServiceManager;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.servlet.http.HttpServletRequest;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implements the WSMF WSDL logic. This is invoked by the WSMFServlet to answer the HPSOAM NS.
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jul 17, 2007<br/>
 *
 */
public class WSMFService implements ApplicationContextAware {
    private final Logger logger = Logger.getLogger(WSMFService.class.getName());
    public static final String FOUNDATION_NS = "http://schemas.hp.com/wsmf/2003/03/Foundation";

    /** Name of cluster property that enables/disables integration with HP SOA Manager. */
    public static final String HPSOAM_ENABLED = "hpsoamEnabled";

    /** Name of cluster property that determines whether SSL is required for Agent access. */
    public static final String HPSOAM_REQUIRE_SSL = "hpsoamRequireSSL";

    /** Name of cluster property that determines whether credentials of Admintrators/Operators are required for Agent access. */
    public static final String HPSOAM_REQUIRE_CREDENTIALS = "hpsoamRequireCreds";

    private ContainerManagedObject containerMO;
    private ServiceManager serviceManager;
    private ApplicationContext applicationContext;
    private long bogusPushEventNr = 0;
    static final Pattern serviceoidPattern = Pattern.compile(".*/service/([0-9a-fA-F]{32}|\\d{1,20}).*");

    public static boolean isEnabled() {
        return ConfigFactory.getBooleanProperty( HPSOAM_ENABLED, false );
    }

    public static boolean isSSLRequired() {
        return ConfigFactory.getBooleanProperty( HPSOAM_REQUIRE_SSL, true );
    }

    public static boolean isCredentialsRequired() {
        return ConfigFactory.getBooleanProperty( HPSOAM_REQUIRE_CREDENTIALS, true );
    }

    public WSMFService(ServiceManager serviceManager, MessageProcessor messageProcessor) {
        this.serviceManager = serviceManager;
        this.containerMO = new ContainerManagedObject(serviceManager, messageProcessor);
    }

    public class RequestContext {
        public Goid serviceid;
        public String payload;
        public Document payloadXml;
        public String url;
        public HttpServletRequest req;
        public WSMFMethod method;
    }

    public String respondTo(String requestPayload, HttpServletRequest req) throws SAXException, InvalidDocumentFormatException {
        Document payloadXML = XmlUtil.stringToDocument(requestPayload);
        WSMFMethod methodInvoked;
        try {
            methodInvoked = WSMFMethod.fromXML(payloadXML);
        } catch (WSMFMethod.UnsupportedMethodException e) {
            logger.log(Level.WARNING, "Unsupported operation", e);
            return null;
        }

        // Create a request context
        RequestContext context = new RequestContext();
        context.method = methodInvoked;
        context.payload = requestPayload;
        context.req = req;
        context.payloadXml = payloadXML;
        context.url = WSMFServlet.getFullURL(req);
        Matcher matcher = serviceoidPattern.matcher(context.url);
        if (matcher.matches()) {
            context.serviceid = GoidUpgradeMapper.mapId(EntityType.SERVICE, matcher.group(1));
        }

        if (context.serviceid != null) {
            return containerMO.respondTo(context);
        }

        if (methodInvoked == WSMFMethod.GET_TYPE) {
            return getGetTypeResponse();
        } else if (methodInvoked == WSMFMethod.GET_MANAGED_HOST_NAME) {
            return getGetManagedObjectHostNameResponse(req);
        } else if (methodInvoked == WSMFMethod.GET_MANAGEMENT_WSDL_URL) {
            return getManagementWSDLURL(req);
        } else if (methodInvoked == WSMFMethod.GET_MANAGED_OBJ_VERSION) {
            return getManagedObjVersion();
        } else if (methodInvoked == WSMFMethod.GET_RESOURCE_VERSION) {
            return getResourceVersion();
        } else if (methodInvoked == WSMFMethod.GET_NAME) {
            return getName();
        } else if (methodInvoked == WSMFMethod.GET_VENDOR) {
            return getVendor();
        } else if (methodInvoked == WSMFMethod.GET_SPEC_RELATIONSHIPS) {
            return getSpecRelationships(req);
        } else if (methodInvoked == WSMFMethod.GET_RESOURCE_HOSTNAME) {
            return getResHostname(req);
        } else if (methodInvoked == WSMFMethod.GET_STATUS) {
            return getStatus();
        } else if (methodInvoked == WSMFMethod.GET_DESCRIPTION) {
            return getDescription(req);
        } else if (methodInvoked == WSMFMethod.GET_CREATEDON) {
            return getCreatedOn();
        } else if (methodInvoked == WSMFMethod.GET_RELATIONSHIPS) {
            return getRelationships(req);
        } else if (methodInvoked == WSMFMethod.GET_LOGLEVEL) {
            return getLogLevel();
        } else if (methodInvoked == WSMFMethod.GET_PUSHSUBSCRIBE) {
            return pushSubscribe();
        } else if (methodInvoked == WSMFMethod.GET_SUPPEVENTTYPES) {
            return getSupportedEventTypes();
        } else if (methodInvoked == WSMFMethod.GET) {
            return containerMO.handlePerformanceWindowRequest(context);
        } else if (methodInvoked == WSMFMethod.GET_CANCEL_SUBSCRIPTION) {
            return cancelSubscription();
        }

        logger.severe("Method not supported in container mo " + methodInvoked);
        throw new RuntimeException("Missing handling for " + methodInvoked);
    }

    private String getSupportedEventTypes() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
               "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:Events=\"http://schemas.hp.com/wsmf/2003/03/Events#\">\n" +
               "    <soapenv:Body>\n" +
               "        <Events:GetSupportedEventTypesResponse>\n" +
               "            <Events:EventTypeList>\n" +
               "                <Events:EventType>http://schemas.hp.com/wsmf/2003/03/Foundation/Event/RelationshipsChanged</Events:EventType>\n" +
               "                <Events:EventType>http://schemas.hp.com/mip/2004/WsExecutionEnvironment/Event/MessageTraceNotification</Events:EventType>\n" +
               "            </Events:EventTypeList>\n" +
               "        </Events:GetSupportedEventTypesResponse>\n" +
               "    </soapenv:Body>\n" +
               "</soapenv:Envelope>";
    }

    private String cancelSubscription() {
        // todo, for real
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:Events=\"http://schemas.hp.com/wsmf/2003/03/Events#\">\n" +
                "    <soapenv:Body>\n" +
                "        <Events:CancelSubscriptionResponse/>\n" +
                "    </soapenv:Body>\n" +
                "</soapenv:Envelope>";
    }

    private String pushSubscribe() {
        // todo, for real
        // ignore push events for now
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:Events=\"http://schemas.hp.com/wsmf/2003/03/Events#\">\n" +
                "    <soapenv:Body>\n" +
                "        <Events:SubscribeResponse>\n" +
                "            <Events:Result>" + getBogusSubscribeResponseURN() + "</Events:Result>\n" +
                "        </Events:SubscribeResponse>\n" +
                "    </soapenv:Body>\n" +
                "</soapenv:Envelope>";
    }

    private String getBogusSubscribeResponseURN() {
        ++bogusPushEventNr;
        return "urn:subscription-push-" + bogusPushEventNr;
    }

    private String getLogLevel() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:Wsee=\"http://openview.hp.com/xmlns/mip/2005/03/Wsee\">\n" +
                "    <soapenv:Body>\n" +
                "        <Wsee:getLogLevelResponse>INFO</Wsee:getLogLevelResponse>\n" +
                "    </soapenv:Body>\n" +
                "</soapenv:Envelope>";
    }

    private String getRelationships(HttpServletRequest req) {
        StringBuffer out = new StringBuffer();
        out.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                   "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                   "    <soapenv:Body xmlns:Foundation=\"http://schemas.hp.com/wsmf/2003/03/Foundation\">\n" +
                   "        <Foundation:GetRelationshipsResponse>\n" +
                   "            <Foundation:RelationshipList>\n");
        for (ServiceManagedObject service : containerMO.getServiceMOs()) {
            // hpsoam doesn't care about services that dont have a WSDL
            out.append("                <Foundation:Relationship>\n");
            out.append("                    <Foundation:HowRelated>http://schemas.hp.com/wsmf/2003/03/Relations/Contains</Foundation:HowRelated>\n");
            out.append("                    <Foundation:RelatedObject>" + service.generateMOWsdlUrl(req) + "</Foundation:RelatedObject>\n");
            out.append("                </Foundation:Relationship>\n");
        }
        out.append("            </Foundation:RelationshipList>\n" +
                "        </Foundation:GetRelationshipsResponse>\n" +
                "    </soapenv:Body>\n" +
                "</soapenv:Envelope>");
        return out.toString();
    }

    private String getCreatedOn() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:Foundation=\"http://schemas.hp.com/wsmf/2003/03/Foundation\">\n" +
                "    <soapenv:Body>\n" +
                "        <Foundation:GetCreatedOnResponse>" + ISO8601Date.format(new Date(Long.parseLong(containerMO.getVersion()))) + "</Foundation:GetCreatedOnResponse>\n" +
                "    </soapenv:Body>\n" +
                "</soapenv:Envelope>";
    }

    private String getDescription(HttpServletRequest req) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:Foundation=\"http://schemas.hp.com/wsmf/2003/03/Foundation\">\n" +
                "    <soapenv:Body>\n" +
                "        <Foundation:GetDescriptionResponse>SecureSpan Gateway internal WSMF agent @ " + getHostFromReq(req) + "</Foundation:GetDescriptionResponse>\n" +
                "    </soapenv:Body>\n" +
                "</soapenv:Envelope>";
    }

    private String getStatus() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:Foundation=\"http://schemas.hp.com/wsmf/2003/03/Foundation\">\n" +
                "    <soapenv:Body>\n" +
                "        <Foundation:GetStatusResponse>http://schemas.hp.com/wsmf/2003/03/Foundation/Status/Operational</Foundation:GetStatusResponse>\n" +
                "    </soapenv:Body>\n" +
                "</soapenv:Envelope>";
    }

    private String getResHostname(HttpServletRequest req) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:Foundation=\"http://schemas.hp.com/wsmf/2003/03/Foundation\">\n" +
                "    <soapenv:Body>\n" +
                "        <Foundation:GetResourceHostNameResponse>" + getHostFromReq(req) + "</Foundation:GetResourceHostNameResponse>\n" +
                "    </soapenv:Body>\n" +
                "</soapenv:Envelope>";
    }

    private String getSpecRelationships(HttpServletRequest req) {
        StringBuffer out = new StringBuffer();
        out.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                   "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                   "    <soapenv:Body xmlns:Foundation=\"http://schemas.hp.com/wsmf/2003/03/Foundation\">\n" +
                   "        <Foundation:GetSpecificRelationshipsResponse>\n" +
                   "            <Foundation:RelationshipObjectList>\n");
        for (ServiceManagedObject service : containerMO.getServiceMOs()) {
                out.append("                <Foundation:RelationshipObject>" + service.generateMOWsdlUrl(req) + "</Foundation:RelationshipObject>\n");
        }
        out.append("            </Foundation:RelationshipObjectList>\n" +
                   "        </Foundation:GetSpecificRelationshipsResponse>\n" +
                   "    </soapenv:Body>\n" +
                   "</soapenv:Envelope>");
        return out.toString();
    }

    private String getVendor() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                "    <soapenv:Body xmlns:Foundation=\"http://schemas.hp.com/wsmf/2003/03/Foundation\">\n" +
                "        <Foundation:GetVendorResponse>Layer 7 Technologies</Foundation:GetVendorResponse>\n" +
                "    </soapenv:Body>\n" +
                "</soapenv:Envelope>";
    }

    private String getName() {
            return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                    "    <soapenv:Body xmlns:Foundation=\"http://schemas.hp.com/wsmf/2003/03/Foundation\">\n" +
                    "        <Foundation:GetNameResponse>SecureSpan Gateway</Foundation:GetNameResponse>\n" +
                    "    </soapenv:Body>\n" +
                    "</soapenv:Envelope>";
    }

    private String getResourceVersion() {
            return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                    "    <soapenv:Body xmlns:Foundation=\"http://schemas.hp.com/wsmf/2003/03/Foundation\">\n" +
                    "        <Foundation:GetResourceVersionResponse>" + containerMO.getResourceVersion() +
                    "</Foundation:GetResourceVersionResponse>\n" +
                    "    </soapenv:Body>\n" +
                    "</soapenv:Envelope>";
    }

    private String getManagedObjVersion() {
            return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                    "    <soapenv:Body xmlns:Foundation=\"http://schemas.hp.com/wsmf/2003/03/Foundation\">\n" +
                    "        <Foundation:GetManagedObjectVersionResponse>" + containerMO.getVersion() +
                    "</Foundation:GetManagedObjectVersionResponse>\n" +
                    "    </soapenv:Body>\n" +
                    "</soapenv:Envelope>";
    }

    private static String getPortWithColonFromReq(HttpServletRequest req) {
        String res = getPortFromReq(req);
        if (res == null) return "";
        else return ":" + res;
    }

    private static String getPortFromReq(HttpServletRequest req) {
        URL url;
        try {
            url = new URL(WSMFServlet.getFullURL(req));
        } catch (MalformedURLException e) {
            // can't happen
            throw new RuntimeException("bad url " + WSMFServlet.getFullURL(req));
        }
        int output = url.getPort();
        if (output == -1) return null;
        return "" + url.getPort();
    }

    private static String getHostFromReq(HttpServletRequest req) {
        URL url;
        try {
            url = new URL(WSMFServlet.getFullURL(req));
        } catch (MalformedURLException e) {
            // can't happen
            throw new RuntimeException("bad url " + WSMFServlet.getFullURL(req));
        }
        return url.getHost();
    }

    public static String managementWSDLURL(HttpServletRequest req) {
        return req.getScheme() + "://" + getHostFromReq(req) + getPortWithColonFromReq(req) + "/ssg/wsmf/static/WseeMO.wsdl";
    }

    public String getManagementWSDLURL(HttpServletRequest req) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:f=\"http://schemas.hp.com/wsmf/2003/03/Foundation\">\n" +
                "    <soapenv:Body>\n" +
                "       <f:GetManagementWsdlUrlResponse>" + managementWSDLURL(req) + "</f:GetManagementWsdlUrlResponse>\n" +
                "    </soapenv:Body>\n" +
                "</soapenv:Envelope>";
    }

    private String getGetManagedObjectHostNameResponse(HttpServletRequest req) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:f=\"http://schemas.hp.com/wsmf/2003/03/Foundation\">\n" +
                "    <soapenv:Body>\n" +
                "        <f:GetManagedObjectHostNameResponse>" + getHostFromReq(req) + "</f:GetManagedObjectHostNameResponse>\n" +
                "    </soapenv:Body>\n" +
                "</soapenv:Envelope>";
    }


    private String getGetTypeResponse() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:f=\"http://schemas.hp.com/wsmf/2003/03/Foundation\">\n" +
                "    <soapenv:Body>\n" +
                "        <f:GetTypeResponse>http://wsm.agent.AGENT</f:GetTypeResponse>\n" +
                "    </soapenv:Body>\n" +
                "</soapenv:Envelope>";
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public String handleMOSpecificGET(String fullURL, HttpServletRequest req) {
        Matcher matcher = serviceoidPattern.matcher(fullURL);
        if (matcher.matches()) {
            return containerMO.handleMOSpecificGET(fullURL, GoidUpgradeMapper.mapId(EntityType.SERVICE, matcher.group(1)), req);
        }
        throw new RuntimeException("Cannot happen");
    }
}
