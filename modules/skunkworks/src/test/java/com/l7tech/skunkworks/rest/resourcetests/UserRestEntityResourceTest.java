package com.l7tech.skunkworks.rest.resourcetests;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.password.PasswordHasher;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.identity.*;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.security.cert.TestCertificateGenerator;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.skunkworks.rest.tools.RestEntityTests;
import com.l7tech.skunkworks.rest.tools.RestResponse;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.IgnoreOnDaily;
import com.l7tech.util.Charsets;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import junit.framework.Assert;
import org.apache.http.entity.ContentType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.URLEncoder;
import java.security.cert.X509Certificate;
import java.util.*;

import static org.junit.Assert.*;

@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class UserRestEntityResourceTest extends RestEntityTests<User, UserMO> {

    private UserManager internalUserManager;
    private IdentityProvider internalIdentityProvider;
    private IdentityProviderFactory identityProviderFactory;
    private final String internalProviderId = IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID.toString();
    private final String adminUserId = new Goid(0,3).toString();
    private List<User> users = new ArrayList<>();

    private String strongPassword = "12!@qwQW";

    @Before
    public void before() throws Exception {
        identityProviderFactory = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("identityProviderFactory", IdentityProviderFactory.class);
        internalIdentityProvider = identityProviderFactory.getProvider(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID);
        internalUserManager = internalIdentityProvider.getUserManager();
        PasswordHasher passwordHasher = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("passwordHasher", PasswordHasher.class);

        //Create the users

        InternalUser user = new InternalUser("User1");
        user.setFirstName("First1");
        user.setLastName("Last1");
        user.setHashedPassword(passwordHasher.hashPassword("password".getBytes(Charsets.UTF8)));
        user.setEnabled(true);

        internalUserManager.save(user,null);
        users.add(user);

        user = new InternalUser("User2");
        user.setFirstName("First2");
        user.setLastName("Last2");
        user.setHashedPassword(passwordHasher.hashPassword("password".getBytes(Charsets.UTF8)));
        user.setEnabled(false);

        internalUserManager.save(user,null);
        users.add(user);

        user = new InternalUser("User3");
        user.setFirstName("First3");
        user.setLastName("Last3");
        user.setEmail("me@here.test");
        user.setHashedPassword(passwordHasher.hashPassword("password".getBytes(Charsets.UTF8)));
        user.setEnabled(true);

        internalUserManager.save(user,null);
        users.add(user);

    }

    @After
    public void after() throws FindException, DeleteException {
        Collection<IdentityHeader> all = internalUserManager.findAllHeaders();
        for (IdentityHeader user : all) {
            if(!user.getStrId().equals(adminUserId)) {
                internalUserManager.delete(user.getStrId());
            }
        }
    }

    @Override
    public List<String> getRetrievableEntityIDs() {
        return Functions.map(users, new Functions.Unary<String, User>() {
            @Override
            public String call(User user) {
                return user.getId();
            }
        });
    }

    @Override
    public List<UserMO> getCreatableManagedObjects() {
        List<UserMO> users = new ArrayList<>();

        UserMO userMO = ManagedObjectFactory.createUserMO();
        userMO.setProviderId(internalProviderId);
        userMO.setLogin("login");
        PasswordFormatted password = ManagedObjectFactory.createPasswordFormatted();
        password.setFormat("plain");
        password.setPassword(strongPassword);
        userMO.setPassword(password);
        userMO.setFirstName("first name");
        userMO.setLastName("last name");
        users.add(userMO);

        return users;
    }

    @Override
    public List<UserMO> getUpdateableManagedObjects() {
        List<UserMO> users = new ArrayList<>();

        User user = this.users.get(0);
        UserMO userMO = ManagedObjectFactory.createUserMO();
        userMO.setId(user.getId());
        userMO.setProviderId(user.getProviderId().toString());
        userMO.setLogin(user.getLogin() + " Updated");
        userMO.setFirstName(user.getFirstName());
        userMO.setLastName(user.getLastName());
        users.add(userMO);

        //update twice
        userMO = ManagedObjectFactory.createUserMO();
        userMO.setId(user.getId());
        userMO.setProviderId(user.getProviderId().toString());
        userMO.setLogin(user.getLogin() + " Updated");
        userMO.setFirstName(user.getFirstName());
        userMO.setLastName(user.getLastName());
        users.add(userMO);

        return users;
    }

    @Override
    public Map<UserMO, Functions.BinaryVoid<UserMO, RestResponse>> getUnCreatableManagedObjects() {
        CollectionUtils.MapBuilder<UserMO, Functions.BinaryVoid<UserMO, RestResponse>> builder = CollectionUtils.MapBuilder.builder();

        // no password
        UserMO userMO = ManagedObjectFactory.createUserMO();
        userMO.setProviderId(internalProviderId);
        userMO.setLogin("login");
        userMO.setFirstName("first name");
        userMO.setLastName("last name");
        builder.put(userMO, new Functions.BinaryVoid<UserMO, RestResponse>() {
            @Override
            public void call(UserMO userMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
                ErrorResponse error = getErrorResponse(restResponse);
                assertEquals("InvalidResource", error.getType());
                assertTrue(error.getDetail().contains("Password required"));
            }
        });

        // weak password
        userMO = ManagedObjectFactory.createUserMO();
        userMO.setProviderId(internalProviderId);
        userMO.setLogin("login");
        PasswordFormatted password = ManagedObjectFactory.createPasswordFormatted();
        password.setFormat("plain");
        password.setPassword("x");
        userMO.setPassword(password);
        userMO.setFirstName("first name");
        userMO.setLastName("last name");
        builder.put(userMO, new Functions.BinaryVoid<UserMO, RestResponse>() {
            @Override
            public void call(UserMO userMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
                ErrorResponse error = getErrorResponse(restResponse);
                assertEquals("InvalidResource", error.getType());
                assertTrue(error.getDetail().contains("invalid password"));
            }
        });

        return builder.map();
    }

    @Override
    public void testCreateWithIdEntity() throws Exception {
        // not implemented
    }

    @Override
    public void testCreateEntitySpecifyIDFailed() throws Exception {
        // not implemented
    }

    @Override
    public void testCreateWithIdEntityUnprivileged() throws Exception {
        // not implemented
    }

    @Override
    public Map<UserMO, Functions.BinaryVoid<UserMO, RestResponse>> getUnUpdateableManagedObjects() {
        CollectionUtils.MapBuilder<UserMO, Functions.BinaryVoid<UserMO, RestResponse>> builder = CollectionUtils.MapBuilder.builder();

        User user = this.users.get(0);
        UserMO userMO = ManagedObjectFactory.createUserMO();
        userMO.setId(getGoid().toString());
        userMO.setProviderId(user.getProviderId().toString());
        userMO.setLogin(user.getLogin() + " Updated");
        userMO.setFirstName(user.getFirstName());
        userMO.setLastName(user.getLastName());

        builder.put(userMO, new Functions.BinaryVoid<UserMO, RestResponse>() {
            @Override
            public void call(UserMO userMO, RestResponse restResponse) {
                Assert.assertEquals(404, restResponse.getStatus());
            }
        });

        return builder.map();
    }

    @Override
    public Map<String, Functions.BinaryVoid<String, RestResponse>> getUnGettableManagedObjectIds() {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, Functions.BinaryVoid<String, RestResponse>> getUnDeleteableManagedObjectIds() {
        CollectionUtils.MapBuilder<String, Functions.BinaryVoid<String, RestResponse>> builder = CollectionUtils.MapBuilder.builder();
        builder.put(adminUserId, new Functions.BinaryVoid<String, RestResponse>() {
            @Override
            public void call(String id, RestResponse restResponse) {
                Assert.assertEquals(404, restResponse.getStatus());
                ErrorResponse error = getErrorResponse(restResponse);
                assertEquals("ResourceNotFound", error.getType());
            }
        });

        return builder.map();
    }

    @Override
    public Map<String, Functions.BinaryVoid<String, RestResponse>> getBadListQueries() {
        return Collections.emptyMap();
    }

    @Override
    public List<String> getDeleteableManagedObjectIDs() {
        return Functions.map(users, new Functions.Unary<String, User>() {
            @Override
            public String call(User user) {
                return user.getId();
            }
        });
    }

    @Override
    public String getResourceUri() {
        return "identityProviders/"+internalProviderId+"/users";
    }

    @Override
    public String getType() {
        return EntityType.USER.name();
    }

    @Override
    public String getExpectedTitle(String id) throws FindException {
        User entity = internalUserManager.findByPrimaryKey(id);
        Assert.assertNotNull(entity);
        return entity.getLogin();
    }

    @Override
    public void verifyLinks(String id, List<Link> links) throws FindException {
        User entity = internalUserManager.findByPrimaryKey(id);
        Assert.assertNotNull(entity);
    }

    @Override
    public void verifyEntity(String id, UserMO managedObject) throws FindException {
        User entity = internalUserManager.findByPrimaryKey(id);
        if (managedObject == null) {
            Assert.assertNull(entity);
        } else {
            Assert.assertNotNull(entity);

            Assert.assertEquals(entity.getId(), managedObject.getId());
            Assert.assertEquals(entity.getLogin(), managedObject.getLogin());
            Assert.assertEquals(entity.getFirstName(), managedObject.getFirstName());
            Assert.assertEquals(entity.getLastName(), managedObject.getLastName());
            Assert.assertEquals(entity.getEmail(), managedObject.getEmail());
        }
    }

    @Override
    public Map<String, List<String>> getListQueryAndExpectedResults() throws FindException {

        return CollectionUtils.MapBuilder.<String, List<String>>builder()
                .put("", Arrays.asList(users.get(0).getId(),users.get(1).getId(),users.get(2).getId(),adminUserId))
                .put("sort=login", Arrays.asList(users.get(0).getId(),users.get(1).getId(),users.get(2).getId(),adminUserId))
                .put("sort=id", Arrays.asList(adminUserId,users.get(0).getId(),users.get(1).getId(),users.get(2).getId()))
                .put("sort=login&order=desc", Arrays.asList(adminUserId,users.get(2).getId(),users.get(1).getId(),users.get(0).getId()))
                .put("login=" + URLEncoder.encode(users.get(0).getLogin()), Arrays.asList(users.get(0).getId()))
                .put("login=" + URLEncoder.encode(users.get(0).getLogin()) + "&login=" + URLEncoder.encode(users.get(1).getLogin()), Functions.map(users.subList(0, 2), new Functions.Unary<String, User>() {
                    @Override
                    public String call(User user) {
                        return user.getId();
                    }
                }))
                .put("login=banName", Collections.<String>emptyList())
               .map();
    }

    @Override
    protected boolean getDefaultListIsOrdered() {
        return false;
    }

    private ErrorResponse getErrorResponse(RestResponse restResponse) {
        try {
            StreamSource source = new StreamSource(new StringReader(restResponse.getBody()));
            return MarshallingUtils.unmarshal(ErrorResponse.class, source);
        } catch (IOException e) {
            fail("Error response expected: " + e.getMessage());
        }
        // never reach
        return null;
    }

    @Test
    public void testUserSetCertificateUnprivileged() throws Exception {
        InternalUser user = createUnprivilegedUser();

        String userId = users.get(0).getId();
        try {
            // create certificate
            X509Certificate certificate = new TestCertificateGenerator().subject("cn=user1").generate();
            CertificateData certData = ManagedObjectFactory.createCertificateData(certificate);
            RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest("identityProviders/" + internalProviderId + "/users/" + userId + "/certificate", null, HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), writeMOToString(certData), user);
            Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
            Assert.assertEquals(401, response.getStatus());

        }finally {
            userManager.delete(user);
        }
    }

    @Test
    public void testUserRevokeCertificateUnprivileged() throws Exception {
        InternalUser user = createUnprivilegedUser();

        String userId = users.get(0).getId();
        String userLogin = users.get(0).getLogin();
        try {
            // create certificate
            X509Certificate certificate = new TestCertificateGenerator().subject("cn="+userLogin).generate();
            CertificateData certData = ManagedObjectFactory.createCertificateData(certificate);
            RestResponse response = processRequest("identityProviders/" + internalProviderId + "/users/" + userId + "/certificate", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), writeMOToString(certData));
            assertEquals(200, response.getStatus());

            // revoke certificate
            response = getDatabaseBasedRestManagementEnvironment().processRequest("identityProviders/" + internalProviderId + "/users/" + userId + "/certificate",null, HttpMethod.DELETE, ContentType.APPLICATION_XML.toString(), "",user);
            Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
            Assert.assertEquals(401, response.getStatus());

        }finally {
            userManager.delete(user);

            RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest("identityProviders/" + internalProviderId + "/users/" + userId + "/certificate",null, HttpMethod.DELETE, ContentType.APPLICATION_XML.toString(), "");
            assertEquals(204, response.getStatus());
        }
    }

    @Test
    public void testUserGetCertificateUnprivileged() throws Exception {
        InternalUser user = createUnprivilegedUser();

        String userId = users.get(0).getId();
        String userLogin = users.get(0).getLogin();
        try {
            // create certificate
            X509Certificate certificate = new TestCertificateGenerator().subject("cn="+userLogin).generate();
            CertificateData certData = ManagedObjectFactory.createCertificateData(certificate);
            RestResponse response = processRequest("identityProviders/" + internalProviderId + "/users/" + userId + "/certificate", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), writeMOToString(certData));
            assertEquals(200, response.getStatus());

            // get certificate
            response = getDatabaseBasedRestManagementEnvironment().processRequest("identityProviders/" + internalProviderId + "/users/" + userId + "/certificate",null, HttpMethod.GET, ContentType.APPLICATION_XML.toString(), "",user);
            Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
            Assert.assertEquals(401, response.getStatus());

        }finally {
            userManager.delete(user);

            RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest("identityProviders/" + internalProviderId + "/users/" + userId + "/certificate",null, HttpMethod.DELETE, ContentType.APPLICATION_XML.toString(), "");
            assertEquals(204, response.getStatus());
        }
    }


    protected String writeMOToString(Object  mo) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        final StreamResult result = new StreamResult( bout );
        MarshallingUtils.marshal( mo, result, false );
        return bout.toString();
    }
}
