package com.l7tech.common.http.prov.apache;

import com.l7tech.common.http.GenericHttpException;
import com.l7tech.common.http.GenericHttpRequest;
import com.l7tech.common.http.GenericHttpRequestParams;
import com.l7tech.common.http.HttpMethod;
import com.l7tech.test.BugNumber;
import com.l7tech.util.ExceptionUtils;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import static org.junit.Assert.*;
import org.junit.Test;

import java.net.URL;

/**
 * Unit tests for commons http client
 */
public class CommonsHttpClientTest {

    @Test
    @BugNumber(9258)
    public void testInvalidURLPath() throws Exception {
        final CommonsHttpClient client = new CommonsHttpClient( new SimpleHttpConnectionManager(true) );

        for ( final HttpMethod method : HttpMethod.values() ) {
            if ( method == HttpMethod.OTHER ) continue;
            final GenericHttpRequest request = client.createRequest( method, new GenericHttpRequestParams( new URL("http://host/this is not a valid path") ) );
            try {
                request.getResponse();
                fail( "Expected failure due to invalid uri path" );
            } catch ( GenericHttpException e ) {
                // ensure expected exception
                assertTrue( "Error is for path", ExceptionUtils.getMessage( e ).contains( "this is not a valid path" ));
            }
        }
    }

}
