package com.l7tech.security.xml.processor;

import com.l7tech.security.xml.SupportedSignatureMethods;
import com.l7tech.util.HexUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.*;
import static org.junit.Assert.*;
import org.apache.harmony.security.asn1.ASN1Sequence;
import org.apache.harmony.security.asn1.ASN1Type;
import org.apache.harmony.security.asn1.ASN1Integer;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Random;
import java.util.zip.GZIPInputStream;

/**
 *
 */
public class EcdsaSignatureMethodTest {
    private static Provider addedProvider = null;
    private static final boolean IS_WINDOWS = System.getProperty("os.name").startsWith("Win");
    Random random = new Random(1L);

    @BeforeClass
    public static void initProviders() {
        if (Security.getProvider("BC") == null) {
            addedProvider = new BouncyCastleProvider();
            Security.insertProviderAt(addedProvider, 1);
        }
    }

    @AfterClass
    public static void cleanupProviders() {
        if (addedProvider != null) {
            Security.removeProvider(addedProvider.getName());
        }
    }

    @Test
    public void testApacheSignature() throws Exception {
        // Test replying the Apache signature through our ECDSA signature method to ensure that it verifies

        // We need to use the Bouncy Castle key factory because the signing key uses explicit EC curve params (instead of a named curve)
        PublicKey bcPublicKey = KeyFactory.getInstance("EC", new BouncyCastleProvider()).generatePublic(new X509EncodedKeySpec(HexUtils.unHexDump(
                "308201333081ec06072a8648ce3d02013081e0020101302c06072a8648ce3d0101022100fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffd97" +
                "30440420fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffd940420000000000000000000000000000000000000000000000000000000000000" +
                "00a60441040000000000000000000000000000000000000000000000000000000000000001726e1b8e1f676325d820afa5bac0d489cad6b0d220dc1c4edd5336636160df" +
                "83022100ffffffffffffffffffffffffffffffff6c611070995ad10045841b09b761b8930201010342000466e6f3e345a243ebff9eb8e17e2cd8984225b4a22bfe7ee1c0" +
                "6c291b7083124dce4c39bc90068b2848157da99d2ff45a47c131acfeef7d1c95ce42c891f404ad"
        )));
        PublicKey publicKey = (PublicKey)KeyFactory.getInstance("EC").translateKey(bcPublicKey);

        byte[] dataSigned = HexUtils.unHexDump(
                "3c64733a5369676e6564496e666f20786d6c6e733a64733d22687474703a2f2f7777772e77332e6f72672f323030302f30392f786d6c6473696723223e0a0a3c64733a43" +
                "616e6f6e6963616c697a6174696f6e4d6574686f6420416c676f726974686d3d22687474703a2f2f7777772e77332e6f72672f323030312f31302f786d6c2d6578632d63" +
                "31346e23223e3c2f64733a43616e6f6e6963616c697a6174696f6e4d6574686f643e0a3c64733a5369676e61747572654d6574686f6420416c676f726974686d3d226874" +
                "74703a2f2f7777772e77332e6f72672f323030312f30342f786d6c647369672d6d6f72652365636473612d73686131223e3c2f64733a5369676e61747572654d6574686f" +
                "643e0a3c64733a5265666572656e6365205552493d22223e0a3c64733a5472616e73666f726d733e0a3c64733a5472616e73666f726d20416c676f726974686d3d226874" +
                "74703a2f2f7777772e77332e6f72672f323030302f30392f786d6c6473696723656e76656c6f7065642d7369676e6174757265223e3c2f64733a5472616e73666f726d3e" +
                "0a3c64733a5472616e73666f726d20416c676f726974686d3d22687474703a2f2f7777772e77332e6f72672f54522f323030312f5245432d786d6c2d6331346e2d323030" +
                "31303331352357697468436f6d6d656e7473223e3c2f64733a5472616e73666f726d3e0a3c2f64733a5472616e73666f726d733e0a3c64733a4469676573744d6574686f" +
                "6420416c676f726974686d3d22687474703a2f2f7777772e77332e6f72672f323030302f30392f786d6c647369672373686131223e3c2f64733a4469676573744d657468" +
                "6f643e0a3c64733a44696765737456616c75653e4c4b7955704e615a4a326a6f7a6e567a77457570354a44777453303d3c2f64733a44696765737456616c75653e0a3c2f" +
                "64733a5265666572656e63653e0a3c2f64733a5369676e6564496e666f3e"
        );

        byte[] dsigSignatureValue = HexUtils.unHexDump(
                "4266b9239019892cd0e89e146708e9b5e0f6aaf41c94004a0133d0e4c67b9a638561f8fcc409695d6bb6bbe39aff89a93fd8caf4e51471353d3ec7ee733faa3c"
        );

        EcdsaSignatureMethod meth = new EcdsaSignatureMethod("SHA1withECDSA", SupportedSignatureMethods.ECDSA_SHA1.getAlgorithmIdentifier(), null);

        meth.initVerify(publicKey);
        meth.update(dataSigned);
        boolean result = meth.verify(dsigSignatureValue);

        assertTrue("Signature must verify", result);
    }

