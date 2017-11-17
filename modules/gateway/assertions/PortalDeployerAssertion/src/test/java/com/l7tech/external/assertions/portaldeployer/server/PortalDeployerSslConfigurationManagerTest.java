package com.l7tech.external.assertions.portaldeployer.server;

import static com.l7tech.external.assertions.portaldeployer.server.PortalDeployerSslConfigurationManagerImpl.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.*;
import com.l7tech.common.password.PasswordHasher;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.User;
import com.l7tech.identity.UserManager;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.security.cert.TestCertificateGenerator;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.server.transport.http.SslClientTrustManager;
import com.l7tech.util.Pair;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509KeyManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

/**
 * Unit test for {@link PortalDeployerSslConfigurationManagerImpl}
 */
@RunWith(MockitoJUnitRunner.class)
public class PortalDeployerSslConfigurationManagerTest {

  public static final String PORTALMAN_LOGIN = "portalman.blah";
  @Mock
  private ApplicationContext context;
  @Mock
  private SsgKeyStoreManager ssgKeyStoreManager;
  @Mock
  private SslClientTrustManager trustManager;
  @Mock
  private X509KeyManager portalmanKeyManager;
  @Mock
  private IdentityProvider internalIdentityProvider;
  @Mock
  private PasswordHasher passwordHasher;
  @Mock
  private ClientCertManager clientCertManager;
  @Mock
  private RoleManager roleManager;
  @Mock
  private UserManager userManager;
  @Mock
  private PlatformTransactionManager transactionManager;
  @Mock
  private IdentityProviderFactory identityProviderFactory;
  @Mock
  private DefaultKey defaultKey;
  @Mock
  private SsgKeyEntry sslInfo;
  @Mock
  private X509Certificate x509Certificate;

  private PortalDeployerSslConfigurationManager portalDeployerSslConfigurationManager;
  private SsgKeyEntry portalmanKey;
  private Role adminRole;

  @Before
  public void setup() throws Exception {

    when(context.getBean("ssgKeyStoreManager", SsgKeyStoreManager.class)).thenReturn(ssgKeyStoreManager);
    when(context.getBean("trustManager", SslClientTrustManager.class)).thenReturn(trustManager);
    when(context.getBean("passwordHasher", PasswordHasher.class)).thenReturn(passwordHasher);
    when(context.getBean("clientCertManager", ClientCertManager.class)).thenReturn(clientCertManager);
    when(context.getBean("roleManager", RoleManager.class)).thenReturn(roleManager);
    when(context.getBean("identityProviderFactory", IdentityProviderFactory.class)).thenReturn(identityProviderFactory);
    when(identityProviderFactory.getProvider(any(Goid.class))).thenReturn(internalIdentityProvider);
    when(internalIdentityProvider.getUserManager()).thenReturn(userManager);
    when(context.getBean("transactionManager", PlatformTransactionManager.class)).thenReturn(transactionManager);
    when(context.getBean("defaultKey", DefaultKey.class)).thenReturn(defaultKey);
    Pair<X509Certificate, PrivateKey> keyCertPair = new TestCertificateGenerator().subject("CN=" + PORTALMAN_LOGIN).generateWithKey();
    X509Certificate cert = keyCertPair.getKey();
    PrivateKey key = keyCertPair.getValue();
    X509Certificate[] certificateChain = new X509Certificate[]{cert};
    //use actual SsgKeyEntry as actual mock always failed on getPrivateKey
    portalmanKey = new SsgKeyEntry(PersistentEntity.DEFAULT_GOID, "portalman", certificateChain, key);
    adminRole = new Role();

    portalDeployerSslConfigurationManager = new PortalDeployerSslConfigurationManagerImpl(context);
  }

  @Test
  public void test_getSniEnabledSocketFactory_createsPortalmanUserAsAdminWithCertificate_ifUserNotFound() throws Exception {
    when(ssgKeyStoreManager.lookupKeyByKeyAlias(eq(PortalDeployerSslConfigurationManagerImpl.PORTALMAN_KEY_ALILAS), eq(PersistentEntity.DEFAULT_GOID))).thenReturn(portalmanKey);
    when(roleManager.findByTag(eq(Role.Tag.ADMIN))).thenReturn(adminRole);
    when(passwordHasher.hashPassword(any(byte[].class))).thenReturn("hashpassword");
    when(userManager.findByLogin(eq(PORTALMAN_LOGIN))).thenThrow(FindException.class);
    when(defaultKey.getSslInfo()).thenReturn(sslInfo);
    when(sslInfo.getCertificate()).thenReturn(x509Certificate);
    ArgumentCaptor<User> userArgumentCaptor = ArgumentCaptor.forClass(User.class);
    when(userManager.save(userArgumentCaptor.capture(), eq(null))).thenReturn(Goid.DEFAULT_GOID.toHexString());

    verifySniSocketFactory(portalDeployerSslConfigurationManager.getSniEnabledSocketFactory("test"), "test");

    //verify created user
    InternalUser createdUser = (InternalUser) userArgumentCaptor.getValue();
    assertEquals(createdUser.getLogin(), PORTALMAN_LOGIN);
    assertEquals(createdUser.getHashedPassword(), "hashpassword");
    assertEquals(createdUser.getExpiration(), -1L);
    assertEquals(createdUser.getProviderId(), INTERNAL_IDENTITY_PROVIDER_GOID);

    //verify certificate & role assignment
    verify(clientCertManager, times(1)).recordNewUserCert(eq(createdUser), eq(portalmanKey.getCertificate()), eq(true));
    verify(roleManager, times(1)).update(eq(adminRole));
    assertEquals(adminRole.getRoleAssignments().stream().findFirst().get().getId(), Goid.DEFAULT_GOID.toHexString());
  }

