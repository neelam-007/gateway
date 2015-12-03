package com.l7tech.external.assertions.mongodb;

import com.l7tech.external.assertions.mongodb.entity.MongoDBConnectionEntity;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.mongodb.MongoClient;
import com.mongodb.ReadPreference;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;
import java.security.Provider;
import java.security.SecureRandom;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;


/**
 * Created by mobna01 on 09/01/15.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Goid.class, JceProvider.class, SSLContext.class, SSLSocketFactory.class})
@PowerMockIgnore("javax.management.*")
public class MongoDBConnectionManagerTest {

    MongoDBConnectionManager mongoDBConnectionManager = null;

    @Mock
    SsgKeyStoreManager mockKeyStoreManager;
    @Mock
    DefaultKey mockDefaultKey;
    @Mock
    MongoDBConnectionEntity mockMongoDBConnectionEntity;


    @Before
    public void setup() throws Exception {

        SecurePassword mockSecurePassword = Mockito.mock(SecurePassword.class);
        SecureRandom mockSecureRandom = Mockito.mock(SecureRandom.class);
        X509TrustManager mockX509TrustManager = Mockito.mock(X509TrustManager.class);
        SecurePasswordManager mockSecurePasswordManager = Mockito.mock(SecurePasswordManager.class);
        Goid mockGoid = Mockito.mock(Goid.class);

        MongoDBConnectionManager.createMongoDBConnectionManager(mockSecurePasswordManager, mockKeyStoreManager, mockX509TrustManager, mockSecureRandom, mockDefaultKey);
        mongoDBConnectionManager = MongoDBConnectionManager.getInstance();

        when(mockMongoDBConnectionEntity.getUri()).thenReturn("mongodb.ca.com");
        when(mockMongoDBConnectionEntity.getPort()).thenReturn("27017");
        when(mockMongoDBConnectionEntity.getDatabaseName()).thenReturn("DBName");
        when(mockMongoDBConnectionEntity.getStoredPasswordGoid()).thenReturn(mockGoid);
        when(mockSecurePasswordManager.findByPrimaryKey(mockGoid)).thenReturn(mockSecurePassword);
        when(mockSecurePasswordManager.findByPrimaryKey(mockGoid).getEncodedPassword()).thenReturn("EncodedPassword");
        when(mockSecurePasswordManager.decryptPassword(anyString())).thenReturn(new char[]{});
        when(mockMongoDBConnectionEntity.getStoredPasswordGoid()).thenReturn(PersistentEntity.DEFAULT_GOID);
        when(mockMongoDBConnectionEntity.getNonDefaultKeystoreId()).thenReturn(mockGoid);

    }

    @After
    public void teardown() throws Exception {
        MongoDBConnectionManager.setInstance(null);
    }

    /**
     * Basic mongoclient object creation with no credentials
     *
     * Test will verify that:
     * ** mongoclient object is not null
     * ** credential is empty
     * ** SSLFactory is default SSLFactory
     *
     * @throws Exception
     */
    @Test
    public void testBasicWithNoCredentials() throws Exception {
        when(mockMongoDBConnectionEntity.getAuthType()).thenReturn(MongoDBEncryption.NO_ENCRYPTION.name());
        when(mockMongoDBConnectionEntity.getUsername()).thenReturn("");
        when(mockMongoDBConnectionEntity.getPassword()).thenReturn(null);

        mongoDBConnectionManager.addConnection(mockMongoDBConnectionEntity);

        MongoClient mongoClient = mongoDBConnectionManager.getConnection(any(Goid.class)).getMongoClient();
        assertNotNull(mongoClient);
        assertEquals(mongoClient.getCredentialsList().size(), 0);
        assertTrue(mongoClient.getMongoClientOptions().getSocketFactory().equals(SocketFactory.getDefault()));

    }

    @Test
    public void testMongoClientIsNullWhenConnectionURIisNull() throws Exception {
        when(mockMongoDBConnectionEntity.getUri()).thenReturn(null);

        mongoDBConnectionManager.addConnection(mockMongoDBConnectionEntity);

        MongoClient mongoClient = mongoDBConnectionManager.getConnection(any(Goid.class)).getMongoClient();
        assertNull(mongoClient);
    }


    @Test
    public void testMongoClientIsNullWhenDBNameIsEmpty() throws Exception {
        when(mockMongoDBConnectionEntity.getDatabaseName()).thenReturn("");

        mongoDBConnectionManager.addConnection(mockMongoDBConnectionEntity);

        MongoClient mongoClient = mongoDBConnectionManager.getConnection(any(Goid.class)).getMongoClient();
        assertNull(mongoClient);
    }

    @Test
    public void testMongoClientIsNullWhenPortIsEmpty() throws Exception {
        when(mockMongoDBConnectionEntity.getPort()).thenReturn("");

        mongoDBConnectionManager.addConnection(mockMongoDBConnectionEntity);

        MongoClient mongoClient = mongoDBConnectionManager.getConnection(any(Goid.class)).getMongoClient();
        assertNull(mongoClient);
    }

    /**
     * Basic mongoclient object creation with credentials
     *
     * Test will verify that:
     * ** mongoclient object is not null
     * ** credential is provided
     * ** credential mechanism is MONGODB-CR
     * ** SSLFactory is default SSLFactory
     *
     * @throws Exception
     */
    @Test
    public void testBasicWithCredentials() throws Exception {

        when(mockMongoDBConnectionEntity.getAuthType()).thenReturn(MongoDBEncryption.NO_ENCRYPTION.name());
        when(mockMongoDBConnectionEntity.getUsername()).thenReturn("user");
        when(mockMongoDBConnectionEntity.getPassword()).thenReturn("password");

        mongoDBConnectionManager.addConnection(mockMongoDBConnectionEntity);

        MongoClient mongoClient = mongoDBConnectionManager.getConnection(any(Goid.class)).getMongoClient();
        assertNotNull(mongoClient);
        assertEquals(mongoClient.getCredentialsList().size(), 1);
        assertEquals(mongoClient.getCredentialsList().get(0).getMechanism(), "MONGODB-CR");
        assertTrue(mongoClient.getMongoClientOptions().getSocketFactory().equals(SocketFactory.getDefault()));
    }

    /**
     * Basic mongoclient object creation with credentials, but no auth type
     *
     * Test will verify that:
     * ** mongoclient object is not null
     * ** credential is provided
     * ** credential mechanism is MONGODB-CR
     * ** SSLFactory is default SSLFactory
     *
     * @throws Exception
     */
    @Test
    public void testBasicWithCredentialsWithNullAuthType() throws Exception {

        //when(mockMongoDBConnectionEntity.getAuthType()).thenReturn(MongoDBEncryption.NO_ENCRYPTION.name());
        when(mockMongoDBConnectionEntity.getUsername()).thenReturn("user");
        when(mockMongoDBConnectionEntity.getPassword()).thenReturn("password");

        mongoDBConnectionManager.addConnection(mockMongoDBConnectionEntity);

        MongoClient mongoClient = mongoDBConnectionManager.getConnection(any(Goid.class)).getMongoClient();
        assertNotNull(mongoClient);
        assertEquals(mongoClient.getCredentialsList().size(), 1);
        assertEquals(mongoClient.getCredentialsList().get(0).getMechanism(), "MONGODB-CR");
        assertTrue(mongoClient.getMongoClientOptions().getSocketFactory().equals(SocketFactory.getDefault()));
    }

    /**
     * SSL Encryption with no private key and no credentials
     *
     * Test will verify that:
     * ** mongoclient object is not null
     * ** credential list is empty
     * ** SSLFactory object is NOT the default SSLFactory
     * ** code for none-private key is not called
     *
     * @throws Exception
     */
    @Test
    public void testSSLWithNokeyWithNoCredentials() throws Exception {

        SSLContext mockSSLContext = PowerMockito.mock(SSLContext.class);
        PowerMockito.mockStatic(SSLContext.class);

            JceProvider mockJceProvider = Mockito.mock(JceProvider.class);
        PowerMockito.mockStatic(JceProvider.class);

        SSLSocketFactory mockSSLFactory = PowerMockito.mock(SSLSocketFactory.class);

        when(mockMongoDBConnectionEntity.getAuthType()).thenReturn(MongoDBEncryption.SSL.name());
        when(mockMongoDBConnectionEntity.getUsername()).thenReturn(null);
        when(mockMongoDBConnectionEntity.getPassword()).thenReturn("");

        when(mockMongoDBConnectionEntity.isUsesNoKey()).thenReturn(true);
        when(mockMongoDBConnectionEntity.isUsesDefaultKeyStore()).thenReturn(false);

        when(JceProvider.getInstance()).thenReturn(mockJceProvider);
        when(SSLContext.getInstance(anyString(), any(Provider.class))).thenReturn(mockSSLContext);
        when(mockSSLContext.getSocketFactory()).thenReturn(mockSSLFactory);

        mongoDBConnectionManager.addConnection(mockMongoDBConnectionEntity);

        MongoClient mongoClient = mongoDBConnectionManager.getConnection(any(Goid.class)).getMongoClient();
        assertNotNull(mongoClient);
        assertEquals(mongoClient.getCredentialsList().size(), 0);
        assertFalse(mongoClient.getMongoClientOptions().getSocketFactory().equals(SocketFactory.getDefault()));
        verify(mockDefaultKey, times(0)).getSslKeyManagers();
        verify(mockKeyStoreManager, times(0)).lookupKeyByKeyAlias(any(String.class), any(Goid.class));
    }

    /**
     * SSL Encryption with no default key and no credentials
     *
     * Test will verify that:
     * ** mongoclient object is not null
     * ** credential list is empty
     * ** SSLFactory object is NOT the default SSLFactory
     * ** code for default private key is called
     *
     * @throws Exception
     */
    @Test
    public void testSSLWithDefaultkeyWithNoCred() throws Exception {

        SSLContext mockSSLContext = PowerMockito.mock(SSLContext.class);
        PowerMockito.mockStatic(SSLContext.class);

        JceProvider mockJceProvider = Mockito.mock(JceProvider.class);
        PowerMockito.mockStatic(JceProvider.class);

        SSLSocketFactory mockSSLFactory = PowerMockito.mock(SSLSocketFactory.class);

        when(mockMongoDBConnectionEntity.getAuthType()).thenReturn(MongoDBEncryption.SSL.name());
        when(mockMongoDBConnectionEntity.getUsername()).thenReturn(null);
        when(mockMongoDBConnectionEntity.getPassword()).thenReturn("");

        when(mockMongoDBConnectionEntity.isUsesNoKey()).thenReturn(false);
        when(mockMongoDBConnectionEntity.isUsesDefaultKeyStore()).thenReturn(true);

        when(JceProvider.getInstance()).thenReturn(mockJceProvider);
        when(SSLContext.getInstance(anyString(), any(Provider.class))).thenReturn(mockSSLContext);
        when(mockSSLContext.getSocketFactory()).thenReturn(mockSSLFactory);

        mongoDBConnectionManager.addConnection(mockMongoDBConnectionEntity);

        MongoClient mongoClient = mongoDBConnectionManager.getConnection(any(Goid.class)).getMongoClient();
        assertNotNull(mongoClient);
        assertEquals(mongoClient.getCredentialsList().size(), 0);
        assertFalse(mongoClient.getMongoClientOptions().getSocketFactory().equals(SocketFactory.getDefault()));
        verify(mockDefaultKey).getSslKeyManagers();
    }

    /**
     * SSL Encryption with private key and credentials
     *
     * Test will verify that:
     * ** mongoclient object is not null
     * ** credential list has a size of 1
     * ** crednetial mechanism is MONGODB-CR
     * ** SSLFactory object is NOT the default SSLFactory
     * ** code for private key is called
     *
     * @throws Exception
     */
    @Test
    public void testSSLWithKeyWithCred() throws Exception {

        SSLContext mockSSLContext = PowerMockito.mock(SSLContext.class);
        PowerMockito.mockStatic(SSLContext.class);

        JceProvider mockJceProvider = Mockito.mock(JceProvider.class);
        PowerMockito.mockStatic(JceProvider.class);

        SSLSocketFactory mockSSLFactory = PowerMockito.mock(SSLSocketFactory.class);
        SsgKeyEntry mockKeyEntry = Mockito.mock(SsgKeyEntry.class);

        when(mockMongoDBConnectionEntity.getAuthType()).thenReturn(MongoDBEncryption.SSL.name());
        when(mockMongoDBConnectionEntity.getUsername()).thenReturn("user");
        when(mockMongoDBConnectionEntity.getPassword()).thenReturn("password");

        when(mockMongoDBConnectionEntity.isUsesNoKey()).thenReturn(false);
        when(mockMongoDBConnectionEntity.isUsesDefaultKeyStore()).thenReturn(false);

        when(JceProvider.getInstance()).thenReturn(mockJceProvider);
        when(SSLContext.getInstance(anyString(), any(Provider.class))).thenReturn(mockSSLContext);
        when(mockSSLContext.getSocketFactory()).thenReturn(mockSSLFactory);
        when(mockKeyStoreManager.lookupKeyByKeyAlias(any(String.class), any(Goid.class))).thenReturn(mockKeyEntry);

        mongoDBConnectionManager.addConnection(mockMongoDBConnectionEntity);

        MongoClient mongoClient = mongoDBConnectionManager.getConnection(any(Goid.class)).getMongoClient();
        assertNotNull(mongoClient);
        assertEquals(mongoClient.getCredentialsList().size(), 1);
        assertEquals(mongoClient.getCredentialsList().get(0).getMechanism(), "MONGODB-CR");
        assertFalse(mongoClient.getMongoClientOptions().getSocketFactory().equals(SocketFactory.getDefault()));
        verify(mockKeyStoreManager).lookupKeyByKeyAlias(any(String.class), any(Goid.class));
    }

    /**
     * X509 Encryption with private key and no credentials
     *
     * Test will verify that:
     * ** mongoclient object is not null
     * ** credential list has a size of 0
     * ** SSLFactory object is NOT the default SSLFactory
     * ** code for private key is called
     *
     * @throws Exception
     */
    @Test
    public void testX509WithKeyWithNoCred() throws Exception {
        SSLContext mockSSLContext = PowerMockito.mock(SSLContext.class);
        PowerMockito.mockStatic(SSLContext.class);

        JceProvider mockJceProvider = Mockito.mock(JceProvider.class);
        PowerMockito.mockStatic(JceProvider.class);

        SSLSocketFactory mockSSLFactory = PowerMockito.mock(SSLSocketFactory.class);
        SsgKeyEntry mockKeyEntry = Mockito.mock(SsgKeyEntry.class);

        when(mockMongoDBConnectionEntity.getAuthType()).thenReturn(MongoDBEncryption.X509_Auth.name());
        when(mockMongoDBConnectionEntity.getUsername()).thenReturn("");

        when(mockMongoDBConnectionEntity.isUsesNoKey()).thenReturn(false);
        when(mockMongoDBConnectionEntity.isUsesDefaultKeyStore()).thenReturn(false);

        when(JceProvider.getInstance()).thenReturn(mockJceProvider);
        when(SSLContext.getInstance(anyString(), any(Provider.class))).thenReturn(mockSSLContext);
        when(mockSSLContext.getSocketFactory()).thenReturn(mockSSLFactory);
        when(mockKeyStoreManager.lookupKeyByKeyAlias(any(String.class), any(Goid.class))).thenReturn(mockKeyEntry);

        mongoDBConnectionManager.addConnection(mockMongoDBConnectionEntity);

        MongoClient mongoClient = mongoDBConnectionManager.getConnection(any(Goid.class)).getMongoClient();
        assertNotNull(mongoClient);
        assertEquals(mongoClient.getCredentialsList().size(), 0);
        assertFalse(mongoClient.getMongoClientOptions().getSocketFactory().equals(SocketFactory.getDefault()));
        verify(mockKeyStoreManager).lookupKeyByKeyAlias(any(String.class), any(Goid.class));
    }

    /**
     * X509 Encryption with default private key and credentials
     *
     * Test will verify that:
     * ** mongoclient object is not null
     * ** credential list has a size of 1
     * ** crednetial mechanism is MONGODB-X509
     * ** SSLFactory object is NOT the default SSLFactory
     * ** code for default private key is called
     *
     * @throws Exception
     */
    @Test
    public void testX509WithDefaultKeyWithCred() throws Exception {

        SSLContext mockSSLContext = PowerMockito.mock(SSLContext.class);
        PowerMockito.mockStatic(SSLContext.class);

        JceProvider mockJceProvider = Mockito.mock(JceProvider.class);
        PowerMockito.mockStatic(JceProvider.class);

        SSLSocketFactory mockSSLFactory = PowerMockito.mock(SSLSocketFactory.class);

        when(mockMongoDBConnectionEntity.getAuthType()).thenReturn(MongoDBEncryption.X509_Auth.name());
        when(mockMongoDBConnectionEntity.getUsername()).thenReturn("user");

        when(mockMongoDBConnectionEntity.isUsesNoKey()).thenReturn(false);
        when(mockMongoDBConnectionEntity.isUsesDefaultKeyStore()).thenReturn(true);

        when(JceProvider.getInstance()).thenReturn(mockJceProvider);
        when(SSLContext.getInstance(anyString(), any(Provider.class))).thenReturn(mockSSLContext);
        when(mockSSLContext.getSocketFactory()).thenReturn(mockSSLFactory);

        mongoDBConnectionManager.addConnection(mockMongoDBConnectionEntity);

        MongoClient mongoClient = mongoDBConnectionManager.getConnection(any(Goid.class)).getMongoClient();
        assertNotNull(mongoClient);
        assertEquals(mongoClient.getCredentialsList().size(), 1);
        assertEquals(mongoClient.getCredentialsList().get(0).getMechanism(), "MONGODB-X509");
        assertFalse(mongoClient.getMongoClientOptions().getSocketFactory().equals(SocketFactory.getDefault()));
        verify(mockDefaultKey).getSslKeyManagers();
    }

    /**
     * Basic credentials with none-default read preference
     *
     * Test will verify that:
     * ** mongoclient object is not null
     * ** SSLFactory object is the default SSLFactory
     * ** read preference is nearest
     *
     * @throws Exception
     */
    @Test
    public void testBasicWithNonDefaultReadPref() throws Exception {

        when(mockMongoDBConnectionEntity.getAuthType()).thenReturn(MongoDBEncryption.NO_ENCRYPTION.name());
        when(mockMongoDBConnectionEntity.getUsername()).thenReturn("");
        when(mockMongoDBConnectionEntity.getPassword()).thenReturn(null);
       when(mockMongoDBConnectionEntity.getReadPreference()).thenReturn(MongoDBReadPreference.Secondary.name());

        mongoDBConnectionManager.addConnection(mockMongoDBConnectionEntity);

        MongoClient mongoClient = mongoDBConnectionManager.getConnection(any(Goid.class)).getMongoClient();
        assertNotNull(mongoClient);
        assertTrue(mongoClient.getMongoClientOptions().getSocketFactory().equals(SocketFactory.getDefault()));
        assertTrue(mongoClient.getMongoClientOptions().getReadPreference().equals(ReadPreference.secondary()));
    }


    /**
     * Basic credentials with invalid read preference
     *
     * Test will verify that:
     * ** mongoclient object is not null
     * ** SSLFactory object is the default SSLFactory
     * ** read preference is set back to primary
     *
     * @throws Exception
     */
    @Test
    public void testBasicWithCredWithInvalidReadPref() throws Exception {

        when(mockMongoDBConnectionEntity.getAuthType()).thenReturn(MongoDBEncryption.NO_ENCRYPTION.name());
        when(mockMongoDBConnectionEntity.getUsername()).thenReturn("user");
        when(mockMongoDBConnectionEntity.getPassword()).thenReturn("password");
        when(mockMongoDBConnectionEntity.getReadPreference()).thenReturn("Invalid Value");

        mongoDBConnectionManager.addConnection(mockMongoDBConnectionEntity);

        MongoClient mongoClient = mongoDBConnectionManager.getConnection(any(Goid.class)).getMongoClient();
        assertNotNull(mongoClient);
        assertTrue(mongoClient.getMongoClientOptions().getSocketFactory().equals(SocketFactory.getDefault()));
        assertTrue(mongoClient.getMongoClientOptions().getReadPreference().equals(ReadPreference.primary()));

    }


    /**
     * SSL connection with null read preference
     *
     * Test will verify that:
     * ** mongoclient object is not null
     * ** SSLFactory object is NOT the default SSLFactory
     * ** read preference is set back to primary
     *
     * @throws Exception
     */
    @Test
    public void testSSLWithNullReadPref() throws Exception {

        SSLContext mockSSLContext = PowerMockito.mock(SSLContext.class);
        PowerMockito.mockStatic(SSLContext.class);

        JceProvider mockJceProvider = Mockito.mock(JceProvider.class);
        PowerMockito.mockStatic(JceProvider.class);

        SSLSocketFactory mockSSLFactory = PowerMockito.mock(SSLSocketFactory.class);
        SsgKeyEntry mockKeyEntry = Mockito.mock(SsgKeyEntry.class);

        when(mockMongoDBConnectionEntity.getAuthType()).thenReturn(MongoDBEncryption.SSL.name());
        when(mockMongoDBConnectionEntity.getUsername()).thenReturn("");
        when(mockMongoDBConnectionEntity.getPassword()).thenReturn(null);
        when(mockMongoDBConnectionEntity.getReadPreference()).thenReturn(null);

        when(mockMongoDBConnectionEntity.isUsesNoKey()).thenReturn(false);
        when(mockMongoDBConnectionEntity.isUsesDefaultKeyStore()).thenReturn(false);

        when(JceProvider.getInstance()).thenReturn(mockJceProvider);
        when(SSLContext.getInstance(anyString(), any(Provider.class))).thenReturn(mockSSLContext);
        when(mockSSLContext.getSocketFactory()).thenReturn(mockSSLFactory);
        when(mockKeyStoreManager.lookupKeyByKeyAlias(any(String.class), any(Goid.class))).thenReturn(mockKeyEntry);

        mongoDBConnectionManager.addConnection(mockMongoDBConnectionEntity);

        MongoClient mongoClient = mongoDBConnectionManager.getConnection(any(Goid.class)).getMongoClient();
        assertNotNull(mongoClient);
        assertTrue(mongoClient.getMongoClientOptions().getReadPreference().equals(ReadPreference.primary()));

    }

    /**
     * X509 connection with valid, none-default read preference
     *
     * Test will verify that:
     * ** mongoclient object is not null
     * ** SSLFactory object is NOT the default SSLFactory
     * ** read preference is set to primary preferred
     *
     * @throws Exception
     */
    @Test
    public void testX509WithValidReadPref() throws Exception {

        SSLContext mockSSLContext = PowerMockito.mock(SSLContext.class);
        PowerMockito.mockStatic(SSLContext.class);

        JceProvider mockJceProvider = Mockito.mock(JceProvider.class);
        PowerMockito.mockStatic(JceProvider.class);

        SSLSocketFactory mockSSLFactory = PowerMockito.mock(SSLSocketFactory.class);
        SsgKeyEntry mockKeyEntry = Mockito.mock(SsgKeyEntry.class);

        when(mockMongoDBConnectionEntity.getAuthType()).thenReturn(MongoDBEncryption.X509_Auth.name());
        when(mockMongoDBConnectionEntity.getUsername()).thenReturn("");
        when(mockMongoDBConnectionEntity.getPassword()).thenReturn(null);
        when(mockMongoDBConnectionEntity.getReadPreference()).thenReturn(MongoDBReadPreference.PrimaryPreferred.name());

        when(mockMongoDBConnectionEntity.isUsesNoKey()).thenReturn(false);
        when(mockMongoDBConnectionEntity.isUsesDefaultKeyStore()).thenReturn(false);

        when(JceProvider.getInstance()).thenReturn(mockJceProvider);
        when(SSLContext.getInstance(anyString(), any(Provider.class))).thenReturn(mockSSLContext);
        when(mockSSLContext.getSocketFactory()).thenReturn(mockSSLFactory);
        when(mockKeyStoreManager.lookupKeyByKeyAlias(any(String.class), any(Goid.class))).thenReturn(mockKeyEntry);

        mongoDBConnectionManager.addConnection(mockMongoDBConnectionEntity);

        MongoClient mongoClient = mongoDBConnectionManager.getConnection(any(Goid.class)).getMongoClient();
        assertNotNull(mongoClient);
        assertTrue(mongoClient.getMongoClientOptions().getReadPreference().equals(ReadPreference.primaryPreferred()));
    }
}

