/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.prov.bc;

import com.l7tech.common.security.JceProvider;
import com.l7tech.common.security.RsaSignerEngine;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERInputStream;
import org.bouncycastle.asn1.pkcs.CertificationRequestInfo;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.jce.X509KeyUsage;
import org.bouncycastle.jce.X509V3CertificateGenerator;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author mike
 * @version 1.0
 */
public class BouncyCastleRsaSignerEngine implements RsaSignerEngine {

    static {
        // Make sure our provider is installed.  Probably unnecessary.
        JceProvider.init();
    }

    private PrivateKey privateKey;
    private X509Certificate caCert;
    X509Name caSubjectName;
    private Long validity;
    private boolean usebc, bccritical;
    private boolean useku, kucritical;
    private boolean useski, skicritical;
    private boolean useaki, akicritical;
    private SecureRandom random;

    private Properties defaultProperties;

    //----------------------------------------

    //Location of CA keystore;
    private String keyStorePath;

    //Password for server keystore, comment out to prompt for pwd.;
    private String storePass;

    //Alias in keystore for CA private key;
    private String privateKeyAlias;

    //Password for CA private key, only used for JKS-keystore. Leave as null for PKCS12-keystore, comment out to prompt;
    private String privateKeyPassString;

    //Validity in days from days date for created certificate;
    //Long validity;

    //Use BasicConstraints?;
    private boolean basicConstraints;

    //BasicConstraints critical? (RFC2459 says YES);
    private boolean basicConstraintsCritical;

    //Use KeyUsage?;
    private boolean keyUsage;

    //KeyUsage critical? (RFC2459 says YES);
    private boolean keyUsageCritical;

    //Use SubjectKeyIdentifier?;
    private boolean subjectKeyIdentifier;

    //SubjectKeyIdentifier critical? (RFC2459 says NO);
    private boolean subjectKeyIdentifierCritical;

    //Use AuthorityKeyIdentifier?;
    private boolean authorityKeyIdentifier;

    //AuthorityKeyIdentifier critical? (RFC2459 says NO);
    private boolean authorityKeyIdentifierCritical;

    //Use SubjectAlternativeName?;
    private boolean subjectAlternativeName;

    //SubjectAlternativeName critical? (RFC2459 says NO);
    private boolean subjectAlternativeNameCritical;

    //Use CRLDistributionPoint?;
    //boolean cRLDistributionPoint;

    //CRLDistributionPoint critical? (RFC2459 says NO);
    private boolean cRLDistributionPointCritical;

    //Use old style altName with email in DN? (RFC2459 says NO);
    private boolean emailInDN;

    //Use CRLNumber?;
    private boolean cRLNumber;

    //CRLNumber critical? (RFC2459 says NO);
    private boolean cRLNumberCritical;

