package com.l7tech.common.util;

import com.l7tech.server.ServerConfig;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class MultipartMessageReader {

    public static final int ATTACHMENTS_BUFFER_SIZE = 100000;
    public static final int SOAP_PART_BUFFER_SIZE = 30000;
    private static final int ATTACHMENT_BLOCK_SIZE = 4096;
    private boolean atLeastOneAttachmentParsed;
    private String multipartBoundary;
    private PushbackInputStream pushbackInputStream = null;
    private Map multipartParts = new HashMap();
    private final Logger logger = Logger.getLogger(getClass().getName());
    private byte[] attachmentsRawData = new byte[ATTACHMENTS_BUFFER_SIZE];
    int writeIndex = 0;
    String fileCacheId = null;
    FileOutputStream fileCache = null;
    String fileCacheName = null;

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

    public String getFileCacheId() {
        return fileCacheId;
    }

    public void setFileCacheId(String fileCacheId) {
        this.fileCacheId = fileCacheId;
    }

    public FileOutputStream getFileCache() {
        return fileCache;
    }

    public String getFileCacheName() {
        return fileCacheName;
    }

    public void closeFileCache() throws IOException {
        if(fileCache != null) {
            fileCache.close();
            fileCache = null;
        }
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
     * Parse the SOAP part of data in the input data stream.
     *
     * @return The SOAP part found. NULL if not found.
     * @throws IOException if there is error reading the input data stream.
     */
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

        part.content = xml.toString();

        // MIME part must has at least one header
        if(part.getHeaders().size() > 0) {
            part.setPostion(multipartParts.size());
            multipartParts.put(part.getHeader(XmlUtil.CONTENT_ID).getValue(), part);
        } else {
            if(part.getContent().length() > 0) {
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

    /**
     * Add line delimiter to the cache.
     *
     * @throws IOException if there is error reading the input data stream.
     */
    private void addLineDelimiter() throws IOException {

        byte[] delimiter = new byte[2];
        delimiter[0] = 0x0d;  // CR
        delimiter[1] = 0x0a;  // LF

        if(fileCache != null) {
            writeDataToFileCache(delimiter);
        } else {

            if(writeIndex + 4 > attachmentsRawData.length) {
                writeDataToFileCache(delimiter);
            } else {
                attachmentsRawData[writeIndex++] = delimiter[0];     // CR
                attachmentsRawData[writeIndex++] = delimiter[1];     // LF
            }
        }
    }

    /**
     * Store a raw header of attachments.
     *
     * @param line The header to be stored.
     * @throws IOException if there is error reading the input data stream.
     */
    private void storeRawHeader(String line) throws IOException {

        if(line == null) {
            throw new IllegalArgumentException("The header line cannot be NULL");
        }

        // NOTE: getBytes returns the byte[] which does not contains the white space characters, i.e. <CR><LF>
        byte[] rawHeader = line.getBytes();

        if(fileCache != null) {
            writeDataToFileCache(line.getBytes());
            addLineDelimiter();
        } else {
            // check if there is enough room
            if(writeIndex + rawHeader.length + 4 > attachmentsRawData.length) {
                writeDataToFileCache(line.getBytes());
                addLineDelimiter();
            } else {

                for(int i=0; i < rawHeader.length; i++) {
                    attachmentsRawData[writeIndex++] = rawHeader[i];
                }

                addLineDelimiter();
            }
        }
    }

    /**
     * Store the raw data of an attachment to the memory cache. If the memory cache is full, store the
     * data to the file cache including those in the memory cache (if any).
     *
     * @return int The length of the attachment in bytes.
     * @throws IOException if there is error reading the input data stream.
     */
    private int storeRawPartContent() throws IOException {

        int d;
        boolean boundaryFound = false;
        boolean crSeen = false;
        int startIndex = -1;
        int endIndex = -1;
        int oldWriteIndex = writeIndex;

        if(fileCache != null) {
            return storeRawPartContentToFileCache();
        }

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
                        if(isMultipartBoundaryFound(attachmentsRawData, startIndex, endIndex)) {
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
                } else {
                    // not the <cr><cf> sequence
                    crSeen = false;
                }
            }
        } while((writeIndex < attachmentsRawData.length) && (d != -1));

        if(!boundaryFound && (d != -1)) {
            return storeRawPartContentToFileCache(attachmentsRawData);
        } else {
            return (writeIndex - oldWriteIndex);
        }
    }

    private void writeDataToFileCache(byte[] data) throws IOException {

        writeDataToFileCache(data, 0, data.length);
    }

    /**
     * Write data to the file cache. Create the file cache if it's never been created.
     *
     * @param data The array of data to be stored in the file cache.
     * @param off  The starting position of the data array is off set by the "off" parameter.
     * @param len  The number of bytes to be stored in the file cache.
     * @throws IOException if there is error reading the input data stream.
     */
    private void writeDataToFileCache(byte[] data, int off, int len) throws IOException {

        if(fileCache == null) {
            if(fileCacheId == null) throw new RuntimeException("File name is NULL. Cannot create file for storing the raw attachments.");

            String propsPath = null;
            String propsKey = "ssg.etc";
            try {

                propsPath = ServerConfig.getInstance().getProperty(propsKey);
                if (propsPath != null && propsPath.length() > 0) {
                    File f = new File(propsPath);
                    if (!f.exists()) {
                        String errorMsg = "The directory " + propsPath + "is required for caching the big attachments but not found. Please ensure the SecureSpan gateway is properly installed.";
                        logger.warning(errorMsg);
                        throw new RuntimeException(errorMsg);
                    }

                    fileCacheName = propsPath + "/req-att-" + fileCacheId;
                    fileCache = new FileOutputStream(fileCacheName, true);
                } else {

                    String errorMsg = "The property " + propsKey + " is not defined. Please ensure the SecureSpan gateway is properly configured.";
                    logger.warning(errorMsg);
                    throw new RuntimeException(errorMsg);
                }
            } catch (FileNotFoundException e) {
                throw new RuntimeException("Unable to create a new file " + propsPath + fileCacheId);
            }
        }

        fileCache.write(data, off, len);
    }

    /**
     * Store the raw data of an attachment to the file cache. The initial part of the attachment
     * is read from the input parameter (data). The remaining part of the attachment is read from
     * the input data stream.
     * @param data  The array of bytes of data to be stored in the file cache.
     * @return int The length of the attachment in bytes.
     * @throws IOException if there is error reading the input data stream.
     */
    private int storeRawPartContentToFileCache(byte[] data) throws IOException {

        int count;

        // write the data in the temp buffer (the initial portion of the MIME part) to the file
        writeDataToFileCache(data);

        // write the remaining portion of the MIME part to the file
        count = storeRawPartContentToFileCache();

        return (data.length + count);
    }

    /**
     * Store the raw data of an attachment to the file cache. The data is read from the input data stream.
     *
     * @return int The length of the attachment in bytes.
     * @throws IOException if there is error reading the input data stream.
     */
    private int storeRawPartContentToFileCache() throws IOException {

        int count = 0;

        // store the remaining data of the attachment part (if any)
        // looking for the multipart boundary
        int d;
        boolean crSeen = false;
        boolean boundaryFound = false;
        byte[] buf = new byte[ATTACHMENT_BLOCK_SIZE];
        int startIndex = -1;      // the starting position of the data in between two <cr><lf> pairs
        int endIndex = -1;        // the ending position of the data in between two <cr><lf> pairs
        int index = 0;

        // read the first byte
        d = pushbackInputStream.read();

        // store the byte
        buf[index++] = (byte) d;

        while(!boundaryFound && (d != -1)) {

            while((index < buf.length) && (d != -1)) {
                // looking for <CR>
                if(d == 0x0d) {
                    crSeen = true;
                } else if (d == 0x0a) {
                    // if <CR><LF> sequenece found
                    if(crSeen) {

                        if(startIndex >= 0) {
                            endIndex = index;

                            // check if the multipart boundary found between the first <CR><LF> and the second <CR><LF>
                            if(isMultipartBoundaryFound(buf, startIndex, endIndex)) {
                                boundaryFound = true;
                                count += index;
                                writeDataToFileCache(buf, 0, index);
                                break;
                            } else {
                                // reset the indices
                                startIndex = endIndex;
                                endIndex = -1;
                            }

                        } else {
                            startIndex = index;
                        }

                        // reset flas
                        crSeen = false;
                    }
                } else {
                    // not the <cr><cf> sequence
                    crSeen = false;
                }
                // read the next byte
                d = pushbackInputStream.read();

                // store the byte
                buf[index++] = (byte) d;
            }

            // if the boundary NOT found when the buffer is full, store the data first and then continue
            if(!boundaryFound) {

                // NOTE: we should not include the case startIndex == 0 in this if statement,
                // otherwise the same data are being checked repeately.
                if(startIndex > 0) {
                    // store the data up to the last <cr><lf> in the buffer to the file cache
                    writeDataToFileCache(buf, 0, startIndex);

                    // move the rest of the data to the beginning of the buffer;
                    int numberOfBytesToMoveBack = index - startIndex;
                    int i = 0;
                    for (; i < numberOfBytesToMoveBack && i < buf.length; i++) {
                         buf[i] = buf[startIndex+i];
                    }

                    // update count
                    count += startIndex;

                    //reset buffer write index
                    index = i;

                    // reset the start index
                    startIndex = 0;

                } else {
                    // store all data in the buffer to the file cache
                    writeDataToFileCache(buf, 0, index);

                    // update count
                    count += index;

                   //reset buffer write index
                    index = 0;

                    // reset the start index
                    startIndex = -1;
                }

                endIndex = -1;

                // read the next byte
                d = pushbackInputStream.read();

                // store the byte
                buf[index++] = (byte) d;
            }
        }

        return count;
    }

    /**
     * Check if the multipart boundary is found in the buffer specified.
     * @param data  The data to be examined.
     * @param startIndex  The starting position of the data to be examined.
     * @param endIndex  The ending position of the data to be examined.
     * @return  TRUE if the multipart boundary is found. FALSE otherwise.
     */
    private boolean isMultipartBoundaryFound(byte[] data, int startIndex, int endIndex) {

        // check if the length of the two objects are the same
        StringBuffer sb = new StringBuffer();
        // convert the byte stream to string
        for(int i=0; i < endIndex - startIndex; i++ ) {
            sb.append((char)data[startIndex+i]);
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
                    part.headers.put(header.getName(), header);
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

    /**
     * Read a line from the input data stream
     * @return The line retrieved from the input data stream. -1 if no more data exists in the input stream.
     * @throws IOException if there is error reading the input data stream.
     */
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
}
