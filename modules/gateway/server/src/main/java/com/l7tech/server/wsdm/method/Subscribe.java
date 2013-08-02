package com.l7tech.server.wsdm.method;

import com.l7tech.objectmodel.Goid;
import com.l7tech.util.*;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.server.wsdm.Namespaces;
import com.l7tech.server.wsdm.faults.*;
import com.l7tech.server.wsdm.subscription.Subscription;
import com.l7tech.server.wsdm.util.ISO8601Duration;
import com.l7tech.message.Message;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.soap.SOAPConstants;
import java.net.URL;
import java.net.MalformedURLException;
import java.text.MessageFormat;
import java.util.*;
import java.io.IOException;

/**
 * Abstraction for the wsnt:Subscribe method
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Nov 2, 2007<br/>
 */
public class Subscribe extends ESMMethod {
    private String callBackAddress;
    private long termination;
    private boolean terminationParsed;
    private String topicValue;
    private String serviceId;
    private String referenceParams;

    private Set<String> unallowedNsInRefParams = new HashSet<String>() {{
        add(SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE);
        add(SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE);
        add(Namespaces.WSA);
    }};

    private static final int MAX_REFERENCE_PARAMS_SIZE = 16384; // match Subscription.getReferenceParams column length

    private Subscribe(final Element subscribeEl, Document doc, Message request, Goid esmServiceGoid) throws GenericNotificationExceptionFault, WsAddressingFault, MalformedURLException {
        super(doc, request, esmServiceGoid);
        try {
            // extract ConsumerReference/Address/Text()
            Element consumerReferenceEl = XmlUtil.findOnlyOneChildElementByName(subscribeEl, Namespaces.WSNT, "ConsumerReference");
            if (consumerReferenceEl == null) {
                throw new InvalidMessageContentExpressionFault("ConsumerReference must be present");
            }
            Element addressEl = XmlUtil.findOnlyOneChildElementByName(consumerReferenceEl, Namespaces.ALL_WSA, "Address");
            if (addressEl == null) {
                throw new InvalidMessageContentExpressionFault("ConsumerReference/Address must be present");
            }
            callBackAddress = XmlUtil.getTextValue(addressEl);
            try {
                URL callback = new URL(callBackAddress);
                String proto = callback.getProtocol();
                if (!proto.equalsIgnoreCase("http") && !proto.equalsIgnoreCase("https")) {
                    throw new InvalidMessageContentExpressionFault(MessageFormat.format("Callback address protocol {0} ({1}) is not allowed. Must be http or https", proto, callBackAddress));
                }
            } catch (MalformedURLException e) {
                throw new InvalidMessageContentExpressionFault(callBackAddress + " is not a valid callback address");
            }

            // Extract InitialTerminationTime value
            Element initialTerminationTimeEl = XmlUtil.findOnlyOneChildElementByName(subscribeEl, Namespaces.WSNT, "InitialTerminationTime");
            if (initialTerminationTimeEl != null) {
                String maybeTerminationTime = XmlUtil.getTextValue(initialTerminationTimeEl);
                if (maybeTerminationTime == null) {
                    throw new UnacceptableTerminationTimeFault("InitialTerminationTime cannot be null");
                }

                try {
                    Date terminationDate = ISO8601Date.parse(maybeTerminationTime);
                    termination = terminationDate.getTime();
                } catch (Exception e) {
                    try {
                        ISO8601Duration duration = new ISO8601Duration(maybeTerminationTime);
                        termination = System.currentTimeMillis() + (duration.inSeconds() * 1000);
                    } catch (Exception e1) {
                        throw new UnacceptableTerminationTimeFault("Invalid InitialTerminationTime value " + maybeTerminationTime);
                    }
                }
                terminationParsed = true;
            }

            // extract reference parameters
            Element refParams = XmlUtil.findOnlyOneChildElementByName(consumerReferenceEl, Namespaces.WSA, "ReferenceParameters");
            if(refParams != null) {
                checkUnallowedNamespaces(refParams);
                final PoolByteArrayOutputStream out = new PoolByteArrayOutputStream(1024);
                try {
                    XmlUtil.nodeToOutputStreamWithXss4j(refParams, out);
                    referenceParams = out.toString(Charsets.UTF8);
                    if(referenceParams.length() > MAX_REFERENCE_PARAMS_SIZE)
                        throw new WsAddressingFault(new WsAddressingFault.WsaFaultDetail(
                            WsAddressingFault.WsaFaultDetailType.PROBLEM_HEADER_QNAME,
                            new HashMap<String, String>() {{ put("errMsg", "WSA Reference Parameters maximum size exceeded: " + referenceParams.length() + " > " + MAX_REFERENCE_PARAMS_SIZE); }},
                            refParams.getLocalName()));
                } catch (final IOException e) {
                    throw new WsAddressingFault(new WsAddressingFault.WsaFaultDetail(
                        WsAddressingFault.WsaFaultDetailType.PROBLEM_HEADER_QNAME,
                        new HashMap<String, String>() {{ put("errMsg", "Error processing WSA header: " + ExceptionUtils.getMessage(e)); }},
                        refParams.getLocalName()));
                } finally {
                    out.close();
                }
            }

            // Parse Filter value
            Element filterEl = XmlUtil.findOnlyOneChildElementByName(subscribeEl, Namespaces.WSNT, "Filter");
            if (filterEl == null) {
                throw new InvalidMessageContentExpressionFault("wsnt:Filter element must be present");
            }
            Element topicExpressionEl = XmlUtil.findOnlyOneChildElementByName(filterEl, Namespaces.WSNT, "TopicExpression");
            if (topicExpressionEl == null) {
                throw new InvalidMessageContentExpressionFault("wsnt:TopicExpression element must be present");
            }
            topicValue = getStandardizeQName(XmlUtil.getTextValue(topicExpressionEl), XmlUtil.getNamespaceMap(topicExpressionEl));
            if (topicValue == null || topicValue.length() < 1) {
                throw new InvalidMessageContentExpressionFault("wsnt:TopicExpression must contain a value");
            }
        } catch (InvalidDocumentFormatException e) {
            throw new InvalidMessageContentExpressionFault("Error in document format");
        }
    }

