package com.l7tech.security.cert;

import com.l7tech.common.io.CertGenParams;
import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.CertificateGeneratorException;
import com.l7tech.common.io.X509GeneralName;

import com.l7tech.security.prov.CertificateRequest;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.util.IOUtils;
import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.pkcs.CertificationRequestInfo;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.operator.ContentVerifierProvider;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.junit.Test;

import javax.security.auth.x500.X500Principal;
import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.*;

import static org.junit.Assert.*;
import static org.junit.Assert.assertTrue;

public class BouncyCastleCertUtilsTest {

    @Test
    // Test creating a simple self-signed certificate.
    public void testGenerateSelfSignedCertificate() {

        final String subjectDn = "CN=FirstNameTest LastNameTest, L=Vancouver, ST=BC, O=CA Technologies, C=CA";
        final String hashAlg = "SHA256withRSA";
        final Date startDate = new Date();              // time from which certificate is valid
        final Date expiryDate = addDays(startDate, 10);          // time after which certificate is not valid
        final BigInteger certSerialNumber = new BigInteger(Long.toString(System.currentTimeMillis()));

        final KeyPair keyPair = JceProvider.getInstance().generateRsaKeyPair();
        final CertGenParams certGenParams = new CertGenParams();
        certGenParams.setSubjectDn(new X500Principal(subjectDn));
        certGenParams.setHashAlgorithm(hashAlg);
        certGenParams.setNotBefore(startDate);
        certGenParams.setNotAfter(expiryDate);
        certGenParams.setSerialNumber(certSerialNumber);

        try {
            final X509Certificate x509Certificate = BouncyCastleCertUtils.generateSelfSignedCertificate(certGenParams, keyPair);

            // verify the attributes from the certificate are equal to the ones used for input.
            assertTrue(x500PrincipalEquals(subjectDn, x509Certificate.getSubjectDN().toString()));
            assertEquals(startDate.toString(), x509Certificate.getNotBefore().toString());
            assertEquals(expiryDate.toString(), x509Certificate.getNotAfter().toString());
            assertEquals(certSerialNumber, x509Certificate.getSerialNumber());
            assertTrue(x500PrincipalEquals(subjectDn, x509Certificate.getIssuerDN().toString())); // make sure the issuerDn is the same as the subject DN.
            assertTrue(keyPair.getPublic().equals(x509Certificate.getPublicKey()));

        } catch (CertificateGeneratorException e) {
            fail("CertificateGeneratorException caught while trying to generate a self-signed certificate.");
        }
    }


    @Test
    // Test creating a self-signed certificate with subject alternative name attributes.
    public void testGenerateSelfSignedCertificateWithSubjAlternativeNames() {

        final int numberALternativeNames = 5;
        final String Ip1AltName = "10.242.15.115";
        final String Ip2AltName = "10.242.15.172";
        final String DNSAltname = "support.example.com";
        final String UriAltName = "https://tst.ca.com:8443/ssl";
        final String emailAltName = "john.smith@example.com";

        final String subjectDn = "CN=FirstNameTest2 LastNameTest2, L=Vancouver, ST=BC, O=CA Technologies, C=CA";
        final String hashAlg = "SHA224withRSA";
        final Date startDate = new Date();              // time from which certificate is valid
        final Date expiryDate = addDays(startDate, 31);          // time after which certificate is not valid
        final BigInteger certSerialNumber = new BigInteger(Long.toString(System.currentTimeMillis()));

        final KeyPair keyPair = JceProvider.getInstance().generateRsaKeyPair();
        final CertGenParams certGenParams = new CertGenParams();
        certGenParams.setSubjectDn(new X500Principal(subjectDn));
        certGenParams.setHashAlgorithm(hashAlg);
        certGenParams.setNotBefore(startDate);
        certGenParams.setNotAfter(expiryDate);
        certGenParams.setSerialNumber(certSerialNumber);

        final List<X509GeneralName> subjectAltNamesInputs = new ArrayList<>();
        subjectAltNamesInputs.add(X509GeneralName.fromIpAddress(Ip1AltName));
        subjectAltNamesInputs.add(X509GeneralName.fromIpAddress(Ip2AltName));
        subjectAltNamesInputs.add(X509GeneralName.fromDnsName(DNSAltname));
        subjectAltNamesInputs.add(new X509GeneralName(X509GeneralName.Type.uniformResourceIdentifier, UriAltName));
        subjectAltNamesInputs.add(new X509GeneralName(X509GeneralName.Type.rfc822Name, emailAltName));

        certGenParams.setSubjectAlternativeNames(subjectAltNamesInputs);
        certGenParams.setIncludeSubjectAlternativeName(true);

        try {
            final X509Certificate x509Certificate = BouncyCastleCertUtils.generateSelfSignedCertificate(certGenParams, keyPair);

            // verify the attributes from the certificate are equal to the ones used for input.
            assertTrue(x500PrincipalEquals(subjectDn, x509Certificate.getSubjectDN().getName()));
            assertEquals(startDate.toString(), x509Certificate.getNotBefore().toString());
            assertEquals(expiryDate.toString(), x509Certificate.getNotAfter().toString());
            assertEquals(certSerialNumber, x509Certificate.getSerialNumber());
            assertTrue(x500PrincipalEquals(subjectDn, x509Certificate.getIssuerDN().toString())); // make sure the issuerDn is the same as the subject DN.
            assertTrue(keyPair.getPublic().equals(x509Certificate.getPublicKey()));

            final Collection<List<?>> subjectAlternativeNames = x509Certificate.getSubjectAlternativeNames();
            final List<X509GeneralName> genNamesFromCert = X509GeneralName.fromList(subjectAlternativeNames);

            // determine if the number of alternative names from the certificate equals the number that was inserted.
            assertEquals(genNamesFromCert.size(),numberALternativeNames);

            // match the subject alternative names found in the certificate with the ones used to generate the certificate.
            assertTrue(genNamesFromCert.get(0).getType() == X509GeneralName.Type.iPAddress);
            assertTrue(genNamesFromCert.get(0).getStringVal().equals(Ip1AltName));

            assertTrue(genNamesFromCert.get(1).getType() == X509GeneralName.Type.iPAddress);
            assertTrue(genNamesFromCert.get(1).getStringVal().equals(Ip2AltName));

            assertTrue(genNamesFromCert.get(2).getType() == X509GeneralName.Type.dNSName);
            assertTrue(genNamesFromCert.get(2).getStringVal().equals(DNSAltname));

            assertTrue(genNamesFromCert.get(3).getType() == X509GeneralName.Type.uniformResourceIdentifier);
            assertTrue(genNamesFromCert.get(3).getStringVal().equals(UriAltName));

            assertTrue(genNamesFromCert.get(4).getType() == X509GeneralName.Type.rfc822Name);
            assertTrue(genNamesFromCert.get(4).getStringVal().equals(emailAltName));

        } catch (CertificateGeneratorException | CertificateParsingException e) {
            fail("CertificateGeneratorException caught while trying to generate a self-signed certificate.");
        }
    }


