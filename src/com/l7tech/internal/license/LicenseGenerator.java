/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.internal.license;

import com.ibm.xml.dsig.SignatureStructureException;
import com.ibm.xml.dsig.XSignatureException;
import com.l7tech.common.security.xml.DsigUtil;
import com.l7tech.common.util.ISO8601Date;
import com.l7tech.common.util.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.util.Date;

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
     *              and possibly other fields; see {@link LicenseSpec}).  Does not need to include a signing
     *              cert and key since the license isn't going to be signed.
     * @return an unsigned license.  Never null.
     * @throws LicenseGeneratorException if a license cannot be generated with this LicenseSpec.
     */
    public static final Document generateUnsignedLicense(LicenseSpec spec) throws LicenseGeneratorException {
        String name = spec.getLicenseeName();
        if (name == null || name.length() < 1) throw new LicenseGeneratorException("A licensee name is required.");
        long id = spec.getLicenseId();
        if (id < 1) throw new LicenseGeneratorException("A unique, positive license ID is required.");

        Document d = XmlUtil.createEmptyDocument(LIC_EL, null, LIC_NS);
        final Element de = d.getDocumentElement();

        appendSimpleElementIfNonEmpty(de, "description", spec.getDescription());
        appendSimpleElementIfNonEmpty(de, "valid", spec.getStartDate());
        appendSimpleElementIfNonEmpty(de, "expires", spec.getExpiryDate());


        try {
            // serialize and reparse before returning to make sure we don't leak any strange DOM states
            return XmlUtil.stringToDocument(XmlUtil.nodeToString(d));
        } catch (IOException e) {
            throw new RuntimeException(e); // can't happen
        } catch (SAXException e) {
            throw new RuntimeException(e); // can't happen
        }
    }

    private static void appendSimpleElementIfNonEmpty(Element parent, String elname, Date date) {
        if (date != null)
            appendSimpleElement(parent, elname, ISO8601Date.format(date));
    }

    private static void appendSimpleElementIfNonEmpty(Element parent, String elname, String value) {
        if (value != null && value.length() > 0)
            appendSimpleElement(parent, elname, value);
    }

    /**
     * Append a simple element of the form &lt;elname&gt;value&lt;/elname&gt; to the parent element.
     *
     * @param parent
     * @param elname
     * @param value
     */
    private static void appendSimpleElement(final Element parent, final String elname, final String value) {
        Element desc = XmlUtil.createAndAppendElement(parent, elname);
        XmlUtil.setTextContent(desc, value);
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
        if (spec.getIssuerCert() == null || spec.getIssuerKey() == null)
            throw new LicenseGeneratorException("An issuer certificate and key are required to generate a signed license.");
        Document d = generateUnsignedLicense(spec);

        try {
            Element signature = DsigUtil.createEnvelopedSignature(d.getDocumentElement(),
                                                                  spec.getIssuerCert(),
                                                                  spec.getIssuerKey(),
                                                                  false);
            d.getDocumentElement().appendChild(signature);
            return d;
        } catch (SignatureException e) {
            throw new LicenseGeneratorException(e);
        } catch (SignatureStructureException e) {
            throw new LicenseGeneratorException(e);
        } catch (XSignatureException e) {
            throw new LicenseGeneratorException(e);
        } catch (CertificateEncodingException e) {
            throw new LicenseGeneratorException(e);
        }
    }

}
