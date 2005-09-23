/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common;

import com.l7tech.common.util.ISO8601Date;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.CertUtils;
import com.l7tech.common.xml.TooManyChildElementsException;
import com.l7tech.common.security.xml.DsigUtil;
import com.l7tech.common.security.xml.SimpleCertificateResolver;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.text.ParseException;
import java.util.Date;
import java.util.Arrays;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.SignatureException;

/**
 * Immutable in-memory representation of a License file.
 */
public class License {
    public static final String LIC_NS = "http://l7tech.com/license";
    private final Document licenseDoc;
    private final long id;
    private final boolean validSignature;
    private final X509Certificate trustedIssuer;
    private final Date startDate;
    private final long startDateUt;
    private final Date expiryDate;
    private final long expiryDateUt;
    private final String description;
    private final String hostname;
    private final String ip;
    private final String licenseeName;
    private final String licenseeContactEmail;
    private final String product;
    private final String versionMajor;
    private final String versionMinor;

    /**
     * Parse the specified license document, and validate the signature if it is signed.
     *
     * The document must follow this schematic:
     * <pre>
     *     license id=NNN
     *         description(blah blah blah)
     *         valid(ISO8601Date string)
     *         expires(ISO8601Date string)
     *         host name=foo.bar.com (or "*")
     *         ip address=1.2.3.4 (or "*")
     *         product name="SecureSpan Gateway" (or "*")
     *             version major=3(or "*") minor=4(or "*")
     *         licensee contactEmail="whatever@wherever"(optional) name="Organization Name"
     *         ds:Signature (optional, assuming license is signed)
     *             ds:SignedInfo ... (signed info must cover the license root element)
     *             ds:SignatureValue ...
     *             ds:KeyInfo
     *                 KeyName("CN=SSGL1, O=L7Tech, OU=Licensing") (must match DN from a trusted issuer cert)
     * </pre>
     *
     * @param licenseXml     a String containing the License as XML data.
     * @param trustedIssuers
     * @throws SAXException if the licenseXml is not well-formed XML
     * @throws ParseException if one of the fields of the license contains illegally-formatted data
     * @throws TooManyChildElementsException if there is more than one copy of an element that there can be only one of (ie, expires, Signature, etc)
     * @throws SignatureException if the license is signed, but the signature wasn't valid or wasn't made by a trusted licence issuer
     */
    public License(String licenseXml, X509Certificate[] trustedIssuers)
            throws SAXException, ParseException, TooManyChildElementsException, SignatureException
    {
        if (licenseXml == null) throw new NullPointerException();
        Document ld = XmlUtil.stringToDocument(licenseXml);
        XmlUtil.stripWhitespace(ld.getDocumentElement());

        Element lic = ld.getDocumentElement();
        if (lic == null) throw new NullPointerException(); // can't happen
        if (!(LIC_NS.equals(lic.getNamespaceURI()))) throw new SAXException("License document element not in namespace " + LIC_NS);
        if (!("license".equals(lic.getLocalName()))) throw new SAXException("License local name is not \"license\"");
        this.licenseDoc = ld;

        try {
            id = Long.parseLong(lic.getAttribute("Id"));
            if (id < 1)
                throw new SAXException("License id is non-positive");
        } catch (NumberFormatException e) {
            throw new SAXException("License id is missing or non-numeric");
        }

        startDate = parseDateElement("valid");
        startDateUt = startDate != null ? startDate.getTime() : Long.MIN_VALUE;
        expiryDate = parseDateElement("expires");
        expiryDateUt = expiryDate != null ? expiryDate.getTime() : Long.MAX_VALUE;
        description = parseStringElement("description");
        hostname = parseStringAttribute("host", "name");
        ip = parseStringAttribute("ip", "address");
        product = parseStringAttribute("product", "name");
        versionMajor = parseStringAttribute("product", "version", "major");
        versionMinor = parseStringAttribute("product", "version", "minor");
        licenseeName = parseStringAttribute("licensee", "name");
        licenseeContactEmail = parseStringAttribute("licensee", "contactEmail");

        // Look for valid signature by trusted issuer
        Element signature = XmlUtil.findOnlyOneChildElementByName(lic, SoapUtil.DIGSIG_URI, "Signature");
        if (signature != null) {
            // See if it is valid and if we trust it
            X509Certificate gotCert = DsigUtil.checkSimpleEnvelopedSignature(signature, new SimpleCertificateResolver(trustedIssuers));
            X509Certificate foundTrustedIssuer = null;
            for (int i = 0; i < trustedIssuers.length; i++) {
                X509Certificate issuer = trustedIssuers[i];
                if (CertUtils.certsAreEqual(issuer, gotCert)) {
                    foundTrustedIssuer = issuer;
                    break;
                }
            }
            if (foundTrustedIssuer == null)
                throw new SignatureException("The license was signed, but we do not recognize the signer as a valid license issuer.");
            trustedIssuer = foundTrustedIssuer;
            validSignature = true;
        } else {
            // No signature
            validSignature = false;
            trustedIssuer = null;
        }

    }