    @Test
    public void testConversionBehavesSameAsApache() throws Exception {

        InputStream rawin;
        GZIPInputStream in;
        BufferedReader reader;

        if (IS_WINDOWS) {
            rawin = EcdsaSignatureMethodTest.class.getClassLoader().getResourceAsStream("com/l7tech/security/xml/apacheEcdsaTestVectors.dat");
            assertNotNull("Test data file not found, apacheEcdsaTestVectors.dat exists?", rawin);
            reader = new BufferedReader(new InputStreamReader(rawin));
        } else {
            rawin = EcdsaSignatureMethodTest.class.getClassLoader().getResourceAsStream("com/l7tech/security/xml/apacheEcdsaTestVectors.gz");
            assertNotNull("Test data file not found; need to add *.gz to resource file extensions?", rawin);
            in = new GZIPInputStream(rawin);
            reader = new BufferedReader(new InputStreamReader(in));
        }

        for (int i = 0;;++i) {
            String line = reader.readLine();
            if (line == null)
                break;
            if (line.trim().length() < 1 || line.charAt(0) == '#')
                continue;
            String[] parts = line.split(",");

            byte[] orig_rs = HexUtils.unHexDump(parts[0]);
            byte[] apache_asn1 = HexUtils.unHexDump(parts[1]);
            byte[] apache_decoded = HexUtils.unHexDump(parts[2]);

            byte[] our_asn1 = EcdsaSignatureMethod.encodeAsn1(orig_rs);
            byte[] our_decoded = EcdsaSignatureMethod.decodeAsn1(apache_asn1);

            assertEquals("iter " + i + ": Our decoder must produce same output as Apache decoder given same input", HexUtils.hexDump(apache_decoded), HexUtils.hexDump(our_decoded));
            assertEquals("iter " + i + ": Our encoder must produce same output as Apache encoder given same input", HexUtils.hexDump(apache_asn1), HexUtils.hexDump(our_asn1));

            assertTrue("iter " + i + ": Our encoder must produce same output as Apache encoder given same input", Arrays.equals(apache_asn1, our_asn1));
            assertTrue("iter " + i + ": Our decoder must produce same output as Apache decoder given same input", Arrays.equals(apache_decoded, our_decoded));
        }
    }

    @Test
    public void testLargeFieldSize_pos_pos() throws Exception {
        doTestLargeField((byte) 1, (byte) 1);
    }

    @Test
    public void testLargeFieldSize_zero_zero() throws Exception {
        doTestLargeField((byte) 0, (byte) 0);
    }

    @Test
    public void testLargeFieldSize_neg_neg() throws Exception {
        doTestLargeField((byte) 253, (byte) 222);
    }

    @Test
    public void testLargeFieldSize_pos_neg() throws Exception {
        doTestLargeField((byte) 1, (byte) 222);
    }

    @Test
    public void testLargeFieldSize_neg_pos() throws Exception {
        doTestLargeField((byte) -1, (byte) 1);
    }

    @Test
    public void testLargeFieldSize_pos_zero() throws Exception {
        doTestLargeField((byte) 1, (byte) 0);
    }

    @Test
    public void testLargeFieldSize_zero_neg() throws Exception {
        doTestLargeField((byte) 0, (byte) 199);
    }        

    private void doTestLargeField(byte rf, byte sf) throws SignatureException, IOException {
        // The apache test vector does not include any signature value larger than a 480 bit field size (120 byte signature)
        // because its encoder generates an incorrect ASN.1 sequence for (r,s) values larger than this; and its
        // decoder does not handle sequences larger than this (although it correctly detects that it cannot handle it,
        // and throws an exception).

        // We will need to ensure that this works if we want to support curves larger than 480 bits (such as P-521).
        byte[] rs = new byte[128];
        random.nextBytes(rs);

        if (rs[0] == 0) rs[0] = rf;
        if (rs[128/2] == 0) rs[128/2] = sf;

        byte[] asn1 = EcdsaSignatureMethod.encodeAsn1(rs);
        byte[] decoded = EcdsaSignatureMethod.decodeAsn1(asn1);

        assertEquals("Decoded must match original raw value", HexUtils.hexDump(rs), HexUtils.hexDump(decoded));
        assertTrue(Arrays.equals(rs, decoded));

        // Make sure a real ASN.1 parser is happy with the encoded form
        ASN1Sequence seq_int_int = new ASN1Sequence(new ASN1Type[]{ new ASN1Integer(), new ASN1Integer() });
        Object[] seq = (Object[])seq_int_int.decode(asn1);
        assertEquals(2, seq.length);
        assertTrue((seq[0] instanceof byte[]));
        assertTrue((seq[1] instanceof byte[]));
    }
}
