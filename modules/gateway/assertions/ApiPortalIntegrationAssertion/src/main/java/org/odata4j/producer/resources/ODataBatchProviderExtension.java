package org.odata4j.producer.resources;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Extend OData4j batch provider on the same package as we need access to the private method of
 *
 * @author raqri01, 6/30/14
 */
public class ODataBatchProviderExtension extends ODataBatchProvider {

    public List<BatchBodyPart> readFrom(
            HttpHeaders httpHeaders,
            UriInfo uriInfo,
            InputStream inputStream) throws IOException, WebApplicationException {
        List<BatchBodyPart> parts = new ArrayList<BatchBodyPart>();

        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        final String ContentType = "content-type:";
        String currentLine = "";

        while ((currentLine = br.readLine()) != null) {
            if (currentLine.toLowerCase().startsWith(ContentType)) {
                String ctype = currentLine.substring(ContentType.length()).trim();
                if (ctype.toLowerCase().startsWith("")) {
                    parts.add(parseBodyPart(br, httpHeaders, uriInfo));
                }
            }
        }

        br.close();
        return parts;
    }

    private BatchBodyPart parseBodyPart(BufferedReader br, HttpHeaders httpHeaders, UriInfo uriInfo) throws IOException {
        BatchBodyPart block = new BatchBodyPart(httpHeaders, uriInfo);
        final int SKIP_CONTENT_BEGIN = 2;

        String line = "";
        while ((line = br.readLine()) != null) {
            if (line.equals("")) {
                continue;
            }
            if (line.startsWith("Content-ID")) {
                continue;
            }
            if (line.startsWith("--")) {
                return validateBodyPart(block);
            }

            if (block.getHttpMethod() == null) {
                for (HTTP_METHOD method : HTTP_METHOD.values()) {
                    if (line.startsWith(method.name())) {
                        String uri = line.substring(method.name().length() + 1);
                        int lastIdx = uri.lastIndexOf(":");
                        if (lastIdx <= -1) {
                            lastIdx = uri.lastIndexOf(" ");
                        }
                        if (lastIdx != -1) {
                            uri = uri.substring(0, lastIdx);
                        }

                        block.setHttpMethod(method);
                        block.setUri(uri);
                        break;
                    }
                }
            } else {
                Integer idx = line.indexOf(':');
                String key = line.substring(0, idx);
                String value = line.substring(idx + 1).trim();
                block.getHeaders().putSingle(key, value);

                if (key.toLowerCase().equals("content-length")) {
                    int capacity = Integer.parseInt(value);
                    char[] buf = new char[capacity];
                    int offset = 0;

                    br.skip(SKIP_CONTENT_BEGIN);

                    while (offset != capacity) {
                        offset += br.read(buf, offset, capacity - offset);
                    }

                    block.setEntity(new String(buf));
                    return validateBodyPart(block);
                }
            }
        }

        throw new IllegalArgumentException("Cann't parse block");
    }

    private static BatchBodyPart validateBodyPart(BatchBodyPart block) {
        if (block.getHttpMethod() == null ? "" == null : block.getHttpMethod().toString().equals("")) {
            throw new IllegalArgumentException("Block HTTP METHOD is empty.");
        }

        if (block.getUri() == null ? "" == null : block.getUri().equals("")) {
            throw new IllegalArgumentException("Block URI is empty.");
        }

        return block;
    }


}
