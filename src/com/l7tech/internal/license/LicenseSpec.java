/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.internal.license;

import com.l7tech.common.License;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * Represents parameters for generating a license.  Pass to {@link LicenseGenerator} to generate a license.
 * Generated licenses may be signed if the LicenseSpec is created with a certificate and private key.
 */
public class LicenseSpec {
    private X509Certificate issuerCert;
    private PrivateKey issuerKey;
    private long licenseId = 0;
    private Date startDate = null;
    private Date expiryDate = null;
    private String description = null;
    private String licenseeName = null;
    private String licenseeContactEmail = null;
    private String hostname = "*";
    private String ip = "*";
    private String product = "*";
    private String versionMinor = "*";
    private String versionMajor = "*";
    private String eulaText = null;
    private String featureLabel = null;
    private Set<String> rootFeatures = new LinkedHashSet<String>();

    /**
     * Create a LicenseSpec for generating an unsigned license.  Primarily useful for testing, since unsigned
     * licenses are not accepted by license-managed products.
     */
    public LicenseSpec() {
        this(null, null);
    }

    /**
     * Create a LicenseSpec for generating a signed license.  If a signed license is generated ({@link LicenseGenerator}),
     * it will be signed with the specified private key, and include a reference to the specified certificate.
     *
     * @param issuerCert  the certificate with which to sign the new license, or null to generate an unsigned license.
     * @param issuerKey   the private key with which to sign the new license.  Required if issuerCert is not null.
     * @throws NullPointerException if issuerCert is specified without issuerKey
     */
    public LicenseSpec(X509Certificate issuerCert, PrivateKey issuerKey) {
        if (issuerCert != null && issuerKey == null) throw new NullPointerException("Issuer private key must be specified to generate a signed license.");
        this.issuerCert = issuerCert;
        this.issuerKey = issuerKey;
    }

    /**
     * Attempt to configure this LicenseSpec so that the License generated from it will look like the specified license.
     * The license being imported won't be able to be duplicated completely -- in particular note these limitations:
     *  -  the new license issuer will be the issuer used by this LicenseSpec rather than the original issuer
     *  -  any XML comments and formatting will be ignored
     *  -  any XML elements or license features not supported by this version of LicenseSpec will not be imported
     *
     * @param license the license whose fields and grants should be imported.  Must not be null.
     */
    public void copyFrom(License license) {
        final Object spec = license.getSpec();
        if (!(spec instanceof License.LicenseGrants))
            throw new IllegalArgumentException("License stores grants in an unrecognized format");

        License.LicenseGrants grants = (License.LicenseGrants)spec;

        this.setLicenseId(license.getId());
        this.setLicenseeName(license.getLicenseeName());
        this.setLicenseeContactEmail(license.getLicenseeContactEmail());
        this.setDescription(license.getDescription());
        this.setStartDate(license.getStartDate());
        this.setExpiryDate(license.getExpiryDate());
        this.setEulaText(license.getEulaText());

        this.setIp(grants.getIp());
        this.setHostname(grants.getHostname());
        this.setProduct(grants.getProduct());
        this.setVersionMajor(grants.getVersionMajor());
        this.setVersionMinor(grants.getVersionMinor());
        this.setFeatureLabel(grants.getFeatureLabel());

        this.clearRootFeatures();
        this.rootFeatures.addAll(Arrays.asList(grants.getRootFeatureSetNames()));
    }

    /** @return a read-only view of the root feature names that will be enabled by this licensespec. */
    public Set<String> getRootFeatures() {
        return Collections.unmodifiableSet(rootFeatures);
    }

    /** Clear all root features for this license spec. */
    public void clearRootFeatures() {
        rootFeatures.clear();
    }

    /**
     * Add a root feature (named feature set, ie "set:Profile:IPS" or "assertion:OneOrMore") to this license spec.
     *
     * @param featureName  the feature to add.  Required.
     */
    public void addRootFeature(String featureName) {
        rootFeatures.add(featureName);
    }

    /**
     * Change the signing info that will be used by this LicenseSpec.
     *
     * @param issuerCert  the certificate with which to sign the new license, or null to generate an unsigned license.
     * @param issuerKey   the private key with which to sign the new license.  Required if issuerCert is not null.
     */
    public void setSigningInfo(X509Certificate issuerCert, PrivateKey issuerKey) {
        if (issuerCert != null && issuerKey == null) throw new NullPointerException("Issuer private key must be specified to generate a signed license.");
        this.issuerCert = issuerCert;
        this.issuerKey = issuerKey;
    }

    /** @return the issuer certificate, or null if this LicenseSpec is uanble to generate a signed license. */
    public X509Certificate getIssuerCert() {
        return issuerCert;
    }

    /** @return the issuer private key, or null if this LicenseSpec is unable to generate a signed license. */
    public PrivateKey getIssuerKey() {
        return issuerKey;
    }

    /** @return the License ID that would be used if a license were generated from this LicenseSpec, or 0 if it has not yet been set. */
    public long getLicenseId() {
        return licenseId;
    }

