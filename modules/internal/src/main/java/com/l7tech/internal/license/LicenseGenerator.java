/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.internal.license;

import com.ibm.xml.dsig.SignatureStructureException;
import com.ibm.xml.dsig.XSignatureException;
import com.l7tech.util.*;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.security.xml.DsigUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Random;

/**
 * Class that generates License XML files.  An instance of this class may only be used to generate one license.
 */
public final class LicenseGenerator {
    private static final String LIC_EL = "license";
    private static final String LIC_NS = "http://l7tech.com/license";

    private LicenseGenerator() {}

    /** Exception thrown if a license cannot be generated. */
    public static class LicenseGeneratorException extends Exception {
        public LicenseGeneratorException() {}
        public LicenseGeneratorException(String message) { super(message); }
        public LicenseGeneratorException(String message, Throwable cause) { super(message, cause); }
        public LicenseGeneratorException(Throwable cause) { super(cause); }
    }

    /**
     * Generate an unsigned license.  This can be useful for debugging, testing, and quick GUI feedback
     * since signing can be slow.  Note though that an unsigned license is very unlikely to be accepted
     * by an actual license-managed product.
     *
     * @param spec  the license specification to generate.  Must be complete (containing a valid ID and licensee name
     *              and possibly other fields; see {@link com.l7tech.internal.license.LicenseSpec}).  Does not need to include a signing
     *              cert and key since the license isn't going to be signed.
     * @param ignoreErrors if true, will attempt to ignore errors in the license spec and return a partial license document.
     * @return an unsigned license.  Never null.
     * @throws LicenseGeneratorException if a license cannot be generated with this LicenseSpec.
     */
    public static Document generateUnsignedLicense(LicenseSpec spec, boolean ignoreErrors) throws LicenseGeneratorException {
        String name = spec.getLicenseeName();
        if (name == null || name.length() < 1) {
            if (!ignoreErrors)
                throw new LicenseGeneratorException("A licensee name is required.");
            name = "";
        }
        long id = spec.getLicenseId();
        if (id < 1) {
            if (!ignoreErrors)
                throw new LicenseGeneratorException("A unique, positive license ID is required.");
        }

        if (spec.getEulaText() == null || spec.getEulaText().trim().length() < 1)
            if (!ignoreErrors)
                throw new LicenseGeneratorException("Non-empty EULA text is required.");

        Document d = XmlUtil.createEmptyDocument(LIC_EL, null, LIC_NS);
        final Element de = d.getDocumentElement();

        de.setAttribute("Id", Long.toString(id));
        appendSimpleElementIfNonEmpty(de, "description", spec.getDescription());
        Element licenseAttributes = appendSimpleElement(de, "licenseAttributes", null);
        for (String attribute: spec.getAttributes()) {
            appendSimpleElementIfNonEmpty(licenseAttributes, "attribute", attribute);
        }
        appendSimpleElementIfNonEmpty(de, "valid", spec.getStartDate());
        appendSimpleElementIfNonEmpty(de, "expires", spec.getExpiryDate());
        Element host = appendSimpleElement(de, "host", null);
        host.setAttribute("name", spec.getHostname());
        Element ip = appendSimpleElement(de, "ip", null);
        ip.setAttribute("address", spec.getIp());
        Element product = appendSimpleElement(de, "product", null);
        product.setAttribute("name", spec.getProduct());
        Element version = appendSimpleElement(product, "version", null);
        version.setAttribute("major", spec.getVersionMajor());
        version.setAttribute("minor", spec.getVersionMinor());
        appendSimpleElementIfNonEmpty(de, "featureLabel", spec.getFeatureLabel());

        for (String featureName : spec.getRootFeatures()) {
            Element featureset = appendSimpleElement(product, "featureset", null);
            featureset.setAttribute("name", featureName);
        }

        Element licensee = appendSimpleElement(de, "licensee", null);
        licensee.setAttribute("name", name);
        setAttributeIfNonEmpty(licensee, "contactEmail", spec.getLicenseeContactEmail());

        appendSimpleElementIfNonEmpty(de, "eulatext", base64(spec.getEulaText()));

        try {
            // serialize and reparse before returning to make sure we don't leak any strange DOM states
            return XmlUtil.stringToDocument(XmlUtil.nodeToString(d));
        } catch (IOException e) {
            throw new RuntimeException(e); // can't happen
        } catch (SAXException e) {
            throw new RuntimeException(e); // can't happen
        }
    }

    private static Element appendSimpleElementIfNonEmpty(Element parent, String elname, Date date) {
        if (date != null)
            return appendSimpleElement(parent, elname, ISO8601Date.format(date));
        return null;
    }

