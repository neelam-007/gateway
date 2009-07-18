package com.l7tech.server;

import com.l7tech.common.http.*;
import com.l7tech.common.http.prov.apache.CommonsHttpClient;
import com.l7tech.test.BugNumber;
import org.junit.*;
import static org.junit.Assert.*;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;

/**
 *
 */
public class CommonsHttpClientMultipleContentLengthHeadersTest {
    private static Server server;
    private static final String RESP = "<h1>Hello</h1>";

    @BeforeClass
    public static void startServer() throws Exception {
        server = new Server(7353);
        server.setHandler(new AbstractHandler() {
            @Override
            public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException, ServletException {
                response.setContentType("text/xml");
                response.setStatus(HttpServletResponse.SC_OK);
                response.addHeader("Content-Length", String.valueOf(RESP.length()));
                response.addHeader("Content-Length", String.valueOf(RESP.length()));
                response.getWriter().println(RESP);
                ((Request)request).setHandled(true);            }
        });
        server.start();
    }

    @AfterClass
    public static void stopServer() throws Exception {
        if (server != null) server.stop();
    }

    @Test
    @BugNumber(7353)
    public void testFetchMultipleContentLength() throws Exception {
        GenericHttpClient client = new CommonsHttpClient();
        GenericHttpRequest request = client.createRequest(HttpMethod.POST, new GenericHttpRequestParams(new URL("http://localhost:7353")));
        request.setInputStream(new ByteArrayInputStream("<blah/>".getBytes()));
        GenericHttpResponse response = request.getResponse();
        String got = response.getAsString(false);
        assertEquals(RESP, got);
        assertEquals((double)RESP.length(), response.getContentLength(), 0.00004);
    }
}
