/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.gateway.common;

import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.licensing.FeatureSetExpander;
import com.l7tech.security.xml.DsigUtil;
import com.l7tech.security.xml.SimpleSecurityTokenResolver;
import com.l7tech.util.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.Serializable;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.*;

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
    private final String eulaText;
    private final Set<String> allEnabledFeatures;
    private final Set<String> attributes;

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
        final String[] rootFeatureSetNames;
        final String featureLabel;

        public LicenseGrants(String hostname, String ip, String product, String versionMajor, String versionMinor,
                             String[] rootFeatureSetNames, String featureLabel)
        {
            this.hostname = hostname;
            this.ip = ip;
            this.product = product;
            this.versionMajor = versionMajor;
            this.versionMinor = versionMinor;
            this.rootFeatureSetNames = rootFeatureSetNames;
            this.featureLabel = featureLabel;
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

        public String[] getRootFeatureSetNames() {
            return rootFeatureSetNames;
        }

        public String getFeatureLabel() {
            return featureLabel;
        }

        /** @return a string description of the grants. */
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

            if (rootFeatureSetNames.length > 0) {
                sb.append(" featuring ");

                if (featureLabel != null)
                    sb.append(featureLabel);
                else
                    describeFeatures(sb);
            }

            return sb.toString();
        }

        private void describeFeatures(StringBuffer sb) {
            String setprof = "set:Profile:";
            int setproflen = setprof.length();
            for (int i = 0; i < rootFeatureSetNames.length; i++) {
                String name = rootFeatureSetNames[i];
                if (i > 0) {
                    if (i == rootFeatureSetNames.length - 1) sb.append(" and ");
                    else sb.append(", ");
                }

                // TODO look up the human-readable marketing name rather than attempting to make one up here
                if (name.startsWith(setprof) && name.length() > setproflen) {
                    sb.append("SecureSpan ");
                    name = name.substring(setproflen);
                    if (name.equals("Accel")) name = "Acceleration";
                    else if (name.equals("IPS")) name = "XML IPS";
                    else if (name.equals("PolicyIntegrationPoint")) name ="Policy Integration Point";
                    sb.append(name);
                } else {
                    sb.append(name);
                }
            }
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
            if (!Arrays.equals(rootFeatureSetNames, that.rootFeatureSetNames)) return false;

            return true;
        }

        public int hashCode() {
            int result;
            result = (hostname != null ? hostname.hashCode() : 0);
            result = 29 * result + (ip != null ? ip.hashCode() : 0);
            result = 29 * result + (product != null ? product.hashCode() : 0);
            result = 29 * result + (versionMajor != null ? versionMajor.hashCode() : 0);
            result = 29 * result + (versionMinor != null ? versionMinor.hashCode() : 0);
            result = 29 * result + Arrays.hashCode(rootFeatureSetNames);
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
     *             featureset name=set:Profile:Whatever
     *             featureset name=set:Profile:Somethingelse
     *             featureset name=assertion:Extra
     *         licensee contactEmail="whatever@wherever"(optional) name="Organization Name"
     *         ds:Signature (optional, assuming license is signed)
     *             ds:SignedInfo ... (signed info must cover the license root element)
     *             ds:SignatureValue ...
     *             ds:KeyInfo
     *                 KeyName("CN=SSGL1, O=L7Tech, OU=Licensing") (must match DN from a trusted issuer cert)
     * </pre>
     *
     * @param licenseXml     a String containing the License as XML data.
     * @param trustedIssuers license signing certificates that whose signatures will be considered valid.  If null,
     *                       no license signatures will be considered valid.
     * @param featureSetExpander  a {@link FeatureSetExpander} to explode out the
     *                            complete set of leaf features given the possibly-more-abstract feature set names
     *                            explicitly listed in the License. If null, only the features named explicitly in the
     *                            license will be considered enabled.
     * @throws SAXException if the licenseXml is not well-formed XML
     * @throws ParseException if one of the fields of the license contains illegally-formatted data
     * @throws TooManyChildElementsException if there is more than one copy of an element that there can be only one of (ie, expires, Signature, etc)
     * @throws SignatureException if the license is signed, but the signature wasn't valid or wasn't made by a trusted licence issuer
     * @throws InvalidLicenseException if the license is invalid for immediately-obvious semantic reasons
     */
    public License(String licenseXml, X509Certificate[] trustedIssuers, FeatureSetExpander featureSetExpander)
            throws SAXException, ParseException, TooManyChildElementsException, SignatureException, InvalidLicenseException {
        if (licenseXml == null) throw new NullPointerException("licenseXml must not be null");
        Document ld = XmlUtil.stringToDocument(licenseXml);
        DomUtils.stripWhitespace(ld.getDocumentElement());

        Element lic = ld.getDocumentElement();
        if (lic == null) throw new NullPointerException(); // can't happen
        if (!(LIC_NS.equals(lic.getNamespaceURI()))) throw new InvalidLicenseException("License document element not in namespace " + LIC_NS);
        if (!("license".equals(lic.getLocalName()))) throw new InvalidLicenseException("License local name is not \"license\"");
        this.licenseXml = licenseXml;

        try {
            id = Long.parseLong(lic.getAttribute("Id"));
            if (id < 1)
                throw new InvalidLicenseException("License Id is non-positive");
        } catch (NumberFormatException e) {
            throw new InvalidLicenseException("License Id is missing or non-numeric");
        }

        startDate = parseDateElement(ld, "valid");
        startDateUt = startDate != null ? startDate.getTime() : Long.MIN_VALUE;
        expiryDate = parseDateElement(ld, "expires");
        expiryDateUt = expiryDate != null ? expiryDate.getTime() : Long.MAX_VALUE;
        final String desc = parseWildcardStringElement(ld, "description");
        description = "*".equals(desc) ? null : desc;
        attributes = Collections.unmodifiableSet(parseWildcardStringLicenseAttributes(ld, "licenseAttributes", "attribute"));
        String hostname = parseWildcardStringAttribute(ld, "host", "name");
        String ip = parseWildcardStringAttribute(ld, "ip", "address");
        String product = parseWildcardStringAttribute(ld, "product", "name");
        String versionMajor = parseWildcardStringAttribute(ld, "product", "version", "major");
        String versionMinor = parseWildcardStringAttribute(ld, "product", "version", "minor");
        String featureLabel = parseSimpleText(ld, "featureLabel");

        this.eulaText = parseEulaText(ld);

        Set<String> featureSets = new HashSet<>();
        collectFeatureSets(ld, "product", "featureset", featureSets);
        allEnabledFeatures = Collections.unmodifiableSet(featureSetExpander == null ? featureSets : featureSetExpander.getAllEnabledFeatures(featureSets));

        //noinspection unchecked
        this.g = new LicenseGrants(hostname, ip, product, versionMajor, versionMinor, featureSets.toArray(new String[featureSets.size()]), featureLabel);
        licenseeName = parseWildcardStringAttribute(ld, "licensee", "name");
        requireValue("licensee name", licenseeName);
        final String cemail = parseWildcardStringAttribute(ld, "licensee", "contactEmail");
        licenseeContactEmail = "*".equals(cemail) ? null : cemail;

        // Look for valid signature by trusted issuer
        Element signature = DomUtils.findOnlyOneChildElementByName(lic, DsigUtil.DIGSIG_URI, "Signature");
        if (signature != null && trustedIssuers != null) {
            // See if it is valid and if we trust it
            final X509Certificate gotCert;
            try {
                gotCert = DsigUtil.checkSimpleEnvelopedSignature(signature, new SimpleSecurityTokenResolver(trustedIssuers));
            } catch (CertificateEncodingException e) {
                throw new SignatureException("Unable to encode trusted issuer certificate: " + ExceptionUtils.getMessage(e), e);
            }
            X509Certificate foundTrustedIssuer = null;
            //noinspection ForLoopReplaceableByForEach
            for (int i = 0; i < trustedIssuers.length; i++) {
                X509Certificate issuer = trustedIssuers[i];
                if ( CertUtils.certsAreEqual(issuer, gotCert)) {
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

    /**
     * Parse the document and find all license attributes.
     * @param licenseDoc: a document containing all license information
     * @param parentElemName: refer to the element name, "licenseAttributes".
     * @param childElemName: refers to the sub-element name, "attribute".
     * @return all license attribute name.
     * @throws TooManyChildElementsException
     */
    private Set<String> parseWildcardStringLicenseAttributes(Document licenseDoc, String parentElemName, String childElemName) throws TooManyChildElementsException {
        Set<String> attrList = new HashSet<>();
        Element lic = licenseDoc.getDocumentElement();
        Element licAttrs = DomUtils.findOnlyOneChildElementByName(lic, lic.getNamespaceURI(), parentElemName);
        if (licAttrs == null) {
            return attrList;
        }
        List<Element> elems = DomUtils.findChildElementsByName(licAttrs, licAttrs.getNamespaceURI(), childElemName);
        for (Element elem: elems) {
            if (elem != null) {
                attrList.add(DomUtils.getTextValue(elem));
            }
        }
        return attrList;
    }

    public Set<String> getAttributes() {
        return attributes;
    }

    private String parseEulaText(Document ld) throws TooManyChildElementsException, InvalidLicenseException {
        String eulaText = parseWildcardStringElement(ld, "eulatext");
        if ("*".equals(eulaText)) eulaText = null;
        if (eulaText != null && eulaText.trim().length() > 0) try {
            eulaText = new String( IOUtils.uncompressGzip(HexUtils.decodeBase64(eulaText.trim(), true)), Charsets.UTF8);
        } catch (IOException e) {
            throw new InvalidLicenseException("unable to decode eulatext contents", e);
        }
        eulaText = eulaText != null && eulaText.length() > 0 ? eulaText : null;
        return eulaText;
    }

    private String parseSimpleText(Document ld, String elementName) throws TooManyChildElementsException {
        Element lic = ld.getDocumentElement();
        Element elm = DomUtils.findOnlyOneChildElementByName(lic, lic.getNamespaceURI(), elementName);
        if (elm == null)
            return null;
        String val = DomUtils.getTextValue(elm);
        if (val == null || val.length() < 1)
            return null;
        return val;
    }

    /**
     * Collect all feature sets enabled in the specified license document.
     *
     * @param ld  the license document.  Must not be null.  Must be a valid license document.
     * @param nameTopEl  name of the top-level element to search for.  Must be an immediate child of the license root element.
     * @param name2ndEl  name of the 2nd-level element to search for.  Must be an immediate child of nameTopEl.
     * @param featureSets  the set to add them to.  Must not be null.
     *
     * @return the number of feature set names that were added to the set.  May be zero if none were found.
     *
     * @throws InvalidLicenseException        if the document format is incorrect
     * @throws TooManyChildElementsException  if the document format is incorrect
     */
    private int collectFeatureSets(Document ld, String nameTopEl, String name2ndEl, Set featureSets)
            throws InvalidLicenseException, TooManyChildElementsException
    {
        Element lic = ld.getDocumentElement();
        Element telm = DomUtils.findOnlyOneChildElementByName(lic, lic.getNamespaceURI(), nameTopEl);
        if (telm == null) throw new InvalidLicenseException("License contains no product element");

        List elms = DomUtils.findChildElementsByName(telm, lic.getNamespaceURI(), name2ndEl);
        if (elms == null || elms.isEmpty())
            return 0; // No features enabled -- FeatureSetExpander should enable backwards-compat-mode

        int added = 0;
        //noinspection ForLoopReplaceableByForEach
        for (Iterator i = elms.iterator(); i.hasNext();) {
            Element element = (Element)i.next();
            String featureSetName = element.getAttribute("name");
            if (featureSetName == null || featureSetName.trim().length() < 1)
                throw new InvalidLicenseException("License contains feature set with no name");
            //noinspection unchecked
            featureSets.add(featureSetName);
            ++added;
        }

        return added;
    }

    private void requireValue(String name, String val) throws InvalidLicenseException {
        if (val == null || val.length() < 1 || "*".equals(val))
            throw new InvalidLicenseException("License does not specify the required value '" + name + "'");
    }

    /**
     * Parse a simple String out of a top-level element of this license.
     *
     * @param licenseDoc  the license DOM Document.  Required.
     * @param elementName  name of the element to search for.  Must be an immediate child of the license root element.
     * @return the string parsed out of this element, or "*" if the element wasn't found or it was empty.
     * @throws com.l7tech.util.TooManyChildElementsException if there is more than one element with this name.
     */
    private String parseWildcardStringElement(Document licenseDoc, String elementName) throws TooManyChildElementsException {
        Element lic = licenseDoc.getDocumentElement();
        Element elm = DomUtils.findOnlyOneChildElementByName(lic, lic.getNamespaceURI(), elementName);
        if (elm == null)
            return "*";
        String val = DomUtils.getTextValue(elm);
        if (val == null || val.length() < 1)
            return "*";
        return val;
    }

    /**
     * Parse a simple String out of an attribute of a top-level element of this license.
     *
     * @param licenseDoc  the license DOM Document.  Required.
     * @param elementName  name of the element to search for.  Must be an immediate child of the license root element.
     * @param attributeName name of the attribute of this element to snag.  Must not be in any namespace.
     * @return the string parsed out of this element, or "*" if the element wasn't found or it was empty.
     * @throws com.l7tech.util.TooManyChildElementsException if there is more than one element with this name.
     */
    private String parseWildcardStringAttribute(Document licenseDoc, String elementName, String attributeName) throws TooManyChildElementsException {
        Element lic = licenseDoc.getDocumentElement();
        Element elm = DomUtils.findOnlyOneChildElementByName(lic, lic.getNamespaceURI(), elementName);
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
     * @param licenseDoc  the license DOM Document.  Required.
     * @param nameTopEl  name of the top-level element to search for.  Must be an immediate child of the license root element.
     * @param name2ndEl  name of the 2nd-level element to search for.  Must be an immediate child of nameTopEl.
     * @param attributeName name of the attribute of this element to snag.  Must not be in any namespace.
     * @return the string parsed out of this element, or "*" if the element wasn't found or it was empty.
     * @throws com.l7tech.util.TooManyChildElementsException if there is more than one element with this name.
     */
    private String parseWildcardStringAttribute(Document licenseDoc, String nameTopEl, String name2ndEl, String attributeName) throws TooManyChildElementsException {
        Element lic = licenseDoc.getDocumentElement();
        Element telm = DomUtils.findOnlyOneChildElementByName(lic, lic.getNamespaceURI(), nameTopEl);
        if (telm == null)
            return "*";
        Element elm = DomUtils.findOnlyOneChildElementByName(telm, lic.getNamespaceURI(), name2ndEl);
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
     * @param licenseDoc  the license DOM Document.  Required.
     * @param elementName name of the element to search for.  Must be an immediate child of the license root element.
     * @return the date parsed out of this element, or null if the element was not found.
     * @throws java.text.ParseException if there is a date but it is invalid.
     * @throws com.l7tech.util.TooManyChildElementsException if there is more than one element with this name.
     */
    private Date parseDateElement(Document licenseDoc, String elementName) throws ParseException, TooManyChildElementsException {
        Element lic = licenseDoc.getDocumentElement();
        Element elm = DomUtils.findOnlyOneChildElementByName(lic, lic.getNamespaceURI(), elementName);
        if (elm == null)
            return null;
        return ISO8601Date.parse(DomUtils.getTextValue(elm));
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

    /**
     * @return custom EULA text to display when this license is installed, or null if no custom EULA was provided.
     */
    public String getEulaText() {
        return eulaText;
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
    }

    /**
     * Check if the current license would enable access to the given feature if it were valid.
     * <p>
     * Notes: For performance, this method does not check the validity of this license.  It is assumed that the
     * caller has already checked this before querying for individual features.
     *
     * @param name  the name of the feature.  Must not be null or empty.
     * @return true iff. this feature is enabled by this license.
     */
    public boolean isFeatureEnabled(String name) {
        return allEnabledFeatures.contains(name);
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
        return "*".equals(g.ip) || wantIp.equals(g.ip);
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
        return "*".equals(g.hostname) || wantHostname.equals(g.hostname);
    }

    public String toString() {
        return String.valueOf(id) + " (" + getLicenseeName() + ")";
    }

    /**
     * For internal use only (license maker GUI) -- do not call this method anywhere else.
     * @return the opaque Spec object.
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
        if (eulaText != null ? !eulaText.equals(license.eulaText) : license.eulaText != null) return false;

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
        result = 29 * result + (eulaText != null ? eulaText.hashCode() : 0);
        return result;
    }
}
