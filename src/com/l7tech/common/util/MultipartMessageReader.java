package com.l7tech.common.util;

import org.w3c.dom.Document;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.*;
import java.util.logging.Logger;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class MultipartMessageReader {

    public static final int ATTACHMENTS_BUFFER_SIZE = 1000000;
    public static final int SOAP_PART_BUFFER_SIZE = 10000;
    private boolean atLeastOneAttachmentParsed;
    private String multipartBoundary;
    private PushbackInputStream pushbackInputStream = null;
    private Map multipartParts = new HashMap();
    private final Logger logger = Logger.getLogger(getClass().getName());
    private byte[] attachmentsRawData = new byte[ATTACHMENTS_BUFFER_SIZE];
    int writeIndex = 0;

    public MultipartMessageReader(InputStream inputStream, String multipartBoundary) {
        pushbackInputStream = new PushbackInputStream(inputStream, SOAP_PART_BUFFER_SIZE);
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

    public byte[] getRawAttachments() {
        return attachmentsRawData;
    }

    public int getgetRawAttachmentsSize() {
        return writeIndex;
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
            if(position == 0) {
                 return parseSoapPart();
            } else {
                 return parseMultipart(position);
            }
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

    private MultipartUtil.Part parseSoapPart() throws IOException {
        StringBuffer xml = new StringBuffer();
        MultipartUtil.Part part = null;

        if(multipartParts.size() > 0 ) {
            // the part to be retrived is already parsed
            return getMessagePartFromMap(0);
        }

        // If it is the first time to parse soap part
        String firstBoundary = null;

        while((firstBoundary = readLine()) != null) {
            if(firstBoundary.trim().equals("")) continue;
            if (firstBoundary.equals(XmlUtil.MULTIPART_BOUNDARY_PREFIX + multipartBoundary)) {
                break;
            } else {
                throw new IOException("Initial multipart boundary not found");
            }
        }

        String line;

        part = new MultipartUtil.Part();
        boolean headers = true;

        while ((line = readLine()) != null) {
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
        }

        part.content = xml.toString().getBytes();

        // MIME part must has at least one header
        if(part.getHeaders().size() > 0) {
            part.setPostion(multipartParts.size());
            multipartParts.put(part.getHeader(XmlUtil.CONTENT_ID).getValue(), part);
        } else {
            if(part.getContent().length > 0) {
                logger.info("An incomplete MIME part is received. Headers not found");
            }
            part = null;
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

        MultipartUtil.Part part = null;

        if(lastPartPosition == 0) {
            return parseSoapPart();
        }

        if(multipartParts.size() > 0 && multipartParts.size() > lastPartPosition) {
            // the part to be retrived is already parsed
            return getMessagePartFromMap(lastPartPosition);
        }

        String line;
        while((multipartParts.size() <= lastPartPosition) && ((line = readLine()) != null)) {

            part = new MultipartUtil.Part();
            boolean headers = true;
            do {
                if (headers) {
                    if (line.length() == 0) {
                        headers = false;
                        addLineDelimiter();
                        part.setContentLength(storeRawPartContent());
                        break;
                    }
                    storeRawHeader(line);
                    MultipartUtil.HeaderValue header = MultipartUtil.parseHeader(line);
                    part.headers.put(header.getName(), header);
                }
            } while ((line = readLine()) != null);

            // MIME part must has at least one header
            if(part.getHeaders().size() > 0) {
                part.setPostion(multipartParts.size());
                multipartParts.put(part.getHeader(XmlUtil.CONTENT_ID).getValue(), part);
            }
        }
        if(multipartParts.size() >= 2 ) atLeastOneAttachmentParsed = true;
        return part;
    }

    private void addLineDelimiter() {

        if(writeIndex + 4 > attachmentsRawData.length) {
            throw new RuntimeException("The size of attachment(s) exceeds the buffer size. Unable to handle the request.");
        } else {
            attachmentsRawData[writeIndex++] = 0x0d;     // CR
            attachmentsRawData[writeIndex++] = 0x0a;     // LF
        }
    }

    private void storeRawHeader(String line) {

        if(line == null) {
            throw new IllegalArgumentException("The header line cannot be NULL");
        }

        byte[] rawHeader = line.getBytes();

        // check if there is enough room
        if(writeIndex + rawHeader.length + 2 > attachmentsRawData.length) {
            throw new RuntimeException("The size of attachment(s) exceeds the buffer size. Unable to handle the request.");
        }

        for(int i=0; i < rawHeader.length; i++) {
            attachmentsRawData[writeIndex++] = rawHeader[i];
        }

        addLineDelimiter();
    }

    private int storeRawPartContent() throws IOException {

        int d;
        boolean boundaryFound = false;
        boolean crSeen = false;
        int startIndex = -1;
        int endIndex = -1;
        int oldWriteIndex = writeIndex;

        // looking for the multipart boundary
        do {
            d = pushbackInputStream.read();

            // store the byte
            attachmentsRawData[writeIndex++] = (byte) d;

            // looking for <CR>
            if(d == 0x0d) {
                crSeen = true;
            } else if (d == 0x0a) {
                // if <CR><LF> sequenece found
                if(crSeen) {

                    if(startIndex >= 0) {
                        endIndex = writeIndex;

                        // check if the multipart boundary found between the first <CR><LF> and the second <CR><LF>
                        if(isMultipartBoundaryFound(startIndex, endIndex)) {
                            boundaryFound = true;
                            break;
                        } else {
                            // reset the indices
                            startIndex = endIndex;
                            endIndex = -1;
                        }

                    } else {
                        startIndex = writeIndex;
                    }

                    // reset flas
                    crSeen = false;
                }
            }
        } while((writeIndex < attachmentsRawData.length - 4) && (d != -1));

        if(!boundaryFound && (d == -1)) {
            throw new RuntimeException("The size of attachment(s) exceeds the buffer size. Unable to handle the request.");
        }
        return (writeIndex - oldWriteIndex);
    }

    private boolean isMultipartBoundaryFound(int startIndex, int endIndex) {

        // check if the length of the two objects are the same
        if((multipartBoundary.length()) + 2 != (endIndex - startIndex - 4)) return false;

        StringBuffer sb = new StringBuffer();
        // convert the byte stream to string
        for(int i=0; i < endIndex - startIndex; i++ ) {
            sb.append((char)attachmentsRawData[startIndex+i]);
        }

        if(sb.toString().trim().startsWith("--" + multipartBoundary)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * This parser only peels the the multipart message up to the part required.
     *
     * @param cid The part to be parsed given the content id of the part.
     * @return Part The part parsed. Return NULL if not found.
     * @throws IOException
     */
    private MultipartUtil.Part parseMultipart(String cid) throws IOException {

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
                        addLineDelimiter();
                        part.setContentLength(storeRawPartContent());
                        break;
                    }
                    storeRawHeader(line);
                    MultipartUtil.HeaderValue header = MultipartUtil.parseHeader(line);
                    part.headers.put(header.getName(), header);
                }
            } while ((line = readLine()) != null);

            // MIME part must has at least one header
            if(part.getHeaders().size() > 0) {
                part.setPostion(multipartParts.size());
                String contentId = part.getHeader(XmlUtil.CONTENT_ID).getValue();
                multipartParts.put(contentId, part);
                if(cid.endsWith(contentId) ||
                   cid.endsWith(MultipartUtil.removeConentIdBrackets(contentId))) {
                    // the requested MIME part is found, stop here.
                    partFound = true;
                    break;
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

            part.content = xml.toString().getBytes();
            part.setPostion(multipartParts.size());
            multipartParts.put(part.getHeader(XmlUtil.CONTENT_ID).getValue(), part);
        }
    }
}