  @Test
  public void test_getSniEnabledSocketFactory_updatesPortalmanUserAsAdminWithCertificate_ifUserFoundWithNoCertificate() throws Exception {
    Role adminRole = new Role();
    when(ssgKeyStoreManager.lookupKeyByKeyAlias(eq(PortalDeployerSslConfigurationManagerImpl.PORTALMAN_KEY_ALILAS), eq(PersistentEntity.DEFAULT_GOID))).thenReturn(portalmanKey);
    when(clientCertManager.getUserCert(any(User.class))).thenReturn(null);
    when(roleManager.findByTag(eq(Role.Tag.ADMIN))).thenReturn(adminRole);
    when(userManager.findByLogin(eq(PORTALMAN_LOGIN))).thenReturn(mock(User.class));
    when(defaultKey.getSslInfo()).thenReturn(sslInfo);
    when(sslInfo.getCertificate()).thenReturn(x509Certificate);

    verifySniSocketFactory(portalDeployerSslConfigurationManager.getSniEnabledSocketFactory("test"), "test");

    //verify user not created if found
    verify(userManager, never()).save(any(User.class), eq(null));
    //verify certificate & role assignment
    verify(clientCertManager, times(1)).recordNewUserCert(any(User.class), eq(portalmanKey.getCertificate()), eq(true));
    verify(roleManager, times(1)).update(eq(adminRole));
    assertEquals(adminRole.getRoleAssignments().stream().findFirst().get().getId(), Goid.DEFAULT_GOID.toHexString());
  }

  @Test(expected = PortalDeployerConfigurationException.class)
  public void test_getSniEnabledSocketFactory_throwsException_ifPortalmanKeyNotFound() throws Exception {
    when(ssgKeyStoreManager.lookupKeyByKeyAlias(eq(PortalDeployerSslConfigurationManagerImpl.PORTALMAN_KEY_ALILAS), eq(PersistentEntity.DEFAULT_GOID))).thenThrow(FindException.class);

    portalDeployerSslConfigurationManager.getSniEnabledSocketFactory("test");
  }

  @Test(expected = PortalDeployerConfigurationException.class)
  public void test_getSniEnabledSocketFactory_throwsException_ifPortalmanKeyNotRetrievable() throws Exception {
    when(ssgKeyStoreManager.lookupKeyByKeyAlias(eq(PortalDeployerSslConfigurationManagerImpl.PORTALMAN_KEY_ALILAS), eq(PersistentEntity.DEFAULT_GOID))).thenThrow(KeyStoreException.class);

    portalDeployerSslConfigurationManager.getSniEnabledSocketFactory("test");
  }

  @Test(expected = PortalDeployerConfigurationException.class)
  public void test_getSniEnabledSocketFactory_throwsException_ifUserCreationFails() throws Exception {
    when(ssgKeyStoreManager.lookupKeyByKeyAlias(eq(PortalDeployerSslConfigurationManagerImpl.PORTALMAN_KEY_ALILAS), eq(PersistentEntity.DEFAULT_GOID))).thenReturn(portalmanKey);
    when(userManager.findByLogin(eq(PORTALMAN_LOGIN))).thenThrow(FindException.class);
    when(userManager.save(any(User.class), eq(null))).thenThrow(SaveException.class);

    portalDeployerSslConfigurationManager.getSniEnabledSocketFactory("test");
  }

  @Test(expected = PortalDeployerConfigurationException.class)
  public void test_getSniEnabledSocketFactory_throwsException_ifUserRoleUpdateFails() throws Exception {
    when(ssgKeyStoreManager.lookupKeyByKeyAlias(eq(PortalDeployerSslConfigurationManagerImpl.PORTALMAN_KEY_ALILAS), eq(PersistentEntity.DEFAULT_GOID))).thenReturn(portalmanKey);
    when(roleManager.findByTag(eq(Role.Tag.ADMIN))).thenReturn(adminRole);
    when(userManager.findByLogin(eq(PORTALMAN_LOGIN))).thenReturn(mock(User.class));
    when(transactionManager.getTransaction(any(TransactionDefinition.class))).thenReturn(mock(TransactionStatus.class));
    doThrow(UpdateException.class).when(roleManager).update(eq(adminRole));

    portalDeployerSslConfigurationManager.getSniEnabledSocketFactory("test");
  }

  @Test(expected = PortalDeployerConfigurationException.class)
  public void test_getSniEnabledSocketFactory_throwsException_ifUserCertificateUpdateFails() throws Exception {
    when(ssgKeyStoreManager.lookupKeyByKeyAlias(eq(PortalDeployerSslConfigurationManagerImpl.PORTALMAN_KEY_ALILAS), eq(PersistentEntity.DEFAULT_GOID))).thenReturn(portalmanKey);
    when(userManager.findByLogin(eq(PORTALMAN_LOGIN))).thenReturn(mock(User.class));
    doThrow(UpdateException.class).when(clientCertManager).recordNewUserCert(any(User.class), any(Certificate.class), anyBoolean());

    portalDeployerSslConfigurationManager.getSniEnabledSocketFactory("test");
  }

  private void verifySniSocketFactory(SSLSocketFactory sslSocketFactory, String sniHostname) throws Exception {
    SSLSocket socket = (SSLSocket) sslSocketFactory.createSocket();
    SNIHostName sniServerName = (SNIHostName) socket.getSSLParameters().getServerNames().stream().findFirst().get();
    assertEquals(sniHostname, sniServerName.getAsciiName());
  }
}
