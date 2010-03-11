package com.l7tech.gateway.client;

import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;

/**
 * Gateway management client tests for developer use.
 */
@Ignore("Developer tests (require Gateway)")
public class DeveloperClientTest {

    private final String gateway = "http://localhost:8080/wsman";
    private final String username = "admin";
    private final String password = "password";
    private final String inputFile = "/tmp/in.xml";
    private final String clusterPropertyId = "226885632";
    private final String serviceId = "17268736";

    @Test
    public void testEnumerateEverything() throws Exception {
        final Collection<String> types = Arrays.asList( "clusterProperty", "folder", "jdbcConnection", "jmsDestination", "identityProvider", "policy", "privateKey", "resourceDocument", "service", "trustedCertificate" );
        for ( String type : types ) {
            GatewayManagementClient gmc = new GatewayManagementClient( new String[]{ gateway, "enumerate", "-type", type, "-username", username, "-password", password }, System.out, System.out );
            gmc.run();
        }
    }

    @Test
    public void testEnumerate() throws Exception {
        GatewayManagementClient gmc = new GatewayManagementClient( new String[]{ gateway, "enumerate", "-type", "clusterProperty", "-username", username, "-password", password }, System.out, System.out );
        gmc.run();
    }

    @Test
    public void testGet() throws Exception {
        GatewayManagementClient gmc = new GatewayManagementClient( new String[]{ gateway, "get", "-type", "service", "-id", serviceId, "-username", username, "-password", password }, System.out, System.out );
        gmc.run();
    }

    @Test
    public void testPut() throws Exception {
        GatewayManagementClient gmc = new GatewayManagementClient( new String[]{ gateway, "put", "-type", "service", "-username", username, "-password", password, "-inFile", inputFile }, System.out, System.out );
        gmc.run();
    }

    @Test
    public void testCreate() throws Exception {
        GatewayManagementClient gmc = new GatewayManagementClient( new String[]{ gateway, "create", "-type", "clusterProperty", "-username", username, "-password", password, "-inFile", "/tmp/cpin.xml" }, System.out, System.out );
        gmc.run();
    }

    @Test
    public void testDelete() throws Exception {
        GatewayManagementClient gmc = new GatewayManagementClient( new String[]{ gateway, "delete", "-type", "clusterProperty", "-id", clusterPropertyId, "-username", username, "-password", password }, System.out, System.out );
        gmc.run();
    }

    @Test
    public void testValidatePolicy() throws Exception {
        GatewayManagementClient gmc = new GatewayManagementClient( new String[]{ gateway, "validate", "-type", "service", "-id", serviceId, "-username", username, "-password", password }, System.out, System.out );
        gmc.run();
    }

    @Test
    public void testImportPolicy() throws Exception {
        GatewayManagementClient gmc = new GatewayManagementClient( new String[]{ gateway, "import", "-type", "service", "-id", serviceId, "-username", username, "-password", password, "-inFile", inputFile, "-import", "replace", "IdProviderReference", "491520", "147226624"}, System.out, System.out );
        gmc.run();
    }

    @Test
    public void testExportPolicy() throws Exception {
        GatewayManagementClient gmc = new GatewayManagementClient( new String[]{ gateway, "export", "-type", "service", "-id", serviceId, "-username", username, "-password", password }, System.out, System.out );
        gmc.run();
    }
}
