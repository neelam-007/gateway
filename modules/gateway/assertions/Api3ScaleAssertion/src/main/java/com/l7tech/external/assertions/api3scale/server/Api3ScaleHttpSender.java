package com.l7tech.external.assertions.api3scale.server;

import com.l7tech.common.http.*;
import com.l7tech.common.http.prov.apache.CommonsHttpClient;
import com.l7tech.common.mime.ContentTypeHeader;
import net.threescale.api.v2.ApiHttpResponse;
import net.threescale.api.v2.HttpSender;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;

/**
 * User: wlui
 */
public class Api3ScaleHttpSender implements HttpSender {
    private static final Logger logger = Logger.getLogger(Api3ScaleHttpSender.class.getName());
    private final GenericHttpClient httpClient;
    private SimpleHttpClient.SimpleHttpResponse httpResponse = null;

    /**
     * Normal Constructor using live implementation.
     */
    public Api3ScaleHttpSender() {
        httpClient = new CommonsHttpClient();
    }

    /**
     * Send a POST message.
     *
     * @param hostUrl  Url and parameters to send to the server.
     * @param postData Data to be POSTed.
     * @return Transaction data returned from the server.
     */
    @Override
    public ApiHttpResponse sendPostToServer(String hostUrl, String postData) {
        SimpleHttpClient simpleClient = new SimpleHttpClient(httpClient);
        try {
            GenericHttpRequestParams params = new GenericHttpRequestParams();
            params.setTargetUrl(new URL(hostUrl + "/transactions.xml"));
            params.setContentType(ContentTypeHeader.APPLICATION_X_WWW_FORM_URLENCODED);
            byte[] bytes = postData.getBytes();
            params.setContentLength((long)bytes.length);

            httpResponse  =  simpleClient.post(params, bytes);

            ApiHttpResponse response = new ApiHttpResponse(httpResponse.getStatus(),httpResponse.getString());
            return response;

        } catch (Exception e) {
            logger.warning("Error generating post request: "+e.getMessage());
            return handleErrors(null);
        }
    }

    /**
     * Send a Get message to the server
     *
     * @param hostUrlWithParameters
     * @return Response from Server for successful action
     */
    @Override
    public ApiHttpResponse sendGetToServer(String hostUrlWithParameters) {
        SimpleHttpClient simpleClient = new SimpleHttpClient(httpClient);
        try {
            httpResponse  =  simpleClient.get(hostUrlWithParameters);
            ApiHttpResponse response = new ApiHttpResponse(httpResponse.getStatus(), httpResponse.getString());
            return response;
        } catch (Exception e) {
            logger.warning("Error generating get request: "+e.getMessage());
            return handleErrors(null);
        }
    }

    public String getHttpResponseString() {
        try {
            return httpResponse.getString();
        } catch (IOException e) {
            return null;
        }
    }

    private ApiHttpResponse handleErrors(SimpleHttpClient.SimpleHttpResponse response) {
        if (response != null) {
            try {
                return new ApiHttpResponse(response.getStatus(), response.getString());
            } catch (IOException e) {
                return new ApiHttpResponse(HttpConstants.STATUS_SERVER_ERROR, IOERROR_RESPONSE);
            }
        } else {
            return new ApiHttpResponse(HttpConstants.STATUS_SERVER_ERROR, ERROR_CONNECTING_RESPONSE);
        }
    }

    private final String  IOERROR_RESPONSE =
        "<?xml version=\"1.0\" encoding=\"utf-8\" ?> " +
        "<error code=\"ioerror\">IO Error connecting to the server</error>";

    private final String  ERROR_CONNECTING_RESPONSE =
        "<?xml version=\"1.0\" encoding=\"utf-8\" ?> " +
        "<error code=\"server_error\">Could not connect to the server</error>";

}
