package com.l7tech.identity.cert;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.Arrays;
import java.util.Date;
import java.math.BigInteger;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.*;
import org.bouncycastle.jce.*;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.pkcs.CertificationRequestInfo;
import java.util.Properties;

/**
 *  Creates X509 certificates using RSA keys.
 */
public class RSASigner {

    private PrivateKey privateKey;
    private X509Certificate rootCert;
    private X509Certificate caCert;
    X509Name caSubjectName;
    private Long validity;
    private boolean usebc, bccritical;
    private boolean useku, kucritical;
    private boolean useski, skicritical;
    private boolean useaki, akicritical;
    private boolean usecrldist, crldistcritical;
    String crldisturi;
    private SecureRandom random;

    public static String DEFAULT_KEYSTORE_NAME = "keystore";
    public static String DEFAULT_PRIVATE_KEY_ALIAS = "MyKey";

    Properties defaultProperties;

    //----------------------------------------

    //Location of CA keystore;
    String keyStorePath;
    //Location of CA keystore;
    String keyStoreType;

    //Password for server keystore, comment out to prompt for pwd.;
    String storePass;

    //Alias in keystore for CA private key;
    String privateKeyAlias;

    //Password for CA private key, only used for JKS-keystore. Leave as null for PKCS12-keystore, comment out to prompt;
    String privateKeyPassString;

    //Validity in days from days date for created certificate;
    //Long validity;

    //Use BasicConstraints?;
    boolean basicConstraints;

    //BasicConstraints critical? (RFC2459 says YES);
    boolean basicConstraintsCritical;

    //Use KeyUsage?;
    boolean keyUsage;

    //KeyUsage critical? (RFC2459 says YES);
    boolean keyUsageCritical;

    //Use SubjectKeyIdentifier?;
    boolean subjectKeyIdentifier;

    //SubjectKeyIdentifier critical? (RFC2459 says NO);
    boolean subjectKeyIdentifierCritical;

    //Use AuthorityKeyIdentifier?;
    boolean authorityKeyIdentifier;

    //AuthorityKeyIdentifier critical? (RFC2459 says NO);
    boolean authorityKeyIdentifierCritical;

    //Use SubjectAlternativeName?;
    boolean subjectAlternativeName;

    //SubjectAlternativeName critical? (RFC2459 says NO);
    boolean subjectAlternativeNameCritical;

    //Use CRLDistributionPoint?;
    boolean cRLDistributionPoint;

    //CRLDistributionPoint critical? (RFC2459 says NO);
    boolean cRLDistributionPointCritical;

    //URI of CRLDistributionPoint?;
    String cRLDistURI;

    //Use old style altName with email in DN? (RFC2459 says NO);
    boolean emailInDN;

    //Use CRLNumber?;
    boolean cRLNumber;

    //CRLNumber critical? (RFC2459 says NO);
    boolean cRLNumberCritical;

    //-------------------------------------------
    /**
     *  Constructor for the RSASigner object sets all fields to the values
     * define in the pass properties.
     *
     */
    public RSASigner(Properties properties) {
        initProperties(properties);
        initClass();
    }

    /**
     *  Constructor for the RSASigner object sets all fields to their most common usage using
     * the passed keystore parameters to retreive the private key,
     */
    public RSASigner(String keyStorePath, String storePass, String privateKeyAlias, String privateKeyPass) {
        initDefaults();
        this.keyStorePath = keyStorePath;
        this.storePass = storePass;
        this.privateKeyAlias = privateKeyAlias;
        this.privateKeyPassString = privateKeyPass;
        initClass();
    }

    /**
     *  Constructor for the RSASigner object sets all fields to their most common usage.    Initialises the
     *  the signer to prompt for passwords via the commandline.   The name of the keystore and private key alias
     * are drawn from the propertie file.
     */
    public RSASigner() {
        initDefaults();
        initClass();
    }

    /**
     *  Implements ISignSession::createCertificate
     *
     *@param  dn             Description of the Parameter
     *@param  pk             Description of the Parameter
     *@return                Description of the Return Value
     *@exception  Exception  Description of the Exception
     */
    public Certificate createCertificate(String dn, PublicKey pk) throws Exception {
        System.out.println(">createCertificate(pk)");
        // Standard key usages for end users are: digitalSignature | keyEncipherment or nonRepudiation
        // Default key usage is digitalSignature | keyEncipherment

        // Create an array for KeyUsage acoording to X509Certificate.getKeyUsage()
        boolean[] keyusage = new boolean[9];
        Arrays.fill(keyusage, false);
        // digitalSignature
        keyusage[0] = true;
        // keyEncipherment
        keyusage[2] = true;
        System.out.println("<createCertificate(pk)");
        return createCertificate(dn, pk, keyusage);
    }
    // createCertificate


