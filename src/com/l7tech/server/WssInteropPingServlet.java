/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.util.SoapFaultUtils;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.xml.MessageNotSoapException;
import com.l7tech.common.xml.SoapFaultDetail;
import com.l7tech.common.xml.SoapFaultDetailImpl;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author mike
 */
public class WssInteropPingServlet extends HttpServlet {
    protected void doPost(HttpServletRequest hreq, HttpServletResponse hres)
            throws ServletException, IOException
    {
        try {
            Document d = XmlUtil.parse(hreq.getInputStream());
            if (!SoapUtil.isSoapMessage(d)) throw new MessageNotSoapException("Request message is not SOAP");

            Element payload = SoapUtil.getPayloadElement(d);
            if (payload == null) throw new MessageNotSoapException("Request message has no payload");
            if (!"Ping".equals(payload.getLocalName())) throw new InvalidDocumentFormatException("Request message payload is not a Ping element");
            String payloadString = XmlUtil.getTextValue(payload);

            Element header = SoapUtil.getHeaderElement(d);
            if (header == null) throw new InvalidDocumentFormatException("Request message has no SOAP Header");
            Element pingHeader = XmlUtil.findOnlyOneChildElementByName(header, (String)null, "PingHeader");
            if (pingHeader == null) throw new InvalidDocumentFormatException("Request message has no PingHeader SOAP header");
            String headerString = XmlUtil.getTextValue(pingHeader);

            String responseString = payloadString + headerString;

            String respSkel = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                              " <soap:Body>\n" +
                              "     <PingResponse xmlns=\"http://xmlsoap.org/Ping\"/>\n" +
                              " </soap:Body>\n" +
                              "</soap:Envelope>";
            Document responseDoc = XmlUtil.stringToDocument(respSkel);
            Element pingResponse = SoapUtil.getPayloadElement(responseDoc);
            if (pingResponse == null) throw new IllegalStateException("Internal error - response with no payload"); // can't happen

            pingResponse.appendChild(XmlUtil.createTextNode(pingResponse, responseString));

            hres.setStatus(200);
            hres.setContentType(ContentTypeHeader.XML_DEFAULT.getFullValue());
            XmlUtil.nodeToOutputStream(responseDoc, hres.getOutputStream());
            hres.flushBuffer();
        } catch (SAXException e) {
            error(hreq, hres, e);
        } catch (InvalidDocumentFormatException e) {
            error(hreq, hres, e);
        }
    }

    protected void doGet(HttpServletRequest hreq, HttpServletResponse hres) throws ServletException, IOException
    {
        error(hreq, hres, new UnsupportedOperationException("HTTP GET not supported; please use POST"));
    }

    private void error(HttpServletRequest hreq, HttpServletResponse hres, Throwable e) throws IOException {
        hres.setContentType(ContentTypeHeader.XML_DEFAULT.getFullValue());
        hres.setStatus(500);
        SoapFaultDetail sfd = new SoapFaultDetailImpl("je:" + e.getClass().getName(),
                                                      "Exception: " + e.getClass().getName() + ": " + e.getMessage(),
                                                      null);
        try {
            Document fault = SoapFaultUtils.generateSoapFaultDocument(sfd, "wssInteropPingServer");
            XmlUtil.nodeToFormattedOutputStream(fault, hres.getOutputStream());
        } catch (SAXException e1) {
            throw new RuntimeException(e1); // can't happen
        }

        hres.flushBuffer();
    }
}
