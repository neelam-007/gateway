package com.l7tech.server.attachments;

import com.l7tech.server.ServerConfig;
import com.l7tech.common.attachments.MultipartMessageReader;
import com.l7tech.common.util.MultipartUtil;
import com.l7tech.common.util.XmlUtil;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class ServerMultipartMessageReader extends MultipartMessageReader {

    private final Logger logger = Logger.getLogger(getClass().getName());

    public ServerMultipartMessageReader(InputStream inputStream, String multipartBoundary) {
        pushbackInputStream = new PushbackInputStream(inputStream, SOAP_PART_BUFFER_SIZE);
        this.multipartBoundary = multipartBoundary;
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
     * Get all attachements of the message.
     *
     * @return Map a list of attachments.
     * @throws IOException
     */
    public Map getMessageAttachments() throws IOException {

        Map attachments = new HashMap();

        Set parts = multipartParts.keySet();
        Iterator itr = parts.iterator();
        while (itr.hasNext()) {
            Object o = (Object) itr.next();
            Object val  = (Object) multipartParts.get(o);
            if(val instanceof MultipartUtil.Part) {
                MultipartUtil.Part part = (MultipartUtil.Part) val;

                // excluding the SOAP part
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

    public int getRawAttachmentsSize() {
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
     * Get the part from the parsed part list given the content Id of the part.
     *
     * @param cid  The content id of the part to be retrieved.
     * @return Part The part parsed.  Return NULL if not found.
     * @throws IOException if there is error reading the input data stream.
     */
    private MultipartUtil.Part getMessagePartFromMap(String cid) throws IOException {

        MultipartUtil.Part part = null;

        Set keys = multipartParts.keySet();

        Iterator itr = keys.iterator();
        while (itr.hasNext()) {
            String partName = (String) itr.next();
            if(validateContentId(cid, partName))  {
                MultipartUtil.Part currentPart = (MultipartUtil.Part) multipartParts.get(partName);
                part = currentPart;
                break;
            }
        }
        return part;
    }

    /**
     * Validate the content Id specified.
     * @param cid The content Id to be validated.
     * @param cidInPartHeader The content Id found in the header of the MIME part.
     * @return TRUE if the cid equals to the cidInPartHeader or matches the last part of the cidInPartHeader.
     * @throws IOException if there is error reading the input data stream.
     */
    private boolean validateContentId(String cid, String cidInPartHeader) throws IOException {
        if(cid.equals(cidInPartHeader) ||
                   cid.endsWith(MultipartUtil.removeConentIdBrackets(cidInPartHeader))) {
            return true;
        }
        return false;
    }

    /**
     * This parser only peels the the multipart message up to the part required.
     *
     * @param cid The part to be parsed given the content id of the part.
     * @return Part The part parsed. Return NULL if not found.
     * @throws IOException if there is error reading the input data stream.
     */
    private MultipartUtil.Part parseMultipart(String cid) throws IOException {

        MultipartUtil.Part part = null;
        boolean partFound = false;

        if( cid == null) throw new IllegalArgumentException("Cannot find the MIME part. The content id cannot NULL");

        // the part to be retrived is already parsed
        if((part = getMessagePartFromMap(cid)) != null) return part;
        String line;

        while (!partFound && (line = readLine()) != null)  {
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
                    part.getHeaders().put(header.getName(), header);
                }
            } while ((line = readLine()) != null);

            // MIME part must has at least one header
            if(part.getHeaders().size() > 0) {
                part.setPostion(multipartParts.size());
                String contentId = part.getHeader(XmlUtil.CONTENT_ID).getValue();
                multipartParts.put(contentId, part);
                if(validateContentId(cid, contentId)) {
                    // the requested MIME part is found, stop here.
                    partFound = true;
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

}
