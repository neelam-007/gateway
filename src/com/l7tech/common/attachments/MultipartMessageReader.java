package com.l7tech.common.attachments;

import com.l7tech.common.util.MultipartUtil;
import com.l7tech.common.util.XmlUtil;

import java.io.*;
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
abstract public class MultipartMessageReader {
    public static final int ATTACHMENTS_BUFFER_SIZE = 100000;
    public static final int SOAP_PART_BUFFER_SIZE = 30000;
    protected static final int ATTACHMENT_BLOCK_SIZE = 4096;
    protected boolean atLeastOneAttachmentParsed;
    protected String multipartBoundary;
    protected PushbackInputStream pushbackInputStream = null;
    protected Map multipartParts = new HashMap();
    protected byte[] attachmentsRawData = new byte[ATTACHMENTS_BUFFER_SIZE];
    protected int writeIndex = 0;
    String fileCacheId = null;
    String fileCachePath = null;
    FileOutputStream fileCache = null;
    String fileCacheName = null;
    protected boolean bufferFlushed = false;
    private final Logger logger = Logger.getLogger(getClass().getName());

    abstract protected String getFileCachePath();

    public String getMultipartBoundary() {
        return multipartBoundary;
    }

    public PushbackInputStream getPushbackInputStream() {
        return pushbackInputStream;
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
        if (fileCache != null) {
            fileCache.close();
        }
    }

    public byte[] getMemoryCache() {
        return attachmentsRawData;
    }

    public int getMemoryCacheDataLength() {
        return writeIndex;
    }

    /**
     * Gets the XML part of the message from the provided reader.
     * <p/>
     * Works with both multipart/related (SOAP with Attachments) as long as the first part is text/xml,
     * and of course without attachments.
     * <p/>
     *
     * @return the XML as a String
     * @throws java.io.IOException if a multipart message has an invalid format, or the content cannot be read
     */
    public MultipartUtil.Part getSoapPart() throws IOException {
        return parseMultipart(0);
    }

    /**
     * Get the part from the parsed part list given the position of the part.
     *
     * @param position The position of the part to be retrieved.
     * @return Part The part parsed.  Return NULL if not found.
     */
    protected MultipartUtil.Part getMessagePartFromMap(int position) {

        MultipartUtil.Part part = null;

        Set keys = multipartParts.keySet();

        Iterator itr = keys.iterator();
        while (itr.hasNext()) {
            MultipartUtil.Part currentPart = (MultipartUtil.Part)multipartParts.get(itr.next());
            if (currentPart.getPosition() == position) {
                part = currentPart;
                break;
            }
        }
        return part;
    }

