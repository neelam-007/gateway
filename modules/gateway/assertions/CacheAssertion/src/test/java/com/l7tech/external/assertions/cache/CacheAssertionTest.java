package com.l7tech.external.assertions.cache;

import com.l7tech.common.io.IOUtils;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.StashManager;
import com.l7tech.common.mime.HybridStashManager;
import com.l7tech.external.assertions.cache.server.ServerCacheLookupAssertion;
import com.l7tech.external.assertions.cache.server.ServerCacheStorageAssertion;
import com.l7tech.gateway.common.Component;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.event.system.Stopping;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.util.ResourceUtils;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Test the CacheStorageAssertion and the CacheLookupAssertion.
 *
 * @noinspection JavaDoc
 */
public class CacheAssertionTest extends TestCase {
    private static final ApplicationEventProxy eventProxy = new ApplicationEventProxy();
    private static final StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();

    static {
        beanFactory.addBean( "stashManagerFactory", TestStashManagerFactory.getInstance() );
        beanFactory.addBean( "applicationEventProxy", eventProxy );
    }

    public CacheAssertionTest( String name ) {
        super( name );
    }

    public static Test suite() {
        return new TestSuite( CacheAssertionTest.class );
    }

    public static void main( String[] args ) {
        junit.textui.TestRunner.run( suite() );
    }

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

    public void testDefaultCache() throws Exception {
        ServerCacheStorageAssertion storass = new ServerCacheStorageAssertion( new CacheStorageAssertion(), beanFactory );
        ServerCacheLookupAssertion lookass = new ServerCacheLookupAssertion( new CacheLookupAssertion(), beanFactory );

        PolicyEnforcementContext ctx = makeContext();
        ctx.getResponse().initialize( XmlUtil.stringAsDocument( "<responseToCache/>" ) );
        String cachedString = messageBodyToString( ctx.getResponse() );
        assertTrue( cachedString.contains( "<responseToCache" ) ); // may be canonicalized; we'll accept it if so
        AssertionStatus storeResult = storass.checkRequest( ctx );
        assertEquals( storeResult, AssertionStatus.NONE );

        ctx = makeContext();
        ctx.getResponse().initialize( XmlUtil.stringAsDocument( "<responseToReplace/>" ) );
        AssertionStatus lookResult = lookass.checkRequest( ctx );

        // Must hit
        assertEquals( lookResult, AssertionStatus.NONE );
        assertEquals( messageBodyToString( ctx.getResponse() ), cachedString );
    }

    public void testCachesDontOverlap() throws Exception {
        ServerCacheStorageAssertion storass = new ServerCacheStorageAssertion( new CacheStorageAssertion(), beanFactory );
        final CacheLookupAssertion lookbean = new CacheLookupAssertion();
        lookbean.setCacheId( "aDifferentCacheId" );
        ServerCacheLookupAssertion lookass = new ServerCacheLookupAssertion( lookbean, beanFactory );

        PolicyEnforcementContext ctx = makeContext();
        ctx.getResponse().initialize( XmlUtil.stringAsDocument( "<responseToCache/>" ) );
        String cachedString = messageBodyToString( ctx.getResponse() );
        assertTrue( cachedString.contains( "<responseToCache" ) ); // may be canonicalized; we'll accept it if so
        AssertionStatus storeResult = storass.checkRequest( ctx );
        assertEquals( storeResult, AssertionStatus.NONE );

        ctx = makeContext();
        ctx.getResponse().initialize( XmlUtil.stringAsDocument( "<responseToReplace/>" ) );
        String origResponse = messageBodyToString( ctx.getResponse() );
        assertTrue( origResponse.contains( "<responseToReplace" ) ); // may be canonicalized; we'll accept it if so
        AssertionStatus lookResult = lookass.checkRequest( ctx );

        // Must miss (different cache id)
        assertEquals( AssertionStatus.FALSIFIED, lookResult );
        assertEquals( messageBodyToString( ctx.getResponse() ), origResponse );
    }

