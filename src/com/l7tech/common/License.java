/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common;

import com.l7tech.common.security.xml.DsigUtil;
import com.l7tech.common.security.xml.SimpleCertificateResolver;
import com.l7tech.common.util.*;
import com.l7tech.common.xml.TooManyChildElementsException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.Serializable;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;

/**
 * Immutable in-memory representation of a License file.
 */
public final class License implements Serializable {
    public static final String LIC_NS = "http://l7tech.com/license";
    private final String licenseXml;
    private final long id;
    private final boolean validSignature;
    private final X509Certificate trustedIssuer;
    private final Date startDate;
    private final long startDateUt;
    private final Date expiryDate;
    private final long expiryDateUt;
    private final String description;
    private final String licenseeName;
    private final String licenseeContactEmail;

    // Grant information -- details of how grants are expressed and stored is not exposed in the License interface.
    private final LicenseGrants g;

    /**
     * Store the license grants.  The format of grants will change in the future.
     * This is only for the use of the license generation GUI.  Other code should just use the query methods
     * on License to see if what they want to do is permitted by the license.
     *
     */
    public static final class LicenseGrants implements Serializable {
        final String hostname;
        final String ip;
        final String product;
        final String versionMajor;
        final String versionMinor;

        public LicenseGrants(String hostname, String ip, String product, String versionMajor, String versionMinor) {
            this.hostname = hostname;
            this.ip = ip;
            this.product = product;
            this.versionMajor = versionMajor;
            this.versionMinor = versionMinor;
        }

        public String getHostname() {
            return hostname;
        }

        public String getIp() {
            return ip;
        }

        public String getProduct() {
            return product;
        }

        public String getVersionMajor() {
            return versionMajor;
        }

        public String getVersionMinor() {
            return versionMinor;
        }

        /** Get a string description of the grants. */
        public String asEnglish() {
            final StringBuffer sb = new StringBuffer();

            final boolean anyIp = "*".equals(ip);
            final boolean anyHost = "*".equals(hostname);
            final boolean anyMajor = "*".equals(versionMajor);
            final boolean anyMinor = "*".equals(versionMinor);

            sb.append("Use of ");
            sb.append(anyMajor && anyMinor ? "any version of " : "");
            if ("*".equals(product)) {
                sb.append("any product that accepts this license");
            } else {
                sb.append(product);

                if (anyMajor == anyMinor) {
                    if (!anyMajor)
                        sb.append(" version ").append(versionMajor).append(".").append(versionMinor);
                } else if (!anyMinor) {
                    sb.append(" minor version ").append(versionMinor);
                } else {
                    sb.append(" major version ").append(versionMajor);
                }
            }

            if (!anyIp)
                sb.append(", with the IP address ").append(ip);
            if (!anyHost)
                sb.append(anyIp ? ", with" : " and").append(" the host name ").append(hostname);

            return sb.toString();
        }

        /** @noinspection RedundantIfStatement*/
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final LicenseGrants that = (LicenseGrants)o;

            if (hostname != null ? !hostname.equals(that.hostname) : that.hostname != null) return false;
            if (ip != null ? !ip.equals(that.ip) : that.ip != null) return false;
            if (product != null ? !product.equals(that.product) : that.product != null) return false;
            if (versionMajor != null ? !versionMajor.equals(that.versionMajor) : that.versionMajor != null) return false;
            if (versionMinor != null ? !versionMinor.equals(that.versionMinor) : that.versionMinor != null) return false;

            return true;
        }

