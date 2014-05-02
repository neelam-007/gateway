package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.TestDocuments;
import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.security.keystore.SsgKeyFinderStub;
import com.l7tech.server.security.keystore.SsgKeyStoreManagerStub;
import com.l7tech.util.CollectionUtils;
import org.apache.http.entity.ContentType;
import org.junit.*;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.StringReader;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Logger;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;

/**
 *
 */
public class PrivateKeyRestServerGatewayManagementAssertionTest extends ServerRestGatewayManagementAssertionTestBase {
    private static final Logger logger = Logger.getLogger(PrivateKeyRestServerGatewayManagementAssertionTest.class.getName());

    private static SsgKeyEntry ssgKeyEntry;
    private static SsgKeyStoreManagerStub keyStoreManagerStub;
    private static ClusterPropertyManager clusterPropManager;
    private static SsgKeyFinderStub keyFinder;
    private static final String privateKeyBasePath = "privateKeys/";
    protected static final Goid keyStoreGoid = new Goid(0,0);

    protected PrivateKeyResourceFactory privateKeyResourceFactory;

    @Before
    public void before() throws Exception {
        super.before();

        keyStoreManagerStub = applicationContext.getBean("ssgKeyStoreManager", SsgKeyStoreManagerStub.class);
        ssgKeyEntry = new SsgKeyEntry(keyStoreGoid, "alice", new X509Certificate[]{TestDocuments.getWssInteropAliceCert()}, TestDocuments.getWssInteropAliceKey());
        keyFinder = new SsgKeyFinderStub(Arrays.asList(ssgKeyEntry));
        keyStoreManagerStub.setKeyFinder(keyFinder);

        clusterPropManager = applicationContext.getBean("clusterPropertyManager", ClusterPropertyManager.class);


    }

    @After
    public void after() throws Exception {
        super.after();
        keyStoreManagerStub.setKeyFinder(new SsgKeyFinderStub());

        Collection<EntityHeader> props = new ArrayList<>(clusterPropManager.findAllHeaders());
        for (EntityHeader prop : props) {
            clusterPropManager.delete(prop.getGoid());
        }
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        ServerRestGatewayManagementAssertionTestBase.beforeClass();
    }

    @Test
    public void getEntityTest() throws Exception {
        RestResponse response = processRequest(privateKeyBasePath + ssgKeyEntry.getId(), HttpMethod.GET, null, "");
        logger.info(response.toString());

        PrivateKeyMO result = getMO(response);

        assertEquals("Private Key identifier:", ssgKeyEntry.getId(), result.getId());
        assertEquals("Private Key alias:", ssgKeyEntry.getAlias(), result.getAlias());
    }

    @Test
    public void setSpecialPurposeTest() throws Exception {
        RestResponse response = processRequest(privateKeyBasePath + ssgKeyEntry.getId() + "/specialPurpose?purpose=AUDIT_VIEWER", HttpMethod.PUT, null, "");
        logger.info(response.toString());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Item item = MarshallingUtils.unmarshal(Item.class, source);

        String prop = clusterPropManager.getProperty("keyStore.auditViewer.alias");

        assertEquals("Private Key identifier:", ssgKeyEntry.getId(), item.getId());
        assertEquals("Private Key alias:", ssgKeyEntry.getAlias(), item.getName());
        assertEquals("Special purpose id:", ssgKeyEntry.getId(), prop);
    }


    private PrivateKeyMO getMO(RestResponse response) throws JAXBException, IOException {
        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Item item = MarshallingUtils.unmarshal(Item.class, source);
        return (PrivateKeyMO) item.getContent();
    }

    private String moToString(PrivateKeyMO mo) throws JAXBException, IOException {
        JAXBContext jsxb = JAXBContext.newInstance(PrivateKeyMO.class);
        final DOMResult result = new DOMResult();
        jsxb.createMarshaller().marshal(mo, result);

        return XmlUtil.nodeToString(result.getNode());
    }

    @Test
    public void updateEntityTest() throws Exception {

        // get
        RestResponse responseGet = processRequest(privateKeyBasePath + ssgKeyEntry.getId(), HttpMethod.GET, null, "");
        Assert.assertEquals(AssertionStatus.NONE, responseGet.getAssertionStatus());
        logger.info(responseGet.toString());
        PrivateKeyMO entityGot = getMO( responseGet);

        // update
        X509Certificate[] certs = new X509Certificate[]{TestDocuments.getWssInteropBobCert()};
        entityGot.setCertificateChain(CollectionUtils.list(ManagedObjectFactory.createCertificateData(TestDocuments.getWssInteropBobCert())));
        RestResponse response = processRequest(privateKeyBasePath + entityGot.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),moToString(entityGot) );

        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        logger.info(response.toString());
        assertEquals("Created Private Key goid:", entityGot.getId(), getFirstReferencedGoid(response));

        // check entity
        SsgKeyEntry updatedEntity = keyFinder.getCertificateChain(entityGot.getAlias());

        assertEquals("Private Key id:", ssgKeyEntry.getId(), updatedEntity.getId());
        assertEquals("Private Key name:", ssgKeyEntry.getName(), updatedEntity.getName());
        assertArrayEquals("Private Certificate Chain:", certs, updatedEntity.getCertificateChain());
    }

    @Test
    public void deleteEntityTest() throws Exception {

        RestResponse response = processRequest(privateKeyBasePath + ssgKeyEntry.getId(), HttpMethod.DELETE, null, "");
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        // check entity
        Assert.assertEquals(keyFinder.getAliases().size(), 0);
    }

    @Test
    public void listEntitiesTest() throws Exception {

        RestResponse response = processRequest(privateKeyBasePath, HttpMethod.GET, null, "");

        logger.info(response.toString());
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        ItemsList<PrivateKeyMO> item = MarshallingUtils.unmarshal(ItemsList.class, source);

        // check entity
        Assert.assertEquals(keyFinder.getAliases().size(), item.getContent().size());
    }
}
