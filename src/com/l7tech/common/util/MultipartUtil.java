package com.l7tech.common.util;

import com.l7tech.message.Message;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class MultipartUtil {

    static public void addModifiedSoapPart(StringBuffer sbuf, String modifiedSoapEnvelope, Message.Part part, String boundary) {

        if(sbuf == null) throw new IllegalArgumentException("The StringBuffer is NULL");
        if(modifiedSoapEnvelope == null) throw new IllegalArgumentException("The modified SOAP envelope is NULL");
        if(part == null) throw new IllegalArgumentException("The SOAP part is NULL");
        if(boundary == null) throw new IllegalArgumentException("The StringBuffer is NULL");

        sbuf.append(XmlUtil.MULTIPART_BOUNDARY_PREFIX + boundary + "\n");
        Map headerMap = part.getHeaders();
        Set headerKeys = headerMap.keySet();
        Iterator headerItr = headerKeys.iterator();
        while(headerItr.hasNext()) {
            Message.HeaderValue headerValue = (Message.HeaderValue) headerMap.get(headerItr.next());
            sbuf.append(headerValue.getName()).append(": ").append(headerValue.getValue());

            // append parameters of the header
            Map parameters = headerValue.getParams();
            Set paramKeys = parameters.keySet();
            Iterator paramItr = paramKeys.iterator();
            while (paramItr.hasNext()) {
                String paramName = (String) paramItr.next();
                sbuf.append("; ").append(paramName).append("=").append(parameters.get(paramName));
            }
            sbuf.append("\n");
        }
        sbuf.append("\n").append(modifiedSoapEnvelope).append("\n");
        sbuf.append(XmlUtil.MULTIPART_BOUNDARY_PREFIX + boundary + "\n");
    }


    static public void addMultiparts(StringBuffer sbuf, Map multiparts, String boundary) {

        if(sbuf == null) throw new IllegalArgumentException("The StringBuffer is NULL");
        if(multiparts == null) throw new IllegalArgumentException("The multiparts map is NULL");
        if(boundary == null) throw new IllegalArgumentException("The multiparts boundary is NULL");

        boolean fisrtAttachment = true;

        // add attachments
        Set attachmentKeys = multiparts.keySet();
        Iterator itr = attachmentKeys.iterator();
        while (itr.hasNext()) {
            if(!fisrtAttachment)
                sbuf.append("\n" + XmlUtil.MULTIPART_BOUNDARY_PREFIX + boundary + "\n");

            Object o = (Object) itr.next();
            Message.Part part = (Message.Part) multiparts.get(o);

            Map headers = part.getHeaders();
            Set headerKeys = headers.keySet();
            Iterator headerItr = headerKeys.iterator();
            while (headerItr.hasNext()) {
                String headerName = (String) headerItr.next();
                Message.HeaderValue hv = (Message.HeaderValue) headers.get(headerName);
                sbuf.append(hv.getName()).append(": ").append(hv.getValue());

                // append parameters of the header
                Map parameters = hv.getParams();
                Set paramKeys = parameters.keySet();
                Iterator paramItr = paramKeys.iterator();
                while (paramItr.hasNext()) {
                    String paramName = (String) paramItr.next();
                    sbuf.append("; ").append(paramName).append("=").append(parameters.get(paramName));
                }
                sbuf.append("\n");
            }
            sbuf.append("\n" + part.getContent());
            sbuf.append("\n" + XmlUtil.MULTIPART_BOUNDARY_PREFIX + boundary + XmlUtil.MULTIPART_BOUNDARY_PREFIX + "\n");
        }

        // the last boundary has delimiter after the boundary
        sbuf.append("\n" + XmlUtil.MULTIPART_BOUNDARY_PREFIX + boundary + XmlUtil.MULTIPART_BOUNDARY_PREFIX + "\n");
    }
}
