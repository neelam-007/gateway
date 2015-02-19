package com.l7tech.external.assertions.encodedecode.server;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import com.l7tech.common.io.CertUtils;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.external.assertions.encodedecode.EncodeDecodeAssertion;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.variable.DataType;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.test.BugId;
import com.l7tech.util.Functions;
import com.l7tech.util.HexUtils;
import com.l7tech.util.IOUtils;
import org.apache.commons.codec.binary.Base32;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.X509Certificate;

/**
 * Test the EncodeDecodeAssertion.
 */
public class ServerEncodeDecodeAssertionTest{

    public static String messageWithBinaryMimePart =
                    "UFVUIC9wdXR0ZXIgSFRUUC8xLjENCkFjY2VwdC1FbmNvZGluZzogZ3ppcCxkZWZsYXRlDQpDb250\n" +
                    "ZW50LVR5cGU6IG11bHRpcGFydC9mb3JtLWRhdGE7IGJvdW5kYXJ5PSItLS0tPV9QYXJ0XzBfMjg3\n" +
                    "NzQ1NDU5LjEzNDU3NjYxNDMzMzEiDQpNSU1FLVZlcnNpb246IDEuMA0KVXNlci1BZ2VudDogSmFr\n" +
                    "YXJ0YSBDb21tb25zLUh0dHBDbGllbnQvMy4xDQpIb3N0OiBsb2NhbGhvc3Q6ODA4MA0KQ29udGVu\n" +
                    "dC1MZW5ndGg6IDExNDQNCg0KDQotLS0tLS09X1BhcnRfMF8yODc3NDU0NTkuMTM0NTc2NjE0MzMz\n" +
                    "MQ0KQ29udGVudC1UeXBlOiBtdWx0aXBhcnQvZm9ybS1kYXRhDQpDb250ZW50LVRyYW5zZmVyLUVu\n" +
                    "Y29kaW5nOiA4Yml0DQoNCjxhPmlucHV0PC9hPg0KLS0tLS0tPV9QYXJ0XzBfMjg3NzQ1NDU5LjEz\n" +
                    "NDU3NjYxNDMzMzENCkNvbnRlbnQtVHlwZTogYXBwbGljYXRpb24vb2N0ZXQtc3RyZWFtOyBuYW1l\n" +
                    "PXBhcnNlcg0KQ29udGVudC1UcmFuc2Zlci1FbmNvZGluZzogYmluYXJ5DQpDb250ZW50LURpc3Bv\n" +
                    "c2l0aW9uOiBmb3JtLWRhdGE7IG5hbWU9InBhcnNlciI7IGZpbGVuYW1lPSJwYXJzZXIiDQoNCjCC\n" +
                    "AwYwggHuoAMCAQICCEJdB6diKkd1MA0GCSqGSIb3DQEBDAUAMCExEDAOBgNVBBETB2FiY2RlZmcx\n" +
                    "DTALBgNVBAMTBGZyZWQwHhcNMTIwNDA1MjExNDMzWhcNMTcwNDA0MjExNDMzWjAhMRAwDgYDVQQR\n" +
                    "EwdhYmNkZWZnMQ0wCwYDVQQDEwRmcmVkMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA\n" +
                    "t9cke58z/nUIQJqJDBd3J9wFQxAhSujDaUXtmBkJCNTHJjYQxEDaGDeP7M4wpfuBmLJ5Ugv8EvWU\n" +
                    "yaD208bFjP69e1ubnfIjnkuTF6uElkR5MGKTM+aXeLDQkfkJtod/7T7B5lktrsVU8fwgZgk/cBwl\n" +
                    "QhN0sBARSY5Vloph3yZoYk7MIj5jgv7BCPk/MdD6nPOFMt6LvTY1Ffrb8ObUN1OVCfhfkcXaS0MF\n" +
                    "jFttN4FfiHxFRbPlu7rTKNw4yIMsb2tSjVv4RGm+VqpabcIi5W8m3O7rtOsfgS/FU7wp/b94MOzW\n" +
                    "eImq/jXEzTHDzYXM/wyolweTsrmtTLo1WdBhXwIDAQABo0IwQDAdBgNVHQ4EFgQUerKZR0i8molf\n" +
                    "DUdWwbKjTXBvZ9owHwYDVR0jBBgwFoAUerKZR0i8molfDUdWwbKjTXBvZ9owDQYJKoZIhvcNAQEM\n" +
                    "BQADggEBAJLYZRs4JvkYFdU1NrlhioJFujDtettOcLnoypPAggzdXIz6DR36ewfp7dQHgmefWreh\n" +
                    "ZXMwQw3q9e0GuUnpQhnlldTi6O3rujgNwbRtgULYRnNmIQgWXysa24aB5YIPpnOQIT1iekyoHceW\n" +
                    "/miwCysbr8ve2pptn5uN1t/X8Kys0Z4fnckLXbOGhYpdaxgaapI4kVVkmhM1PtAtdHzOQJcaddwP\n" +
                    "Az5fJbxDh/fPmEyR0W8uu2QnSZ68bDrw8FoZGqjRvkKvrPy1bjVoxVV0oWUFsIB189N3UFUdNm2V\n" +
                    "CGfOoMRBxcJR3u7HewnBsfn+wt7yofnptzLY3WFS6TampAgNCi0tLS0tLT1fUGFydF8wXzI4Nzc0\n" +
                    "NTQ1OS4xMzQ1NzY2MTQzMzMxLS0NCg==";

