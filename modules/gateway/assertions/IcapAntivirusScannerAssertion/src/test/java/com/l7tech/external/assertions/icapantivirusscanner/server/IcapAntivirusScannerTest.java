package com.l7tech.external.assertions.icapantivirusscanner.server;

import com.l7tech.external.assertions.icapantivirusscanner.IcapConnectionDetail;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;

/**
 *
 */
public class IcapAntivirusScannerTest {

    @Test
    public void testValidConnection() {
        IcapConnectionDetail connectionDetail = new IcapConnectionDetail();
        connectionDetail.setHostname("sophosav-1");
        connectionDetail.setPort(1344);
        connectionDetail.setServiceName("avscan");
        IcapAntivirusScanner scanner = new IcapAntivirusScanner(connectionDetail);
        boolean result = scanner.testConnection();
        scanner.disconnect();
        Assert.assertTrue("connecting to a good server was unsuccessful", result);
    }

    @Test
    public void testScanCleanPayload() {
        IcapConnectionDetail connectionDetail = new IcapConnectionDetail();
        connectionDetail.setHostname("sophosav-1");
        connectionDetail.setPort(1344);
        connectionDetail.setServiceName("avscan");
        IcapAntivirusScanner scanner = new IcapAntivirusScanner(connectionDetail);
        try {
            IcapAntivirusScanner.IcapResponse response = scanner.scan("testPayload", Collections.<String, String>emptyMap(), "glkajskdjf 235234 lkj;kjasdf".getBytes());
            String replyCode = response.getIcapHeader(IcapAntivirusScanner.IcapResponse.STATUS_CODE);
            Assert.assertEquals("204", replyCode);
            Assert.assertNull(response.getIcapHeader("X-Violation-Name"));
        } catch (IOException e) {
            Assert.fail("testScanCleanPayload() failed: " + e.getMessage());
        }
        scanner.disconnect();
    }

    @Test
    public void testScanInfectedPayload() {
        IcapConnectionDetail connectionDetail = new IcapConnectionDetail();
        connectionDetail.setHostname("sophosav-1");
        connectionDetail.setPort(1344);
        connectionDetail.setServiceName("avscan");
        IcapAntivirusScanner scanner = new IcapAntivirusScanner(connectionDetail);
        try {
            IcapAntivirusScanner.IcapResponse response = scanner.scan("testPayload", Collections.<String, String>emptyMap(), "X5O!P%@AP[4\\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*".getBytes());
            String replyCode = response.getIcapHeader(IcapAntivirusScanner.IcapResponse.STATUS_CODE);
            Assert.assertEquals("200", replyCode);
            Assert.assertNotNull(response.getIcapHeader("X-Violation-Name"));
        } catch (IOException e) {
            Assert.fail("testScanInfectedPayload() failed: " + e.getMessage());
        }
        scanner.disconnect();
    }

    @Test
    public void testInvalidConnection() {
        IcapConnectionDetail connectionDetail = new IcapConnectionDetail();
        connectionDetail.setHostname("oohshiny");
        connectionDetail.setPort(1344);
        connectionDetail.setServiceName("avscan");
        IcapAntivirusScanner scanner = new IcapAntivirusScanner(connectionDetail);
        boolean result = scanner.testConnection();
        scanner.disconnect();
        Assert.assertFalse("connecting to a bad server was successful", result);
    }

}
