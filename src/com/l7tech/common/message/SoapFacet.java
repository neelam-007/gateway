/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.message;

import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.util.SoapFaultUtils;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.xml.SoapFaultDetail;
import com.l7tech.common.xml.SoftwareFallbackException;
import com.l7tech.common.xml.TarariProber;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides the SOAP aspect of a Message.
 */
class SoapFacet extends MessageFacet {
    private SoapFaultDetail faultDetail = null;
    private final SoapInfo soapInfo;

    private transient static final Logger logger = Logger.getLogger(SoapFacet.class.getName());


    /**
     * Create a new SoapFacet for the specified message, wrapping the specified root facet.  SoapFacets can only
     * be created on messages for which {@link #getSoapInfo} has already returned a non-null value.
     *
     * @param message  the message being enhanced.  May not be null.
     * @param facet  the previous root facet.  May not be null.  Must contain an XML facet.
     * @param soapInfo  the non-null result of calling getSoapInfo() with this message's first part content.
     */
    public SoapFacet(Message message, MessageFacet facet, SoapInfo soapInfo) {
        super(message, facet);

        if (soapInfo == null) throw new NullPointerException();
        this.soapInfo = soapInfo;
    }

    /**
     * Check if an already-known-to-be-XML Message is actually SOAP, and if so, returns a SoapInfo
     * for creating a SoapFacet.
     *
     * @param message  the Message to examine.  Must already have a MimeKnob and an XmlKnob.
     * @return the SoapInfo if this Message is SOAP, or null if it is not SOAP.
     * @throws IOException if there is a problem reading the Message data.
     * @throws SAXException if the XML is not well formed or has invalid namespace declarations
     * @throws NoSuchPartException if the Message first part has already been destructively read
     */
    public static SoapInfo getSoapInfo(Message message) throws IOException, SAXException, NoSuchPartException {
        if (TarariProber.isTarariPresent()) {
            try {
                return TarariProber.getSoapInfoTarari(message.getMimeKnob().getFirstPart().getInputStream(false));
            } catch (SoftwareFallbackException e) {
                // TODO if this happens a lot for perfectly reasonable reasons, downgrade to something below INFO
                logger.log(Level.INFO, "Falling back from hardware to software processing", e);
                // fallthrough to software
            }
        }
        return getSoapInfoDom(message.getXmlKnob().getDocumentReadOnly());
    }

    /** Software fallback version of getSoapInfo.  Requires DOM parsing have been done already. */
    private static SoapInfo getSoapInfoDom(Document document) throws SAXException {
        boolean hasSecurityNode = false;
        String payloadNs = null;
        if (SoapUtil.isSoapMessage(document)) {
            try {
                List els = SoapUtil.getSecurityElements(document);
                if (els != null && !els.isEmpty()) hasSecurityNode = true;
                payloadNs = SoapUtil.getPayloadNamespaceUri(document);
                return new SoapInfo(payloadNs, hasSecurityNode);
            } catch (InvalidDocumentFormatException e) {
                throw new SAXException(e);
            }
        } else {
            return null;
        }
    }

    public MessageKnob getKnob(Class c) {
        if (c == SoapKnob.class) {
            return new SoapKnob() {

                public String getPayloadNamespaceUri() throws IOException, SAXException {
                    if (soapInfo != null && soapInfo.payloadNsUri != null) {
                        return soapInfo.payloadNsUri;
                    } else {
                        return SoapUtil.getPayloadNamespaceUri(getMessage().getXmlKnob().getDocumentReadOnly());
                    }
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

                public boolean isSecurityHeaderPresent() {
                    return soapInfo.hasSecurityNode;
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
                       faultDetail = SoapFaultUtils.gatherSoapFaultDetail(getMessage().getXmlKnob().getDocumentReadOnly());
                    return faultDetail;
                }
            };
        }
        return super.getKnob(c);
    }
}
