package com.l7tech.external.assertions.portaldeployer.server;

import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.SSLSocketFactoryWrapper;
import com.l7tech.common.io.SingleCertX509KeyManager;
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
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.server.transport.http.SslClientTrustManager;
import com.l7tech.util.Charsets;
import com.l7tech.util.ExceptionUtils;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509KeyManager;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Manages the SSL dependencies for the Portal Deployer. The portalman private key created during enrollment is used
 * to authenticate the Portal Deployer to the message broker and restman. To setup the mutual auth with restman,
 * a user is created using the cn of the portalman key and its certificate is associated with it.
 */
public class PortalDeployerSslConfigurationManagerImpl implements PortalDeployerSslConfigurationManager {
  private static final Logger logger = Logger.getLogger(PortalDeployerSslConfigurationManagerImpl.class.getName());
  public static final Goid INTERNAL_IDENTITY_PROVIDER_GOID = new Goid("0000000000000000fffffffffffffffe");
  public static final String PORTALMAN_KEY_ALILAS = "portalman";
  public static final String TLSV1_2 = "TLSv1.2";

  private SsgKeyStoreManager ssgKeyStoreManager;
  private SslClientTrustManager trustManager;
  private X509KeyManager portalmanKeyManager;
  private IdentityProvider internalIdentityProvider;
  private PasswordHasher passwordHasher;
  private ClientCertManager clientCertManager;
  private SsgKeyEntry portalmanKey;
  private RoleManager roleManager;
  private UserManager userManager;
  private PlatformTransactionManager transactionManager;
  private SSLContext sslContext;
  private DefaultKey defaultKey;

  public PortalDeployerSslConfigurationManagerImpl(ApplicationContext context) {
    ssgKeyStoreManager = context.getBean("ssgKeyStoreManager", SsgKeyStoreManager.class);
    trustManager = context.getBean("trustManager", SslClientTrustManager.class);
    passwordHasher = context.getBean("passwordHasher", PasswordHasher.class);
    clientCertManager = context.getBean("clientCertManager", ClientCertManager.class);
    roleManager = context.getBean("roleManager", RoleManager.class);
    defaultKey = context.getBean("defaultKey", DefaultKey.class);
    try {
      internalIdentityProvider = context.getBean("identityProviderFactory", IdentityProviderFactory.class).getProvider(INTERNAL_IDENTITY_PROVIDER_GOID);
      userManager = internalIdentityProvider.getUserManager();
    } catch (FindException e) {
      //TODO: figure out how to handle this properly, unsure of what exception to propogate to module
      logger.log(Level.SEVERE, String.format("Unable to look up internalIdentityProvider key: %s", ExceptionUtils.getMessage(e)), e);
    }
    transactionManager = context.getBean("transactionManager", PlatformTransactionManager.class);
  }

  @Override
  public SSLSocketFactory getSniEnabledSocketFactory(final String sniHostname) throws PortalDeployerConfigurationException {
    return new SSLSocketFactoryWrapper(getSslContext().getSocketFactory()) {
      @Override
      protected Socket notifySocket(final Socket socket) {
        if (socket instanceof SSLSocket && sniHostname != null) {
          final SSLSocket sslSocket = (SSLSocket) socket;
          SSLParameters params = sslSocket.getSSLParameters();
          params.setServerNames(Collections.singletonList(new SNIHostName(sniHostname)));
          sslSocket.setSSLParameters(params);
        }
        return socket;
      }
    };
  }

  /**
   * Initializes the SSL dependencies for the Portal Deployer which are:
   * 1. The portalman key created during enrollment
   * 2. An administrator user that is associated with the portalman key's certificate used for mutual-auth with Restman
   * 3. An SSL Context using the portalman key and the Gateway's trust manager to allow for authenticated out-bound
   * communication with the Portal.
   *
   * @throws PortalDeployerConfigurationException if any over the above dependencies cannot be initialized
   */
  private void initSslDependencies() throws PortalDeployerConfigurationException {
    try {
      portalmanKey = ssgKeyStoreManager.lookupKeyByKeyAlias(PORTALMAN_KEY_ALILAS, PersistentEntity.DEFAULT_GOID);
      initPortalmanUser(portalmanKey.getCertificate());
      portalmanKeyManager = new SingleCertX509KeyManager(portalmanKey.getCertificateChain(), portalmanKey.getPrivate(), portalmanKey.getAlias());
      sslContext = SSLContext.getInstance(TLSV1_2);
      sslContext.init(new KeyManager[]{portalmanKeyManager}, new TrustManager[]{new SelfTrustManager(defaultKey), trustManager}, JceProvider.getInstance().getSecureRandom());
    } catch (FindException e) {
      logAndThrowException(String.format("Unable to find portalman key: %s", ExceptionUtils.getMessage(e)), e);
    } catch (KeyStoreException e) {
      logAndThrowException(String.format("Unable to look up portalman key: %s", ExceptionUtils.getMessage(e)), e);
    } catch (KeyManagementException | UnrecoverableKeyException | NoSuchAlgorithmException e) {
      logAndThrowException(String.format("Unable to initialize portalman SSL depdencies: %s", ExceptionUtils.getMessage(e)), e);
    } catch (IOException e) {
      logAndThrowException(String.format("Unable to trust default key : %s", ExceptionUtils.getMessage(e)), e);
    }
  }

