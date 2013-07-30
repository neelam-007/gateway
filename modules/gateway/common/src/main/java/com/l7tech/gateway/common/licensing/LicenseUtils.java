package com.l7tech.gateway.common.licensing;

import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.InvalidLicenseException;
import com.l7tech.security.xml.DsigUtil;
import com.l7tech.security.xml.SimpleSecurityTokenResolver;
import com.l7tech.util.*;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Jamie Williams - wilja33 - jamie.williams2@ca.com
 */
public class LicenseUtils {
    public static final String LICENSE_NAMESPACE = "http://l7tech.com/license";
    public static final String DOCUMENT_ELEMENT_LOCAL_NAME = "license";
    public static final String ID_ATTRIBUTE = "Id";
    public static final String START_DATE_ELEMENT = "valid";
    public static final String EXPIRY_DATE_ELEMENT = "expires";
    public static final String DESCRIPTION_ELEMENT = "description";
    public static final String LICENSE_ATTRIBUTES_ELEMENT = "licenseAttributes";
    public static final String ATTRIBUTE_ELEMENT = "attribute";
    public static final String HOST_ELEMENT = "host";
    public static final String NAME_ATTRIBUTE = "name";
    public static final String PRODUCT_ELEMENT = "product";
    public static final String VERSION_ELEMENT = "version";
    public static final String MAJOR_ATTRIBUTE = "major";
    public static final String MINOR_ATTRIBUTE = "minor";
    public static final String ADDRESS_ATTRIBUTE = "address";
    public static final String IP_ELEMENT = "ip";
    public static final String FEATURE_LABEL_ELEMENT = "featureLabel";
    public static final String LICENSEE_ELEMENT = "licensee";
    public static final String CONTACT_EMAIL_ATTRIBUTE = "contactEmail";
    public static final String FEATURESET_ELEMENT = "featureset";
    public static final String SIGNATURE_ELEMENT = "Signature";
    public static final String EULATEXT_ELEMENT = "eulatext";

    // error messages
    public static final String LICENSE_XML_INVALID_ERROR_MSG = "License XML invalid.";
    public static final String START_DATE_PARSING_ERROR_MSG = "Could not parse license start date.";
    public static final String EXPIRY_DATE_PARSING_ERROR_MSG = "Could not parse license expiry date.";
    public static final String ID_FORMAT_ERROR_MSG = "License Id is missing or non-numeric.";
    public static final String ID_NON_POSITIVE_ERROR_MSG = "License Id is non-positive.";
    public static final String LICENSE_EMPTY_ERROR_MSG = "License is empty.";
    public static final String EULA_PARSING_ERROR_MSG = "Unable to read EULA text.";
    public static final String MISSING_REQUIRED_INFORMATION_ERROR_MSG = "License is missing required information.";
    public static final String SIGNER_NOT_RECOGNIZED_ERROR_MSG = "The license signer is not recognized as a trusted license issuer.";
    public static final String SIGNATURE_VALIDATION_ERROR_MSG = "Signature could not be validated.";
    public static final String ISSUER_CERTIFICATE_ENCODING_ERROR_MSG = "Unable to encode trusted issuer certificate.";

    public static final String ASTERISK = "*";

    public static String getLicenseeEmail(Document doc) throws TooManyChildElementsException {
        String licenseeEmail = parseWildcardStringAttribute(doc, LICENSEE_ELEMENT, CONTACT_EMAIL_ATTRIBUTE);
        return "*".equals(licenseeEmail) ? null : licenseeEmail;
    }

    public static String getLicenseeName(Document doc) throws TooManyChildElementsException, InvalidLicenseException {
        String licenseeName = parseWildcardStringAttribute(doc, LICENSEE_ELEMENT, NAME_ATTRIBUTE);
        requireValue(licenseeName);
        return licenseeName;
    }

    public static String getProductName(Document doc) throws TooManyChildElementsException {
        return parseWildcardStringAttribute(doc, PRODUCT_ELEMENT, NAME_ATTRIBUTE);
    }

    public static String getIpAddress(Document doc) throws TooManyChildElementsException {
        return parseWildcardStringAttribute(doc, IP_ELEMENT, ADDRESS_ATTRIBUTE);
    }

    public static String getHostname(Document doc) throws TooManyChildElementsException {
        return parseWildcardStringAttribute(doc, HOST_ELEMENT, NAME_ATTRIBUTE);
    }