        public int hashCode() {
            int result;
            result = (hostname != null ? hostname.hashCode() : 0);
            result = 29 * result + (ip != null ? ip.hashCode() : 0);
            result = 29 * result + (product != null ? product.hashCode() : 0);
            result = 29 * result + (versionMajor != null ? versionMajor.hashCode() : 0);
            result = 29 * result + (versionMinor != null ? versionMinor.hashCode() : 0);
            return result;
        }
    }

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
     * @throws InvalidLicenseException if the license is invalid for immediately-obvious semantic reasons
     */
    public License(String licenseXml, X509Certificate[] trustedIssuers)
            throws SAXException, ParseException, TooManyChildElementsException, SignatureException, InvalidLicenseException {
        if (licenseXml == null) throw new NullPointerException();
        Document ld = XmlUtil.stringToDocument(licenseXml);
        XmlUtil.stripWhitespace(ld.getDocumentElement());

        Element lic = ld.getDocumentElement();
        if (lic == null) throw new NullPointerException(); // can't happen
        if (!(LIC_NS.equals(lic.getNamespaceURI()))) throw new InvalidLicenseException("License document element not in namespace " + LIC_NS);
        if (!("license".equals(lic.getLocalName()))) throw new InvalidLicenseException("License local name is not \"license\"");
        this.licenseXml = licenseXml;

        try {
            id = Long.parseLong(lic.getAttribute("Id"));
            if (id < 1)
                throw new InvalidLicenseException("License id is non-positive");
        } catch (NumberFormatException e) {
            throw new InvalidLicenseException("License id is missing or non-numeric");
        }

        startDate = parseDateElement(ld, "valid");
        startDateUt = startDate != null ? startDate.getTime() : Long.MIN_VALUE;
        expiryDate = parseDateElement(ld, "expires");
        expiryDateUt = expiryDate != null ? expiryDate.getTime() : Long.MAX_VALUE;
        final String desc = parseWildcardStringElement(ld, "description");
        description = "*".equals(desc) ? null : desc;
        String hostname = parseWildcardStringAttribute(ld, "host", "name");
        String ip = parseWildcardStringAttribute(ld, "ip", "address");
        String product = parseWildcardStringAttribute(ld, "product", "name");
        String versionMajor = parseWildcardStringAttribute(ld, "product", "version", "major");
        String versionMinor = parseWildcardStringAttribute(ld, "product", "version", "minor");
        this.g = new LicenseGrants(hostname, ip, product, versionMajor, versionMinor);
        licenseeName = parseWildcardStringAttribute(ld, "licensee", "name");
        requireValue("licensee name", licenseeName);
        final String cemail = parseWildcardStringAttribute(ld, "licensee", "contactEmail");
        licenseeContactEmail = "*".equals(cemail) ? null : cemail;

        // Look for valid signature by trusted issuer
        Element signature = XmlUtil.findOnlyOneChildElementByName(lic, SoapUtil.DIGSIG_URI, "Signature");
        if (signature != null && trustedIssuers != null) {
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
            // No signature, or not prepared to check one
            validSignature = false;
            trustedIssuer = null;
        }

    }

    private void requireValue(String name, String val) throws InvalidLicenseException {
        if (val == null || val.length() < 1 || "*".equals(val))
            throw new InvalidLicenseException("License does not specify the required value " + name);
    }

    /**
     * Parse a simple String out of a top-level element of this license.
     *
     * @param elementName  name of the element to search for.  Must be an immediate child of the license root element.
     * @return the string parsed out of this element, or "*" if the element wasn't found or it was empty.
     * @throws com.l7tech.common.xml.TooManyChildElementsException if there is more than one element with this name.
     */
    private String parseWildcardStringElement(Document licenseDoc, String elementName) throws TooManyChildElementsException {
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
     * @throws com.l7tech.common.xml.TooManyChildElementsException if there is more than one element with this name.
     */
    private String parseWildcardStringAttribute(Document licenseDoc, String elementName, String attributeName) throws TooManyChildElementsException {
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
     * @throws com.l7tech.common.xml.TooManyChildElementsException if there is more than one element with this name.
     */
    private String parseWildcardStringAttribute(Document licenseDoc, String nameTopEl, String name2ndEl, String attributeName) throws TooManyChildElementsException {
        Element lic = licenseDoc.getDocumentElement();
        Element telm = XmlUtil.findOnlyOneChildElementByName(lic, lic.getNamespaceURI(), nameTopEl);
        if (telm == null)
            return "*";
        Element elm = XmlUtil.findOnlyOneChildElementByName(telm, lic.getNamespaceURI(), name2ndEl);
        if (elm == null)
            return "*";
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
     * @throws java.text.ParseException if there is a date but it is invalid.
     * @throws com.l7tech.common.xml.TooManyChildElementsException if there is more than one element with this name.
     */
    private Date parseDateElement(Document licenseDoc, String elementName) throws ParseException, TooManyChildElementsException {
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
     * Check if this license was signed by a trusted issuer and that the signature was valid.
     *
     * @return true iff. this license was signed by a certificate that is trusted to issue licenses to us.
     */
    public boolean isValidSignature() {
        return validSignature;
    }

    /** @return the licensee contact email address, or null if the license didn't contain one. */
    public String getLicenseeContactEmail() {
        return licenseeContactEmail;
    }

    /** @return the licensee name.  Never null or empty. */
    public String getLicenseeName() {
        return licenseeName;
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
    public String asXml() {
        return licenseXml;
    }

    /**
     * Get a human-readable multiline textual summary of the grants offered by this license.
     * <p>
     * Notes: For performance, this method does not check the validity of this license.  It is assumed that the
     * caller has already checked this before querying for human-readable grant information.
     *
     * @return human-readable summary of the grants offered by this license.  Never null.
     */
    public String getGrants() {
        return g.asEnglish();
    }


    /**
     * Check the validity of this license, including signature and expiration.
     * If this method returns, the license was signed by a trusted license
     * issuer and has not expired.
     *
     * @throws InvalidLicenseException if the license was not signed by a trusted license issuer, has expired, or is not yet valid
     */
    public void checkValidity() throws InvalidLicenseException {
        if (!isValidSignature())
            throw new InvalidLicenseException("License " + id + " was not signed by a trusted license issuer");
        long now = System.currentTimeMillis();
        if (now < startDateUt)
            throw new InvalidLicenseException("License " + id + " is not yet valid: becomes valid on " +
                    startDate + " (" + DateUtils.makeRelativeDateMessage(startDate, false) + ")");
        if (now > expiryDateUt)
            throw new InvalidLicenseException("License " + id + " has expired: expired on " +
                    expiryDate + " (" + DateUtils.makeRelativeDateMessage(expiryDate, false) + ")");

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

    /**
     * Check if this license allows use of the specified product and version.
     * <p>
     * Notes: For performance, this method does not check the validity of this license.  It is assumed that the
     * caller has already checked this before querying for individual products.
     *
     * @param wantProduct  the product that should be allowed.
     * @param wantMajorVersion the major version of the product we are checking.
     * @param wantMinorVersion the minor version of the product we are checking.
     * @return true iff. this license grants access to this product
     */
    public boolean isProductEnabled(String wantProduct, String wantMajorVersion, String wantMinorVersion) {
        if ("*".equals(g.product))
            return true;
        if (!wantProduct.equals(g.product))
            return false;

        if (!("*".equals(g.versionMajor))) {
            if (!wantMajorVersion.equals(g.versionMajor))
                return false;
        }

        if (!("*".equals(g.versionMinor))) {
            if (!wantMinorVersion.equals(g.versionMinor))
                return false;
        }

        return true;
    }

    /**
     * Check if this license allows use of the specified host IP address.
     * <p>
     * Notes: For performance, this method does not check the validity of this license.  It is assumed that the
     * caller has already checked this before querying for individual IP addresses.
     *
     * @param wantIp  the IP that we want to allow.
     * @return true iff. this IP is allowed by this license.
     */
    public boolean isIpEnabled(String wantIp) {
        if ("*".equals(g.ip))
            return true;
        return wantIp.equals(g.ip);
    }

    /**
     * Check if this license allows use of the specified hostname.
     * <p>
     * Notes: For performance, this method does not check the validity of this license.  It is assumed that the
     * caller has already checked this before querying for individual hostnames.
     *
     * @param wantHostname  the hostname that we seek to authorize.
     * @return true iff. this hostname is allowed by this license.
     */
    public boolean isHostnameEnabled(String wantHostname) {
        if ("*".equals(g.hostname))
            return true;
        return wantHostname.equals(g.hostname);
    }

    public String toString() {
        return String.valueOf(id) + " (" + getLicenseeName() + ")";
    }

    /**
     * For internal use only (license maker GUI) -- do not call this method anywhere else.
     */
    public Object getSpec() {
        return g;
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
        if (g != null ? !g.equals(license.g) : license.g != null) return false;
        if (licenseeContactEmail != null ? !licenseeContactEmail.equals(license.licenseeContactEmail) : license.licenseeContactEmail != null) return false;
        if (licenseeName != null ? !licenseeName.equals(license.licenseeName) : license.licenseeName != null) return false;
        if (trustedIssuer != null ? !CertUtils.certsAreEqual(trustedIssuer, license.trustedIssuer) : license.trustedIssuer != null) return false;

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
        result = 29 * result + (int)(startDateUt ^ (startDateUt >>> 32));
        result = 29 * result + (int)(expiryDateUt ^ (expiryDateUt >>> 32));
        result = 29 * result + (description != null ? description.hashCode() : 0);
        result = 29 * result + (licenseeName != null ? licenseeName.hashCode() : 0);
        result = 29 * result + (licenseeContactEmail != null ? licenseeContactEmail.hashCode() : 0);
        result = 29 * result + (g != null ? g.hashCode() : 0);
        return result;
    }
}
