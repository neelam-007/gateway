/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.message;

import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.xml.MessageNotSoapException;
import org.xml.sax.SAXException;

import java.io.IOException;

/**
 * Provides the SOAP aspect of a Message.
 */
class SoapFacet extends MessageFacet {
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
        if (!SoapUtil.isSoapMessage(message.getXmlKnob().getDocument()))
            throw new MessageNotSoapException();
    }

    public MessageKnob getKnob(Class c) {
        if (c == SoapKnob.class) {
            return new SoapKnob() {
                public String getPayloadNamespaceUri() throws IOException, SAXException {
                    return SoapUtil.getPayloadNamespaceUri(getMessage().getXmlKnob().getDocument());
                }
            };
        }
        return super.getKnob(c);
    }
}