    @Test
    public void testGeneratePkcs10CertificateSigningRequest() {

        final String subjectDn = "CN=FirstNameTest3 LastNameTest3, L=Vancouver, ST=BC, O=CA Technologies, C=CA";
        final X500Principal x500PrincipalSubjDnInput = new X500Principal(subjectDn);
        final int expiryDays = 27;
        final String sigAlg = "SHA256withRSA";
        final KeyPair keyPair = JceProvider.getInstance().generateRsaKeyPair();

        final CertGenParams params = new CertGenParams(x500PrincipalSubjDnInput, expiryDays, false, sigAlg);
        final CertificateRequest certificateRequest;

        try {
            certificateRequest = BouncyCastleCertUtils.makeCertificateRequest(params, keyPair);
            final X500Principal x500PrincipalFromCert = new X500Principal(certificateRequest.getSubjectAsString());

            // verify the subject DN from the certificate request l7tech object
            assertTrue(x500PrincipalEquals(x500PrincipalSubjDnInput.getName(), x500PrincipalFromCert.getName()));

            // verify public key
            assertTrue(keyPair.getPublic().equals(certificateRequest.getPublicKey()));

            // verify the signature
            final PKCS10CertificationRequest pkcs10 = new PKCS10CertificationRequest(certificateRequest.getEncoded());
            final ContentVerifierProvider verifierProvider = new JcaContentVerifierProviderBuilder().build(certificateRequest.getPublicKey());
            assertTrue(pkcs10.isSignatureValid(verifierProvider));

            // verify the subject DN from the PKCS10CertificationRequest obj
            assertTrue(x500PrincipalEquals(x500PrincipalSubjDnInput.getName(), pkcs10.getSubject().toString()));

            //compare signature algorithms used.
            final String sigAlgFromCert = pkcs10.getSignatureAlgorithm().getAlgorithm().getId();
            final DefaultSignatureAlgorithmIdentifierFinder sigAlgIdFinder = new DefaultSignatureAlgorithmIdentifierFinder();
            final String inputSigAlgId = sigAlgIdFinder.find(sigAlg).getAlgorithm().getId();
            assertEquals(inputSigAlgId, sigAlgFromCert);

        } catch (Exception e) {
            fail("Exception caught while generating certificate or verifying signature.");
        }
    }


