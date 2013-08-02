package com.l7tech.server.hpsoam;

import com.l7tech.objectmodel.Goid;
import com.l7tech.util.InetAddressUtil;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.util.ISO8601Date;
import com.l7tech.util.DomUtils;
import com.l7tech.util.IOUtils;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.service.PublishedService;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.servlet.http.HttpServletRequest;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The Service Managed Object
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Sep 19, 2007<br/>
 */
public class ServiceManagedObject {
    private final Logger logger = Logger.getLogger(ServiceManagedObject.class.getName());
    private PublishedService service;
    private long creation;
    private long bogusPushEventNr = 0;


    public ServiceManagedObject(PublishedService service) {
        this.service = service;
        creation = System.currentTimeMillis();
    }

    public Goid getServiceID() {
        return service.getGoid();
    }

    public PublishedService getPublishedService() {
        return service;
    }

    /**
     * Provided for use when properties of the published service have changed.
     */
    public void setPublishedService(PublishedService service) {
        this.service = service;
    }

    public String respondTo(WSMFService.RequestContext context) {
        WSMFMethod methodInvoked = context.method;

        if (methodInvoked == WSMFMethod.GET_TYPE) {
            return getGetTypeResponse();
        } else if (methodInvoked == WSMFMethod.GET_MANAGED_HOST_NAME) {
            return getGetManagedObjectHostNameResponse(context);
        } else if (methodInvoked == WSMFMethod.GET_MANAGEMENT_WSDL_URL) {
            return getManagementWSDLURL(context);
        } else if (methodInvoked == WSMFMethod.GET_MANAGED_OBJ_VERSION) {
            return getManagedObjVersion();
        } else if (methodInvoked == WSMFMethod.GET_RESOURCE_VERSION) {
            return getResourceVersion();
        } else if (methodInvoked == WSMFMethod.GET_NAME) {
            return getName();
        } else if (methodInvoked == WSMFMethod.GET_VENDOR) {
            return getVendor();
        } else if (methodInvoked == WSMFMethod.GET_SPEC_RELATIONSHIPS) {
            return getSpecRelationships();
        } else if (methodInvoked == WSMFMethod.GET_RESOURCE_HOSTNAME) {
            return getResHostname(context);
        } else if (methodInvoked == WSMFMethod.GET_STATUS) {
            return getStatus();
        } else if (methodInvoked == WSMFMethod.GET_DESCRIPTION) {
            return getDescription();
        } else if (methodInvoked == WSMFMethod.GET_CREATEDON) {
            return getCreatedOn();
        } else if (methodInvoked == WSMFMethod.GET_RELATIONSHIPS) {
            return getRelationships(context);
        } else if (methodInvoked == WSMFMethod.GET_LOGLEVEL) {
            return getLogLevel();
        } else if (methodInvoked == WSMFMethod.GET_PUSHSUBSCRIBE) {
            return pushSubscribe();
        } else if (methodInvoked == WSMFMethod.GET_SUPPEVENTTYPES) {
            return getSupportedEventTypes();
        } else if (methodInvoked == WSMFMethod.GET) {
            return getGet(context);
        } else if (methodInvoked == WSMFMethod.GET_OP_WSDLURL) {
            return getOperationalWsdlURL(context);
        }

        logger.severe("Method not implemented in service MO " + methodInvoked);
        throw new RuntimeException("Missing handling for " + methodInvoked);
    }

