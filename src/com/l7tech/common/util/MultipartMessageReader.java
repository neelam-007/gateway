package com.l7tech.common.util;

import org.w3c.dom.Document;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class MultipartMessageReader {

    private boolean atLeastOneAttachmentParsed;
    private String multipartBoundary;
    private PushbackInputStream pushbackInputStream = null;
    private Map multipartParts = new HashMap();
    private final Logger logger = Logger.getLogger(getClass().getName());

    public MultipartMessageReader(InputStream inputStream, String multipartBoundary) {
        pushbackInputStream = new PushbackInputStream(inputStream, 10000);
        this.multipartBoundary = multipartBoundary;
    }

    public String getMultipartBoundary() {
        return multipartBoundary;
    }

    public PushbackInputStream getPushbackInputStream() {
        return pushbackInputStream;
    }

    public boolean isAtLeastOneAttachmentParsed() {
        return atLeastOneAttachmentParsed;
    }

    public boolean hasNextMessagePart() throws IOException {
        if(getMessagePart(multipartParts.size()) == null) {
            return false;
        }
        return true;
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
    public MultipartUtil.Part getSoapPart() throws IOException {
        return parseMultipart(0);
    }

    /**
     * Get all attachements of the message.
     *
     * @return Map a list of attachments.
     * @throws IOException
     */
    public Map getMessageAttachments() throws IOException {

        // parse all multiple parts
        parseAllMultiparts();

        Map attachments = new HashMap();

        Set parts = multipartParts.keySet();
        Iterator itr = parts.iterator();
        while (itr.hasNext()) {
            Object o = (Object) itr.next();
            Object val  = (Object) multipartParts.get(o);
            if(val instanceof MultipartUtil.Part) {
                MultipartUtil.Part part = (MultipartUtil.Part) val;
                if(part.getPosition() > 0) {
                    attachments.put(part.getHeader(XmlUtil.CONTENT_ID).getValue(), part);
                }
            } else {
                throw new RuntimeException("The entry retrived from multipartParts object is not the type of com.l7tech.Message.Part");
            }
        }
        return attachments;
    }

    /**
     * This parser only peels the the multipart message up to the part required.
     *
     * @param position The position of the part to be parsed
     * @return Part The part parsed.  Return NULL if not found.
     * @throws IOException
     */
    public MultipartUtil.Part getMessagePart(int position) throws IOException {

        if(multipartParts.size() <= position) {
             return parseMultipart(position);
        } else {
            return getMessagePartFromMap(position);
        }
    }

    /**
     * This parser only peels the the multipart message up to the part required.
     *
     * @param cid The content Id of the part to be parsed
     * @return Part The part parsed.  Return NULL if not found.
     * @throws IOException
     */
    public MultipartUtil.Part getMessagePart(String cid) throws IOException {
        return parseMultipart(cid);
    }

    /**
     * Get the part from the parsed part list given the position of the part.
     *
     * @param position  The position of the part to be retrieved.
     * @return Part The part parsed.  Return NULL if not found.
     */
    private MultipartUtil.Part getMessagePartFromMap(int position) {

        MultipartUtil.Part part = null;

        Set keys = multipartParts.keySet();

        Iterator itr = keys.iterator();
        while (itr.hasNext()) {
            MultipartUtil.Part currentPart = (MultipartUtil.Part) multipartParts.get(itr.next());
            if(currentPart.getPosition() == position) {
                part = currentPart;
                break;
            }
        }
        return part;
    }

   /**
     * Get the part from the parsed part list given the content Id of the part.
     *
     * @param cid  The content id of the part to be retrieved.
     * @return Part The part parsed.  Return NULL if not found.
     */
    private MultipartUtil.Part getMessagePartFromMap(String cid) {

        MultipartUtil.Part part = null;

        Set keys = multipartParts.keySet();

        Iterator itr = keys.iterator();
        while (itr.hasNext()) {
            String partName = (String) itr.next();
            if(cid.equals(partName)) {
                MultipartUtil.Part currentPart = (MultipartUtil.Part) multipartParts.get(partName);
                part = currentPart;
                break;
            }
        }
        return part;
    }

    /**
     * This parser only peels the the multipart message up to the part required.
     *
     * @param lastPartPosition The position of the part to be parsed
     * @return Part The part parsed.  Return NULL if not found.
     * @throws IOException
     */
    private MultipartUtil.Part parseMultipart(int lastPartPosition) throws IOException {

        StringBuffer xml = new StringBuffer();
        MultipartUtil.Part part = null;

        if(multipartParts.size() > 0 && multipartParts.size() > lastPartPosition) {
            // the part to be retrived is already parsed
            return getMessagePartFromMap(lastPartPosition);
        }

        // If it is the first time to parse soap part
        if(lastPartPosition == 0) {

            String firstBoundary = readLine();
            if (!firstBoundary.equals(XmlUtil.MULTIPART_BOUNDARY_PREFIX + multipartBoundary)) throw new IOException("Initial multipart boundary not found");
        }

        String line;
        while((multipartParts.size() <= lastPartPosition) && ((line = readLine()) != null)) {

            part = new MultipartUtil.Part();
            boolean headers = true;
            do {
                if (headers) {
                    if (line.length() == 0) {
                        headers = false;
                        continue;
                    }
                    MultipartUtil.HeaderValue header = MultipartUtil.parseHeader(line);
                    part.headers.put(header.getName(), header);
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
            } while ((line = readLine()) != null);

            part.content = xml.toString();

            // MIME part must has at least one header
            if(part.getHeaders().size() > 0) {
                part.setPostion(multipartParts.size());
                multipartParts.put(part.getHeader(XmlUtil.CONTENT_ID).getValue(), part);
            } else {
                if(part.content.trim().length() > 0) {
                    logger.info("An incomplete MIME part is received. Headers not found");
                }
                part = null;
            }
        }
        if(multipartParts.size() >= 2 ) atLeastOneAttachmentParsed = true;
        return part;
    }

    /**
     * This parser only peels the the multipart message up to the part required.
     *
     * @param cid The part to be parsed given the content id of the part.
     * @return Part The part parsed. Return NULL if not found.
     * @throws IOException
     */
    private MultipartUtil.Part parseMultipart(String cid) throws IOException {

        StringBuffer xml = new StringBuffer();
        MultipartUtil.Part part = null;
        boolean partFound = false;

        if( cid == null) throw new IllegalArgumentException("Cannot find the MIME part. The content id cannot NULL");

        // the part to be retrived is already parsed
        if((part = getMessagePartFromMap(cid)) != null) return part;
        String line;

        while ((line = readLine()) != null)  {
            part = new MultipartUtil.Part();
            boolean headers = true;
            do {
                if (headers) {
                    if (line.length() == 0) {
                        headers = false;
                        continue;
                    }
                    MultipartUtil.HeaderValue header = MultipartUtil.parseHeader(line);
                    part.headers.put(header.getName(), header);
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
            } while ((line = readLine()) != null);

            // MIME part must has at least one header
            if(part.getHeaders().size() > 0) {
                part.content = xml.toString();
                part.setPostion(multipartParts.size());
                String contentId = part.getHeader(XmlUtil.CONTENT_ID).getValue();
                multipartParts.put(contentId, part);
                if(cid.endsWith(contentId)) {
                    // the requested MIME part is found, stop here.
                    partFound = true;
                    break;
                }
            } else {
                 if(part.content.trim().length() > 0) {
                    logger.info("An incomplete MIME part is received. Headers not found");
                }
            }
        }

        if(multipartParts.size() >= 2 ) atLeastOneAttachmentParsed = true;

        if(partFound) {
            return part;
        } else {
            return null;
        }
    }

    private String readLine() throws IOException {
        boolean newlineFound = false;
        long byteCount = 0;
        byte[] buf = new byte[256];


        StringBuffer sb = new StringBuffer();
        do {
            int read = pushbackInputStream.read(buf, 0, buf.length);
            if(read <= 0) {
                if(byteCount > 0) {
                    return sb.toString();
                } else {
                    return null;
                }
            }

            for (int i = 0; i < read; i++) {
                sb.append((char)buf[i]);
                if(buf[i] == '\n') {
                    newlineFound = true;
                    // push the rest back to the stream
                    pushbackInputStream.unread(buf, i+1, read-(i+1));
                    break;
                }
            }
        } while(!newlineFound);

        return sb.toString().trim();
    }

    /**
     * This parser only peels the the multipart message up to the part required.
     *
     * @throws IOException
     */
    private void parseAllMultiparts() throws IOException {

        StringBuffer xml = new StringBuffer();
        MultipartUtil.Part part = null;

        String line;
        while ((line = readLine()) != null) {
            part = new MultipartUtil.Part();
            boolean headers = true;
            do {
                if (headers) {
                    if (line.length() == 0) {
                        headers = false;
                        continue;
                    }
                    MultipartUtil.HeaderValue header = MultipartUtil.parseHeader(line);
                    part.headers.put(header.getName(), header);
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
            } while ((line = readLine()) != null);

            part.content = xml.toString();
            part.setPostion(multipartParts.size());
            multipartParts.put(part.getHeader(XmlUtil.CONTENT_ID).getValue(), part);
        }
    }
}
