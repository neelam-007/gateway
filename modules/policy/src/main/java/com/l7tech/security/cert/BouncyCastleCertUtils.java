package com.l7tech.security.cert;

import com.l7tech.common.io.CertGenParams;
import com.l7tech.common.io.CertificateGeneratorException;
import com.l7tech.common.io.ParamsCertificateGenerator;
import com.l7tech.common.io.X509GeneralName;
import com.l7tech.security.prov.CertificateRequest;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.security.prov.bc.BouncyCastleCertificateRequest;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ExceptionUtils;
import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.pkcs.*;
import org.bouncycastle.asn1.pkcs.Attribute;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.jetbrains.annotations.NotNull;

import javax.security.auth.x500.X500Principal;
import java.io.IOException;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Certificate utility methods that require static imports of Bouncy Castle classes.
 */
public class BouncyCastleCertUtils  {

    // true to set attrs to null (pre-6.0-2 behavior); false to set attrs to empty DERSet (Bug #10534)
    private static final boolean omitAttrs = ConfigFactory.getBooleanProperty( "com.l7tech.security.cert.csr.omitAttrs", false );
    private static final Logger LOGGER = Logger.getLogger(BouncyCastleCertUtils.class.getName());


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
     * @throws SignatureException        if there is a problem signing the certificate request.
     */
    public static CertificateRequest makeCertificateRequest(CertGenParams certGenParams, KeyPair keyPair) throws SignatureException {

        if (certGenParams.getSubjectDn() == null)
            throw new IllegalArgumentException("certGenParams must include a subject DN for the CSR");

        final Provider sigProvider = JceProvider.getInstance().getProviderFor(JceProvider.SERVICE_CSR_SIGNING);
        final X500Principal subject = certGenParams.getSubjectDn();

        //get extensions i.e. SAN (Subject Alternative Names)
        final Extensions extensions = getSubjectAlternativeNamesExtensions(certGenParams);

        String sigAlg = certGenParams.getSignatureAlgorithm();
        if (sigAlg == null)
            sigAlg = ParamsCertificateGenerator.getSigAlg(keyPair.getPublic(), certGenParams.getHashAlgorithm(), sigProvider);

        // Generate request
        final X500Name x500Name = new X500Name(subject.getName(X500Principal.RFC2253));
        final PKCS10CertificationRequestBuilder requestBuilder = new JcaPKCS10CertificationRequestBuilder(
                x500Name, keyPair.getPublic());
        if (!omitAttrs && extensions != null){
            requestBuilder.addAttribute(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest, extensions);
        }

        try {
            final ContentSigner signer = getContentSigner(sigAlg, keyPair, sigProvider);
            return new BouncyCastleCertificateRequest(requestBuilder.build(signer), keyPair.getPublic());

        } catch (OperatorCreationException e) {
            throw new SignatureException("The content signer failed to build." + ExceptionUtils.getMessage(e));
        }
    }