    /**
     * @param licenseId the License ID to use when a license is generated from this LicenseSpec.
     *                  Must be positive.
     *                  If two licenses are generated that have the same issuer, those two license must be
     *                  generated with difference license IDs.  If you don't care about the license ID,
     *                  and assuming that nobody else is concurrently generated licenses at the same time,
     *                  then just use {@link System#currentTimeMillis()} and then sleep for one millisecond.
     */
    public void setLicenseId(long licenseId) {
        if (licenseId < 0) throw new IllegalArgumentException("License ID must be positive.");
        // Note: we allow zero to pass here since it's the default value.  The LicenseGenerator will reject it later on.
        this.licenseId = licenseId;
    }

    /** Increment the license ID. */
    void rollLicenseId() {
        this.licenseId++;
    }

    /** @return the start date of the license, or null if the license would be generated without an explicit start date. */
    public Date getStartDate() {
        return startDate;
    }

    /** @param startDate the earliest date at which the license should be valid, or null to be valid on any date that is non-expired. */
    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    /** @return the expiry date of the license, or null if the license would be generated without an expiry date. */
    public Date getExpiryDate() {
        return expiryDate;
    }

    /** @param expiryDate the expiry date of the license to generate, or null to generate a license that never expires. */
    public void setExpiryDate(Date expiryDate) {
        this.expiryDate = expiryDate;
    }

    /**
     * @return the licensee name, or null if one has not yet been set.
     */
    public String getLicenseeName() {
        return licenseeName;
    }

    /**
     * Set the name of the licensee.  This must be set before a license is generated.
     *
     * @param licenseeName the name of the licensee.  Must be non-null to generate a license.
     */
    public void setLicenseeName(String licenseeName) {
        this.licenseeName = licenseeName;
    }

    /**
     * @return the licensee contact email address, or null if the license would be generated without one.
     */
    public String getLicenseeContactEmail() {
        return licenseeContactEmail;
    }

    /**
     * Set the licensee contact email address, if one is to be included.
     *
     * @param licenseeContactEmail the licensee contact email address, or null to avoid including one.
     */
    public void setLicenseeContactEmail(String licenseeContactEmail) {
        this.licenseeContactEmail = licenseeContactEmail;
    }

    /** @return the human readable short description of this license, or null if one will not be included. */
    public String getDescription() {
        return description;
    }

    /**
     * Set the human readable short description of this license, ie "SecureSpan Gateway evaluation license".
     * Set to null if no human readable short description should be included in the generated license.
     *
     * @param description the human readable short description, or null to avoid including one.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /** @return the hostname to which to bind the license, or "*" to allow any hostname.  Never null. */
    public String getHostname() {
        return hostname;
    }

    /** @param hostname the hostname to bind the license, or "*" to allow any hostname. */
    public void setHostname(String hostname) {
        if (hostname == null) hostname = "*";
        this.hostname = hostname;
    }

    /** @return the ip address to which to bind the license, or "*" to allow any ip address.  Never null. */
    public String getIp() {
        return ip;
    }

    /** @param ip the ip to which to bind the license, or "*" to allow any ip address. */
    public void setIp(String ip) {
        if (ip == null) ip = "*";
        this.ip = ip;
    }

    /** @return the product to which to bind the license, or "*" to allow any product. Never null. */
    public String getProduct() {
        return product;
    }

    /** @param product the name of the product to which to bind the license, or "*" to allow any product. */
    public void setProduct(String product) {
        if (product == null) product = "*";
        this.product = product;
    }

    /** @return the minor version to which to bind the license, or "*" to allow any minor version. Never null. */
    public String getVersionMinor() {
        return versionMinor;
    }

    /** @param versionMinor the minor version to which to bind the liense, or "*" to allow any minor version. */
    public void setVersionMinor(String versionMinor) {
        if (versionMinor == null) versionMinor = "*";
        this.versionMinor = versionMinor;
    }

    /** @return the major version to which to bind the license, or "*" to allow any major version. Never null. */
    public String getVersionMajor() {
        return versionMajor;
    }

    /** @param versionMajor the major version to which to bind the license, or "*" to allow any major version. */
    public void setVersionMajor(String versionMajor) {
        if (versionMajor == null) versionMajor = "*";
        this.versionMajor = versionMajor;
    }

    /** @return the literal text of the EULA to include in the new license, or null to avoid including any. */
    public String getEulaText() {
        return eulaText;
    }

    /** @param eulaText the literal text of the EULA to include in the new license, or null to avoid including any. */
    public void setEulaText(String eulaText) {
        this.eulaText = eulaText;
    }

    /** @return the string to show instead of the generated feature grants, or null. */
    public String getFeatureLabel() {
        return featureLabel;
    }

    /** @param featureLabel A string to show instead of the generated feature grants string, or null to just use the generated string. */
    public void setFeatureLabel(String featureLabel) {
        this.featureLabel = featureLabel;
    }
}
