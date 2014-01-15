/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server;

import com.l7tech.gateway.common.LicenseException;
import com.l7tech.gateway.common.License;
import com.l7tech.gateway.common.InvalidLicenseException;
import com.l7tech.policy.AssertionLicense;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.util.TooManyChildElementsException;

import java.util.HashSet;
import java.util.Set;
import java.security.SignatureException;
import java.text.ParseException;

import org.xml.sax.SAXException;

/**
 * A permissive license manager, for testing, that enables all features by default.
 */
public class TestLicenseManager implements AssertionLicense, UpdatableLicenseManager {
    private final Set<String> disabledFeatures = new HashSet<String>();
    private final String licenseText = 
            "<license Id=\"1\" xmlns=\"http://l7tech.com/license\">\n" +
            "    <description>Layer 7 Development</description>\n" +
            "    <licenseAttributes/>\n" +
            "    <valid>2008-05-06T19:20:40.447Z</valid>\n" +
            "    <expires>2019-05-06T19:20:42.583Z</expires>\n" +
            "    <host name=\"*\"/>\n" +
            "    <ip address=\"*\"/>\n" +
            "    <product name=\"Layer 7 SecureSpan Suite\"/>\n" +
            "    <featureLabel>SecureSpan Gateway</featureLabel>\n" +
            "    <licensee name=\"Layer 7 Development\"/>\n" +
            "</license>";

    public TestLicenseManager() {
    }

    public TestLicenseManager(final Set<String> disabledFeatures) {
        for (String feature : disabledFeatures) {
            disableFeature(feature);
        }
    }

    public boolean isAssertionEnabled( Assertion assertion ) {
        return true;
    }

    public boolean isFeatureEnabled(String feature) {
        return !disabledFeatures.contains(feature);
    }

    public void requireFeature(String feature) throws LicenseException {
        if (!isFeatureEnabled(feature))
            throw new LicenseException("feature " + feature + " is not enabled");
    }

    public void enableFeature(String feature) {
        disabledFeatures.remove(feature);
    }

    public void disableFeature(String feature) {
        disabledFeatures.add(feature);
    }

    public License getCurrentLicense() throws InvalidLicenseException {
        try {
            return new License(licenseText, null, null);
        } catch (SAXException e) {
            throw new InvalidLicenseException("Error creating test license", e);
        } catch (ParseException e) {
            throw new InvalidLicenseException("Error creating test license", e);
        } catch (TooManyChildElementsException e) {
            throw new InvalidLicenseException("Error creating test license", e);
        } catch (SignatureException e) {
            throw new InvalidLicenseException("Error creating test license", e);
        }
    }

    public void installNewLicense(String newLicenseXml) throws InvalidLicenseException, UpdateException {
     
    }

    @Override
    public void validateLicense(String licenseXml) throws InvalidLicenseException {

    }
}
