package com.l7tech.server.security;

import com.ibm.xml.dsig.IDResolver;
import com.ibm.xml.dsig.KeyInfo;
import com.ibm.xml.dsig.SignatureContext;
import com.ibm.xml.dsig.Validity;
import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.security.cert.KeyUsageActivity;
import com.l7tech.security.cert.KeyUsageChecker;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.xml.DsigUtil;
import com.l7tech.security.xml.KeyInfoElement;
import com.l7tech.security.xml.SecurityTokenResolver;
import com.l7tech.security.xml.XmlElementVerifierConfig;
import com.l7tech.security.xml.processor.WssProcessorAlgorithmFactory;
import com.l7tech.server.identity.cert.TrustedCertCache;
import com.l7tech.util.*;
import com.l7tech.xml.InvalidDocumentSignatureException;
import com.l7tech.xml.soap.SoapUtil;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jaxen.JaxenException;
import org.jaxen.dom.DOMXPath;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.security.auth.x500.X500Principal;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Utility class that can verify a signed XML element, using an XmlElementVerifierConfig.
 */
public class XmlElementVerifier {
    static final String PROP_CERT_PARSE_BC_FALLBACK = "com.l7tech.external.assertions.xmlsec.certParseBcFallback";
    public static boolean CERT_PARSE_BC_FALLBACK = ConfigFactory.getBooleanProperty(PROP_CERT_PARSE_BC_FALLBACK, false);

    private final XmlElementVerifierConfig config;
    private final SecurityTokenResolver securityTokenResolver;
    private final TrustedCertCache trustedCertCache;
    private final Audit audit;
    private final Logger logger;

    private final X509Certificate preconfiguredCert;

    public XmlElementVerifier(XmlElementVerifierConfig config, SecurityTokenResolver securityTokenResolver, TrustedCertCache trustedCertCache, Audit audit, Logger logger)
            throws CertificateException
    {
        this.config = config;
        this.securityTokenResolver = securityTokenResolver;
        this.trustedCertCache = trustedCertCache;
        this.audit = audit;
        this.logger = logger;
        this.preconfiguredCert = getSelectedCertificate();
    }

