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

    static public String addModifiedSoapPart(String modifiedSoapEnvelope, Message.Part part, String boundary) throws IOException {
        StringBuffer sb = new StringBuffer();
        sb.append(XmlUtil.MULTIPART_BOUNDARY_PREFIX + boundary + "\n");
        Map headerMap = part.getHeaders();
        Set headerKeys = headerMap.keySet();
        Iterator headerItr = headerKeys.iterator();
        while(headerItr.hasNext()) {
            Message.HeaderValue headerValue = (Message.HeaderValue) headerMap.get(headerItr.next());
            sb.append(headerValue.getName()).append(":").append(headerValue.getValue());

            // append parameters of the header
            Map parameters = headerValue.getParams();
            Set paramKeys = parameters.keySet();
            Iterator paramItr = paramKeys.iterator();
            while (paramItr.hasNext()) {
                String paramName = (String) paramItr.next();
                sb.append("; ").append(paramName).append("=").append(parameters.get(paramName));
            }
            sb.append("\n");
        }
        sb.append(modifiedSoapEnvelope).append("\n");
        sb.append(XmlUtil.MULTIPART_BOUNDARY_PREFIX + boundary + "\n");
        return sb.toString();
    }


    static public String addMultiparts(String requestXml, Map multiparts, String boundary) throws IOException {
        StringBuffer sb = new StringBuffer();

        // append the SOAP part
        sb.append(requestXml);

        // add attachments
        Set attachmentKeys = multiparts.keySet();
        Iterator itr = attachmentKeys.iterator();
        while (itr.hasNext()) {
            Object o = (Object) itr.next();
            Message.Part part = (Message.Part) multiparts.get(o);

            Map headers = part.getHeaders();
            Set headerKeys = headers.keySet();
            Iterator headerItr = headerKeys.iterator();
            while (headerItr.hasNext()) {
                String headerName = (String) headerItr.next();
                Message.HeaderValue hv = (Message.HeaderValue) headers.get(headerName);
                sb.append(hv.getName()).append(":").append(hv.getValue());

                // append parameters of the header
                Map parameters = hv.getParams();
                Set paramKeys = parameters.keySet();
                Iterator paramItr = paramKeys.iterator();
                while (paramItr.hasNext()) {
                    String paramName = (String) paramItr.next();
                    sb.append("; ").append(paramName).append("=").append(parameters.get(paramName)).append(";");
                }
                sb.append("\n");
            }
            sb.append("\n" + part.getContent());
            sb.append("\n" + XmlUtil.MULTIPART_BOUNDARY_PREFIX + boundary + "\n");
        }

        return sb.toString();
    }
}
