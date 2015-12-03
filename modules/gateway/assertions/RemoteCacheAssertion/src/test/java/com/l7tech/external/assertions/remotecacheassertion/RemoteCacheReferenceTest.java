package com.l7tech.external.assertions.remotecacheassertion;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.remotecacheassertion.server.*;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.GenericEntityHeader;
import com.l7tech.policy.exporter.ExternalReferenceFinder;
import com.l7tech.util.InvalidDocumentFormatException;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import static junit.framework.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class RemoteCacheReferenceTest {

    private ExternalReferenceFinder mockFinder;
    private RemoteCacheEntity mockEntity;
    private Goid goid = new Goid("62c37da96355dfb88d755023a53c5f76");
    private EntityManager<RemoteCacheEntity, GenericEntityHeader> mockEntityManager;
    private RemoteCacheEntityAdmin mockAdmin;
    private RemoteCacheAssertion mockAssertion;
    private Collection<RemoteCacheEntity> list;
    private RemoteCacheReference reference;

    private static final String REFERENCES_BEGIN = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<exp:References xmlns:exp=\"http://www.layer7tech.com/ws/policy/export\">\n";
    private static final String REFERENCES_END = "</exp:References>";

    @Before
    public void setUp() throws Exception {
        list = new ArrayList<>();
        mockFinder = mock(ExternalReferenceFinder.class);
        mockAssertion = mock(RemoteCacheAssertion.class);
        mockEntity = mock(RemoteCacheEntity.class);
        mockAdmin = mock(RemoteCacheEntityAdmin.class);
        mockEntityManager = mock(EntityManager.class);

        when(mockAssertion.getRemoteCacheGoid()).thenReturn(goid);
        when(mockFinder.getGenericEntityManager(RemoteCacheEntity.class)).thenReturn(mockEntityManager);
        when(mockAdmin.find(mockAssertion.getRemoteCacheGoid())).thenReturn(mockEntity);
        when(mockEntityManager.findByPrimaryKey(mockAssertion.getRemoteCacheGoid())).thenReturn(mockEntity);
        when(mockFinder.getGenericEntityManager(RemoteCacheEntity.class)).thenReturn(mockEntityManager);

        HashMap<String, String> properties = new HashMap<>();
        properties.put(RedisRemoteCache.PROPERTY_IS_CLUSTER, "false");
        properties.put(RedisRemoteCache.PROPERTY_PASSWORD, "");
        properties.put(RedisRemoteCache.PROPERTY_SERVERS, "127.0.0.1:6379");

        when(mockEntity.getGoid()).thenReturn(goid);
        when(mockEntity.getName()).thenReturn("redis_test");
        when(mockEntity.getTimeout()).thenReturn(0);
        when(mockEntity.getType()).thenReturn(RemoteCacheTypes.Redis.getEntityType());
        when(mockEntity.isEnabled()).thenReturn(Boolean.TRUE);
        when(mockEntity.getProperties()).thenReturn(properties);
        reference = new RemoteCacheReference(mockFinder, mockAssertion);


    }

    @Test
    public void shouldVerifyWhenReferenceFound() throws Exception {
        RemoteCacheEntity foundMockRemoteCache = mock(RemoteCacheEntity.class);
        when(foundMockRemoteCache.getName()).thenReturn("redis_test");
        when(foundMockRemoteCache.getGoid()).thenReturn(goid);
        when(mockEntityManager.findByPrimaryKey(mockAssertion.getRemoteCacheGoid())).thenReturn(foundMockRemoteCache);

        assertTrue(reference.verifyReference());
    }

    @Test
    public void shouldNotVerifyWhenReferenceFoundDoesnotMatch() throws Exception {
        RemoteCacheEntity foundMockRemoteCache = mock(RemoteCacheEntity.class);
        when(foundMockRemoteCache.getName()).thenReturn("newName");
        when(foundMockRemoteCache.getGoid()).thenReturn(goid);
        when(mockEntityManager.findByPrimaryKey(mockAssertion.getRemoteCacheGoid())).thenReturn(foundMockRemoteCache);

        assertFalse(reference.verifyReference());
    }

    @Test
    public void shouldVerifyWhenReferencePartialFoundByName() throws Exception {
        when(mockEntityManager.findByPrimaryKey(mockAssertion.getRemoteCacheGoid())).thenReturn(null);
        list.add(mockEntity);
        when(mockEntityManager.findAll()).thenReturn(list);

        assertTrue(reference.verifyReference());
    }

    @Test
    public void shouldNotVerifyWhenReferenceNotFound() throws Exception {
        when(mockEntityManager.findByPrimaryKey(mockAssertion.getRemoteCacheGoid())).thenReturn(null);
        assertFalse(reference.verifyReference());
    }

    @Test
    public void shouldLocalizeReplaceStoreAssertion() throws Exception {
        Goid goid1 = new Goid(2, 1);
        RemoteCacheEntity newMockRemotecache = mock(RemoteCacheEntity.class);
        when(mockEntityManager.findByPrimaryKey(mockAssertion.getRemoteCacheGoid())).thenReturn(newMockRemotecache);
        when(newMockRemotecache.getName()).thenReturn("newName");
        RemoteCacheStoreAssertion assertion = new RemoteCacheStoreAssertion();
        assertion.setRemoteCacheGoid(goid);
        reference.setLocalizeReplace(goid1);

        Boolean result = reference.localizeAssertion(assertion);

        assertEquals(goid1, assertion.getRemoteCacheGoid());
        assertTrue(result);
    }

    @Test
    public void shouldLocalizeReplaceLookkupAssertion() throws Exception {
        Goid goid1 = new Goid(2, 1);
        RemoteCacheEntity newMockRemotecache = mock(RemoteCacheEntity.class);
        when(mockEntityManager.findByPrimaryKey(mockAssertion.getRemoteCacheGoid())).thenReturn(newMockRemotecache);
        when(newMockRemotecache.getName()).thenReturn("newName");
        RemoteCacheLookupAssertion assertion = new RemoteCacheLookupAssertion();
        assertion.setRemoteCacheGoid(goid);
        reference.setLocalizeReplace(goid1);

        Boolean result = reference.localizeAssertion(assertion);

        assertEquals(goid1, assertion.getRemoteCacheGoid());
        assertTrue(result);
    }

    @Test
    public void shouldLocalizeIgnoreAssertion() throws Exception {
        RemoteCacheLookupAssertion assertion = new RemoteCacheLookupAssertion();
        reference.setLocalizeIgnore();

        Boolean result = reference.localizeAssertion(assertion);

        assertTrue(result);
    }

    @Test
    public void shouldNotLocalizeDeleteLookupAssertion() throws Exception {
        RemoteCacheLookupAssertion assertion = new RemoteCacheLookupAssertion();
        assertion.setRemoteCacheGoid(goid);
        reference.setLocalizeDelete();

        Boolean result = reference.localizeAssertion(assertion);

        assertFalse(result);
    }

    @Test
    public void shouldNotLocalizeDeleteStoreAssertion() throws Exception {
        RemoteCacheStoreAssertion assertion = new RemoteCacheStoreAssertion();
        assertion.setRemoteCacheGoid(goid);
        reference.setLocalizeDelete();

        Boolean result = reference.localizeAssertion(assertion);

        assertFalse(result);
    }

    @Test(expected = InvalidDocumentFormatException.class)
    public void parseReferenceFromInvalidRefElement() throws Exception {
        String Invalid_REFERENCE_ELEM = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "    <SomethingElse RefType=\"aDifferentRef\">\n" +
                "       <GOID>62c37da96355dfb88d755023a53c5f76</GOID>" +
                "        <Name>redis_test</Name>\n" +
                "        <Type>redis</Type>\n" +
                "        <Timeout>0</Timeout>\n" +
                "        <Properties>\n" +
                "            <Property mame=\"isCluster\">false</Property>\n" +
                "            <Property mame=\"servers\">127.0.0.1:6379</Property>\n" +
                "            <Property mame=\"password\"/>\n" +
                "        </Properties>\n" +
                "    </SomethingElse>";

        Element element = XmlUtil.parse(Invalid_REFERENCE_ELEM).getDocumentElement();

        reference.parseFromElement(mockFinder, element);
    }

    //There was a spelling mistake in the properties name element. Instead of name, we had mame.
    //This unit test ensures that previously exported policies in older versions, still work successfully
    @Test
    public void testNameElementRefactor() throws Exception {

        final String REF_EL_UPGRADE_CLIENT_REDIS = "    <RemoteCacheReference RefType=\"com.l7tech.external.assertions.remotecacheassertion.RemoteCacheReference\">\n" +
                "        <GOID>62c37da96355dfb88d755023a53c5f76</GOID>\n" +
                "        <Name>redis_test</Name>\n" +
                "        <Type>redis</Type>\n" +
                "        <Timeout>0</Timeout>\n" +
                "        <Properties>\n" +
                "            <Property mame=\"isCluster\">false</Property>\n" +
                "            <Property mame=\"servers\">127.0.0.1:6379</Property>\n" +
                "            <Property mame=\"password\"/>\n" +
                "        </Properties>\n" +
                "    </RemoteCacheReference>\n";

        final String REF_EL_NAME_REFACTOR = "    <RemoteCacheReference RefType=\"com.l7tech.external.assertions.remotecacheassertion.RemoteCacheReference\">\n" +
                "        <GOID>62c37da96355dfb88d755023a53c5f76</GOID>\n" +
                "        <Name>redis_test</Name>\n" +
                "        <Type>redis</Type>\n" +
                "        <Enabled>true</Enabled>\n" +
                "        <Timeout>0</Timeout>\n" +
                "        <Properties>\n" +
                "            <Property name=\"password\"/>\n" +
                "            <Property name=\"servers\">127.0.0.1:6379</Property>\n" +
                "            <Property name=\"isCluster\">false</Property>\n" +
                "        </Properties>\n" +
                "    </RemoteCacheReference>\n";

        final Element element = XmlUtil.parse("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + REF_EL_UPGRADE_CLIENT_REDIS).getDocumentElement();
        RemoteCacheReference newReference = (RemoteCacheReference) reference.parseFromElement(mockFinder, element);

        final Element newElement = XmlUtil.createEmptyDocument("References", "exp", "http://www.layer7tech.com/ws/policy/export").getDocumentElement();
        newReference.serializeToRefElement(newElement);
        final String asXml = XmlUtil.nodeToFormattedString(newElement);

        assertEquals(REFERENCES_BEGIN + REF_EL_NAME_REFACTOR + REFERENCES_END, asXml.trim());
    }

    //Redis ExternalReference Tests

    @Test
    public void shouldVerifyWhenReferencePartialFoundByPropertyRedisTest() throws Exception {
        HashMap<String, String> properties = new HashMap<>();
        properties.put(RedisRemoteCache.PROPERTY_IS_CLUSTER, "false");
        properties.put(RedisRemoteCache.PROPERTY_PASSWORD, "");
        properties.put(RedisRemoteCache.PROPERTY_SERVERS, "127.0.0.1:6379");

        RemoteCacheEntity newMockRemotecache = mock(RemoteCacheEntity.class);
        when(newMockRemotecache.getName()).thenReturn("newName");
        when(newMockRemotecache.getType()).thenReturn(RemoteCacheTypes.Redis.getEntityType());
        when(newMockRemotecache.getProperties()).thenReturn(properties);
        when(mockEntityManager.findByPrimaryKey(mockAssertion.getRemoteCacheGoid())).thenReturn(null);
        list.add(newMockRemotecache);
        when(mockEntityManager.findAll()).thenReturn(list);

        assertTrue(reference.verifyReference());
    }

    @Test
    public void serializeToRefElementRedisTest() throws Exception {
        final String REF_EL_BASIC_CLIENT_REDIS = "    <RemoteCacheReference RefType=\"com.l7tech.external.assertions.remotecacheassertion.RemoteCacheReference\">\n" +
                "        <GOID>62c37da96355dfb88d755023a53c5f76</GOID>\n" +
                "        <Name>redis_test</Name>\n" +
                "        <Type>redis</Type>\n" +
                "        <Enabled>true</Enabled>\n" +
                "        <Timeout>0</Timeout>\n" +
                "        <Properties>\n" +
                "            <Property name=\"password\"/>\n" +
                "            <Property name=\"servers\">127.0.0.1:6379</Property>\n" +
                "            <Property name=\"isCluster\">false</Property>\n" +
                "        </Properties>\n" +
                "    </RemoteCacheReference>\n";

        final Element element = XmlUtil.createEmptyDocument("References", "exp", "http://www.layer7tech.com/ws/policy/export").getDocumentElement();
        reference.serializeToRefElement(element);
        final String asXml = XmlUtil.nodeToFormattedString(element);

        assertEquals(REFERENCES_BEGIN + REF_EL_BASIC_CLIENT_REDIS + REFERENCES_END, asXml.trim());
    }

    @Test
    public void parseReferenceFromElementRedisTest() throws Exception {
        final String REF_EL_UPGRADE_CLIENT_REDIS = "    <RemoteCacheReference RefType=\"com.l7tech.external.assertions.remotecacheassertion.RemoteCacheReference\">\n" +
                "        <GOID>62c37da96355dfb88d755023a53c5f76</GOID>\n" +
                "        <Name>redis_test</Name>\n" +
                "        <Type>redis</Type>\n" +
                "        <Timeout>0</Timeout>\n" +
                "        <Properties>\n" +
                "            <Property mame=\"isCluster\">false</Property>\n" +
                "            <Property mame=\"servers\">127.0.0.1:6379</Property>\n" +
                "            <Property mame=\"password\"/>\n" +
                "        </Properties>\n" +
                "    </RemoteCacheReference>\n";

        final String REF_EL_BASIC_CLIENT_REDIS = "    <RemoteCacheReference RefType=\"com.l7tech.external.assertions.remotecacheassertion.RemoteCacheReference\">\n" +
                "        <GOID>62c37da96355dfb88d755023a53c5f76</GOID>\n" +
                "        <Name>redis_test</Name>\n" +
                "        <Type>redis</Type>\n" +
                "        <Enabled>true</Enabled>\n" +
                "        <Timeout>0</Timeout>\n" +
                "        <Properties>\n" +
                "            <Property name=\"password\"/>\n" +
                "            <Property name=\"servers\">127.0.0.1:6379</Property>\n" +
                "            <Property name=\"isCluster\">false</Property>\n" +
                "        </Properties>\n" +
                "    </RemoteCacheReference>\n";

        Element element = XmlUtil.parse("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + REF_EL_BASIC_CLIENT_REDIS).getDocumentElement();

        RemoteCacheReference newReference = (RemoteCacheReference) reference.parseFromElement(mockFinder, element);

        assertEquals("redis_test", newReference.getName());
        assertEquals(goid, newReference.getOid());
        assertEquals(0, newReference.getTimeout());
        assertEquals(RemoteCacheTypes.Redis.getEntityType(), newReference.getType());
        assertTrue(newReference.isEnabled());
        assertEquals(3, newReference.getProperties().size());
        assertEquals("false", newReference.getProperties().get(RedisRemoteCache.PROPERTY_IS_CLUSTER));
        assertEquals("", newReference.getProperties().get(RedisRemoteCache.PROPERTY_PASSWORD));
        assertEquals("127.0.0.1:6379", newReference.getProperties().get(RedisRemoteCache.PROPERTY_SERVERS));

        element = XmlUtil.parse("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + REF_EL_UPGRADE_CLIENT_REDIS).getDocumentElement();

        newReference = (RemoteCacheReference) reference.parseFromElement(mockFinder, element);

        assertEquals("redis_test", newReference.getName());
        assertEquals(goid, newReference.getOid());
        assertEquals(0, newReference.getTimeout());
        assertEquals(RemoteCacheTypes.Redis.getEntityType(), newReference.getType());
        assertTrue(newReference.isEnabled());
        assertEquals(3, newReference.getProperties().size());
        assertEquals("false", newReference.getProperties().get(RedisRemoteCache.PROPERTY_IS_CLUSTER));
        assertEquals("", newReference.getProperties().get(RedisRemoteCache.PROPERTY_PASSWORD));
        assertEquals("127.0.0.1:6379", newReference.getProperties().get(RedisRemoteCache.PROPERTY_SERVERS));
    }

    //Memcached ExternalReference Tests:
    @Test
    public void shouldVerifyWhenReferencePartialFoundByPropertyMemCachedTest() throws Exception {
        HashMap<String, String> properties = new HashMap<>();
        properties.put(MemcachedRemoteCache.PROP_BUCKET_SPECIFIED, "false");
        properties.put(MemcachedRemoteCache.PROP_SERVERPORTS, "localhost:11211");
        properties.put(MemcachedRemoteCache.PROP_PASSWORD, "password");
        properties.put(MemcachedRemoteCache.PROP_BUCKETNAME, "username");

        when(mockEntity.getGoid()).thenReturn(goid);
        when(mockEntity.getName()).thenReturn("memecached_test");
        when(mockEntity.getTimeout()).thenReturn(0);
        when(mockEntity.getType()).thenReturn(RemoteCacheTypes.Memcached.getEntityType());
        when(mockEntity.isEnabled()).thenReturn(Boolean.TRUE);
        when(mockEntity.getProperties()).thenReturn(properties);
        reference = new RemoteCacheReference(mockFinder, mockAssertion);


        RemoteCacheEntity newMockRemotecache = mock(RemoteCacheEntity.class);
        when(newMockRemotecache.getName()).thenReturn("newName");
        when(newMockRemotecache.getType()).thenReturn(RemoteCacheTypes.Memcached.getEntityType());
        when(newMockRemotecache.getProperties()).thenReturn(properties);
        when(mockEntityManager.findByPrimaryKey(mockAssertion.getRemoteCacheGoid())).thenReturn(null);
        list.add(newMockRemotecache);
        when(mockEntityManager.findAll()).thenReturn(list);

        assertTrue(reference.verifyReference());
    }

    @Test
    public void shouldVerifyWhenReferencePartialFoundByPropertyWithSASLMemCachedTest() throws Exception {
        HashMap<String, String> properties = new HashMap<>();
        properties.put(MemcachedRemoteCache.PROP_BUCKET_SPECIFIED, "true");
        properties.put(MemcachedRemoteCache.PROP_SERVERPORTS, "localhost:11211");
        properties.put(MemcachedRemoteCache.PROP_PASSWORD, "password");
        properties.put(MemcachedRemoteCache.PROP_BUCKETNAME, "username");

        when(mockEntity.getGoid()).thenReturn(goid);
        when(mockEntity.getName()).thenReturn("memecached_test");
        when(mockEntity.getTimeout()).thenReturn(0);
        when(mockEntity.getType()).thenReturn(RemoteCacheTypes.Memcached.getEntityType());
        when(mockEntity.isEnabled()).thenReturn(Boolean.TRUE);
        when(mockEntity.getProperties()).thenReturn(properties);
        reference = new RemoteCacheReference(mockFinder, mockAssertion);


        RemoteCacheEntity newMockRemotecache = mock(RemoteCacheEntity.class);
        when(newMockRemotecache.getName()).thenReturn("newName");
        when(newMockRemotecache.getType()).thenReturn(RemoteCacheTypes.Memcached.getEntityType());
        when(newMockRemotecache.getProperties()).thenReturn(properties);
        when(mockEntityManager.findByPrimaryKey(mockAssertion.getRemoteCacheGoid())).thenReturn(null);
        list.add(newMockRemotecache);
        when(mockEntityManager.findAll()).thenReturn(list);

        assertTrue(reference.verifyReference());
    }

    @Test
    public void serializeToRefElementMemCachedTest() throws Exception {
        final String REF_EL_BASIC_CLIENT_MEMCACHED = "    <RemoteCacheReference RefType=\"com.l7tech.external.assertions.remotecacheassertion.RemoteCacheReference\">\n" +
                "        <GOID>62c37da96355dfb88d755023a53c5f76</GOID>\n" +
                "        <Name>memcached</Name>\n" +
                "        <Type>memcached</Type>\n" +
                "        <Enabled>true</Enabled>\n" +
                "        <Timeout>0</Timeout>\n" +
                "        <Properties>\n" +
                "            <Property name=\"ports\">server1:11211,server2:11212,server3:11213</Property>\n" +
                "            <Property name=\"bucketSpecified\">false</Property>\n" +
                "        </Properties>\n" +
                "    </RemoteCacheReference>\n";

        HashMap<String, String> properties = new HashMap<>();
        properties.put(MemcachedRemoteCache.PROP_BUCKET_SPECIFIED, "false");
        properties.put(MemcachedRemoteCache.PROP_SERVERPORTS, "server1:11211,server2:11212,server3:11213");

        when(mockEntity.getGoid()).thenReturn(goid);
        when(mockEntity.getName()).thenReturn("memcached");
        when(mockEntity.getTimeout()).thenReturn(0);
        when(mockEntity.getType()).thenReturn(RemoteCacheTypes.Memcached.getEntityType());
        when(mockEntity.isEnabled()).thenReturn(Boolean.TRUE);
        when(mockEntity.getProperties()).thenReturn(properties);
        reference = new RemoteCacheReference(mockFinder, mockAssertion);

        final Element element = XmlUtil.createEmptyDocument("References", "exp", "http://www.layer7tech.com/ws/policy/export").getDocumentElement();
        reference.serializeToRefElement(element);
        final String asXml = XmlUtil.nodeToFormattedString(element);

        assertEquals(REFERENCES_BEGIN + REF_EL_BASIC_CLIENT_MEMCACHED + REFERENCES_END, asXml.trim());
    }

    @Test
    public void parseReferenceFromElementMemCachedTest() throws Exception {
        final String REF_EL_BASIC_CLIENT_MEMCACHED = "    <RemoteCacheReference RefType=\"com.l7tech.external.assertions.remotecacheassertion.RemoteCacheReference\">\n" +
                "        <GOID>62c37da96355dfb88d755023a53c5f76</GOID>\n" +
                "        <Name>memcached</Name>\n" +
                "        <Type>memcached</Type>\n" +
                "        <Enabled>true</Enabled>\n" +
                "        <Timeout>0</Timeout>\n" +
                "        <Properties>\n" +
                "            <Property name=\"ports\">server1:11211,server2:11212,server3:11213</Property>\n" +
                "            <Property name=\"bucketSpecified\">false</Property>\n" +
                "        </Properties>\n" +
                "    </RemoteCacheReference>\n";

        final String REF_EL_BASIC_CLIENT_MEMCACHED_WITH_SASL = "    <RemoteCacheReference RefType=\"com.l7tech.external.assertions.remotecacheassertion.RemoteCacheReference\">\n" +
                "        <GOID>62c37da96355dfb88d755023a53c5f76</GOID>\n" +
                "        <Name>memcached</Name>\n" +
                "        <Type>memcached</Type>\n" +
                "        <Enabled>true</Enabled>\n" +
                "        <Timeout>0</Timeout>\n" +
                "        <Properties>\n" +
                "            <Property name=\"bucketName\">username</Property>\n" +
                "            <Property mame=\"password\">password</Property>\n" +
                "            <Property name=\"ports\">server1:11211,server2:11212,server3:11213</Property>\n" +
                "            <Property name=\"bucketSpecified\">true</Property>\n" +
                "        </Properties>\n" +
                "    </RemoteCacheReference>\n";


        //Test memcache with SASL disabled
        Element element = XmlUtil.parse("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + REF_EL_BASIC_CLIENT_MEMCACHED).getDocumentElement();
        RemoteCacheReference newReference = (RemoteCacheReference) reference.parseFromElement(mockFinder, element);

        assertEquals("memcached", newReference.getName());
        assertEquals(goid, newReference.getOid());
        assertEquals(0, newReference.getTimeout());
        assertEquals(RemoteCacheTypes.Memcached.getEntityType(), newReference.getType());
        assertTrue(newReference.isEnabled());
        assertEquals(2, newReference.getProperties().size());
        assertEquals("false", newReference.getProperties().get(MemcachedRemoteCache.PROP_BUCKET_SPECIFIED));
        assertEquals("server1:11211,server2:11212,server3:11213", newReference.getProperties().get(MemcachedRemoteCache.PROP_SERVERPORTS));
        assertNull(newReference.getProperties().get(MemcachedRemoteCache.PROP_BUCKETNAME));
        assertNull(newReference.getProperties().get(MemcachedRemoteCache.PROP_PASSWORD));

        //Test memcache with SASL enabled
        element = XmlUtil.parse("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + REF_EL_BASIC_CLIENT_MEMCACHED_WITH_SASL).getDocumentElement();
        newReference = (RemoteCacheReference) reference.parseFromElement(mockFinder, element);

        assertEquals("memcached", newReference.getName());
        assertEquals(goid, newReference.getOid());
        assertEquals(0, newReference.getTimeout());
        assertEquals(RemoteCacheTypes.Memcached.getEntityType(), newReference.getType());
        assertTrue(newReference.isEnabled());
        assertEquals(4, newReference.getProperties().size());
        assertEquals("true", newReference.getProperties().get(MemcachedRemoteCache.PROP_BUCKET_SPECIFIED));
        assertEquals("server1:11211,server2:11212,server3:11213", newReference.getProperties().get(MemcachedRemoteCache.PROP_SERVERPORTS));
        assertEquals("username", newReference.getProperties().get(MemcachedRemoteCache.PROP_BUCKETNAME));
        assertEquals("password", newReference.getProperties().get(MemcachedRemoteCache.PROP_PASSWORD));
    }

    //GemFire ExternalReference Tests:
    @Test
    public void shouldVerifyWhenReferencePartialFoundByPropertyGemFireTest() throws Exception {
        HashMap<String, String> properties = new HashMap<>();
        properties.put(GemfireRemoteCache.PROPERTY_CACHE_NAME, "gemfire_test");
        properties.put(GemfireRemoteCache.PROPERTY_CACHE_OPTION, "locator");
        properties.put(GemfireRemoteCache.PROPERTY_SERVERS, "localhost:1234");
        properties.put("gemfireprop1", "value1");
        properties.put("gemfireprop2", "value2");


        when(mockEntity.getGoid()).thenReturn(goid);
        when(mockEntity.getName()).thenReturn("gemfire_test");
        when(mockEntity.getTimeout()).thenReturn(0);
        when(mockEntity.getType()).thenReturn(RemoteCacheTypes.GemFire.getEntityType());
        when(mockEntity.isEnabled()).thenReturn(Boolean.TRUE);
        when(mockEntity.getProperties()).thenReturn(properties);

        reference = new RemoteCacheReference(mockFinder, mockAssertion);


        RemoteCacheEntity newMockRemotecache = mock(RemoteCacheEntity.class);
        when(newMockRemotecache.getName()).thenReturn("newName");
        when(newMockRemotecache.getType()).thenReturn(RemoteCacheTypes.GemFire.getEntityType());
        when(newMockRemotecache.getProperties()).thenReturn(properties);
        when(mockEntityManager.findByPrimaryKey(mockAssertion.getRemoteCacheGoid())).thenReturn(null);
        list.add(newMockRemotecache);
        when(mockEntityManager.findAll()).thenReturn(list);

        assertTrue(reference.verifyReference());
    }

    @Test
    public void serializeToRefElementGemFireTest() throws Exception {
        final String REF_EL_BASIC_CLIENT_GEMFIRE = "    <RemoteCacheReference RefType=\"com.l7tech.external.assertions.remotecacheassertion.RemoteCacheReference\">\n" +
                "        <GOID>62c37da96355dfb88d755023a53c5f76</GOID>\n" +
                "        <Name>gemfire_test</Name>\n" +
                "        <Type>gemfire</Type>\n" +
                "        <Enabled>true</Enabled>\n" +
                "        <Timeout>0</Timeout>\n" +
                "        <Properties>\n" +
                "            <Property name=\"gemfireprop1\">value1</Property>\n" +
                "            <Property name=\"gemfireprop2\">value2</Property>\n" +
                "            <Property name=\"cacheName\">gemfire_test</Property>\n" +
                "            <Property name=\"servers\">localhost:1234</Property>\n" +
                "            <Property name=\"cacheOption\">locator</Property>\n" +
                "        </Properties>\n" +
                "    </RemoteCacheReference>\n";

        HashMap<String, String> properties = new HashMap<>();
        properties.put(GemfireRemoteCache.PROPERTY_CACHE_NAME, "gemfire_test");
        properties.put(GemfireRemoteCache.PROPERTY_CACHE_OPTION, "locator");
        properties.put(GemfireRemoteCache.PROPERTY_SERVERS, "localhost:1234");
        properties.put("gemfireprop1", "value1");
        properties.put("gemfireprop2", "value2");


        when(mockEntity.getGoid()).thenReturn(goid);
        when(mockEntity.getName()).thenReturn("gemfire_test");
        when(mockEntity.getTimeout()).thenReturn(0);
        when(mockEntity.getType()).thenReturn(RemoteCacheTypes.GemFire.getEntityType());
        when(mockEntity.isEnabled()).thenReturn(Boolean.TRUE);
        when(mockEntity.getProperties()).thenReturn(properties);

        reference = new RemoteCacheReference(mockFinder, mockAssertion);

        final Element element = XmlUtil.createEmptyDocument("References", "exp", "http://www.layer7tech.com/ws/policy/export").getDocumentElement();
        reference.serializeToRefElement(element);
        final String asXml = XmlUtil.nodeToFormattedString(element);

        assertEquals(REFERENCES_BEGIN + REF_EL_BASIC_CLIENT_GEMFIRE + REFERENCES_END, asXml.trim());
    }

    @Test
    public void parseReferenceFromElementGemFireTest() throws Exception {
        final String REF_EL_BASIC_CLIENT_GEMFIRE = "    <RemoteCacheReference RefType=\"com.l7tech.external.assertions.remotecacheassertion.RemoteCacheReference\">\n" +
                "        <GOID>62c37da96355dfb88d755023a53c5f76</GOID>\n" +
                "        <Name>gemfire_test</Name>\n" +
                "        <Type>gemfire</Type>\n" +
                "        <Enabled>true</Enabled>\n" +
                "        <Timeout>0</Timeout>\n" +
                "        <Properties>\n" +
                "            <Property name=\"gemfireprop1\">value1</Property>\n" +
                "            <Property name=\"gemfireprop2\">value2</Property>\n" +
                "            <Property name=\"cacheName\">gemfire_test</Property>\n" +
                "            <Property name=\"servers\">localhost:1234</Property>\n" +
                "            <Property name=\"cacheOption\">locator</Property>\n" +
                "        </Properties>\n" +
                "    </RemoteCacheReference>\n";

        Element element = XmlUtil.parse("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + REF_EL_BASIC_CLIENT_GEMFIRE).getDocumentElement();
        RemoteCacheReference newReference = (RemoteCacheReference) reference.parseFromElement(mockFinder, element);

        assertEquals("gemfire_test", newReference.getName());
        assertEquals(goid, newReference.getOid());
        assertEquals(0, newReference.getTimeout());
        assertEquals(RemoteCacheTypes.GemFire.getEntityType(), newReference.getType());
        assertTrue(newReference.isEnabled());
        assertEquals(5, newReference.getProperties().size());
        assertEquals("localhost:1234", newReference.getProperties().get(GemfireRemoteCache.PROPERTY_SERVERS));
        assertEquals("gemfire_test", newReference.getProperties().get(GemfireRemoteCache.PROPERTY_CACHE_NAME));
        assertEquals("locator", newReference.getProperties().get(GemfireRemoteCache.PROPERTY_CACHE_OPTION));
        assertEquals("value1", newReference.getProperties().get("gemfireprop1"));
        assertEquals("value2", newReference.getProperties().get("gemfireprop2"));
    }


    //Coherence ExternalReference Tests:
    @Test
    public void shouldVerifyWhenReferencePartialFoundByPropertyCoherenceTest() throws Exception {
        HashMap<String, String> properties = new HashMap<>();
        properties.put(CoherenceRemoteCache.PROP_SERVERS, "localhost:1234");
        properties.put(CoherenceRemoteCache.PROP_CACHE_NAME, "coherence");

        when(mockEntity.getGoid()).thenReturn(goid);
        when(mockEntity.getName()).thenReturn("coherence");
        when(mockEntity.getTimeout()).thenReturn(0);
        when(mockEntity.getType()).thenReturn(RemoteCacheTypes.Coherence.getEntityType());
        when(mockEntity.isEnabled()).thenReturn(Boolean.TRUE);
        when(mockEntity.getProperties()).thenReturn(properties);

        reference = new RemoteCacheReference(mockFinder, mockAssertion);


        RemoteCacheEntity newMockRemotecache = mock(RemoteCacheEntity.class);
        when(newMockRemotecache.getName()).thenReturn("newName");
        when(newMockRemotecache.getType()).thenReturn(RemoteCacheTypes.Coherence.getEntityType());
        when(newMockRemotecache.getProperties()).thenReturn(properties);
        when(mockEntityManager.findByPrimaryKey(mockAssertion.getRemoteCacheGoid())).thenReturn(null);
        list.add(newMockRemotecache);
        when(mockEntityManager.findAll()).thenReturn(list);

        assertTrue(reference.verifyReference());
    }

    @Test
    public void serializeToRefElementCoherenceTest() throws Exception {
        final String REF_EL_BASIC_CLIENT = "    <RemoteCacheReference RefType=\"com.l7tech.external.assertions.remotecacheassertion.RemoteCacheReference\">\n" +
                "        <GOID>62c37da96355dfb88d755023a53c5f76</GOID>\n" +
                "        <Name>coherence_test</Name>\n" +
                "        <Type>coherence</Type>\n" +
                "        <Enabled>true</Enabled>\n" +
                "        <Timeout>0</Timeout>\n" +
                "        <Properties>\n" +
                "            <Property name=\"cacheName\">coherence</Property>\n" +
                "            <Property name=\"servers\">localhost:1234</Property>\n" +
                "        </Properties>\n" +
                "    </RemoteCacheReference>\n";

        HashMap<String, String> properties = new HashMap<>();
        properties.put(CoherenceRemoteCache.PROP_CACHE_NAME, "coherence");
        properties.put(CoherenceRemoteCache.PROP_SERVERS, "localhost:1234");

        when(mockEntity.getGoid()).thenReturn(goid);
        when(mockEntity.getName()).thenReturn("coherence_test");
        when(mockEntity.getTimeout()).thenReturn(0);
        when(mockEntity.getType()).thenReturn(RemoteCacheTypes.Coherence.getEntityType());
        when(mockEntity.isEnabled()).thenReturn(Boolean.TRUE);
        when(mockEntity.getProperties()).thenReturn(properties);

        reference = new RemoteCacheReference(mockFinder, mockAssertion);

        final Element element = XmlUtil.createEmptyDocument("References", "exp", "http://www.layer7tech.com/ws/policy/export").getDocumentElement();
        reference.serializeToRefElement(element);
        final String asXml = XmlUtil.nodeToFormattedString(element);

        assertEquals(REFERENCES_BEGIN + REF_EL_BASIC_CLIENT + REFERENCES_END, asXml.trim());
    }

    @Test
    public void parseReferenceFromElementCoherenceTest() throws Exception {
        final String REF_EL_BASIC_CLIENT = "    <RemoteCacheReference RefType=\"com.l7tech.external.assertions.remotecacheassertion.RemoteCacheReference\">\n" +
                "        <GOID>62c37da96355dfb88d755023a53c5f76</GOID>\n" +
                "        <Name>coherence_test</Name>\n" +
                "        <Type>coherence</Type>\n" +
                "        <Enabled>true</Enabled>\n" +
                "        <Timeout>0</Timeout>\n" +
                "        <Properties>\n" +
                "            <Property name=\"cacheName\">coherence</Property>\n" +
                "            <Property name=\"servers\">localhost:1234</Property>\n" +
                "        </Properties>\n" +
                "    </RemoteCacheReference>\n";

        Element element = XmlUtil.parse("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + REF_EL_BASIC_CLIENT).getDocumentElement();
        RemoteCacheReference newReference = (RemoteCacheReference) reference.parseFromElement(mockFinder, element);

        assertEquals("coherence_test", newReference.getName());
        assertEquals(goid, newReference.getOid());
        assertEquals(0, newReference.getTimeout());
        assertEquals(RemoteCacheTypes.Coherence.getEntityType(), newReference.getType());
        assertTrue(newReference.isEnabled());
        assertEquals(2, newReference.getProperties().size());
        assertEquals("localhost:1234", newReference.getProperties().get(CoherenceRemoteCache.PROP_SERVERS));
        assertEquals("coherence", newReference.getProperties().get(CoherenceRemoteCache.PROP_CACHE_NAME));
    }

    //Terracata ExternalReference Tests:
    @Test
    public void shouldVerifyWhenReferencePartialFoundByPropertyTerracottaTest() throws Exception {
        HashMap<String, String> properties = new HashMap<>();
        properties.put(TerracottaRemoteCache.PROPERTY_URLS, "localhost:1234");
        properties.put(TerracottaRemoteCache.PROPERTY_CACHE_NAME, "terracotta");

        when(mockEntity.getGoid()).thenReturn(goid);
        when(mockEntity.getName()).thenReturn("terracotta");
        when(mockEntity.getTimeout()).thenReturn(0);
        when(mockEntity.getType()).thenReturn(RemoteCacheTypes.Terracotta.getEntityType());
        when(mockEntity.isEnabled()).thenReturn(Boolean.TRUE);
        when(mockEntity.getProperties()).thenReturn(properties);

        reference = new RemoteCacheReference(mockFinder, mockAssertion);


        RemoteCacheEntity newMockRemotecache = mock(RemoteCacheEntity.class);
        when(newMockRemotecache.getName()).thenReturn("newName");
        when(newMockRemotecache.getType()).thenReturn(RemoteCacheTypes.Terracotta.getEntityType());
        when(newMockRemotecache.getProperties()).thenReturn(properties);
        when(mockEntityManager.findByPrimaryKey(mockAssertion.getRemoteCacheGoid())).thenReturn(null);
        list.add(newMockRemotecache);
        when(mockEntityManager.findAll()).thenReturn(list);

        assertTrue(reference.verifyReference());
    }

    @Test
    public void serializeToRefElementTerracottaTest() throws Exception {
        final String REF_EL_BASIC_CLIENT = "    <RemoteCacheReference RefType=\"com.l7tech.external.assertions.remotecacheassertion.RemoteCacheReference\">\n" +
                "        <GOID>62c37da96355dfb88d755023a53c5f76</GOID>\n" +
                "        <Name>Terracotta</Name>\n" +
                "        <Type>terracotta</Type>\n" +
                "        <Enabled>true</Enabled>\n" +
                "        <Timeout>0</Timeout>\n" +
                "        <Properties>\n" +
                "            <Property name=\"cacheName\">terracotta_test</Property>\n" +
                "            <Property name=\"servers\">localhost:1234</Property>\n" +
                "        </Properties>\n" +
                "    </RemoteCacheReference>\n";

        HashMap<String, String> properties = new HashMap<>();
        properties.put(CoherenceRemoteCache.PROP_CACHE_NAME, "terracotta_test");
        properties.put(CoherenceRemoteCache.PROP_SERVERS, "localhost:1234");

        when(mockEntity.getGoid()).thenReturn(goid);
        when(mockEntity.getName()).thenReturn("Terracotta");
        when(mockEntity.getTimeout()).thenReturn(0);
        when(mockEntity.getType()).thenReturn(RemoteCacheTypes.Terracotta.getEntityType());
        when(mockEntity.isEnabled()).thenReturn(Boolean.TRUE);
        when(mockEntity.getProperties()).thenReturn(properties);

        reference = new RemoteCacheReference(mockFinder, mockAssertion);

        final Element element = XmlUtil.createEmptyDocument("References", "exp", "http://www.layer7tech.com/ws/policy/export").getDocumentElement();
        reference.serializeToRefElement(element);
        final String asXml = XmlUtil.nodeToFormattedString(element);

        assertEquals(REFERENCES_BEGIN + REF_EL_BASIC_CLIENT + REFERENCES_END, asXml.trim());
    }

    @Test
    public void parseReferenceFromElementTerracottaTest() throws Exception {
        final String REF_EL_BASIC_CLIENT = "    <RemoteCacheReference RefType=\"com.l7tech.external.assertions.remotecacheassertion.RemoteCacheReference\">\n" +
                "        <GOID>62c37da96355dfb88d755023a53c5f76</GOID>\n" +
                "        <Name>Terracotta</Name>\n" +
                "        <Type>terracotta</Type>\n" +
                "        <Enabled>true</Enabled>\n" +
                "        <Timeout>0</Timeout>\n" +
                "        <Properties>\n" +
                "            <Property name=\"cacheName\">terracotta_test</Property>\n" +
                "            <Property name=\"servers\">localhost:1234</Property>\n" +
                "        </Properties>\n" +
                "    </RemoteCacheReference>\n";

        Element element = XmlUtil.parse("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + REF_EL_BASIC_CLIENT).getDocumentElement();
        RemoteCacheReference newReference = (RemoteCacheReference) reference.parseFromElement(mockFinder, element);

        assertEquals("Terracotta", newReference.getName());
        assertEquals(goid, newReference.getOid());
        assertEquals(0, newReference.getTimeout());
        assertEquals(RemoteCacheTypes.Terracotta.getEntityType(), newReference.getType());
        assertTrue(newReference.isEnabled());
        assertEquals(2, newReference.getProperties().size());
        assertEquals("localhost:1234", newReference.getProperties().get(CoherenceRemoteCache.PROP_SERVERS));
        assertEquals("terracotta_test", newReference.getProperties().get(CoherenceRemoteCache.PROP_CACHE_NAME));
    }

}
