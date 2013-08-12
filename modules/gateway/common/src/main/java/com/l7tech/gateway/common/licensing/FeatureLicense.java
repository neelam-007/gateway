package com.l7tech.gateway.common.licensing;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.builder.HashCodeBuilder;

import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Set;

/**
 * Immutable in-memory representation of a Feature License file.
 *
 * @author Jamie Williams - wilja33 - jamie.williams2@ca.com
 */
public class FeatureLicense implements Serializable {

    public static final String WILDCARD = "*";
    private final long id;
    private final String description;
    private final String licenseeName;
    private final String licenseeContactEmail;
    private final Date startDate;
    private final Date expiryDate;
    private final String eulaText;
    private final Set<String> attributes;
    private final Set<String> featureSets;
    private final String hostname;
    private final String ip;
    private final String productName;
    private final String majorVersion;
    private final String minorVersion;
    private final String featureLabel;
    private final X509Certificate trustedIssuer;
    private final LicenseDocument licenseDocument;

    public FeatureLicense(long id, String description, String licenseeName, String licenseeContactEmail,
                          Date startDate, Date expiryDate, String eulaText, Set<String> attributes,
                          Set<String> featureSets, String hostname, String ip, String productName,
                          String majorVersion, String minorVersion, String featureLabel,
                          X509Certificate trustedIssuer, LicenseDocument licenseDocument) {
        this.id = id;
        this.description = description;
        this.licenseeName = licenseeName;
        this.licenseeContactEmail = licenseeContactEmail;
        this.startDate = startDate;
        this.expiryDate = expiryDate;
        this.eulaText = eulaText;
        this.attributes = attributes;
        this.featureSets = featureSets;
        this.hostname = hostname;
        this.ip = ip;
        this.productName = productName;
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
        this.featureLabel = featureLabel;
        this.trustedIssuer = trustedIssuer;
        this.licenseDocument = licenseDocument;
    }

    /**
     * Check the validity of this license, including signature and expiration.
     * N.B. A license's validity is unrelated to whether it
     *
     * @return true iff the license is valid (signed by a trusted license issuer, current time within license period)
     */
    public boolean isValid() {
        return hasTrustedIssuer() && isLicensePeriodCurrent();
    }

    /**
     * Check if the license is valid for the given major and minor version numbers.
     *
     * @param majorVersion the major version number
     * @param minorVersion the minor version number
     * @return iff both the given major and minor version numbers are covered by the license
     */
    public boolean isVersionEnabled(String majorVersion, String minorVersion) {
        return (WILDCARD.equals(this.majorVersion) || this.majorVersion.equals(majorVersion)) &&
                (WILDCARD.equals(this.minorVersion) || this.minorVersion.equals(minorVersion));
    }

    public boolean isLicensePeriodCurrent() {
        return isLicensePeriodCurrent(System.currentTimeMillis());
    }

    public boolean isLicensePeriodCurrent(long time) {
        return isLicensePeriodStartBefore(time) && isLicensePeriodExpiryAfter(time);
    }

    public boolean isLicensePeriodStartBefore(long timeInMillis) {
        return timeInMillis > (null != startDate ? startDate.getTime() : Long.MIN_VALUE);
    }

    public boolean isLicensePeriodExpiryAfter(long timeInMillis) {
        return timeInMillis < (null != expiryDate ? expiryDate.getTime() : Long.MAX_VALUE);
    }

    public boolean hasTrustedIssuer() {
        return null != trustedIssuer;
    }

    public boolean isProductEnabled(String wantProductName) {
        return WILDCARD.equals(wantProductName) || productName.equals(wantProductName);
    }

    public long getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public String getLicenseeName() {
        return licenseeName;
    }

    public String getLicenseeContactEmail() {
        return licenseeContactEmail;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getExpiryDate() {
        return expiryDate;
    }

    public String getEulaText() {
        return eulaText;
    }

    public Set<String> getAttributes() {
        return attributes;
    }

    public Set<String> getFeatureSets() {
        return featureSets;
    }

    public String getHostname() {
        return hostname;
    }

    public String getIp() {
        return ip;
    }

    public String getProductName() {
        return productName;
    }

    public String getMajorVersion() {
        return majorVersion;
    }

    public String getMinorVersion() {
        return minorVersion;
    }

    public String getFeatureLabel() {
        return featureLabel;
    }

    public X509Certificate getTrustedIssuer() {
        return trustedIssuer;
    }

    public LicenseDocument getLicenseDocument() {
        return licenseDocument;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(43, 47)
                .append(id)
                .append(description)
                .append(licenseeName)
                .append(licenseeContactEmail)
                .append(startDate)
                .append(expiryDate)
                .append(eulaText)
                .append(attributes)
                .append(featureSets)
                .append(hostname)
                .append(ip)
                .append(productName)
                .append(majorVersion)
                .append(minorVersion)
                .append(featureLabel)
                .append(trustedIssuer)
                .append(licenseDocument)
                .toHashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (null == o || getClass() != o.getClass()) return false;

        FeatureLicense that = (FeatureLicense) o;

        return ObjectUtils.equals(this.id, that.id) &&
                ObjectUtils.equals(this.description, that.getDescription()) &&
                ObjectUtils.equals(this.licenseeName, that.getLicenseeName()) &&
                ObjectUtils.equals(this.licenseeContactEmail, that.getLicenseeContactEmail()) &&
                ObjectUtils.equals(this.startDate, that.getStartDate()) &&
                ObjectUtils.equals(this.expiryDate, that.getExpiryDate()) &&
                ObjectUtils.equals(this.eulaText, that.getEulaText()) &&
                ObjectUtils.equals(this.attributes, that.getAttributes()) &&
                ObjectUtils.equals(this.featureSets, that.getFeatureSets()) &&
                ObjectUtils.equals(this.hostname, that.getHostname()) &&
                ObjectUtils.equals(this.ip, that.getIp()) &&
                ObjectUtils.equals(this.productName, that.getProductName()) &&
                ObjectUtils.equals(this.majorVersion, that.getMajorVersion()) &&
                ObjectUtils.equals(this.minorVersion, that.getMinorVersion()) &&
                ObjectUtils.equals(this.featureLabel, that.getFeatureLabel()) &&
                ObjectUtils.equals(this.trustedIssuer, that.getTrustedIssuer()) &&
                ObjectUtils.equals(this.licenseDocument, that.getLicenseDocument());
    }

    public String toString() {
        return String.valueOf(id) + " (" + licenseeName + ")";
    }
}