    public static String base64EncodedMimePart =
                    "MIIDBjCCAe6gAwIBAgIIQl0Hp2IqR3UwDQYJKoZIhvcNAQEMBQAwITEQMA4GA1UE" +
                    "ERMHYWJjZGVmZzENMAsGA1UEAxMEZnJlZDAeFw0xMjA0MDUyMTE0MzNaFw0xNzA0" +
                    "MDQyMTE0MzNaMCExEDAOBgNVBBETB2FiY2RlZmcxDTALBgNVBAMTBGZyZWQwggEi" +
                    "MA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQC31yR7nzP+dQhAmokMF3cn3AVD" +
                    "ECFK6MNpRe2YGQkI1McmNhDEQNoYN4/szjCl+4GYsnlSC/wS9ZTJoPbTxsWM/r17" +
                    "W5ud8iOeS5MXq4SWRHkwYpMz5pd4sNCR+Qm2h3/tPsHmWS2uxVTx/CBmCT9wHCVC" +
                    "E3SwEBFJjlWWimHfJmhiTswiPmOC/sEI+T8x0Pqc84Uy3ou9NjUV+tvw5tQ3U5UJ" +
                    "+F+RxdpLQwWMW203gV+IfEVFs+W7utMo3DjIgyxva1KNW/hEab5WqlptwiLlbybc" +
                    "7uu06x+BL8VTvCn9v3gw7NZ4iar+NcTNMcPNhcz/DKiXB5Oyua1MujVZ0GFfAgMB" +
                    "AAGjQjBAMB0GA1UdDgQWBBR6splHSLyaiV8NR1bBsqNNcG9n2jAfBgNVHSMEGDAW" +
                    "gBR6splHSLyaiV8NR1bBsqNNcG9n2jANBgkqhkiG9w0BAQwFAAOCAQEAkthlGzgm" +
                    "+RgV1TU2uWGKgkW6MO16205wuejKk8CCDN1cjPoNHfp7B+nt1AeCZ59at6FlczBD" +
                    "Der17Qa5SelCGeWV1OLo7eu6OA3BtG2BQthGc2YhCBZfKxrbhoHlgg+mc5AhPWJ6" +
                    "TKgdx5b+aLALKxuvy97amm2fm43W39fwrKzRnh+dyQtds4aFil1rGBpqkjiRVWSa" +
                    "EzU+0C10fM5Alxp13A8DPl8lvEOH98+YTJHRby67ZCdJnrxsOvDwWhkaqNG+Qq+s" +
                    "/LVuNWjFVXShZQWwgHXz03dQVR02bZUIZ86gxEHFwlHe7sd7CcGx+f7C3vKh+em3" +
                    "MtjdYVLpNqakCA==";

    @Test
    public void testEncodeMimePart() throws Exception {

        ContentTypeHeader MULTIPART_RELATED = ContentTypeHeader.parseValue("multipart/related; charset=utf-8; boundary=\"----=_Part_0_287745459.1345766143331\"");
        final Message message = new Message();
        message.initialize(new ByteArrayStashManager(),MULTIPART_RELATED,
                 new ByteArrayInputStream(HexUtils.decodeBase64(messageWithBinaryMimePart)) );
        PartInfo part = message.getMimeKnob().getPart(1);
        String result = (String)oneWayTest( EncodeDecodeAssertion.TransformType.BASE64_ENCODE, AssertionStatus.NONE, true, 0, part);
        assertEquals(result,base64EncodedMimePart);
    }

    @Test
    public void testBase64() throws Exception {
        roundTripTest( EncodeDecodeAssertion.TransformType.BASE64_ENCODE,
                       EncodeDecodeAssertion.TransformType.BASE64_DECODE,
                       false );

        roundTripTest( EncodeDecodeAssertion.TransformType.BASE64_ENCODE,
                       EncodeDecodeAssertion.TransformType.BASE64_DECODE,
                       true );
    }

    @Test
    public void testBase32() throws Exception {
        roundTripTest( EncodeDecodeAssertion.TransformType.BASE32_ENCODE,
                EncodeDecodeAssertion.TransformType.BASE32_DECODE,
                false );

        roundTripTest( EncodeDecodeAssertion.TransformType.BASE32_ENCODE,
                EncodeDecodeAssertion.TransformType.BASE32_DECODE,
                true );
    }

    @Test
    public void testBase64Strict() throws Exception {
        oneWayTest( EncodeDecodeAssertion.TransformType.BASE64_DECODE, AssertionStatus.NONE, true, 0, "L 3 R l e H\tQvI\rHdpd\nGg    gc3BhY2VzIGFuZCA8Ij5z" );
        oneWayTest( EncodeDecodeAssertion.TransformType.BASE64_DECODE, AssertionStatus.FALSIFIED, true, 0, "L3 [ RleHQvIHdpdGggc3BhY2VzIGFuZCA8Ij5z" );
    }

