package com.l7tech.console.security;

import com.l7tech.util.Config;
import com.l7tech.util.MockConfig;
import com.l7tech.util.Pair;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Properties;

public class SecurityProviderImplTest {

    private Properties configProperties = new Properties();
    private Config config = new MockConfig(configProperties);

    @Before
    public void before() {
        configProperties.clear();
    }

    @After
    public void after() {
        configProperties.clear();
    }

    @Test
    public void testVersionCompatibility_VersionsCompatible() throws Exception {
        //test that the version compatibility checker passes on version that should be compatible
        testVersionCompatibility("10.0.00", "9.4.00", "2.0.00", "1.5.00");
    }

    @Test(expected = VersionException.class)
    public void testVersionCompatibility_GatewayVersionBelowManagerMinimum_VersionExceptionThrown() throws Exception {
        //test that it fails when the gateway version is lower then the version required
        testVersionCompatibility("10.0.00", "11.2.00", "1.0.00", "1.0.00");
    }

    @Test(expected = VersionException.class)
    public void testVersionCompatibility_ManagerVersionBelowGatewayMinimum_VersionExceptionThrown() throws Exception {
        //test that it fails when the policy manager version is lower then the version required
        testVersionCompatibility("10.0.00", "9.2.00", "3.3.00", "5.4.00");
    }

    @Test
    public void testVersionCompatibility_92GatewayWithCompatibleManager() throws Exception {
        //tests that the response is successful when the gateway is version 9.2
        String gatewayVersion = "9.2.00";
        String minimunGatewayVersionRequired = "9.2.00";
        String policyManagerVersion = "2.0.00";
        String minimumPolicyManagerVersionRequired = "20060228"; //this is what the 9.2 gateway returns
        configProperties.setProperty("gateway.version.minimum", minimunGatewayVersionRequired);
        configProperties.setProperty("policyManager.version", policyManagerVersion);

        Pair<Version, Version> versions = new SecurityProviderImpl().validateVersionCompatibility(gatewayVersion, minimumPolicyManagerVersionRequired, PolicyManagerBuildInfoTestFactory.getNewInstance(config));

        Assert.assertEquals("Gateway Version is incorrect", gatewayVersion, versions.left.toString());
        Assert.assertEquals("Gateway Version is incorrect", "1.0.00", versions.right.toString());
    }

    private void testVersionCompatibility(String gatewayVersion, String minimunGatewayVersionRequired, String policyManagerVersion, String minimumPolicyManagerVersionRequired) throws VersionException {
        configProperties.setProperty("gateway.version.minimum", minimunGatewayVersionRequired);
        configProperties.setProperty("policyManager.version", policyManagerVersion);

        Pair<Version, Version> versions = new SecurityProviderImpl().validateVersionCompatibility(gatewayVersion, minimumPolicyManagerVersionRequired, PolicyManagerBuildInfoTestFactory.getNewInstance(config));
        Assert.assertEquals("Gateway Version is incorrect", gatewayVersion, versions.left.toString());
        Assert.assertEquals("Gateway Version is incorrect", minimumPolicyManagerVersionRequired, versions.right.toString());
    }

}