    private String getOperationalWsdlURL(WSMFService.RequestContext context) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                "    <soapenv:Body xmlns:ns1=\"http://schemas.hp.com/wsmf/2003/03/Service\">\n" +
                "       <ns1:GetOperationalWsdlUrlResponse>" + generateContainedWsdlUrl(context.req) + "</ns1:GetOperationalWsdlUrlResponse>\n" +
                "    </soapenv:Body>\n" +
                "</soapenv:Envelope>";
    }

    private String getGet(WSMFService.RequestContext context) {
        logger.severe("unexpected call on the service mo " + context.payload);
        return null;
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

    private String getManagementWSDLURL(WSMFService.RequestContext context) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:f=\"http://schemas.hp.com/wsmf/2003/03/Foundation\">\n" +
                "    <soapenv:Body>\n" +
                "       <f:GetManagementWsdlUrlResponse>" + context.req.getScheme() + "://" + getHostFromReq(context.req) + getPortWithColonFromReq(context.req) + "/ssg/wsmf/service/" + service.getGoid() + "?wsdl</f:GetManagementWsdlUrlResponse>\n" +
                "    </soapenv:Body>\n" +
                "</soapenv:Envelope>";
    }

    private String getGetManagedObjectHostNameResponse(WSMFService.RequestContext context) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:f=\"http://schemas.hp.com/wsmf/2003/03/Foundation\">\n" +
                "    <soapenv:Body>\n" +
                "        <f:GetManagedObjectHostNameResponse>" + getHostFromReq(context.req) + "</f:GetManagedObjectHostNameResponse>\n" +
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

    private String getManagedObjVersion() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                "    <soapenv:Body xmlns:Foundation=\"http://schemas.hp.com/wsmf/2003/03/Foundation\">\n" +
                "        <Foundation:GetManagedObjectVersionResponse>" + service.getVersion() +
                "</Foundation:GetManagedObjectVersionResponse>\n" +
                "    </soapenv:Body>\n" +
                "</soapenv:Envelope>";
    }

    private String getResourceVersion() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                "    <soapenv:Body xmlns:Foundation=\"http://schemas.hp.com/wsmf/2003/03/Foundation\">\n" +
                "        <Foundation:GetResourceVersionResponse>" + service.getVersion() +
                "</Foundation:GetResourceVersionResponse>\n" +
                "    </soapenv:Body>\n" +
                "</soapenv:Envelope>";
    }

    private String getName() {
            String servicename = service.getName();
            return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                    "    <soapenv:Body xmlns:Foundation=\"http://schemas.hp.com/wsmf/2003/03/Foundation\">\n" +
                    "        <Foundation:GetNameResponse>" + servicename + "</Foundation:GetNameResponse>\n" +
                    "    </soapenv:Body>\n" +
                    "</soapenv:Envelope>";
    }

    private String getVendor() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                "    <soapenv:Body xmlns:Foundation=\"http://schemas.hp.com/wsmf/2003/03/Foundation\">\n" +
                "        <Foundation:GetVendorResponse>Layer 7 Technologies</Foundation:GetVendorResponse>\n" +
                "    </soapenv:Body>\n" +
                "</soapenv:Envelope>";
    }

    private String getSpecRelationships() {
        logger.severe("unexpected call on the servicemo");
        return null;
    }

    private String getResHostname(WSMFService.RequestContext context) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:Foundation=\"http://schemas.hp.com/wsmf/2003/03/Foundation\">\n" +
                "    <soapenv:Body>\n" +
                "        <Foundation:GetResourceHostNameResponse>" + getHostFromReq(context.req) + "</Foundation:GetResourceHostNameResponse>\n" +
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

    private String getDescription() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:Foundation=\"http://schemas.hp.com/wsmf/2003/03/Foundation\">\n" +
                "    <soapenv:Body>\n" +
                "        <Foundation:GetDescriptionResponse>Service Managed Object for " + service.getName() + "</Foundation:GetDescriptionResponse>\n" +
                "    </soapenv:Body>\n" +
                "</soapenv:Envelope>";
    }

    private String getCreatedOn() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:Foundation=\"http://schemas.hp.com/wsmf/2003/03/Foundation\">\n" +
                "    <soapenv:Body>\n" +
                "        <Foundation:GetCreatedOnResponse>" + ISO8601Date.format(new Date(creation)) + "</Foundation:GetCreatedOnResponse>\n" +
                "    </soapenv:Body>\n" +
                "</soapenv:Envelope>";
    }

    public String generateMOWsdlUrl(HttpServletRequest req) {
        return req.getScheme() + "://" + getHostFromReq(req) + getPortWithColonFromReq(req) + "/ssg/wsmf/service/" + service.getGoid() + "?wsdl=mo";
    }

    public String generateContainedWsdlUrl(HttpServletRequest req) {
        return req.getScheme() + "://" + getHostFromReq(req) + getPortWithColonFromReq(req) + "/ssg/wsmf/service/" + service.getGoid() + "?wsdl=contained";
    }

    public static String generateContainedWsdlUrl(HttpServletRequest req, String svcid) {
        return req.getScheme() + "://" + getHostFromReq(req) + getPortWithColonFromReq(req) + "/ssg/wsmf/service/" + svcid + "?wsdl=contained";
    }

    private String getRelationships(WSMFService.RequestContext context) {
        StringBuffer out = new StringBuffer();
        out.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                   "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                   "    <soapenv:Body xmlns:Foundation=\"http://schemas.hp.com/wsmf/2003/03/Foundation\">\n" +
                   "        <Foundation:GetRelationshipsResponse>\n" +
                   "            <Foundation:RelationshipList>\n");

        out.append("                <Foundation:Relationship>\n");
        out.append("                    <Foundation:HowRelated>http://schemas.hp.com/wsmf/2003/03/Relations/ContainedIn</Foundation:HowRelated>\n");
        out.append("                    <Foundation:RelatedObject>" + WSMFService.managementWSDLURL(context.req) + "</Foundation:RelatedObject>\n");
        out.append("                </Foundation:Relationship>\n");

        out.append("                <Foundation:Relationship>\n");
        out.append("                    <Foundation:HowRelated>http://schemas.hp.com/wsmf/2003/03/Relations/Contains</Foundation:HowRelated>\n");
        out.append("                    <Foundation:RelatedObject>" + generateContainedWsdlUrl(context.req) + "</Foundation:RelatedObject>\n");
        out.append("                </Foundation:Relationship>\n");

        out.append("            </Foundation:RelationshipList>\n" +
                "        </Foundation:GetRelationshipsResponse>\n" +
                "    </soapenv:Body>\n" +
                "</soapenv:Envelope>");
        return out.toString();
    }

    public String handleMOSpecificGET(String fullURL, HttpServletRequest req) {
        if (fullURL.contains("wsdl=contained")) {
            URL ssgurl;
            String routinguri = service.getRoutingUri();            
            String proto = "http";
            if (req.isSecure()) proto = "https";
            int port = req.getServerPort();
            String portStr = "";
            if (!("https".equals(proto) && port == 443) &&
                !("http".equals(proto) && port == 80)) {
                portStr = ":" + port;
            }
            try {
                if (routinguri == null || routinguri.length() < 1) {

                    ssgurl = new URL(proto + "://" + InetAddressUtil.getHostForUrl(req.getServerName()) +
                                     portStr + SecureSpanConstants.SERVICE_FILE +
                                     Goid.toString(service.getGoid()));
                } else {
                    ssgurl = new URL(proto + "://" + InetAddressUtil.getHostForUrl(req.getServerName()) +
                                     portStr + routinguri);
                }
                Document wsdlDoc = XmlUtil.stringToDocument(service.getWsdlXml());
                substituteSoapAddressURL(wsdlDoc, ssgurl);
                return XmlUtil.nodeToFormattedString(wsdlDoc);
            } catch (Exception e) {
                logger.log(Level.WARNING, "cannot compose endpoint target for wsdl", e);
                return service.getWsdlXml();
            }
        } else if (fullURL.contains("wsdl=mo")) {
            try {
                return produceServiceMOWSDL(req);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Cannot produce servicemo wsdl", e);
                return null;
            }
        } else {
            logger.severe("Unexpected GET on service mo " + fullURL);
            return null;
        }
    }

    private String produceServiceMOWSDL(HttpServletRequest req) throws IOException {
        InputStream is = getInputStreamFromCP("ServiceMO.wsdl");
        String beforeEdits = new String( IOUtils.slurpStream(is));
        beforeEdits = beforeEdits.replace("^^^INSERT_METHOD_HOST_PORT_HERE^^^", getHostPort(req));
        beforeEdits = beforeEdits.replace("^^^INSERT_TARGET_HERE^^^", req.getScheme() + "://" + getHostFromReq(req) + getPortWithColonFromReq(req) + "/ssg/wsmf/service/" + service.getGoid());
        beforeEdits = beforeEdits.replace("^^^INSERT_NAME_HERE^^^", service.getName() + " Managed Object");
        return beforeEdits;
    }

    private static InputStream getInputStreamFromCP(String resourceToRead) throws FileNotFoundException {
        ClassLoader cl = WSMFServlet.class.getClassLoader();
        String pathToLoad = "com/l7tech/server/hpsoam/resources/" + resourceToRead;
        InputStream i = cl.getResourceAsStream(pathToLoad);
        if (i == null) {
            throw new FileNotFoundException(pathToLoad);
        }
        return i;
    }

    private String getHostPort(HttpServletRequest req) {
        String output = req.getRequestURL().toString();
        int indexofthing = output.indexOf("//");
        int end = output.indexOf("/", indexofthing+2);
        return output.substring(0, end);
    }

    private void substituteSoapAddressURL(Document wsdl, URL newURL) {
        // todo, this was stolen from WsdlProxyServlet. we may want to move that logic in some sort of wsdlutil.

        // get http://schemas.xmlsoap.org/wsdl/ 'port' element
        NodeList portlist = wsdl.getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/", "port");
        for (int i = 0; i < portlist.getLength(); i++) {
            Element portel = (Element)portlist.item(i);
            // get child http://schemas.xmlsoap.org/wsdl/soap/ 'address'
            List addresses = DomUtils.findChildElementsByName(portel, "http://schemas.xmlsoap.org/wsdl/soap/", "address");
            // change the location attribute with new URL
            for (Object address1 : addresses) {
                Element address = (Element) address1;
                address.setAttribute("location", newURL.toString());
            }

            // and for soap12 (this is better than just leaving the protected service url in there)
            List addressesToRemove = DomUtils.findChildElementsByName(portel, "http://schemas.xmlsoap.org/wsdl/soap12/", "address");
            for (Object anAddressesToRemove : addressesToRemove) {
                Element address = (Element) anAddressesToRemove;
                address.setAttribute("location", newURL.toString());
            }
        }
    }
}