    /**
     * Parse a simple String out of a top-level element of this license.
     *
     * @param elementName  name of the element to search for.  Must be an immediate child of the license root element.
     * @return the string parsed out of this element, or "*" if the element wasn't found or it was empty.
     * @throws TooManyChildElementsException if there is more than one element with this name.
     */
    private String parseStringElement(String elementName) throws TooManyChildElementsException {
        Element lic = licenseDoc.getDocumentElement();
        Element elm = XmlUtil.findOnlyOneChildElementByName(lic, lic.getNamespaceURI(), elementName);
        if (elm == null)
            return "*";
        String val = XmlUtil.getTextValue(elm);
        if (val == null || val.length() < 1)
            return "*";
        return val;
    }

    /**
     * Parse a simple String out of an attribute of a top-level element of this license.
     *
     * @param elementName  name of the element to search for.  Must be an immediate child of the license root element.
     * @param attributeName name of the attribute of this element to snag.  Must not be in any namespace.
     * @return the string parsed out of this element, or "*" if the element wasn't found or it was empty.
     * @throws TooManyChildElementsException if there is more than one element with this name.
     */
    private String parseStringAttribute(String elementName, String attributeName) throws TooManyChildElementsException {
        Element lic = licenseDoc.getDocumentElement();
        Element elm = XmlUtil.findOnlyOneChildElementByName(lic, lic.getNamespaceURI(), elementName);
        if (elm == null)
            return "*";
        String val = elm.getAttribute(attributeName);
        if (val == null || val.length() < 1)
            return "*";
        return val;
    }

    /**
     * Parse a simple String out of an attribute of a second-level element of this license.
     *
     * @param nameTopEl  name of the top-level element to search for.  Must be an immediate child of the license root element.
     * @param name2ndEl  name of the 2nd-level element to search for.  Must be an immediate child of nameTopEl.
     * @param attributeName name of the attribute of this element to snag.  Must not be in any namespace.
     * @return the string parsed out of this element, or "*" if the element wasn't found or it was empty.
     * @throws TooManyChildElementsException if there is more than one element with this name.
     */
    private String parseStringAttribute(String nameTopEl, String name2ndEl, String attributeName) throws TooManyChildElementsException {
        Element lic = licenseDoc.getDocumentElement();
        Element telm = XmlUtil.findOnlyOneChildElementByName(lic, lic.getNamespaceURI(), nameTopEl);
        if (telm == null)
            return "*";
        Element elm = XmlUtil.findOnlyOneChildElementByName(telm, lic.getNamespaceURI(), name2ndEl);
        String val = elm.getAttribute(attributeName);
        if (val == null || val.length() < 1)
            return "*";
        return val;
    }

    /**
     * Parse a date out of this license.
     *
     * @param elementName name of the element to search for.  Must be an immediate child of the license root element.
     * @return the date parsed out of this element, or null if the element was not found.
     * @throws ParseException if there is a date but it is invalid.
     * @throws TooManyChildElementsException if there is more than one element with this name.
     */
    private Date parseDateElement(String elementName) throws ParseException, TooManyChildElementsException {
        Element lic = licenseDoc.getDocumentElement();
        Element elm = XmlUtil.findOnlyOneChildElementByName(lic, lic.getNamespaceURI(), elementName);
        if (elm == null)
            return null;
        return ISO8601Date.parse(XmlUtil.getTextValue(elm));
    }

    /**
     * Get the ID of this license.  Every license generated by a given license issuer should have
     * a different ID.
     *
     * @return the Id of this license.  Always a positive integer.
     */
    public long getId() {
        return id;
    }

    /**
     * Get the XML document that was used to produce this License instance.
     *
     * @return the XML document from which this License instance was generated.  Never null.
     */
    public Document getDocument() {
        return licenseDoc;
    }

    /**
     * Get the expiry date for this license.
     *
     * @return the expiry date of this license, or null if it never expires.
     */
    public Date getExpiryDate() {
        return expiryDate;
    }

    /**
     * Get the certificate of the trusted issuer that issued this license, if the license was signed
     * by a trusted issuer.
     *
     * @return the issuer's certificate, or null if this license was not signed by a trusted issuer.
     */
    public X509Certificate getTrustedIssuer() {
        return trustedIssuer;
    }

    /**
     * Check if this license was signed by a trusted issuer.
     *
     * @return true iff. this license was signed by a certificate that is trusted to issue licenses to us.
     */
    public boolean isValidSignature() {
        return validSignature;
    }

    /** @return the minor version this license codes for, or "*" if it allows any minor version. */
    public String getVersionMinor() {
        return versionMinor;
    }

    /** @return the major version this license codes for, or "*" if it allows any major version. */
    public String getVersionMajor() {
        return versionMajor;
    }

    /** @return the product this license codes for, or "*" if it allows any product. */
    public String getProduct() {
        return product;
    }

