package com.l7tech.security.cert;

import com.l7tech.common.io.CertGenParams;
import com.l7tech.common.io.CertificateGeneratorException;
import com.l7tech.common.io.ParamsCertificateGenerator;
import com.l7tech.common.io.X509GeneralName;
import com.l7tech.security.prov.CertificateRequest;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.security.prov.bc.BouncyCastleCertificateRequest;
import com.l7tech.util.ConfigFactory;

import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.x509.extension.X509ExtensionUtil;


import javax.security.auth.x500.X500Principal;
import java.io.IOException;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * Certificate utility methods that require static imports of Bouncy Castle classes.
 */
public class BouncyCastleCertUtils  {

    // true to set attrs to null (pre-6.0-2 behavior); false to set attrs to empty DERSet (Bug #10534)
    private static final boolean omitAttrs = ConfigFactory.getBooleanProperty( "com.l7tech.security.cert.csr.omitAttrs", false );

    /**
     * Generate a self-signed certificate from the specified KeyPair and the specified cert generation parameters.
     *
     * @param certGenParams configuration of the cert to generate.  Required.  Must have a non-null subjectDn.
     * @param keyPair       an RSA key pair to use for the certificate.  Required.
     * @return the new self-signed certificate.  Never null.
     * @throws CertificateGeneratorException  if there is a problem producing the new cert
     */
    public static X509Certificate generateSelfSignedCertificate(CertGenParams certGenParams, KeyPair keyPair) throws CertificateGeneratorException {
        return new ParamsCertificateGenerator(certGenParams).generateCertificate(keyPair.getPublic(), keyPair.getPrivate(), null);
    }

    /**
     * Create a PKCS#10 certification request using the specified DN and key pair.
     *
     * @param certGenParams parameters describing the CSR to create.  Required.
     *                      Must contain a non-null subjectDn.
     *                      May contain a non-null signatureAlgorithm; if not, a default will be picked.
     * @param keyPair  a key pair to use for the CSR.  Required.
     * @return a new PKCS#10 certification request including the specified DN and public key, signed with the
     *         specified private key.  Never null.
     * @throws SignatureException        if there is a problem signing the cert
     * @throws InvalidKeyException       if there is a problem with the provided key pair
     * @throws NoSuchProviderException   if the current asymmetric JCE provider is incorrect
     * @throws NoSuchAlgorithmException  if a required algorithm is not available in the current asymmetric JCE provider
     */
    public static CertificateRequest makeCertificateRequest(CertGenParams certGenParams, KeyPair keyPair) throws SignatureException, InvalidKeyException, NoSuchProviderException, NoSuchAlgorithmException {
        if (certGenParams.getSubjectDn() == null)
            throw new IllegalArgumentException("certGenParams must include a subject DN for the CSR");
        Provider sigProvider = JceProvider.getInstance().getProviderFor(JceProvider.SERVICE_CSR_SIGNING);
        X500Principal subject = certGenParams.getSubjectDn();
        DERSet attrSet;
        //get extensions i.e. SAN (Subject Alternative Names)
        X509Extensions extensions = getSubjectAlternativeNamesExtensions(certGenParams);
        if(extensions != null ) {
            Attribute attribute = new Attribute(
                    PKCSObjectIdentifiers.pkcs_9_at_extensionRequest,
                    new DERSet(extensions));
            attrSet = new DERSet(attribute);
        }
        else {
            attrSet = new DERSet(new ASN1EncodableVector());
        }

        ASN1Set attrs = omitAttrs ? null : attrSet;
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();

        String sigAlg = certGenParams.getSignatureAlgorithm();
        if (sigAlg == null)
            sigAlg = ParamsCertificateGenerator.getSigAlg(publicKey, certGenParams.getHashAlgorithm(), sigProvider);

        // Generate request
        final PKCS10CertificationRequest certReq = sigProvider == null
                ? new PKCS10CertificationRequest(sigAlg, subject, publicKey, attrs, privateKey, null)
                : new PKCS10CertificationRequest(sigAlg, subject, publicKey, attrs, privateKey, sigProvider.getName());
        return new BouncyCastleCertificateRequest(certReq, publicKey);
    }

