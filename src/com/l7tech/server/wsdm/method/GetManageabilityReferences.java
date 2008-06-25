package com.l7tech.server.wsdm.method;

import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.server.wsdm.Namespaces;
import com.l7tech.server.wsdm.faults.FaultMappableException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Abstraction of mows-xs:GetManageabilityReferences
 * <p/>
 * @deprecated
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Nov 2, 2007<br/>
 */
public class GetManageabilityReferences extends ESMMethod {
    private String resourceIdRequested;

    private GetManageabilityReferences(Document doc) throws FaultMappableException {
        try {
            // look for the presence of an incoming Header/ResourceId
            Element headerEl = SoapUtil.getHeaderElement(doc);
            if (headerEl == null) return;

            Element resId = XmlUtil.findOnlyOneChildElementByName(headerEl, Namespaces.MUWS_P1_XS, "ResourceId");
            if (resId != null) {
                resourceIdRequested = XmlUtil.getTextValue(resId);
            }
        } catch (Exception e) {
            throw new FaultMappableException(e.getMessage());
        }
    }

    public String getResourceIdRequested() {
        return resourceIdRequested;
    }

    public static GetManageabilityReferences resolve(Document doc) throws FaultMappableException {
        try {
            Element bodychild = getFirstBodyChild(doc);
            if (bodychild == null) return null;
            if (testElementLocalName(bodychild, "GetManageabilityReferences")) {
                return new GetManageabilityReferences(doc);
            }
            return null;
        } catch (Exception e) {
            throw new FaultMappableException(e.getMessage());
        }
    }

    public String generateResponseDocument(String refAddressVal, String refResIdVal) {
        return
        "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:wsa=\"http://www.w3.org/2005/08/addressing\">\n" +
        "  <soap:Header>\n" +
        "    <wsa:Action>\n" +
        "      http://docs.oasis-open.org/wsdm/mows/GetManageabilityReferencesResponse\n" +
        "    </wsa:Action>\n" +
        "  </soap:Header>\n" +
        "  <soap:Body>\n" +
        "    <mows-xs:GetManageabilityReferencesResponse xmlns:mows-xs=\"http://docs.oasis-open.org/wsdm/2004/12/mows/wsdm-mows.xsd\">\n" +
        "      <muws-p1-xs:ManageabilityEndpointReference xmlns:muws-p1-xs=\"http://docs.oasis-open.org/wsdm/2004/12/muws/wsdm-muws-part1.xsd\">\n" +
        "        <wsa:EndpointReference>\n" +
        "          <wsa:Address>" +
                    refAddressVal +
        "          </wsa:Address>\n" +
        "          <wsa:ReferenceParameters>\n" +
        "            <muws-p1-xs:ResourceId>" +
                    refResIdVal +
        "            </muws-p1-xs:ResourceId>\n" +
        "          </wsa:ReferenceParameters>\n" +
        "        </wsa:EndpointReference>\n" +
        "      </muws-p1-xs:ManageabilityEndpointReference>\n" +
        "    </mows-xs:GetManageabilityReferencesResponse>\n" +
        "  </soap:Body>\n" +
        "</soap:Envelope>";
    }
}
