/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.message;

import com.l7tech.common.util.SoapFaultUtils;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.xml.MessageNotSoapException;
import com.l7tech.common.xml.SoapFaultDetail;
import org.xml.sax.SAXException;

import java.io.IOException;

/**
 * Provides the SOAP aspect of a Message.
 */
class SoapFacet extends MessageFacet {
    private SoapFaultDetail faultDetail = null;

    /**
     * Create a new SoapFacet for the specified message, wrapping the specified root facet.
     *
     * @param message  the message being enhanced.  May not be null.
     * @param facet  the previous root facet.  May not be null.  Must contain an XML facet.
     * @throws SAXException if the first part's content type is not text/xml.
     * @throws IOException if XML serialization throws IOException, perhaps due to a lazy Document.
     * @throws MessageNotSoapException if there is an XML document but it doesn't look like a valid SOAP envelope
     */
    public SoapFacet(Message message, MessageFacet facet)
            throws SAXException, IOException, MessageNotSoapException
    {
        super(message, facet);
        if (!SoapUtil.isSoapMessage(message.getXmlKnob().getDocument(false)))
            throw new MessageNotSoapException();
    }

    public MessageKnob getKnob(Class c) {
        if (c == SoapKnob.class) {
            return new SoapKnob() {

                public String getPayloadNamespaceUri() throws IOException, SAXException {
                    return SoapUtil.getPayloadNamespaceUri(getMessage().getXmlKnob().getDocument(false));
                }

                public boolean isFault() throws SAXException, IOException, InvalidDocumentFormatException {
                    return gatherFaultDetail() != null;
                }

                public SoapFaultDetail getFaultDetail() throws InvalidDocumentFormatException, IOException, SAXException {
                    if (!isFault()) throw new IllegalStateException("Message is not a SOAP fault");
                    return faultDetail;
                }

                public void setFaultDetail(SoapFaultDetail faultDetail) {
                    if (faultDetail == null || faultDetail.getFaultActor() == null || faultDetail.getFaultActor().length() < 1)
                        throw new IllegalArgumentException("Fault detail is null or has a missing or empty fault actor");
                    SoapFacet.this.faultDetail = faultDetail;
                }

                /**
                 * Get the fault details for this message.
                 *
                 * @return the fault details for this message if it's a SOAP fault, or null if it isn't.
                 * @throws SAXException if the first part's content type is not text/xml.
                 * @throws IOException if there is a problem reading XML from the first part's InputStream
                 * @throws InvalidDocumentFormatException if the message is not SOAP or there is more than one Body
                 * @throws com.l7tech.common.xml.MissingRequiredElementException if the message looks like a SOAP fault but has a missing or empty faultcode
                 */
                private SoapFaultDetail gatherFaultDetail() throws SAXException, IOException, InvalidDocumentFormatException {
                    if (faultDetail == null)
                       faultDetail = SoapFaultUtils.gatherSoapFaultDetail(getMessage().getXmlKnob().getDocument(false));
                    return faultDetail;
                }
            };
        }
        return super.getKnob(c);
    }
}
