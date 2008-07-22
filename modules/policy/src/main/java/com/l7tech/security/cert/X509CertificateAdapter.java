package com.l7tech.security.cert;

import com.l7tech.common.io.BufferPoolByteArrayOutputStream;
import com.l7tech.util.HexUtils;
import com.l7tech.util.ResourceUtils;

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

    public X509Certificate unmarshal(final String value) throws Exception {
        byte [] bytes = HexUtils.decodeBase64(value);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        ObjectInputStream inStream = new ObjectInputStream(inputStream);
        return (X509Certificate) inStream.readObject();
    }

    public String marshal(X509Certificate value) throws Exception {
        BufferPoolByteArrayOutputStream outBuf ;
        ObjectOutputStream outStream = null;
        
        try{
            outBuf = new BufferPoolByteArrayOutputStream(1024);
            outStream = new ObjectOutputStream(outBuf);
            outStream.writeObject(value);
            byte [] bytes = outBuf.detachPooledByteArray();
            return HexUtils.encodeBase64(bytes);
        }finally{
            ResourceUtils.closeQuietly(outStream);
        }
    }
}
