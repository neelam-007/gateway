package com.l7tech.external.assertions.cache;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.HybridStashManager;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.StashManager;
import com.l7tech.external.assertions.cache.server.ServerCacheLookupAssertion;
import com.l7tech.external.assertions.cache.server.ServerCacheStorageAssertion;
import com.l7tech.external.assertions.cache.server.SsgCacheManager;
import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.message.Message;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.wsp.WspConstants;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.server.MockClusterPropertyManager;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.event.system.Stopping;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.test.BugNumber;
import com.l7tech.util.IOUtils;
import com.l7tech.util.ResourceUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/**
 * Test the CacheStorageAssertion and the CacheLookupAssertion.
 *
 * @noinspection JavaDoc
 */

public class CacheAssertionTest {
    private static final ApplicationEventProxy eventProxy = new ApplicationEventProxy();
    private static final StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();

    static {
        beanFactory.addBean( "messageCacheStashManagerFactory", TestStashManagerFactory.getInstance() );
        beanFactory.addBean( "applicationEventProxy", eventProxy );
        beanFactory.addBean( "clusterPropertyManager", new MockClusterPropertyManager( new ClusterProperty("messageCache.resetGeneration", "0")));
    }

    @Before
    public void resetSsgCacheManager(){
        Field field = null;
        try{
            field = SsgCacheManager.class.getDeclaredField("INSTANCE");
            field.setAccessible(true);
            final Object o = field.get(SsgCacheManager.class);
            if(o != null){
                ((AtomicReference)o).set(null);
            }
        }
        catch(Exception e){
            fail("Error resting SsgCacheManager via reflection: " + e.getMessage());
        }
        finally {
            if(field != null){
                field.setAccessible(false);
            }
        }
    }

    protected BeanFactory getBeanFactory() {
        return beanFactory;
    }

    @Ignore("unknown")
    @Test
    public void testFeatureNames() throws Exception {
        assertEquals( "assertion:CacheStorage", new CacheStorageAssertion().getFeatureSetName() );
        assertEquals( "assertion:CacheLookup", new CacheLookupAssertion().getFeatureSetName() );
    }

    static String messageBodyToString( Message msg ) throws IOException, NoSuchPartException {
        final InputStream is = msg.getMimeKnob().getEntireMessageBodyAsInputStream();
        try {
            return new String( IOUtils.slurpStream( is ), msg.getMimeKnob().getOuterContentType().getEncoding() );
        } finally {
            ResourceUtils.closeQuietly( is );
        }
    }

    @Test
    public void testDefaultCache() throws Exception {
        ServerCacheStorageAssertion storass = new ServerCacheStorageAssertion(
            new CacheStorageAssertion() {{ setTarget(TargetMessageType.RESPONSE); }},
            beanFactory
        );
        ServerCacheLookupAssertion lookass = new ServerCacheLookupAssertion(
            new CacheLookupAssertion() {{ setTarget(TargetMessageType.RESPONSE); }},
            beanFactory
        );

        PolicyEnforcementContext ctx = makeContext();
        ctx.getResponse().initialize( XmlUtil.stringAsDocument( "<responseToCache/>" ) );
        String cachedString = messageBodyToString( ctx.getResponse() );
        assertTrue( cachedString.contains( "<responseToCache" ) ); // may be canonicalized; we'll accept it if so
        AssertionStatus storeResult = storass.checkRequest( ctx );
        assertEquals( AssertionStatus.NONE, storeResult);

        ctx = makeContext();
        ctx.getResponse().initialize( XmlUtil.stringAsDocument( "<responseToReplace/>" ) );
        AssertionStatus lookResult = lookass.checkRequest( ctx );

        // Must hit
        assertEquals( AssertionStatus.NONE, lookResult );
        assertEquals( messageBodyToString( ctx.getResponse() ), cachedString );
    }

