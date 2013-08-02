package com.l7tech.server.wsdm.method;

import com.l7tech.objectmodel.Goid;
import com.l7tech.server.wsdm.Namespaces;
import com.l7tech.server.wsdm.faults.GenericNotificationExceptionFault;
import com.l7tech.server.wsdm.faults.ResourceUnknownFault;
import com.l7tech.server.wsdm.subscription.Subscription;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.message.Message;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.soap.SOAPConstants;
import java.io.IOException;
import java.net.MalformedURLException;

/**
 * Abstraction for the Unsubscribe method
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Nov 2, 2007<br/>
 */
public class Unsubscribe extends ESMMethod {
    private String subscriptionIdValue;

    private Unsubscribe(String subscriptionId, Element unSubscribeEl, Document doc, Message request, Goid esmServiceGoid) throws GenericNotificationExceptionFault, MalformedURLException {
        super(doc, request, esmServiceGoid);

        // Find the SubscriptionId element and parse the id of the subscription needing unsubscription
        NodeList nl = unSubscribeEl.getElementsByTagNameNS("*", "SubscriptionId" );
        for (int i = 0; i < nl.getLength(); i++) {
            if (nl.item(i) instanceof Element) {
                Element subidel = (Element)nl.item(i);
                String maybetheid = XmlUtil.getTextValue(subidel);
                if (maybetheid != null && maybetheid.length() > 0) {
                    subscriptionIdValue = maybetheid;
                    break;
                }
            }
        }

        if ( subscriptionIdValue == null ) {
            subscriptionIdValue = subscriptionId;
        }

        if ( subscriptionIdValue == null ) {
            throw new ResourceUnknownFault("No SubscriptionID provided");
        }
    }

    public static Unsubscribe resolve(Message request, Goid esmServiceGoid) throws GenericNotificationExceptionFault, SAXException, IOException {
        Document doc = request.getXmlKnob().getDocumentReadOnly();
        try {
            Element bodychild = getFirstBodyChild(doc);
            if ( bodychild != null ) {
                if (testElementLocalName(bodychild, "Unsubscribe")) {
                    return new Unsubscribe(getSubscriptionIdAddressingParameter(doc), bodychild, doc, request, esmServiceGoid);
                }
            }
        } catch (InvalidDocumentFormatException e) {
            throw new GenericNotificationExceptionFault("cannot get body child");
        }

        return null;
    }


    public String getSubscriptionIdValue() {
        return subscriptionIdValue;
    }

    public String respond(String thisURL) {
        return "<soap:Envelope xmlns:soap=\"" + SOAPConstants.URI_NS_SOAP_ENVELOPE + "\" xmlns:wsa=\"" + Namespaces.WSA + "\">\n" +
        "  <soap:Header>\n" +
        "    <wsa:Action>\n" +
        "      http://docs.oasis-open.org/wsn/bw-2/SubscriptionManager/UnsubscribeResponse\n" +
        "    </wsa:Action>\n" +
        "    <wsa:RelatesTo>\n" +
        "      " + getMessageId() + "\n" +
        "    </wsa:RelatesTo>\n" +
        "  </soap:Header>\n" +
        "  <soap:Body>\n" +
        "    <wsnt:UnsubscribeResponse xmlns:wsnt=\"" + Namespaces.WSNT + "\">\n" +
        "      <wsnt:SubscriptionReference>\n" +
        "        <wsa:EndpointReference>\n" +
        "          <wsa:Address>\n" +
        "            " + thisURL + "\n" +
        "          </wsa:Address>\n" +
        "          <wsa:ReferenceParameters>\n" +
        "            <l7sub:SubscriptionId xmlns:l7sub=\"" + Subscription.NS + "\">\n" +
        "              " + subscriptionIdValue + "\n" +
        "            </l7sub:SubscriptionId>\n" +
        "          </wsa:ReferenceParameters>\n" +
        "        </wsa:EndpointReference>\n" +
        "      </wsnt:SubscriptionReference>\n" +
        "    </wsnt:UnsubscribeResponse>\n" +
        "  </soap:Body>\n" +
        "</soap:Envelope>";
    }
}