    @Test
    public void testBase32Strict() throws Exception {
        String testString = "test input string \u0000 \u0373 \u8827";
        String testStringEncoded = "ORSXG5BANFXHA5LUEBZXI4TJNZTSAABAZWZSB2FAU4======";

        assertEquals( testStringEncoded, oneWayTest( EncodeDecodeAssertion.TransformType.BASE32_ENCODE, AssertionStatus.NONE, true, 0, testString ) );
        assertEquals( testString, oneWayTest( EncodeDecodeAssertion.TransformType.BASE32_DECODE, AssertionStatus.NONE, true, 0, testStringEncoded ) );
        assertEquals( testString, oneWayTest( EncodeDecodeAssertion.TransformType.BASE32_DECODE, AssertionStatus.NONE, true, 0, " O RSXG5  BAN F X H \t A5\r\nLUE\nBZX\r I4TJNZTSAA\t\tBAZWZSB2FAU4===\r\n==  =\r\n" ) );
        oneWayTest( EncodeDecodeAssertion.TransformType.BASE32_DECODE, AssertionStatus.NONE, true, 0, "ORSXG5BANFXHA5LUEBZXI4TJNZTSAABAZWZSB2FA" );
        oneWayTest( EncodeDecodeAssertion.TransformType.BASE32_DECODE, AssertionStatus.NONE, true, 0, "ORSXG5BANFXHA5LUEBZXI4TJNZTSAABAZWZSB2FAXX======" );
        oneWayTest( EncodeDecodeAssertion.TransformType.BASE32_DECODE, AssertionStatus.NONE, true, 0, "ORSXG5BANFXHA5LUEBZXI4TJNZTSAABAZWZSB2FAXXXX====" );
        oneWayTest( EncodeDecodeAssertion.TransformType.BASE32_DECODE, AssertionStatus.NONE, true, 0, "ORSXG5BANFXHA5LUEBZXI4TJNZTSAABAZWZSB2FAXXXXX===" );
        oneWayTest( EncodeDecodeAssertion.TransformType.BASE32_DECODE, AssertionStatus.NONE, true, 0, "ORSXG5BANFXHA5LUEBZXI4TJNZTSAABAZWZSB2FAXXXXXXX=" );

        oneWayTest( EncodeDecodeAssertion.TransformType.BASE32_DECODE, AssertionStatus.FALSIFIED, true, 0, "ORSXG5BANFXHA5LUEBZXI4TJNZ=TSAABAZWZSB2FAU4======" );
        oneWayTest( EncodeDecodeAssertion.TransformType.BASE32_DECODE, AssertionStatus.FALSIFIED, true, 0, "ORSXG5BANFXHA5LUEBZXI4TJNzTSAABAZWZSB2FAU4======" );
        oneWayTest( EncodeDecodeAssertion.TransformType.BASE32_DECODE, AssertionStatus.FALSIFIED, true, 0, "ORSXG5BANFXH\u0000A5LUEBZXI4TJNZTSAABAZWZSB2FAU4======" );
        oneWayTest( EncodeDecodeAssertion.TransformType.BASE32_DECODE, AssertionStatus.FALSIFIED, true, 0, "ORSXG5BANFXH\u0000A5LUEBZXI4TJNZTSAABAZWZSB2FA" );
        oneWayTest( EncodeDecodeAssertion.TransformType.BASE32_DECODE, AssertionStatus.FALSIFIED, true, 0, "ORSXG5BANFXHA5LUEBZXI4TJNZTSAABAZWZSB2FAX======" );
        oneWayTest( EncodeDecodeAssertion.TransformType.BASE32_DECODE, AssertionStatus.FALSIFIED, true, 0, "ORSXG5BANFXHA5LUEBZXI4TJNZTSAABAZWZSB2FA======" );
        oneWayTest( EncodeDecodeAssertion.TransformType.BASE32_DECODE, AssertionStatus.FALSIFIED, true, 0, "ORSXG5BANFXHA5LUEBZXI4TJNZTSAABAZWZSB2FA======" );
        oneWayTest( EncodeDecodeAssertion.TransformType.BASE32_DECODE, AssertionStatus.FALSIFIED, true, 0, "ORSXG5BANFXHA5LUEBZXI4TJNZTSAABAZWZSB2FA=" );
        oneWayTest( EncodeDecodeAssertion.TransformType.BASE32_DECODE, AssertionStatus.FALSIFIED, true, 0, "ORSXG5BANFXHA5LUEBZXI4TJNZTSAABAZWZSB2FA==" );
        oneWayTest( EncodeDecodeAssertion.TransformType.BASE32_DECODE, AssertionStatus.FALSIFIED, true, 0, "ORSXG5BANFXHA5LUEBZXI4TJNZTSAABAZWZSB2FA===" );
        oneWayTest( EncodeDecodeAssertion.TransformType.BASE32_DECODE, AssertionStatus.FALSIFIED, true, 0, "ORSXG5BANFXHA5LUEBZXI4TJNZTSAABAZWZSB2FA====" );
        oneWayTest( EncodeDecodeAssertion.TransformType.BASE32_DECODE, AssertionStatus.FALSIFIED, true, 0, "ORSXG5BANFXHA5LUEBZXI4TJNZTSAABAZWZSB2FA=====" );
        oneWayTest( EncodeDecodeAssertion.TransformType.BASE32_DECODE, AssertionStatus.FALSIFIED, true, 0, "ORSXG5BANFXHA5LUEBZXI4TJNZTSAABAZWZSB2FA======" );
        oneWayTest( EncodeDecodeAssertion.TransformType.BASE32_DECODE, AssertionStatus.FALSIFIED, true, 0, "ORSXG5BANFXHA5LUEBZXI4TJNZTSAABAZWZSB2FA=======" );
        oneWayTest( EncodeDecodeAssertion.TransformType.BASE32_DECODE, AssertionStatus.FALSIFIED, true, 0, "ORSXG5BANFXHA5LUEBZXI4TJNZTSAABAZWZSB2FA========" );
        oneWayTest( EncodeDecodeAssertion.TransformType.BASE32_DECODE, AssertionStatus.FALSIFIED, true, 0, "ORSXG5BANFXHA5LUEBZXI4TJNZTSAABAZWZSB2FA=========" );
        oneWayTest( EncodeDecodeAssertion.TransformType.BASE32_DECODE, AssertionStatus.FALSIFIED, true, 0, "ORSXG5BANFXHA5LUEBZXI4TJNZTSAABAZWZSB2FA==========" );
        oneWayTest( EncodeDecodeAssertion.TransformType.BASE32_DECODE, AssertionStatus.FALSIFIED, true, 0, "ORSXG5BANFXHA5LUEBZXI4TJNZTSAABAZWZSB2FAX" );
        oneWayTest( EncodeDecodeAssertion.TransformType.BASE32_DECODE, AssertionStatus.FALSIFIED, true, 0, "ORSXG5BANFXHA5LUEBZXI4TJNZTSAABAZWZSB2FAX=" );
        oneWayTest( EncodeDecodeAssertion.TransformType.BASE32_DECODE, AssertionStatus.FALSIFIED, true, 0, "ORSXG5BANFXHA5LUEBZXI4TJNZTSAABAZWZSB2FAX==" );
        oneWayTest( EncodeDecodeAssertion.TransformType.BASE32_DECODE, AssertionStatus.FALSIFIED, true, 0, "ORSXG5BANFXHA5LUEBZXI4TJNZTSAABAZWZSB2FAX===" );
        oneWayTest( EncodeDecodeAssertion.TransformType.BASE32_DECODE, AssertionStatus.FALSIFIED, true, 0, "ORSXG5BANFXHA5LUEBZXI4TJNZTSAABAZWZSB2FAX====" );
        oneWayTest( EncodeDecodeAssertion.TransformType.BASE32_DECODE, AssertionStatus.FALSIFIED, true, 0, "ORSXG5BANFXHA5LUEBZXI4TJNZTSAABAZWZSB2FAX=====" );
        oneWayTest( EncodeDecodeAssertion.TransformType.BASE32_DECODE, AssertionStatus.FALSIFIED, true, 0, "ORSXG5BANFXHA5LUEBZXI4TJNZTSAABAZWZSB2FAX======" );
        oneWayTest( EncodeDecodeAssertion.TransformType.BASE32_DECODE, AssertionStatus.FALSIFIED, true, 0, "ORSXG5BANFXHA5LUEBZXI4TJNZTSAABAZWZSB2FAX=======" );
        oneWayTest( EncodeDecodeAssertion.TransformType.BASE32_DECODE, AssertionStatus.FALSIFIED, true, 0, "ORSXG5BANFXHA5LUEBZXI4TJNZTSAABAZWZSB2FAX========" );
        oneWayTest( EncodeDecodeAssertion.TransformType.BASE32_DECODE, AssertionStatus.FALSIFIED, true, 0, "ORSXG5BANFXHA5LUEBZXI4TJNZTSAABAZWZSB2FAX=========" );
        oneWayTest( EncodeDecodeAssertion.TransformType.BASE32_DECODE, AssertionStatus.FALSIFIED, true, 0, "ORSXG5BANFXHA5LUEBZXI4TJNZTSAABAZWZSB2FAX==========" );
        oneWayTest( EncodeDecodeAssertion.TransformType.BASE32_DECODE, AssertionStatus.FALSIFIED, true, 0, "ORSXG5BANFXHA5LUEBZXI4TJNZTSAABAZWZSB2FAXX" );
        oneWayTest( EncodeDecodeAssertion.TransformType.BASE32_DECODE, AssertionStatus.FALSIFIED, true, 0, "ORSXG5BANFXHA5LUEBZXI4TJNZTSAABAZWZSB2FAXX=" );
        oneWayTest( EncodeDecodeAssertion.TransformType.BASE32_DECODE, AssertionStatus.FALSIFIED, true, 0, "ORSXG5BANFXHA5LUEBZXI4TJNZTSAABAZWZSB2FAXX==" );
        oneWayTest( EncodeDecodeAssertion.TransformType.BASE32_DECODE, AssertionStatus.FALSIFIED, true, 0, "ORSXG5BANFXHA5LUEBZXI4TJNZTSAABAZWZSB2FAXX===" );
        oneWayTest( EncodeDecodeAssertion.TransformType.BASE32_DECODE, AssertionStatus.FALSIFIED, true, 0, "ORSXG5BANFXHA5LUEBZXI4TJNZTSAABAZWZSB2FAXX====" );
        oneWayTest( EncodeDecodeAssertion.TransformType.BASE32_DECODE, AssertionStatus.FALSIFIED, true, 0, "ORSXG5BANFXHA5LUEBZXI4TJNZTSAABAZWZSB2FAXX=====" );
        oneWayTest( EncodeDecodeAssertion.TransformType.BASE32_DECODE, AssertionStatus.FALSIFIED, true, 0, "ORSXG5BANFXHA5LUEBZXI4TJNZTSAABAZWZSB2FAXX=======" );
        oneWayTest( EncodeDecodeAssertion.TransformType.BASE32_DECODE, AssertionStatus.FALSIFIED, true, 0, "ORSXG5BANFXHA5LUEBZXI4TJNZTSAABAZWZSB2FAXX========" );
        oneWayTest( EncodeDecodeAssertion.TransformType.BASE32_DECODE, AssertionStatus.FALSIFIED, true, 0, "ORSXG5BANFXHA5LUEBZXI4TJNZTSAABAZWZSB2FAXX=========" );
        oneWayTest( EncodeDecodeAssertion.TransformType.BASE32_DECODE, AssertionStatus.FALSIFIED, true, 0, "ORSXG5BANFXHA5LUEBZXI4TJNZTSAABAZWZSB2FAXX==========" );
        oneWayTest( EncodeDecodeAssertion.TransformType.BASE32_DECODE, AssertionStatus.FALSIFIED, true, 0, "ORSXG5BANFXHA5LUEBZXI4TJNZTSAABAZWZSB2FAXXX" );
        oneWayTest( EncodeDecodeAssertion.TransformType.BASE32_DECODE, AssertionStatus.FALSIFIED, true, 0, "ORSXG5BANFXHA5LUEBZXI4TJNZTSAABAZWZSB2FAXXX=" );
        oneWayTest( EncodeDecodeAssertion.TransformType.BASE32_DECODE, AssertionStatus.FALSIFIED, true, 0, "ORSXG5BANFXHA5LUEBZXI4TJNZTSAABAZWZSB2FAXXX==" );
        oneWayTest( EncodeDecodeAssertion.TransformType.BASE32_DECODE, AssertionStatus.FALSIFIED, true, 0, "ORSXG5BANFXHA5LUEBZXI4TJNZTSAABAZWZSB2FAXXX===" );
        oneWayTest( EncodeDecodeAssertion.TransformType.BASE32_DECODE, AssertionStatus.FALSIFIED, true, 0, "ORSXG5BANFXHA5LUEBZXI4TJNZTSAABAZWZSB2FAXXX====" );
        oneWayTest( EncodeDecodeAssertion.TransformType.BASE32_DECODE, AssertionStatus.FALSIFIED, true, 0, "ORSXG5BANFXHA5LUEBZXI4TJNZTSAABAZWZSB2FAXXX=====" );
        oneWayTest( EncodeDecodeAssertion.TransformType.BASE32_DECODE, AssertionStatus.FALSIFIED, true, 0, "ORSXG5BANFXHA5LUEBZXI4TJNZTSAABAZWZSB2FAXXX======" );
        oneWayTest( EncodeDecodeAssertion.TransformType.BASE32_DECODE, AssertionStatus.FALSIFIED, true, 0, "ORSXG5BANFXHA5LUEBZXI4TJNZTSAABAZWZSB2FAXXX=======" );
        oneWayTest( EncodeDecodeAssertion.TransformType.BASE32_DECODE, AssertionStatus.FALSIFIED, true, 0, "ORSXG5BANFXHA5LUEBZXI4TJNZTSAABAZWZSB2FAXXX========" );
        oneWayTest( EncodeDecodeAssertion.TransformType.BASE32_DECODE, AssertionStatus.FALSIFIED, true, 0, "ORSXG5BANFXHA5LUEBZXI4TJNZTSAABAZWZSB2FAXXX=========" );
        oneWayTest( EncodeDecodeAssertion.TransformType.BASE32_DECODE, AssertionStatus.FALSIFIED, true, 0, "ORSXG5BANFXHA5LUEBZXI4TJNZTSAABAZWZSB2FAXXX==========" );
        oneWayTest( EncodeDecodeAssertion.TransformType.BASE32_DECODE, AssertionStatus.FALSIFIED, true, 0, "ORSXG5BANFXHA5LUEBZXI4TJNZTSAABAZWZSB2FAXXX======" );
        oneWayTest( EncodeDecodeAssertion.TransformType.BASE32_DECODE, AssertionStatus.FALSIFIED, true, 0, "ORSXG5BANFXHA5LUEBZXI4TJNZTSAABAZWZSB2FAXXXX======" );
        oneWayTest( EncodeDecodeAssertion.TransformType.BASE32_DECODE, AssertionStatus.FALSIFIED, true, 0, "ORSXG5BANFXHA5LUEBZXI4TJNZTSAABAZWZSB2FAXXXXX======" );
        oneWayTest( EncodeDecodeAssertion.TransformType.BASE32_DECODE, AssertionStatus.FALSIFIED, true, 0, "ORSXG5BANFXHA5LUEBZXI4TJNZTSAABAZWZSB2FAXXXXXX======" );
    }

