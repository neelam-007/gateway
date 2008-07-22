package com.l7tech.server.hpsoam;

import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.util.DomUtils;
import com.l7tech.util.InvalidDocumentFormatException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A method of the WSMF standard
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Aug 1, 2007<br/>
 */
public class WSMFMethod {
    public static final WSMFMethod GET_TYPE                         = new WSMFMethod(100, "GetType");
    public static final WSMFMethod GET_MANAGED_HOST_NAME            = new WSMFMethod(101, "GetManagedObjectHostName");
    public static final WSMFMethod GET_MANAGEMENT_WSDL_URL          = new WSMFMethod(102, "GetManagementWsdlUrl");
    public static final WSMFMethod GET_MANAGED_OBJ_VERSION          = new WSMFMethod(103, "GetManagedObjectVersion");
    public static final WSMFMethod GET_RESOURCE_VERSION             = new WSMFMethod(104, "GetResourceVersion");
    public static final WSMFMethod GET_NAME                         = new WSMFMethod(105, "GetName");
    public static final WSMFMethod GET_VENDOR                       = new WSMFMethod(106, "GetVendor");
    public static final WSMFMethod GET_SPEC_RELATIONSHIPS           = new WSMFMethod(107, "GetSpecificRelationships");
    public static final WSMFMethod GET_RESOURCE_HOSTNAME            = new WSMFMethod(108, "GetResourceHostName");
    public static final WSMFMethod GET_STATUS                       = new WSMFMethod(109, "GetStatus");
    public static final WSMFMethod GET_DESCRIPTION                  = new WSMFMethod(110, "GetDescription");
    public static final WSMFMethod GET_CREATEDON                    = new WSMFMethod(111, "GetCreatedOn");
    public static final WSMFMethod GET_RELATIONSHIPS                = new WSMFMethod(112, "GetRelationships");
    public static final WSMFMethod GET_LOGLEVEL                     = new WSMFMethod(113, "getLogLevel");
    public static final WSMFMethod GET_PUSHSUBSCRIBE                = new WSMFMethod(114, "PushSubscribe");
    public static final WSMFMethod GET_SUPPEVENTTYPES               = new WSMFMethod(115, "GetSupportedEventTypes");
    public static final WSMFMethod GET                              = new WSMFMethod(116, "Get");
    public static final WSMFMethod GET_OP_WSDLURL                   = new WSMFMethod(117, "GetOperationalWsdlUrl");
    public static final WSMFMethod GET_CANCEL_SUBSCRIPTION          = new WSMFMethod(118, "CancelSubscription");
    public static final WSMFMethod GET_TRAILINGLOGS                 = new WSMFMethod(119, "getTrailingLogMessages");
    public static final WSMFMethod SET_LOGLEVEL                     = new WSMFMethod(120, "setLogLevel");
    // setLogLevelForCategory?

    public static final String FOUNDATION_NS   = WSMFService.FOUNDATION_NS;
    public static final String OPENVIEWMIP_NS  = "http://openview.hp.com/xmlns/mip/2005/03/Wsee";
    public static final String WSMF_EVENTS_NS  = "http://schemas.hp.com/wsmf/2003/03/Events#";
    public static final String WSMF_SERVICE_NS = "http://schemas.hp.com/wsmf/2003/03/Service";

    private final int id;
    private final String name;

