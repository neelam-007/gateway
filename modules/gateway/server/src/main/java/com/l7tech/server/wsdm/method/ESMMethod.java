package com.l7tech.server.wsdm.method;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.server.wsdm.faults.FaultMappableException;
import com.l7tech.server.wsdm.subscription.Subscription;
import com.l7tech.server.wsdm.Namespaces;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.xml.soap.SoapUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.List;

/**
 * Entry point for resolving which method is being invoked
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Nov 2, 2007<br/>
 */
public abstract class ESMMethod {
    private Document reqestDoc;

    public static ESMMethod resolve(Document doc) throws FaultMappableException {
        /*
        ESMMethod output = GetManageabilityReferences.resolve(doc);
        if (output != null) return appendDocAndReturn(output, doc);
        */
        ESMMethod output = GetMultipleResourceProperties.resolve(doc);
        if (output != null) return appendDocAndReturn(output, doc);

        /*
        output = GetResourceProperty.resolve(doc);
        if (output != null) return appendDocAndReturn(output, doc);
        */
        output = Renew.resolve(doc);
        if (output != null) return appendDocAndReturn(output, doc);

        output = Subscribe.resolve(doc);
        if (output != null) return appendDocAndReturn(output, doc);

        output = Unsubscribe.resolve(doc);
        if (output != null) return appendDocAndReturn(output, doc);

        return null;
    }

    private static ESMMethod appendDocAndReturn(ESMMethod m, Document doc) {
        if (m == null) return null;
        m.setReqestDoc(doc);
        return m;
    }

    static Element getFirstBodyChild(Document doc) throws InvalidDocumentFormatException {
        Element bodyel = SoapUtil.getBodyElement(doc);
        if (bodyel == null) return null;
        return XmlUtil.findFirstChildElement(bodyel);
    }

    /**
     * Access the SubscriptionId if present as a WS-Addressing reference parameter.
     *
     * <p>This SubscriptionId must be part of the SOAP Header (when present).</p>
     *
     * <pre>
     *     &lt;l7sub:SubscriptionId wsa:IsReferenceParameter="true" xmlns:l7sub="http://www.layer7tech.com/ns/wsdm/subscription">
     *         54ca0956-2553-41ee-abb1-3215c2ccc30c
     *     &lt;/l7sub:SubscriptionId>
     * </pre>
     *
     * @param doc The SOAP message DOM
     * @return The subscriptionId or null
     * @throws InvalidDocumentFormatException If the message is not SOAP
     */
    static String getSubscriptionIdAddressingParameter(final Document doc) throws InvalidDocumentFormatException {
        String subscriptionIdValue = null;

        Element headerel = SoapUtil.getHeaderElement(doc);
        if ( headerel != null ) {
            // find the subscription id
            List<Element> elements = XmlUtil.findChildElementsByName( headerel, Subscription.NS, "SubscriptionId" );
            for ( Element subidel : elements ) {
                if ( "true".equals(subidel.getAttributeNS(Namespaces.WSA, "IsReferenceParameter")) ) {
                    String maybetheid = XmlUtil.getTextValue(subidel);
                    if ( maybetheid != null && maybetheid.length() > 0 ) {
                        subscriptionIdValue = maybetheid;
                        break;
                    }
                }
            }
        }

        return subscriptionIdValue;
    }

    static boolean testElementLocalName(Element el, String localname) {
        return el.getLocalName() != null && el.getLocalName().equals(localname);
    }


    public Document getReqestDoc() {
        return reqestDoc;
    }

    public void setReqestDoc(Document reqestDoc) {
        this.reqestDoc = reqestDoc;
    }
}
