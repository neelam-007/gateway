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

    static public String addAttachments(String requestXml, Map attachments, String boundary) throws IOException {
        StringBuffer sb = new StringBuffer();

        // append the SOAP part
        sb.append(requestXml);

        // add attachments
        Set attachmentKeys = attachments.keySet();
        Iterator itr = attachmentKeys.iterator();
        while (itr.hasNext()) {
            Object o = (Object) itr.next();
            Message.Part part = (Message.Part) attachments.get(o);

            sb.append("\n" + XmlUtil.MULTIPART_BOUNDARY_PREFIX + boundary + "\n");

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
        }

        return sb.toString();
    }
}