    public static Subscribe resolve(Message request, Goid esmServiceGoid) throws GenericNotificationExceptionFault, WsAddressingFault, SAXException, IOException {
        Document doc = request.getXmlKnob().getDocumentReadOnly();
        Element bodychild;
        try {
            bodychild = getFirstBodyChild(doc);
        } catch (InvalidDocumentFormatException e) {
            throw new GenericNotificationExceptionFault("Cannot retrieve soap body");
        }
        if (bodychild == null) return null;
        if (testElementLocalName(bodychild, "Subscribe")) {
            return new Subscribe(bodychild, doc, request, esmServiceGoid);
        }
        return null;
    }

    public String getCallBackAddress() {
        return callBackAddress;
    }

    public long getTermination() {
        return termination;
    }

    public boolean isTerminationParsed() {
        return terminationParsed;
    }

    public String getReferenceParams() {
        return referenceParams;
    }

    /**
     * Get the topic value.
     *
     * <p>If the QName used a non-default prefix this will have been modified
     * to the "standard" namespace prefix for the namespace.</p> 
     *
     * @return The value.
     */
    public String getTopicValue() {
        return topicValue;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String respond(String thisURL, String subscriptionid) {
        return
        "<soap:Envelope xmlns:soap=\"" + SOAPConstants.URI_NS_SOAP_ENVELOPE + "\" xmlns:wsa=\"" + Namespaces.WSA + "\">\n" +
        "  <soap:Header>\n" +
        "    <wsa:Action>\n" +
        "      http://docs.oasis-open.org/wsn/bw-2/NotificationProducer/SubscribeResponse\n" +
        "    </wsa:Action>\n" +
        "    <wsa:RelatesTo>\n" +
        "      " + getMessageId() + "\n" +
        "    </wsa:RelatesTo>\n" +
        "  </soap:Header>\n" +
        "  <soap:Body>\n" +
        "    <wsnt:SubscribeResponse xmlns:wsnt=\"" + Namespaces.WSNT + "\">\n" +
        "      <wsnt:SubscriptionReference>\n" +
        "        <wsa:EndpointReference>\n" +
        "          <wsa:Address>\n" +
        "            " + thisURL + "\n" +
        "          </wsa:Address>\n" +
        "          <wsa:ReferenceParameters>\n" +
        "            <l7sub:SubscriptionId xmlns:l7sub=\"" + Subscription.NS + "\">\n" +
        "              " + subscriptionid + "\n" +
        "            </l7sub:SubscriptionId>\n" +
        "          </wsa:ReferenceParameters>\n" +
        "        </wsa:EndpointReference>\n" +
        "      </wsnt:SubscriptionReference>\n" +
        "    </wsnt:SubscribeResponse>\n" +
        "  </soap:Body>\n" +
        "</soap:Envelope>";
    }

    private String getStandardizeQName( final String qname, final Map<String,String> namespaces ) {
        String canonical = qname;

        if ( canonical != null ) {
            String[] parts = canonical.split(":",2);
            if ( parts.length == 2 ) {
                String namespace = namespaces.get( parts[0] );
                if ( namespace != null ) {
                    if ( Namespaces.MOWSE.equals(namespace) ) {
                        canonical = "mowse:" + parts[1];
                    } else if ( Namespaces.MUWS_EV.equals(namespace) ) {
                        canonical = "muws-ev:" + parts[1];
                    } else if ( parts[0].equals( "mowse" ) ||
                                parts[0].equals( "muws-ev" ) ) {
                        // This prevents use of the "standard" prefixes
                        // with other namespaces
                        canonical = parts[1];
                    }
                }
            }
        }

        return canonical;
    }

    private void checkUnallowedNamespaces(Element refParams) throws WsAddressingFault {
        Node node = refParams.getFirstChild();
        Stack<Node> stack = new Stack<Node>();
        while (node != null) {
            // test
            for(final String unallowedNS : unallowedNsInRefParams) {
                if (unallowedNS.equals(node.getNamespaceURI()) || node.isDefaultNamespace(unallowedNS))
                    throw new WsAddressingFault( new WsAddressingFault.WsaFaultDetail(
                        WsAddressingFault.WsaFaultDetailType.PROBLEM_HEADER_QNAME,
                        new HashMap<String,String>() {{ put("errMsg", "Namespace not allowed in WSA Reference Parameters: " + unallowedNS); }},
                        refParams.getLocalName()
                    ));
            }
            // move to next
            if (node.hasChildNodes()) {
                stack.push(node);
                node = node.getFirstChild();
            } else {
                while((node = node.getNextSibling()) == null && ! stack.isEmpty()) {
                    node = stack.pop();
                }
            }
        }
    }
}

/*

If the NotificationProducer is unable or unwilling to set the TerminationTime resource property of the Subscription
resource to the requested time or a value greater, or if this requested time is not in the future, then the
NotificationProducer MUST return an UnacceptableInitialTerminationTimeFault message.

The use of the xsi:nil attribute with value <code>true</code> indicates there is no scheduled termination time requested for
the Subscription, implying that the requested Subscription has infinite duration.

Request:
<soap:Envelope>
  <soap:Header>
    <wsa:Action>
      http://docs.oasis-open.org/wsn/bw-2/NotificationProducer/SubscribeRequest
    </wsa:Action>
  </soap:Header>
  <soap:Body>
    <wsnt:Subscribe>
      <wsnt:ConsumerReference>
        <wsa:Address>
          http://esmsubscriptionconsumer.acme.com
        </wsa:Address>
      </wsnt:ConsumerReference>
      <wsnt:Filter>
        <wsnt:TopicExpression Dialect= "http://docs.oasis-open.org/wsn/t-1/TopicExpression/Simple">
          mowse:MetricsCapability
        </wsnt:TopicExpression>
      </wsnt:Filter>
      <wsnt:InitialTerminationTime>
        2007-10-10T12:00:00-05:00
      </wsnt:InitialTerminationTime>
    </wsnt:Subscribe>
  </soap:Body>
</soap:Envelope>

Response:
<soap:Envelope>
  <soap:Header>
    <wsa:Action>
      http://docs.oasis-open.org/wsn/bw-2/NotificationProducer/SubscribeResponse
    </wsa:Action>
  </soap:Header>
  <soap:Body>
    <wsnt:SubscribeResponse>
      <wsnt:SubscriptionReference>
        <wsa:EndpointReference>
          <wsa:Address>
            http://dod.mil/services/EsmSubscriptionManagementService
          </wsa:Address>
          <wsa:ReferenceParameters>
            <SubscriptionId xmlns="urn:example:wsn:subscription:manager">
              991be01c-e5ca-4d00-811e-b6e29079e7da
            </SubscriptionId>
          </wsa:ReferenceParameters>
        </wsa:EndpointReference>
      </wsnt:SubscriptionReference>
    </wsnt:SubscribeResponse>
  </soap:Body>
</soap:Envelope>
*/