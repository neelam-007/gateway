package com.l7tech.proxy.attachments;

import com.l7tech.common.mime.MultipartMessageReader;
import com.l7tech.proxy.ClientProxy;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.logging.Logger;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class ClientMultipartMessageReader extends MultipartMessageReader {
    private final Logger logger = Logger.getLogger(getClass().getName());
    
    public ClientMultipartMessageReader(InputStream inputStream, String multipartBoundary) {
        pushbackInputStream = new PushbackInputStream(inputStream, SOAP_PART_BUFFER_SIZE);
        this.multipartBoundary = multipartBoundary;
    }

    protected String getFileCachePath() {

        String proxyConfigPath = ClientProxy.PROXY_CONFIG;

        if (proxyConfigPath != null && proxyConfigPath.length() > 0) {
            File f = new File(proxyConfigPath);
            if (!f.exists()) {
                String errorMsg = "The directory " + proxyConfigPath + "is required for caching the big attachments but not found.";
                logger.severe(errorMsg);
                throw new RuntimeException(errorMsg);
            }
        } else {

            String errorMsg = "Internal error! The Proxy config path is not defined";
            logger.severe(errorMsg);
            throw new RuntimeException(errorMsg);
        }
        return proxyConfigPath;
    }

    public void storeAllAttachmentsToCache() throws IOException {
        int d;

        writeIndex = 0;

        while(writeIndex < attachmentsRawData.length) {

            d = pushbackInputStream.read();

            // if no more data
            if(d == -1) {
                // the memory has enough space all attachments
                return;
            } else {
                // store the byte in memory cache
                attachmentsRawData[writeIndex++] = (byte) d;
            }
        }

        // the buffer is full and there is more data to be stored in cache
        writeDataToFileCache(attachmentsRawData);

        // store the attachments directly to the file cache
        storeAllAttachmentsToFileCache();
    }

    private void storeAllAttachmentsToFileCache() throws IOException {
        int d;

        // store data in file cache block by block
        byte[] buf = new byte[ATTACHMENT_BLOCK_SIZE];
        int index = 0;

        while ((d = pushbackInputStream.read()) != -1) {
            buf[index++] = (byte) d;

            // if no more data
            if(d == -1) {
                writeDataToFileCache(buf, 0, index);
                break;
            }

            if(index == ATTACHMENT_BLOCK_SIZE) {
                writeDataToFileCache(buf);
                index = 0;
            }
        }

        // store the last block in the buffer to the file cache
        if(index > 0 ) {
            writeDataToFileCache(buf, 0, index);
        }

    }

}
