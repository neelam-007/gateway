/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Feb 21, 2005<br/>
 */
package com.l7tech.server;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;
import com.l7tech.common.message.Message;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;

import java.io.ByteArrayInputStream;

/**
 * A test class for the SymantecAntivirusScanEngineClient class.
 *
 * @author flascelles@layer7-tech.com
 */
public class SymantecAntivirusScanEngineClientTest extends TestCase {

    public static Test suite() {
        TestSuite suite = new TestSuite(SymantecAntivirusScanEngineClientTest.class);
        return suite;
    }

    protected void setUp() {
        if (scanner == null) {
            System.setProperty("com.l7tech.server.savseEnable", "yes");
            System.setProperty("com.l7tech.server.savsePort", "7777");
            System.setProperty("com.l7tech.server.savseHost", "phlox.l7tech.com");
            scanner = new SymantecAntivirusScanEngineClient();
        }
    }

    public static void main(String[] args) throws Throwable {
        //for (int i = 0; i < 100; i++) {
            junit.textui.TestRunner.run(suite());
        //}
    }

    public void testInfectedMultipartMsg() throws Exception {
        Message msg = makeMsg(MULTIPART_CONTENTTYPE,
                              INFECTED_MULTIPART_MIME_MSG_PAYLOAD.getBytes());
        SymantecAntivirusScanEngineClient.SAVScanEngineResponse[] res = scanMsg(msg);
        boolean[] infected = interpretResults(res);
        assertTrue("got the right number of response outputs", res.length == 2);
        assertFalse("the first part is not infected", infected[0]);
        assertTrue("the first part is infected", infected[1]);
    }

    public void testCleanMultipartMsg() throws Exception {
        Message msg = makeMsg(MULTIPART_CONTENTTYPE,
                              CLEAN_MULTIPART_MIME_MSG_PAYLOAD.getBytes());
        SymantecAntivirusScanEngineClient.SAVScanEngineResponse[] res = scanMsg(msg);
        boolean[] infected = interpretResults(res);
        assertTrue("got the right number of response outputs", res.length == 2);
        assertFalse("the first part is not infected", infected[0]);
        assertFalse("the first part is not infected", infected[1]);
    }

    public void testCleanSimpleMsg() throws Exception {
        Message msg = makeMsg("text/xml",
                              CLEAN_SIMPLE_MSG.getBytes());
        SymantecAntivirusScanEngineClient.SAVScanEngineResponse[] res = scanMsg(msg);
        boolean[] infected = interpretResults(res);
        assertTrue("got the right number of response outputs", res.length == 1);
        assertFalse("the first part is not infected", infected[0]);
    }

    public void testInfectedSimpleMsg() throws Exception {
        Message msg = makeMsg("application/octet-stream",
                              VIRUS);
        SymantecAntivirusScanEngineClient.SAVScanEngineResponse[] res = scanMsg(msg);
        boolean[] infected = interpretResults(res);
        assertTrue("got the right number of response outputs", res.length == 1);
        assertTrue("the first part is infected", infected[0]);
    }

    public void testGettingOptions() throws Exception {
        scanner.getSavScanEngineOptions();
        //System.out.println("Scanner options:\n" + scanner.getSavScanEngineOptions());
    }

    private Message makeMsg(String contentTypeValue, byte[] content) throws Exception {
        Message msg = new Message();
        msg.initialize(new ByteArrayStashManager(),
                      ContentTypeHeader.parseValue(contentTypeValue),
                      new ByteArrayInputStream(content));
        return msg;
    }

    private SymantecAntivirusScanEngineClient.SAVScanEngineResponse[] scanMsg(Message msg) throws Exception {
        SymantecAntivirusScanEngineClient.SAVScanEngineResponse[] output = scanner.scan(msg);
        /*for (int i = 0; i < output.length; i++) {
            System.out.println(output[i]);
        }*/
        return output;
    }

