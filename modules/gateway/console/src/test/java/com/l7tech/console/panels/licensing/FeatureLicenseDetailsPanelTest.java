package com.l7tech.console.panels.licensing;

import com.l7tech.console.security.*;
import com.l7tech.gateway.common.licensing.FeatureLicense;
import com.l7tech.util.CollectionUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.security.cert.X509Certificate;
import java.util.Date;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class FeatureLicenseDetailsPanelTest {

    @Mock
    private X509Certificate trustedIssuer;

    @Before
    public void before() {
        Mockito.when(trustedIssuer.getSubjectDN()).thenReturn(() -> "principal name");
    }

    @Test
    public void testValidLicense() {
        FeatureLicense featureLicense = new FeatureLicense(123L, "description", "licenseeName", "licenseeContactEmail",
                new Date(System.currentTimeMillis() - (1000 * 60 * 60 * 24)), new Date(System.currentTimeMillis() + (1000 * 60 * 60 * 24)), "eula", CollectionUtils.set("attributes"), CollectionUtils.set("featureSets"), "hostname", "ip",
                "productName", "9", "2", "featureLabel",
                trustedIssuer, null);
        GatewayInfoHolder gatewayInfoHolder = GatewayVersionInfoHolderTestFactory.getNewInstance("9.2.00", "1.0.00", "productName", "legacy");
        FeatureLicenseDetailsPanel featureLicenseDetailsPanel = new FeatureLicenseDetailsPanel(featureLicense, gatewayInfoHolder, "ssgHost");

        assertEquals(FeatureLicenseDetailsPanel.LICENSE_STATUS_VALID, featureLicenseDetailsPanel.statusField.getText());
    }

    @Test
    public void testLicense_LegacyProductName() {
        //Test that old licenses with the old product name (secure span gateway) continue to work
        FeatureLicense featureLicense = new FeatureLicense(123L, "description", "licenseeName", "licenseeContactEmail",
                new Date(System.currentTimeMillis() - (1000 * 60 * 60 * 24)), new Date(System.currentTimeMillis() + (1000 * 60 * 60 * 24)), "eula", CollectionUtils.set("attributes"), CollectionUtils.set("featureSets"), "hostname", "ip",
                "legacy", "9", "2", "featureLabel",
                trustedIssuer, null);
        GatewayInfoHolder gatewayInfoHolder = GatewayVersionInfoHolderTestFactory.getNewInstance("9.2.00", "1.0.00", "productName", "legacy");
        FeatureLicenseDetailsPanel featureLicenseDetailsPanel = new FeatureLicenseDetailsPanel(featureLicense, gatewayInfoHolder, "ssgHost");

        assertEquals(FeatureLicenseDetailsPanel.LICENSE_STATUS_VALID, featureLicenseDetailsPanel.statusField.getText());
    }

    @Test
    public void testLicense_OldGatewayVersion() {
        //Test when the license is for an old gateway version
        FeatureLicense featureLicense = new FeatureLicense(123L, "description", "licenseeName", "licenseeContactEmail",
                new Date(System.currentTimeMillis() - (1000 * 60 * 60 * 24)), new Date(System.currentTimeMillis() + (1000 * 60 * 60 * 24)), "eula", CollectionUtils.set("attributes"), CollectionUtils.set("featureSets"), "hostname", "ip",
                "productName", "8", "2", "featureLabel",
                trustedIssuer, null);
        GatewayInfoHolder gatewayInfoHolder = GatewayVersionInfoHolderTestFactory.getNewInstance("9.2.00", "1.0.00", "productName", "legacy");
        FeatureLicenseDetailsPanel featureLicenseDetailsPanel = new FeatureLicenseDetailsPanel(featureLicense, gatewayInfoHolder, "ssgHost");

        assertEquals(FeatureLicenseDetailsPanel.LICENSE_STATUS_INVALID, featureLicenseDetailsPanel.statusField.getText());
    }

    @Test
    public void testLicense_Expired() {
        //Test when the license is expired
        FeatureLicense featureLicense = new FeatureLicense(123L, "description", "licenseeName", "licenseeContactEmail",
                new Date(System.currentTimeMillis() - (1000 * 60 * 60 * 24)), new Date(System.currentTimeMillis() - (1000 * 60 * 60 * 24)), "eula", CollectionUtils.set("attributes"), CollectionUtils.set("featureSets"), "hostname", "ip",
                "productName", "9", "2", "featureLabel",
                trustedIssuer, null);
        GatewayInfoHolder gatewayInfoHolder = GatewayVersionInfoHolderTestFactory.getNewInstance("9.2.00", "1.0.00", "productName", "legacy");
        FeatureLicenseDetailsPanel featureLicenseDetailsPanel = new FeatureLicenseDetailsPanel(featureLicense, gatewayInfoHolder, "ssgHost");

        assertEquals(FeatureLicenseDetailsPanel.LICENSE_STATUS_EXPIRED, featureLicenseDetailsPanel.statusField.getText());
    }

    @Test
    public void testLicense_Future() {
        //Test whe the license is for the future.
        FeatureLicense featureLicense = new FeatureLicense(123L, "description", "licenseeName", "licenseeContactEmail",
                new Date(System.currentTimeMillis() + (1000 * 60 * 60 * 24)), new Date(System.currentTimeMillis() + (2 * 1000 * 60 * 60 * 24)), "eula", CollectionUtils.set("attributes"), CollectionUtils.set("featureSets"), "hostname", "ip",
                "productName", "9", "2", "featureLabel",
                trustedIssuer, null);
        GatewayInfoHolder gatewayInfoHolder = GatewayVersionInfoHolderTestFactory.getNewInstance("9.2.00", "1.0.00", "productName", "legacy");
        FeatureLicenseDetailsPanel featureLicenseDetailsPanel = new FeatureLicenseDetailsPanel(featureLicense, gatewayInfoHolder, "ssgHost");

        assertEquals(FeatureLicenseDetailsPanel.LICENSE_STATUS_INVALID, featureLicenseDetailsPanel.statusField.getText());
    }
}