    @Test
    public void testCachesDontOverlap() throws Exception {
        ServerCacheStorageAssertion storass = new ServerCacheStorageAssertion( new CacheStorageAssertion(), beanFactory );
        final CacheLookupAssertion lookbean = new CacheLookupAssertion();
        lookbean.setCacheId( "aDifferentCacheId" );
        ServerCacheLookupAssertion lookass = new ServerCacheLookupAssertion( lookbean, beanFactory );

        PolicyEnforcementContext ctx = makeContext();
        ctx.getResponse().initialize(XmlUtil.stringAsDocument("<responseToCache/>"));
        String cachedString = messageBodyToString(ctx.getResponse());
        assertTrue(cachedString.contains("<responseToCache")); // may be canonicalized; we'll accept it if so
        AssertionStatus storeResult = storass.checkRequest( ctx );
        assertEquals(AssertionStatus.NONE, storeResult);

        ctx = makeContext();
        ctx.getResponse().initialize( XmlUtil.stringAsDocument( "<responseToReplace/>" ) );
        String origResponse = messageBodyToString( ctx.getResponse() );
        assertTrue( origResponse.contains( "<responseToReplace" ) ); // may be canonicalized; we'll accept it if so
        AssertionStatus lookResult = lookass.checkRequest( ctx );

        // Must miss (different cache id)
        assertEquals( AssertionStatus.FALSIFIED, lookResult );
        assertEquals( origResponse, messageBodyToString( ctx.getResponse() ) );
    }

    @Test
    public void testCacheKeysMatter() throws Exception {
        ServerCacheStorageAssertion storass1 = makeStorAss( "myFunkyKey1" );
        ServerCacheLookupAssertion lookass1 = makeLookAss( "myFunkyKey1" );

        final String funkyResp1 = "<resp>funkyKey1</resp>";
        PolicyEnforcementContext ctx = makeContext( funkyResp1 );
        AssertionStatus storeResult1 = storass1.checkRequest(ctx);
        assertEquals(AssertionStatus.NONE, storeResult1);

        ServerCacheStorageAssertion storass2 = makeStorAss("myOtherKey2");
        ServerCacheLookupAssertion lookass2 = makeLookAss( "myOtherKey2" );

        final String otherResp2 = "<resp>otherKey2</resp>";
        ctx.close();
        ctx = makeContext( otherResp2 );
        AssertionStatus storeResult2 = storass2.checkRequest( ctx );
        assertEquals( AssertionStatus.NONE, storeResult2 );

        // Must hit
        ctx.close();
        ctx = makeContext();
        AssertionStatus lookResult1 = lookass1.checkRequest( ctx );
        assertEquals( AssertionStatus.NONE, lookResult1 );
        assertEquals( messageBodyToString( ctx.getResponse() ), funkyResp1 );

        // Must hit
        ctx.close();
        ctx = makeContext();
        AssertionStatus lookResult2 = lookass2.checkRequest( ctx );
        assertEquals( AssertionStatus.NONE, lookResult2 );
        assertEquals( messageBodyToString( ctx.getResponse() ), otherResp2 );
    }

    private static ServerCacheLookupAssertion makeLookAss( String cacheKey ) throws PolicyAssertionException {
        final CacheLookupAssertion lookbean1 = new CacheLookupAssertion();
        lookbean1.setTarget(TargetMessageType.RESPONSE);
        lookbean1.setCacheEntryKey(cacheKey);
        return new ServerCacheLookupAssertion( lookbean1, beanFactory );
    }

    private static ServerCacheStorageAssertion makeStorAss( String cacheKey ) throws PolicyAssertionException {
        final CacheStorageAssertion storbean1 = new CacheStorageAssertion();
        storbean1.setTarget(TargetMessageType.RESPONSE);
        storbean1.setCacheEntryKey( cacheKey );
        return new ServerCacheStorageAssertion( storbean1, beanFactory );
    }

    private static PolicyEnforcementContext makeContext() {
        return makeContext( "<myresponse/>" );
    }

    private static PolicyEnforcementContext makeContext( String res ) {
        Message request = new Message();
        request.initialize(XmlUtil.stringAsDocument("<myrequest/>"));
        Message response = new Message();
        response.initialize(XmlUtil.stringAsDocument(res));
        return PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
    }

