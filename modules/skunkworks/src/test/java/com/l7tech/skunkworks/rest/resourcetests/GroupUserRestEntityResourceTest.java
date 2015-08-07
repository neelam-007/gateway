package com.l7tech.skunkworks.rest.resourcetests;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.*;
import com.l7tech.common.password.PasswordHasher;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.identity.*;
import com.l7tech.identity.fed.FederatedIdentityProviderConfig;
import com.l7tech.identity.internal.InternalGroup;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.security.cert.ParamsKeyGenerator;
import com.l7tech.security.cert.TestCertificateGenerator;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.cert.TrustedCertManager;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.security.token.UsernamePasswordSecurityToken;
import com.l7tech.security.token.http.TlsClientCertToken;
import com.l7tech.server.identity.AuthenticatingIdentityProvider;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.skunkworks.rest.tools.RestEntityTestBase;
import com.l7tech.skunkworks.rest.tools.RestResponse;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.IgnoreOnDaily;
import com.l7tech.util.Charsets;
import org.apache.http.entity.ContentType;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.jce.X509KeyUsage;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.security.auth.x500.X500Principal;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Logger;

import static org.junit.Assert.*;

@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class GroupUserRestEntityResourceTest extends RestEntityTestBase{
    private static final Logger logger = Logger.getLogger(GroupUserRestEntityResourceTest.class.getName());
    private static final SecureRandom defaultRandom = new SecureRandom();
    private final String internalProviderId = IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID.toString();
    private GroupManager internalGroupManager;
    private UserManager internalUserManager;
    private IdentityProvider internalIdentityProvider;
    private IdentityProviderFactory identityProviderFactory;
    private IdentityProviderConfigManager idConfigManager;
    private String otherIdentityProviderId;
    private List<String> usersToCleanup = new ArrayList<>();
    private List<String> fipUsersToCleanup = new ArrayList<>();
    private List<String> groupsToCleanup = new ArrayList<>();
    private String shortHashedPassword = "$6$blahsalt$iAUOw3SVBmtcHXXnfvb8/NohNmNC3gzf1uuHG5Iz33/2g6kyLnmoip0nLEhpwbktZb/XG8jWHdS9zsLhhvoeM/";
    private String shortPassword = "12";
    private String strongPassword = "12!@qwQW";
    private String strongPassword2 = "34#$erER";
    private TrustedCertManager trustedCertManager;
    private CertGenParams c;
    private KeyGenParams k;
    private SecureRandom random;
    private String signatureProviderName;
    private List<TrustedCert> trustedCerts = new ArrayList<>();
    private String federatedIPId;


    @Before
    public void before() throws Exception {
        init();

        identityProviderFactory = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("identityProviderFactory", IdentityProviderFactory.class);
        idConfigManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("identityProviderConfigManager", IdentityProviderConfigManager.class);
        internalIdentityProvider = identityProviderFactory.getProvider(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID);
        internalGroupManager = internalIdentityProvider.getGroupManager();
        internalUserManager = internalIdentityProvider.getUserManager();
        PasswordHasher passwordHasher = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("passwordHasher", PasswordHasher.class);
        trustedCertManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("trustedCertManager", TrustedCertManager.class);


        // add users
        InternalUser user1 = new InternalUser("user1");
        user1.setHashedPassword(passwordHasher.hashPassword("password".getBytes(Charsets.UTF8)));
        InternalUser user2 = new InternalUser("user2");
        user2.setHashedPassword(passwordHasher.hashPassword("password2".getBytes(Charsets.UTF8)));
        internalUserManager.save(user1,null);
        internalUserManager.save(user2,null);
        usersToCleanup.add(user1.getId());
        usersToCleanup.add(user2.getId());

        // add internal group
        InternalGroup group1 =  new InternalGroup("group1");
        InternalGroup group2 =  new InternalGroup("group2");
        internalGroupManager.saveGroup(group1);
        internalGroupManager.saveGroup(group2);
        internalGroupManager.addUser(user1, group1);
        internalGroupManager.addUser(user2, group2);
        groupsToCleanup.add(group1.getId());
        groupsToCleanup.add(group2.getId());

        LdapIdentityProviderConfig otherIdProviderConfig = new LdapIdentityProviderConfig();
        otherIdProviderConfig.setName("Other ID provider");
        otherIdProviderConfig.setLdapUrl(new String[]{"ldap://test:789"});
        otherIdProviderConfig.setTemplateName("MicrosoftActiveDirectory");
        otherIdProviderConfig.setSearchBase("searchBase");
        otherIdProviderConfig.setBindDN("bindDN");
        otherIdProviderConfig.setBindPasswd("password");
        otherIdentityProviderId = idConfigManager.save(otherIdProviderConfig).toString();

        TrustedCert trustedCert = new TrustedCert();
        trustedCert.setName("Cert 1");
        trustedCert.setRevocationCheckPolicyType(TrustedCert.PolicyUsageType.NONE);
        trustedCert.setTrustedFor(TrustedCert.TrustedFor.SIGNING_CLIENT_CERTS, false);
        trustedCert.setTrustedFor(TrustedCert.TrustedFor.SIGNING_SERVER_CERTS, false);
        trustedCert.setTrustedFor(TrustedCert.TrustedFor.SAML_ATTESTING_ENTITY, false);
        trustedCert.setTrustedFor(TrustedCert.TrustedFor.SAML_ISSUER, false);
        trustedCert.setTrustedFor(TrustedCert.TrustedFor.SSL, false);
        trustedCert.setTrustAnchor(true);
        trustedCert.setVerifyHostname(true);
        trustedCert.setCertificate(generateCert());

        trustedCertManager.save(trustedCert);
        trustedCerts.add(trustedCert);

        FederatedIdentityProviderConfig fip = new FederatedIdentityProviderConfig();
        fip.setName("FIP");
        fip.setTrustedCertGoids(new Goid[]{trustedCert.getGoid()});
        federatedIPId = idConfigManager.save(fip).toString();
    }

    public void init() {
        random = defaultRandom;
        signatureProviderName = null;

        k = new KeyGenParams();
        k.setAlgorithm("RSA");
        k.setNamedParam("sect163k1");
        k.setKeySize(1024);

        c = new CertGenParams();
        c.setSerialNumber(new BigInteger(64, random).abs());
        c.setNotBefore(new Date(new Date().getTime() - (10 * 60 * 1000L))); // default: 10 min ago
        c.setDaysUntilExpiry(20 * 365);
        c.setNotAfter(null);
        c.setSignatureAlgorithm(null);
        c.setSubjectDn(new X500Principal("cn=test"));
        c.setIncludeBasicConstraints(false);
        c.setKeyUsageBits(X509KeyUsage.digitalSignature | X509KeyUsage.keyEncipherment | X509KeyUsage.nonRepudiation);
        c.setIncludeKeyUsage(true);
        c.setKeyUsageCritical(true);
        c.setIncludeSki(true);
        c.setIncludeAki(true);
        c.setIncludeExtendedKeyUsage(true);
        c.setExtendedKeyUsageCritical(true);
        c.setExtendedKeyUsageKeyPurposeOids(Arrays.asList(KeyPurposeId.anyExtendedKeyUsage.getId()));
        c.setIncludeSubjectDirectoryAttributes(false);
        c.setSubjectDirectoryAttributesCritical(false);
        c.setCountryOfCitizenshipCountryCodes(Collections.<String>emptyList());
        c.setCertificatePolicies(Collections.<String>emptyList());
        c.setSubjectAlternativeNames(Collections.<X509GeneralName>emptyList());
    }

    @After
    public void after() throws FindException, DeleteException {
        for (String group : groupsToCleanup) {
            internalGroupManager.delete(group);
        }

        for (String user : usersToCleanup) {
            internalUserManager.delete(internalUserManager.findByPrimaryKey(user));
        }

        IdentityProvider fipIdProvider = identityProviderFactory.getProvider(Goid.parseGoid(federatedIPId));
        for (String user : fipUsersToCleanup) {
            fipIdProvider.getUserManager().delete(fipIdProvider.getUserManager().findByPrimaryKey(user));
        }

        idConfigManager.delete(Goid.parseGoid(otherIdentityProviderId));
        idConfigManager.delete(Goid.parseGoid(federatedIPId));

        Collection<TrustedCert> all = trustedCertManager.findAll();
        for (TrustedCert trustedCert : all) {
            trustedCertManager.delete(trustedCert.getGoid());
        }

    }

    private X509Certificate generateCert() throws CertificateGeneratorException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, CertificateException {
        final KeyPair subjectKeyPair = new ParamsKeyGenerator(k, random).generateKeyPair();
        final PublicKey subjectPublicKey = subjectKeyPair.getPublic();
        final PrivateKey subjectPrivateKey = subjectKeyPair.getPrivate();

        ParamsCertificateGenerator certgen = new ParamsCertificateGenerator(c, random, signatureProviderName);
        return (X509Certificate) CertUtils.getFactory().generateCertificate(new ByteArrayInputStream(certgen.generateCertificate(subjectPublicKey, subjectPrivateKey, null).getEncoded()));
    }

    protected String writeMOToString(Object  mo) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        final StreamResult result = new StreamResult( bout );
        MarshallingUtils.marshal( mo, result, false );
        return bout.toString();
    }

    @Test
    public void UserCreateTest() throws Exception {

        UserMO userMO = ManagedObjectFactory.createUserMO();
        userMO.setProviderId(internalProviderId);
        userMO.setName("login");
        userMO.setLogin("login");
        PasswordFormatted password = ManagedObjectFactory.createPasswordFormatted();
        password.setFormat("plain");
        password.setPassword(strongPassword);
        userMO.setPassword(password);
        userMO.setFirstName("first name");
        userMO.setLastName("last name");

        String userMOString = writeMOToString(userMO);
        RestResponse response = processRequest("identityProviders/" + internalProviderId + "/users", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), userMOString);
        assertEquals(201, response.getStatus());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Item<UserMO> item = MarshallingUtils.unmarshal(Item.class, source);
        assertEquals("User Name:", userMO.getLogin(), item.getName());
        assertEquals(EntityType.USER.toString(), item.getType());
        assertNull(item.getContent());

        String userId = item.getId();
        usersToCleanup.add(userId);

        User user = internalUserManager.findByPrimaryKey(userId);

        assertNotNull(user);
        assertEquals("User Name:", userMO.getName(), user.getName());
        assertEquals("User Login:", userMO.getLogin(), user.getLogin());
        assertEquals("User First name:", userMO.getFirstName(), user.getFirstName());
        assertEquals("User last name:", userMO.getLastName(), user.getLastName());

        // check password, try login with new user
        LoginCredentials creds = LoginCredentials.makeLoginCredentials(
                new UsernamePasswordSecurityToken(SecurityTokenType.UNKNOWN, userMO.getLogin(), password.getPassword().toCharArray()), null);
        final AuthenticationResult providerAuthResult = ((AuthenticatingIdentityProvider) internalIdentityProvider).authenticate(creds, true);
        assertNotNull(providerAuthResult);
        assertNotNull(providerAuthResult.getUser());
        assertEquals(userMO.getLogin(), providerAuthResult.getUser().getLogin());
    }

    @Test
    public void FipUserCreateTest() throws Exception {

        UserMO userMO = ManagedObjectFactory.createUserMO();
        userMO.setProviderId(federatedIPId);
        userMO.setName("login");
        userMO.setLogin("login");
        userMO.setFirstName("first name");
        userMO.setLastName("last name");
        userMO.setSubjectDn("cn=login");

        String userMOString = writeMOToString(userMO);
        RestResponse response = processRequest("identityProviders/" + federatedIPId + "/users", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), userMOString);
        assertEquals(201, response.getStatus());

        StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Item<UserMO> item = MarshallingUtils.unmarshal(Item.class, source);
        assertEquals("User Name:", userMO.getLogin(), item.getName());
        assertEquals(EntityType.USER.toString(), item.getType());
        assertNull(item.getContent());

        String userId = item.getId();
        fipUsersToCleanup.add(userId);

        User user = identityProviderFactory.getProvider(Goid.parseGoid(federatedIPId)).getUserManager().findByPrimaryKey(userId);

        assertNotNull(user);
        assertEquals("User Name:", userMO.getLogin(), user.getName());
        assertEquals("User Login:", userMO.getLogin(), user.getLogin());
        assertEquals("User First name:", userMO.getFirstName(), user.getFirstName());
        assertEquals("User last name:", userMO.getLastName(), user.getLastName());
        assertEquals("User subject DN:", userMO.getSubjectDn(), user.getSubjectDn());

        // set certificate
        X509Certificate certificate = new TestCertificateGenerator().subject("cn=login").generate();
        CertificateData certData = ManagedObjectFactory.createCertificateData(certificate);
        response = processRequest("identityProviders/" + federatedIPId + "/users/" + userId + "/certificate", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), writeMOToString(certData));
        assertEquals(200, response.getStatus());

        // set bad
        certificate = new TestCertificateGenerator().subject("cn=other").generate();
        certData = ManagedObjectFactory.createCertificateData(certificate);
        response = processRequest("identityProviders/" + federatedIPId + "/users/" + userId + "/certificate", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), writeMOToString(certData));
        assertEquals(400, response.getStatus());

        source = new StreamSource(new StringReader(response.getBody()));
        ErrorResponse error = MarshallingUtils.unmarshal(ErrorResponse.class, source);
        assertEquals("InvalidResource",error.getType());
        assertTrue(error.getDetail().contains("Certificate subject name (other)does not match user login"));
    }

    @Test
    public void UserCreateHashedPasswordTest() throws Exception {

        UserMO userMO = ManagedObjectFactory.createUserMO();
        userMO.setProviderId(internalProviderId);
        userMO.setName("login");
        userMO.setLogin("login");
        PasswordFormatted password = ManagedObjectFactory.createPasswordFormatted();
        password.setFormat("sha512crypt");
        password.setPassword(shortHashedPassword);
        userMO.setPassword(password);
        userMO.setFirstName("first name");
        userMO.setLastName("last name");

        String userMOString = writeMOToString(userMO);
        logger.info(userMOString);
        RestResponse response = processRequest("identityProviders/" + internalProviderId + "/users", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), userMOString);
        assertEquals(201, response.getStatus());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Item<UserMO> item = MarshallingUtils.unmarshal(Item.class, source);
        assertEquals("User Name:", userMO.getLogin(), item.getName());
        assertEquals(EntityType.USER.toString(), item.getType());

        String userId = item.getId();
        usersToCleanup.add(userId);

        User user = internalUserManager.findByPrimaryKey(userId);

        assertNotNull(user);
        assertEquals("User Name:", userMO.getLogin(), user.getName());
        assertEquals("User Login:", userMO.getLogin(), user.getLogin());
        assertEquals("User First name:", userMO.getFirstName(), user.getFirstName());
        assertEquals("User last name:", userMO.getLastName(), user.getLastName());

        // check password, try login with new user
        LoginCredentials creds = LoginCredentials.makeLoginCredentials(
                new UsernamePasswordSecurityToken(SecurityTokenType.UNKNOWN, userMO.getLogin(), shortPassword.toCharArray()), null);
        final AuthenticationResult providerAuthResult = ((AuthenticatingIdentityProvider) internalIdentityProvider).authenticate(creds, true);
        assertNotNull(providerAuthResult);
        assertNotNull(providerAuthResult.getUser());
        assertEquals(userMO.getLogin(),providerAuthResult.getUser().getLogin());
    }

    @Test
    public void UserCreatePasswordFailTest() throws Exception {

        UserMO userMO = ManagedObjectFactory.createUserMO();
        userMO.setProviderId(internalProviderId);
        userMO.setLogin("login");
        PasswordFormatted password = ManagedObjectFactory.createPasswordFormatted();
        password.setFormat("plain");
        password.setPassword(shortPassword);
        userMO.setPassword(password);
        userMO.setFirstName("first name");
        userMO.setLastName("last name");

        String userMOString = writeMOToString(userMO);
        RestResponse response = processRequest("identityProviders/" + internalProviderId + "/users", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), userMOString);
        assertEquals(400, response.getStatus());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        ErrorResponse error = MarshallingUtils.unmarshal(ErrorResponse.class, source);
        assertEquals("InvalidResource",error.getType());
        assertTrue(error.getDetail().contains("Resource validation failed due to 'INVALID_VALUES' Unable to create user, invalid password:"));
    }

    @Test
    public void UserUpdateTest() throws Exception {

        UserMO userMO = ManagedObjectFactory.createUserMO();
        userMO.setId(usersToCleanup.get(0));
        userMO.setProviderId(internalProviderId);
        userMO.setName("login");
        userMO.setLogin("login");
        userMO.setFirstName("first name");
        userMO.setLastName("last name");

        String userMOString = writeMOToString(userMO);
        RestResponse response = processRequest("identityProviders/" + internalProviderId + "/users/" + userMO.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), userMOString);
        assertEquals(200, response.getStatus());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Item<UserMO> item = MarshallingUtils.unmarshal(Item.class, source);
        assertEquals("User Name:", userMO.getLogin(), item.getName());
        assertEquals("User id:", userMO.getId(), item.getId());
        assertEquals(EntityType.USER.toString(), item.getType());
        assertNull(item.getContent());

        User user = internalUserManager.findByPrimaryKey(userMO.getId());

        assertNotNull(user);
        assertEquals("User Name:", userMO.getLogin(), user.getName());
        assertEquals("User Login:", userMO.getLogin(), user.getLogin());
        assertEquals("User First name:", userMO.getFirstName(), user.getFirstName());
        assertEquals("User last name:", userMO.getLastName(), user.getLastName());

        //update twice test
        response = processRequest("identityProviders/" + internalProviderId + "/users/" + userMO.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), userMOString);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void UserChangePasswordInvalidInternalUserTest() throws Exception {

        RestResponse response = processRequest("identityProviders/" + internalProviderId + "/users/" + otherIdentityProviderId + "/password", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), strongPassword2);
        assertEquals(404, response.getStatus());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        ErrorResponse error = MarshallingUtils.unmarshal(ErrorResponse.class, source);
        assertEquals("ResourceNotFound",error.getType());
        assertTrue(error.getDetail().contains("not found"));
    }

    @Test
    public void UserChangePasswordFailTest() throws Exception {

        String userId = usersToCleanup.get(0);
        String simplePassword = shortPassword;

        RestResponse response = processRequest("identityProviders/" + internalProviderId + "/users/" + userId + "/password", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), simplePassword);
        assertEquals(400, response.getStatus());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        ErrorResponse error = MarshallingUtils.unmarshal(ErrorResponse.class, source);
        assertEquals("InvalidResource",error.getType());
        assertTrue(error.getDetail().contains("Unable to change user password, invalid password:"));
    }

    @Test
    public void UserChangePasswordTest() throws Exception {

        String userId = usersToCleanup.get(0);
        String password = strongPassword2;

        RestResponse response = processRequest("identityProviders/" + internalProviderId + "/users/" + userId + "/password", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), password);
        assertEquals(200, response.getStatus());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Item<UserMO> item = MarshallingUtils.unmarshal(Item.class, source);
        assertEquals("User Name:", "user1", item.getName());
        assertEquals("User id:", userId, item.getId());
        assertEquals(EntityType.USER.toString(), item.getType());

        // check password, try login with new password
        LoginCredentials creds = LoginCredentials.makeLoginCredentials(
                new UsernamePasswordSecurityToken(SecurityTokenType.UNKNOWN, item.getContent().getLogin(), strongPassword2.toCharArray()), null);
        final AuthenticationResult providerAuthResult = ((AuthenticatingIdentityProvider) internalIdentityProvider).authenticate(creds, true);
        assertNotNull(providerAuthResult);
        assertNotNull(providerAuthResult.getUser());
        assertEquals(item.getContent().getLogin(),providerAuthResult.getUser().getLogin());
    }

    @Test
    public void UserChangeHashedPasswordTest() throws Exception {

        String userId = usersToCleanup.get(0);
        String password = shortHashedPassword;

        RestResponse response = processRequest("identityProviders/" + internalProviderId + "/users/" + userId + "/password?format=sha512crypt", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), password);
        assertEquals(200, response.getStatus());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Item<UserMO> item = MarshallingUtils.unmarshal(Item.class, source);
        assertEquals("User Name:", "user1", item.getName());
        assertEquals("User id:", userId, item.getId());
        assertEquals(EntityType.USER.toString(), item.getType());

        // check password, try login with new password
        LoginCredentials creds = LoginCredentials.makeLoginCredentials(
                new UsernamePasswordSecurityToken(SecurityTokenType.UNKNOWN, item.getContent().getLogin(), shortPassword.toCharArray()), null);
        final AuthenticationResult providerAuthResult = ((AuthenticatingIdentityProvider) internalIdentityProvider).authenticate(creds, true);
        assertNotNull(providerAuthResult);
        assertNotNull(providerAuthResult.getUser());
        assertEquals(item.getContent().getLogin(),providerAuthResult.getUser().getLogin());
    }

    @Test
    public void UserSetCertificateTest() throws Exception {
        String userId = usersToCleanup.get(0);

        // create certificate
        X509Certificate certificate = new TestCertificateGenerator().subject("cn=user1").generate();
        CertificateData certData = ManagedObjectFactory.createCertificateData(certificate);
        RestResponse response = processRequest("identityProviders/" + internalProviderId + "/users/" + userId + "/certificate", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), writeMOToString(certData));
        assertEquals(200, response.getStatus());

        // try login with new certificate
        LoginCredentials creds = LoginCredentials.makeLoginCredentials(
                new TlsClientCertToken(certificate), null);
        final AuthenticationResult providerAuthResult = ((AuthenticatingIdentityProvider) internalIdentityProvider).authenticate(creds, true);
        assertNotNull(providerAuthResult);
        assertNotNull(providerAuthResult.getUser());
        assertEquals(userId,providerAuthResult.getUser().getId());
    }

    @Test
    public void UserSetCertificateWrongLoginTest() throws Exception {
        String userId = usersToCleanup.get(0);

        // create certificate
        X509Certificate certificate = new TestCertificateGenerator().subject("cn=test").generate();
        CertificateData certData = ManagedObjectFactory.createCertificateData(certificate);
        RestResponse response = processRequest("identityProviders/" + internalProviderId + "/users/" + userId + "/certificate", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), writeMOToString(certData));
        assertEquals(400, response.getStatus());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        ErrorResponse error = MarshallingUtils.unmarshal(ErrorResponse.class, source);
        assertEquals("InvalidResource",error.getType());
        assertEquals("Resource validation failed due to 'INVALID_VALUES' Certificate subject name (test)does not match user login", error.getDetail());
    }


    @Test
    public void UserRevokeCertificateTest() throws Exception {
        String userId = usersToCleanup.get(0);

        // create certificate
        X509Certificate certificate = new TestCertificateGenerator().subject("cn=user1").generate();
        CertificateData certData = ManagedObjectFactory.createCertificateData(certificate);
        RestResponse response = processRequest("identityProviders/" + internalProviderId + "/users/" + userId + "/certificate", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), writeMOToString(certData));
        assertEquals(200, response.getStatus());

        // try login with new certificate
        LoginCredentials creds = LoginCredentials.makeLoginCredentials(
                new TlsClientCertToken(certificate), null);
        AuthenticationResult providerAuthResult = ((AuthenticatingIdentityProvider) internalIdentityProvider).authenticate(creds, true);
        assertNotNull(providerAuthResult);
        assertNotNull(providerAuthResult.getUser());
        assertEquals(userId,providerAuthResult.getUser().getId());

        // revoke certificate
        response = processRequest("identityProviders/" + internalProviderId + "/users/" + userId + "/certificate", HttpMethod.DELETE, ContentType.APPLICATION_XML.toString(), "");
        assertEquals(204, response.getStatus());

        // try login with certificate, should fail
        try {
            providerAuthResult = ((AuthenticatingIdentityProvider) internalIdentityProvider).authenticate(creds, true);
        }catch(InvalidClientCertificateException e){
            assertEquals("No certificate found for user user1",e.getMessage());
            return;
        }
        assertNull(providerAuthResult);
    }

    @Test
    public void UserDeleteTest() throws Exception {

        String userId = usersToCleanup.get(0);
        RestResponse response = processRequest("identityProviders/" + internalProviderId + "/users/" + userId, HttpMethod.DELETE, null, "");
        assertEquals(204, response.getStatus());

        usersToCleanup.remove(userId);

        // check entity
        Assert.assertNull(internalUserManager.findByPrimaryKey(userId));
    }

    @Test
    public void UserSearchTest() throws Exception {

        RestResponse response = processRequest("identityProviders/"+internalProviderId+"/users?login=user1", HttpMethod.GET, ContentType.APPLICATION_XML.toString(),"");
        assertEquals(200, response.getStatus());
        ItemsList<UserMO> userList = MarshallingUtils.unmarshal(ItemsList.class, new StreamSource(new StringReader(response.getBody())));

        assertNotNull(userList.getContent());
        assertEquals(1, userList.getContent().size());
        assertEquals(usersToCleanup.get(0),userList.getContent().get(0).getId());

    }

    @Test
    public void UserListTest() throws Exception {

        RestResponse response = processRequest("identityProviders/"+internalProviderId+"/users", HttpMethod.GET, ContentType.APPLICATION_XML.toString(),"");
        assertEquals(200, response.getStatus());
        ItemsList<UserMO> userList = MarshallingUtils.unmarshal(ItemsList.class, new StreamSource(new StringReader(response.getBody())));

        assertNotNull(userList.getContent());
        assertEquals(3, userList.getContent().size());

    }

    @Test
    public void UserErrorList() throws Exception {
        RestResponse response = processRequest("identityProviders/" + new Goid(234, 234).toString() + "/users/", HttpMethod.GET, ContentType.APPLICATION_XML.toString(), "");
        assertEquals(404, response.getStatus());

        StreamSource source = new StreamSource(new StringReader(response.getBody()));
        ErrorResponse error = MarshallingUtils.unmarshal(ErrorResponse.class, source);
        assertEquals("ResourceNotFound", error.getType());
        assertEquals("IdentityProvider not found", error.getDetail());
    }

    @Test
    public void UserErrorGet() throws Exception {
        // invalid id provider
        RestResponse response = processRequest("identityProviders/"+ groupsToCleanup.get(1) +"/users/"+usersToCleanup.get(1), HttpMethod.GET, ContentType.APPLICATION_XML.toString(),"");
        assertEquals(404, response.getStatus());

        StreamSource source = new StreamSource(new StringReader(response.getBody()));
        ErrorResponse error = MarshallingUtils.unmarshal(ErrorResponse.class, source);
        assertEquals("ResourceNotFound",error.getType());
        assertEquals("IdentityProvider not found",error.getDetail());

        // user not found
        response = processRequest("identityProviders/"+internalProviderId+"/users/"+ groupsToCleanup.get(1), HttpMethod.GET, ContentType.APPLICATION_XML.toString(),"");
        assertEquals(404, response.getStatus());

        source = new StreamSource(new StringReader(response.getBody()));
        error = MarshallingUtils.unmarshal(ErrorResponse.class, source);
        assertEquals("ResourceNotFound",error.getType());
        assertTrue(error.getDetail().contains("not found"));
    }

    @Test
    public void UserErrorCreate() throws Exception {
        UserMO userMO = ManagedObjectFactory.createUserMO();
        userMO.setProviderId(internalProviderId);
        userMO.setLogin("login");
        userMO.setFirstName("first name");
        userMO.setLastName("last name");
        PasswordFormatted password = ManagedObjectFactory.createPasswordFormatted();
        password.setFormat("plain");
        password.setPassword(strongPassword);
        userMO.setPassword(password);
        String userMOString = writeMOToString(userMO);

        // non internal id provider
        userMO.setProviderId(otherIdentityProviderId);
        userMOString = writeMOToString(userMO);
        RestResponse response = processRequest("identityProviders/"+otherIdentityProviderId+"/users/", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),userMOString);
        assertEquals(400, response.getStatus());
        StreamSource source = new StreamSource(new StringReader(response.getBody()));
        ErrorResponse error = MarshallingUtils.unmarshal(ErrorResponse.class, source);
        assertEquals("InvalidResource",error.getType());
        assertTrue(error.getDetail().contains("Only supported for internal or federated user."));

        // id provider not found
        userMO.setProviderId(usersToCleanup.get(1));
        userMOString = writeMOToString(userMO);
        response = processRequest("identityProviders/"+usersToCleanup.get(1)+"/users/", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),userMOString);
        assertEquals(404, response.getStatus());
        source = new StreamSource(new StringReader(response.getBody()));
        error = MarshallingUtils.unmarshal(ErrorResponse.class, source);
        assertEquals("ResourceNotFound",error.getType());
        assertEquals("IdentityProvider not found",error.getDetail());

        // id provider different in MO and query
        userMO.setProviderId(internalProviderId);
        userMOString = writeMOToString(userMO);
        response = processRequest("identityProviders/"+usersToCleanup.get(1)+"/users/", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),userMOString);
        assertEquals(400, response.getStatus());
        source = new StreamSource(new StringReader(response.getBody()));
        error = MarshallingUtils.unmarshal(ErrorResponse.class, source);
        assertEquals("InvalidArgument",error.getType());
        assertEquals("Invalid value for argument 'providerId'. Must specify the same provider ID",error.getDetail());

        // no identity provider
        userMO.setProviderId(null);
        userMOString = writeMOToString(userMO);
        response = processRequest("identityProviders/"+internalProviderId+"/users/", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),userMOString);
        assertEquals(400, response.getStatus());
        source = new StreamSource(new StringReader(response.getBody()));
        error = MarshallingUtils.unmarshal(ErrorResponse.class, source);
        assertEquals("BadRequest",error.getType());
    }

    @Test
    public void UserErrorUpdate() throws Exception {
        UserMO userMO = ManagedObjectFactory.createUserMO();
        userMO.setId(usersToCleanup.get(0));
        userMO.setProviderId(internalProviderId);
        userMO.setLogin("login");
        userMO.setFirstName("first name");
        userMO.setLastName("last name");
        String userMOString = writeMOToString(userMO);

        // non internal id provider
        userMO.setProviderId(otherIdentityProviderId);
        userMO.setId(usersToCleanup.get(1));
        userMOString = writeMOToString(userMO);
        RestResponse response = processRequest("identityProviders/"+otherIdentityProviderId+"/users/"+usersToCleanup.get(1), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),userMOString);
        assertEquals(400, response.getStatus());
        StreamSource source = new StreamSource(new StringReader(response.getBody()));
        ErrorResponse error = MarshallingUtils.unmarshal(ErrorResponse.class, source);
        assertEquals("InvalidResource",error.getType());
        assertTrue(error.getDetail().contains("Only supported for internal or federated identity provider."));

        // id provider not found
        userMO.setProviderId(groupsToCleanup.get(1));
        userMO.setId(usersToCleanup.get(1));
        userMOString = writeMOToString(userMO);
        response = processRequest("identityProviders/"+groupsToCleanup.get(1)+"/users/"+ usersToCleanup.get(1), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),userMOString);
        assertEquals(404, response.getStatus());
        source = new StreamSource(new StringReader(response.getBody()));
        error = MarshallingUtils.unmarshal(ErrorResponse.class, source);
        assertEquals("ResourceNotFound",error.getType());
        assertTrue(error.getDetail().contains("not found"));

        // user not found
        userMO.setProviderId(internalProviderId);
        userMO.setId(groupsToCleanup.get(1));
        userMOString = writeMOToString(userMO);
        response = processRequest("identityProviders/"+internalProviderId+"/users/"+ groupsToCleanup.get(1), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),userMOString);
        assertEquals(404, response.getStatus());
        source = new StreamSource(new StringReader(response.getBody()));
        error = MarshallingUtils.unmarshal(ErrorResponse.class, source);
        assertEquals("ResourceNotFound",error.getType());
        assertTrue(error.getDetail().contains("not found"));
    }

    @Test
    public void UserErrorDelete() throws Exception {
        // user not found
        RestResponse response = processRequest("identityProviders/"+internalProviderId+"/users/"+ groupsToCleanup.get(1), HttpMethod.DELETE, null,"");
        assertEquals(404, response.getStatus());
        StreamSource source = new StreamSource(new StringReader(response.getBody()));
        ErrorResponse error = MarshallingUtils.unmarshal(ErrorResponse.class, source);
        assertEquals("ResourceNotFound",error.getType());
        assertTrue(error.getDetail().contains("not found"));

        // id provider not found
        response = processRequest("identityProviders/"+ groupsToCleanup.get(1) +"/users/"+ groupsToCleanup.get(1), HttpMethod.DELETE, null,"");
        assertEquals(404, response.getStatus());
        source = new StreamSource(new StringReader(response.getBody()));
        error = MarshallingUtils.unmarshal(ErrorResponse.class, source);
        assertEquals("ResourceNotFound",error.getType());
        assertTrue(error.getDetail().contains("not found"));

        // not internal id provider
        response = processRequest("identityProviders/"+ otherIdentityProviderId +"/users/"+ groupsToCleanup.get(1), HttpMethod.DELETE, null,"");
        assertEquals(400, response.getStatus());
        source = new StreamSource(new StringReader(response.getBody()));
        error = MarshallingUtils.unmarshal(ErrorResponse.class, source);
        assertEquals("InvalidResource",error.getType());
        assertTrue(error.getDetail().contains("Only supported for internal or federated user."));
    }


    @Test
    public void GroupSearchTest() throws Exception {

        RestResponse response = processRequest("identityProviders/"+internalProviderId+"/groups?name=group1", HttpMethod.GET, ContentType.APPLICATION_XML.toString(),"");
        assertEquals(200, response.getStatus());
        ItemsList<GroupMO> groupList = MarshallingUtils.unmarshal(ItemsList.class, new StreamSource(new StringReader(response.getBody())));

        assertNotNull(groupList.getContent());
        assertEquals(1, groupList.getContent().size());
        assertEquals(groupsToCleanup.get(0),groupList.getContent().get(0).getId());

    }

    @Test
    public void GroupGetTest() throws Exception {

        RestResponse response = processRequest("identityProviders/"+internalProviderId+"/groups/"+groupsToCleanup.get(1), HttpMethod.GET, ContentType.APPLICATION_XML.toString(),"");
        assertEquals(200, response.getStatus());
        Item<GroupMO> groupList = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        assertNotNull(groupList.getContent());
        assertEquals(groupsToCleanup.get(1),groupList.getContent().getId());
    }

    @Test
    public void GroupTestInvalidIdentityProvider() throws Exception {
        // LIST
        RestResponse response = processRequest("identityProviders/"+new Goid(234,234).toString()+"/groups/", HttpMethod.GET, ContentType.APPLICATION_XML.toString(),"");
        assertEquals(404, response.getStatus());

        StreamSource source = new StreamSource(new StringReader(response.getBody()));
        ErrorResponse error = MarshallingUtils.unmarshal(ErrorResponse.class, source);
        assertEquals("ResourceNotFound",error.getType());
        assertEquals("IdentityProvider could not be found",error.getDetail());

        // GET
        response = processRequest("identityProviders/"+new Goid(234,234).toString()+"/groups/"+groupsToCleanup.get(1), HttpMethod.GET, ContentType.APPLICATION_XML.toString(),"");
        assertEquals(404, response.getStatus());

        source = new StreamSource(new StringReader(response.getBody()));
        error = MarshallingUtils.unmarshal(ErrorResponse.class, source);
        assertEquals("ResourceNotFound",error.getType());
        assertEquals("IdentityProvider could not be found",error.getDetail());
    }

    @Test
    public void GroupListTest() throws Exception {

        RestResponse response = processRequest("identityProviders/"+internalProviderId+"/groups", HttpMethod.GET, ContentType.APPLICATION_XML.toString(),"");
        assertEquals(200, response.getStatus());
        ItemsList<GroupMO> groupList = MarshallingUtils.unmarshal(ItemsList.class, new StreamSource(new StringReader(response.getBody())));

        assertNotNull(groupList.getContent());
        assertEquals(2, groupList.getContent().size());

    }

    @Test
    public void IdentityProviderGetDefaultTest() throws Exception {
        RestResponse response = processRequest("identityProviders/default", HttpMethod.GET, ContentType.APPLICATION_XML.toString(),"");
        assertEquals(200, response.getStatus());
        Item<IdentityProviderMO> internalIdProvider = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        assertNotNull(internalIdProvider.getContent());
        assertEquals(internalProviderId, internalIdProvider.getId());
    }
}
