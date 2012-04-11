package com.l7tech.external.assertions.xmlsec.server;

import com.ibm.xml.dsig.IDResolver;
import com.ibm.xml.dsig.KeyInfo;
import com.ibm.xml.dsig.SignatureContext;
import com.ibm.xml.dsig.Validity;
import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.xmlsec.NonSoapVerifyElementAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.security.cert.KeyUsageActivity;
import com.l7tech.security.cert.KeyUsageChecker;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.xml.DsigUtil;
import com.l7tech.security.xml.KeyInfoElement;
import com.l7tech.security.xml.SecurityTokenResolver;
import com.l7tech.security.xml.processor.WssProcessorAlgorithmFactory;
import com.l7tech.server.identity.cert.TrustedCertCache;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.util.*;
import com.l7tech.xml.InvalidDocumentSignatureException;
import com.l7tech.xml.InvalidXpathException;
import com.l7tech.xml.soap.SoapUtil;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jaxen.JaxenException;
import org.jaxen.dom.DOMXPath;
import org.springframework.beans.factory.BeanFactory;
import org.w3c.dom.*;

import javax.security.auth.x500.X500Principal;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.*;

import static com.l7tech.external.assertions.xmlsec.NonSoapVerifyElementAssertion.*;

/**
 * Server side implementation of non-SOAP signature verification.
 */
public class ServerNonSoapVerifyElementAssertion extends ServerNonSoapSecurityAssertion<NonSoapVerifyElementAssertion> {
    static final String PROP_CERT_PARSE_BC_FALLBACK = "com.l7tech.external.assertions.xmlsec.certParseBcFallback";
    static boolean CERT_PARSE_BC_FALLBACK = ConfigFactory.getBooleanProperty( PROP_CERT_PARSE_BC_FALLBACK, false );

    private static final int COL_SIGNED_ELEMENT = 0;
    private static final int COL_SIGNER_CERT = 1;
    private static final int COL_SIG_METHOD_URI = 2;
    private static final int COL_DIG_METHOD_URI = 3;
    private static final int COL_VALIDATED_SIGNATURE_VALUES = 4;
    private static final int COL_SIGNATURE_ELEMENT = 5;

    private final SecurityTokenResolver securityTokenResolver;
    private final TrustedCertCache trustedCertCache;
    private final IdAttributeConfig idAttributeConfig;

    public ServerNonSoapVerifyElementAssertion(NonSoapVerifyElementAssertion assertion, BeanFactory beanFactory) throws InvalidXpathException, ParseException {
        super(assertion);
        this.securityTokenResolver = beanFactory.getBean("securityTokenResolver", SecurityTokenResolver.class);
        this.trustedCertCache = beanFactory.getBean( "trustedCertCache", TrustedCertCache.class );

        final FullQName[] idAttrsArray = assertion.getCustomIdAttrs();
        Collection<FullQName> ids = idAttrsArray == null || idAttrsArray.length < 1 ? DEFAULT_ID_ATTRS : Arrays.asList(idAttrsArray);

        this.idAttributeConfig = IdAttributeConfig.makeIdAttributeConfig(ids);
    }

    private static <T> Collection<T> getColumn(List<Object[]> table, int column, Class<T> clazz) {
        List<T> ret = new ArrayList<T>();
        for (Object[] row : table) {
            final Object obj = row[column];
            if (!clazz.isInstance(obj))
                throw new ClassCastException("Column " + column + " contains non-" + clazz);
            //noinspection unchecked
            ret.add((T)obj);
        }
        return ret;
    }