    @Test
    public void testDefaultCacheShutdown() throws Exception {
        ServerCacheStorageAssertion storass = new ServerCacheStorageAssertion( new CacheStorageAssertion(), beanFactory );
        ServerCacheLookupAssertion lookass = new ServerCacheLookupAssertion( new CacheLookupAssertion(), beanFactory );

        PolicyEnforcementContext ctx = makeContext();
        ctx.getResponse().initialize( XmlUtil.stringAsDocument( "<responseToCache/>" ) );
        AssertionStatus storeResult = storass.checkRequest( ctx );
        assertEquals( storeResult, AssertionStatus.NONE );

        // Now shut down the cache and verify that it no longer finds the result
        eventProxy.onApplicationEvent( new Stopping( this, Component.GATEWAY, "0.0.0.0" ) );

        ctx = makeContext();
        ctx.getResponse().initialize( XmlUtil.stringAsDocument( "<responseToReplace/>" ) );
        String origResponse = messageBodyToString( ctx.getResponse() );
        assertTrue( origResponse.contains( "<responseToReplace" ) ); // may be canonicalized; we'll accept it if so
        AssertionStatus lookResult = lookass.checkRequest( ctx );

        // Must miss
        assertEquals( lookResult, AssertionStatus.FALSIFIED );
        assertEquals( messageBodyToString( ctx.getResponse() ), origResponse );
    }

    /**
     * Validate that pre fangtooth XML which contains a max age in seconds is correctly converted to seconds when
     * parsed.
     * @throws Exception
     */
    @Test
    @BugNumber(12094)
    public void testMaxEntryAgeWithPreFangtoothXml() throws Exception {
        validateMaxEntryAgeProperty(LOOKUP_PRE_FANGTOOTH, "2");
    }

    @Test
    @BugNumber(12094)
    public void testMaxEntryAgeWithPostFangtoothXml() throws Exception {
        validateMaxEntryAgeProperty(LOOKUP_POST_FANGTOOTH, "3");
    }

    @Test
    @BugNumber(12094)
    public void testCacheStorageWithPreFangtoothXml() throws Exception {
        cacheStorageHelper(STORAGE_PRE_FANGTOOTH, "2", "3", "5");
    }

    @Test
    @BugNumber(12094)
    public void testCacheStorageWithPostFangtoothXml() throws Exception {
        cacheStorageHelper(STORAGE_POST_FANGTOOTH, "7", "11", "13");
    }

    // - PRIVATE

    private final static String LOOKUP_PRE_FANGTOOTH = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            "        <L7p:CacheLookup>\n" +
            "            <L7p:ContentTypeOverride stringValue=\"\"/>\n" +
            "            <L7p:MaxEntryAgeMillis longValue=\"2000\"/>\n" +
            "            <L7p:Target target=\"REQUEST\"/>\n" +
            "        </L7p:CacheLookup>\n" +
            "    </wsp:All>\n" +
            "</wsp:Policy>";

    private final static String LOOKUP_POST_FANGTOOTH = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            "        <L7p:CacheLookup>\n" +
            "            <L7p:ContentTypeOverride stringValue=\"\"/>\n" +
            "            <L7p:MaxEntryAgeSeconds stringValue=\"3\"/>\n" +
            "            <L7p:Target target=\"REQUEST\"/>\n" +
            "        </L7p:CacheLookup>\n" +
            "    </wsp:All>\n" +
            "</wsp:Policy>\n";

    private final String STORAGE_PRE_FANGTOOTH = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "    <wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "        <wsp:All wsp:Usage=\"Required\">\n" +
            "                    <L7p:CacheStorage>\n" +
            "                        <L7p:MaxEntries intValue=\"2\"/>\n" +
            "                        <L7p:MaxEntryAgeMillis longValue=\"3\"/>\n" +
            "                        <L7p:MaxEntrySizeBytes longValue=\"5\"/>\n" +
            "                    </L7p:CacheStorage>\n" +
            "        </wsp:All>\n" +
            "    </wsp:Policy>";

