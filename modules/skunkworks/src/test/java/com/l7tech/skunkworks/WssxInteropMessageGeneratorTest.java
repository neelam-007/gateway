package com.l7tech.skunkworks;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.security.token.UsernameTokenImpl;
import com.l7tech.util.HexUtils;
import com.l7tech.xml.soap.SoapUtil;
import org.junit.*;
import org.w3c.dom.Element;

import java.util.logging.Logger;

/**
 *
 */
public class WssxInteropMessageGeneratorTest {
    private static final Logger log = Logger.getLogger(WssxInteropMessageGeneratorTest.class.getName());


    @Test
    public void test_2113_UsernameTokenRequest() throws Exception {
        WssxInteropMessageGenerator gen = new WssxInteropMessageGenerator();
        final UsernameTokenImpl utok = new UsernameTokenImpl("Alice", "ecilA".toCharArray(), null, HexUtils.randomBytes(16), true);
        gen.dreq().setUsernameTokenCredentials(utok);
        log.info(XmlUtil.nodeToFormattedString(gen.generateRequest()));
    }

    // Request encrypted but not signed
    @Test
    public void test_2131_WSS10_EncryptedUsernameToken() throws Exception {
        WssxInteropMessageGenerator gen = new WssxInteropMessageGenerator();
        final UsernameTokenImpl utok = new UsernameTokenImpl("Alice", "ecilA".toCharArray(), null, HexUtils.randomBytes(16), true);
        gen.dreq().setUsernameTokenCredentials(utok);
        gen.dreq().setEncryptionAlgorithm("http://www.w3.org/2001/04/xmlenc#aes256-cbc");
        gen.dreq().setEncryptUsernameToken(true);
        gen.dreq().getElementsToSign().clear();
        gen.dreq().getElementsToEncrypt().add(SoapUtil.getBodyElement(gen.doc));
        gen.dreq().setRecipientCertificate(gen.getBobInfo().getCertificate());
        gen.dreq().setTimestampTimeoutMillis(2000000000);
        log.info(XmlUtil.nodeToFormattedString(gen.generateRequest()));
    }
    
    // Uses wss11 so request can be "signed" using same DKT that was used to sign and encrypt the usernametoken
    @Test
    public void test_214_WSS11_EncryptedUsernameToken() throws Exception {
        WssxInteropMessageGenerator gen = new WssxInteropMessageGenerator();
        final UsernameTokenImpl utok = new UsernameTokenImpl("Alice", "ecilA".toCharArray());
        gen.dreq().setUsernameTokenCredentials(utok);
        gen.dreq().setEncryptUsernameToken(true);
        gen.dreq().setSignUsernameToken(true);
        gen.dreq().setSignTimestamp();
        gen.dreq().setEncryptionAlgorithm("http://www.w3.org/2001/04/xmlenc#aes256-cbc");
        final Element bodyEl = SoapUtil.getBodyElement(gen.doc);
        gen.dreq().getElementsToEncrypt().add(bodyEl);
        gen.dreq().getElementsToSign().add(bodyEl);
        gen.dreq().setRecipientCertificate(gen.getBobInfo().getCertificate());
        gen.dreq().setTimestampTimeoutMillis(2000000000);
        gen.generateRequest();
        log.info("Reformatted: " + XmlUtil.nodeToFormattedString(gen.doc));
        log.info("Not reformatted: " + XmlUtil.nodeToString(gen.doc));
    }

    @Test
    public void test_222_WSS10_X509MutualAuth() throws Exception {
        WssxInteropMessageGenerator gen = new WssxInteropMessageGenerator();
        gen.dreq().setSenderMessageSigningCertificate(gen.getAliceInfo().getCertificate());
        gen.dreq().setSenderMessageSigningPrivateKey(gen.getAliceInfo().getPrivate());
        gen.dreq().setRecipientCertificate(gen.getBobInfo().getCertificate());
        gen.dreq().setSignTimestamp();
        gen.dreq().setEncryptionAlgorithm("http://www.w3.org/2001/04/xmlenc#aes256-cbc");
        final Element bodyEl = SoapUtil.getBodyElement(gen.doc);
        gen.dreq().getElementsToEncrypt().add(bodyEl);
        gen.dreq().getElementsToSign().add(bodyEl);
        gen.dreq().setTimestampTimeoutMillis(2000000000);
        gen.generateRequest();
        log.info("Reformatted: " + XmlUtil.nodeToFormattedString(gen.doc));
        log.info("Not reformatted: " + XmlUtil.nodeToString(gen.doc));
    }
}
