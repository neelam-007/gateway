package com.l7tech.skunkworks.http;

import com.l7tech.common.http.*;
import com.l7tech.common.http.prov.apache.components.HttpComponentsClient;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.util.Charsets;
import com.l7tech.util.IOUtils;

import java.io.ByteArrayInputStream;
import java.net.URL;

/**
 * Test client for sending unusual HTTP request method combinations (such as DELETE with a body, per Bug #12168).
 * <p/>
 * This test requires that CommonsHttpClient has been updated to support (by default) sending a body with a DELETE.
 */
public class HttpMethodTestClient {
    public static void main(String[] args) throws Exception {
        String server = "http://127.0.0.1:8080/method";

        GenericHttpClient client = new HttpComponentsClient();
        final GenericHttpRequestParams params = new GenericHttpRequestParams(new URL(server));
        params.setForceIncludeRequestBody(true);
        params.setContentType(ContentTypeHeader.TEXT_DEFAULT);
        GenericHttpRequest req = client.createRequest(HttpMethod.DELETE, params);
        try {

            req.setInputStream(new ByteArrayInputStream("Request body with delete through Gateway with content type".getBytes(Charsets.UTF8)));
            //req.setInputStream(new ByteArrayInputStream("".getBytes(Charsets.UTF8)));

            GenericHttpResponse resp = req.getResponse();
            System.out.println("Response status: " + resp.getStatus());
            System.out.println("Content-Type: " + resp.getContentType().getFullValue());
            System.out.println("Content: " + new String(IOUtils.slurpStream(resp.getInputStream()), Charsets.UTF8));
        } finally {
            req.close();
        }
    }
}
