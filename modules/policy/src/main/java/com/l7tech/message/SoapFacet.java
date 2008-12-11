/*
 * Copyright (C) 2004-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.message;

import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.xml.soap.SoapFaultUtils;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.xml.SoapFaultDetail;
import com.l7tech.xml.SoftwareFallbackException;
import com.l7tech.xml.TarariLoader;
import com.l7tech.xml.tarari.TarariMessageContext;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides the SOAP aspect of a Message.
 */
class SoapFacet extends MessageFacet {
    private SoapFaultDetail faultDetail = null;
    private SoapInfo soapInfo;

    private static final Logger logger = Logger.getLogger(SoapFacet.class.getName());
    private static final QName[] EMPTY_QNAMES = new QName[0];


    /**
     * Create a new SoapFacet for the specified message, wrapping the specified root facet.  SoapFacets can only
     * be created on messages for which {@link #getSoapInfo()} has already returned a non-null value.
     *
     * @param message  the message being enhanced.  May not be null.
     * @param facet  the previous root facet.  May not be null.  Must contain an XML facet.
     * @param initialSoapInfo  the non-null result of calling getSoapInfo() with this message's first part content.
     */
    public SoapFacet(Message message, MessageFacet facet, SoapInfo initialSoapInfo) {
        super(message, facet);

        if (initialSoapInfo == null) throw new NullPointerException();
        this.soapInfo = initialSoapInfo;
    }

    /**
     * Check if an already-known-to-be-XML Message is actually SOAP, and if so, returns a SoapInfo
     * for creating a SoapFacet.
     *
     * @param message  the Message to examine.  Must already have a MimeKnob and an XmlKnob.
     * @return the SoapInfo for this Message.  Never null.  See {@link SoapInfo#isSoap()}
     * @throws IOException if there is a problem reading the Message data.
     * @throws SAXException if the XML is not well formed or has invalid namespace declarations
     * @throws NoSuchPartException if the Message first part has already been destructively read
     */
    static SoapInfo getSoapInfo(Message message) throws IOException, SAXException, NoSuchPartException {
        HasSoapAction haver = (HasSoapAction) message.getKnob(HasSoapAction.class);
        final String soapAction = haver == null ? null : haver.getSoapAction();

        XmlKnob xk = (XmlKnob) message.getKnob(XmlKnob.class);
        if (xk != null && xk.isDomParsed() && !xk.isTarariWanted()) {
            // Performance hack... If this policy doesn't strongly prefer Tarari, use the DOM that's already present for
            // SOAP identification purposes
            return getSoapInfoDom(message.getXmlKnob().getDocumentReadOnly(), soapAction);
        }

        TarariMessageContextFactory mcfac = TarariLoader.getMessageContextFactory();
        if (mcfac != null) {
            try {
                InputStream inputStream = message.getMimeKnob().getFirstPart().getInputStream(false);
                TarariMessageContext mc = mcfac.makeMessageContext(inputStream);
                SoapInfo soapInfo = mc.getSoapInfo(soapAction);
                TarariKnob tarariKnob = (TarariKnob)message.getKnob(TarariKnob.class);
                if (tarariKnob == null) {
                    message.attachKnob(TarariKnob.class, new TarariKnob(message, mc, soapInfo));
                } else {
                    tarariKnob.setContext(mc, soapInfo);
                }

                return soapInfo;
            } catch (SoftwareFallbackException e) {
                logger.log(Level.INFO, "Falling back from hardware to software processing", e);
                // fallthrough to software
            }
        }
        return getSoapInfoDom(message.getXmlKnob().getDocumentReadOnly(), soapAction);
    }

    /**
     * Software fallback version of getSoapInfo.  Requires DOM parsing have been done already.
     * @return a SoapInfo.  Never null.
     * @param document the DOM for the request; must not be null.
     * @param soapAction the SOAPAction value from the transport layer. May be null.
     * @throws org.xml.sax.SAXException if the message cannot be parsed (likely a lazy DOM implementation)
     */
    private static SoapInfo getSoapInfoDom(Document document, String soapAction) throws SAXException {
        boolean hasSecurityNode = false;
        final QName[] payloadNs;
        if (SoapUtil.isSoapMessage(document)) {
            try {
                // if (soapAction == null) tryToGetSoapActionFromWsaHeader();
                List els = SoapUtil.getSecurityElements(document);
                if (els != null && !els.isEmpty()) hasSecurityNode = true;
                payloadNs = SoapUtil.getPayloadNames(document);
                return new SoapInfo(true, soapAction, payloadNs != null ? payloadNs : EMPTY_QNAMES, hasSecurityNode);
            } catch (InvalidDocumentFormatException e) {
                throw new SAXException(e);
            }
        } else {
            return new SoapInfo(false, soapAction, EMPTY_QNAMES, false);
        }
    }

    public MessageKnob getKnob(Class c) {
        if (c == SoapKnob.class) {
            return new SoapKnob() {

                public QName[] getPayloadNames() throws IOException, SAXException, NoSuchPartException {
                    return getSoapInfo().getPayloadNames();
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

                public boolean isSecurityHeaderPresent() throws NoSuchPartException, IOException, SAXException {
                    return getSoapInfo().hasSecurityNode;
                }

                public void invalidate() {
                    // TODO invalidate faultDetail too? It's not connected to the message content anyway
                    soapInfo = null;
                }

                /**
                 * Get the fault details for this message.
                 *
                 * @return the fault details for this message if it's a SOAP fault, or null if it isn't.
                 * @throws SAXException if the first part's content type is not text/xml.
                 * @throws IOException if there is a problem reading XML from the first part's InputStream
                 * @throws InvalidDocumentFormatException if the message is not SOAP or there is more than one Body
                 * @throws com.l7tech.util.MissingRequiredElementException if the message looks like a SOAP fault but has a missing or empty faultcode
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

    private SoapInfo getSoapInfo() throws NoSuchPartException, IOException, SAXException {
        if (soapInfo == null) {
            soapInfo = SoapFacet.getSoapInfo(getMessage());
        }
        return soapInfo;
    }
}