    public static String getMinorVersionNumber(Document doc) throws TooManyChildElementsException {
        return parseWildcardStringAttribute(doc, PRODUCT_ELEMENT, VERSION_ELEMENT, MINOR_ATTRIBUTE);
    }

    public static String getMajorVersionNumber(Document doc) throws TooManyChildElementsException {
        return parseWildcardStringAttribute(doc, PRODUCT_ELEMENT, VERSION_ELEMENT, MAJOR_ATTRIBUTE);
    }

    public static String getFeatureLabel(Document doc) throws TooManyChildElementsException {
        return parseSimpleText(doc, FEATURE_LABEL_ELEMENT);
    }

    /**
     * Parse the document and find all license attributes.
     * @param doc: a document containing all license information
     * @return all license attribute name.
     * @throws com.l7tech.util.TooManyChildElementsException
     */
    public static Set<String> getAttributes(Document doc) throws TooManyChildElementsException {
        Set<String> attributes = new HashSet<>();

        Element lic = doc.getDocumentElement();
        Element licAttrs = DomUtils.findOnlyOneChildElementByName(lic, lic.getNamespaceURI(), LICENSE_ATTRIBUTES_ELEMENT);

        if (licAttrs == null) {
            return attributes;
        }

        List<Element> elems = DomUtils.findChildElementsByName(licAttrs, licAttrs.getNamespaceURI(), ATTRIBUTE_ELEMENT);

        for (Element elem: elems) {
            if (elem != null) {
                attributes.add(DomUtils.getTextValue(elem));
            }
        }

        return attributes;
    }

    public static String getDescription(Document doc) throws TooManyChildElementsException {
        String desc = parseWildcardStringElement(doc, DESCRIPTION_ELEMENT);
        return "*".equals(desc) ? null : desc;
    }

    public static Date getStartDate(Document doc) throws TooManyChildElementsException, InvalidLicenseException {
        // find and parse start date
        try {
            return parseDateElement(doc, START_DATE_ELEMENT);
        } catch (ParseException e) {
            throw new InvalidLicenseException(START_DATE_PARSING_ERROR_MSG, e);
        }
    }

    public static Date getExpiryDate(Document doc) throws TooManyChildElementsException, InvalidLicenseException {
        // find and parse expiry date
        try {
            return parseDateElement(doc, EXPIRY_DATE_ELEMENT);
        } catch (ParseException e) {
            throw new InvalidLicenseException(EXPIRY_DATE_PARSING_ERROR_MSG, e);
        }
    }

    public static long getId(Document doc) throws InvalidLicenseException {
        long id;
        // validate Id
        try {
            id = Long.parseLong(doc.getDocumentElement().getAttribute(ID_ATTRIBUTE));
        } catch (NumberFormatException e) {
            throw new InvalidLicenseException(ID_FORMAT_ERROR_MSG);
        }

        if (id < 1)
            throw new InvalidLicenseException(ID_NON_POSITIVE_ERROR_MSG);

        return id;
    }

    public static Document parseLicenseDocument(String licenseXml) throws InvalidLicenseException {
        if (licenseXml.isEmpty()) {
            throw new InvalidLicenseException(LICENSE_EMPTY_ERROR_MSG);
        }

        // parse and validate license document

        Document doc;

        try {
            doc = XmlUtil.stringToDocument(licenseXml);
            DomUtils.stripWhitespace(doc.getDocumentElement());
        } catch (SAXException e) {
            throw new InvalidLicenseException(LICENSE_XML_INVALID_ERROR_MSG, e);
        }

        Element lic = doc.getDocumentElement();

        if (!(LICENSE_NAMESPACE.equals(lic.getNamespaceURI()))) {
            throw new InvalidLicenseException(LICENSE_XML_INVALID_ERROR_MSG,
                    new InvalidDocumentFormatException("License document element not in namespace " + LICENSE_NAMESPACE));
        }

        if (!(DOCUMENT_ELEMENT_LOCAL_NAME.equals(lic.getLocalName()))) {
            throw new InvalidLicenseException(LICENSE_XML_INVALID_ERROR_MSG,
                    new InvalidDocumentFormatException("License local name is not \"" + DOCUMENT_ELEMENT_LOCAL_NAME + "\""));
        }

        return doc;
    }