    /** @return the licensee contact email address, or null if the license didn't contain one. */
    public String getLicenseeContactEmail() {
        return licenseeContactEmail;
    }

    /** @return the licensee name.  Never null or empty. */
    public String getLicenseeName() {
        return licenseeName;
    }

    /** @return the IP address this license codes for, or "*" if it allows any IP address. */
    public String getIp() {
        return ip;
    }

    /** @return the hostname this license codes for, or "*" if it allows any hostname. */
    public String getHostname() {
        return hostname;
    }

    /** @return the short human-readable description of this license, or null if it didn't contain one. */
    public String getDescription() {
        return description;
    }

    /** @return the earliest date at which this license can be considered valid, or null if the license period starts at the dawn of time. */
    public Date getStartDate() {
        return startDate;
    }

    /** @return the XML Document that produced this License.  Never null. */
    public Document asDocument() {
        return licenseDoc;
    }

    /**
     * Check the validity of this license.  If this method returns, the license was signed by a trusted license
     * issuer and has not expired.
     *
     * @throws LicenseException if the license was not signed by a trusted license issuer, has expired, or is not yet valid
     */
    public void checkValidity() throws LicenseException {
        if (!isValidSignature())
            throw new LicenseException("License " + id + " was not signed by a trusted license issuer");
        long now = System.currentTimeMillis();
        if (now < startDateUt)
            throw new LicenseException("License " + id + " is not yet valid: becomes valid on " + startDate);
        if (now > expiryDateUt)
            throw new LicenseException("License " + id + " has expired: expired on " + expiryDate);

        // Ok looks good
        return;
    }

    /**
     * Check if the current license would enable access to the given feature if it were valid.
     * <p>
     * Notes: For performance, this method does not check the validity of this license.  It is assumed that the
     * caller has already checked this before querying for individual features.
     *
     * @param name
     * @return true iff. this feature is enabled by this license.
     */
    public boolean isFeatureEnabled(String name) {
        // Currently there is no feature-granular control -- all features are enabled by any valid license.
        return true;
    }

    public String toString() {
        return String.valueOf(id) + " (" + getLicenseeName() + ")";
    }

    /** @noinspection RedundantIfStatement*/
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final License license = (License)o;

        if (expiryDateUt != license.expiryDateUt) return false;
        if (id != license.id) return false;
        if (startDateUt != license.startDateUt) return false;
        if (validSignature != license.validSignature) return false;
        if (description != null ? !description.equals(license.description) : license.description != null) return false;
        if (expiryDate != null ? !expiryDate.equals(license.expiryDate) : license.expiryDate != null) return false;
        if (hostname != null ? !hostname.equals(license.hostname) : license.hostname != null) return false;
        if (ip != null ? !ip.equals(license.ip) : license.ip != null) return false;
        if (licenseeContactEmail != null ? !licenseeContactEmail.equals(license.licenseeContactEmail) : license.licenseeContactEmail != null) return false;
        if (licenseeName != null ? !licenseeName.equals(license.licenseeName) : license.licenseeName != null) return false;
        if (product != null ? !product.equals(license.product) : license.product != null) return false;
        if (startDate != null ? !startDate.equals(license.startDate) : license.startDate != null) return false;
        if (trustedIssuer != null ? !CertUtils.certsAreEqual(trustedIssuer, license.trustedIssuer) : license.trustedIssuer != null) return false;
        if (versionMajor != null ? !versionMajor.equals(license.versionMajor) : license.versionMajor != null) return false;
        if (versionMinor != null ? !versionMinor.equals(license.versionMinor) : license.versionMinor != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (int)(id ^ (id >>> 32));
        result = 29 * result + (validSignature ? 1 : 0);
        try {
            result = 29 * result + (trustedIssuer != null ? Arrays.hashCode(trustedIssuer.getEncoded()) : 0);
        } catch (CertificateEncodingException e) {
            // Can't happen, but if it does, we'll ignore it and allow the hash code to differ which should cause an update
        }
        result = 29 * result + (startDate != null ? startDate.hashCode() : 0);
        result = 29 * result + (int)(startDateUt ^ (startDateUt >>> 32));
        result = 29 * result + (expiryDate != null ? expiryDate.hashCode() : 0);
        result = 29 * result + (int)(expiryDateUt ^ (expiryDateUt >>> 32));
        result = 29 * result + (description != null ? description.hashCode() : 0);
        result = 29 * result + (hostname != null ? hostname.hashCode() : 0);
        result = 29 * result + (ip != null ? ip.hashCode() : 0);
        result = 29 * result + (licenseeName != null ? licenseeName.hashCode() : 0);
        result = 29 * result + (licenseeContactEmail != null ? licenseeContactEmail.hashCode() : 0);
        result = 29 * result + (product != null ? product.hashCode() : 0);
        result = 29 * result + (versionMajor != null ? versionMajor.hashCode() : 0);
        result = 29 * result + (versionMinor != null ? versionMinor.hashCode() : 0);
        return result;
    }

}
