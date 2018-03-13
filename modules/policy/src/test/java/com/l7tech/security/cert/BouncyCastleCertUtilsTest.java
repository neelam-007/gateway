package com.l7tech.security.cert;

import com.l7tech.common.io.CertGenParams;
import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.X509GeneralName;

import com.l7tech.util.IOUtils;
import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.pkcs.CertificationRequestInfo;
import org.bouncycastle.asn1.x509.X509Extension;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.junit.Test;
import sun.security.util.DerValue;

import javax.security.auth.x500.X500Principal;
import java.io.*;
import java.util.*;

import static org.junit.Assert.*;

public class BouncyCastleCertUtilsTest {

    @Test
    public void testReadCsrAndExtractSubjectAlternativeNamesFromAttributes()  throws Exception{
        InputStream in = BouncyCastleCertUtils.class.getClassLoader().getResourceAsStream("com/l7tech/security/cert/test.csr.pem");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        IOUtils.copyStream(in, bos);
        byte[] csrBytes = bos.toByteArray();

        CertGenParams certGenParams = new CertGenParams();
        byte[] decodedCsrBytes = CertUtils.csrPemToBinary(csrBytes);
        PKCS10CertificationRequest pkcs10 = new PKCS10CertificationRequest(decodedCsrBytes);
        CertificationRequestInfo certReqInfo = pkcs10.getCertificationRequestInfo();
        certGenParams.setSubjectDn(new X500Principal(certReqInfo.getSubject().getEncoded()));
        ASN1Set attrSet = certReqInfo.getAttributes();
        List collection = BouncyCastleCertUtils.extractSubjectAlternativeNamesFromCsrInfoAttr(attrSet);
        certGenParams.setSubjectAlternativeNames(collection);

        DEROctetString derIpAddress = new DEROctetString(new byte[]{10,7,10,10});
        byte[] otherValue = new byte[]{-96,19,6,3,42,3,4,-96,12,12,10,105,100,101,110,116,105,102,105,101,114};

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
        CertGenParams params = new CertGenParams();
        params.setSubjectDn(new X500Principal("cn=test"));
        params.setIncludeSubjectAlternativeName(true);
        List<X509GeneralName> sANs = new ArrayList<>();
        sANs.add(new X509GeneralName(X509GeneralName.Type.dNSName, "test.ca.com"));
        sANs.add(new X509GeneralName(X509GeneralName.Type.iPAddress, "111.222.33.44"));
        params.setSubjectAlternativeNames(sANs);
        X509Extensions extension =  BouncyCastleCertUtils.getSubjectAlternativeNamesExtensions(params);
        ASN1ObjectIdentifier[] expectedOids = {new ASN1ObjectIdentifier("2.5.29.17")};
        assertArrayEquals(expectedOids, extension.getExtensionOIDs());
        X509Extension extension1 = extension.getExtension(expectedOids[0]);
        ASN1OctetString actual = extension1.getValue();
        assertEquals("#3013820b746573742e63612e636f6d87046fde212c",actual.toString());
    }
}