    private static Element appendSimpleElementIfNonEmpty(Element parent, String elname, String value) {
        if (value != null && value.length() > 0)
            return appendSimpleElement(parent, elname, value);
        return null;
    }

    /**
     * @param in  raw input.  If null or empty, this method returns null.
     * @return the base64-ed input, or null if null was passed in.
     */
    private static String base64(String in) {
        if (in == null || in.trim().length() < 1)
            return null;
        try {
            String enc = HexUtils.encodeBase64(IOUtils.compressGzip(in.getBytes(Charsets.UTF8)));
            return enc.replaceAll("(?<!\n)\r(?!\n)|(?<!\r)\n(?!\r)", "\r\n");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e); // can't happen
        } catch (IOException e) {
            throw new RuntimeException(e); // can't happen
        }
    }

    /*
     * Append a simple element of the form &lt;elname&gt;value&lt;/elname&gt; to the parent element.
     */
    private static Element appendSimpleElement(final Element parent, final String elname, final String value) {
        Element elm = DomUtils.createAndAppendElement(parent, elname);
        if (value != null && value.length() > 0)
            DomUtils.setTextContent(elm, value);
        return elm;
    }

    private static void setAttributeIfNonEmpty(Element element, String attribute, String value) {
        if (value != null && value.length() > 0)
            element.setAttribute(attribute, value);
    }

    /**
     * Generate a signed license.  This requires that the specification include a signing cert and key.
     *
     * @param spec  the license specification to generate.  Must be complete (containing a valid ID and licensee name
     *              and possibly other fields; see {@link LicenseSpec}).  Needs to include a signing
     *              cert and key.
     *
     * @return an signed license document.  Never null.
     * @throws LicenseGeneratorException if a license cannot be generated with this LicenseSpec.
     */
    public static Document generateSignedLicense(LicenseSpec spec) throws LicenseGeneratorException {
        final X509Certificate signerCert = spec.getIssuerCert();
        final PrivateKey signerKey = spec.getIssuerKey();
        if (signerCert == null || signerKey == null)
            throw new LicenseGeneratorException("An issuer certificate and key are required to generate a signed license.");
        Document d = generateUnsignedLicense(spec, false);

        return signLicenseDocument(d, signerCert, signerKey);
    }

    /**
     * Sign an existing license document.
     * The existing document will be assumed to be valid.  Unless you already have a valid license document,
     * you should use generateSignedLicense() to generate the license XML and sign it all in one step.
     * <p>
     * Use this method only if the license XML may contain additional material that you do not want to lose.
     * Note, though, that the additional material will have its whitespace stripped along with everything else
     * before the signature is created.
     * <p>
     * Any existing ds:Signature second-level elements will be removed before the new signature is created.
     *
     * @param licenseDoc     the license document to sign.  Must be non-null.  Will be _assumed_ to be a valid license document.
     * @param signerCert  certificate to sign it with.  Must not be null.
     * @param signerKey   key to sign it with.  Must not be null.
     * @return the newly-signed document.
     * @throws LicenseGeneratorException if the document could not be signed
     */
    public static Document signLicenseDocument(Document licenseDoc, X509Certificate signerCert, PrivateKey signerKey)
            throws LicenseGeneratorException
    {
        if (signerCert == null || signerKey == null)
            throw new LicenseGeneratorException("An issuer certificate and key are required to generate a signed license.");
        try {
            // Find and remove any existing ds:Signature
            DsigUtil.stripSignatures(licenseDoc.getDocumentElement());

            // Reparse to clean out consecutive text nodes
            licenseDoc = XmlUtil.stringToDocument(XmlUtil.nodeToString(licenseDoc));

            DomUtils.stripWhitespace(licenseDoc.getDocumentElement());
            licenseDoc = XmlUtil.stringToDocument(XmlUtil.nodeToString(licenseDoc));
            Element signature = DsigUtil.createEnvelopedSignature(licenseDoc.getDocumentElement(),
                                                                  signerCert,
                                                                  signerKey,
                                                                  null,
                                                                  signerCert.getSubjectDN().getName(), null);
            licenseDoc.getDocumentElement().appendChild(signature);
            return licenseDoc;
        } catch (SignatureException e) {
            throw new LicenseGeneratorException(e);
        } catch (SignatureStructureException e) {
            throw new LicenseGeneratorException(e);
        } catch (XSignatureException e) {
            throw new LicenseGeneratorException(e);
        } catch (IOException e) {
            throw new LicenseGeneratorException(e); // shouldn't happen
        } catch (SAXException e) {
            throw new LicenseGeneratorException(e); // unlikely
        }
    }

    public static long generateRandomId(Random random) {
        long rand;

        do {
            rand = Math.abs(random.nextLong());
        } while (rand == 0); // reroll zeros

        return rand;
    }
}