    public static String parseEulaText(Document ld) throws TooManyChildElementsException, InvalidLicenseException {
        String eulaText = parseWildcardStringElement(ld, EULATEXT_ELEMENT);

        if (ASTERISK.equals(eulaText)) {
            return null;
        }

        if (eulaText != null && eulaText.trim().length() > 0) try {
            eulaText = new String(IOUtils.uncompressGzip(HexUtils.decodeBase64(eulaText.trim(), true)), Charsets.UTF8);
        } catch (IOException e) {
            throw new InvalidLicenseException(EULA_PARSING_ERROR_MSG, e);
        }

        return eulaText != null && eulaText.length() > 0 ? eulaText : null;
    }

    public static String parseSimpleText(Document ld, String elementName) throws TooManyChildElementsException {
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
     * @return the set of feature set names listed in the license
     *
     * @throws InvalidLicenseException        if the document format is incorrect
     * @throws TooManyChildElementsException  if the document format is incorrect
     */
    public static Set<String> getFeatureSets(Document ld)
            throws InvalidLicenseException, TooManyChildElementsException
    {
        Set<String> featureSets = new HashSet<>();

        Element lic = ld.getDocumentElement();
        Element product = DomUtils.findOnlyOneChildElementByName(lic, lic.getNamespaceURI(), PRODUCT_ELEMENT);

        if (product == null) {
            throw new InvalidLicenseException(LICENSE_XML_INVALID_ERROR_MSG);
        }

        List<Element> featureSetsElements = DomUtils.findChildElementsByName(product, lic.getNamespaceURI(), FEATURESET_ELEMENT);

        if (featureSetsElements == null || featureSetsElements.isEmpty()) {
            throw new InvalidLicenseException(MISSING_REQUIRED_INFORMATION_ERROR_MSG);
        }

        for (Element element : featureSetsElements) {
            String featureSetName = element.getAttribute(NAME_ATTRIBUTE);

            if (featureSetName == null || featureSetName.trim().length() < 1)
                throw new InvalidLicenseException(LICENSE_XML_INVALID_ERROR_MSG);

            featureSets.add(featureSetName);
        }

        return featureSets;
    }

    public static void requireValue(String val) throws InvalidLicenseException {
        if (val == null || val.length() < 1 || ASTERISK.equals(val)) {
            throw new InvalidLicenseException(MISSING_REQUIRED_INFORMATION_ERROR_MSG);
        }
    }

    /**
     * Parse a simple String out of a top-level element of this license.
     *
     * @param licenseDoc  the license DOM Document.  Required.
     * @param elementName  name of the element to search for.  Must be an immediate child of the license root element.
     * @return the string parsed out of this element, or "*" if the element wasn't found or it was empty.
     * @throws com.l7tech.util.TooManyChildElementsException if there is more than one element with this name.
     */
    public static String parseWildcardStringElement(Document licenseDoc, String elementName) throws TooManyChildElementsException {
        Element lic = licenseDoc.getDocumentElement();
        Element e = DomUtils.findOnlyOneChildElementByName(lic, lic.getNamespaceURI(), elementName);

        if (e == null) {
            return ASTERISK;
        }

        String val = DomUtils.getTextValue(e);

        if (val == null || val.length() < 1) {
            return ASTERISK;
        }

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
    public static String parseWildcardStringAttribute(Document licenseDoc, String elementName, String attributeName) throws TooManyChildElementsException {
        Element lic = licenseDoc.getDocumentElement();
        Element elm = DomUtils.findOnlyOneChildElementByName(lic, lic.getNamespaceURI(), elementName);
        if (elm == null)
            return ASTERISK;
        String val = elm.getAttribute(attributeName);
        if (val == null || val.length() < 1)
            return ASTERISK;
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
    public static String parseWildcardStringAttribute(Document licenseDoc, String nameTopEl, String name2ndEl, String attributeName) throws TooManyChildElementsException {
        Element lic = licenseDoc.getDocumentElement();
        Element telm = DomUtils.findOnlyOneChildElementByName(lic, lic.getNamespaceURI(), nameTopEl);
        if (telm == null)
            return ASTERISK;
        Element elm = DomUtils.findOnlyOneChildElementByName(telm, lic.getNamespaceURI(), name2ndEl);
        if (elm == null)
            return ASTERISK;
        String val = elm.getAttribute(attributeName);
        if (val == null || val.length() < 1)
            return ASTERISK;
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
    public static Date parseDateElement(Document licenseDoc, String elementName) throws ParseException, TooManyChildElementsException {
        Element lic = licenseDoc.getDocumentElement();
        Element elm = DomUtils.findOnlyOneChildElementByName(lic, lic.getNamespaceURI(), elementName);

        if (elm == null) {
            return null;
        }

        return new SimpleDateFormat(DateUtils.ISO8601_PATTERN).parse(DomUtils.getTextValue(elm));
    }

    public static X509Certificate getTrustedIssuer(@NotNull Document doc, @NotNull X509Certificate[] trustedIssuers) throws TooManyChildElementsException, InvalidLicenseException {
        // Look for valid signature by trusted issuer
        Element signature = DomUtils.findOnlyOneChildElementByName(doc.getDocumentElement(), DsigUtil.DIGSIG_URI, SIGNATURE_ELEMENT);

        if (signature == null) {
            return null;
        }

        // See if it is valid and if we trust it
        final X509Certificate signatureCert;

        try {
            signatureCert =
                    DsigUtil.checkSimpleEnvelopedSignature(signature, new SimpleSecurityTokenResolver(trustedIssuers));
        } catch (CertificateEncodingException e) {
            throw new InvalidLicenseException(ISSUER_CERTIFICATE_ENCODING_ERROR_MSG, e);
        } catch (SignatureException e) {
            throw new InvalidLicenseException(SIGNATURE_VALIDATION_ERROR_MSG, e);
        }

        for (X509Certificate trustedCert : trustedIssuers) {
            if (CertUtils.certsAreEqual(trustedCert, signatureCert)) {
                return trustedCert;
            }
        }

        throw new InvalidLicenseException(SIGNER_NOT_RECOGNIZED_ERROR_MSG);
    }

    /** @return a string description of the grants. */
    public static String getGrantsAsEnglish(FeatureLicense license) {
        final StringBuffer sb = new StringBuffer();

        final boolean anyIp = "*".equals(license.getIp());
        final boolean anyHost = "*".equals(license.getHostname());
        final boolean anyMajor = "*".equals(license.getMajorVersion());
        final boolean anyMinor = "*".equals(license.getMinorVersion());

        sb.append("Use of ");
        sb.append(anyMajor && anyMinor ? "any version of " : "");
        if ("*".equals(license.getProductName())) {
            sb.append("any product that accepts this license");
        } else {
            sb.append(license.getProductName());

            if (anyMajor == anyMinor) {
                if (!anyMajor)
                    sb.append(" version ").append(license.getMajorVersion()).append(".").append(license.getMinorVersion());
            } else if (!anyMinor) {
                sb.append(" minor version ").append(license.getMinorVersion());
            } else {
                sb.append(" major version ").append(license.getMajorVersion());
            }
        }

        if (!anyIp)
            sb.append(", with the IP address ").append(license.getIp());
        if (!anyHost)
            sb.append(anyIp ? ", with" : " and").append(" the host name ").append(license.getHostname());

        sb.append(" featuring ");

        if (license.getFeatureLabel() != null)
            sb.append(license.getFeatureLabel());
        else
            describeFeatures(license, sb);

        return sb.toString();
    }

    private static void describeFeatures(FeatureLicense license, StringBuffer sb) {
        String[] featureSets = license.getFeatureSets().toArray(new String[license.getFeatureSets().size()]);

        String setProf = "set:Profile:";

        int setProfLen = setProf.length();

        for (int i = 0; i < featureSets.length; i++) {
            String name = featureSets[i];

            if (i > 0) {
                if (i == featureSets.length - 1) sb.append(" and ");
                else sb.append(", ");
            }

            // TODO look up the human-readable marketing name rather than attempting to make one up here
            if (name.startsWith(setProf) && name.length() > setProfLen) {
                sb.append("SecureSpan ");
                name = name.substring(setProfLen);
                if (name.equals("Accel")) name = "Acceleration";
                else if (name.equals("IPS")) name = "XML IPS";
                else if (name.equals("PolicyIntegrationPoint")) name ="Policy Integration Point";
                sb.append(name);
            } else {
                sb.append(name);
            }
        }
    }
}