    private static final DOMXPath extractSignatureMethodUri;
    static {
        try {
            extractSignatureMethodUri = new DOMXPath("string(ds:SignedInfo/ds:SignatureMethod/@Algorithm)");
            extractSignatureMethodUri.addNamespace("ds", SoapUtil.DIGSIG_URI);
        } catch (JaxenException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Verify a ds:Signature using the current config and return a table summarizing the results.
     *
     * @param sigElement the ds:Signature element to verify.  Required.
     * @param elementsById a lookup by their relevant ID attribute value of all possible Elements that might be referenced from this Signature,
     *                     such as the Map returned by {@link com.l7tech.util.DomUtils#getElementByIdMap(org.w3c.dom.Document, com.l7tech.util.IdAttributeConfig)}.  Required.
     * @param variableMap a variable map in which to look for a certificate from a context variable, if we are configured to use one.  If null, we will ignore any configured verifyCertificateName.  Optional.
     * @return a table summarizing the signature verification results.  There is one row for each element referenced by the signature, in the format:
     *          { elementCovered, signingCert, sigMethodUri, digestMethodUri, validatedSignatureValue, sigElement }.
     * @throws Exception on verification failure
     */
    public List<Object[]> verifySignature(@NotNull Element sigElement, final @NotNull Map<String, Element> elementsById, final @Nullable Map<String, Object> variableMap) throws Exception {
        final Document doc = sigElement.getOwnerDocument();
        Element keyInfoElement = KeyInfo.searchForKeyInfo(sigElement);

        X509Certificate signingCert;
        if (preconfiguredCert != null && (config.isIgnoreKeyInfo() || keyInfoElement == null)) {
            signingCert = preconfiguredCert;
        } else if (config.getVerifyCertificateVariableName() != null && (config.isIgnoreKeyInfo() || keyInfoElement == null) && variableMap != null) {
            signingCert = resolveSigningCertFromVariable(variableMap);
        } else {
            // then expect to use the keyInfo element as before
            if (keyInfoElement == null)
                throw new InvalidDocumentFormatException("KeyInfo element not found in Signature Element");

            signingCert = resolveKeyInfoByX509Data(keyInfoElement, securityTokenResolver);
        }

        if (signingCert == null)
            throw new SignatureException("Unable to determine signing certificate");

        PublicKey signingKey = signingCert.getPublicKey();

        KeyUsageChecker.requireActivity(KeyUsageActivity.verifyXml, signingCert);
        signingCert.checkValidity();

        // Validate signature
        final SignatureContext sigContext = DsigUtil.createSignatureContextForValidation();
        sigContext.setIDResolver(new IDResolver() {
            @Override
            public Element resolveID(Document document, String s) {
                if (document != doc)
                    throw new IllegalArgumentException("Unable to resolve element in different Document");
                return elementsById.get(s);
            }
        });

        final boolean[] sawMoreThanOneDigestMethod = { false };
        final String[] lastSeenDigestMethod = { null };
        sigContext.setAlgorithmFactory(new WssProcessorAlgorithmFactory() {
            @Override
            public MessageDigest getDigestMethod(String s) throws NoSuchAlgorithmException, NoSuchProviderException {
                if (lastSeenDigestMethod[0] != null && !lastSeenDigestMethod[0].equals(s)) {
                    sawMoreThanOneDigestMethod[0] = true;
                }
                lastSeenDigestMethod[0] = s;
                return super.getDigestMethod(s);
            }
        });
        Validity validity = DsigUtil.verify(sigContext, sigElement, signingKey);

        if (!validity.getCoreValidity()) {
            throw new InvalidDocumentSignatureException(DsigUtil.getInvalidSignatureMessage(validity));
        }

        // Save the SignatureValue
        Element sigValueEl = DomUtils.findOnlyOneChildElementByName(sigElement, sigElement.getNamespaceURI(), "SignatureValue");
        if (sigValueEl == null)
            throw new InvalidDocumentFormatException("Valid ds:Signature contained no ds:SignatureValue"); // can't happen
        String validatedSignatureValue = XmlUtil.getTextValue(sigValueEl);

        // Save the SignatureMethod
        String sigMethodUri = (String)extractSignatureMethodUri.selectSingleNode(sigElement);
        if (sigMethodUri == null || sigMethodUri.trim().length() < 1)
            throw new InvalidDocumentFormatException("Unable to extract SignatureMethod Algorithm URI"); // can't happen

        // Save the DigestMethod
        String digestMethodUri = sawMoreThanOneDigestMethod[0] ? "http://layer7tech.com/digestMethodUris/sawMultiple" : lastSeenDigestMethod[0];

        // Remember which elements were covered
        List<Object[]> outRows = new ArrayList<Object[]>();
        final int numRefs = validity.getNumberOfReferences();
        for (int i = 0; i < numRefs; i++) {
            // Resolve each elements one by one.
            String refId = validity.getReferenceURI(i);
            if (refId != null && refId.length() > 1 && refId.charAt(0) == '#')
                refId = refId.substring(1);
            Element elementCovered = elementsById.get(refId);
            if (elementCovered == null) {
                String msg = "Element covered by signature cannot be found in original document nor in " +
                        "processed document. URI: " + refId;
                logger.warning(msg);
                throw new InvalidDocumentFormatException(msg);
            }

            Object[] outRow = { elementCovered, signingCert, sigMethodUri, digestMethodUri, validatedSignatureValue, sigElement };
            outRows.add(outRow);
        }

        return outRows;
    }

    private X509Certificate resolveSigningCertFromVariable(Map<String, Object> variableMap) throws NoSuchVariableException, IOException, CertificateException {
        final String varName = config.getVerifyCertificateVariableName();
        Object value = variableMap.get(varName);
        if (value == null)
            throw new NoSuchVariableException(varName, "Verify certificate variable not found");

        if (value instanceof X509Certificate) {
            return (X509Certificate) value;
        } else if (value instanceof String) {
            String s = (String) value;
            return CertUtils.decodeFromPEM(s, false);
        } else {
            throw new NoSuchVariableException(varName, "Verify certificate variable was found but was not of type X509Certificate or PEM String");
        }
    }

    /**
     * Returns the specified signature verification certificate in the assertion.  This method only returns a
     * cert when the assertion is configured with a pre-defined certificate (either by certificate Oid or by Name).
     *
     * @return the X509Certificate that was selected in assertion and null if a pre-configured cert was not specified
     * @throws java.security.cert.CertificateException when an error is encountered while retrieving a certificate
     */
    private X509Certificate getSelectedCertificate() throws CertificateException {

        X509Certificate selectedCert = null;
        String description = "";
        try {
            final Goid certOid = config.getVerifyCertificateGoid();
            final String certName = config.getVerifyCertificateName();

            if ( certOid != null ) {
                description = "id #" + certOid;
                TrustedCert trustedCertificate = trustedCertCache.findByPrimaryKey(certOid);
                if ( trustedCertificate != null ) {
                    selectedCert = trustedCertificate.getCertificate();
                } else {
                    throw new CertificateException("Could not find trusted certificate " + description);
                }
            } else if ( certName != null ) {
                description = "name " + certName;
                Collection<TrustedCert> trustedCertificates = trustedCertCache.findByName( certName );
                X509Certificate certificate = null;
                X509Certificate expiredCertificate = null;
                for ( TrustedCert trustedCert : trustedCertificates ) {
                    if ( !isExpiredCert(trustedCert) ) {
                        certificate = trustedCert.getCertificate();
                        break;
                    } else if ( expiredCertificate == null ) {
                        expiredCertificate = trustedCert.getCertificate();
                    }
                }

                if ( certificate != null || expiredCertificate != null ) {
                    selectedCert = certificate!=null ? certificate : expiredCertificate;
                } else {
                    throw new CertificateException("Could not find trusted certificate " + description);
                }
            }
        } catch ( FindException e ) {
            throw new CertificateException("Error when finding trusted certificate: " + description + ": " + ExceptionUtils.getMessage(e), e);
        }

        return selectedCert;
    }

    /**
     * Checks whether the certificate in the argument is expired.
     *
     * @param trustedCert the TrustedCert to check
     * @return true if the certificate is expired, false otherwise
     */
    private boolean isExpiredCert( final TrustedCert trustedCert ) {
        boolean expired = true;

        try {
            expired = trustedCert.isExpiredCert();
        } catch (CertificateException e) {
            audit.logAndAudit(AssertionMessages.WSSECURITY_RECIP_CERT_EXP, new String[]{trustedCert.getName() + " (#" + trustedCert.getGoid() + ")"}, e);
        }

        return expired;
    }

    private X509Certificate resolveKeyInfoByX509Data(Element keyInfo, SecurityTokenResolver securityTokenResolver) throws CertificateException {
        try {
            Element x509Data = DomUtils.findOnlyOneChildElementByName(keyInfo, SoapConstants.DIGSIG_URI, "X509Data");
            if (x509Data == null)
                throw new KeyInfoElement.UnsupportedKeyInfoFormatException("KeyInfo did not contain any recognized certificate reference format");

            return handleX509Data(x509Data, securityTokenResolver);

        } catch (KeyInfoElement.UnsupportedKeyInfoFormatException e) {
            audit.logAndAudit(AssertionMessages.EXCEPTION_INFO_WITH_MORE_INFO, new String[]{"Unrecognized KeyInfo format: " + ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e));
            return null;
        } catch (InvalidDocumentFormatException e) {
            audit.logAndAudit(AssertionMessages.EXCEPTION_INFO_WITH_MORE_INFO, new String[]{"Unable to parse KeyInfo: " + ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e));
            return null;
        } catch (IOException e) {
            audit.logAndAudit(AssertionMessages.EXCEPTION_INFO_WITH_MORE_INFO, new String[]{"Unable to parse KeyInfo: " + ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e));
            return null;
        }
    }

    private static X509Certificate handleX509Data(Element x509Data, SecurityTokenResolver securityTokenResolver) throws CertificateException, InvalidDocumentFormatException, IOException {
        // Use X509Data
        Element x509CertEl = DomUtils.findOnlyOneChildElementByName(x509Data, SoapConstants.DIGSIG_URI, "X509Certificate");
        Element x509SkiEl = DomUtils.findOnlyOneChildElementByName(x509Data, SoapConstants.DIGSIG_URI, "X509SKI");
        Element x509IssuerSerialEl = DomUtils.findOnlyOneChildElementByName(x509Data, SoapConstants.DIGSIG_URI, "X509IssuerSerial");
        Element x509SubjectNameEl = DomUtils.findOnlyOneChildElementByName(x509Data, SoapConstants.DIGSIG_URI, "X509SubjectName");
        if (x509CertEl != null) {
            String certBase64 = DomUtils.getTextValue(x509CertEl);
            byte[] certBytes = CertUtils.decodeCertBytesFromPEM(certBase64, false);
            try {
                return CertUtils.decodeCert(certBytes);
            } catch (CertificateException e) {
                if (!CERT_PARSE_BC_FALLBACK)
                    throw e;
                return (X509Certificate) CertificateFactory.getInstance("X.509", new BouncyCastleProvider()).generateCertificate(new ByteArrayInputStream(certBytes));
            }
        } else if (x509SkiEl != null) {
            String skiRaw = DomUtils.getTextValue(x509SkiEl);
            String ski = HexUtils.encodeBase64(HexUtils.decodeBase64(skiRaw, true), true);
            return securityTokenResolver.lookupBySki(ski);
        } else if (x509IssuerSerialEl != null) {
            final Element issuerEl = DomUtils.findExactlyOneChildElementByName(x509IssuerSerialEl, SoapConstants.DIGSIG_URI, "X509IssuerName");
            final Element serialEl = DomUtils.findExactlyOneChildElementByName(x509IssuerSerialEl, SoapConstants.DIGSIG_URI, "X509SerialNumber");

            final String issuerVal = DomUtils.getTextValue(issuerEl);
            if (issuerVal.length() == 0) throw new MissingRequiredElementException("X509IssuerName was empty");
            final String serialVal = DomUtils.getTextValue(serialEl);
            if (serialVal.length() == 0) throw new MissingRequiredElementException("X509SerialNumber was empty");
            return securityTokenResolver.lookupByIssuerAndSerial(new X500Principal(issuerVal), new BigInteger(serialVal));
        } else if (x509SubjectNameEl != null) {
            final String subjectName = DomUtils.getTextValue(x509SubjectNameEl);
            if (subjectName.length() == 0) throw new MissingRequiredElementException("X509SubjectName was empty");
            return securityTokenResolver.lookupByKeyName(subjectName);
        } else {
            throw new KeyInfoElement.UnsupportedKeyInfoFormatException("KeyInfo X509Data was not in a supported format");
        }
    }
}