    /**
     * Get Subject Alternative Names extensions
     * @param certGenParams Certificate General Parameters
     * @return X509Extensions object or null if no extensions found
     */
    protected static Extensions getSubjectAlternativeNamesExtensions(final CertGenParams certGenParams) {
        List<X509GeneralName> sans = certGenParams.getSubjectAlternativeNames();

        // check if we have any SANs in the request
        if (sans == null || sans.isEmpty()) {
            return null;
        }

        final List<GeneralName> generalNameList = new ArrayList<>();
        for (final X509GeneralName san : sans) {
            generalNameList.add(new GeneralName(san.getType().getTag(), san.getStringVal()));
        }

        final GeneralNames subjectAltNames = GeneralNames.getInstance(new DERSequence(generalNameList.toArray(new GeneralName[] {})));
        ExtensionsGenerator extGen = new ExtensionsGenerator();
        try {
            extGen.addExtension(Extension.subjectAlternativeName, false, subjectAltNames);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING,
                    "Error processing object: " + ExceptionUtils.getMessage(e),
                    ExceptionUtils.getDebugException(e));
            return null;
        }
        return extGen.generate();
    }

    /**
     * Generate a CertificateRequest using the specified Crypto provider.
     *
     * @param username  the username to put in the cert
     * @param keyPair the public and private keys
     * @param provider provider to use for crypto operations, or null to use best preferences.
     * @return a new CertificateRequest instance.  Never null.
     * @throws SignatureException        if there is a problem signing the certificate request.
     */
    public static CertificateRequest makeCertificateRequest(String username, KeyPair keyPair, Provider provider) throws SignatureException {

        X500Name subject = new X500Name("cn=" + username);

        // Generate request
        try {
            final PKCS10CertificationRequestBuilder requestBuilder = new JcaPKCS10CertificationRequestBuilder(subject, keyPair.getPublic());
            final ContentSigner signer = getContentSigner(JceProvider.DEFAULT_CSR_SIG_ALG, keyPair, provider);
            return new BouncyCastleCertificateRequest(requestBuilder.build(signer), keyPair.getPublic());

        } catch (OperatorCreationException e) {
            throw new SignatureException("The content signer failed to build." + ExceptionUtils.getMessage(e));
        }
    }

    /**
     *
     * @param sigAlg The signature algorithm name
     * @param keyPair The key pair
     * @param provider The provider used, ie: Bouncy Castle, CCJ. Leave as null for default.
     * @return The JcaContentSigner
     * @throws OperatorCreationException If there is an error while creating the content signer
     */
    private static ContentSigner getContentSigner(String sigAlg, KeyPair keyPair, Provider provider) throws OperatorCreationException {
        return provider == null ?
                new JcaContentSignerBuilder(sigAlg).build(keyPair.getPrivate())
                : new JcaContentSignerBuilder(sigAlg).setProvider(provider).build(keyPair.getPrivate());
    }


    /**
     * Gets the Subject Alternative Names from the CSR (Certificate Signing Request).
     *
     * @param pkcs10 The CSR.
     * @return the list of X509GeneralName
     */
    public static List<X509GeneralName> extractSubjectAlternativeNamesFromCertRequest(PKCS10CertificationRequest pkcs10) throws IOException {

        List<X509GeneralName> x509GeneralNames = new ArrayList<>();

        final Attribute[] attributes = pkcs10.getAttributes(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest);
        for (Attribute attribute : attributes) {
            ASN1Set attValue = attribute.getAttrValues();
            if (attValue != null) {
                ASN1Encodable extension = attValue.getObjectAt(0);
                if ((extension instanceof Extensions) || (extension instanceof DERSequence)) {
                    Extensions extensions = Extensions.getInstance(extension);
                    GeneralNames subjectAltName = GeneralNames.fromExtensions(extensions, Extension.subjectAlternativeName);
                    if (subjectAltName != null) {
                        addX509GeneralNameToList(subjectAltName, x509GeneralNames);
                    }
                }
            }
        }
        return Collections.unmodifiableList(x509GeneralNames);
    }

    /**
     * Extracts the Bouncy Castle GeneralNames and adds it to the X509GeneralName (layer7 class) list.
     *
     * @param genNames            The Bouncy Castle GeneralNames to be converted.
     * @param x509GeneralNameList The converted GeneralGames will be appended to the x509GeneralNameList.
     */
    private static void addX509GeneralNameToList(@NotNull GeneralNames genNames, List<X509GeneralName> x509GeneralNameList) throws IOException {

        final GeneralName[] generalNames = genNames.getNames();

        // Note for some of the case statements, it will fallthrough on purpose.
        for (GeneralName generalName : generalNames) {
            X509GeneralName.Type type = X509GeneralName.getTypeFromTag(generalName.getTagNo());
            switch (generalName.getTagNo()) {
                case GeneralName.ediPartyName:
                case GeneralName.x400Address:
                case GeneralName.otherName:
                    x509GeneralNameList.add(new X509GeneralName(type, generalName.getEncoded(ASN1Encoding.DER)));
                    break;
                case GeneralName.directoryName:
                    x509GeneralNameList.add(new X509GeneralName(type, X500Name.getInstance(generalName.getName()).toString()));
                    break;
                case GeneralName.dNSName:
                case GeneralName.rfc822Name:
                case GeneralName.uniformResourceIdentifier:
                    x509GeneralNameList.add((new X509GeneralName(type, ((ASN1String) generalName.getName()).getString())));
                    break;
                case GeneralName.registeredID:
                    x509GeneralNameList.add(new X509GeneralName(type, ASN1ObjectIdentifier.getInstance(generalName.getName()).getId()));
                    break;
                case GeneralName.iPAddress:
                    x509GeneralNameList.add(new X509GeneralName(type, DEROctetString.getInstance(generalName.getName()).getEncoded(ASN1Encoding.DER)));
                    break;
                default:
                    throw new IOException("Bad tag number: " + generalName.getTagNo());
            }
        }
    }
}
