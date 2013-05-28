package com.l7tech.common.io;

import com.google.mockwebserver.MockResponse;
import com.google.mockwebserver.MockWebServer;
import com.l7tech.common.TestDocuments;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

public class PermissiveSSLSocketFactoryTest {


    private MockWebServer server;
    @Before
    public void setUp() throws Exception {
        server = new MockWebServer();
        server.useHttps(new TestSSLSocketFactory(), false);
        server.enqueue(new MockResponse().setBody("test"));
        server.play();
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testConnectWithPermissiveSSLSocketFactory() throws Exception {
        URL url = server.getUrl("/");
        HttpsURLConnection connection = (HttpsURLConnection)url.openConnection();
        connection.setReadTimeout(1000);
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setUseCaches(false);
        connection.setAllowUserInteraction(false);
        connection.setHostnameVerifier( new PermissiveHostnameVerifier() );
        connection.setSSLSocketFactory( new PermissiveSSLSocketFactory() );
        connection.setRequestProperty("Accept-Language", "en-US");

        try {
            InputStream response = connection.getInputStream();
            if (response != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(response));
                assertEquals(HttpURLConnection.HTTP_OK, connection.getResponseCode());
                assertEquals("test", reader.readLine());
            }
        } catch (Exception e) {
            fail();
        }

    }
}
