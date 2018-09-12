package com.l7tech.server.communityschemas;

import com.l7tech.common.http.GenericHttpHeaders;
import com.l7tech.common.http.HttpHeader;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.LoggingAudit;
import com.l7tech.security.MockGenericHttpClient;
import com.l7tech.server.util.TestingHttpClientFactory;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.MockConfig;
import com.l7tech.util.MockTimer;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import static org.junit.Assert.*;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

/**
 *
 */
public class SchemaManagerImplTest {

    private static final Logger logger = Logger.getLogger( SchemaManagerImplTest.class.getName() );
    private static final Audit audit = new LoggingAudit( logger );

    @Test
    public void testHttpFail() throws Exception {
        try {
            final SchemaManager manager = getTestSchemaManager( );
            manager.getSchemaByUri( audit, "http://bad" ).close();
        } catch ( IOException ioe ) {
            ioe.printStackTrace();
            assertTrue( "404 error", ioe.getMessage().contains( "404" ));
        }
    }

    @Test(expected=SAXException.class)
    public void testParseFail() throws Exception {
        final SchemaManager manager = getTestSchemaManager(  );
        manager.registerSchema( "http://bad/html.html", null, "<html><body>HTML content<br></body></html>" );
        manager.getSchemaByUri( audit, "http://bad/html.html" ).close();
    }

    @Test
    public void testEntityResolutionFailureRecognition() {
        final SchemaManagerImpl manager = getTestSchemaManager(  );

        try {
            XmlUtil.parse( new InputSource(new StringReader("<!DOCTYPE schema SYSTEM \"http://www.w3.org/2001/XMLSchema.dtd\"><schema/>")), null );
            fail("Should have entity resolution failure.");
        } catch ( IOException e ) {
            assertTrue( "Exception is resource not permitted : " + ExceptionUtils.getMessage( e ), manager.isResourceNotPermitted( e ) );
        } catch ( SAXException e ) {
            assertTrue( "Exception is resource not permitted : " + ExceptionUtils.getMessage( e ), manager.isResourceNotPermitted( e ) );
        }
    }

    private SchemaManagerImpl getTestSchemaManager( ) {

        final TestingHttpClientFactory httpClientFactory = new TestingHttpClientFactory();
        httpClientFactory.setMockHttpClient( new MockGenericHttpClient( 404, new GenericHttpHeaders( new HttpHeader[0]), null, null, null ) );

        final SchemaConfiguration schemaConfiguration = new SchemaConfiguration(new MockConfig(new Properties(){{ setProperty("schemaRecompileLatency", "0"); }}));

        return new SchemaManagerImpl(
                schemaConfiguration,
                new MockTimer(),
                new SchemaSourceResolver[]{
                        new HttpSchemaSourceResolver( schemaConfiguration, httpClientFactory, XmlUtil.getSafeEntityResolver() )
                },
                XmlUtil.getSafeEntityResolver()
                ){
            @Override
            public SchemaHandle getSchemaByUri( final Audit audit, final String url ) throws IOException, SAXException {
                final SchemaHandle handle = super.getSchemaByUri( audit, url );
                return handle;
            }

        };
    }
}
