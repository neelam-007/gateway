package com.l7tech.common.util;

import java.io.IOException;
import java.util.*;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class MultipartUtil {
    public static final String ENCODING = "UTF-8";
    public static final String XML_VERSION = "1.0";

    static public void addModifiedSoapPart(StringBuffer sbuf, String modifiedSoapEnvelope, Part part, String boundary) {

        if(sbuf == null) throw new IllegalArgumentException("The StringBuffer is NULL");
        if(modifiedSoapEnvelope == null) throw new IllegalArgumentException("The modified SOAP envelope is NULL");
        if(part == null) throw new IllegalArgumentException("The SOAP part is NULL");
        if(boundary == null) throw new IllegalArgumentException("The StringBuffer is NULL");

        sbuf.append(XmlUtil.MULTIPART_BOUNDARY_PREFIX + boundary + "\r\n");
        Map headerMap = part.getHeaders();
        Set headerKeys = headerMap.keySet();
        Iterator headerItr = headerKeys.iterator();
        while(headerItr.hasNext()) {
            HeaderValue headerValue = (HeaderValue) headerMap.get(headerItr.next());
            sbuf.append(headerValue.getName()).append(": ").append(headerValue.getValue());

            // append parameters of the header
            Map parameters = headerValue.getParams();
            Set paramKeys = parameters.keySet();
            Iterator paramItr = paramKeys.iterator();
            while (paramItr.hasNext()) {
                String paramName = (String) paramItr.next();
                sbuf.append("; ").append(paramName).append("=").append(parameters.get(paramName));
            }
            sbuf.append("\r\n");
        }
        sbuf.append("\r\n").append(modifiedSoapEnvelope);
        sbuf.append("\r\n" + XmlUtil.MULTIPART_BOUNDARY_PREFIX + boundary + "\r\n");
    }

    static public HeaderValue parseHeader(String header) throws IOException {
        StringTokenizer stok = new StringTokenizer(header, ":; ", false);
        String name = null;
        String value = null;
        Map params = new HashMap();
        while (stok.hasMoreTokens()) {
            String tok = stok.nextToken();
            int epos = tok.indexOf("=");
            if (epos == -1) {
                if (name == null)
                    name = tok;
                else if (value == null)
                    value = unquote(tok.trim());
                else
                    throw new IOException("Encountered unexpected bare word '" + tok + "' in header");
            } else if (epos > 0) {
                String aname = tok.substring(0,epos);
                String avalue = tok.substring(epos+1);
                avalue = unquote(avalue.trim());

                params.put(aname.trim(),avalue.trim());
            } else throw new IOException("Invalid Content-Type header format ('=' at position " + epos + ")");
        }
        return new HeaderValue(name.trim(), value.trim(), params);
    }

    static public String unquote( String value ) throws IOException {

        if(value == null) return value;

        if (value.startsWith("\"")) {
            if (value.endsWith("\"")) {
                value = value.substring(1,value.length()-1);
            } else throw new IOException("Invalid header format (mismatched quotes in value)");
        }
        return value;
    }

    static public String removeConentIdBrackets(String value) throws IOException {

        if(value == null) return value;

        if (value.startsWith("<")) {
            if (value.endsWith(">")) {
                value = value.substring(1,value.length()-1);
            } else throw new IOException("Invalid Content Id format (mismatched brackets in value)");
        }
        return value;
    }

    public static class HeaderValue {
        private final String name;
        private final String value;
        private final Map params;

        public HeaderValue(String name, String value, Map params) {
            this.name = name;
            this.value = value;
            this.params = params == null ? new HashMap() : params;
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }

        private Map getParams() {
            return params;
        }

        public Object getParam(Object key) {
            return params.get(key);
        }

        public String toString() {
            StringBuffer strongbad = new StringBuffer();
            strongbad.append(name).append(": ").append(value);
            for ( Iterator i = params.keySet().iterator(); i.hasNext(); ) {
                String key = (String)i.next();
                String value = (String)params.get(key);
                strongbad.append("; ").append(key).append("=\"").append(value).append("\"");
            }
            return strongbad.toString();
        }
    }

    public static class Part {
        public HeaderValue getHeader(String name) {
            return (HeaderValue)headers.get(name);
        }

        public int getPosition() {
            return position;
        }

        public void setPostion(int position) {
            this.position = position;
        }

        public String getContent() {
            return content;
        }

        public Map getHeaders() {
            return headers;
        }

        public int getContentLength() {
            if(position == 0) {
                return content.length();
            }
            return contentLength;
        }

        public void setContentLength(int length) {
            contentLength = length;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public boolean isValidated() {
            return validated;
        }

        public void setValidated(boolean validated) {
            this.validated = validated;
        }

        protected String content;
        protected Map headers = new TreeMap(String.CASE_INSENSITIVE_ORDER);
        protected int position;
        protected boolean validated = false;
        int contentLength;
    }
}