    private final String STORAGE_POST_FANGTOOTH = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "    <wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "        <wsp:All wsp:Usage=\"Required\">\n" +
            "                    <L7p:CacheStorage>\n" +
            "                        <L7p:MaxEntries stringValue=\"7\"/>\n" +
            "                        <L7p:MaxEntryAgeMillis stringValue=\"11\"/>\n" +
            "                        <L7p:MaxEntrySizeBytes stringValue=\"13\"/>\n" +
            "                    </L7p:CacheStorage>\n" +
            "        </wsp:All>\n" +
            "    </wsp:Policy>";

    private void validateMaxEntryAgeProperty(final String xml, final String expectedMaxAgeValueInSeconds) throws Exception {
        AssertionRegistry assReg = new AssertionRegistry();
        assReg.registerAssertion(CacheLookupAssertion.class);
        WspConstants.setTypeMappingFinder(assReg);
        Assertion ass = WspReader.getDefault().parseStrictly(xml, WspReader.INCLUDE_DISABLED);

        AllAssertion allAss = (AllAssertion) ass;
        final List<Assertion> children = allAss.getChildren();
        final CacheLookupAssertion cacheLookupAss = (CacheLookupAssertion) children.get(0);
        assertEquals(expectedMaxAgeValueInSeconds, cacheLookupAss.getMaxEntryAgeSeconds());
    }

    private void cacheStorageHelper(final String xml, final String maxEntries, final String maxAgeMillis, final String maxSizeBytes) throws Exception {
        AssertionRegistry assReg = new AssertionRegistry();
        assReg.registerAssertion(CacheStorageAssertion.class);
        WspConstants.setTypeMappingFinder(assReg);
        Assertion ass = WspReader.getDefault().parseStrictly(xml, WspReader.INCLUDE_DISABLED);

        AllAssertion allAss = (AllAssertion) ass;
        final List<Assertion> children = allAss.getChildren();
        final CacheStorageAssertion cacheStorageAss = (CacheStorageAssertion) children.get(0);
        assertEquals(maxEntries, cacheStorageAss.getMaxEntries());
        assertEquals(maxAgeMillis, cacheStorageAss.getMaxEntryAgeMillis());
        assertEquals(maxSizeBytes, cacheStorageAss.getMaxEntrySizeBytes());
    }

    private static final class TestStashManagerFactory implements StashManagerFactory {
        private static AtomicLong stashFileUnique = new AtomicLong( 0 );

        private static long getStashFileUnique() {
            return stashFileUnique.getAndIncrement();
        }

        private static class ConfigHolder {
            private static final int DISK_THRESHOLD = ServerConfig.getInstance().getAttachmentDiskThreshold();
            private static final File ATTACHMENT_DIR;
            private static final String PREFIX;

            static {
                try {
                    File temp = File.createTempFile( "temp", ".txt" );
                    temp.delete();
                    ATTACHMENT_DIR = temp.getParentFile();
                    System.out.println( "Using temp attachment directory '" + ATTACHMENT_DIR.getAbsolutePath() + "'." );
                    PREFIX = "att" + temp.getName().replace( ".txt", "T" );
                }
                catch( IOException ioe ) {
                    throw new RuntimeException( "Could not get temporary directory!", ioe );
                }
            }

            private static final StashManagerFactory INSTANCE = new TestStashManagerFactory();
        }

        /**
         * Get the TestStashManagerFactory.
         *
         * @return The test StashManagerFactory.
         */
        public static StashManagerFactory getInstance() {
            return ConfigHolder.INSTANCE;
        }

        /**
         * Create a new StashManager to use for some request.  A HybridStashManager will be created
         *
         * @return a new StashManager instance.  Never null.
         */
        @Override
        public StashManager createStashManager() {
            StashManager stashManager = new HybridStashManager( ConfigHolder.DISK_THRESHOLD,
                    ConfigHolder.ATTACHMENT_DIR,
                    ConfigHolder.PREFIX + getStashFileUnique() );
            return stashManager;
        }
    }
}
