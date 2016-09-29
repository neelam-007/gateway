package com.l7tech.server;

import com.l7tech.common.TestKeys;
import com.l7tech.common.io.CertGenParams;
import com.l7tech.common.io.KeyGenParams;
import com.l7tech.gateway.common.audit.Messages;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.security.keystore.SsgKeyMetadata;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.ObjectNotFoundException;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.security.keystore.SsgKeyFinder;
import com.l7tech.server.security.keystore.SsgKeyStore;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.util.GoidUpgradeMapperTestUtil;
import com.l7tech.util.NotFuture;
import com.l7tech.util.SyspropUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;

/**
 * This was created: 8/16/13 as 1:32 PM
 *
 * @author Victor Kazakov
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultKeyImplTest {

    ServerConfig serverConfig = ServerConfig.getInstance();
    @Mock
    ClusterPropertyManager clusterPropertyManager;
    @Mock
    SsgKeyStoreManager keyStoreManager;
    @Mock
    SsgKeyStore ssgKeyStore;
    @Mock
    PlatformTransactionManager transactionManager;
    @Mock
    TransactionStatus transactionStatus;

    X509Certificate testCert;
    PrivateKey testKey;

    DefaultKeyImpl defaultKey;
    private Goid keystoreGoid = new Goid(0, 3);
    private long keystoreOid = 3;
    private String alias = "mycaKey";

    @Before
    public void setTestCertAndKey() throws Exception {
        testCert = TestKeys.getCert( TestKeys.RSA_2048_CERT_X509_B64 );
        testKey = TestKeys.getKey( "RSA", TestKeys.RSA_2048_KEY_PKCS8_B64 );
    }

    @After
    public void cleanup() {
        SyspropUtil.clearProperties(
                "com.l7tech.bootstrap.env.sslkey.enable"
        );
    }

    private void prepareTestKey() throws FindException, KeyStoreException {
        defaultKey = new DefaultKeyImpl(serverConfig, clusterPropertyManager, keyStoreManager, transactionManager);

        SsgKeyEntry key = SsgKeyEntry.createDummyEntityForAuditing(keystoreGoid, alias);
        Mockito.when(keyStoreManager.lookupKeyByKeyAlias(alias, keystoreGoid)).thenReturn(key);
        GoidUpgradeMapperTestUtil.addPrefix("keystore_file", 0);
    }

    @Test
    public void testCreateNewSslKey() throws Exception {
        Mockito.when( keyStoreManager.lookupKeyByKeyAlias( anyString(), Matchers.<Goid>anyObject() ) ).
                thenThrow( new ObjectNotFoundException( "No key with this alias" ) );

        Mockito.when( keyStoreManager.findAll() ).thenReturn( Collections.<SsgKeyFinder>singletonList( ssgKeyStore ) );

        Mockito.when( ssgKeyStore.isMutable() ).thenReturn( true );
        Mockito.when( ssgKeyStore.getKeyStore() ).thenReturn( ssgKeyStore );
        Mockito.when( ssgKeyStore.generateKeyPair( Matchers.<Runnable>anyObject(), anyString(), Matchers.<KeyGenParams>anyObject(),
                Matchers.<CertGenParams>anyObject(), Matchers.<SsgKeyMetadata>anyObject() ) ).
                thenReturn( new NotFuture<>( testCert ) );

        // At this point, the default SSL key is supposed to have been generated successfully
        SsgKeyEntry createdKeyEntry = SsgKeyEntry.createDummyEntityForAuditing( new Goid( 0, 1 ), "SSL" );
        Mockito.when( ssgKeyStore.getCertificateChain( "SSL" ) ).
                thenReturn( createdKeyEntry );

        Mockito.when( transactionManager.getTransaction( Matchers.<TransactionDefinition>any() ) ).
                thenReturn( transactionStatus );

        defaultKey = new DefaultKeyImpl(serverConfig, clusterPropertyManager, keyStoreManager, transactionManager);
        SsgKeyEntry sslInfo = defaultKey.getSslInfo();

        Mockito.verify( ssgKeyStore, times( 1 ) ).generateKeyPair( Matchers.<Runnable>anyObject(), anyString(), Matchers.<KeyGenParams>anyObject(),
                Matchers.<CertGenParams>anyObject(), Matchers.<SsgKeyMetadata>anyObject() );
        Mockito.verify( ssgKeyStore, times( 0 ) ).storePrivateKeyEntry( anyBoolean(), Mockito.<Runnable>anyObject(),
                Mockito.<SsgKeyEntry>anyObject(), anyBoolean() );

        assertTrue( sslInfo == createdKeyEntry );

    }

    @Test
    public void testCreateDefaultKeyFromEnvironment() throws Exception {
        SyspropUtil.setProperty( "com.l7tech.bootstrap.env.sslkey.enable", "true" );

        Mockito.when( keyStoreManager.lookupKeyByKeyAlias( anyString(), Matchers.<Goid>anyObject() ) ).
                thenThrow( new ObjectNotFoundException( "No key with this alias" ) );

        Mockito.when( keyStoreManager.findAll() ).thenReturn( Collections.<SsgKeyFinder>singletonList( ssgKeyStore ) );

        Mockito.when( ssgKeyStore.isMutable() ).thenReturn( true );
        Mockito.when( ssgKeyStore.getKeyStore() ).thenReturn( ssgKeyStore );
        Mockito.when( ssgKeyStore.generateKeyPair( Matchers.<Runnable>anyObject(), anyString(), Matchers.<KeyGenParams>anyObject(),
                Matchers.<CertGenParams>anyObject(), Matchers.<SsgKeyMetadata>anyObject() ) ).
                thenReturn( new NotFuture<>( testCert ) );
        Mockito.when( ssgKeyStore.storePrivateKeyEntry( anyBoolean(), Mockito.<Runnable>anyObject(),
                Mockito.<SsgKeyEntry>anyObject(), anyBoolean() ) ).
                thenReturn( new NotFuture<>( true ) );

        // At this point, the default SSL key is supposed to have been generated successfully
        SsgKeyEntry createdKeyEntry = SsgKeyEntry.createDummyEntityForAuditing( new Goid( 0, 1 ), "SSL" );
        Mockito.when( ssgKeyStore.getCertificateChain( "SSL" ) ).
                thenReturn( createdKeyEntry );

        Mockito.when( transactionManager.getTransaction( Matchers.<TransactionDefinition>any() ) ).
                thenReturn( transactionStatus );

        final Map<String, String> ENV = new HashMap<String, String>();
        ENV.put( "SSG_SSL_KEY", SSG_SSL_KEY );
        ENV.put( "SSG_SSL_KEY_PASS", SSG_SSL_KEY_PASS );

        defaultKey = new DefaultKeyImpl(serverConfig, clusterPropertyManager, keyStoreManager, transactionManager) {
            @Override
            protected String getenv( String env ) {
                return ENV.get( env );
            }
        };

        SsgKeyEntry sslInfo = defaultKey.getSslInfo();

        Mockito.verify( ssgKeyStore, times( 0 ) ).generateKeyPair( Matchers.<Runnable>anyObject(), anyString(), Matchers.<KeyGenParams>anyObject(),
                Matchers.<CertGenParams>anyObject(), Matchers.<SsgKeyMetadata>anyObject() );
        Mockito.verify( ssgKeyStore, times( 1 ) ).storePrivateKeyEntry( anyBoolean(), Mockito.<Runnable>anyObject(),
                Mockito.<SsgKeyEntry>anyObject(), anyBoolean() );

        assertTrue( sslInfo == createdKeyEntry );
    }


    @Test
    public void testPropertyWithOid() throws Exception {
        prepareTestKey();

        System.setProperty("com.l7tech.server.keyStore.defaultCa.alias", keystoreOid+":"+alias);

        SsgKeyEntry key = defaultKey.getCaInfo();

        assertNotNull(key);
        Assert.assertEquals(keystoreGoid, key.getKeystoreId());
        Assert.assertEquals(alias, key.getAlias());
        Assert.assertEquals(keystoreGoid+":"+alias, key.getId());
    }

    @Test
    public void testPropertyWithGoid() throws KeyStoreException, FindException {
        prepareTestKey();

        System.setProperty("com.l7tech.server.keyStore.defaultCa.alias", keystoreGoid+":"+alias);

        SsgKeyEntry key = defaultKey.getCaInfo();

        assertNotNull(key);
        Assert.assertEquals(keystoreGoid, key.getKeystoreId());
        Assert.assertEquals(alias, key.getAlias());
        Assert.assertEquals(keystoreGoid+":"+alias, key.getId());
    }

    private static final String SSG_SSL_KEY_PASS = "7layer";

    private static final String SSG_SSL_KEY =
        "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" +
        "g4xbLTHgICBAAAAA==";
}