    @Test
    public void testReadCsrAndExtractSubjectAlternativeNamesFromAttributes() throws Exception {
        final InputStream in = BouncyCastleCertUtils.class.getClassLoader().getResourceAsStream("com/l7tech/security/cert/test.csr.pem");
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        IOUtils.copyStream(in, bos);
        final byte[] csrBytes = bos.toByteArray();

        final CertGenParams certGenParams = new CertGenParams();
        final byte[] decodedCsrBytes = CertUtils.csrPemToBinary(csrBytes);
        final PKCS10CertificationRequest pkcs10 = new PKCS10CertificationRequest(decodedCsrBytes);
        final CertificationRequestInfo certReqInfo = pkcs10.toASN1Structure().getCertificationRequestInfo();
        certGenParams.setSubjectDn(new X500Principal(certReqInfo.getSubject().getEncoded()));
        final ASN1Set attrSet = certReqInfo.getAttributes();
        final List collection = BouncyCastleCertUtils.extractSubjectAlternativeNamesFromCsrInfoAttr(attrSet);
        certGenParams.setSubjectAlternativeNames(collection);

        final DEROctetString derIpAddress = new DEROctetString(new byte[]{10, 7, 10, 10});
        final byte[] otherValue = new byte[]{-96, 19, 6, 3, 42, 3, 4, -96, 12, 12, 10, 105, 100, 101, 110, 116, 105, 102, 105, 101, 114};

        assertEquals(9, certGenParams.getSubjectAlternativeNames().size());
        assertEquals(X509GeneralName.Type.dNSName, certGenParams.getSubjectAlternativeNames().get(0).getType());
        assertEquals("your-new-domain.com", certGenParams.getSubjectAlternativeNames().get(0).getStringVal());
        assertEquals(X509GeneralName.Type.dNSName, certGenParams.getSubjectAlternativeNames().get(1).getType());
        assertEquals("www.your-new-domain.com", certGenParams.getSubjectAlternativeNames().get(1).getStringVal());
        assertEquals(X509GeneralName.Type.rfc822Name, certGenParams.getSubjectAlternativeNames().get(2).getType());
        assertEquals("test@ca.com", certGenParams.getSubjectAlternativeNames().get(2).getStringVal());
        assertEquals(X509GeneralName.Type.iPAddress, certGenParams.getSubjectAlternativeNames().get(3).getType());
        assertArrayEquals(derIpAddress.getEncoded(), certGenParams.getSubjectAlternativeNames().get(3).getDerVal());
        assertEquals(X509GeneralName.Type.registeredID, certGenParams.getSubjectAlternativeNames().get(4).getType());
        assertEquals("1.2.3.4", certGenParams.getSubjectAlternativeNames().get(4).getStringVal());
        assertEquals(X509GeneralName.Type.uniformResourceIdentifier, certGenParams.getSubjectAlternativeNames().get(5).getType());
        assertEquals("urn:ISSN:1535-3613", certGenParams.getSubjectAlternativeNames().get(5).getStringVal());
        assertEquals(X509GeneralName.Type.uniformResourceIdentifier, certGenParams.getSubjectAlternativeNames().get(6).getType());
        assertEquals("http://appserver:6394/wa/r/myApp", certGenParams.getSubjectAlternativeNames().get(6).getStringVal());
        assertEquals(X509GeneralName.Type.otherName, certGenParams.getSubjectAlternativeNames().get(7).getType());
        assertArrayEquals(otherValue, certGenParams.getSubjectAlternativeNames().get(7).getDerVal());
        assertEquals(X509GeneralName.Type.directoryName, certGenParams.getSubjectAlternativeNames().get(8).getType());
        assertEquals("C=UK,O=My Organization,OU=My Unit,CN=My Name", certGenParams.getSubjectAlternativeNames().get(8).getStringVal());
    }

    @Test
    public void testGetSubjectAlternativeNameExtensions() throws Exception {
        final CertGenParams params = new CertGenParams();
        params.setSubjectDn(new X500Principal("cn=test"));
        params.setIncludeSubjectAlternativeName(true);
        final List<X509GeneralName> sANs = new ArrayList<>();
        sANs.add(new X509GeneralName(X509GeneralName.Type.dNSName, "test.ca.com"));
        sANs.add(new X509GeneralName(X509GeneralName.Type.iPAddress, "111.222.33.44"));
        params.setSubjectAlternativeNames(sANs);
        final Extensions extensions = BouncyCastleCertUtils.getSubjectAlternativeNamesExtensions(params);
        final ASN1ObjectIdentifier[] expectedOids = {Extension.subjectAlternativeName};
        assertArrayEquals(expectedOids, extensions.getExtensionOIDs());
        final Extension extension1 = extensions.getExtension(expectedOids[0]);
        final ASN1OctetString actual = extension1.getExtnValue();
        assertEquals("#3013820b746573742e63612e636f6d87046fde212c", actual.toString());
    }

    private static Date addDays(Date date, int days) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DATE, days);
        return cal.getTime();
    }

    private static boolean x500PrincipalEquals(String subjectDn1, String subjectDn2) {

        final X500Principal x500Principal1 = new X500Principal(subjectDn1);
        final X500Principal x500Principal2 = new X500Principal(subjectDn2);
        return x500Principal1.equals(x500Principal2);
    }
}