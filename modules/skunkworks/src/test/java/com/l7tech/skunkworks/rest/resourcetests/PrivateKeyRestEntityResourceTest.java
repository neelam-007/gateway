package com.l7tech.skunkworks.rest.resourcetests;

import com.l7tech.common.TestDocuments;
import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.CertGenParams;
import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.*;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.security.keystore.SsgKeyFinder;
import com.l7tech.server.security.keystore.SsgKeyStore;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.skunkworks.rest.tools.RestEntityTests;
import com.l7tech.skunkworks.rest.tools.RestResponse;
import com.l7tech.test.BugId;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.IgnoreOnDaily;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.IOUtils;
import junit.framework.Assert;
import org.apache.http.entity.ContentType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static junit.framework.Assert.assertEquals;

@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class PrivateKeyRestEntityResourceTest extends RestEntityTests<SsgKeyEntry, PrivateKeyMO> {
    private SsgKeyStoreManager ssgKeyStoreManager;
    private List<SsgKeyEntry> ssgKeyEntries = new ArrayList<>();
    private static final Goid defaultKeystoreId = new Goid(0, 2);
    private SsgKeyStore defaultKeyStore;
    private ClusterPropertyManager clusterPropManager;

    @Before
    public void before() throws Exception {
        clusterPropManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("clusterPropertyManager", ClusterPropertyManager.class);
        ssgKeyStoreManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("ssgKeyStoreManager", SsgKeyStoreManager.class);
        //Create the active connectors

        SsgKeyFinder defaultKeyFinder = ssgKeyStoreManager.findByPrimaryKey(defaultKeystoreId);
        defaultKeyStore = defaultKeyFinder.getKeyStore();

        SsgKeyEntry ssgKeyEntry = new SsgKeyEntry(defaultKeystoreId, "alice", new X509Certificate[]{TestDocuments.getWssInteropAliceCert()}, TestDocuments.getWssInteropAliceKey());
        defaultKeyStore.storePrivateKeyEntry(null, ssgKeyEntry, false).get();

        ssgKeyEntries.add(ssgKeyEntry);

        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(this.getClass().getResourceAsStream("testkey.p12"), "".toCharArray());
        ssgKeyEntry = new SsgKeyEntry(defaultKeystoreId, "testkey", new X509Certificate[]{(X509Certificate) ks.getCertificate("testkey")}, (PrivateKey) ks.getKey("testkey", "".toCharArray()));
        defaultKeyStore.storePrivateKeyEntry(null, ssgKeyEntry, false).get();

        ssgKeyEntries.add(ssgKeyEntry);

        ssgKeyEntry = new SsgKeyEntry(defaultKeystoreId, "bob", new X509Certificate[]{TestDocuments.getWssInteropBobCert()}, TestDocuments.getWssInteropBobKey());
        defaultKeyStore.storePrivateKeyEntry(null, ssgKeyEntry, false).get();

        ssgKeyEntries.add(ssgKeyEntry);
    }

    @After
    public void after() throws FindException, DeleteException, KeyStoreException, ExecutionException, InterruptedException {
        for (String aliases : defaultKeyStore.getAliases()) {
            defaultKeyStore.deletePrivateKeyEntry(null, aliases).get();
        }

        ClusterProperty prop = clusterPropManager.findByUniqueName("keyStore.auditViewer.alias");
        if(prop != null) {
            clusterPropManager.delete(prop);
        }
    }

    @Override
    public List<String> getRetrievableEntityIDs() {
        return Functions.map(ssgKeyEntries, new Functions.Unary<String, SsgKeyEntry>() {
            @Override
            public String call(SsgKeyEntry ssgKeyEntry) {
                return ssgKeyEntry.getId();
            }
        });
    }

    @Override
    @Test
    public void testGetNotExisting() throws Exception {
        Goid badId = new Goid(0, 0);
        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/" + badId.toString() + ":badName", HttpMethod.GET, null, "");

        Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals("Expected successful response", 404, response.getStatus());
        Assert.assertNotNull("Expected not null response body", response.getBody());
    }

    @Override
    public List<PrivateKeyMO> getCreatableManagedObjects() {
        //creating private keys using PrivateKeyMO is not allowed.
        return Collections.emptyList();
    }

    @Override
    @Test
    public void testCreateEntity() throws Exception {
        Map<String, PrivateKeyCreationContext> privateKeyCreationContexts = new HashMap<>();

        PrivateKeyCreationContext privateKeyCreationContext = ManagedObjectFactory.createPrivateKeyCreationContext();
        privateKeyCreationContext.setDn("CN=srcAlias");
        privateKeyCreationContext.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("ecName", "secp384r1")
                .map());
        privateKeyCreationContexts.put(defaultKeystoreId + ":oneProperty", privateKeyCreationContext);

        privateKeyCreationContext = ManagedObjectFactory.createPrivateKeyCreationContext();
        privateKeyCreationContext.setDn("CN=srcAlias");
        privateKeyCreationContext.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .map());
        privateKeyCreationContexts.put(defaultKeystoreId + ":emptyProperties", privateKeyCreationContext);

        privateKeyCreationContext = ManagedObjectFactory.createPrivateKeyCreationContext();
        privateKeyCreationContext.setDn("CN=srcAlias");
        privateKeyCreationContexts.put(defaultKeystoreId + ":nullProperties", privateKeyCreationContext);

        privateKeyCreationContext = ManagedObjectFactory.createPrivateKeyCreationContext();
        privateKeyCreationContext.setDn("CN=srcAlias");
        privateKeyCreationContext.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("ecName", "secp384r1")
                .put("rsaKeySize", 516)
                .put("daysUntilExpiry", 2)
                .put("caCapable", true)
                .put("signatureHashAlgorithm", "SHA384")
                .map());
        privateKeyCreationContexts.put(defaultKeystoreId + ":allProperties", privateKeyCreationContext);

        for (Map.Entry<String, PrivateKeyCreationContext> keyCreationContextEntry : privateKeyCreationContexts.entrySet()) {
            RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/" + keyCreationContextEntry.getKey(), HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(keyCreationContextEntry.getValue())));

            Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
            Assert.assertEquals("Expected successful response", 201, response.getStatus());
            Assert.assertNotNull("Expected not null response body", response.getBody());

            final StreamSource source = new StreamSource(new StringReader(response.getBody()));
            Item item = MarshallingUtils.unmarshal(Item.class, source);

            Assert.assertEquals("Type is incorrect", getType(), item.getType());
            Assert.assertEquals("Type is incorrect", getExpectedTitle(item.getId()), item.getName());
            Assert.assertNotNull("TimeStamp must always be present", item.getDate());

            Assert.assertTrue("Need at least one link", item.getLinks() != null && item.getLinks().size() > 0);
            Link self = findLink("self", item.getLinks());
            Assert.assertNotNull("self link must be present", self);
            Assert.assertEquals("self link is incorrect", getDatabaseBasedRestManagementEnvironment().getUriStart() + getResourceUri() + "/" + item.getId(), self.getUri());

            verifyLinks(item.getId(), item.getLinks());

            Assert.assertNull(item.getContent());
            verifyEntity(item.getId(), keyCreationContextEntry.getValue());
        }
    }

    @Override
    public List<PrivateKeyMO> getUpdateableManagedObjects() {
        List<PrivateKeyMO> privateKeyMOs = new ArrayList<>();

        SsgKeyEntry ssgKeyEntry = ssgKeyEntries.get(1);

        PrivateKeyMO privateKeyMO = ManagedObjectFactory.createPrivateKey();
        privateKeyMO.setId(ssgKeyEntry.getId());
        CertificateData certificateData = ManagedObjectFactory.createCertificateData("MIIDBzCCAe+gAwIBAgIJAPwknkAkOol6MA0GCSqGSIb3DQEBDAUAMBIxEDAOBgNVBAMTB215YWxpYXMwHhcNMTQwNTAxMTcyODAxWhcNMTYwNDMwMTcyODAxWjASMRAwDgYDVQQDEwd0ZXN0a2V5MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAtxOwho2GTZE8o73+7CieN/qCS8XPyWu44nKJHrZ10GxPCdS8pdsogO7+jmcDyS519A7JOlK4tWVlveIFBX3BXMxY++N1Ltc+0IZIykRfL2nMQiiB6GnGLdtWJmZgjF6W+MfOhsHicFiif1s3qDq6zheaOj18ICAogGmCgL9tLJgIjHxaVzhenEohUgzhmkE13oEV1JbDhD9WUOnPKM7nnthxm6QKipO8LQ3by96P6w8iSgmDeoa0lqHAECKhqBkUQfGyZriP4kzGp5uAJfrwKlpFHd1o3dKzS4vhloXebic6keZXvBZvkmKCdtSJ6E9zGOCuNwa3VYXqqdWIqrwA3QIDAQABo2AwXjAMBgNVHRMBAf8EAjAAMA4GA1UdDwEB/wQEAwIF4DAdBgNVHQ4EFgQUlwsKy2We9dHyINGe5+cH1D6RcfowHwYDVR0jBBgwFoAUvCsGYJAc220BIfW1wrUytQaTiPEwDQYJKoZIhvcNAQEMBQADggEBAIrQIpqEU9EKnTYrswheOru9SkDf+0pp2SZvxaCB0lFMcbALD9WWKowtTDr1sIYGURNKgPPhdFjw70htb8zIvFxeBkmKAD5s1xdDeMKHzLnFUTeObzxAPvFkLPNkdveskHfzp/6VSHOM0J9MZpOKMMocm2XzE5TJspFDNmq8Cdvi9YcSL9z80ATcUut0QombaHMBcwfd+yMC44rIOLUTTJFcTB6bloYIFIgrfDiYLDIeMaRklez8J6wGJ9m2e+gZnm8yR6W1ocFnrJhohNXgIOCVNrGnoNmJz8oe7cGoCjO/tNObYCN3dyw7/ifF5qLRpCV+aPi+HjkGklkxLSYLYRc=");
        certificateData.setIssuerName("CN=myalias");
        certificateData.setSubjectName("CN=testkey");
        certificateData.setSerialNumber(new BigInteger("18168820795042335098"));
        CertificateData certificateData2 = ManagedObjectFactory.createCertificateData("MIIDDTCCAfWgAwIBAgIJAPJV6z3+qaB1MA0GCSqGSIb3DQEBDQUAMBIxEDAOBgNVBAMTB215YWxpYXMwHhcNMTQwNDMwMjE1MjUwWhcNMTkwNDI5MjE1MjUwWjASMRAwDgYDVQQDEwdteWFsaWFzMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAuYPMN6Mwr1YjVtUtHCwh2BdhmltsD2piVQaaCrceiJoV0F0h7vQ4jSZFgp1p2KnlZAFJbS1M6t63X1ncQLqgQTX+MDwg+MqMv1f9qI9opZwiazHrSygGpSBY3jo3d32SG3JJFnG9lYzN8gFv0I6YG6dZzrSzDB33rwvKa05yaMf/wThrTXvcKz76i+SihyrIYEXHg3Ku4od3VsxjTefaPTrCsQXaxMNpON5M/0CUZfQwM2yiMhGSe23h36EJJDr6jSZovx1c+3XTZUYSc44SbFPzlcai19VEv4Hwh7wURfbw841BYKqf5miDFiz5cB7+1Wlhwa4ISFsjzzVCtrZLLQIDAQABo2YwZDASBgNVHRMBAf8ECDAGAQH/AgEBMA4GA1UdDwEB/wQEAwIBBjAdBgNVHQ4EFgQUvCsGYJAc220BIfW1wrUytQaTiPEwHwYDVR0jBBgwFoAUvCsGYJAc220BIfW1wrUytQaTiPEwDQYJKoZIhvcNAQENBQADggEBAHpppRP7OoyI4S2RbQqExargTdbo2xnJYhnK0jqxbBoLPgdDxQ8EHoZCoUwf+8qtGZ0SA8g4OJb8R8ZA8rAI7BjNVwxYo/nEfJKlZYzzfWrb7cceFm6lyGJwjRNs8H3p9WEpS+VA+L4RnswFhyjzz95bdi89SMlFm2BfLGEcpey6lZChWlQYEO0vOoCeXu7f8VStZtwi4vQFfPY0v1niDhgMVT7CwPn/kxWfBtzJngog4C2FI06wINl2nF6Uq+HPfuYIgg9EV8u8jOUBGZQg0O2GlHceJ93nhihaGSvrJ2CBQJO/JNcdnVCvwA+qS19x1+kIdcoSbWJySDI3F6i3+uQ=");
        certificateData2.setIssuerName("CN=myalias");
        certificateData2.setSubjectName("CN=myalias");
        certificateData2.setSerialNumber(new BigInteger("17462121781697028213"));

        try {
            privateKeyMO.setCertificateChain(Arrays.asList(certificateData, certificateData2));
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
        privateKeyMO.setAlias(ssgKeyEntry.getAlias());

        privateKeyMOs.add(privateKeyMO);

        //update twice
        privateKeyMO = ManagedObjectFactory.createPrivateKey();
        privateKeyMO.setId(ssgKeyEntry.getId());
        certificateData = ManagedObjectFactory.createCertificateData("MIIDBzCCAe+gAwIBAgIJAPwknkAkOol6MA0GCSqGSIb3DQEBDAUAMBIxEDAOBgNVBAMTB215YWxpYXMwHhcNMTQwNTAxMTcyODAxWhcNMTYwNDMwMTcyODAxWjASMRAwDgYDVQQDEwd0ZXN0a2V5MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAtxOwho2GTZE8o73+7CieN/qCS8XPyWu44nKJHrZ10GxPCdS8pdsogO7+jmcDyS519A7JOlK4tWVlveIFBX3BXMxY++N1Ltc+0IZIykRfL2nMQiiB6GnGLdtWJmZgjF6W+MfOhsHicFiif1s3qDq6zheaOj18ICAogGmCgL9tLJgIjHxaVzhenEohUgzhmkE13oEV1JbDhD9WUOnPKM7nnthxm6QKipO8LQ3by96P6w8iSgmDeoa0lqHAECKhqBkUQfGyZriP4kzGp5uAJfrwKlpFHd1o3dKzS4vhloXebic6keZXvBZvkmKCdtSJ6E9zGOCuNwa3VYXqqdWIqrwA3QIDAQABo2AwXjAMBgNVHRMBAf8EAjAAMA4GA1UdDwEB/wQEAwIF4DAdBgNVHQ4EFgQUlwsKy2We9dHyINGe5+cH1D6RcfowHwYDVR0jBBgwFoAUvCsGYJAc220BIfW1wrUytQaTiPEwDQYJKoZIhvcNAQEMBQADggEBAIrQIpqEU9EKnTYrswheOru9SkDf+0pp2SZvxaCB0lFMcbALD9WWKowtTDr1sIYGURNKgPPhdFjw70htb8zIvFxeBkmKAD5s1xdDeMKHzLnFUTeObzxAPvFkLPNkdveskHfzp/6VSHOM0J9MZpOKMMocm2XzE5TJspFDNmq8Cdvi9YcSL9z80ATcUut0QombaHMBcwfd+yMC44rIOLUTTJFcTB6bloYIFIgrfDiYLDIeMaRklez8J6wGJ9m2e+gZnm8yR6W1ocFnrJhohNXgIOCVNrGnoNmJz8oe7cGoCjO/tNObYCN3dyw7/ifF5qLRpCV+aPi+HjkGklkxLSYLYRc=");
        certificateData.setIssuerName("CN=myalias");
        certificateData.setSubjectName("CN=testkey");
        certificateData.setSerialNumber(new BigInteger("18168820795042335098"));
        certificateData2 = ManagedObjectFactory.createCertificateData("MIIDDTCCAfWgAwIBAgIJAPJV6z3+qaB1MA0GCSqGSIb3DQEBDQUAMBIxEDAOBgNVBAMTB215YWxpYXMwHhcNMTQwNDMwMjE1MjUwWhcNMTkwNDI5MjE1MjUwWjASMRAwDgYDVQQDEwdteWFsaWFzMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAuYPMN6Mwr1YjVtUtHCwh2BdhmltsD2piVQaaCrceiJoV0F0h7vQ4jSZFgp1p2KnlZAFJbS1M6t63X1ncQLqgQTX+MDwg+MqMv1f9qI9opZwiazHrSygGpSBY3jo3d32SG3JJFnG9lYzN8gFv0I6YG6dZzrSzDB33rwvKa05yaMf/wThrTXvcKz76i+SihyrIYEXHg3Ku4od3VsxjTefaPTrCsQXaxMNpON5M/0CUZfQwM2yiMhGSe23h36EJJDr6jSZovx1c+3XTZUYSc44SbFPzlcai19VEv4Hwh7wURfbw841BYKqf5miDFiz5cB7+1Wlhwa4ISFsjzzVCtrZLLQIDAQABo2YwZDASBgNVHRMBAf8ECDAGAQH/AgEBMA4GA1UdDwEB/wQEAwIBBjAdBgNVHQ4EFgQUvCsGYJAc220BIfW1wrUytQaTiPEwHwYDVR0jBBgwFoAUvCsGYJAc220BIfW1wrUytQaTiPEwDQYJKoZIhvcNAQENBQADggEBAHpppRP7OoyI4S2RbQqExargTdbo2xnJYhnK0jqxbBoLPgdDxQ8EHoZCoUwf+8qtGZ0SA8g4OJb8R8ZA8rAI7BjNVwxYo/nEfJKlZYzzfWrb7cceFm6lyGJwjRNs8H3p9WEpS+VA+L4RnswFhyjzz95bdi89SMlFm2BfLGEcpey6lZChWlQYEO0vOoCeXu7f8VStZtwi4vQFfPY0v1niDhgMVT7CwPn/kxWfBtzJngog4C2FI06wINl2nF6Uq+HPfuYIgg9EV8u8jOUBGZQg0O2GlHceJ93nhihaGSvrJ2CBQJO/JNcdnVCvwA+qS19x1+kIdcoSbWJySDI3F6i3+uQ=");
        certificateData2.setIssuerName("CN=myalias");
        certificateData2.setSubjectName("CN=myalias");
        certificateData2.setSerialNumber(new BigInteger("17462121781697028213"));

        try {
            privateKeyMO.setCertificateChain(Arrays.asList(certificateData, certificateData2));
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
        privateKeyMO.setAlias(ssgKeyEntry.getAlias());

        privateKeyMOs.add(privateKeyMO);

        return privateKeyMOs;
    }

    @Override
    public Map<PrivateKeyMO, Functions.BinaryVoid<PrivateKeyMO, RestResponse>> getUnCreatableManagedObjects() {
        CollectionUtils.MapBuilder<PrivateKeyMO, Functions.BinaryVoid<PrivateKeyMO, RestResponse>> builder = CollectionUtils.MapBuilder.builder();

        return builder.map();
    }

    @Test
    public void createPrivateKeysFailed() throws Exception {
        Map<String, PrivateKeyCreationContext> privateKeyCreationContexts = new HashMap<>();

        //invalid algorithm
        PrivateKeyCreationContext privateKeyCreationContext = ManagedObjectFactory.createPrivateKeyCreationContext();
        privateKeyCreationContext.setDn("CN=invalid_algorithm");
        privateKeyCreationContext.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("signatureHashAlgorithm", "INVALID")
                .map());
        privateKeyCreationContexts.put(defaultKeystoreId + ":invalid_algorithm", privateKeyCreationContext);

        //duplicate alias
        privateKeyCreationContext = ManagedObjectFactory.createPrivateKeyCreationContext();
        privateKeyCreationContext.setDn(ssgKeyEntries.get(1).getSubjectDN());
        privateKeyCreationContexts.put(ssgKeyEntries.get(1).getId(), privateKeyCreationContext);

        //invalid expiry days
        privateKeyCreationContext = ManagedObjectFactory.createPrivateKeyCreationContext();
        privateKeyCreationContext.setDn("CN=invalid_expiry_days");
        privateKeyCreationContext.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("daysUntilExpiry", -1)
                .map());
        privateKeyCreationContexts.put(defaultKeystoreId + ":invalid_expiry_days", privateKeyCreationContext);

        //invalid dn
        privateKeyCreationContext = ManagedObjectFactory.createPrivateKeyCreationContext();
        privateKeyCreationContext.setDn("invalid_dn");
        privateKeyCreationContexts.put(defaultKeystoreId + ":invalid_dn", privateKeyCreationContext);

        for (Map.Entry<String, PrivateKeyCreationContext> keyCreationContextEntry : privateKeyCreationContexts.entrySet()) {
            RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/" + keyCreationContextEntry.getKey(), HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(keyCreationContextEntry.getValue())));

            Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
            Assert.assertEquals("Expected successful response", 400, response.getStatus());
        }
    }

    @Override
    public Map<PrivateKeyMO, Functions.BinaryVoid<PrivateKeyMO, RestResponse>> getUnUpdateableManagedObjects() {
        CollectionUtils.MapBuilder<PrivateKeyMO, Functions.BinaryVoid<PrivateKeyMO, RestResponse>> builder = CollectionUtils.MapBuilder.builder();

        return builder.map();
    }

    @Override
    public Map<String, Functions.BinaryVoid<String, RestResponse>> getUnGettableManagedObjectIds() {
        CollectionUtils.MapBuilder<String, Functions.BinaryVoid<String, RestResponse>> builder = CollectionUtils.MapBuilder.builder();
        builder.put("123"+ssgKeyEntries.get(0).getId(), new Functions.BinaryVoid<String, RestResponse>() {
            @Override
            public void call(String s, RestResponse restResponse) {
                Assert.assertEquals("Expected successful response", 400, restResponse.getStatus());
            }
        }).put(ssgKeyEntries.get(0).getId()+"123", new Functions.BinaryVoid<String, RestResponse>() {
            @Override
            public void call(String s, RestResponse restResponse) {
                Assert.assertEquals("Expected successful response", 404, restResponse.getStatus());
            }
        });
        return builder.map();
    }

    @Override
    public Map<String, Functions.BinaryVoid<String, RestResponse>> getUnDeleteableManagedObjectIds() {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, Functions.BinaryVoid<String, RestResponse>> getBadListQueries() {
        return Collections.emptyMap();
    }

    @Override
    public List<String> getDeleteableManagedObjectIDs() {
        return Functions.map(ssgKeyEntries, new Functions.Unary<String, SsgKeyEntry>() {
            @Override
            public String call(SsgKeyEntry ssgKeyEntry) {
                return ssgKeyEntry.getId();
            }
        });
    }

    @Override
    @Test
    public void testDeleteNoExistingEntity() throws Exception {
        Goid badId = new Goid(0, 0);
        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/" + badId.toString() + ":badName", HttpMethod.DELETE, null, "");

        Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals("Expected successful response", 404, response.getStatus());
        Assert.assertNotNull("Expected not null response body", response.getBody());
    }

    @Override
    public String getResourceUri() {
        return "privateKeys";
    }

    @Override
    public String getType() {
        return EntityType.SSG_KEY_ENTRY.name();
    }

    @Override
    public String getExpectedTitle(String id) throws Exception {
        String keyInfo[] = id.split(":");
        SsgKeyEntry entity = ssgKeyStoreManager.lookupKeyByKeyAlias(keyInfo[1], Goid.parseGoid(keyInfo[0]));
        Assert.assertNotNull(entity);
        return entity.getName();
    }

    @Override
    public void verifyLinks(String id, List<Link> links) throws Exception {
        String keyInfo[] = id.split(":");
        SsgKeyEntry entity = ssgKeyStoreManager.lookupKeyByKeyAlias(keyInfo[1], Goid.parseGoid(keyInfo[0]));
        Assert.assertNotNull(entity);
    }

    @Override
    public void verifyEntity(String id, PrivateKeyMO managedObject) throws Exception {
        String keyInfo[] = id.split(":");
        SsgKeyEntry entity = null;
        try {
            entity = ssgKeyStoreManager.lookupKeyByKeyAlias(keyInfo[1], Goid.parseGoid(keyInfo[0]));
        } catch (ObjectNotFoundException e) {
            //do nothing
        }
        if (managedObject == null) {
            Assert.assertNull(entity);
        } else {
            Assert.assertNotNull(entity);

            Assert.assertEquals(entity.getId(), managedObject.getId());
            Assert.assertEquals(entity.getAlias(), managedObject.getAlias());
            if (managedObject.getKeystoreId() != null) {
                Assert.assertEquals(entity.getKeystoreId().toString(), managedObject.getKeystoreId());
            }
            if (managedObject.getProperties() != null && managedObject.getProperties().get("keyAlgorithm") != null && !entity.isRestrictedAccess()) {
                Assert.assertEquals(entity.getPrivate().getAlgorithm(), managedObject.getProperties().get("keyAlgorithm"));
            }
            Assert.assertEquals(entity.getCertificateChain().length, managedObject.getCertificateChain().size());
            for (int i = 0; i < entity.getCertificateChain().length; i++) {
                X509Certificate cert = entity.getCertificateChain()[i];
                CertificateData certificateData = managedObject.getCertificateChain().get(i);
                Assert.assertEquals(cert.getSubjectDN().getName().replaceAll(", ", ","), certificateData.getSubjectName());
                Assert.assertEquals(cert.getIssuerDN().getName().replaceAll(", ", ","), certificateData.getIssuerName());
                Assert.assertEquals(cert.getSerialNumber(), certificateData.getSerialNumber());
                org.junit.Assert.assertArrayEquals(cert.getEncoded(), certificateData.getEncoded());
            }
        }
    }

    private void verifyEntity(String id, PrivateKeyCreationContext privateKeyCreationContext) throws KeyStoreException, FindException {
        String keyInfo[] = id.split(":");
        SsgKeyEntry entity = null;
        try {
            entity = ssgKeyStoreManager.lookupKeyByKeyAlias(keyInfo[1], Goid.parseGoid(keyInfo[0]));
        } catch (ObjectNotFoundException e) {
            //do nothing
        }
        if (privateKeyCreationContext == null) {
            Assert.assertNull(entity);
        } else {
            Assert.assertNotNull(entity);
            Assert.assertEquals(entity.getSubjectDN(), privateKeyCreationContext.getDn());
        }
    }

    @Override
    public Map<String, List<String>> getListQueryAndExpectedResults() throws FindException {
        return CollectionUtils.MapBuilder.<String, List<String>>builder()
                .put("", Functions.map(ssgKeyEntries, new Functions.Unary<String, SsgKeyEntry>() {
                    @Override
                    public String call(SsgKeyEntry ssgKeyEntry) {
                        return ssgKeyEntry.getId();
                    }
                }))
                .put("alias=" + URLEncoder.encode(ssgKeyEntries.get(0).getAlias()), Arrays.asList(ssgKeyEntries.get(0).getId()))
                .put("alias=" + URLEncoder.encode(ssgKeyEntries.get(0).getAlias()) + "&alias=" + URLEncoder.encode(ssgKeyEntries.get(1).getAlias()), Functions.map(ssgKeyEntries.subList(0, 2), new Functions.Unary<String, SsgKeyEntry>() {
                    @Override
                    public String call(SsgKeyEntry ssgKeyEntry) {
                        return ssgKeyEntry.getId();
                    }
                }))
                .put("alias=banName", Collections.<String>emptyList())
                .map();
    }

    @Test
    public void setSpecialPurposeTest() throws Exception {
        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/" + ssgKeyEntries.get(0).getId() + "/specialPurpose?purpose=AUDIT_VIEWER", HttpMethod.PUT, null, "");

        verifyMOResponse(ssgKeyEntries.get(0).getId(), response);

        String prop = clusterPropManager.getProperty("keyStore.auditViewer.alias");
        assertEquals("Special purpose id:", ssgKeyEntries.get(0).getId(), prop);
    }

    @Test
    public void setSpecialPurposeOnTwokeysTest() throws Exception {
        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/" + ssgKeyEntries.get(0).getId() + "/specialPurpose?purpose=AUDIT_VIEWER", HttpMethod.PUT, null, "");

        verifyMOResponse(ssgKeyEntries.get(0).getId(), response);

        String prop = clusterPropManager.getProperty("keyStore.auditViewer.alias");
        assertEquals("Special purpose id:", ssgKeyEntries.get(0).getId(), prop);

        response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/" + ssgKeyEntries.get(1).getId() + "/specialPurpose?purpose=AUDIT_VIEWER", HttpMethod.PUT, null, "");

        verifyMOResponse(ssgKeyEntries.get(1).getId(), response);

        prop = clusterPropManager.getProperty("keyStore.auditViewer.alias");
        assertEquals("Special purpose id:", ssgKeyEntries.get(1).getId(), prop);
    }

    @Test
    public void setSpecialPurposeTestUnknownPurposeFail() throws Exception {
        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/" + ssgKeyEntries.get(0).getId() + "/specialPurpose?purpose=BAD_PURPOSE", HttpMethod.PUT, null, "");

        Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals(400, response.getStatus());
    }

    @Test
    public void setSpecialPurposeNotExistingFail() throws Exception {
        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/" + defaultKeystoreId + ":badAlias" + "/specialPurpose?purpose=BAD_PURPOSE", HttpMethod.PUT, null, "");

        Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals(404, response.getStatus());
    }

    @Test
    public void exportKey() throws Exception {
        PrivateKeyExportContext privateKeyExportContext = new PrivateKeyExportContext();
        String password = "password";
        String alias = ssgKeyEntries.get(0).getAlias();
        privateKeyExportContext.setPassword(password);

        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/" + ssgKeyEntries.get(0).getId() + "/export", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), objectToString(privateKeyExportContext));

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Item<PrivateKeyExportResult> item = MarshallingUtils.unmarshal(Item.class, source);

        PrivateKeyExportResult export = item.getContent();
        assertEquals("Private Key identifier:", ssgKeyEntries.get(0).getId(), item.getId());
        Assert.assertNotNull(export.getPkcs12Data());
        Assert.assertTrue(export.getPkcs12Data().length > 0);

        //test that the key is ok
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(new ByteArrayInputStream(export.getPkcs12Data()), password.toCharArray());
        Assert.assertTrue(ks.containsAlias(alias));
    }

    @Test
    public void exportKeyNoPassword() throws Exception {
        PrivateKeyExportContext privateKeyExportContext = new PrivateKeyExportContext();
        String password = "";
        String alias = ssgKeyEntries.get(0).getAlias();
        privateKeyExportContext.setPassword(password);

        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/" + ssgKeyEntries.get(0).getId() + "/export", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), objectToString(privateKeyExportContext));

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Item<PrivateKeyExportResult> item = MarshallingUtils.unmarshal(Item.class, source);

        PrivateKeyExportResult export = item.getContent();
        assertEquals("Private Key identifier:", ssgKeyEntries.get(0).getId(), item.getId());
        Assert.assertNotNull(export.getPkcs12Data());
        Assert.assertTrue(export.getPkcs12Data().length > 0);

        //test that the key is ok
        try {
            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(new ByteArrayInputStream(export.getPkcs12Data()), password.toCharArray());
            Assert.assertTrue(ks.containsAlias(alias));
        } catch (IOException e) {
            throw new IOException("Exception caused by Pkcs12Data: " + response.getBody(), e);
        }
    }

    @Test
    public void exportKeyDifferentAlias() throws Exception {
        PrivateKeyExportContext privateKeyExportContext = new PrivateKeyExportContext();
        String password = "password";
        String alias = "myNewAlias";
        privateKeyExportContext.setPassword(password);
        privateKeyExportContext.setAlias(alias);

        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/" + ssgKeyEntries.get(0).getId() + "/export", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), objectToString(privateKeyExportContext));

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Item<PrivateKeyExportResult> item = MarshallingUtils.unmarshal(Item.class, source);

        PrivateKeyExportResult export = item.getContent();
        assertEquals("Private Key identifier:", ssgKeyEntries.get(0).getId(), item.getId());
        Assert.assertNotNull(export.getPkcs12Data());
        Assert.assertTrue(export.getPkcs12Data().length > 0);

        //test that the key is ok
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(new ByteArrayInputStream(export.getPkcs12Data()), password.toCharArray());
        Assert.assertTrue(ks.containsAlias(alias));
    }

    @Test
    public void exportKeyFailNotExisting() throws Exception {
        PrivateKeyExportContext privateKeyExportContext = new PrivateKeyExportContext();
        String password = "password";
        privateKeyExportContext.setPassword(password);
        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/" + defaultKeystoreId + ":badAlias" + "/export", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), objectToString(privateKeyExportContext));

        Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals(404, response.getStatus());
    }

    @Test
    public void importKey() throws Exception {
        PrivateKeyImportContext privateKeyImportContext = new PrivateKeyImportContext();
        privateKeyImportContext.setPkcs12Data( IOUtils.slurpStream( this.getClass().getResourceAsStream( "testkey.p12" ) ));
        privateKeyImportContext.setPassword("");
        privateKeyImportContext.setAlias("testkey");

        String importId = defaultKeystoreId.toString() + ":importtest";
        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/" + importId + "/import", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), objectToString(privateKeyImportContext));

        verifyMOResponse(importId, response);
    }

    @Test
    public void importKeyWithoutSpecifyingAlias() throws Exception {
        PrivateKeyImportContext privateKeyImportContext = new PrivateKeyImportContext();
        privateKeyImportContext.setPkcs12Data(IOUtils.slurpStream(this.getClass().getResourceAsStream("testkey.p12")));
        privateKeyImportContext.setPassword("");

        String importId = defaultKeystoreId.toString() + ":importtest";
        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/" + importId + "/import", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), objectToString(privateKeyImportContext));

        verifyMOResponse(importId, response);
    }

    @Test
    public void importKeyExistingFail() throws Exception {
        PrivateKeyImportContext privateKeyImportContext = new PrivateKeyImportContext();
        privateKeyImportContext.setPkcs12Data(IOUtils.slurpStream(this.getClass().getResourceAsStream("testkey.p12")));
        privateKeyImportContext.setPassword("");
        privateKeyImportContext.setAlias("testkey");

        String importId = defaultKeystoreId.toString() + ":alice";
        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/" + importId + "/import", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), objectToString(privateKeyImportContext));

        Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals(403, response.getStatus());
    }

    @Test
    public void importKeyBadPasswordFail() throws Exception {
        PrivateKeyImportContext privateKeyImportContext = new PrivateKeyImportContext();
        privateKeyImportContext.setPkcs12Data(IOUtils.slurpStream(this.getClass().getResourceAsStream("testkey.p12")));
        privateKeyImportContext.setPassword("badPassword");
        privateKeyImportContext.setAlias("testkey");

        String importId = defaultKeystoreId.toString() + ":importtest";
        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/" + importId + "/import", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), objectToString(privateKeyImportContext));

        Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals(403, response.getStatus());
    }

    @Test
    public void exportImportKey() throws Exception {
        //EXPORT
        PrivateKeyExportContext privateKeyExportContext = new PrivateKeyExportContext();
        String password = "password";
        String alias = "myNewAlias";
        privateKeyExportContext.setPassword(password);
        privateKeyExportContext.setAlias(alias);

        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/" + ssgKeyEntries.get(0).getId() + "/export", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), objectToString(privateKeyExportContext));

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Item<PrivateKeyExportResult> item = MarshallingUtils.unmarshal(Item.class, source);

        PrivateKeyExportResult export = item.getContent();
        assertEquals("Private Key identifier:", ssgKeyEntries.get(0).getId(), item.getId());
        Assert.assertNotNull(export.getPkcs12Data());
        Assert.assertTrue(export.getPkcs12Data().length > 0);

        //test that the key is ok
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(new ByteArrayInputStream(export.getPkcs12Data()), password.toCharArray());
        Assert.assertTrue(ks.containsAlias(alias));

        //IMPORT
        PrivateKeyImportContext privateKeyImportContext = new PrivateKeyImportContext();
        privateKeyImportContext.setPkcs12Data(export.getPkcs12Data());
        privateKeyImportContext.setPassword(password);
        privateKeyImportContext.setAlias(alias);

        String importId = defaultKeystoreId.toString() + ":importtest";
        response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/" + importId + "/import", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), objectToString(privateKeyImportContext));

        verifyMOResponse(importId, response);
    }

    @Test
    public void generateCSRSpecifyDN() throws Exception {
        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/" + ssgKeyEntries.get(0).getId() + "/generateCSR", "csrSubjectDN=CN%3DmyCSRCN", HttpMethod.GET, ContentType.APPLICATION_XML.toString(), "");

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Item<PrivateKeyGenerateCsrResult> item = MarshallingUtils.unmarshal(Item.class, source);

        PrivateKeyGenerateCsrResult privateKeyGenerateCsrResult = item.getContent();
        assertEquals("Private Key identifier:", ssgKeyEntries.get(0).getId(), item.getId());
        Assert.assertNotNull(privateKeyGenerateCsrResult.getCsrData());
        Assert.assertTrue(privateKeyGenerateCsrResult.getCsrData().length > 0);
    }

    @Test
    public void generateCSRSpecifyDNAndHash() throws Exception {
        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/" + ssgKeyEntries.get(0).getId() + "/generateCSR", "csrSubjectDN=CN%3DmyCSRCN&signatureHash=SHA256", HttpMethod.GET, ContentType.APPLICATION_XML.toString(), "");

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Item<PrivateKeyGenerateCsrResult> item = MarshallingUtils.unmarshal(Item.class, source);

        PrivateKeyGenerateCsrResult privateKeyGenerateCsrResult = item.getContent();
        assertEquals("Private Key identifier:", ssgKeyEntries.get(0).getId(), item.getId());
        Assert.assertNotNull(privateKeyGenerateCsrResult.getCsrData());
        Assert.assertTrue(privateKeyGenerateCsrResult.getCsrData().length > 0);
    }

    @Test
    public void generateCSR() throws Exception {
        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/" + ssgKeyEntries.get(0).getId() + "/generateCSR", "", HttpMethod.GET, ContentType.APPLICATION_XML.toString(), "");

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Item<PrivateKeyGenerateCsrResult> item = MarshallingUtils.unmarshal(Item.class, source);

        PrivateKeyGenerateCsrResult privateKeyGenerateCsrResult = item.getContent();
        assertEquals("Private Key identifier:", ssgKeyEntries.get(0).getId(), item.getId());
        Assert.assertNotNull(privateKeyGenerateCsrResult.getCsrData());
        Assert.assertTrue(privateKeyGenerateCsrResult.getCsrData().length > 0);
    }

    @BugId("SSG-8539")
    @Test
    public void generateCSRBadSignature() throws Exception {
        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/" + ssgKeyEntries.get(0).getId() + "/generateCSR", "signatureHash=Invalid", HttpMethod.GET, ContentType.APPLICATION_XML.toString(), "");

        Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals(400, response.getStatus());
    }

    @BugId("SSG-8539")
    @Test
    public void generateCSRBadCSRSubjectDN() throws Exception {
        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/" + ssgKeyEntries.get(0).getId() + "/generateCSR", "csrSubjectDN=Invalid", HttpMethod.GET, ContentType.APPLICATION_XML.toString(), "");

        Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals(400, response.getStatus());
    }

    @Test
    public void signCert() throws Exception {
        byte[] csrBytes = defaultKeyStore.makeCertificateSigningRequest(ssgKeyEntries.get(1).getAlias(), new CertGenParams(
                ssgKeyEntries.get(1).getCertificate().getSubjectX500Principal(),
                356,
                false,
                null)).getEncoded();
        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/" + ssgKeyEntries.get(0).getId() + "/signCert", "subjectDN=CN%3Dname&signatureHash=SHA256", HttpMethod.PUT, "application/x-pem-file", CertUtils.encodeCsrAsPEM(csrBytes));

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Item<PrivateKeySignCsrResult> item = MarshallingUtils.unmarshal(Item.class, source);

        PrivateKeySignCsrResult privateKeySignCsrResult = item.getContent();
        assertEquals("Private Key identifier:", ssgKeyEntries.get(0).getId(), item.getId());
        Assert.assertNotNull(privateKeySignCsrResult.getCertData());
        Assert.assertTrue(privateKeySignCsrResult.getCertData().length() > 0);
    }

    @Test
    public void signCertNohash() throws Exception {
        byte[] csrBytes = defaultKeyStore.makeCertificateSigningRequest(ssgKeyEntries.get(1).getAlias(), new CertGenParams(
                ssgKeyEntries.get(1).getCertificate().getSubjectX500Principal(),
                356,
                false,
                null)).getEncoded();
        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/" + ssgKeyEntries.get(0).getId() + "/signCert", "subjectDN=CN%3Dname", HttpMethod.PUT, "application/x-pem-file", CertUtils.encodeCsrAsPEM(csrBytes));

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Item<PrivateKeySignCsrResult> item = MarshallingUtils.unmarshal(Item.class, source);

        PrivateKeySignCsrResult privateKeySignCsrResult = item.getContent();
        assertEquals("Private Key identifier:", ssgKeyEntries.get(0).getId(), item.getId());
        Assert.assertNotNull(privateKeySignCsrResult.getCertData());
        Assert.assertTrue(privateKeySignCsrResult.getCertData().length() > 0);
    }

    @Test
    public void signCertNoDN() throws Exception {
        byte[] csrBytes = defaultKeyStore.makeCertificateSigningRequest(ssgKeyEntries.get(1).getAlias(), new CertGenParams(
                ssgKeyEntries.get(1).getCertificate().getSubjectX500Principal(),
                356,
                false,
                null)).getEncoded();
        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/" + ssgKeyEntries.get(0).getId() + "/signCert", "", HttpMethod.PUT, "application/x-pem-file", CertUtils.encodeCsrAsPEM(csrBytes));

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Item<PrivateKeySignCsrResult> item = MarshallingUtils.unmarshal(Item.class, source);

        PrivateKeySignCsrResult privateKeySignCsrResult = item.getContent();
        assertEquals("Private Key identifier:", ssgKeyEntries.get(0).getId(), item.getId());
        Assert.assertNotNull(privateKeySignCsrResult.getCertData());
        Assert.assertTrue(privateKeySignCsrResult.getCertData().length() > 0);
    }

    @Test
    public void signCertBadDN() throws Exception {
        byte[] csrBytes = defaultKeyStore.makeCertificateSigningRequest(ssgKeyEntries.get(1).getAlias(), new CertGenParams(
                ssgKeyEntries.get(1).getCertificate().getSubjectX500Principal(),
                356,
                false,
                null)).getEncoded();
        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/" + ssgKeyEntries.get(0).getId() + "/signCert", "subjectDN=name", HttpMethod.PUT, "application/x-pem-file", CertUtils.encodeCsrAsPEM(csrBytes));

        Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals(400, response.getStatus());
    }

    @Test
    public void signCertBadHash() throws Exception {
        byte[] csrBytes = defaultKeyStore.makeCertificateSigningRequest(ssgKeyEntries.get(1).getAlias(), new CertGenParams(
                ssgKeyEntries.get(1).getCertificate().getSubjectX500Principal(),
                356,
                false,
                null)).getEncoded();
        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/" + ssgKeyEntries.get(0).getId() + "/signCert", "signatureHash=INVALID", HttpMethod.PUT, "application/x-pem-file", CertUtils.encodeCsrAsPEM(csrBytes));

        Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals(400, response.getStatus());
    }

    @Test
    public void signCertBadExpiry() throws Exception {
        byte[] csrBytes = defaultKeyStore.makeCertificateSigningRequest(ssgKeyEntries.get(1).getAlias(), new CertGenParams(
                ssgKeyEntries.get(1).getCertificate().getSubjectX500Principal(),
                356,
                false,
                null)).getEncoded();
        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/" + ssgKeyEntries.get(0).getId() + "/signCert", "expiryAge=-10", HttpMethod.PUT, "application/x-pem-file", CertUtils.encodeCsrAsPEM(csrBytes));

        Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals(400, response.getStatus());
    }

    @Test
    public void signCertBadCert() throws Exception {
        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/" + ssgKeyEntries.get(0).getId() + "/signCert", "", HttpMethod.PUT, "application/x-pem-file", "BADCERT");

        Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals(400, response.getStatus());
    }
}
