package com.l7tech.gateway.config.client.beans;

import org.junit.Before;
import org.junit.Test;

import java.net.URL;

import static org.junit.Assert.*;

/* This test class is to verify getNodeApiTimeout() function in NodeManagementApiFactory.java class.
   and to verify host.node.api.timeout.millis property, against various input values, for example: Zero, Non-numeric, Negative values etc..
 */
public class NodeManagementApiFactoryTest {
    private NodeManagementApiFactory testApiTimeout = null;

    @Before
    public void setup() {
        testApiTimeout = new NodeManagementApiFactory();
    }

    @Test
    public void testGetNodeApiTimeoutHostConfigDoesNotExist() {
        String testHostConfig = this.getFilePath("com/l7tech/gateway/config/client/beans");
        assertNotNull("Unable to get location com/l7tech/gateway/config/client/beans", testHostConfig);
        testHostConfig += "/does_not_exist_host.properties";
        long timeout = testApiTimeout.getNodeApiTimeout(testHostConfig);
        assertEquals("Failed to get default timeout when host.properties does not exist.",
                NodeManagementApiFactory.DEFAULT_NODE_API_CALL_TIMEOUT_MILLIS, timeout);
    }

    @Test
    public void testGetNodeApiTimeoutNoValueInHostConfig() {
        String testHostConfig = this.getFilePath("com/l7tech/gateway/config/client/beans/host_no_value.properties");
        assertNotNull("Unable to get test file host_no_value.properties", testHostConfig);
        long timeout = testApiTimeout.getNodeApiTimeout(testHostConfig);
        assertEquals("host.node.api.timeout.millis has no value hence the default timeout is returned",
                NodeManagementApiFactory.DEFAULT_NODE_API_CALL_TIMEOUT_MILLIS, timeout);
    }

    @Test
    public void testGetNodeApiTimeoutValidValueInHostConfig() {
        String testHostConfig = this.getFilePath("com/l7tech/gateway/config/client/beans/host_valid_value.properties");
        assertNotNull("Unable to get test file host_valid_value.properties", testHostConfig);
        long timeout = testApiTimeout.getNodeApiTimeout(testHostConfig);
        assertEquals("host.node.api.timeout.millis has a valid timeout in it",
                300000, timeout);
    }

    @Test
    public void testGetNodeApiTimeoutZeroValueInHostConfig() {
        String testHostConfig = this.getFilePath("com/l7tech/gateway/config/client/beans/host_zero_as_a_value.properties");
        assertNotNull("Unable to get test file host_zero_as_a_value.properties", testHostConfig);
        long timeout = testApiTimeout.getNodeApiTimeout(testHostConfig);
        assertEquals("host.node.api.timeout.millis has been defined as zero as a value, hence the default timeout is returned",
                NodeManagementApiFactory.DEFAULT_NODE_API_CALL_TIMEOUT_MILLIS, timeout);
    }

    @Test
    public void testGetNodeApiTimeoutNonNumericValueTypeInHostConfig() {
        String testHostConfig = this.getFilePath("com/l7tech/gateway/config/client/beans/host_non_numeric_value.properties");
        assertNotNull("Unable to get test file host_non_numeric_value.properties", testHostConfig);
        long timeout = testApiTimeout.getNodeApiTimeout(testHostConfig);
        assertEquals("host.node.api.timeout.millis has non numeric value, hence the default timeout is returned",
                NodeManagementApiFactory.DEFAULT_NODE_API_CALL_TIMEOUT_MILLIS, timeout);
    }

    @Test
    public void testGetNodeApiTimeoutNegativeValueTypeInHostConfig() {
        String testHostConfig = this.getFilePath("com/l7tech/gateway/config/client/beans/host_negative_value.properties");
        assertNotNull("Unable to get test file host_negative_value.properties", testHostConfig);
        long timeout = testApiTimeout.getNodeApiTimeout(testHostConfig);
        assertEquals("host.node.api.timeout.millis has a negative value, hence the default timeout is returned",
                NodeManagementApiFactory.DEFAULT_NODE_API_CALL_TIMEOUT_MILLIS, timeout);
    }

    @Test
    public void testGetNodeApiTimeoutWhenPropertyDoesNotExistInHostConfig() {
        String testHostConfig = this.getFilePath("com/l7tech/gateway/config/client/beans/host_property_does_not_exist.properties");
        assertNotNull("Unable to get test file host_property_does_not_exist.properties", testHostConfig);
        long timeout = testApiTimeout.getNodeApiTimeout(testHostConfig);
        assertEquals("host.node.api.timeout.millis does not exist in host.properties file, hence the default timeout is returned",
                NodeManagementApiFactory.DEFAULT_NODE_API_CALL_TIMEOUT_MILLIS, timeout);
    }

    private String getFilePath(String path) {
        URL resource = this.getClass().getClassLoader().getResource(path);
        if (resource == null) {
            fail("Host config file not found.");
        }
        return resource.getFile();
    }
}
