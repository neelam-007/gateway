package com.l7tech.skunkworks.rest.resourcetests;

import com.l7tech.gateway.api.Link;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.StoredPasswordMO;
import com.l7tech.gateway.common.resources.HttpConfiguration;
import com.l7tech.gateway.common.resources.HttpProxyConfiguration;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gateway.common.siteminder.SiteMinderConfiguration;
import com.l7tech.objectmodel.*;
import com.l7tech.server.globalresources.HttpConfigurationManager;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.server.siteminder.SiteMinderConfigurationManager;
import com.l7tech.skunkworks.rest.tools.RestEntityTests;
import com.l7tech.skunkworks.rest.tools.RestResponse;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.IgnoreOnDaily;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;

import java.net.URLEncoder;
import java.text.ParseException;
import java.util.*;

@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class SecurePasswordRestEntityResourceTest extends RestEntityTests<SecurePassword, StoredPasswordMO> {
    private SecurePasswordManager securePasswordManager;
    private SiteMinderConfigurationManager siteMinderConfigurationManager;
    private List<SecurePassword> securePasswords = new ArrayList<>();
    private SiteMinderConfiguration siteMinderConfiguration;
    private SecurePassword siteMinderPass;
    private HttpConfigurationManager httpConfigurationManager;
    private SecurePassword httpConfigPass;
    private SecurePassword httpConfigProxyPass;
    private HttpConfiguration httpConfiguration;

    @Before
    public void before() throws SaveException, FindException {
        securePasswordManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("securePasswordManager", SecurePasswordManager.class);
        siteMinderConfigurationManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("siteMinderConfigurationManager", SiteMinderConfigurationManager.class);
        httpConfigurationManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("httpConfigurationManager", HttpConfigurationManager.class);

        //Create the active connectors

        SecurePassword securePassword = new SecurePassword();
        securePassword.setName("Password1");
        securePassword.setType(SecurePassword.SecurePasswordType.PASSWORD);
        securePassword.setDescription("My Password1 Description");
        securePassword.setLastUpdateAsDate(new Date());
        securePassword.setUsageFromVariable(true);
        securePassword.setEncodedPassword(securePasswordManager.encryptPassword("pass1".toCharArray()));

        securePasswordManager.save(securePassword);
        securePasswords.add(securePassword);

        securePassword = new SecurePassword();
        securePassword.setName("Password2");
        securePassword.setType(SecurePassword.SecurePasswordType.PASSWORD);
        securePassword.setDescription("My Password2 Description");
        securePassword.setLastUpdateAsDate(new Date());
        securePassword.setUsageFromVariable(false);
        securePassword.setEncodedPassword(securePasswordManager.encryptPassword("pass2".toCharArray()));

        securePasswordManager.save(securePassword);
        securePasswords.add(securePassword);

        securePassword = new SecurePassword();
        securePassword.setName("Password3");
        securePassword.setType(SecurePassword.SecurePasswordType.PEM_PRIVATE_KEY);
        securePassword.setDescription("My Password3 Description");
        securePassword.setLastUpdateAsDate(new Date());
        securePassword.setUsageFromVariable(true);
        securePassword.setEncodedPassword(securePasswordManager.encryptPassword("pass3".toCharArray()));

        securePasswordManager.save(securePassword);
        securePasswords.add(securePassword);

        siteMinderPass = new SecurePassword();
        siteMinderPass.setName("SiteMinderPass");
        siteMinderPass.setType(SecurePassword.SecurePasswordType.PASSWORD);
        siteMinderPass.setDescription("My siteminder Description");
        siteMinderPass.setLastUpdateAsDate(new Date());
        siteMinderPass.setUsageFromVariable(false);
        siteMinderPass.setEncodedPassword(securePasswordManager.encryptPassword("pass2".toCharArray()));

        securePasswordManager.save(siteMinderPass);

        httpConfigPass = new SecurePassword();
        httpConfigPass.setName("HttpConfigPass");
        httpConfigPass.setType(SecurePassword.SecurePasswordType.PASSWORD);
        httpConfigPass.setDescription("My httpConfigPass Description");
        httpConfigPass.setLastUpdateAsDate(new Date());
        httpConfigPass.setUsageFromVariable(false);
        httpConfigPass.setEncodedPassword(securePasswordManager.encryptPassword("pass2".toCharArray()));

        securePasswordManager.save(httpConfigPass);

        httpConfigProxyPass = new SecurePassword();
        httpConfigProxyPass.setName("HttpConfigProxyPass");
        httpConfigProxyPass.setType(SecurePassword.SecurePasswordType.PASSWORD);
        httpConfigProxyPass.setDescription("My httpConfigProxyPass Description");
        httpConfigProxyPass.setLastUpdateAsDate(new Date());
        httpConfigProxyPass.setUsageFromVariable(false);
        httpConfigProxyPass.setEncodedPassword(securePasswordManager.encryptPassword("pass2".toCharArray()));

        securePasswordManager.save(httpConfigProxyPass);

        siteMinderConfiguration = new SiteMinderConfiguration();
        siteMinderConfiguration.setName("Test Siteminder Config");
        siteMinderConfiguration.setUserName("username");
        siteMinderConfiguration.setPasswordGoid(siteMinderPass.getGoid());
        siteMinderConfiguration.setAddress("0.0.0.0");
        siteMinderConfiguration.setClusterThreshold(3);
        siteMinderConfiguration.setSecret("secret");
        siteMinderConfigurationManager.save(siteMinderConfiguration);

        httpConfiguration = new HttpConfiguration();
        httpConfiguration.setHost("myHost");
        httpConfiguration.setUsername("UserName");
        httpConfiguration.setPasswordGoid(httpConfigPass.getGoid());
        httpConfiguration.setPort(5555);
        httpConfiguration.setConnectTimeout(1000);
        httpConfiguration.setFollowRedirects(false);
        httpConfiguration.setNtlmDomain("ntlmDomain");
        httpConfiguration.setNtlmHost("ntlmHost");
        httpConfiguration.setPath("path");
        httpConfiguration.setProtocol(HttpConfiguration.Protocol.HTTP);
        httpConfiguration.setReadTimeout(2000);
        httpConfiguration.setTlsCipherSuites("tlsCipherSuites");
        httpConfiguration.setTlsKeystoreAlias("tlsKeystoreAlias");
        httpConfiguration.setTlsKeystoreGoid(new Goid(888, 999));
        httpConfiguration.setTlsVersion("1");
        httpConfiguration.setTlsKeyUse(HttpConfiguration.Option.DEFAULT);
        httpConfiguration.setProxyUse(HttpConfiguration.Option.CUSTOM);
        HttpProxyConfiguration httpProxyConfiguration = new HttpProxyConfiguration();
        httpProxyConfiguration.setPasswordGoid(httpConfigProxyPass.getGoid());
        httpProxyConfiguration.setHost("ProxyHost");
        httpProxyConfiguration.setPort(8888);
        httpProxyConfiguration.setUsername("ProxyUserName");
        httpConfiguration.setProxyConfiguration(httpProxyConfiguration);

        httpConfigurationManager.save(httpConfiguration);
    }

    @After
    public void after() throws FindException, DeleteException {
        siteMinderConfigurationManager.delete(siteMinderConfiguration.getGoid());
        httpConfigurationManager.delete(httpConfiguration.getGoid());
        Collection<SecurePassword> all = securePasswordManager.findAll();
        for (SecurePassword securePassword : all) {
            securePasswordManager.delete(securePassword.getGoid());
        }
    }

    @Override
    public List<String> getRetrievableEntityIDs() {
        List<SecurePassword> allPasswords = new ArrayList<>(securePasswords);
        allPasswords.add(siteMinderPass);
        allPasswords.add(httpConfigPass);
        allPasswords.add(httpConfigProxyPass);
        return Functions.map(allPasswords, new Functions.Unary<String, SecurePassword>() {
            @Override
            public String call(SecurePassword securePassword) {
                return securePassword.getId();
            }
        });
    }

    @Override
    public List<StoredPasswordMO> getCreatableManagedObjects() {
        List<StoredPasswordMO> storedPasswordMOs = new ArrayList<>();

        StoredPasswordMO storedPassword = ManagedObjectFactory.createStoredPassword();
        storedPassword.setId(getGoid().toString());
        storedPassword.setName("CreatedPassword");
        storedPassword.setPassword("myPass");
        storedPasswordMOs.add(storedPassword);

        storedPassword = ManagedObjectFactory.createStoredPassword();
        storedPassword.setId(getGoid().toString());
        storedPassword.setName("CreatedPassword2");
        storedPassword.setPassword("myPass");
        storedPassword.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .map());
        storedPasswordMOs.add(storedPassword);

        storedPassword = ManagedObjectFactory.createStoredPassword();
        storedPassword.setId(getGoid().toString());
        storedPassword.setName("CreatedPassword3");
        storedPassword.setPassword("myPass");
        storedPassword.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("usageFromVariable", true)
                .put("description", "my create password description")
                .put("type", "PEM Private Key")
                .map());
        storedPasswordMOs.add(storedPassword);

        return storedPasswordMOs;
    }

    @Override
    public List<StoredPasswordMO> getUpdateableManagedObjects() {
        List<StoredPasswordMO> storedPasswordMOs = new ArrayList<>();

        SecurePassword securePassword = this.securePasswords.get(0);
        StoredPasswordMO storedPasswordMO = ManagedObjectFactory.createStoredPassword();
        storedPasswordMO.setId(securePassword.getId());
        storedPasswordMO.setName(securePassword.getName() + "Updated");
        storedPasswordMOs.add(storedPasswordMO);

        storedPasswordMO = ManagedObjectFactory.createStoredPassword();
        storedPasswordMO.setId(securePasswords.get(1).getId());
        storedPasswordMO.setName(securePasswords.get(1).getName() + "Updated");
        storedPasswordMO.setPassword("UpdatedPass");
        storedPasswordMOs.add(storedPasswordMO);

        storedPasswordMO = ManagedObjectFactory.createStoredPassword();
        storedPasswordMO.setId(securePasswords.get(2).getId());
        storedPasswordMO.setName(securePasswords.get(2).getName() + "Updated");
        storedPasswordMOs.add(storedPasswordMO);

        //update twice
        storedPasswordMO = ManagedObjectFactory.createStoredPassword();
        storedPasswordMO.setId(securePasswords.get(2).getId());
        storedPasswordMO.setName(securePasswords.get(2).getName() + "Updated");
        storedPasswordMOs.add(storedPasswordMO);

        return storedPasswordMOs;
    }

    @Override
    public Map<StoredPasswordMO, Functions.BinaryVoid<StoredPasswordMO, RestResponse>> getUnCreatableManagedObjects() {
        CollectionUtils.MapBuilder<StoredPasswordMO, Functions.BinaryVoid<StoredPasswordMO, RestResponse>> builder = CollectionUtils.MapBuilder.builder();

        StoredPasswordMO storedPasswordMO = ManagedObjectFactory.createStoredPassword();
        storedPasswordMO.setName(securePasswords.get(2).getName());
        storedPasswordMO.setPassword("myPass");

        builder.put(storedPasswordMO, new Functions.BinaryVoid<StoredPasswordMO, RestResponse>() {
            @Override
            public void call(StoredPasswordMO storedPassword, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        storedPasswordMO = ManagedObjectFactory.createStoredPassword();
        storedPasswordMO.setName("CreatingwithnoPass");

        builder.put(storedPasswordMO, new Functions.BinaryVoid<StoredPasswordMO, RestResponse>() {
            @Override
            public void call(StoredPasswordMO storedPassword, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        return builder.map();
    }

    @Override
    public Map<StoredPasswordMO, Functions.BinaryVoid<StoredPasswordMO, RestResponse>> getUnUpdateableManagedObjects() {
        CollectionUtils.MapBuilder<StoredPasswordMO, Functions.BinaryVoid<StoredPasswordMO, RestResponse>> builder = CollectionUtils.MapBuilder.builder();

        StoredPasswordMO storedPasswordMO = ManagedObjectFactory.createStoredPassword();
        storedPasswordMO.setId(securePasswords.get(1).getId());
        storedPasswordMO.setName(securePasswords.get(2).getName());

        builder.put(storedPasswordMO, new Functions.BinaryVoid<StoredPasswordMO, RestResponse>() {
            @Override
            public void call(StoredPasswordMO storedPassword, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
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

        //SSG-8245
        builder.put(siteMinderPass.getId(), new Functions.BinaryVoid<String, RestResponse>() {
            @Override
            public void call(String passwordID, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        builder.put(httpConfigPass.getId(), new Functions.BinaryVoid<String, RestResponse>() {
            @Override
            public void call(String passwordID, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });
        builder.put(httpConfigProxyPass.getId(), new Functions.BinaryVoid<String, RestResponse>() {
            @Override
            public void call(String passwordID, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
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
        return Functions.map(securePasswords, new Functions.Unary<String, SecurePassword>() {
            @Override
            public String call(SecurePassword securePassword) {
                return securePassword.getId();
            }
        });
    }

    @Override
    public String getResourceUri() {
        return "passwords";
    }

    @Override
    public String getType() {
        return EntityType.SECURE_PASSWORD.name();
    }

    @Override
    public String getExpectedTitle(String id) throws FindException {
        SecurePassword entity = securePasswordManager.findByPrimaryKey(Goid.parseGoid(id));
        Assert.assertNotNull(entity);
        return entity.getName();
    }

    @Override
    public void verifyLinks(String id, List<Link> links) throws FindException {
        SecurePassword entity = securePasswordManager.findByPrimaryKey(Goid.parseGoid(id));
        Assert.assertNotNull(entity);
    }

    @Override
    public void verifyEntity(String id, StoredPasswordMO managedObject) throws FindException, ParseException {
        SecurePassword entity = securePasswordManager.findByPrimaryKey(Goid.parseGoid(id));
        if (managedObject == null) {
            Assert.assertNull(entity);
        } else {
            Assert.assertNotNull(entity);

            Assert.assertEquals(entity.getId(), managedObject.getId());
            Assert.assertEquals(entity.getName(), managedObject.getName());
            Assert.assertEquals(entity.getDescription(), managedObject.getProperties() == null ? null : managedObject.getProperties().get("description"));
            //if this is not specified the it will use the current setting or default to password
            if(managedObject.getProperties() != null && managedObject.getProperties().get("type") != null) {
                verifyType(entity.getType(), (String) managedObject.getProperties().get("type"));
            }
            //on create this does not need to be specified.
            if(managedObject.getProperties() != null && managedObject.getProperties().get("lastUpdated") != null) {
                Assert.assertEquals(entity.getLastUpdateAsDate(), managedObject.getProperties().get("lastUpdated"));
            }
            Assert.assertEquals(entity.isUsageFromVariable(), (managedObject.getProperties() == null || managedObject.getProperties().get("usageFromVariable") == null) ? false : managedObject.getProperties().get("usageFromVariable"));
            if (managedObject.getPassword() != null) {
                Assert.assertEquals(new String(securePasswordManager.decryptPassword(entity.getEncodedPassword())), managedObject.getPassword());
            }
        }
    }

    private void verifyType(SecurePassword.SecurePasswordType type, String typeString) {
        switch (type) {
            case PASSWORD:
                //null defaults to password so that is ok.
                Assert.assertEquals("Password", typeString);
                break;
            case PEM_PRIVATE_KEY:
                Assert.assertEquals("PEM Private Key", typeString);
                break;
            default:
                Assert.fail("Unknown type");
        }
    }

    @Override
    public Map<String, List<String>> getListQueryAndExpectedResults() throws FindException {
        List<SecurePassword> allPasswords = new ArrayList<>(securePasswords);
        allPasswords.add(siteMinderPass);
        allPasswords.add(httpConfigPass);
        allPasswords.add(httpConfigProxyPass);
        return CollectionUtils.MapBuilder.<String, List<String>>builder()
                .put("", Functions.map(allPasswords, new Functions.Unary<String, SecurePassword>() {
                    @Override
                    public String call(SecurePassword securePassword) {
                        return securePassword.getId();
                    }
                }))
                .put("name=" + URLEncoder.encode(securePasswords.get(0).getName()), Arrays.asList(securePasswords.get(0).getId()))
                .put("name=" + URLEncoder.encode(securePasswords.get(0).getName()) + "&name=" + URLEncoder.encode(securePasswords.get(1).getName()), Functions.map(securePasswords.subList(0, 2), new Functions.Unary<String, SecurePassword>() {
                    @Override
                    public String call(SecurePassword securePassword) {
                        return securePassword.getId();
                    }
                }))
                .put("name=banName", Collections.<String>emptyList())
                .put("type=Password", Arrays.asList(securePasswords.get(0).getId(), securePasswords.get(1).getId(), siteMinderPass.getId(), httpConfigPass.getId(), httpConfigProxyPass.getId()))
                .put("type=" + URLEncoder.encode("PEM Private Key"), Arrays.asList(securePasswords.get(2).getId()))
                .put("name=" + URLEncoder.encode(securePasswords.get(0).getName()) + "&name=" + URLEncoder.encode(securePasswords.get(1).getName()) + "&sort=name&order=desc", Arrays.asList(securePasswords.get(1).getId(), securePasswords.get(0).getId()))
                .map();
    }
}