  /**
   * Creates an administrator user to associate with the provided certificate in the Gateway's Internal Identity
   * Provider. The user will be created using the cn of the certificate and it will be associated with the created user
   * for mutual-auth calls via Restman.
   *
   * @param portalmanCertificate the certificate used to create the user.
   * @throws PortalDeployerConfigurationException if any errors are encountered creating the user.
   */
  private void initPortalmanUser(final X509Certificate portalmanCertificate) throws PortalDeployerConfigurationException {
    String portalmanUserLogin = CertUtils.getCn(portalmanCertificate);
    User portalmanUser = findPortalmanUser(portalmanUserLogin);
    if (portalmanUser == null) {
      logger.log(Level.FINE, String.format("Creating portalman user [%s].", portalmanUserLogin));
      portalmanUser = buildAndCreateNewUser(portalmanUserLogin);
    }
    associateCertWithUser(portalmanCertificate, portalmanUser);
    assignUserToAdminRole(portalmanUser);
  }

  /**
   * Assigns the specified user to the admin role.
   *
   * @param portalmanUser The user to assign to the admin role.
   * @throws PortalDeployerConfigurationException if any errors are encountered assigning the user to admin.
   */
  private void assignUserToAdminRole(final User portalmanUser) throws PortalDeployerConfigurationException {
    try {
      Role role = roleManager.findByTag(Role.Tag.ADMIN);
      role.addAssignedUser(portalmanUser);
      Boolean result = updateRoleWithAssignment(role);
      if (result == false) {
        logAndThrowException("Unable to save portalman user with admin role.", new UpdateException());
      }
    } catch (FindException e) {
      logAndThrowException(String.format("Unable to find admin role: %s", ExceptionUtils.getMessage(e)), e);
    }
  }

  /**
   * Saves the role and returns true or false if the operation succeeds or not.
   *
   * @param role Role to save
   * @return True or false if save operation completes.
   */
  private Boolean updateRoleWithAssignment(Role role) {
    return new TransactionTemplate(transactionManager).execute(new TransactionCallback<Boolean>() {
      @Override
      public Boolean doInTransaction(final TransactionStatus transactionStatus) {
        boolean roleUpdated = false;
        try {
          roleManager.update(role);
          roleUpdated = true;
        } catch (final ObjectModelException e) {
          transactionStatus.setRollbackOnly();
          logger.log(Level.WARNING, "Unable to save portalman user with admin role: " + ExceptionUtils.getMessage(e), e);
        }
        return roleUpdated;
      }
    });
  }

  private User findPortalmanUser(final String portalmanUserLogin) {
    try {
      return userManager.findByLogin(portalmanUserLogin);
    } catch (FindException e) {
      logger.log(Level.INFO, String.format("Unable to find portalman user [%s].", ExceptionUtils.getMessage(e)));
    }
    return null;
  }

  /**
   * Creates a new user using the login specified with a random hashed password.
   *
   * @return A user created with the login specified.
   * @throws PortalDeployerConfigurationException if any errors are encountered saving the user.
   */
  private User buildAndCreateNewUser(String portalmanUserLogin) throws PortalDeployerConfigurationException {
    InternalUser newPortalmanUser = new InternalUser();
    newPortalmanUser.setLogin(portalmanUserLogin);
    newPortalmanUser.setProviderId(INTERNAL_IDENTITY_PROVIDER_GOID);
    newPortalmanUser.setEnabled(true);
    newPortalmanUser.setExpiration(-1L);
    //hashedPassword is required but will be replaced with certificate and unused
    newPortalmanUser.setHashedPassword(passwordHasher.hashPassword(UUID.randomUUID().toString().getBytes(Charsets.UTF8)));
    createPortalmanUser(newPortalmanUser);
    return newPortalmanUser;
  }

  private void createPortalmanUser(InternalUser newPortalmanUser) throws PortalDeployerConfigurationException {
    try {
      String id = userManager.save(newPortalmanUser, null);
      newPortalmanUser.setGoid(Goid.parseGoid(id));
    } catch (SaveException e) {
      logAndThrowException(String.format("Unable to create portalman user: %s", ExceptionUtils.getMessage(e)), e);
    }
  }

  private void associateCertWithUser(X509Certificate portalmanCertificate, User portalmanUser) throws PortalDeployerConfigurationException {
    try {
      clientCertManager.recordNewUserCert(portalmanUser, portalmanCertificate, true);
    } catch (UpdateException ue) {
      logAndThrowException(String.format("Unable to record new cert for portalman user: %s", ExceptionUtils.getMessage(ue)), ue);
    }
  }

  private void logAndThrowException(String message, Throwable cause) throws PortalDeployerConfigurationException {
    logger.log(Level.WARNING, message, cause);
    throw new PortalDeployerConfigurationException(message, cause);
  }

  public SSLContext getSslContext() throws PortalDeployerConfigurationException {
    if (sslContext == null) {
      initSslDependencies();
    }
    return sslContext;
  }
}
