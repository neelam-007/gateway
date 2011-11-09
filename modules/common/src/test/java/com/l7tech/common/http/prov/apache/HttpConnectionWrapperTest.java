package com.l7tech.common.http.prov.apache;

import com.l7tech.test.util.TestUtils;
import org.apache.commons.httpclient.HttpConnection;
import org.junit.Test;

/**
 *
 */
public class HttpConnectionWrapperTest {
    @Test
    public void testDelegation() {
        TestUtils.assertOverridesAllMethods( HttpConnection.class, HttpConnectionWrapper.class );
    }
}