    private boolean[] interpretResults(SymantecAntivirusScanEngineClient.SAVScanEngineResponse[] res) throws Exception {
        boolean[] output = new boolean[res.length];
        for (int i = 0; i < res.length; i++) {
            output[i] = scanner.savseResponseIndicateInfection(res[i]);
        }
        return output;
    }

    private SymantecAntivirusScanEngineClient scanner;
    private static final byte[] VIRUS = {0x58, 0x35, 0x4f, 0x21, 0x50, 0x25,
                                         0x40, 0x41, 0x50, 0x5b, 0x34, 0x5c, 0x50, 0x5a,
                                         0x58, 0x35, 0x34, 0x28, 0x50, 0x5e, 0x29, 0x37,
                                         0x43, 0x43, 0x29, 0x37, 0x7d, 0x24, 0x45, 0x49,
                                         0x43, 0x41, 0x52, 0x2d, 0x53, 0x54, 0x41, 0x4e,
                                         0x44, 0x41, 0x52, 0x44, 0x2d, 0x41, 0x4e, 0x54,
                                         0x49, 0x56, 0x49, 0x52, 0x55, 0x53, 0x2d, 0x54,
                                         0x45, 0x53, 0x54, 0x2d, 0x46, 0x49, 0x4c, 0x45,
                                         0x21, 0x24, 0x48, 0x2b, 0x48, 0x2a};

    public static final String MULTIPART_CONTENTTYPE = "multipart/mixed; boundary=MIME_boundary; start=\"<soapRequest>\"";

    public static final String INFECTED_MULTIPART_MIME_MSG_PAYLOAD =
            "--MIME_boundary\r\n" +
            "Content-Type: text/xml; charset=UTF-8\r\n" +
            "Content-Transfer-Encoding: 8bit\r\n" +
            "Content-ID: <soapRequest>\r\n\r\n" +
            "<?xml version='1.0' ?>\r\n" +
            "<SOAP-ENV:Envelope\r\n" +
            "xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">\r\n" +
            "<SOAP-ENV:Body>\r\n" +
            "<blah/>\r\n" +
            "</SOAP-ENV:Body>\r\n" +
            "</SOAP-ENV:Envelope>\r\n\r\n" +
            "--MIME_boundary\r\n" +
            "Content-Type: application/octet-stream\r\n" +
            "Content-Transfer-Encoding: binary\r\n" +
            "l7_something: dkfdjhgkghkd\r\n" +
            "Content-ID: <attachment-1>\r\n\r\n" +
            new String(VIRUS) +
            "\r\n--MIME_boundary--";

    public static final String CLEAN_MULTIPART_MIME_MSG_PAYLOAD =
            "--MIME_boundary\r\n" +
            "Content-Type: text/xml; charset=UTF-8\r\n" +
            "Content-Transfer-Encoding: 8bit\r\n" +
            "Content-ID: <soapRequest>\r\n\r\n" +
            "<?xml version='1.0' ?>\r\n" +
            "<SOAP-ENV:Envelope\r\n" +
            "xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">\r\n" +
            "<SOAP-ENV:Body>\r\n" +
            "<blah/>\r\n" +
            "</SOAP-ENV:Body>\r\n" +
            "</SOAP-ENV:Envelope>\r\n\r\n" +
            "--MIME_boundary\r\n" +
            "Content-Type: application/octet-stream\r\n" +
            "Content-Transfer-Encoding: binary\r\n" +
            "l7_something: dkfdjhgkghkd\r\n" +
            "Content-ID: <attachment-1>\r\n\r\n" +
            "blahblahblahblahblah" +
            "\r\n--MIME_boundary--";

    public static final String CLEAN_SIMPLE_MSG =
            "<?xml version='1.0' ?>\r\n" +
            "<SOAP-ENV:Envelope\r\n" +
            "xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">\r\n" +
            "<SOAP-ENV:Body>\r\n" +
            "<blah/>\r\n" +
            "</SOAP-ENV:Body>\r\n" +
            "</SOAP-ENV:Envelope>\r\n\r\n";
}

