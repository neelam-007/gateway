package com.l7tech.server.attachments;

import com.l7tech.common.mime.MimeHeader;
import com.l7tech.common.mime.MimeUtil;
import com.l7tech.common.mime.MultipartMessageReader;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.server.ServerConfig;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * // TODO make this go away
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

    protected String getFileCachePath() {
        String propsKey = "ssg.etc";
        String propsPath = ServerConfig.getInstance().getProperty(propsKey);
        if (propsPath != null && propsPath.length() > 0) {
            File f = new File(propsPath);
            if (!f.exists()) {
                String errorMsg = "The directory " + propsPath + "is required for caching the big attachments but not found. Please ensure the SecureSpan gateway is properly installed.";
                logger.severe(errorMsg);
                throw new RuntimeException(errorMsg);
            }
        } else {

            String errorMsg = "The property " + propsKey + " is not defined. Please ensure the SecureSpan gateway is properly configured.";
            logger.severe(errorMsg);
            throw new RuntimeException(errorMsg);
        }
        return propsPath;
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
            if(val instanceof PartInfo) {
                PartInfo part = (PartInfo) val;

                // excluding the SOAP part
                if(part.getPosition() > 0) {
                    attachments.put(part.getHeader(MimeUtil.CONTENT_ID).getValue(), part);
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
    public PartInfo getMessagePart(int position) throws IOException {

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
    public PartInfo getMessagePart(String cid) throws IOException {
        return parseMultipart(cid);
    }

    /**
     * Get the part from the parsed part list given the content Id of the part.
     *
     * @param cid  The content id of the part to be retrieved.
     * @return Part The part parsed.  Return NULL if not found.
     * @throws IOException if there is error reading the input data stream.
     */
    private PartInfo getMessagePartFromMap(String cid) throws IOException {

        PartInfo part = null;

        Set keys = multipartParts.keySet();

        Iterator itr = keys.iterator();
        while (itr.hasNext()) {
            String partName = (String) itr.next();
            if(validateContentId(cid, partName))  {
                PartInfo currentPart = (PartInfo) multipartParts.get(partName);
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
                   cid.endsWith(MimeUtil.removeConentIdBrackets(cidInPartHeader))) {
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
    private PartInfo parseMultipart(String cid) throws IOException {

        PartInfo part = null;
        boolean partFound = false;

        if( cid == null) throw new IllegalArgumentException("Cannot find the MIME part. The content id cannot NULL");

        // the part to be retrived is already parsed
        if((part = getMessagePartFromMap(cid)) != null) return part;
        String line;

        while (!partFound && (line = readLine()) != null)  {
            //part = new PartInfo();
            boolean headers = true;
            do {
                if (headers) {
                    if (line.length() == 0) {
                        headers = false;
                        addLineDelimiter();
//                        part.setContentLength(storeRawPartContent());
                        break;
                    }
                    storeRawHeader(line);
                    MimeHeader header = MimeUtil.parseHeader(line);
                    part.getHeaders().add(header);
                }
            } while ((line = readLine()) != null);

            // MIME part must has at least one header
            if(part.getHeaders().size() > 0) {
//                part.setPostion(multipartParts.size());
                String contentId = part.getHeader(MimeUtil.CONTENT_ID).getValue();
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