    @Override
    protected AssertionStatus processAffectedElements(PolicyEnforcementContext context, Message message, Document doc, List<Element> affectedElements) throws Exception {
        List<Object[]> infos = new ArrayList<Object[]>();

        Map<String, Element> elementsById = DomUtils.getElementByIdMap(doc, idAttributeConfig);
        elementsById.put("", doc.getDocumentElement());

        final X509Certificate selectedCert = getSelectedCertificate();

        for (Element sigElement : affectedElements) {
            List<Object[]> results = verifySignature(sigElement, elementsById, selectedCert);
            infos.addAll(results);
        }

        Collection<Element> signedElements = getColumn(infos, COL_SIGNED_ELEMENT, Element.class);
        Collection<X509Certificate> signerCerts = getColumn(infos, COL_SIGNER_CERT, X509Certificate.class);
        Collection<String> signatureMethodUris = getColumn(infos, COL_SIG_METHOD_URI, String.class);
        Collection<String> digestMethodUris = getColumn(infos, COL_DIG_METHOD_URI, String.class);
        Collection<String> signatureValues = getColumn(infos, COL_VALIDATED_SIGNATURE_VALUES, String.class);
        Collection<Element> signatureElements = getColumn(infos, COL_SIGNATURE_ELEMENT, Element.class);

        context.setVariable(assertion.prefix(VAR_ELEMENTS_VERIFIED), signedElements.toArray(new Element[signedElements.size()]));
        context.setVariable(assertion.prefix(VAR_SIGNING_CERTIFICATES), signerCerts.toArray(new X509Certificate[signerCerts.size()]));
        context.setVariable(assertion.prefix(VAR_SIGNATURE_METHOD_URIS), signatureMethodUris.toArray(new String[signatureMethodUris.size()]));
        context.setVariable(assertion.prefix(VAR_DIGEST_METHOD_URIS), digestMethodUris.toArray(new String[digestMethodUris.size()]));
        context.setVariable(assertion.prefix(VAR_SIGNATURE_VALUES), signatureValues.toArray(new String[signatureMethodUris.size()]));
        context.setVariable(assertion.prefix(VAR_SIGNATURE_ELEMENTS), signatureElements.toArray(new Element[signatureElements.size()]));

        return AssertionStatus.NONE;
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

    private List<Object[]> verifySignature(Element sigElement, final Map<String, Element> elementsById, final X509Certificate selectedCert) throws Exception {
        final Document doc = sigElement.getOwnerDocument();
        Element keyInfoElement = KeyInfo.searchForKeyInfo(sigElement);

        X509Certificate signingCert;
        if (selectedCert != null && (assertion.isIgnoreKeyInfo() || keyInfoElement == null)) {
            signingCert = selectedCert;
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
            StringBuilder msg = new StringBuilder("Signature not valid. " + validity.getSignedInfoMessage());
            for (int i = 0; i < validity.getNumberOfReferences(); i++) {
                msg.append("\n\tElement ").append(validity.getReferenceURI(i)).append(": ").append(validity.getReferenceMessage(i));
            }
            logger.warning(msg.toString());
            throw new InvalidDocumentSignatureException(msg.toString());
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

    /**
     * Returns the specified signature verification certificate in the assertion.  This method only returns a
     * cert when the assertion is configured with a pre-defined certificate (either by certificate Oid or by Name).
     *
     * @return the X509Certificate that was selected in assertion and null if a pre-configured cert was not specified
     * @throws CertificateException when an error is encountered while retrieving a certificate
     */
    private X509Certificate getSelectedCertificate() throws CertificateException {

        X509Certificate selectedCert = null;
        String description = "";
        try {
            final long certOid = assertion.getVerifyCertificateOid();
            final String certName = assertion.getVerifyCertificateName();

            if ( certOid > 0 ) {
                description = "id #" + certOid;
                TrustedCert trustedCertificate = trustedCertCache.findByPrimaryKey( certOid );
                if ( trustedCertificate != null ) {
                    selectedCert = trustedCertificate.getCertificate();
                } else {
                    logAndAudit(AssertionMessages.WSSECURITY_RECIP_NO_CERT, description);
                    throw new AssertionStatusException(AssertionStatus.FALSIFIED);
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
                    logAndAudit(AssertionMessages.WSSECURITY_RECIP_NO_CERT, description);
                    throw new AssertionStatusException(AssertionStatus.FALSIFIED);
                }
            }
        } catch ( FindException e ) {
            logAndAudit(AssertionMessages.WSSECURITY_RECIP_CERT_ERROR, description);
            throw new AssertionStatusException(AssertionStatus.FALSIFIED);
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
            logAndAudit(AssertionMessages.WSSECURITY_RECIP_CERT_EXP, new String[]{ trustedCert.getName() + " (#"+trustedCert.getOid()+")"}, e);
        }

        return expired;
    }

    @SuppressWarnings({"ThrowableResultOfMethodCallIgnored"})
    private X509Certificate resolveKeyInfoByX509Data(Element keyInfo, SecurityTokenResolver securityTokenResolver) throws CertificateException {
        try {
            Element x509Data = DomUtils.findOnlyOneChildElementByName(keyInfo, SoapConstants.DIGSIG_URI, "X509Data");
            if (x509Data == null)
                throw new KeyInfoElement.UnsupportedKeyInfoFormatException("KeyInfo did not contain any recognized certificate reference format");

            return handleX509Data(x509Data, securityTokenResolver);

        } catch (KeyInfoElement.UnsupportedKeyInfoFormatException e) {
            logAndAudit(AssertionMessages.EXCEPTION_INFO_WITH_MORE_INFO, new String[] { "Unrecognized KeyInfo format: " + ExceptionUtils.getMessage(e) }, ExceptionUtils.getDebugException(e));
            return null;
        } catch (InvalidDocumentFormatException e) {
            logAndAudit(AssertionMessages.EXCEPTION_INFO_WITH_MORE_INFO, new String[] { "Unable to parse KeyInfo: " + ExceptionUtils.getMessage(e) }, ExceptionUtils.getDebugException(e));
            return null;
        } catch (IOException e) {
            logAndAudit(AssertionMessages.EXCEPTION_INFO_WITH_MORE_INFO, new String[] { "Unable to parse KeyInfo: " + ExceptionUtils.getMessage(e) }, ExceptionUtils.getDebugException(e));
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
                return (X509Certificate)CertificateFactory.getInstance("X.509", new BouncyCastleProvider()).generateCertificate(new ByteArrayInputStream(certBytes));
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
