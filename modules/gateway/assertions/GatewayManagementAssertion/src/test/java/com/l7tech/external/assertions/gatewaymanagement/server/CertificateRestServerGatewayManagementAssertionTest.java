package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.TestDocuments;
import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.Reference;
import com.l7tech.gateway.api.References;
import com.l7tech.gateway.api.TrustedCertificateMO;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.server.identity.cert.TestTrustedCertManager;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.junit.*;
import org.mockito.InjectMocks;
import org.w3c.dom.Document;

import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;

import static junit.framework.Assert.assertEquals;

/**
 *
 */
public class CertificateRestServerGatewayManagementAssertionTest extends ServerRestGatewayManagementAssertionTestBase {
    private static final Logger logger = Logger.getLogger(CertificateRestServerGatewayManagementAssertionTest.class.getName());

    private static final TrustedCert cert = new TrustedCert();
    private static TestTrustedCertManager trustedCertManager;
    private static final String certBasePath = "trustedCertificates/";

    @InjectMocks
    protected CertificateResourceFactory certificateResourceFactory;

    @Before
    public void before() throws Exception {
        super.before();
        trustedCertManager = applicationContext.getBean("trustedCertManager", TestTrustedCertManager.class);
        cert.setGoid(new Goid(0, 1234L));
        cert.setName("Alice");
        cert.setCertificate(TestDocuments.getWssInteropAliceCert());
        cert.setRevocationCheckPolicyType(TrustedCert.PolicyUsageType.USE_DEFAULT);
        trustedCertManager.save(cert);
    }

    @After
    public void after() throws Exception {
        super.after();
        Collection<EntityHeader> entities = new ArrayList<>(trustedCertManager.findAllHeaders());
        for (EntityHeader entity : entities) {
            trustedCertManager.delete(entity.getGoid());
        }
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        ServerRestGatewayManagementAssertionTestBase.beforeClass();
    }

    @Test
    public void getEntityTest() throws Exception {
        RestResponse response = processRequest(certBasePath + cert.getId(), HttpMethod.GET, null, "");
        logger.info(response.toString());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Reference reference = MarshallingUtils.unmarshal(Reference.class, source);
        TrustedCertificateMO result = (TrustedCertificateMO) reference.getResource();

        assertEquals("Certificate identifier:", cert.getId(), result.getId());
        assertEquals("Certificate name:", cert.getName(), result.getName());
    }

    @Test
    public void createEntityTest() throws Exception {

        TrustedCertificateMO createObject = certificateResourceFactory.asResource(cert);
        createObject.setId(null);
        createObject.setName("New Cert");
        Document request = ManagedObjectFactory.write(createObject);
        RestResponse response = processRequest(certBasePath, HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));

        TrustedCert createdEntity = trustedCertManager.findByPrimaryKey(new Goid(getFirstReferencedGoid(response)));

        assertEquals("Certificate name:", createdEntity.getName(), createObject.getName());
    }

    @Test
    public void createEntityInvalidCertificateDataTest() throws Exception {

        TrustedCertificateMO createObject = certificateResourceFactory.asResource(cert);
        createObject.setId(null);
        createObject.setName("New Cert");
        createObject.getCertificateData().setEncoded("bad".getBytes());
        Document request = ManagedObjectFactory.write(createObject);
        RestResponse response = processRequest(certBasePath, HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));

        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
        Assert.assertTrue(response.getBody().contains("INVALID_VALUES"));
    }

    @Test
    public void createEntityWithIDTest() throws Exception {

        Goid goid = new Goid(12345678L, 5678);
        TrustedCertificateMO createObject = certificateResourceFactory.asResource(cert);
        createObject.setId(null);
        createObject.setName("New Cert");
        Document request = ManagedObjectFactory.write(createObject);
        RestResponse response = processRequest(certBasePath + goid, HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));

        assertEquals("Created certificate goid:", goid.toString(), getFirstReferencedGoid(response));

        TrustedCert createdEntity = trustedCertManager.findByPrimaryKey(goid);
        assertEquals("Certificate name:", createdEntity.getName(), createObject.getName());
    }

    @Test
    public void updateEntityTest() throws Exception {

        // get
        RestResponse responseGet = processRequest(certBasePath + cert.getId(), HttpMethod.GET, null, "");
        Assert.assertEquals(AssertionStatus.NONE, responseGet.getAssertionStatus());
        final StreamSource source = new StreamSource(new StringReader(responseGet.getBody()));
        TrustedCertificateMO entityGot = (TrustedCertificateMO) MarshallingUtils.unmarshal(Reference.class, source).getResource();

        // update
        entityGot.setName(entityGot.getName() + "_mod");
        RestResponse response = processRequest(certBasePath + entityGot.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(entityGot)));

        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        assertEquals("Created Certificate goid:", entityGot.getId(), getFirstReferencedGoid(response));

        // check entity
        TrustedCert updatedEntity = trustedCertManager.findByPrimaryKey(new Goid(getFirstReferencedGoid(response)));

        assertEquals("Certificate id:", updatedEntity.getId(), cert.getId());
        assertEquals("Certificate name:", updatedEntity.getName(), entityGot.getName());
    }

    @Test
    public void deleteEntityTest() throws Exception {

        RestResponse response = processRequest(certBasePath + cert.getId(), HttpMethod.DELETE, null, "");
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        // check entity
        Assert.assertNull(trustedCertManager.findByPrimaryKey(cert.getGoid()));
    }

    @Test
    public void listEntitiesTest() throws Exception {

        RestResponse response = processRequest(certBasePath, HttpMethod.GET, null, "");
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Reference<References> reference = MarshallingUtils.unmarshal(Reference.class, source);

        // check entity
        Assert.assertEquals(1, reference.getResource().getReferences().size());
    }
}
