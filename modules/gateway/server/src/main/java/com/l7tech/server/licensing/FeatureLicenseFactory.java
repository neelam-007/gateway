package com.l7tech.server.licensing;

import com.l7tech.gateway.common.InvalidLicenseException;
import com.l7tech.gateway.common.licensing.FeatureLicense;
import com.l7tech.gateway.common.licensing.LicenseDocument;
import com.l7tech.util.TooManyChildElementsException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;

import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Date;
import java.util.Set;

import static com.l7tech.gateway.common.licensing.LicenseUtils.*;

/**
 * Factory for creating FeatureLicense instances.
 *
 * @author Jamie Williams - wilja33 - jamie.williams2@ca.com
 */
public class FeatureLicenseFactory {
    /**
     * Creates a new FeatureLicense instance from the specified {@link LicenseDocument}, without examining any
     * signature that may exist.
     * Validation and XML parsing delegated to {@link com.l7tech.gateway.common.licensing.LicenseUtils}.
     *
     * @param licenseDocument representation of the License file
     * @return new FeatureLicense
     * @throws InvalidLicenseException
     */
    public FeatureLicense newInstance(@NotNull LicenseDocument licenseDocument) throws InvalidLicenseException {
        return newInstance(licenseDocument, null);
    }

    /**
     * Creates a new FeatureLicense instance from the specified {@link LicenseDocument}.
     * Validation and XML parsing delegated to {@link com.l7tech.gateway.common.licensing.LicenseUtils}.
     *
     * @param licenseDocument representation of the License file
     * @param trustedIssuers the trusted issuers of Gateway licenses, used to validate the license signature
     * @return new FeatureLicense
     * @throws InvalidLicenseException
     */
    public FeatureLicense newInstance(@NotNull LicenseDocument licenseDocument,
                                      @Nullable X509Certificate[] trustedIssuers) throws InvalidLicenseException {
        long id;
        Date startDate;
        Date expiryDate;
        String description;
        String licenseeName;
        String licenseeContactEmail;
        String eula;
        String hostname;
        String ip;
        String productName;
        String majorVersion;
        String minorVersion;
        String featureLabel;
        Set<String> attributes;
        Set<String> featureSets;
        X509Certificate trustedIssuer = null;

        Document doc = parseLicenseDocument(licenseDocument.getContents());

        try {
            id = getId(doc);
            description = getDescription(doc);
            licenseeName = getLicenseeName(doc);
            licenseeContactEmail = getLicenseeEmail(doc);
            startDate = getStartDate(doc);
            expiryDate = getExpiryDate(doc);
            eula = parseEulaText(doc);
            attributes = Collections.unmodifiableSet(getAttributes(doc));
            featureSets = getFeatureSets(doc);
            hostname = getHostname(doc);
            ip = getIpAddress(doc);
            productName = getProductName(doc);
            majorVersion = getMajorVersionNumber(doc);
            minorVersion = getMinorVersionNumber(doc);
            featureLabel = getFeatureLabel(doc);

            if(null != trustedIssuers && trustedIssuers.length > 0) {
                trustedIssuer = getTrustedIssuer(doc, trustedIssuers);
            }
        } catch (TooManyChildElementsException e) {
            throw new InvalidLicenseException("License XML invalid.", e);
        }

        return new FeatureLicense(id, description, licenseeName, licenseeContactEmail,
                startDate, expiryDate, eula, attributes, featureSets, hostname, ip,
                productName, majorVersion, minorVersion, featureLabel,
                trustedIssuer, licenseDocument);
    }
}
