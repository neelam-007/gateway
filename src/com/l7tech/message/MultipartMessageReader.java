package com.l7tech.message;

import com.l7tech.common.util.XmlUtil;

import java.io.*;
import java.util.*;

import org.w3c.dom.Document;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class MultipartMessageReader {

    protected Document _document;
    protected boolean multipart;
    protected String multipartBoundary;
    protected BufferedReader breader = null;
    protected Map multipartParts = new HashMap();

    public MultipartMessageReader(BufferedReader reader, String multipartBoundary) {
        this.breader = reader;
        this.multipartBoundary = multipartBoundary;
    }

    public MultipartMessageReader(InputStream inputStream, String multipartBoundary) {
        this.breader = new BufferedReader(new InputStreamReader(inputStream));
        this.multipartBoundary = multipartBoundary;
    }

    public String getMultipartBoundary() {
        return multipartBoundary;
    }

    /**
     * Gets the XML part of the message from the provided reader.
     * <p>
     * Works with both multipart/related (SOAP with Attachments) as long as the first part is text/xml,
     * and of course without attachments.
     * <p>
     * @return the XML as a String
     * @throws java.io.IOException if a multipart message has an invalid format, or the content cannot be read
     */
    public Message.Part getSoapPart() throws IOException {
        return parseMultipart(0);
    }

    public Map getMessageAttachments() throws IOException {

        // parse all multiple parts
        parseAllMultiparts(breader);

        Map attachments = new HashMap();

        Set parts = multipartParts.keySet();
        Iterator itr = parts.iterator();
        while (itr.hasNext()) {
            Object o = (Object) itr.next();
            Object val  = (Object) multipartParts.get(o);
            if(val instanceof Message.Part) {
                Message.Part part = (Message.Part) val;
                if(part.getPosition() > 0) {
                    attachments.put(part.getHeader(XmlUtil.CONTENT_ID).value, part);
                }
            } else {
                throw new RuntimeException("The entry retrived from multipartParts object is not the type of com.l7tech.Message.Part");
            }
            System.out.println("The object is: " + o.toString());
        }
        return attachments;
    }

    protected Message.Part getMessagePart(int position) throws IOException {

        if(multipartParts.size() < position) {
             return parseMultipart(position);
        } else {
            return getMessagePartFromMap(position);
        }
    }

    private Message.Part getMessagePartFromMap(int position) {

        Message.Part part = null;

        Set keys = multipartParts.keySet();

        Iterator itr = keys.iterator();
        while (itr.hasNext()) {
            Message.Part currentPart = (Message.Part) multipartParts.get(itr.next());
            if(currentPart.getPosition() == position) {
                part = currentPart;
                break;
            }
        }
        return part;
    }

    static public Message.HeaderValue parseHeader(String header) throws IOException {
        StringTokenizer stok = new StringTokenizer(header, ":; ", false);
        Message.HeaderValue result = new Message.HeaderValue();
        while (stok.hasMoreTokens()) {
            String tok = stok.nextToken();
            int epos = tok.indexOf("=");
            if (epos == -1) {
                if (result.name == null)
                    result.name = tok;
                else if (result.value == null)
                    result.value = unquote(tok);
                else
                    throw new IOException("Encountered unexpected bare word '" + tok + "' in header");
            } else if (epos > 0) {
                String name = tok.substring(0,epos);
                String value = tok.substring(epos+1);
                value = unquote( value );

                result.params.put(name,value);
            } else throw new IOException("Invalid Content-Type header format ('=' at position " + epos + ")");
        }
        return result;
    }

    static private String unquote( String value ) throws IOException {
        if (value.startsWith("\"")) {
            if (value.endsWith("\"")) {
                value = value.substring(1,value.length()-1);
            } else throw new IOException("Invalid header format (mismatched quotes in value)");
        }
        return value;
    }

    /**
     * This parser only peels the the multipart message up to the part required.
     *
     * @param lastPart The part to be parsed
     * @return Part The part parsed
     * @throws IOException
     */
    private Message.Part parseMultipart(int lastPart) throws IOException {

        StringBuffer xml = new StringBuffer();
        Message.Part part = null;

        if(multipartParts.size() > 0 && multipartParts.size() > lastPart) {
            // the part to be retrived is already parsed
            return getMessagePartFromMap(lastPart);
        }

        // If it is the first time to parse soap part
        if(lastPart == 0) {
            String firstBoundary = breader.readLine();            
            if (!firstBoundary.equals(XmlUtil.MULTIPART_BOUNDARY_PREFIX + multipartBoundary)) throw new IOException("Initial multipart boundary not found");
        }

        for (int i = 0; i <= lastPart; i++) {

            part = new Message.Part();
            String line;
            boolean headers = true;
            while ((line = breader.readLine()) != null) {
                if (headers) {
                    if (line.length() == 0) {
                        headers = false;
                        continue;
                    }
                    Message.HeaderValue header = parseHeader(line);
                    part.headers.put(header.name, header);
                } else {
                    if (line.startsWith("--" + multipartBoundary)) {
                        // The boundary is on a line by itself so the previous content doesn't actually contain the last \n
                        // The next part is left in the reader for later
                        if (xml.length() > 0 && xml.charAt(xml.length()-1) == '\n')
                            xml.deleteCharAt(xml.length()-1);
                        break;
                    } else {
                        xml.append(line);
                        xml.append("\n");
                    }
                }
            }
            part.content = xml.toString();
            part.setPostion(multipartParts.size());
            multipartParts.put(part.getHeader(XmlUtil.CONTENT_ID).value, part);
        }
        return part;
    }

    /**
     * This parser only peels the the multipart message up to the part required.
     *
     * @param breader  The data source
     * @throws IOException
     */
    private void parseAllMultiparts(BufferedReader breader) throws IOException {

        StringBuffer xml = new StringBuffer();
        Message.Part part = null;

        String line;
        while ((line = breader.readLine()) != null) {
            part = new Message.Part();
            boolean headers = true;
            do {
                if (headers) {
                    if (line.length() == 0) {
                        headers = false;
                        continue;
                    }
                    Message.HeaderValue header = parseHeader(line);
                    part.headers.put(header.name, header);
                } else {
                    if (line.startsWith("--" + multipartBoundary)) {
                        // The boundary is on a line by itself so the previous content doesn't actually contain the last \n
                        // The next part is left in the reader for later
                        if (xml.length() > 0 && xml.charAt(xml.length() - 1) == '\n')
                            xml.deleteCharAt(xml.length() - 1);
                        break;
                    } else {
                        xml.append(line);
                        xml.append("\n");
                    }
                }
            } while ((line = breader.readLine()) != null);

            part.content = xml.toString();
            part.setPostion(multipartParts.size());
            multipartParts.put(part.getHeader(XmlUtil.CONTENT_ID).value, part);
        }
    }


}
