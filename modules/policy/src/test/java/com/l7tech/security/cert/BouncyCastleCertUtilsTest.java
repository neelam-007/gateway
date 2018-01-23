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

        DEROctetString derIpAddress = new DEROctetString(new byte[]{111,44,23,45});

        assertEquals(3, certGenParams.getSubjectAlternativeNames().size());
        assertEquals(X509GeneralName.Type.rfc822Name, certGenParams.getSubjectAlternativeNames().get(0).getType());
        assertEquals("test@ca.com", certGenParams.getSubjectAlternativeNames().get(0).getStringVal());
        assertEquals(X509GeneralName.Type.iPAddress, certGenParams.getSubjectAlternativeNames().get(1).getType());
        assertArrayEquals(derIpAddress.getEncoded(), certGenParams.getSubjectAlternativeNames().get(1).getDerVal());
        assertEquals(X509GeneralName.Type.dNSName, certGenParams.getSubjectAlternativeNames().get(2).getType());
        assertEquals("test.ca.com", certGenParams.getSubjectAlternativeNames().get(2).getStringVal());
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