    @Test
    public void testRawByteArrayInput() throws Exception {
        String testString = "test input string \u0000 \u0373 \u8827";
        String testStringEncoded = "ORSXG5BANFXHA5LUEBZXI4TJNZTSAABAZWZSB2FAU4======";

        assertEquals( testStringEncoded, oneWayTest( EncodeDecodeAssertion.TransformType.BASE32_ENCODE, AssertionStatus.NONE, true, 0, testString.getBytes() ) );
        assertEquals( testString, oneWayTest( EncodeDecodeAssertion.TransformType.BASE32_DECODE, AssertionStatus.NONE, true, 0, testStringEncoded.getBytes() ) );
    }

    @Test
    @BugId("SSG-6087")
    public void testBase64TrailingCharsAfterPadding() throws Exception {
        // strict
        assertEquals("test", oneWayTest(EncodeDecodeAssertion.TransformType.BASE64_DECODE, AssertionStatus.NONE, true, 0, "dGVzdA=="));
        assertEquals("test", oneWayTest(EncodeDecodeAssertion.TransformType.BASE64_DECODE, AssertionStatus.NONE, true, 0, "dGVzdA="));
        assertEquals("test", oneWayTest(EncodeDecodeAssertion.TransformType.BASE64_DECODE, AssertionStatus.NONE, true, 0, "dGVzdA"));
        // whitespace should be trimmed before padding is validated
        assertEquals("test", oneWayTest(EncodeDecodeAssertion.TransformType.BASE64_DECODE, AssertionStatus.NONE, true, 0, " dGVzdA "));
        assertEquals("test", oneWayTest(EncodeDecodeAssertion.TransformType.BASE64_DECODE, AssertionStatus.NONE, true, 0, " dGVzdA= "));
        assertEquals("test", oneWayTest(EncodeDecodeAssertion.TransformType.BASE64_DECODE, AssertionStatus.NONE, true, 0, " dGVzdA== "));
        assertNull(oneWayTest(EncodeDecodeAssertion.TransformType.BASE64_DECODE, AssertionStatus.FALSIFIED, true, 0, "dGVzdA==x"));
        assertNull(oneWayTest(EncodeDecodeAssertion.TransformType.BASE64_DECODE, AssertionStatus.FALSIFIED, true, 0, "dGVzdA=x"));
        assertNull(oneWayTest(EncodeDecodeAssertion.TransformType.BASE64_DECODE, AssertionStatus.FALSIFIED, true, 0, " dGVzdA==x "));
        assertNull(oneWayTest(EncodeDecodeAssertion.TransformType.BASE64_DECODE, AssertionStatus.FALSIFIED, true, 0, "dGVzdA= x"));

        // non-strict
        assertEquals("test", oneWayTest(EncodeDecodeAssertion.TransformType.BASE64_DECODE, AssertionStatus.NONE, false, 0, "dGVzdA==x"));
        assertEquals("test", oneWayTest(EncodeDecodeAssertion.TransformType.BASE64_DECODE, AssertionStatus.NONE, false, 0, "dGVzdA=x"));
    }

