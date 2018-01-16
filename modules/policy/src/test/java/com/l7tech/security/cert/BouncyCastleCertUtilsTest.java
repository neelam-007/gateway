package com.l7tech.security.cert;

import com.l7tech.common.io.CertGenParams;
import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.X509GeneralName;

import com.l7tech.util.IOUtils;
import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.pkcs.CertificationRequestInfo;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.junit.After;
import org.junit.Before;
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

        assertEquals(2, certGenParams.getSubjectAlternativeNames().size());
        assertEquals(X509GeneralName.Type.dNSName, certGenParams.getSubjectAlternativeNames().get(0).getType());
        assertEquals("test.ca.com", certGenParams.getSubjectAlternativeNames().get(0).getStringVal());
    }
}