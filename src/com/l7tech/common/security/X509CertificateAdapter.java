package com.l7tech.common.security;

import com.l7tech.common.security.TrustedCert;
import com.l7tech.common.io.BufferPoolByteArrayOutputStream;
import com.l7tech.common.util.HexUtils;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ByteArrayInputStream;
import java.security.cert.X509Certificate;

/**
 * Created by IntelliJ IDEA.
 * User: darmstrong
 * Date: Apr 17, 2008
 * Time: 2:41:42 PM
 * X509Certificate cannot be marshalled directly by JAXB due to not having a default constructor.
 * Also we need to convert the certificate to base64 for storing as XML. This class will be called
 * when the X509Certificate is being marshalled and unmarshalled.
 */
public class X509CertificateAdapter extends XmlAdapter<String, X509Certificate> {

    public X509Certificate unmarshal(String v) throws Exception {
        byte [] bytes = HexUtils.decodeBase64(v);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        ObjectInputStream inStream = new ObjectInputStream(inputStream);
        X509Certificate cert = (X509Certificate) inStream.readObject();
        return cert;
    }

    public String marshal(X509Certificate v) throws Exception {

        BufferPoolByteArrayOutputStream outBuf = null;
        ObjectOutputStream outStream = null;
        
        try{
            outBuf = new BufferPoolByteArrayOutputStream(1024);
            outStream = new ObjectOutputStream(outBuf);
            outStream.writeObject(v);
            byte [] bytes = outBuf.detachPooledByteArray();
            String returnString = HexUtils.encodeBase64(bytes);
            return returnString;
        }finally{
            outStream.close();//not needed due to detach
        }
    }
}
