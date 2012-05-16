package com.l7tech.proxy.processor;

import com.l7tech.common.http.GenericHttpHeaders;
import com.l7tech.common.http.HttpConstants;
import com.l7tech.common.http.HttpHeader;
import com.l7tech.common.http.SimpleHttpClient;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.message.HttpResponseKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.TrueAssertion;
import com.l7tech.proxy.datamodel.*;
import com.l7tech.proxy.message.PolicyApplicationContext;
import com.l7tech.security.MockGenericHttpClient;
import com.l7tech.test.BugNumber;
import com.l7tech.util.FileUtils;
import com.l7tech.util.SyspropUtil;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Unit tests for proxy message processor.
 */
public class MessageProcessorTest {

    @BeforeClass
    public static void init() throws IOException {
        final File file = FileUtils.createTempDirectory( "l7tech-MessageProcessorTest-", null, null, true );
        SyspropUtil.setProperty( "com.l7tech.proxy.configDir", file.getAbsolutePath() );
    }

    @AfterClass
    public static void cleanupSystemProperties() {
        SyspropUtil.clearProperties(
            "com.l7tech.proxy.configDir"
        );
    }

    @Test
    public void testSecurityWithoutTimestamp() throws Exception {
        final String responseMessage = RESPONSE_MESSAGE.replaceFirst( "(?ms:<wsu:Timestamp>.*</wsu:Timestamp>)", "" );
        assertFalse( "Timestamp in message", responseMessage.contains( "wsu:Timestamp" ));

        processMessage( REQUEST_MESSAGE, responseMessage );
    }

    @Test
    public void testTimestampWithoutCreated() throws Exception {
        final String responseMessage = RESPONSE_MESSAGE.replaceFirst( "(?ms:<wsu:Created>.*</wsu:Created>)", "" );
        assertFalse( "Created in message", responseMessage.contains( "wsu:Created" ));

        processMessage( REQUEST_MESSAGE, responseMessage );
    }

    @BugNumber(9164)
    @Test
    public void testTimestampWithoutExpiry() throws Exception {
        final String responseMessage = RESPONSE_MESSAGE.replaceFirst( "(?ms:<wsu:Expires>.*</wsu:Expires>)", "" );
        assertFalse( "Expires in message", responseMessage.contains( "wsu:Expires" ));
        
        processMessage( REQUEST_MESSAGE, responseMessage );
    }

    /**
     * Run the given request and response messages though the proxy message processor
     *
     * <p>The messages are expected to process successfully.</p>
     */
    private void processMessage( final String requestMessage,
                                 final String responseMessage ) throws Exception {
        final SsgManagerStub ssgManager = new SsgManagerStub();
        ssgManager.clear();
        final Ssg ssgFake = ssgManager.createSsg();
        ssgFake.setLocalEndpoint("gateway1");
        ssgFake.setSsgAddress( "127.0.0.1" );
        ssgManager.add(ssgFake);

        // Make a do-nothing PolicyManager
        final PolicyManagerStub policyManager = new PolicyManagerStub();
        policyManager.setPolicy(new Policy(new TrueAssertion(), "testpolicy"));
        ssgFake.getRuntime().setPolicyManager(policyManager);


        byte[] responseData = responseMessage.getBytes("UTF-8");
        MockGenericHttpClient client = new MockGenericHttpClient(200, new GenericHttpHeaders(new HttpHeader[]{ContentTypeHeader.XML_DEFAULT}), ContentTypeHeader.XML_DEFAULT, (long)responseData.length, responseData);
        ssgFake.getRuntime().setHttpClient( new SimpleHttpClient(client) );

        final MessageProcessor messageProcessor = new MessageProcessor();

        final Message request = new Message( XmlUtil.parse( requestMessage ) );
        final Message response = new Message();

        final URL originalUrl = new URL("http://127.0.0.1:7700/gateway1/test");
        final PolicyApplicationContext context = new PolicyApplicationContext(
                ssgFake,
                request,
                response,
                null,
                new PolicyAttachmentKey("urn:namespace", "soapAction", originalUrl.getFile()),
                originalUrl );

        messageProcessor.processMessage( context );

        final HttpResponseKnob hrk = response.getKnob(HttpResponseKnob.class);
        int status = hrk == null ? HttpConstants.STATUS_SERVER_ERROR : hrk.getStatus();
        assertEquals( "HTTP OK", HttpConstants.STATUS_OK, status);
    }

    private static final String REQUEST_MESSAGE =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<soapenv:Envelope\n" +
            "    xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
            "    xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
            "    <soapenv:Header/>\n" +
            "    <soapenv:Body>\n" +
            "        <tns:listProducts xmlns:tns=\"http://warehouse.acme.com/ws\">\n" +
            "            <tns:delay>0</tns:delay>\n" +
            "        </tns:listProducts>\n" +
            "    </soapenv:Body>\n" +
            "</soapenv:Envelope>";

    private static final String RESPONSE_MESSAGE =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<soapenv:Envelope\n" +
            "    xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
            "    xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
            "    <soapenv:Header>\n" +
            "        <wsse:Security actor=\"secure_span\" soapenv:mustUnderstand=\"1\" xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\">\n" +
            "            <wsu:Timestamp>\n" +
            "                <wsu:Created>2010-01-01T00:00:00.000Z</wsu:Created>\n" +
            "                <wsu:Expires>3030-01-01T00:00:00.000Z</wsu:Expires>\n" +
            "            </wsu:Timestamp>\n" +
            "        </wsse:Security>\n" +
            "    </soapenv:Header>" +
            "    <soapenv:Body>\n" +
            "        <tns:listProducts xmlns:tns=\"http://warehouse.acme.com/ws\">\n" +
            "            <tns:delay>0</tns:delay>\n" +
            "        </tns:listProducts>\n" +
            "    </soapenv:Body>\n" +
            "</soapenv:Envelope>";

}
