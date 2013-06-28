package com.l7tech.server.transport.http;

import org.apache.catalina.connector.Connector;
import org.apache.coyote.Response;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class ConnectorTest {

    @Test
    public void testTrimmedContentType() throws Exception {

        String contentType = "text/xml; charset=UTF-8";
        String trimmedContentType = "text/xml;charset=UTF-8";

        Connector connector = new Connector();
        connector.setTrimContentType(true);

        Response response = new Response();
        response.setConnector(connector);

        response.setContentType(contentType);

        assertEquals(trimmedContentType, response.getContentType());

    }

    @Test
    public void testNonTrimmedContentType() throws Exception {

        String contentType = "text/xml; charset=UTF-8";

        Connector connector = new Connector();
        connector.setTrimContentType(false);

        Response response = new Response();
        response.setConnector(connector);

        response.setContentType(contentType);

        assertEquals(contentType, response.getContentType());

    }
}
