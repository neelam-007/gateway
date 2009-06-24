package com.l7tech.common.io;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Parameters for generating a new certificate.
 * <p/>
 * This should express everything we need to tell a certificate generation utility (aside from Keys and issuer information)
 * without using any Bouncy Castle or other certgen-utility-specific classes.
 */
public class CertGenParams implements Serializable {
    // Base fields
    private String subjectDn;
    private int daysUntilExpiry;
    private Date notBefore;
    private Date notAfter;
    private String signatureAlgorithm;
    private BigInteger serialNumber;

    // SKI and AKI extensions
    private boolean includeSki = true;
    private boolean includeAki = true;

    // Basic constraints extension.  Always critical, if included.
    private boolean includeBasicConstraints;
    private boolean basicConstraintsCa;
    private Integer basicConstratinsPathLength;

    // Key usage extension
    private boolean includeKeyUsage;
    private boolean keyUsageCritical = true;
    private int keyUsageBits;

    // Extended key usage extension
    private boolean includeExtendedKeyUsage;
    private boolean extendedKeyUsageCritical = true;
    private List<String> extendedKeyUsageKeyPurposeOids;

    // Subject directory attributes extension (country of citizenship)
    private boolean includeSubjectDirectoryAttributes;
    private boolean subjectDirectoryAttributesCritical;
    private List<String> countryOfCitizenshipCountryCodes;

    public CertGenParams() {
    }

    /**
     * Convenience constructor that sets the subject DN, the days until expiry, and the CA flag.
     * <p/>
     * If the CA flag is set to true, this constructor will also set up with basic constraints with path length 1, and
     * a key usage allowing cert and CRL signing but nothing else.
     *
     * @param subjectDn the subject dn, ie "cn=www.example.com".  Required.
     * @param expiryDays days from now until the cert shall expire, or 0 to use default.
     * @param ca if true, the cert will be configured as a CA cert
     */
    public CertGenParams(String subjectDn, int expiryDays, boolean ca, String sigAlg) {
        setSubjectDn(subjectDn);

        setSignatureAlgorithm(sigAlg);

        setDaysUntilExpiry(expiryDays);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR, -1);
        setNotBefore(cal.getTime());
        cal.add(Calendar.DAY_OF_MONTH, expiryDays);
        setNotAfter(cal.getTime());

        if (ca) {
            setIncludeBasicConstraints(true);
            setBasicConstraintsCa(true);
            setBasicConstratinsPathLength(1);
            setIncludeKeyUsage(true);
            setKeyUsageCritical(true);
            setKeyUsageBits((1 << 2) | (1 << 1));
        }        
    }

    public String getSubjectDn() {
        return subjectDn;
    }

    public void setSubjectDn(String subjectDn) {
        this.subjectDn = subjectDn;
    }

    public int getDaysUntilExpiry() {
        return daysUntilExpiry;
    }

    /** @param daysUntilExpiry Days until expiry, or 0 for default.  Ignored if notAfter is specified. */
    public void setDaysUntilExpiry(int daysUntilExpiry) {
        this.daysUntilExpiry = daysUntilExpiry;
    }

    public Date getNotBefore() {
        return notBefore;
    }

    public void setNotBefore(Date notBefore) {
        this.notBefore = notBefore;
    }

    public Date getNotAfter() {
        return notAfter;
    }

    public void setNotAfter(Date notAfter) {
        this.notAfter = notAfter;
    }

    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public void setSignatureAlgorithm(String signatureAlgorithm) {
        this.signatureAlgorithm = signatureAlgorithm;
    }

    public BigInteger getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(BigInteger serialNumber) {
        this.serialNumber = serialNumber;
    }

    public boolean isIncludeSki() {
        return includeSki;
    }

    public void setIncludeSki(boolean includeSki) {
        this.includeSki = includeSki;
    }

    public boolean isIncludeAki() {
        return includeAki;
    }

    public void setIncludeAki(boolean includeAki) {
        this.includeAki = includeAki;
    }

    public boolean isIncludeBasicConstraints() {
        return includeBasicConstraints;
    }

    public void setIncludeBasicConstraints(boolean includeBasicConstraints) {
        this.includeBasicConstraints = includeBasicConstraints;
    }

    public boolean isBasicConstraintsCa() {
        return basicConstraintsCa;
    }

    public void setBasicConstraintsCa(boolean basicConstraintsCa) {
        this.basicConstraintsCa = basicConstraintsCa;
    }

    public Integer getBasicConstratinsPathLength() {
        return basicConstratinsPathLength;
    }

    public void setBasicConstratinsPathLength(Integer basicConstratinsPathLength) {
        this.basicConstratinsPathLength = basicConstratinsPathLength;
    }

    public boolean isIncludeKeyUsage() {
        return includeKeyUsage;
    }

    public void setIncludeKeyUsage(boolean includeKeyUsage) {
        this.includeKeyUsage = includeKeyUsage;
    }

    public boolean isKeyUsageCritical() {
        return keyUsageCritical;
    }

    public void setKeyUsageCritical(boolean keyUsageCritical) {
        this.keyUsageCritical = keyUsageCritical;
    }

    public int getKeyUsageBits() {
        return keyUsageBits;
    }

    public void setKeyUsageBits(int keyUsageBits) {
        this.keyUsageBits = keyUsageBits;
    }

    public boolean isIncludeExtendedKeyUsage() {
        return includeExtendedKeyUsage;
    }

    public void setIncludeExtendedKeyUsage(boolean includeExtendedKeyUsage) {
        this.includeExtendedKeyUsage = includeExtendedKeyUsage;
    }

    public boolean isExtendedKeyUsageCritical() {
        return extendedKeyUsageCritical;
    }

    public void setExtendedKeyUsageCritical(boolean extendedKeyUsageCritical) {
        this.extendedKeyUsageCritical = extendedKeyUsageCritical;
    }

    public List<String> getExtendedKeyUsageKeyPurposeOids() {
        return extendedKeyUsageKeyPurposeOids;
    }

    public void setExtendedKeyUsageKeyPurposeOids(List<String> extendedKeyUsageKeyPurposeOids) {
        this.extendedKeyUsageKeyPurposeOids = extendedKeyUsageKeyPurposeOids;
    }

    public boolean isIncludeSubjectDirectoryAttributes() {
        return includeSubjectDirectoryAttributes;
    }

    public void setIncludeSubjectDirectoryAttributes(boolean includeSubjectDirectoryAttributes) {
        this.includeSubjectDirectoryAttributes = includeSubjectDirectoryAttributes;
    }

    public boolean isSubjectDirectoryAttributesCritical() {
        return subjectDirectoryAttributesCritical;
    }

    public void setSubjectDirectoryAttributesCritical(boolean subjectDirectoryAttributesCritical) {
        this.subjectDirectoryAttributesCritical = subjectDirectoryAttributesCritical;
    }

    public List<String> getCountryOfCitizenshipCountryCodes() {
        return countryOfCitizenshipCountryCodes;
    }

    public void setCountryOfCitizenshipCountryCodes(List<String> countryOfCitizenshipCountryCodes) {
        this.countryOfCitizenshipCountryCodes = countryOfCitizenshipCountryCodes;
    }

    /**
     * Disable all extensions.
     * This currently disables inclusion of a SKI, AKI, basic constraints, key usage, and extended key usage.
     * In the future, if further extensions are supported by CertGenParams, this method will disable them as well.
     */
    public void disableAllExtensions() {
        includeAki = false;
        includeSki = false;
        includeBasicConstraints = false;
        includeKeyUsage = false;
        includeExtendedKeyUsage = false;
    }
}

