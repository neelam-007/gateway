package com.l7tech.identity.cert;

import com.l7tech.common.security.JceProvider;
import com.l7tech.common.security.RsaSignerEngine;
import com.l7tech.common.util.HexUtils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.cert.Certificate;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jul 28, 2003
 *
 * Creates X509 certificates using RSA keys.
 * recycled code from smorrison who originally stole it from Justin Wood & Tomas Gustavson
 * stripped out the se.anatom.ejbca dependencies and removed deprecated calls
 */
public class RSASigner {
    private RsaSignerEngine rsaSignerEngine;

    /**
     * Constructor for the RSASigner object sets all fields to their most common usage using
     * the passed keystore parameters to retreive the private key,
     */
    public RSASigner(String keyStorePath, String storePass, String privateKeyAlias, String privateKeyPass) {
        rsaSignerEngine = JceProvider.createRsaSignerEngine(keyStorePath, storePass, privateKeyAlias, privateKeyPass);
    }

    /**
     * handles csr contained in a file. can be called from console when signing the ssl public key for ssg if the
     * root kstore is present.
     * Usage : java RSASigner rootkstorePath rootkstorepass rootkeyAlias rootprivateKeyPass csrfilepath outputcertpath
     */
    public static void main(String[] args) throws Exception {
        if (args == null || args.length < 6) {
            System.out.println("USAGE:");
            System.out.println("java RSASigner rootkstorePath rootkstorepass rootkeyAlias rootprivateKeyPass csrfilepath outputcertpath");
            return;
        }
        // read the csr from the file
        byte[] csrfromfile = null;
        byte[] b64Encoded = HexUtils.slurpStream(new FileInputStream(args[4]), 16384);
        String tmpStr = new String(b64Encoded);
        String beginKey = "-----BEGIN NEW CERTIFICATE REQUEST-----";
        String endKey = "-----END NEW CERTIFICATE REQUEST-----";

        int beggining = tmpStr.indexOf(beginKey) + beginKey.length();
        int end = tmpStr.indexOf(endKey);
        String b64str = tmpStr.substring(beggining, end);
        sun.misc.BASE64Decoder base64decoder = new sun.misc.BASE64Decoder();
        csrfromfile = base64decoder.decodeBuffer(b64str);

        // instantiate the signer
        RSASigner me = new RSASigner(args[0], args[1], args[2], args[3]);
        Certificate cert = me.createCertificate(csrfromfile);

        // serialize the cert to the path provided
        byte[] certbytes = cert.getEncoded();
        FileOutputStream output = null;
        try {
            output = new FileOutputStream(args[5]);
            output.write(certbytes);
        } finally {
            if ( output != null ) output.close();
        }
    }

    /**
     * Create a certificate from the given PKCS10 Certificate Request.
     *
     * @param pkcs10req  the PKCS10 certificate signing request, expressed in binary form.
     * @return a signed X509 client certificate
     * @throws Exception if something bad happens
     */
    public Certificate createCertificate(byte[] pkcs10req) throws Exception {
        return rsaSignerEngine.createCertificate(pkcs10req);
    }
}