    /**
     * Parse the SOAP part of data in the input data stream.
     *
     * @return The SOAP part found. NULL if not found.
     * @throws java.io.IOException if there is error reading the input data stream.
     */
    protected MultipartUtil.Part parseSoapPart() throws IOException {
        StringBuffer xml = new StringBuffer();
        MultipartUtil.Part part = null;

        if (multipartParts.size() > 0) {
            // the part to be retrived is already parsed
            return getMessagePartFromMap(0);
        }

        // If it is the first time to parse soap part
        String firstBoundary = null;

        while ((firstBoundary = readLine()) != null) {
            if (firstBoundary.trim().equals("")) continue;
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
                part.getHeaders().put(header.getName(), header);
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
        }

        part.setContent(xml.toString());

        // MIME part must has at least one header
        if (part.getHeaders().size() > 0) {
            part.setPostion(multipartParts.size());
            multipartParts.put(part.getHeader(XmlUtil.CONTENT_ID).getValue(), part);
        } else {
            if (part.getContent().length() > 0) {
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
     * @throws java.io.IOException
     */
    protected MultipartUtil.Part parseMultipart(int lastPartPosition) throws IOException {

        MultipartUtil.Part part = null;

        if (lastPartPosition == 0) {
            return parseSoapPart();
        }

        if (multipartParts.size() > 0 && multipartParts.size() > lastPartPosition) {
            // the part to be retrived is already parsed
            return getMessagePartFromMap(lastPartPosition);
        }

        String line;
        while ((multipartParts.size() <= lastPartPosition) && ((line = readLine()) != null)) {

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
            if (part.getHeaders().size() > 0) {
                part.setPostion(multipartParts.size());
                multipartParts.put(part.getHeader(XmlUtil.CONTENT_ID).getValue(), part);
            }
        }
        if (multipartParts.size() >= 2) atLeastOneAttachmentParsed = true;
        return part;
    }

    /**
     * Add line delimiter to the cache.
     *
     * @throws java.io.IOException if there is error reading the input data stream.
     */
    protected void addLineDelimiter() throws IOException {

        byte[] delimiter = new byte[2];
        delimiter[0] = 0x0d;  // CR
        delimiter[1] = 0x0a;  // LF

        if (!bufferFlushed) {
            if (writeIndex + 4 > attachmentsRawData.length) {
                // write the data in the buffer to the file cache
                writeDataToFileCache(attachmentsRawData, 0, writeIndex);
                bufferFlushed = true;

                // write the header to the file cache
                writeDataToFileCache(delimiter);
            } else {
                attachmentsRawData[writeIndex++] = delimiter[0];     // CR
                attachmentsRawData[writeIndex++] = delimiter[1];     // LF
            }
        } else {
            // write the header to the file cache
            writeDataToFileCache(delimiter);
        }
    }

    /**
     * Store a raw header of attachments.
     *
     * @param line The header to be stored.
     * @throws java.io.IOException if there is error reading the input data stream.
     */
    protected void storeRawHeader(String line) throws IOException {

        if (line == null) {
            throw new IllegalArgumentException("The header line cannot be NULL");
        }

        // NOTE: getBytes returns the byte[] which does not contains the white space characters, i.e. <CR><LF>
        byte[] rawHeader = line.getBytes();

        // check if there is enough room
        if (!bufferFlushed) {
            if (writeIndex + rawHeader.length + 4 > attachmentsRawData.length) {

                // write the data in the buffer to the file cache
                writeDataToFileCache(attachmentsRawData, 0, writeIndex);
                bufferFlushed = true;

                // write the header to the file cache
                writeDataToFileCache(line.getBytes());
                addLineDelimiter();
            } else {
                for (int i = 0; i < rawHeader.length; i++) {
                    attachmentsRawData[writeIndex++] = rawHeader[i];
                }

                addLineDelimiter();
            }
        } else {
            writeDataToFileCache(line.getBytes());
            addLineDelimiter();
        }
    }

    /**
     * Store the raw data of an attachment to the memory cache. If the memory cache is full, store the
     * data to the file cache including those in the memory cache (if any).
     *
     * @return int The length of the attachment in bytes.
     * @throws java.io.IOException if there is error reading the input data stream.
     */
    protected int storeRawPartContent() throws IOException {

        int d;
        boolean boundaryFound = false;
        boolean crSeen = false;
        int startIndex = -1;
        int endIndex = -1;
        int oldWriteIndex = writeIndex;

        if (fileCache != null) {
            return storeRawPartContentToFileCache();
        }

        // looking for the multipart boundary
        do {
            d = pushbackInputStream.read();

            // store the byte
            attachmentsRawData[writeIndex++] = (byte)d;

            // looking for <CR>
            if (d == 0x0d) {
                crSeen = true;
            } else if (d == 0x0a) {
                // if <CR><LF> sequenece found
                if (crSeen) {

                    if (startIndex >= 0) {
                        endIndex = writeIndex;

                        // check if the multipart boundary found between the first <CR><LF> and the second <CR><LF>
                        if (isMultipartBoundaryFound(attachmentsRawData, startIndex, endIndex)) {
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
        } while ((writeIndex < attachmentsRawData.length) && (d != -1));

        // if the buffer is full but boundary not found and some more data from the input stream
        if (!boundaryFound && (d != -1)) {
            int count;
            if (startIndex > 2) {
                // push back the data from the last <cr><lf> seen
                pushbackInputStream.unread(attachmentsRawData, startIndex - 2, attachmentsRawData.length - startIndex + 2);
                count = storeRawPartContentToFileCache(attachmentsRawData, 0, startIndex - 2) - oldWriteIndex;
            } else {
                count = storeRawPartContentToFileCache(attachmentsRawData) - oldWriteIndex;
            }
            bufferFlushed = true;
            return count;
        } else {
            return (writeIndex - oldWriteIndex);
        }
    }

    protected void writeDataToFileCache(byte[] data) throws IOException {

        writeDataToFileCache(data, 0, data.length);
    }

    /**
     * Write data to the file cache. Create the file cache if it's never been created.
     *
     * @param data The array of data to be stored in the file cache.
     * @param off  The starting position of the data array is off set by the "off" parameter.
     * @param len  The number of bytes to be stored in the file cache.
     * @throws java.io.IOException if there is error reading the input data stream.
     */
    protected void writeDataToFileCache(byte[] data, int off, int len) throws IOException {

        if (fileCache == null) {
            if (fileCacheId == null) throw new RuntimeException("File name is NULL. Cannot create file for storing the raw attachments.");

            try {
                if (getFileCachePath() != null) {
                    fileCacheName = getFileCachePath() + "/tmp-file-req-att-" + fileCacheId;
                    fileCache = new FileOutputStream(fileCacheName, true);
                } else {
                    throw new RuntimeException("The File Cache path is NULL");
                }
            } catch (FileNotFoundException e) {
                throw new RuntimeException("Unable to create a new file " + getFileCachePath() + fileCacheId);
            }
        }

        fileCache.write(data, off, len);
        fileCache.flush();
    }

    /**
     * Store the raw data of an attachment to the file cache. The initial part of the attachment
     * is read from the input parameter (data). The remaining part of the attachment is read from
     * the input data stream.
     *
     * @param data The array of bytes of data to be stored in the file cache.
     * @return int The length of the attachment in bytes.
     * @throws java.io.IOException if there is error reading the input data stream.
     */
    private int storeRawPartContentToFileCache(byte[] data) throws IOException {
        return storeRawPartContentToFileCache(data, 0, data.length);
    }

    /**
     * Store the raw data of an attachment to the file cache. The initial part of the attachment
     * is read from the input parameter (data). The remaining part of the attachment is read from
     * the input data stream.
     *
     * @param data The array of bytes of data to be stored in the file cache.
     * @param off  The starting position of the data array is off set by the "off" parameter.
     * @param len  The number of bytes to be stored in the file cache.
     * @return int The length of the attachment in bytes.
     * @throws java.io.IOException if there is error reading the input data stream.
     */
    private int storeRawPartContentToFileCache(byte[] data, int off, int len) throws IOException {
        int count;

        // write the data in the temp buffer (the initial portion of the MIME part) to the file
        writeDataToFileCache(data, off, len);

        // write the remaining portion of the MIME part to the file
        count = storeRawPartContentToFileCache();

        return (len + count);
    }

    /**
     * Store the raw data of an attachment to the file cache. The data is read from the input data stream.
     *
     * @return int The length of the attachment in bytes.
     * @throws java.io.IOException if there is error reading the input data stream.
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
        buf[index++] = (byte)d;

        while (!boundaryFound && (d != -1)) {

            while ((index < buf.length) && (d != -1)) {
                // looking for <CR>
                if (d == 0x0d) {
                    crSeen = true;
                } else if (d == 0x0a) {
                    // if <CR><LF> sequenece found
                    if (crSeen) {

                        if (startIndex >= 0) {
                            endIndex = index;

                            // check if the multipart boundary found between the first <CR><LF> and the second <CR><LF>
                            if (isMultipartBoundaryFound(buf, startIndex, endIndex)) {
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
                buf[index++] = (byte)d;
            }

            // if the boundary NOT found when the buffer is full, store the data first and then continue
            if (!boundaryFound) {

                // NOTE: we should not include the case startIndex == 0 in this if statement,
                // otherwise the same data are being checked repeately.
                if (startIndex > 0 && startIndex != ATTACHMENT_BLOCK_SIZE - 1) {
                    // store the data up to the last <cr><lf> in the buffer to the file cache
                    writeDataToFileCache(buf, 0, startIndex);

                    // move the rest of the data to the beginning of the buffer;
                    int numberOfBytesToMoveBack = index - startIndex;
                    int i = 0;
                    for (; i < numberOfBytesToMoveBack && i < buf.length; i++) {
                        buf[i] = buf[startIndex + i];
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

                    if (startIndex == ATTACHMENT_BLOCK_SIZE - 1) {
                        // the last two bytes in the buf is <cr><lf>
                        startIndex = 0;
                    } else {
                        // reset the start index
                        startIndex = -1;
                    }
                }

                endIndex = -1;

                // read the next byte
                d = pushbackInputStream.read();

                // store the byte
                buf[index++] = (byte)d;
            }
        }

        return count;
    }

    /**
     * Check if the multipart boundary is found in the buffer specified.
     *
     * @param data       The data to be examined.
     * @param startIndex The starting position of the data to be examined.
     * @param endIndex   The ending position of the data to be examined.
     * @return TRUE if the multipart boundary is found. FALSE otherwise.
     */
    private boolean isMultipartBoundaryFound(byte[] data, int startIndex, int endIndex) {

        // check if the length of the two objects are the same
        StringBuffer sb = new StringBuffer();
        // convert the byte stream to string
        for (int i = 0; i < endIndex - startIndex; i++) {
            sb.append((char)data[startIndex + i]);
        }

        if (sb.toString().trim().startsWith("--" + multipartBoundary)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Read a line from the input data stream
     *
     * @return The line retrieved from the input data stream. -1 if no more data exists in the input stream.
     * @throws java.io.IOException if there is error reading the input data stream.
     */
    protected String readLine() throws IOException {
        boolean newlineFound = false;
        long byteCount = 0;
        byte[] buf = new byte[256];


        StringBuffer sb = new StringBuffer();
        do {
            int read = pushbackInputStream.read(buf, 0, buf.length);
            if (read <= 0) {
                if (byteCount > 0) {
                    return sb.toString();
                } else {
                    return null;
                }
            }

            for (int i = 0; i < read; i++) {
                sb.append((char)buf[i]);
                if (buf[i] == '\n') {
                    newlineFound = true;
                    // push the rest back to the stream
                    pushbackInputStream.unread(buf, i + 1, read - (i + 1));
                    break;
                }
            }
        } while (!newlineFound);

        return sb.toString().trim();
    }

    /**
     * Delete the cache file
     */
    public void deleteCacheFile() {

        if (fileCache == null) return;  // nothing to delete

        try {
            fileCache.close();
            fileCache = null;
        } catch (IOException e) {
            // do nothing
            // the fileCache input stream already been close
        }

        String fileName = getFileCacheName();

        if (fileName == null) {
            logger.warning("Internal error: trying to delete a temp file but the file name is not set");
            return;
        }

        final File deleteFile = new File(fileName);


        if (!deleteFile.exists()) {
            logger.warning("Cannot delete the cache file: " + getFileCacheName() + " does not exist.");
            return;
        }

        if (!deleteFile.delete()) {
            logger.warning("Cannot delete the cache file: " + getFileCacheName());
        }

    }
}