    /**
     *  Constructor for the RsaCertificateSigner object sets all fields to their most common usage using
     * the passed keystore parameters to retreive the private key,
     */
    public BouncyCastleRsaSignerEngine(String keyStorePath, String storePass, String privateKeyAlias, String privateKeyPass) {
        initDefaults();
        this.keyStorePath = keyStorePath;
        this.storePass = storePass;
        this.privateKeyAlias = privateKeyAlias;
        this.privateKeyPassString = privateKeyPass;
        try {
            initClass();
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw new IllegalArgumentException(e.getMessage());
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
        PKCS10CertificationRequest pkcs10 = new PKCS10CertificationRequest(pkcs10req);
        CertificationRequestInfo certReqInfo = pkcs10.getCertificationRequestInfo();
        String dn= certReqInfo.getSubject().toString();
        logger.fine("Signing cert for subject DN = " + dn);
        if (pkcs10.verify() == false) {
            logger.severe("POPO verification failed for " + dn);
            throw new Exception("Verification of signature (popo) on PKCS10 request failed.");
        }
        Certificate ret = null;
        // TODO: extract more information or attributes
        // Standard key usages for end users are: digitalSignature | keyEncipherment or nonRepudiation
        // Default key usage is digitalSignature | keyEncipherment

        // Create an array for KeyUsage acoording to X509Certificate.getKeyUsage()
        boolean[] keyusage = new boolean[9];
        Arrays.fill(keyusage, false);
        // digitalSignature
        keyusage[0] = true;
        // keyEncipherment
        keyusage[2] = true;
        int keyusage1 = sunKeyUsageToBC(keyusage);
        if (false) {
            // If this is a CA, only allow CA-type keyUsage
            keyusage1 = X509KeyUsage.keyCertSign + X509KeyUsage.cRLSign;
        }
        X509Certificate cert = makeBCCertificate(dn, caSubjectName, validity.longValue(), pkcs10.getPublicKey(), keyusage1);
        // Verify before returning
        cert.verify(caCert.getPublicKey());
        ret = cert;
        return ret;
    }


    /**
     *  Description of the Method
     *
     *@param  sku  Description of the Parameter
     *@return      Description of the Return Value
     */
    private int sunKeyUsageToBC(boolean[] sku) {

        int bcku = 0;
        if (sku[0] == true) {
            bcku = bcku | X509KeyUsage.digitalSignature;
        }
        if (sku[1] == true) {
            bcku = bcku | X509KeyUsage.nonRepudiation;
        }
        if (sku[2] == true) {
            bcku = bcku | X509KeyUsage.keyEncipherment;
        }
        if (sku[3] == true) {
            bcku = bcku | X509KeyUsage.dataEncipherment;
        }
        if (sku[4] == true) {
            bcku = bcku | X509KeyUsage.keyAgreement;
        }
        if (sku[5] == true) {
            bcku = bcku | X509KeyUsage.keyCertSign;
        }
        if (sku[6] == true) {
            bcku = bcku | X509KeyUsage.cRLSign;
        }
        if (sku[7] == true) {
            bcku = bcku | X509KeyUsage.encipherOnly;
        }
        if (sku[8] == true) {
            bcku = bcku | X509KeyUsage.decipherOnly;
        }
        return bcku;
    }


    /**
     *  Description of the Method
     *
     *@param  dn             Description of the Parameter
     *@param  caname         Description of the Parameter
     *@param  validity       Description of the Parameter
     *@param  publicKey      Description of the Parameter
     *@param  keyusage       Description of the Parameter
     *@return                Description of the Return Value
     *@exception  Exception  Description of the Exception
     */
    private X509Certificate makeBCCertificate(String dn, X509Name caname, long validity, PublicKey publicKey, int keyusage) throws Exception {
        final String sigAlg = "SHA1WithRSA";
        Date firstDate = new Date();
        // Set back startdate ten minutes to avoid some problems with wrongly set clocks.
        firstDate.setTime(firstDate.getTime() - 10 * 60 * 1000);
        Date lastDate = new Date();
        // validity in days = validity*24*60*60*1000 milliseconds
        lastDate.setTime(lastDate.getTime() + (validity * 24 * 60 * 60 * 1000));

        X509V3CertificateGenerator certgen = new X509V3CertificateGenerator();
        // Serialnumber is random bits, where random generator is initialized with Date.getTime() when this
        // bean is created.
        byte[] serno = new byte[8];
        random.nextBytes(serno);
        certgen.setSerialNumber((new BigInteger(serno)).abs());
        certgen.setNotBefore(firstDate);
        certgen.setNotAfter(lastDate);
        certgen.setSignatureAlgorithm(sigAlg);
        // Make DNs
        certgen.setSubjectDN(new X509Name(dn)/*CertTools.stringToBcX509Name(dn)*/);
        certgen.setIssuerDN(caname);
        certgen.setPublicKey(publicKey);
        //end initialising the cert ------------------------

        // Basic constranits, all subcerts are NOT CAs
        if (usebc == true) {
            boolean isCA = false;
            /*
             *  if ( ((subject.getType() & SecConst.USER_CA) == SecConst.USER_CA) || ((subject.getType() & SecConst.USER_ROOTCA) == SecConst.USER_ROOTCA) )
             *  isCA=true;
             */
            BasicConstraints bc = new BasicConstraints(isCA);
            certgen.addExtension(X509Extensions.BasicConstraints.getId(), bccritical, bc);
        }
        // Key usage
        if (useku == true) {
            X509KeyUsage ku = new X509KeyUsage(keyusage);
            certgen.addExtension(X509Extensions.KeyUsage.getId(), kucritical, ku);
        }
        // Subject key identifier
        if (useski == true) {
            // fla, rewrote this line to avoid deprecated
            // SubjectPublicKeyInfo spki = new SubjectPublicKeyInfo((DERConstructedSequence) new DERInputStream(new ByteArrayInputStream(publicKey.getEncoded())).readObject());
            SubjectPublicKeyInfo spki = new SubjectPublicKeyInfo(ASN1Sequence.getInstance(new DERInputStream(new ByteArrayInputStream(publicKey.getEncoded())).readObject()));
            SubjectKeyIdentifier ski = new SubjectKeyIdentifier(spki);
            certgen.addExtension(X509Extensions.SubjectKeyIdentifier.getId(), skicritical, ski);
        }
        // Authority key identifier
        if (useaki == true) {
            // fla, rewrote this line to avoid deprecated
            // SubjectPublicKeyInfo apki = new SubjectPublicKeyInfo((DERConstructedSequence)new DERInputStream(new ByteArrayInputStream(caCert.getClientCertPublicKey().getEncoded())).readObject());
            SubjectPublicKeyInfo apki = new SubjectPublicKeyInfo(ASN1Sequence.getInstance(new DERInputStream(new ByteArrayInputStream(caCert.getPublicKey().getEncoded())).readObject()));
            AuthorityKeyIdentifier aki = new AuthorityKeyIdentifier(apki);
            //AuthorityKeyIdentifier aki = new AuthorityKeyIdentifier();
            certgen.addExtension(X509Extensions.AuthorityKeyIdentifier.getId(), akicritical, aki);
        }

        /*
         *  not sure how to handle alternative names at this stage (justin)
         *  Subject Alternative name
         *  if ((usesan == true) && (subject.getEmail() != null)) {
         *  GeneralName gn = new GeneralName(new DERIA5String(subject.getEmail()),1);
         *  DERConstructedSequence seq = new DERConstructedSequence();
         *  seq.addObject(gn);
         *  GeneralNames san = new GeneralNames(seq);
         *  certgen.addExtension(X509Extensions.SubjectAlternativeName.getId(), sancritical, san);
         *  }
         *  // CRL Distribution point URI
         */
        /*if (usecrldist == true) {
            GeneralName gn = new GeneralName(new DERIA5String(crldisturi), 6);
            DERConstructedSequence seq = new DERConstructedSequence();
            seq.addObject(gn);
            GeneralNames gns = new GeneralNames(seq);
            DistributionPointName dpn = new DistributionPointName(0, gns);
            DistributionPoint distp = new DistributionPoint(dpn, null, null);
            DERConstructedSequence ext = new DERConstructedSequence();
            ext.addObject(distp);
            certgen.addExtension(X509Extensions.CRLDistributionPoints.getId(), crldistcritical, ext);
        }*/
        X509Certificate cert = certgen.generateX509Certificate(privateKey);
        return cert;
    }
    // makeBCCertificate

    private void initClass() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        InputStream is = new FileInputStream(keyStorePath);

        if (storePass == null)
            throw new IllegalArgumentException("A CA keystore passphrase must be provided");
        char[] keyStorePass = storePass.toCharArray();
        keyStore.load(is, keyStorePass);

        if (privateKeyPassString == null)
            throw new IllegalArgumentException("A CA private key passphrase must be provided");
        char[] privateKeyPass = privateKeyPassString.toCharArray();

        privateKey = (PrivateKey) keyStore.getKey(privateKeyAlias, privateKeyPass);
        if (privateKey == null) {
            logger.severe("Cannot load key with alias '" + privateKeyAlias + "' from keystore '" + keyStorePath + "'");
            throw new Exception("Cannot load key with alias '" + privateKeyAlias + "' from keystore '" + keyStorePath + "'");
        }
        Certificate[] certchain = keyStore.getCertificateChain(privateKeyAlias); // KeyTools.getCertChain(keyStore, privateKeyAlias);
        if (certchain.length < 1) {
            logger.severe("Cannot load certificate chain with alias '" + privateKeyAlias + "' from keystore '" + keyStorePath + "'");
            throw new Exception("Cannot load certificate chain with alias '" + privateKeyAlias + "' from keystore '" + keyStorePath + "'");
        }
        // We only support a ca hierarchy with depth 2.
        caCert = (X509Certificate) certchain[0];

        // We must keep the same order in the DN in the issuer field in created certificates as there
        // is in the subject field of the CA-certificate.
        // mikel: turned off reverse, since the CSR generated by the client proxy has the correct DN order (cn=..., ou=...)
        caSubjectName = new X509Name(false, caCert.getSubjectDN().toString());
        //caSubjectName = CertTools.stringToBcX509Name(caCert.getSubjectDN().toString());

        // Should extensions be used? Critical or not?
        if ((usebc = basicConstraints) == true) {
            bccritical = basicConstraintsCritical;
        }
        if ((useku = keyUsage) == true) {
            kucritical = keyUsageCritical;
        }
        if ((useski = subjectKeyIdentifier) == true) {
            ;
        }
        skicritical = subjectKeyIdentifierCritical;
        if ((useaki = authorityKeyIdentifier) == true) {
            ;
        }
        akicritical = authorityKeyIdentifierCritical;
        /*if ((usecrldist = cRLDistributionPoint) == true) {
            crldistcritical = cRLDistributionPointCritical;
            crldisturi = cRLDistURI;
        }*/

        // Init random number generator for random serialnumbers
        random = new SecureRandom();
        // Using this seed we should get a different seed every time.
        // We are not concerned about the security of the random bits, only that they are different every time.
        // Extracting 64 bit random numbers out of this should give us 2^32 (4 294 967 296) serialnumbers before
        // collisions (which are seriously BAD), well anyhow sufficien for pretty large scale installations.
        // Design criteria: 1. No counter to keep track on. 2. Multiple thereads can generate numbers at once, in
        // a clustered environment etc.

        // this random number is just for the serial number
        long seed = (new Date().getTime()) + this.hashCode();
        random.setSeed(seed);

        /*
         *  Another possibility is to use SecureRandom's default seeding which is designed to be secure:
         *  <p>The seed is produced by counting the number of times the VM
         *  manages to loop in a given period. This number roughly
         *  reflects the machine load at that point in time.
         *  The samples are translated using a permutation (s-box)
         *  and then XORed together. This process is non linear and
         *  should prevent the samples from "averaging out". The s-box
         *  was designed to have even statistical distribution; it's specific
         *  values are not crucial for the security of the seed.
         *  We also create a number of sleeper threads which add entropy
         *  to the system by keeping the scheduler busy.
         *  Twenty such samples should give us roughly 160 bits of randomness.
         *  <P> These values are gathered in the background by a daemon thread
         *  thus allowing the system to continue performing it's different
         *  activites, which in turn add entropy to the random seed.
         *  <p> The class also gathers miscellaneous system information, some
         *  machine dependent, some not. This information is then hashed together
         *  with the 20 seed bytes.
         */
    }