    @Test
    public void testBase16() throws Exception {
        roundTripTest( EncodeDecodeAssertion.TransformType.HEX_ENCODE,
                       EncodeDecodeAssertion.TransformType.HEX_DECODE,
                       false );

        roundTripTest( EncodeDecodeAssertion.TransformType.HEX_ENCODE,
                       EncodeDecodeAssertion.TransformType.HEX_DECODE,
                       true );
    }

    @Test
    public void testZip() throws Exception {
        roundTripTest( EncodeDecodeAssertion.TransformType.ZIP,
                EncodeDecodeAssertion.TransformType.UNZIP,
                DataType.MESSAGE,
                true );
    }

    @Test
    public void testGzip() throws Exception {
        roundTripTest( EncodeDecodeAssertion.TransformType.GZIP,
                EncodeDecodeAssertion.TransformType.GUNZIP,
                DataType.MESSAGE,
                true );
    }

    @Test
    public void testBase16Strict() throws Exception {
        oneWayTest( EncodeDecodeAssertion.TransformType.HEX_DECODE, AssertionStatus.NONE, true, 0, "2F:74 6578742F\r207\n76 \t 974682073706163657320616E64203C223E73" );
        oneWayTest( EncodeDecodeAssertion.TransformType.HEX_DECODE, AssertionStatus.FALSIFIED, true, 0, "2F746578r742F20776974682073706163657320616E64203C223E73" );
    }