    /**
     *  Implements ISignSession::createCertificate
     *
     *@param  dn             Description of the Parameter
     *@param  pk             Description of the Parameter
     *@param  keyusage       Description of the Parameter
     *@return                Description of the Return Value
     *@exception  Exception  Description of the Exception
     */
    public Certificate createCertificate(String dn, PublicKey pk, boolean[] keyusage) throws Exception {
        return createCertificate(dn, pk, sunKeyUsageToBC(keyusage));
    }


    /**
     *  Implements ISignSession::createCertificate
     *
     *@param  dn             Description of the Parameter
     *@param  pk             Description of the Parameter
     *@param  keyusage       Description of the Parameter
     *@return                Description of the Return Value
     *@exception  Exception  Description of the Exception
     */
    public Certificate createCertificate(String dn, PublicKey pk, int keyusage) throws Exception {
        System.out.println(">createCertificate(pk, ku)");
        if (false) {
            // If this is a CA, only allow CA-type keyUsage
            keyusage = X509KeyUsage.keyCertSign + X509KeyUsage.cRLSign;
        }
        System.out.println("dn" + dn);
        System.out.println("caSubjectName" + caSubjectName);
        System.out.println("validity" + validity);
        System.out.println("keyusage" + pk);
        System.out.println("keyusage" + keyusage);

        X509Certificate cert = makeBCCertificate(dn, caSubjectName, validity.longValue(), pk, keyusage);
        // Verify before returning
        cert.verify(caCert.getPublicKey());
        return cert;
    }
    // createCertificate


    /**
     *  Implements ISignSession::createCertificate
     *
     *@param  dn             Description of the Parameter
     *@param  incert         Description of the Parameter
     *@return                Description of the Return Value
     *@exception  Exception  Description of the Exception
     */
    public Certificate createCertificate(String dn, Certificate incert) throws Exception {
        System.out.println(">createCertificate(cert)");
        X509Certificate cert = (X509Certificate) incert;
        try {
            cert.verify(cert.getPublicKey());
        } catch (Exception e) {
            System.out.println("POPO verification failed for " + dn);
            throw new Exception("Verification of signature (popo) on certificate failed.");
        }
        // TODO: extract more extensions than just KeyUsage
        Certificate ret = createCertificate(dn, cert.getPublicKey(), cert.getKeyUsage());
        System.out.println("<createCertificate(cert)");
        return ret;
    }
    // createCertificate


    /**
     *  Implements ISignSession::createCertificate
     *
     *@param  pkcs10req      Description of the Parameter
     *@return                Description of the Return Value
     *@exception  Exception  Description of the Exception
     */
    public Certificate createCertificate(byte[] pkcs10req) throws Exception {
        return createCertificate(pkcs10req, -1);
    }


    /**
     *  Implements ISignSession::createCertificate
     *
     *@param  pkcs10req      Description of the Parameter
     *@param  keyUsage       Description of the Parameter
     *@return                Description of the Return Value
     *@exception  Exception  Description of the Exception
     */
    public Certificate createCertificate(byte[] pkcs10req, int keyUsage) throws Exception {
        System.out.println(">createCertificate(pkcs10)");
        Certificate ret = null;

        DERObject derobj = new DERInputStream(new ByteArrayInputStream(pkcs10req)).readObject();
        System.out.println("DERObject class = " + derobj.getClass().getName());
        // DERConstructedSequence seq = (DERConstructedSequence) derobj;
        // PKCS10CertificationRequest pkcs10 = new PKCS10CertificationRequest(seq);
        // todo, fla try to fix this some error here
        PKCS10CertificationRequest pkcs10 = new PKCS10CertificationRequest(pkcs10req);
        CertificationRequestInfo certReqInfo = pkcs10.getCertificationRequestInfo();
        String dn= certReqInfo.getSubject().toString();
        System.out.println("Signing cert for subject DN="+dn);
        if (pkcs10.verify() == false) {
            System.out.println("POPO verification failed for " + dn);
            throw new Exception("Verification of signature (popo) on PKCS10 request failed.");
        }
        // TODO: extract more information or attributes
        if (keyUsage < 0) {
            ret = createCertificate(dn, pkcs10.getPublicKey());
        } else {
            ret = createCertificate(dn, pkcs10.getPublicKey(), keyUsage);
        }
        System.out.println("<createCertificate(pkcs10)");
        return ret;
    }

