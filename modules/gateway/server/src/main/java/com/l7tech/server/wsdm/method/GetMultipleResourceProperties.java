package com.l7tech.server.wsdm.method;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.server.wsdm.faults.FaultMappableException;
import com.l7tech.server.wsdm.faults.GenericWSRFExceptionFault;
import com.l7tech.server.wsdm.faults.InvalidResourcePropertyQNameFault;
import com.l7tech.util.InvalidDocumentFormatException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstraction for wsrf-rp:GetMultipleResourceProperties
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Nov 2, 2007<br/>
 */
@SuppressWarnings({"ValidExternallyBoundObject", "NonJaxWsWebServices"})
public class GetMultipleResourceProperties extends ESMMethod {
    private ArrayList<ResourceProperty> requestedProperties = new ArrayList<ResourceProperty>();
    private Element getMultipleResourcePropertiesEl;
    private boolean isReallyMultiResource;

    private GetMultipleResourceProperties(Element rootEl) throws InvalidResourcePropertyQNameFault {
        getMultipleResourcePropertiesEl = rootEl;
        isReallyMultiResource = inspectForMultiResource();
        // sometimes, the request has the pattern
        //<soap:Body>
        //    <wsrf-rp:GetMultipleResourceProperties>
        //        muws1:ManageabilityCapability // or whatever
        //    </wsrf-rp:GetMultipleResourceProperties>
        //</soap:Body>
        if (!isReallyMultiResource) {
            String tmp = XmlUtil.getTextValue(getMultipleResourcePropertiesEl);
            ResourceProperty rp = ResourceProperty.fromValue(tmp);
            requestedProperties.add(rp);
        } else {
        // in other cases, the request has the pattern
        //<soap:Body>
        //    <wsrf-rp:GetMultipleResourceProperties>
        //      <wsrf-rp:ResourceProperty>
        //        muws1:ResourceId
        //      </wsrf-rp:ResourceProperty>
        //      <wsrf-rp:ResourceProperty>
        //        muws2:CurrentTime
        //      </wsrf-rp:ResourceProperty>
        //      <wsrf-rp:ResourceProperty>
        //        muws2:OperationalStatus
        //      </wsrf-rp:ResourceProperty>
        //      <wsrf-rp:ResourceProperty>
        //        mows:NumberOfRequests
        //      </wsrf-rp:ResourceProperty>
        //      <wsrf-rp:ResourceProperty>
        //        mows:NumberOfFailedRequests
        //      </wsrf-rp:ResourceProperty>
        //    </wsrf-rp:GetMultipleResourceProperties>
        //  </soap:Body>
            NodeList children = getMultipleResourcePropertiesEl.getChildNodes();
            for ( int i = 0; i < children.getLength(); i++ ) {
                Node n = children.item(i);
                if (n.getNodeType() == Node.ELEMENT_NODE) {
                    Element el = (Element)n;
                    if (el.getLocalName() != null && el.getLocalName().equals("ResourceProperty")) {
                        String value = XmlUtil.getTextValue(el);
                        ResourceProperty rp = ResourceProperty.fromValue(value);
                        if (!requestedProperties.contains(rp)) {
                            requestedProperties.add(rp);
                        }
                    }
                }
            }
        }
    }

    public List<ResourceProperty> getRequestedProperties() {
        return requestedProperties;
    }

    public static GetMultipleResourceProperties resolve(Document doc) throws FaultMappableException {
        Element bodychild;
        try {
            bodychild = getFirstBodyChild(doc);
        } catch (InvalidDocumentFormatException e) {
            throw new GenericWSRFExceptionFault("Unable to retrieve Body element");
        }
        if (bodychild == null) return null;
        if (testElementLocalName(bodychild, "GetMultipleResourceProperties")) {
            return new GetMultipleResourceProperties(bodychild);
        }
        return null;
    }

    private boolean inspectForMultiResource() {
        NodeList children = getMultipleResourcePropertiesEl.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (n instanceof Element) {
                Element el = (Element)n;
                if (el.getLocalName().equals("ResourceProperty")) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isReallyMultiResource() {
        return isReallyMultiResource;
    }
}
