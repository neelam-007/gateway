/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.skunkworks.tarari;

import com.ibm.xml.dsig.AlgorithmFactory;
import com.ibm.xml.dsig.SignatureMethod;
import com.l7tech.message.Message;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.security.xml.processor.MemoizedRsaSha1SignatureMethod;
import com.l7tech.util.HexUtils;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.xml.ElementCursor;
import com.l7tech.xml.InvalidXpathException;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.xml.xpath.*;
import com.l7tech.common.io.CertUtils;
import org.xml.sax.SAXException;

import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * @author alex
 */
class TarariWssProcessingContext {
    private static final Logger logger = Logger.getLogger(TarariWssProcessingContext.class.getName());
    private static final String[][] C14N_ALGORITHM_VALUES = new String[][] { { "http://www.w3.org/2001/10/xml-exc-c14n#" } };
    private static final String[][] SIG_ALGORITHM_VALUES = new String[][] { { "http://www.w3.org/2000/09/xmldsig#rsa-sha1" } };

    private static final String DIGEST_ALG_SHA1 = "http://www.w3.org/2000/09/xmldsig#sha1";

    private static final ThreadLocal tlSha1 = new ThreadLocal() {
        protected Object initialValue() {
            try {
                return MessageDigest.getInstance("SHA-1");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e); // can't happen, misconfigured VM
            }
        }
    };

    public static final boolean memoizeSigs = Boolean.getBoolean("memoizeSigs");
    static { logger.info("memoizeSigs=" + memoizeSigs); }

    private static final Pattern spacePattern = Pattern.compile("\\s+");
    private final Map idToElementMap = new HashMap();
    private final Message msg;
    private MessageDigest sha1;
    private boolean wasSigned = false;

    private String bstId;

    private static class XpathHolder {
        private static final CompiledXpath findAllElementsWithIds;
        static {
            try {
                Map wsus = new HashMap();
                wsus.put("wsu", SoapUtil.WSU_NAMESPACE);
                findAllElementsWithIds = new XpathExpression("//*[@wsu:Id]", wsus).compile();
            } catch (InvalidXpathException e) {
                throw new RuntimeException(e);
            }
        }
    }

    TarariWssProcessingContext(Message msg) throws InvalidDocumentFormatException, IOException, SAXException {
        this.msg = msg;
        if (!msg.isSoap()) throw new InvalidDocumentFormatException();
    }

    private MessageDigest getSha1() {
        if (sha1 == null) {
            sha1 = (MessageDigest)tlSha1.get();
        }
        return sha1;
    }

    void process() throws NoSuchPartException, IOException, SAXException, GeneralSecurityException, InvalidDocumentFormatException {
        ElementCursor ec = msg.getXmlKnob().getElementCursor();
        XpathResult elementsWithIds;
        try {
            elementsWithIds = ec.getXpathResult(XpathHolder.findAllElementsWithIds, null, true);
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
        XpathResultNodeSet nodesWithIds = elementsWithIds.getNodeSet();
        // Collect elements with a wsu:Id
        for (XpathResultIterator j = nodesWithIds.getIterator(); j.hasNext();) {
            ElementCursor elementWithId = j.nextElementAsCursor();
            String id = elementWithId.getAttributeValue("Id", SoapUtil.WSU_URIS_ARRAY);
            if (id == null) continue;
            if (idToElementMap.containsKey(id)) throw new InvalidDocumentFormatException("Duplicate wsu:Id: " + id);
            idToElementMap.put(id, elementWithId);
        }

        ec.moveToDocumentElement();
        ec.moveToFirstChildElement(); // Should be at SOAP header now
        ec.moveToFirstChildElement(); // Should be at first header element
        ec.moveToNextSiblingElement(SoapUtil.SECURITY_EL_NAME, SoapUtil.SECURITY_URIS_ARRAY);
        assert ec.getLocalName().equals(SoapUtil.SECURITY_EL_NAME);
        processSecurityHeader(ec);
        if (!wasSigned) throw new RuntimeException("No valid signature found");
    }

    private void processSecurityHeader(ElementCursor ec) throws InvalidDocumentFormatException {
        ec.visitChildElements(new ElementCursor.Visitor() {
            public void visit(ElementCursor ec) throws InvalidDocumentFormatException {
                // TODO check namespaces!
                String lname = ec.getLocalName();
                if (SoapUtil.BINARYSECURITYTOKEN_EL_NAME.equals(lname)) {
                    String id = ec.getAttributeValue("Id", SoapUtil.WSU_NAMESPACE);
                    if (bstId != null) throw new InvalidDocumentFormatException("Multiple BinarySecurityTokens found in Security header");
                    bstId = id;
                } else if (SoapUtil.SIGNATURE_EL_NAME.equals(lname)) {
                    try {
                        processSignature(ec);
                    } catch (IOException e) {
                        throw new InvalidDocumentFormatException(e);
                    } catch (GeneralSecurityException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
    }

    private static class Reference {
        private final String id;
        private final ElementCursor element;
        private String[] inclusiveNamespacePrefixes;
        private byte[] referenceDigestValue;
        private String digestAlgorithm;
        public boolean digestMatches;

        private Reference(String id, ElementCursor referent) {
            this.id = id;
            this.element = referent;
        }
    }

    private void processSignedInfo(ElementCursor ec, final List references, final List inclusiveNamespacePrefixes) throws InvalidDocumentFormatException {
        ec.visitChildElements(new ElementCursor.Visitor() {
            public void visit(ElementCursor ec) throws InvalidDocumentFormatException {
                String lname = ec.getLocalName();
                if ("CanonicalizationMethod".equals(lname)) {
                    if (ec.matchAttributeValue("Algorithm", C14N_ALGORITHM_VALUES) != 0)
                        throw new IllegalArgumentException("Unsupported C14n Algorithm " + ec.getAttributeValue("Algorithm"));

                    if (ec.moveToFirstChildElement("InclusiveNamespaces", SoapUtil.C14N_EXCLUSIVE)) {
                        String prefixList = ec.getAttributeValue("PrefixList");
                        if (!inclusiveNamespacePrefixes.isEmpty())
                            throw new IllegalStateException("Found more than one Signature/SignedInfo/CanonicalizationMethod/InclusiveNamespaces");
                        inclusiveNamespacePrefixes.addAll(Arrays.asList(spacePattern.split(prefixList)));
                    }
                } else if ("SignatureMethod".equals(lname)) {
                    // TODO support signature algorithms other than rsa-sha1
                    if (ec.matchAttributeValue("Algorithm", SIG_ALGORITHM_VALUES) != 0)
                        throw new IllegalArgumentException("Unsupported Signature Algorithm " + ec.getAttributeValue("Algorithm"));
                } else if ("Reference".equals(lname)) {
                    String uri = cleanUri(ec.getAttributeValue("URI"));
                    final ElementCursor referent = getCursorById(uri);
                    final Reference ref = new Reference(uri, referent);
                    ec.visitChildElements(new ElementCursor.Visitor() {
                        public void visit(ElementCursor ec) throws InvalidDocumentFormatException {
                            String lname = ec.getLocalName();
                            if ("Transforms".equals(lname)) {
                                ec.moveToFirstChildElement();
                                if (!"Transform".equals(ec.getLocalName()))
                                    throw new InvalidDocumentFormatException("Signature/Reference/Transforms did not contain a Transform");

                                if (ec.matchAttributeValue("Algorithm", C14N_ALGORITHM_VALUES) < 0)
                                    throw new InvalidDocumentFormatException("Signature/Reference/Transforms/Transform/@Algorithm had an unexpected value: " + ec.getAttributeValue("Algorithm", SoapUtil.DIGSIG_URI));

                                if (ec.moveToFirstChildElement("InclusiveNamespaces", SoapUtil.C14N_EXCLUSIVE)) {
                                    String prefixList = ec.getAttributeValue("PrefixList");
                                    if (ref.inclusiveNamespacePrefixes != null) throw new IllegalStateException("Already saw prefixes");
                                    ref.inclusiveNamespacePrefixes = spacePattern.split(prefixList);
                                }
                            } else if ("DigestMethod".equals(lname)) {
                                ref.digestAlgorithm = ec.getAttributeValue("Algorithm");
                            } else if ("DigestValue".equals(lname)) {
                                ref.referenceDigestValue = HexUtils.decodeBase64(ec.getTextValue(), true);
                            }
                        }
                    });
                    references.add(ref);
                }
            }
        });
    }

    private ElementCursor getCursorById(String uri) throws InvalidDocumentFormatException {
        final ElementCursor referent = (ElementCursor)idToElementMap.get(uri);
        if (referent == null)
            throw new InvalidDocumentFormatException("Element referenced with wsu:Id = '" + uri.substring(1) + "' could not be found");
        return referent;
    }

    private String cleanUri(String uri) throws InvalidDocumentFormatException {
        if (!uri.startsWith("#") || uri.length() < 2)
            throw new InvalidDocumentFormatException("Unsupported Reference to non-local URI '" + uri + "' (must begin with '#')");
        uri = uri.substring(1);
        return uri;
    }

    private void processSignature(ElementCursor ec) throws InvalidDocumentFormatException, IOException, GeneralSecurityException {
        final List references = new ArrayList();
        final List inclusiveNamespacePrefixes = new ArrayList();
        final ElementCursor[] signedInfoCursor = new ElementCursor[1];
        final X509Certificate[] signingCert = new X509Certificate[1];
        final String[] signatureValueBase64 = new String[1];

        ec.visitChildElements(new ElementCursor.Visitor() {
            public void visit(ElementCursor ec) throws InvalidDocumentFormatException {
                String lname = ec.getLocalName();
                if ("SignedInfo".equals(lname)) {
                    signedInfoCursor[0] = ec.duplicate();
                    processSignedInfo(ec, references, inclusiveNamespacePrefixes);
                } else if ("SignatureValue".equals(lname)) {
                    signatureValueBase64[0] = ec.getTextValue();
                } else if ("KeyInfo".equals(lname)) {
                    ec.moveToFirstChildElement();
                    lname = ec.getLocalName();
                    if ("SecurityTokenReference".equals(lname)) {
                        ec.moveToFirstChildElement();
                        lname = ec.getLocalName();
                        if ("Reference".equals(lname)) {
                            String uri = cleanUri(ec.getAttributeValue("URI"));
                            String valueType = ec.getAttributeValue("ValueType");
                            if (SoapUtil.VALUETYPE_X509.equals(valueType) || SoapUtil.VALUETYPE_X509_2.equals(valueType)) {
                                ElementCursor target = getCursorById(uri);
                                String targetId = target.getAttributeValue("Id", SoapUtil.WSU_NAMESPACE);
                                if (bstId == null || !bstId.equals(targetId))
                                    throw new InvalidDocumentFormatException("Referenced BinarySecurityToken was not found in the expected Security header");
                                String b64 = target.getTextValue();
                                byte[] bytes;
                                try {
                                    bytes = HexUtils.decodeBase64(b64, true);
                                    signingCert[0] = CertUtils.decodeCert(bytes);
                                } catch (CertificateException e) {
                                    throw new RuntimeException(e);
                                }
                            }

                        }
                    }
                }
            }
        });


        // Check that the digests match the referents
        for (Iterator i = references.iterator(); i.hasNext();) {
            Reference reference = (Reference)i.next();
            byte[] referentDigest;
            byte[] canonicalReferentBytes = reference.element.canonicalize(reference.inclusiveNamespacePrefixes);
            if (DIGEST_ALG_SHA1.equals(reference.digestAlgorithm)) {
                referentDigest = getSha1().digest(canonicalReferentBytes);
            } else {
                throw new InvalidDocumentFormatException("Unsupported Digest algorithm: " + reference.digestAlgorithm);
            }

            if (Arrays.equals(referentDigest, reference.referenceDigestValue)) {
                reference.digestMatches = true;
            } else {
                throw new InvalidDocumentFormatException("Reference digest value mismatch for Id " + reference.id);
            }
        }

        String[] prefixes = (String[])inclusiveNamespacePrefixes.toArray(new String[0]);
        ElementCursor signedInfo = signedInfoCursor[0];
        if (signedInfo == null) throw new InvalidDocumentFormatException("No SignedInfo");

        byte[] signatureValueBytes = HexUtils.decodeBase64(signatureValueBase64[0], true);
        byte[] signedInfoBytes = signedInfo.canonicalize(prefixes);

        if (memoizeSigs) {
            // Reuse xss4j cached signature
            SignatureMethod sm = (SignatureMethod)rsaSigMethod.get();
            sm.initVerify(signingCert[0].getPublicKey());
            sm.update(signedInfoBytes);
            if (!sm.verify(signatureValueBytes)) throw new InvalidDocumentFormatException("Signature verification failed");
        } else {
            // Implement using Signature directly
            Signature sig = Signature.getInstance("SHA1withRSA");
            sig.initVerify(signingCert[0].getPublicKey());
            sig.update(signedInfoBytes);
            if (!sig.verify(signatureValueBytes)) throw new InvalidDocumentFormatException("Signature verification failed");
        }

        wasSigned = true;
    }

    private static ThreadLocal rsaSigMethod = new ThreadLocal() {
        protected Object initialValue() {
            try {
                AlgorithmFactory af = new AlgorithmFactory(JceProvider.getAsymmetricJceProvider().getName());
                SignatureMethod wrapped = af.getSignatureMethod(SignatureMethod.RSA,  null);
                return new MemoizedRsaSha1SignatureMethod(wrapped);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e); // can't happen
            } catch (NoSuchProviderException e) {
                throw new RuntimeException(e); // can't happen
            }
        }
    };
}