    @Test
    public void testURL() throws Exception {
        roundTripTest( EncodeDecodeAssertion.TransformType.URL_ENCODE,
                       EncodeDecodeAssertion.TransformType.URL_DECODE,
                       false );

        roundTripTest( EncodeDecodeAssertion.TransformType.URL_ENCODE,
                       EncodeDecodeAssertion.TransformType.URL_DECODE,
                       true );
    }

    @Test
    public void testMessageInputOutput() throws Exception {
        // Test message input
        final Message message = new Message();
        message.initialize( ContentTypeHeader.TEXT_DEFAULT, "/text/ with spaces and <\">s".getBytes( ContentTypeHeader.TEXT_DEFAULT.getEncoding() ));
        String result1 = (String) oneWayTest( EncodeDecodeAssertion.TransformType.BASE64_ENCODE, AssertionStatus.NONE, true, 0, message );
        assertEquals( "B64 message encoding", "L3RleHQvIHdpdGggc3BhY2VzIGFuZCA8Ij5z", result1 );

        // Test message output
        Message result2 = (Message) oneWayTest( AssertionStatus.NONE, "L3RleHQvIHdpdGggc3BhY2VzIGFuZCA8Ij5z", new Functions.UnaryVoid<EncodeDecodeAssertion>(){
            @Override
            public void call( final EncodeDecodeAssertion assertion ) {
                assertion.setTransformType( EncodeDecodeAssertion.TransformType.BASE64_DECODE );
                assertion.setTargetDataType( DataType.MESSAGE );
                assertion.setTargetContentType( ContentTypeHeader.TEXT_DEFAULT.getFullValue() );
            }
        } );
        assertEquals( "Message text", "/text/ with spaces and <\">s", new String(IOUtils.slurpStream( result2.getMimeKnob().getEntireMessageBodyAsInputStream()), ContentTypeHeader.TEXT_DEFAULT.getEncoding()) );
    }