    /**
     * Get Subject Alternative Names and other extensions possible extensions
     * @param certGenParams Certificate General Parameters
     * @return X509Extensions object or null if no extensions found
     */
    private static X509Extensions getSubjectAlternativeNamesExtensions(CertGenParams certGenParams) {
        List<X509GeneralName> sans = certGenParams.getSubjectAlternativeNames();
        // check if we have any SANs in the request
        if(sans != null && sans.size() > 0) {
            List<ASN1Encodable> asn1EncodableList = new ArrayList<>();
            for (X509GeneralName san : sans) {
                asn1EncodableList.add(new GeneralName(san.getType().getTag(), san.getStringVal()));
            }

            ASN1Encodable[] asn1Encodables = asn1EncodableList.toArray(new ASN1Encodable[0]);
            ASN1Sequence sequence = new DERSequence(asn1Encodables);
            GeneralNames subjectAltName = new GeneralNames(sequence);
            // create the extensions object and add it as an attribute
            Vector oids = new Vector();
            Vector values = new Vector();

            oids.add(X509Extensions.SubjectAlternativeName);
            values.add(new X509Extension(false, new DEROctetString(subjectAltName)));

            return new X509Extensions(oids, values);
        }

        return null;
    }

    /**
     * Generate a CertificateRequest using the specified Crypto provider.
     *
     * @param username  the username to put in the cert
     * @param keyPair the public and private keys
     * @param provider provider to use for crypto operations, or null to use best preferences.
     * @return a new CertificateRequest instance.  Never null.
     * @throws java.security.InvalidKeyException  if a CSR cannot be created using the specified keypair
     * @throws java.security.SignatureException   if the CSR cannot be signed
     */
    public static CertificateRequest makeCertificateRequest(String username, KeyPair keyPair, Provider provider) throws InvalidKeyException, SignatureException {
        X509Name subject = new X509Name("cn=" + username);
        ASN1Set attrs = omitAttrs ? null : new DERSet(new ASN1EncodableVector());
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();

        // Generate request
        try {
            PKCS10CertificationRequest certReq = new PKCS10CertificationRequest(JceProvider.DEFAULT_CSR_SIG_ALG, subject, publicKey, attrs, privateKey, provider == null ? null : provider.getName());
            return new BouncyCastleCertificateRequest(certReq, publicKey);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (NoSuchProviderException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * extracts SANs from CSR Info attributes
     * @param attr ASN1Set attributes
     * @return returns a list of
     * @throws Exception thrown if objects cannot be extracted from extension value
     */
    public static List<X509GeneralName> extractSubjectAlternativeNamesFromCsrInfoAttr(ASN1Set attr) throws Exception
    {
        if (attr == null || attr.size() < 1)
        {
            return Collections.EMPTY_LIST;
        }

        DERSequence encodable = (DERSequence) attr.getObjectAt(0);
        if(encodable == null) return Collections.emptyList();

        DERSet set = (DERSet) encodable.getObjectAt(1);
        if(set == null) return Collections.EMPTY_LIST;

        DERSequence seq1 = (DERSequence) set.getObjectAt(0);
        if(seq1 == null) return Collections.EMPTY_LIST;

        DERSequence seq2 = (DERSequence) seq1.getObjectAt(0);
        if(seq2 == null) return Collections.EMPTY_LIST;

        DEROctetString octetStr = (DEROctetString) seq2.getObjectAt(1);
        if(octetStr == null) return Collections.EMPTY_LIST;

        byte[] extVal = octetStr.getEncoded();
        if(extVal == null) return Collections.EMPTY_LIST;

        List<X509GeneralName> temp = new ArrayList<>();
        Enumeration it = DERSequence.getInstance(X509ExtensionUtil.fromExtensionValue(extVal)).getObjects();
        while (it.hasMoreElements())
        {
            GeneralName genName = GeneralName.getInstance(it.nextElement());
            X509GeneralName.Type type = X509GeneralName.getTypeFromTag(genName.getTagNo());
            switch (genName.getTagNo())
            {
                case GeneralName.ediPartyName:
                case GeneralName.x400Address:
                case GeneralName.otherName:
                    temp.add(new X509GeneralName(type, genName.getDEREncoded()));
                    break;
                case GeneralName.directoryName:
                    temp.add(new X509GeneralName(type, X500Name.getInstance(genName.getName()).toString()));
                    break;
                case GeneralName.dNSName:
                case GeneralName.rfc822Name:
                case GeneralName.uniformResourceIdentifier:
                    temp.add((new X509GeneralName(type, ((ASN1String)genName.getName()).getString())));
                    break;
                case GeneralName.registeredID:
                    temp.add(new X509GeneralName(type, ASN1ObjectIdentifier.getInstance(genName.getName()).getId()));
                    break;
                case GeneralName.iPAddress:
                    temp.add(new X509GeneralName(type, DEROctetString.getInstance(genName.getName()).getDEREncoded()));
                    break;
                default:
                    throw new IOException("Bad tag number: " + genName.getTagNo());
            }
        }
        return Collections.unmodifiableList(temp);

    }
}