    /**
     *  Gets the password from the user.
     *
     *@param  password       Description of the Parameter
     *@return                The password value
     *@exception  Exception  Description of the Exception
     */
    private char[] getPassword(String password, String prompt) throws Exception {
        if (password == null) {
            System.out.println(prompt);
            BufferedReader in
                    = new BufferedReader(new InputStreamReader(System.in));
            return (in.readLine()).toCharArray();
        } else {
            return password.toCharArray();
        }
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
    private X509Certificate makeBCCertificate(String dn, X509Name caname,
                                              long validity, PublicKey publicKey, int keyusage)
            throws Exception {
        //start initialising the cert---------------------
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
            SubjectPublicKeyInfo spki = new SubjectPublicKeyInfo((DERConstructedSequence) new DERInputStream(
                    new ByteArrayInputStream(publicKey.getEncoded())).readObject());
            SubjectKeyIdentifier ski = new SubjectKeyIdentifier(spki);
            certgen.addExtension(X509Extensions.SubjectKeyIdentifier.getId(), skicritical, ski);
        }
        // Authority key identifier
        if (useaki == true) {
            SubjectPublicKeyInfo apki = new SubjectPublicKeyInfo((DERConstructedSequence) new DERInputStream(
                    new ByteArrayInputStream(caCert.getPublicKey().getEncoded())).readObject());
            AuthorityKeyIdentifier aki = new AuthorityKeyIdentifier(apki);
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
         */
        // CRL Distribution point URI
        if (usecrldist == true) {
            GeneralName gn = new GeneralName(new DERIA5String(crldisturi), 6);
            DERConstructedSequence seq = new DERConstructedSequence();
            seq.addObject(gn);
            GeneralNames gns = new GeneralNames(seq);
            DistributionPointName dpn = new DistributionPointName(0, gns);
            DistributionPoint distp = new DistributionPoint(dpn, null, null);
            DERConstructedSequence ext = new DERConstructedSequence();
            ext.addObject(distp);
            certgen.addExtension(X509Extensions.CRLDistributionPoints.getId(), crldistcritical, ext);
        }

        X509Certificate cert = certgen.generateX509Certificate(privateKey);

        System.out.println("<makeBCCertificate()");
        return (X509Certificate) cert;
    }
    // makeBCCertificate

    private void initClass() {
        try {
            // Install BouncyCastle provider

            Provider BCJce = new org.bouncycastle.jce.provider.BouncyCastleProvider();

            int result = Security.addProvider(BCJce);
            System.out.println("loaded provider at position " + result);

            // Get env variables and read in nessecary data
            //KeyStore keyStore = KeyStore.getInstance("PKCS12", "BC");
            // KeyStore keyStore = KeyStore.getInstance(keyStoreType); fla modif
            KeyStore keyStore = KeyStore.getInstance("JCEKS");

            System.out.println("keystore:" + keyStorePath);
            InputStream is = new FileInputStream(keyStorePath);

            char[] keyStorePass = getPassword(storePass, "Please enter your keystore password: ");
            keyStore.load(is, keyStorePass);
            System.out.println("privateKeyAlias: " + privateKeyAlias);
            char[] privateKeyPass = getPassword(privateKeyPassString, "Please enter your private key password (may be blank depending on your keystore implementation)");
            if ((new String(privateKeyPass)).equals("null")) {
                privateKeyPass = null;
            }
            System.out.println("about to get private key from keystore");
            privateKey = (PrivateKey) keyStore.getKey(privateKeyAlias, privateKeyPass);
            if (privateKey == null) {
                System.out.println("Cannot load key with alias '" + privateKeyAlias + "' from keystore '" + keyStorePath + "'");
                throw new Exception("Cannot load key with alias '" + privateKeyAlias + "' from keystore '" + keyStorePath + "'");
            }
            Certificate[] certchain = keyStore.getCertificateChain(privateKeyAlias); // KeyTools.getCertChain(keyStore, privateKeyAlias);
            if (certchain.length < 1) {
                System.out.println("Cannot load certificate chain with alias '" + privateKeyAlias + "' from keystore '" + keyStorePath + "'");
                throw new Exception("Cannot load certificate chain with alias '" + privateKeyAlias + "' from keystore '" + keyStorePath + "'");
            }
            // We only support a ca hierarchy with depth 2.
            caCert = (X509Certificate) certchain[0];
            System.out.println("cacertIssuer: " + caCert.getIssuerDN().toString());
            System.out.println("cacertSubject: " + caCert.getSubjectDN().toString());

            // We must keep the same order in the DN in the issuer field in created certificates as there
            // is in the subject field of the CA-certificate.
            // MODIFIDED Jul 2, 2003 (KSM) Added reverse=true to constructor because of issues with DN order for caSubject and the
            // interaction with keytool. Keytool expects this to be in CN=, OU=, ...CA= order
            caSubjectName = new X509Name(true, caCert.getSubjectDN().toString());
            System.out.println("caSubjectName: " + caSubjectName.toString());
            //caSubjectName = CertTools.stringToBcX509Name(caCert.getSubjectDN().toString());

            // root cert is last cert in chain
            rootCert = (X509Certificate) certchain[certchain.length - 1];
            System.out.println("rootcertIssuer: " + rootCert.getIssuerDN().toString());
            System.out.println("rootcertSubject: " + rootCert.getSubjectDN().toString());
            // is root cert selfsigned?
            /*if (!CertTools.isSelfSigned(rootCert)) {
                throw new Exception("Root certificate is not self signed!");
            }*/

            // The validity in days is specified in environment

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
            if ((usecrldist = cRLDistributionPoint) == true) {
                crldistcritical = cRLDistributionPointCritical;
                crldisturi = cRLDistURI;
            }

            // Init random number generator for random serialnumbers
            random = SecureRandom.getInstance("SHA1PRNG");
            // Using this seed we should get a different seed every time.
            // We are not concerned about the security of the random bits, only that they are different every time.
            // Extracting 64 bit random numbers out of this should give us 2^32 (4 294 967 296) serialnumbers before
            // collisions (which are seriously BAD), well anyhow sufficien for pretty large scale installations.
            // Design criteria: 1. No counter to keep track on. 2. Multiple thereads can generate numbers at once, in
            // a clustered environment etc.
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initProperties(Properties props) {
        initDefaults();
        //now run through and set all the properties
        keyStorePath = props.getProperty("keyStorePath");
        keyStoreType = props.getProperty("keyStoreType");
        System.out.println(keyStorePath);
        //NO DEFAULT if this is null the user will be prompted for it later
        storePass = props.getProperty("storePass");
        privateKeyAlias = props.getProperty("privateKeyAlias");
        //NO DEFAULT if this is null the user will be prompted later
        privateKeyPassString = props.getProperty("privateKeyPassString");
        validity = new Long(props.getProperty("validity"));
        basicConstraints = new Boolean(props.getProperty("validity")).booleanValue();
        basicConstraintsCritical = new Boolean(props.getProperty("validity")).booleanValue();
        keyUsage = new Boolean(props.getProperty("validity")).booleanValue();
        keyUsageCritical = new Boolean(props.getProperty("validity")).booleanValue();
        // MODIFIED (KSM) Jun 27, 03 - Added retrieval of critical subject and authority key identifier fields. Need to follow
        // certificate chains properly...
        subjectKeyIdentifier = new Boolean(props.getProperty("subjectKeyIdentifier")).booleanValue();
        subjectKeyIdentifierCritical = new Boolean(props.getProperty("subjectKeyIdentifierCritical")).booleanValue();
        authorityKeyIdentifier = new Boolean(props.getProperty("authorityKeyIdentifier")).booleanValue();
        authorityKeyIdentifierCritical = new Boolean(props.getProperty("authorityKeyIdentifierCritical")).booleanValue();
        subjectAlternativeName = new Boolean(props.getProperty("validity")).booleanValue();
        subjectAlternativeNameCritical = new Boolean(props.getProperty("validity")).booleanValue();
        cRLDistributionPoint = new Boolean(props.getProperty("validity")).booleanValue();
        cRLDistributionPointCritical = new Boolean(props.getProperty("validity")).booleanValue();
        String cRLDistURI = props.getProperty("cRLDistURI");
        emailInDN = new Boolean(props.getProperty("validity")).booleanValue();
        cRLNumber = new Boolean(props.getProperty("validity")).booleanValue();
        cRLNumberCritical = new Boolean(props.getProperty("validity")).booleanValue();
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
        validity = new Long(730);
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
        cRLDistributionPoint = false;
        defaultProperties.setProperty("cRLDistributionPoint", Boolean.toString(cRLDistributionPoint));
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
}