    @Test
    public void testCertificateInputOutput() throws Exception {
        // Test certificate input
        String certB64 = "MIICazCCAdSgAwIBAgIEPb7rVDANBgkqhkiG9w0BAQQFADB6MRMwEQYDVQQDDApKb2huIFNtaXRoMR8wHQYDVQQLDBZKYXZhIFRlY2hub2xvZ3kgQ2VudGVyMQwwCgYDVQQKDANJQk0xEjAQBgNVBAcMCUN1cGVydGlubzETMBEGA1UECAwKQ2FsaWZvcm5pYTELMAkGA1UEBhMCVVMwHhcNMDUwMTAxMDAwMDAwWhcNMjUxMjMxMjM1OTU5WjB6MRMwEQYDVQQDDApKb2huIFNtaXRoMR8wHQYDVQQLDBZKYXZhIFRlY2hub2xvZ3kgQ2VudGVyMQwwCgYDVQQKDANJQk0xEjAQBgNVBAcMCUN1cGVydGlubzETMBEGA1UECAwKQ2FsaWZvcm5pYTELMAkGA1UEBhMCVVMwgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBAK2K2KzkU42+/bfpfDUIo68oA5DQ1iW9F38UrC5/5PVcIVp0cyu28eGr/5n8OVyfZhBg4Kn1q5L5aQFwvQBSskk9RvBkgHYLIFkmOdLv6N1vftEphBSw1E2WB0hyhkzxu8JmV0FJ+dq3jEM/JA4kHsTEOsyYj20/Q1j0Y3Sel+fDAgMBAAEwDQYJKoZIhvcNAQEEBQADgYEAiA+65PCTbLfkB7OLz5OEQUwySoK16nTY3cXKGrq1rWdHAYmr+FfVF+1ePicihDMVqfzZHeHMlNAvjVRliwP4HuU58OMz3Jn+8iJ0exKH9EKgfFZ7csX7cyXtZfvaMTxlAca04muonxJS0FFqxSFgJNScQELaA6R82wse0hksr7o=";
        final X509Certificate certificate = CertUtils.decodeCert( HexUtils.decodeBase64( certB64 ) );
        String result1 = (String) oneWayTest( EncodeDecodeAssertion.TransformType.BASE64_ENCODE, AssertionStatus.NONE, true, 0, certificate );
        assertEquals( "B64 certificate encoding", certB64, result1 );

        // Test certificate output
        X509Certificate certificate2 = (X509Certificate) oneWayTest( AssertionStatus.NONE, certB64, new Functions.UnaryVoid<EncodeDecodeAssertion>(){
            @Override
            public void call( final EncodeDecodeAssertion assertion ) {
                assertion.setTransformType( EncodeDecodeAssertion.TransformType.BASE64_DECODE );
                assertion.setTargetDataType( DataType.CERTIFICATE );
            }
        } );
        assertEquals( "Certificate B64", certB64, HexUtils.encodeBase64(certificate2.getEncoded(), true));        
    }

    @Test
    public void testOutputFormatting() throws Exception {
        final String sourceText = "test source text with special characters and some non-latin ones test source text with special characters and some non-latin ones test source text with special characters and some non-latin ones test source text with special characters and some non-latin ones \u03d0~`!@#$%^&*()_-+=}]{[|\"':;?/>.<,";
        final String out1 = (String) oneWayTest( EncodeDecodeAssertion.TransformType.BASE64_ENCODE, AssertionStatus.NONE, true, 1, sourceText );
        for ( int i=1; i<out1.length(); i=i+2 ) {
            assertTrue( "b64 line break at " + i, out1.charAt( i ) == '\n' );
        }

        final String out2 = (String) oneWayTest( EncodeDecodeAssertion.TransformType.HEX_ENCODE, AssertionStatus.NONE, true, 64, sourceText );
        for ( int i=64; i<out2.length(); i=i+65 ) {
            assertTrue( "hex line break at " + i, out2.charAt( i ) == '\n' );
        }

        final String out3 = (String) oneWayTest( EncodeDecodeAssertion.TransformType.URL_ENCODE, AssertionStatus.NONE, true, 76, sourceText );
        for ( int i=76; i<out3.length(); i=i+77 ) {
            assertTrue( "url line break at " + i, out3.charAt( i ) == '\n' );
        }
    }

