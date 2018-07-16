package com.l7tech.external.assertions.mongodb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.logging.Logger;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.mongodb.entity.MongoDBConnectionEntity;
import com.l7tech.external.assertions.mongodb.entity.MongoDBConnectionEntityAdmin;
import com.l7tech.external.assertions.mongodb.entity.MongoDBConnectionEntityAdminImpl;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.GenericEntityHeader;
import com.l7tech.policy.exporter.ExternalReferenceFinder;
import com.l7tech.util.InvalidDocumentFormatException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.w3c.dom.Element;

/**
 * Created by chaja24 on 9/14/2015.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({MongoDBConnectionManager.class, MongoDBConnectionEntityAdminImpl.class} )
public class MongoDBAssertionExternalReferenceTest {
    private static final Logger log = Logger.getLogger(MongoDBAssertionTest.class.getName());


    private ExternalReferenceFinder mockFinder;
    private MongoDBConnectionEntity  mockEntity;
    private Goid goid = new Goid("c06120de5e0fbff8775222316f93b856");

    private String mongoConnectionName = "test connection";
    private String mongoConnectionDBName = "test";
    private String mongoServerAddress = "server1";
    private String mongoConnectionPort = "27017";
    private String mongoConnectionDefaultReadPreference = "Primary";
    private EntityManager<MongoDBConnectionEntity, GenericEntityHeader> mockEntityManager;
    private MongoDBConnectionEntityAdmin mockAdmin;
    private MongoDBAssertion mockAssertion;
    private Collection<MongoDBConnectionEntity> list;
    private MongoDBReference reference;


    // Note: we leave out the attribute: <Classname>com.l7tech.external.assertions.mongodb.entity.MongoDBConnectionEntity</Classname>
    // for the following 3 sample external refernce XMLs
    // because of the difficulty of calling when(mockEntity.getEntityClassName()) which is a final method and cannot be called by Mockito.
    private static final String REF_EL_BASIC_CLIENT_NO_PASSWORD = "    <MongoDBReference RefType=\"com.l7tech.external.assertions.mongodb.MongoDBReference\">\n" +
            "        <GOID>c06120de5e0fbff8775222316f93b856</GOID>\n" +
            "        <ConnectionName>test connection</ConnectionName>\n" +
            "        <Database>test</Database>\n" +
            "        <Servername>server1</Servername>\n" +
            "        <Port>27017</Port>\n" +
            "        <Encryption>NO_ENCRYPTION</Encryption>\n" +
            "        <UsesDefaultKeyStore>false</UsesDefaultKeyStore>\n" +
            "        <UsesNoKey>false</UsesNoKey>\n" +
            "        <ReadPreference>Primary</ReadPreference>\n" +
            "    </MongoDBReference>\n";

    private static final String REF_EL_BASIC_CLIENT_PASSWORD_AND_CERTIFICATE = "    <MongoDBReference RefType=\"com.l7tech.external.assertions.mongodb.MongoDBReference\">\n" +
            "        <GOID>c06120de5e0fbff8775222316f93b856</GOID>\n" +
            "        <ConnectionName>ConnectionPasswordAndCertificate</ConnectionName>\n" +
            "        <Database>test</Database>\n" +
            "        <Servername>server1</Servername>\n" +
            "        <Port>27017</Port>\n" +
            "        <Encryption>SSL</Encryption>\n" +
            "        <UsesDefaultKeyStore>true</UsesDefaultKeyStore>\n" +
            "        <UsesNoKey>true</UsesNoKey>\n" +
            "        <ReadPreference>PrimaryPreferred</ReadPreference>\n" +
            "    </MongoDBReference>\n";


    private static final String REF_EL_BASIC_CLIENT_CERTIFICATE_AND_CUSTOM_PRIVATE_KEY = "    <MongoDBReference RefType=\"com.l7tech.external.assertions.mongodb.MongoDBReference\">\n" +
            "        <GOID>c06120de5e0fbff8775222316f93b856</GOID>\n" +
            "        <ConnectionName>ConnectionCertificatePrivateKey</ConnectionName>\n" +
            "        <Database>test</Database>\n" +
            "        <Servername>server1</Servername>\n" +
            "        <Port>27017</Port>\n" +
            "        <Encryption>X509_Auth</Encryption>\n" +
            "        <UsesDefaultKeyStore>false</UsesDefaultKeyStore>\n" +
            "        <UsesNoKey>false</UsesNoKey>\n" +
            "        <KeyAlias>test2alias</KeyAlias>\n" +
            "        <NonDefaultKeyStoreId>00000000000000000000000000000002</NonDefaultKeyStoreId>\n" +
            "        <ReadPreference>Primary</ReadPreference>\n" +
            "    </MongoDBReference>\n";

    private static final String REFERENCES_BEGIN = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<exp:References xmlns:exp=\"http://www.layer7tech.com/ws/policy/export\">\n";
    private static final String REFERENCES_END = "</exp:References>";

    @Before
    public void setUp() throws Exception {
      //  MockitoAnnotations.initMocks(this);
        list = new ArrayList<>();
        mockFinder = mock(ExternalReferenceFinder.class);
        mockAssertion = mock(MongoDBAssertion.class);
        mockEntity = mock(MongoDBConnectionEntity.class);
        mockAdmin = mock(MongoDBConnectionEntityAdmin.class);
        mockEntityManager = mock(EntityManager.class);


        MongoDBConnectionEntityAdminImpl mockConnectionEntityAdminImpl = Mockito.mock(MongoDBConnectionEntityAdminImpl.class);
        PowerMockito.mockStatic(MongoDBConnectionEntityAdminImpl.class);

        when(MongoDBConnectionEntityAdminImpl.getInstance(null)).thenReturn(mockConnectionEntityAdminImpl);
        when(mockConnectionEntityAdminImpl.findByGoid(any(Goid.class))).thenReturn(mockEntity);

        when(mockAssertion.getConnectionGoid()).thenReturn(goid);
        when(mockFinder.getGenericEntityManager(MongoDBConnectionEntity.class)).thenReturn(mockEntityManager);
        when(mockAdmin.findByGoid(mockAssertion.getConnectionGoid())).thenReturn(mockEntity);
        when(mockEntityManager.findByPrimaryKey(mockAssertion.getConnectionGoid())).thenReturn(mockEntity);
        when(mockFinder.getGenericEntityManager(MongoDBConnectionEntity.class)).thenReturn(mockEntityManager);

        when(mockEntity.getGoid()).thenReturn(goid);
        when(mockEntity.getName()).thenReturn(mongoConnectionName);
        when(mockEntity.getUri()).thenReturn(mongoServerAddress);
        when(mockEntity.getAuthType()).thenReturn(MongoDBEncryption.NO_ENCRYPTION.name());  //default no encryption
        when(mockEntity.getDatabaseName()).thenReturn(mongoConnectionDBName);
        when(mockEntity.getPort()).thenReturn(mongoConnectionPort);
        when(mockEntity.getReadPreference()).thenReturn(mongoConnectionDefaultReadPreference);
        when(mockEntity.isEnabled()).thenReturn(Boolean.TRUE);
        reference = new MongoDBReference(mockFinder, mockAssertion);

    }

    @Test
    public void serializeToRefElementTestNoPassword() throws Exception {
        final Element element = XmlUtil.createEmptyDocument("References", "exp", "http://www.layer7tech.com/ws/policy/export").getDocumentElement();
        reference.serializeToRefElement(element);
        final String asXml = XmlUtil.nodeToFormattedString(element);

        assertEquals(REFERENCES_BEGIN + REF_EL_BASIC_CLIENT_NO_PASSWORD + REFERENCES_END, asXml.trim());
    }

    @Test
    public void serializeToRefElementTestWithPasswordAndCertificate() throws Exception {


        when(mockEntity.getGoid()).thenReturn(goid);
        when(mockEntity.getName()).thenReturn("ConnectionPasswordAndCertificate");
        when(mockEntity.getAuthType()).thenReturn(MongoDBEncryption.SSL.name());  //encryption using certificate
        when(mockEntity.isUsesDefaultKeyStore()).thenReturn(true);
        when(mockEntity.isUsesNoKey()).thenReturn(true);
        when(mockEntity.getReadPreference()).thenReturn("PrimaryPreferred");
        reference = new MongoDBReference(mockFinder, mockAssertion);

        final Element element = XmlUtil.createEmptyDocument("References", "exp", "http://www.layer7tech.com/ws/policy/export").getDocumentElement();
        reference.serializeToRefElement(element);
        final String asXml = XmlUtil.nodeToFormattedString(element);

        assertEquals(REFERENCES_BEGIN + REF_EL_BASIC_CLIENT_PASSWORD_AND_CERTIFICATE + REFERENCES_END, asXml.trim());
    }


    @Test
    public void serializeToRefElementTestWithCertificateAndCustomPrivateKey() throws Exception {
        Goid keystoreId = new Goid(0,2);

        when(mockEntity.getGoid()).thenReturn(goid);
        when(mockEntity.getName()).thenReturn("ConnectionCertificatePrivateKey");
        when(mockEntity.getAuthType()).thenReturn(MongoDBEncryption.X509_Auth.name());  //encryption using certificate
        when(mockEntity.isUsesNoKey()).thenReturn(false);
        when(mockEntity.getKeyAlias()).thenReturn("test2alias");
        when(mockEntity.getNonDefaultKeystoreId()).thenReturn(keystoreId);
        when(mockEntity.getReadPreference()).thenReturn("Primary");
        reference = new MongoDBReference(mockFinder, mockAssertion);

        final Element element = XmlUtil.createEmptyDocument("References", "exp", "http://www.layer7tech.com/ws/policy/export").getDocumentElement();
        reference.serializeToRefElement(element);
        final String asXml = XmlUtil.nodeToFormattedString(element);

        assertEquals(REFERENCES_BEGIN + REF_EL_BASIC_CLIENT_CERTIFICATE_AND_CUSTOM_PRIVATE_KEY + REFERENCES_END, asXml.trim());
    }

    @Test
    public void testVerifyReferenceOnMatchingGoidAndConnectionName() throws Exception {

        String matchingConnectionName = mongoConnectionName;
        Goid matchingGoid = goid;

        MongoDBConnectionEntity foundConnectionEntity = mock(MongoDBConnectionEntity.class);
        when(foundConnectionEntity.getName()).thenReturn(matchingConnectionName);
        when(foundConnectionEntity.getGoid()).thenReturn(matchingGoid);

        MongoDBConnectionEntityAdminImpl mockConnectionEntityAdminImpl = Mockito.mock(MongoDBConnectionEntityAdminImpl.class);
        PowerMockito.mockStatic(MongoDBConnectionEntityAdminImpl.class);

        when(MongoDBConnectionEntityAdminImpl.getInstance(null)).thenReturn(mockConnectionEntityAdminImpl);
        when(mockConnectionEntityAdminImpl.findByGoid(any(Goid.class))).thenReturn(foundConnectionEntity);

        assertTrue(reference.verifyReference());
    }

    @Test
    public void testVerifyReferenceOnMatchingGoidOnly() throws Exception {

        String nomMatchingConnectionName = "new connection";
        Goid matchingGoid = goid;

        MongoDBConnectionEntity foundConnectionEntity = mock(MongoDBConnectionEntity.class);
        when(foundConnectionEntity.getName()).thenReturn(nomMatchingConnectionName); // found
        when(foundConnectionEntity.getGoid()).thenReturn(matchingGoid);

        MongoDBConnectionEntityAdminImpl mockConnectionEntityAdminImpl = Mockito.mock(MongoDBConnectionEntityAdminImpl.class);
        PowerMockito.mockStatic(MongoDBConnectionEntityAdminImpl.class);

        when(MongoDBConnectionEntityAdminImpl.getInstance(null)).thenReturn(mockConnectionEntityAdminImpl);
        when(mockConnectionEntityAdminImpl.findByGoid(any(Goid.class))).thenReturn(foundConnectionEntity);

        assertFalse(reference.verifyReference());
    }

   // to DO iterate through all connections.
    @Test
    public void testVerifyReferencWhenReferenceGoidDoesNotMatchButMatchName() throws Exception {

        Goid differentGoid = new Goid(122,2);

        MongoDBConnectionEntityAdminImpl mockConnectionEntityAdminImpl = Mockito.mock(MongoDBConnectionEntityAdminImpl.class);
        PowerMockito.mockStatic(MongoDBConnectionEntityAdminImpl.class);

        when(MongoDBConnectionEntityAdminImpl.getInstance(null)).thenReturn(mockConnectionEntityAdminImpl);
        when(mockConnectionEntityAdminImpl.findByGoid(any(Goid.class))).thenReturn(null);


        Collection<MongoDBConnectionEntity> existingConnections = new ArrayList();
        existingConnections.add(mockEntity);
        when(mockEntity.getGoid()).thenReturn(differentGoid);
        when(mockEntity.getName()).thenReturn(mongoConnectionName);
        when(mockConnectionEntityAdminImpl.findByType()).thenReturn(existingConnections);

        assertTrue(reference.verifyReference());

    }

    @Test
    public void testVerifyReferencWhenReferenceNotFound() throws Exception {

        MongoDBConnectionEntityAdminImpl mockConnectionEntityAdminImpl = Mockito.mock(MongoDBConnectionEntityAdminImpl.class);
        PowerMockito.mockStatic(MongoDBConnectionEntityAdminImpl.class);

        when(MongoDBConnectionEntityAdminImpl.getInstance(null)).thenReturn(mockConnectionEntityAdminImpl);
        when(mockConnectionEntityAdminImpl.findByGoid(any(Goid.class))).thenReturn(null);


        assertFalse(reference.verifyReference());
    }


    @Test
    public void testLocalizeAssertionWithReplace() throws Exception {
        Goid newGoid = new Goid(2, 1);

        MongoDBConnectionEntity newMongoConnectionEntity = mock(MongoDBConnectionEntity.class);

        when(mockEntityManager.findByPrimaryKey(mockAssertion.getConnectionGoid())).thenReturn(newMongoConnectionEntity);
        when(newMongoConnectionEntity.getName()).thenReturn("newName");

        MongoDBAssertion assertion = new MongoDBAssertion();


        assertion.setConnectionGoid(goid);
        reference.setLocalizeReplace(newGoid);  // Replace

        Boolean result = reference.localizeAssertion(assertion);

        assertEquals(newGoid, assertion.getConnectionGoid());
        assertTrue(result);
    }

    @Test
    public void testLocalizeAssertionWithIgnore() throws Exception {
        MongoDBAssertion assertion = new MongoDBAssertion();
        reference.setLocalizeIgnore(); // Ignore

        Boolean result = reference.localizeAssertion(assertion);

        assertTrue(result);
    }


    @Test
    public void testLocalizeAssertionWithDelete() throws Exception {
        MongoDBAssertion assertion = new MongoDBAssertion();
        assertion.setConnectionGoid(goid);
        reference.setLocalizeDelete();  // Delete

        Boolean result = reference.localizeAssertion(assertion);

        assertFalse(result);
    }


    @Test
    public void parseReferenceFromElementNoPassword() throws Exception {
        Element element = XmlUtil.parse("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + REF_EL_BASIC_CLIENT_NO_PASSWORD).getDocumentElement();

        MongoDBReference newReference = (MongoDBReference) reference.parseFromElement(mockFinder, element);

        assertEquals(newReference.getConnectionName(), "test connection");
        assertEquals(newReference.getGoid(), goid);
        assertEquals(newReference.getDatabaseName(),mongoConnectionDBName);
        assertEquals(newReference.getServerName(),mongoServerAddress);
        assertEquals(newReference.getPortNumber(),mongoConnectionPort);
        assertEquals(newReference.getEncryption(), MongoDBEncryption.NO_ENCRYPTION.name());
        assertEquals(newReference.isUsesDefaultKeyStore(), false);
        assertEquals(newReference.isUsesNoKey(),false);
        assertNull(newReference.getNonDefaultKeystoreId());
        assertNull(newReference.getPrivateKeyAlias());
        assertEquals(newReference.getReadPreference(), "Primary");
    }

    @Test
    public void parseReferenceFromElementCertificateWithCustomPrivateKey() throws Exception {
        Element element = XmlUtil.parse("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + REF_EL_BASIC_CLIENT_CERTIFICATE_AND_CUSTOM_PRIVATE_KEY).getDocumentElement();

        Goid targetGoid = new Goid(0,2);

        MongoDBReference newReference = (MongoDBReference) reference.parseFromElement(mockFinder, element);

        assertEquals(newReference.getConnectionName(), "ConnectionCertificatePrivateKey");
        assertEquals(newReference.getGoid(), goid);
        assertEquals(newReference.getDatabaseName(),mongoConnectionDBName);
        assertEquals(newReference.getServerName(),mongoServerAddress);
        assertEquals(newReference.getPortNumber(),mongoConnectionPort);
        assertEquals(newReference.getEncryption(), MongoDBEncryption.X509_Auth.name());
        assertEquals(newReference.isUsesDefaultKeyStore(), false);
        assertEquals(newReference.isUsesNoKey(),false);
        assertEquals(newReference.getPrivateKeyAlias(), "test2alias");
        assertEquals(newReference.getNonDefaultKeystoreId(),targetGoid);
        assertEquals(newReference.getReadPreference(), "Primary");
    }

    @Test(expected = InvalidDocumentFormatException.class)
    public void parseReferenceFromInvalidRefElement() throws Exception {
        String Invalid_REFERENCE_ELEM = "    <SomthingElse RefType=\"com.l7tech.external.assertions.mongodb.MongoDBReference\">\n" +
                "        <GOID>c06120de5e0fbff8775222316f93b856</GOID>\n" +
                "        <ConnectionName>test connection</ConnectionName>\n" +
                "        <Database>test</Database>\n" +
                "        <Servername>server1</Servername>\n" +
                "        <Port>27017</Port>\n" +
                "        <Encryption>NO_ENCRYPTION</Encryption>\n" +
                "        <UsesDefaultKeyStore>false</UsesDefaultKeyStore>\n" +
                "        <UsesNoKey>false</UsesNoKey>\n" +
                "        <ReadPreference>Primary</ReadPreference>\n" +
                "    </SomthingElse>\n";

        Element element = XmlUtil.parse(Invalid_REFERENCE_ELEM).getDocumentElement();

        reference.parseFromElement(mockFinder, element);
    }
}