    /**
     * Sets all the class variables to default values and assign default properties.
     */
    private void initDefaults() {
        defaultProperties = new Properties();
        //the passwords are not assigned here by default they stay null

        //file path to the keystore
        keyStorePath = DEFAULT_KEYSTORE_NAME;
        defaultProperties.setProperty("keyStorePath", keyStorePath);
        //the alias given to the private key in the keystore
        privateKeyAlias = DEFAULT_PRIVATE_KEY_ALIAS;
        defaultProperties.setProperty("privateKeyAlias", privateKeyAlias);
        //Validity in days from days date for created certificate;
        validity = new Long(CERT_DAYS_VALID);
        defaultProperties.setProperty("validity", validity.toString());
        //Use BasicConstraints?;
        basicConstraints = true;
        defaultProperties.setProperty("basicConstraints", Boolean.toString(basicConstraints));
        //BasicConstraints critical? (RFC2459 says YES);
        basicConstraintsCritical = true;
        defaultProperties.setProperty("basicConstraintsCritical", Boolean.toString(basicConstraintsCritical));
        //Use KeyUsage?;
        keyUsage = true;
        defaultProperties.setProperty("keyUsage", Boolean.toString(keyUsage));
        //KeyUsage critical? (RFC2459 says YES);
        keyUsageCritical = true;
        defaultProperties.setProperty("keyUsageCritical", Boolean.toString(keyUsageCritical));
        //Use SubjectKeyIdentifier?;
        subjectKeyIdentifier = true;
        defaultProperties.setProperty("subjectKeyIdentifier", Boolean.toString(subjectKeyIdentifier));
        //SubjectKeyIdentifier critical? (RFC2459 says NO);
        subjectKeyIdentifierCritical = false;
        defaultProperties.setProperty("subjectKeyIdentifierCritical", Boolean.toString(subjectKeyIdentifierCritical));
        //Use AuthorityKeyIdentifier?;
        authorityKeyIdentifier = true;
        defaultProperties.setProperty("authorityKeyIdentifier", Boolean.toString(authorityKeyIdentifier));
        //AuthorityKeyIdentifier critical? (RFC2459 says NO);
        authorityKeyIdentifierCritical = false;
        defaultProperties.setProperty("authorityKeyIdentifierCritical", Boolean.toString(authorityKeyIdentifierCritical));
        //Use SubjectAlternativeName?;
        subjectAlternativeName = true;
        defaultProperties.setProperty("subjectAlternativeName", Boolean.toString(subjectAlternativeName));
        //SubjectAlternativeName critical? (RFC2459 says NO);
        subjectAlternativeNameCritical = false;
        defaultProperties.setProperty("subjectAlternativeNameCritical", Boolean.toString(subjectAlternativeNameCritical));
        //Use CRLDistributionPoint?;
        //cRLDistributionPoint = false;
        //defaultProperties.setProperty("cRLDistributionPoint", Boolean.toString(cRLDistributionPoint));
        //CRLDistributionPoint critical? (RFC2459 says NO);
        cRLDistributionPointCritical = false;
        defaultProperties.setProperty("cRLDistributionPointCritical", Boolean.toString(cRLDistributionPointCritical));
        //URI of CRLDistributionPoint?;
        String cRLDistURI = "http://127.0.0.1:8080/webdist/certdist?cmd=crl";
        defaultProperties.setProperty("cRLDistURI", cRLDistURI);
        //Use old style altName with email in DN? (RFC2459 says NO);
        emailInDN = false;
        defaultProperties.setProperty("emailInDN", Boolean.toString(emailInDN));
        //Use CRLNumber?;
        cRLNumber = true;
        defaultProperties.setProperty("cRLNumber", Boolean.toString(cRLNumber));
        //CRLNumber critical? (RFC2459 says NO);
        cRLNumberCritical = false;
        defaultProperties.setProperty("cRLNumberCritical", Boolean.toString(cRLNumberCritical));
    }

    private final Logger logger = Logger.getLogger(getClass().getName());
}