    private WSMFMethod(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public static WSMFMethod fromXML(Document xml) throws InvalidDocumentFormatException, UnsupportedMethodException {
        Element bodyChild = DomUtils.findFirstChildElement( SoapUtil.getBodyElement(xml));
        String bodyNS = bodyChild.getNamespaceURI();
        String nodeName = bodyChild.getLocalName();
        if (bodyNS == null) {
            throw new InvalidDocumentFormatException("The body child doesn't have a ns");
        }

        if (bodyNS.equals(FOUNDATION_NS) && nodeName.equals(GET_MANAGEMENT_WSDL_URL.getName())) {
            return GET_MANAGEMENT_WSDL_URL;
        } else if (bodyNS.equals(FOUNDATION_NS) && nodeName.equals(GET_TYPE.getName())) {
            return GET_TYPE;
        } else if (bodyNS.equals(FOUNDATION_NS) && nodeName.equals(GET_MANAGED_HOST_NAME.getName())) {
            return GET_MANAGED_HOST_NAME;
        } else if (bodyNS.equals(FOUNDATION_NS) && nodeName.equals(GET_MANAGED_OBJ_VERSION.getName())) {
            return GET_MANAGED_OBJ_VERSION;
        } else if (bodyNS.equals(FOUNDATION_NS) && nodeName.equals(GET_RESOURCE_VERSION.getName())) {
            return GET_RESOURCE_VERSION;
        } else if (bodyNS.equals(FOUNDATION_NS) && nodeName.equals(GET_NAME.getName())) {
            return GET_NAME;
        } else if (bodyNS.equals(FOUNDATION_NS) && nodeName.equals(GET_VENDOR.getName())) {
            return GET_VENDOR;
        } else if (bodyNS.equals(FOUNDATION_NS) && nodeName.equals(GET_SPEC_RELATIONSHIPS.getName())) {
            return GET_SPEC_RELATIONSHIPS;
        } else if (bodyNS.equals(FOUNDATION_NS) && nodeName.equals(GET_RESOURCE_HOSTNAME.getName())) {
            return GET_RESOURCE_HOSTNAME;
        } else if (bodyNS.equals(FOUNDATION_NS) && nodeName.equals(GET_STATUS.getName())) {
            return GET_STATUS;
        } else if (bodyNS.equals(FOUNDATION_NS) && nodeName.equals(GET_DESCRIPTION.getName())) {
            return GET_DESCRIPTION;
        } else if (bodyNS.equals(FOUNDATION_NS) && nodeName.equals(GET_CREATEDON.getName())) {
            return GET_CREATEDON;
        } else if (bodyNS.equals(FOUNDATION_NS) && nodeName.equals(GET_RELATIONSHIPS.getName())) {
            return GET_RELATIONSHIPS;
        } else if (bodyNS.equals(OPENVIEWMIP_NS) && nodeName.equals(GET_LOGLEVEL.getName())) {
            return GET_LOGLEVEL;
        } else if (bodyNS.equals(WSMF_EVENTS_NS) && nodeName.equals(GET_PUSHSUBSCRIBE.getName())) {
            return GET_PUSHSUBSCRIBE;
        } else if (bodyNS.equals(WSMF_EVENTS_NS) && nodeName.equals(GET_SUPPEVENTTYPES.getName())) {
            return GET_SUPPEVENTTYPES;
        } else if (bodyNS.equals(OPENVIEWMIP_NS) && nodeName.equals(GET.getName())) {
            return GET;
        } else if (bodyNS.equals(WSMF_SERVICE_NS) && nodeName.equals(GET_OP_WSDLURL.getName())) {
            return GET_OP_WSDLURL;
        } else if (bodyNS.equals(WSMF_EVENTS_NS) && nodeName.equals(GET_CANCEL_SUBSCRIPTION.getName())) {
            return GET_CANCEL_SUBSCRIPTION;
        } else if (bodyNS.equals(OPENVIEWMIP_NS) && nodeName.equals(GET_TRAILINGLOGS.getName())) {
            return GET_TRAILINGLOGS;
        } else if (bodyNS.equals(OPENVIEWMIP_NS) && nodeName.equals(SET_LOGLEVEL.getName())) {
            return SET_LOGLEVEL;
        }

        throw new UnsupportedMethodException("Unsupported or unknown method: " + bodyNS + ":" + nodeName);
    }

    public static class UnsupportedMethodException extends Exception {
        public UnsupportedMethodException(String msg) {
            super(msg);
        }
    }

    public String toString() {
        return "WSMFMethod " + name + " [" + id + "]";
    }
}