    public void testCacheKeysMatter() throws Exception {
        ServerCacheStorageAssertion storass1 = makeStorAss( "myFunkyKey1" );
        ServerCacheLookupAssertion lookass1 = makeLookAss( "myFunkyKey1" );

        final String funkyResp1 = "<resp>funkyKey1</resp>";
        PolicyEnforcementContext ctx = makeContext( funkyResp1 );
        AssertionStatus storeResult1 = storass1.checkRequest( ctx );
        assertEquals( storeResult1, AssertionStatus.NONE );

        ServerCacheStorageAssertion storass2 = makeStorAss( "myOtherKey2" );
        ServerCacheLookupAssertion lookass2 = makeLookAss( "myOtherKey2" );

        final String otherResp2 = "<resp>otherKey2</resp>";
        ctx.close();
        ctx = makeContext( otherResp2 );
        AssertionStatus storeResult2 = storass2.checkRequest( ctx );
        assertEquals( storeResult2, AssertionStatus.NONE );

        // Must hit
        ctx.close();
        ctx = makeContext();
        AssertionStatus lookResult1 = lookass1.checkRequest( ctx );
        assertEquals( lookResult1, AssertionStatus.NONE );
        assertEquals( messageBodyToString( ctx.getResponse() ), funkyResp1 );

        // Must hit
        ctx.close();
        ctx = makeContext();
        AssertionStatus lookResult2 = lookass2.checkRequest( ctx );
        assertEquals( lookResult2, AssertionStatus.NONE );
        assertEquals( messageBodyToString( ctx.getResponse() ), otherResp2 );
    }

    private static ServerCacheLookupAssertion makeLookAss( String cacheKey ) throws PolicyAssertionException {
        final CacheLookupAssertion lookbean1 = new CacheLookupAssertion();
        lookbean1.setCacheEntryKey( cacheKey );
        return new ServerCacheLookupAssertion( lookbean1, beanFactory );
    }

    private static ServerCacheStorageAssertion makeStorAss( String cacheKey ) throws PolicyAssertionException {
        final CacheStorageAssertion storbean1 = new CacheStorageAssertion();
        storbean1.setCacheEntryKey( cacheKey );
        return new ServerCacheStorageAssertion( storbean1, beanFactory );
    }

    private static PolicyEnforcementContext makeContext() {
        return makeContext( "<myresponse/>" );
    }

    private static PolicyEnforcementContext makeContext( String res ) {
        Message request = new Message();
        request.initialize( XmlUtil.stringAsDocument( "<myrequest/>" ) );
        Message response = new Message();
        response.initialize( XmlUtil.stringAsDocument( res ) );
        return new PolicyEnforcementContext( request, response );
    }

    // This test shuts down the cache and so must be the final test executed
    /**
     * @noinspection InstanceMethodNamingConvention
     */
    public void test_FINALTEST_DefaultCacheShutdown() throws Exception {
        ServerCacheStorageAssertion storass = new ServerCacheStorageAssertion( new CacheStorageAssertion(), beanFactory );
        ServerCacheLookupAssertion lookass = new ServerCacheLookupAssertion( new CacheLookupAssertion(), beanFactory );

        PolicyEnforcementContext ctx = makeContext();
        ctx.getResponse().initialize( XmlUtil.stringAsDocument( "<responseToCache/>" ) );
        AssertionStatus storeResult = storass.checkRequest( ctx );
        assertEquals( AssertionStatus.NONE, storeResult );

        // Now shut down the cache and verify that it no longer finds the result
        eventProxy.onApplicationEvent( new Stopping( this, Component.GATEWAY, "0.0.0.0" ) );

        ctx = makeContext();
        ctx.getResponse().initialize( XmlUtil.stringAsDocument( "<responseToReplace/>" ) );
        String origResponse = messageBodyToString( ctx.getResponse() );
        assertTrue( origResponse.contains( "<responseToReplace" ) ); // may be canonicalized; we'll accept it if so
        AssertionStatus lookResult = lookass.checkRequest( ctx );

        // Must miss
        assertEquals( AssertionStatus.FALSIFIED, lookResult );
        assertEquals( messageBodyToString( ctx.getResponse() ), origResponse );
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
        public StashManager createStashManager() {
            StashManager stashManager = new HybridStashManager( ConfigHolder.DISK_THRESHOLD,
                    ConfigHolder.ATTACHMENT_DIR,
                    ConfigHolder.PREFIX + getStashFileUnique() );
            return stashManager;
        }
    }
}