    @Test
    public void testMessageOutput() throws Exception {
        final String sourceText = "test source text with special characters and some non-latin ones test source text with special characters and some non-latin ones test source text with special characters and some non-latin ones test source text with special characters and some non-latin ones \u03d0~`!@#$%^&*()_-+=}]{[|\"':;?/>.<,";
        final Message outputEnc = (Message) oneWayTest(AssertionStatus.NONE, sourceText, new Functions.UnaryVoid<EncodeDecodeAssertion>(){
            @Override
            public void call( final EncodeDecodeAssertion assertion ) {
                assertion.setTransformType( EncodeDecodeAssertion.TransformType.BASE64_ENCODE );
                assertion.setTargetDataType( DataType.MESSAGE );
            }
        } );
        assertNotNull( "Message output encode", outputEnc );
        
        final String sourceB64 = new String( IOUtils.slurpStream( outputEnc.getMimeKnob().getEntireMessageBodyAsInputStream() ));
        final Message outputDec = (Message) oneWayTest(AssertionStatus.NONE, sourceB64, new Functions.UnaryVoid<EncodeDecodeAssertion>(){
            @Override
            public void call( final EncodeDecodeAssertion assertion ) {
                assertion.setTransformType( EncodeDecodeAssertion.TransformType.BASE64_DECODE );
                assertion.setTargetDataType( DataType.MESSAGE );
            }
        } );
        assertNotNull( "Message output decode", outputDec );
    }

    private void roundTripTest( final EncodeDecodeAssertion.TransformType encode,
                                final EncodeDecodeAssertion.TransformType decode,
                                final boolean strict ) throws Exception {
        roundTripTest( encode, decode, DataType.STRING, strict );
    }


    private void roundTripTest( final EncodeDecodeAssertion.TransformType encode,
                                final EncodeDecodeAssertion.TransformType decode,
                                final DataType initialTargetDataType,
                                final boolean strict ) throws Exception {
        final EncodeDecodeAssertion assertion = new EncodeDecodeAssertion();
        assertion.setSourceVariableName( "source" );
        assertion.setTargetVariableName( "output1" );
        assertion.setTargetDataType( initialTargetDataType );
        assertion.setTransformType( encode );

        final String sourceText = "test source text with special characters and some non-latin ones \u03d0~`!@#$%^&*()_-+=}]{[|\"':;?/>.<,";
        final PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext( null, null );
        pec.setVariable( "source", sourceText );

        final ServerEncodeDecodeAssertion serverEncodeDecodeAssertion = new ServerEncodeDecodeAssertion( assertion );
        final AssertionStatus status = serverEncodeDecodeAssertion.checkRequest( pec );

        assertEquals( "Encode status", AssertionStatus.NONE, status );
        assertFalse( "Output not encoded", sourceText.equals( pec.getVariable( "output1" )));

        assertion.setStrict( strict );
        assertion.setSourceVariableName( "output1" );
        assertion.setTargetVariableName( "output2" );
        assertion.setTransformType( decode );
        assertion.setTargetDataType( DataType.STRING );

        final ServerEncodeDecodeAssertion serverEncodeDecodeAssertion2 = new ServerEncodeDecodeAssertion( assertion );
        final AssertionStatus status2 = serverEncodeDecodeAssertion2.checkRequest( pec );

        assertEquals( "Decode status", AssertionStatus.NONE, status2 );
        assertEquals( "Round trip text mismatch", sourceText, pec.getVariable( "output2" ) );
    }

    private Object oneWayTest( final EncodeDecodeAssertion.TransformType encode,
                               final AssertionStatus expectedStatus,
                               final boolean strict,
                               final int lineBreaks,
                               final Object data ) throws Exception {
        return oneWayTest( expectedStatus, data, new Functions.UnaryVoid<EncodeDecodeAssertion>(){
            @Override
            public void call( final EncodeDecodeAssertion assertion ) {
                assertion.setTransformType( encode );
                assertion.setStrict( strict );
                assertion.setLineBreakInterval( lineBreaks );
            }
        } );
    }

    private Object oneWayTest( final AssertionStatus expectedStatus,
                               final Object data,
                               final Functions.UnaryVoid<EncodeDecodeAssertion> configCallback ) throws Exception {
        final EncodeDecodeAssertion assertion = new EncodeDecodeAssertion();
        assertion.setSourceVariableName( "source" );
        assertion.setTargetVariableName( "target" );
        assertion.setTargetDataType( DataType.STRING );
        if ( configCallback!=null ) configCallback.call( assertion );

        final PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext( null, null );
        pec.setVariable( "source", data );

        final ServerEncodeDecodeAssertion serverEncodeDecodeAssertion = new ServerEncodeDecodeAssertion( assertion );
        AssertionStatus status;
        try {
            status = serverEncodeDecodeAssertion.checkRequest( pec );
        } catch ( AssertionStatusException e ) {
            status = e.getAssertionStatus();
        }
        assertEquals( "Decode status", expectedStatus, status );

        return status==AssertionStatus.NONE ? pec.getVariable( "target" ) : null;
    }
}
