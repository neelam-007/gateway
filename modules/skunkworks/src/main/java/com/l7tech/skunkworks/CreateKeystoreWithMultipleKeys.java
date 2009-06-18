package com.l7tech.skunkworks;

import com.l7tech.security.prov.JceProvider;
import com.l7tech.security.prov.RsaSignerEngine;
import com.l7tech.security.prov.bc.BouncyCastleRsaSignerEngine;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Creates two Keystores. One containin the CA cert and the other containing multiple "client" keys
 * (signed by the CA).
 * The latter's size is configurable and will be generated using some hardcoded values.
 * @author megery
 *
 */
public class CreateKeystoreWithMultipleKeys {

    //file name for CA Keystore
    private static final String CA_KS_NAME = "caks.p12";

    //file name for client keystore
    private static final String KEYS_KS_NAME="keys.p12";

    //the default number of keys to put in the client keystore
    private static final int DEFAULT_NUM_KEYS = 2;

    //same password for both the CA and client keystore for simplicity
    private final String capassword = "password";
    private final String keyspassword = "password";

    //populated by command line args
    private String outputDir;
    private int numKeys;
    private String kstype;
    private String hostname;


    private PrivateKey caPrivateKey;
    private X509Certificate caCert;


    public CreateKeystoreWithMultipleKeys(String[] args) {
        if (args.length < 3) {
            System.out.println(usage());
            throw new IllegalArgumentException();
        }

        hostname = args[0];
        kstype = args[1];
        outputDir = args[2];

        if (args.length > 3) {
            numKeys = Integer.parseInt(args[3]);
        }
        else {
            numKeys = DEFAULT_NUM_KEYS;
        }
    }

    private String usage() {
        StringBuffer sb = new StringBuffer();
        sb.append("usage: java ").append(this.getClass().getName()).append(" <hostname> <keystoretype> <outputdirectory> [<numberofkeys>]\n");
        sb.append("where: \n");
        sb.append("\thostname = name of host to be used in the certificates.\n");
        sb.append("\tkeystoretype = the type of keystore to create (eg. BCPKCS12).\n");
        sb.append("\toutputdirectory = the location where the generated keystores will go.\n");
        sb.append("\tnumberofkeys = how many keys to generate and put in the cloent keystore.\n");
        return sb.toString();
    }


    public static void main(String[] args) {
        CreateKeystoreWithMultipleKeys theClass = new CreateKeystoreWithMultipleKeys(args);
        try {
            theClass.doIt();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doIt() throws KeyStoreException, NoSuchAlgorithmException, IOException, CertificateException, SignatureException, InvalidKeyException {
        createCAKS();
        createOtherKS();
    }

    private void createOtherKS() throws KeyStoreException, NoSuchAlgorithmException, IOException, CertificateException, SignatureException, InvalidKeyException {
        File keysFile = new File(outputDir, KEYS_KS_NAME);
        KeyStore ks = KeyStore.getInstance(kstype);
        ks.load(null, null);

        for (int i = 0; i < numKeys; ++i) {
            KeyPair kp = JceProvider.getInstance().generateRsaKeyPair();

            String cnPrefix = "cn=" + String.valueOf(i) + ".";
            X509Certificate sslCert = new BouncyCastleRsaSignerEngine(caPrivateKey, caCert, null).makeSignedCertificate( cnPrefix + hostname,
                                                                   365, kp.getPublic(), RsaSignerEngine.CertType.SSL );
            String alias = "clientalias-" + String.valueOf(i);
            ks.setKeyEntry(alias, kp.getPrivate(), keyspassword.toCharArray(),
                               new X509Certificate[] { sslCert, caCert } );
        }
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(keysFile);
            ks.store(fos, keyspassword.toCharArray());
        } finally {
            if ( fos != null ) fos.close();
        }
    }

    private void createCAKS() throws KeyStoreException, NoSuchAlgorithmException, IOException, CertificateException, SignatureException, InvalidKeyException {

        File caksFile = new File(outputDir, CA_KS_NAME);
        KeyStore caks = KeyStore.getInstance(kstype);
        caks.load(null,null);
        KeyPair cakp = JceProvider.getInstance().generateRsaKeyPair();
        caPrivateKey = cakp.getPrivate();
        caCert = BouncyCastleRsaSignerEngine.makeSelfSignedRootCertificate("cn=root." + hostname, 365, cakp);
        caks.setKeyEntry("ssgroot", caPrivateKey, capassword.toCharArray(), new X509Certificate[] { caCert } );
        FileOutputStream fos = null;

        try {
            fos = new FileOutputStream(caksFile);
            caks.store(fos, capassword.toCharArray());
        } finally {
            if ( fos != null ) fos.close();
        }
    }
}
