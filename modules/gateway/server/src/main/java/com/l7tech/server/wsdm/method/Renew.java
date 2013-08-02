package com.l7tech.server.wsdm.method;

import com.l7tech.objectmodel.Goid;
import com.l7tech.server.wsdm.Namespaces;
import com.l7tech.server.wsdm.faults.GenericNotificationExceptionFault;
import com.l7tech.server.wsdm.faults.ResourceUnknownFault;
import com.l7tech.server.wsdm.faults.UnacceptableTerminationTimeFault;
import com.l7tech.server.wsdm.subscription.Subscription;
import com.l7tech.server.wsdm.util.ISO8601Duration;
import com.l7tech.util.ISO8601Date;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.message.Message;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.soap.SOAPConstants;
import java.text.ParseException;
import java.util.Date;
import java.io.IOException;
import java.net.MalformedURLException;

/**
 * Abstraction for the Renew method
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Nov 2, 2007<br/>
 */
public class Renew extends ESMMethod {
    private long termination;
    private String subscriptionIdValue;
    private String terminationTimeValue;

    private Renew(String subscriptionId, Element r, Document doc, Message request, Goid esmServiceGoid) throws GenericNotificationExceptionFault, MalformedURLException {
        super(doc, request, esmServiceGoid);

        // find the termination time value
        boolean terminationCheck = false;
        NodeList nl = r.getElementsByTagNameNS("*", "TerminationTime");
        for (int i = 0; i < nl.getLength(); i++) {
            if (nl.item(i) instanceof Element) {
                Element termTimeEl = (Element)nl.item(i);
                String maybethetime = XmlUtil.getTextValue(termTimeEl);
                if (maybethetime != null && maybethetime.length() > 0) {
                    terminationTimeValue = maybethetime;
                    try {
                        Date terminationDate;
                        terminationDate = ISO8601Date.parse(terminationTimeValue);
                        termination = terminationDate.getTime();
                    } catch (Exception e) {
                        try {
                            ISO8601Duration duration = new ISO8601Duration(terminationTimeValue);
                            termination = System.currentTimeMillis() + (duration.inSeconds() * 1000);
                        } catch (ParseException e1) {
                            throw new UnacceptableTerminationTimeFault("Invalid InitialTerminationTime value " + terminationTimeValue);
                        }
                    }
                    terminationCheck = true;
                    break;
                }
            }
        }
        if (!terminationCheck) {
            throw new UnacceptableTerminationTimeFault("Missing TerminationTime");
        }
        
        // find the subscription id
        nl = r.getElementsByTagNameNS("*", "SubscriptionId" );
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

        if (subscriptionIdValue == null) {
            throw new ResourceUnknownFault("No SubscriptionID provided");
        }
    }

    public static Renew resolve(Message request, Goid esmServiceGoid) throws GenericNotificationExceptionFault, SAXException, IOException {
        Document doc = request.getXmlKnob().getDocumentReadOnly();
        try {
            Element bodychild = getFirstBodyChild(doc);
            if ( bodychild != null ) {
                if (testElementLocalName(bodychild, "Renew")) {
                    return new Renew(getSubscriptionIdAddressingParameter(doc), bodychild, doc, request, esmServiceGoid);
                }
            }
        } catch (InvalidDocumentFormatException e) {
            throw new GenericNotificationExceptionFault(e.getMessage());
        }

        return null;
    }

    public long getTermination() {
        return termination;
    }

    public String getSubscriptionIdValue() {
        return subscriptionIdValue;
    }

    public String respond(String thisURL) {
        return "<soap:Envelope xmlns:soap=\"" + SOAPConstants.URI_NS_SOAP_ENVELOPE + "\" xmlns:wsa=\"" + Namespaces.WSA + "\">\n" +
        "  <soap:Header>\n" +
        "    <wsa:Action>\n" +
        "      http://docs.oasis-open.org/wsn/bw-2/SubscriptionManager/RenewResponse\n" +
        "    </wsa:Action>\n" +
        "    <wsa:RelatesTo>\n" +
        "      " + getMessageId() + "\n" +
        "    </wsa:RelatesTo>\n" +
        "  </soap:Header>\n" +
        "  <soap:Body>\n" +
        "    <wsnt:RenewResponse xmlns:wsnt=\"" + Namespaces.WSNT + "\">\n" +
        "      <wsnt:TerminationTime>" + terminationTimeValue + "</wsnt:TerminationTime>\n" +
        "      <wsnt:CurrentTime>" + ISO8601Date.format(new Date(System.currentTimeMillis())) + "</wsnt:CurrentTime>\n" +
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
        "    </wsnt:RenewResponse>\n" +
        "  </soap:Body>\n" +
        "</soap:Envelope>";
    }
}
