package com.l7tech.server.wsdm.method;

import com.l7tech.objectmodel.Goid;
import org.w3c.dom.Element;
import org.w3c.dom.Document;
import com.l7tech.message.Message;

import java.net.MalformedURLException;

/**
 * Abstraction for the wsrf-rp:GetResourceProperty method.
 * <p/>
 * @deprecated
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Nov 2, 2007<br/>
 */
@Deprecated
public class GetResourceProperty extends ESMMethod {
    private ResourceProperty propertyRequested;

    private GetResourceProperty(Element getResourcePropertyEl, Document doc, Message request, Goid esmServiceGoid) throws MalformedURLException {
        super(doc, request, esmServiceGoid);
        /*String val = XmlUtil.getTextValue(getResourcePropertyEl);
        if (val == null || val.length() < 1) {
            throw new InvalidDocumentFormatException("Could not find requested wsrf-rp:GetResourceProperty value");
        }
        propertyRequested = ResourceProperty.fromValue(val);
        if (propertyRequested == null) {
            throw new InvalidDocumentFormatException("Unsupported property value "+ val);
        }*/
    }

    public ResourceProperty getPropertyRequested() {
        return propertyRequested;
    }

    public String respondToTopicPropertyRequest() {
        return "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:wsa=\"http://www.w3.org/2005/08/addressing\">\n" +
                "    <soap:Header>\n" +
                "        <wsa:Action>\n" +
                "            http://docs.oasis-open.org/wsrf/rpw-2/GetResourceProperty/GetResourcePropertyResponse\n" +
                "        </wsa:Action>\n" +
                "        <wsa:RelatesTo>\n" +
                "            " + getMessageId() + "\n" +
                "        </wsa:RelatesTo>\n" +
                "    </soap:Header>\n" +
                "    <soap:Body>\n" +
                "        <wsrf-rp:GetResourcePropertyResponse\n" +
                "                xmlns:wsrf-rp=\"http://docs.oasis-open.org/wsrf/2004/06/wsrf-WS-ResourceProperties-1.2-draft-01.xsd\"\n" +
                "                xmlns:wsnt=\"http://docs.oasis-open.org/wsn/b-2\"\n" +
                "                xmlns:muws-ev=\"http://docs.oasis-open.org/wsdm/2004/12/muws/wsdm-muws-part2-events.xml\"\n" +
                "                xmlns:mowse=\"http://docs.oasis-open.org/wsdm/mowse-2.xml\">\n" +
                "            <wsnt:Topic Dialect=\"http://docs.oasis-open.org/wsn/t-1/TopicExpression/Simple\">\n" +
                "                muws-ev:OperationalStatusCapability\n" +
                "            </wsnt:Topic>\n" +
                "            <wsnt:Topic Dialect=\"http://docs.oasis-open.org/wsn/t-1/TopicExpression/Simple\">\n" +
                "                mowse:MetricsCapability\n" +
                "            </wsnt:Topic>\n" +
                "        </wsrf-rp:GetResourcePropertyResponse>\n" +
                "    </soap:Body>\n" +
                "</soap:Envelope>";
    }

    public String responseToManageabilityCapability() {
        return "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:wsa=\"http://www.w3.org/2005/08/addressing\">\n" +
                "    <soap:Header>\n" +
                "        <wsa:Action>\n" +
                "            http://docs.oasis-open.org/wsrf/rpw-2/GetResourceProperty/GetResourcePropertyResponse\n" +
                "        </wsa:Action>\n" +
                "    </soap:Header>\n" +
                "    <soap:Body>\n" +
                "        <wsrf-rp:GetResourcePropertyResponse\n" +
                "                xmlns:wsrf-rp=\"http://docs.oasis-open.org/wsrf/2004/06/wsrf-WS-ResourceProperties-1.2-draft-01.xsd\"\n" +
                "                xmlns:muws-p1-xs=\"http://docs.oasis-open.org/wsdm/2004/12/muws/wsdm-muws-part1.xsd\">\n" +
                "            <muws-p1-xs:ManageabilityCapability>\n" +
                "                http://docs.oasis-open.org/wsdm/muws/capabilities/Identity\n" +
                "            </muws-p1-xs:ManageabilityCapability>\n" +
                "            <muws-p1-xs:ManageabilityCapability>\n" +
                "                http://docs.oasis-open.org/wsdm/muws/capabilities/ManageabilityCharacteristics\n" +
                "            </muws-p1-xs:ManageabilityCapability>\n" +
                "            <muws-p1-xs:ManageabilityCapability>\n" +
                "                http://docs.oasis-open.org/wsdm/muws/capabilities/OperationalStatus\n" +
                "            </muws-p1-xs:ManageabilityCapability>\n" +
                "            <muws-p1-xs:ManageabilityCapability>\n" +
                "                http://docs.oasis-open.org/wsdm/muws/capabilities/Metrics\n" +
                "            </muws-p1-xs:ManageabilityCapability>\n" +
                "            <muws-p1-xs:ManageabilityCapability>\n" +
                "                http://docs.oasis-open.org/mows-2/capabilities/Metrics\n" +
                "            </muws-p1-xs:ManageabilityCapability>\n" +
                "        </wsrf-rp:GetResourcePropertyResponse>\n" +
                "    </soap:Body>\n" +
                "</soap:Envelope>";
    }

    /*public static GetResourceProperty resolve(Document doc) throws InvalidDocumentFormatException {
        Element bodychild = getFirstBodyChild(doc);
        if (bodychild == null) return null;
        if (testElementLocalName(bodychild, "GetResourceProperty")) {
            return new GetResourceProperty(bodychild);
        }
        return null;
    }*/
}
