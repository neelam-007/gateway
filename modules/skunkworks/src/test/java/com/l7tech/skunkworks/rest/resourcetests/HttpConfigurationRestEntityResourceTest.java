package com.l7tech.skunkworks.rest.resourcetests;

import com.l7tech.gateway.api.HttpConfigurationMO;
import com.l7tech.gateway.api.Link;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.common.resources.HttpConfiguration;
import com.l7tech.gateway.common.resources.HttpProxyConfiguration;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.objectmodel.*;
import com.l7tech.server.globalresources.HttpConfigurationManager;
import com.l7tech.server.security.password.SecurePasswordManager;
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
import java.util.*;

@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class HttpConfigurationRestEntityResourceTest extends RestEntityTests<HttpConfiguration, HttpConfigurationMO> {
    private HttpConfigurationManager httpConfigurationManager;
    private SecurePasswordManager securePasswordManager;
    private List<HttpConfiguration> httpConfigurations = new ArrayList<>();
    private SecurePassword securePassword;

    @Before
    public void before() throws SaveException, FindException {
        httpConfigurationManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("httpConfigurationManager", HttpConfigurationManager.class);
        securePasswordManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("securePasswordManager", SecurePasswordManager.class);

        securePassword = new SecurePassword();
        securePassword.setType(SecurePassword.SecurePasswordType.PASSWORD);
        securePassword.setName("MyPass");
        securePassword.setEncodedPassword(securePasswordManager.encryptPassword( "password".toCharArray() ));
        securePasswordManager.save(securePassword);

        //Create the active connectors

        HttpConfiguration httpConfiguration = new HttpConfiguration();
        httpConfiguration.setHost("myHost");
        httpConfiguration.setUsername("UserName");
        httpConfiguration.setPasswordGoid(securePassword.getGoid());
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
        httpProxyConfiguration.setPasswordGoid(securePassword.getGoid());
        httpProxyConfiguration.setHost("ProxyHost");
        httpProxyConfiguration.setPort(8888);
        httpProxyConfiguration.setUsername("ProxyUserName");
        httpConfiguration.setProxyConfiguration(httpProxyConfiguration);

        httpConfigurationManager.save(httpConfiguration);
        httpConfigurations.add(httpConfiguration);

        httpConfiguration = new HttpConfiguration();
        httpConfiguration.setHost("myHost2");
        httpConfiguration.setUsername("UserName2");
        httpConfiguration.setPasswordGoid(securePassword.getGoid());
        httpConfiguration.setPort(5556);
        httpConfiguration.setConnectTimeout(1002);
        httpConfiguration.setFollowRedirects(true);
        httpConfiguration.setNtlmDomain("ntlmDomain2");
        httpConfiguration.setNtlmHost("ntlmHost2");
        httpConfiguration.setPath("path2");
        httpConfiguration.setProtocol(HttpConfiguration.Protocol.HTTPS);
        httpConfiguration.setReadTimeout(2002);
        httpConfiguration.setTlsCipherSuites("tlsCipherSuites2");
        httpConfiguration.setTlsKeystoreAlias("tlsKeystoreAlias2");
        httpConfiguration.setTlsKeystoreGoid(new Goid(882, 999));
        httpConfiguration.setTlsVersion("2");
        httpConfiguration.setTlsKeyUse(HttpConfiguration.Option.CUSTOM);

        httpConfigurationManager.save(httpConfiguration);
        httpConfigurations.add(httpConfiguration);

        httpConfiguration = new HttpConfiguration();
        httpConfiguration.setHost("myHost3");
        httpConfiguration.setUsername("UserName3");
        httpConfiguration.setPasswordGoid(securePassword.getGoid());
        httpConfiguration.setPort(5553);
        httpConfiguration.setConnectTimeout(1003);
        httpConfiguration.setFollowRedirects(false);
        httpConfiguration.setNtlmDomain("ntlmDomain3");
        httpConfiguration.setNtlmHost("ntlmHost3");
        httpConfiguration.setPath("path3");
        httpConfiguration.setProtocol(HttpConfiguration.Protocol.HTTP);
        httpConfiguration.setReadTimeout(2003);
        httpConfiguration.setTlsCipherSuites("tlsCipherSuites3");
        httpConfiguration.setTlsKeystoreAlias("tlsKeystoreAlias3");
        httpConfiguration.setTlsKeystoreGoid(new Goid(0, 0));
        httpConfiguration.setTlsVersion("3");
        httpConfiguration.setTlsKeyUse(HttpConfiguration.Option.NONE);
        httpConfiguration.setProxyUse(HttpConfiguration.Option.NONE);

        httpConfigurationManager.save(httpConfiguration);
        httpConfigurations.add(httpConfiguration);

        httpConfiguration = new HttpConfiguration();
        httpConfiguration.setHost("myHost4");
        httpConfiguration.setPort(5553);
        httpConfiguration.setConnectTimeout(1003);
        httpConfiguration.setFollowRedirects(false);
        httpConfiguration.setReadTimeout(2003);
        httpConfiguration.setTlsKeyUse(HttpConfiguration.Option.NONE);
        httpConfiguration.setProxyUse(HttpConfiguration.Option.NONE);
        httpConfiguration.setTlsKeystoreGoid(new Goid(0, 0));

        httpConfigurationManager.save(httpConfiguration);
        httpConfigurations.add(httpConfiguration);

    }

    @After
    public void after() throws FindException, DeleteException {
        Collection<HttpConfiguration> all = httpConfigurationManager.findAll();
        for (HttpConfiguration httpConfiguration : all) {
            httpConfigurationManager.delete(httpConfiguration.getGoid());
        }

        securePasswordManager.delete(securePassword);
    }

    @Override
    public List<String> getRetrievableEntityIDs() {
        return Functions.map(httpConfigurations, new Functions.Unary<String, HttpConfiguration>() {
            @Override
            public String call(HttpConfiguration httpConfiguration) {
                return httpConfiguration.getId();
            }
        });
    }

    @Override
    public List<HttpConfigurationMO> getCreatableManagedObjects() {
        List<HttpConfigurationMO> httpConfigurations = new ArrayList<>();

        HttpConfigurationMO httpConfiguration = ManagedObjectFactory.createHttpConfiguration();
        httpConfiguration.setId(getGoid().toString());
        httpConfiguration.setUsername("userNew");
        httpConfiguration.setPort(333);
        httpConfiguration.setHost("newHost");
        httpConfiguration.setPasswordId(securePassword.getId());
        httpConfiguration.setProtocol(HttpConfigurationMO.Protocol.HTTP);
        httpConfiguration.setPath("path");
        httpConfiguration.setTlsKeyUse(HttpConfigurationMO.Option.DEFAULT);
        httpConfigurations.add(httpConfiguration);

        httpConfiguration = ManagedObjectFactory.createHttpConfiguration();
        httpConfiguration.setId(getGoid().toString());
        httpConfiguration.setPort(333);
        httpConfiguration.setHost("newHost2");
        httpConfiguration.setProtocol(HttpConfigurationMO.Protocol.HTTP);
        httpConfiguration.setPath("path");
        httpConfiguration.setTlsKeyUse(HttpConfigurationMO.Option.DEFAULT);
        httpConfigurations.add(httpConfiguration);

        httpConfiguration = ManagedObjectFactory.createHttpConfiguration();
        httpConfiguration.setId(getGoid().toString());
        httpConfiguration.setPort(333);
        httpConfiguration.setHost("newHost3");
        httpConfiguration.setProtocol(HttpConfigurationMO.Protocol.HTTP);
        httpConfiguration.setTlsKeyUse(HttpConfigurationMO.Option.DEFAULT);
        httpConfigurations.add(httpConfiguration);

        return httpConfigurations;
    }

    @Override
    public List<HttpConfigurationMO> getUpdateableManagedObjects() {
        List<HttpConfigurationMO> httpConfigurations = new ArrayList<>();

        HttpConfiguration httpConfiguration = this.httpConfigurations.get(0);
        HttpConfigurationMO httpConfigurationMO = ManagedObjectFactory.createHttpConfiguration();
        httpConfigurationMO.setId(httpConfiguration.getId());
        httpConfigurationMO.setUsername(httpConfiguration.getUsername());
        httpConfigurationMO.setPasswordId(httpConfiguration.getPasswordGoid().toString());
        httpConfigurationMO.setPort(httpConfiguration.getPort());
        httpConfigurationMO.setHost(httpConfiguration.getHost() + "Updated");
        httpConfigurationMO.setProtocol(HttpConfigurationMO.Protocol.HTTP);
        httpConfigurationMO.setPath(httpConfiguration.getPath());

        httpConfigurations.add(httpConfigurationMO);

        //update twice
        httpConfigurationMO = ManagedObjectFactory.createHttpConfiguration();
        httpConfigurationMO.setId(httpConfiguration.getId());
        httpConfigurationMO.setUsername(httpConfiguration.getUsername());
        httpConfigurationMO.setPasswordId(httpConfiguration.getPasswordGoid().toString());
        httpConfigurationMO.setPort(httpConfiguration.getPort());
        httpConfigurationMO.setHost(httpConfiguration.getHost() + "Updated");
        httpConfigurationMO.setProtocol(HttpConfigurationMO.Protocol.HTTP);
        httpConfigurationMO.setPath(httpConfiguration.getPath());

        httpConfigurations.add(httpConfigurationMO);

        return httpConfigurations;
    }

    @Override
    public Map<HttpConfigurationMO, Functions.BinaryVoid<HttpConfigurationMO, RestResponse>> getUnCreatableManagedObjects() {
        CollectionUtils.MapBuilder<HttpConfigurationMO, Functions.BinaryVoid<HttpConfigurationMO, RestResponse>> builder = CollectionUtils.MapBuilder.builder();

        HttpConfigurationMO httpConfigurationMO = ManagedObjectFactory.createHttpConfiguration();
        httpConfigurationMO.setHost(httpConfigurations.get(0).getHost());
        httpConfigurationMO.setUsername(httpConfigurations.get(0).getUsername());
        httpConfigurationMO.setPasswordId(httpConfigurations.get(0).getPasswordGoid().toString());
        httpConfigurationMO.setPort(httpConfigurations.get(0).getPort());
        httpConfigurationMO.setProtocol(HttpConfigurationMO.Protocol.HTTP);
        httpConfigurationMO.setPath(httpConfigurations.get(0).getPath());

        builder.put(httpConfigurationMO, new Functions.BinaryVoid<HttpConfigurationMO, RestResponse>() {
            @Override
            public void call(HttpConfigurationMO httpConfigurationMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        httpConfigurationMO = ManagedObjectFactory.createHttpConfiguration();
        httpConfigurationMO.setHost(httpConfigurations.get(0).getHost() + "Updated");
        httpConfigurationMO.setUsername(httpConfigurations.get(0).getUsername());
        httpConfigurationMO.setPasswordId(new Goid(123,456).toString());
        httpConfigurationMO.setPort(httpConfigurations.get(0).getPort());
        httpConfigurationMO.setProtocol(HttpConfigurationMO.Protocol.HTTP);
        httpConfigurationMO.setPath(httpConfigurations.get(0).getPath());

        builder.put(httpConfigurationMO, new Functions.BinaryVoid<HttpConfigurationMO, RestResponse>() {
            @Override
            public void call(HttpConfigurationMO httpConfigurationMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        httpConfigurationMO = ManagedObjectFactory.createHttpConfiguration();
        httpConfigurationMO.setHost(httpConfigurations.get(0).getHost() + "Updated");
        httpConfigurationMO.setUsername(httpConfigurations.get(0).getUsername());
        httpConfigurationMO.setPort(httpConfigurations.get(0).getPort());
        httpConfigurationMO.setProtocol(HttpConfigurationMO.Protocol.HTTP);
        httpConfigurationMO.setPath(httpConfigurations.get(0).getPath());

        builder.put(httpConfigurationMO, new Functions.BinaryVoid<HttpConfigurationMO, RestResponse>() {
            @Override
            public void call(HttpConfigurationMO httpConfigurationMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        httpConfigurationMO.setHost(httpConfigurations.get(0).getHost() + "Updated");
        httpConfigurationMO.setUsername(httpConfigurations.get(0).getUsername());
        httpConfigurationMO.setPasswordId(httpConfigurations.get(0).getPasswordGoid().toString());
        httpConfigurationMO.setPort(httpConfigurations.get(0).getPort());
        httpConfigurationMO.setProtocol(HttpConfigurationMO.Protocol.HTTP);
        httpConfigurationMO.setPath(httpConfigurations.get(0).getPath());
        httpConfigurationMO.setProxyUse(HttpConfigurationMO.Option.CUSTOM);
        HttpConfigurationMO.HttpProxyConfiguration httpProxyConfiguration = new HttpConfigurationMO.HttpProxyConfiguration();
        httpProxyConfiguration.setPasswordId(new Goid(123,456).toString());
        httpProxyConfiguration.setHost("ProxyHost");
        httpProxyConfiguration.setPort(8888);
        httpProxyConfiguration.setUsername("ProxyUserName");
        httpConfigurationMO.setProxyConfiguration(httpProxyConfiguration);

        builder.put(httpConfigurationMO, new Functions.BinaryVoid<HttpConfigurationMO, RestResponse>() {
            @Override
            public void call(HttpConfigurationMO httpConfigurationMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });


        return builder.map();
    }

    @Override
    public Map<HttpConfigurationMO, Functions.BinaryVoid<HttpConfigurationMO, RestResponse>> getUnUpdateableManagedObjects() {
        CollectionUtils.MapBuilder<HttpConfigurationMO, Functions.BinaryVoid<HttpConfigurationMO, RestResponse>> builder = CollectionUtils.MapBuilder.builder();

        HttpConfigurationMO httpConfiguration = ManagedObjectFactory.createHttpConfiguration();
        httpConfiguration.setId(httpConfigurations.get(0).getId());

        builder.put(httpConfiguration, new Functions.BinaryVoid<HttpConfigurationMO, RestResponse>() {
            @Override
            public void call(HttpConfigurationMO httpConfigurationMO, RestResponse restResponse) {
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
        return Collections.emptyMap();
    }

    @Override
    public Map<String, Functions.BinaryVoid<String, RestResponse>> getBadListQueries() {
        return Collections.emptyMap();
    }

    @Override
    public List<String> getDeleteableManagedObjectIDs() {
        return Functions.map(httpConfigurations, new Functions.Unary<String, HttpConfiguration>() {
            @Override
            public String call(HttpConfiguration httpConfiguration) {
                return httpConfiguration.getId();
            }
        });
    }

    @Override
    public String getResourceUri() {
        return "httpConfigurations";
    }

    @Override
    public String getType() {
        return EntityType.HTTP_CONFIGURATION.name();
    }

    @Override
    public String getExpectedTitle(String id) throws FindException {
        HttpConfiguration entity = httpConfigurationManager.findByPrimaryKey(Goid.parseGoid(id));
        Assert.assertNotNull(entity);
        return entity.getHost();
    }

    @Override
    public void verifyLinks(String id, List<Link> links) throws FindException {
        HttpConfiguration entity = httpConfigurationManager.findByPrimaryKey(Goid.parseGoid(id));
        Assert.assertNotNull(entity);
    }

    @Override
    public void verifyEntity(String id, HttpConfigurationMO managedObject) throws FindException {
        HttpConfiguration entity = httpConfigurationManager.findByPrimaryKey(Goid.parseGoid(id));
        if (managedObject == null) {
            Assert.assertNull(entity);
        } else {
            Assert.assertNotNull(entity);

            Assert.assertEquals(entity.getId(), managedObject.getId());
            Assert.assertEquals(entity.getHost(), managedObject.getHost());
            Assert.assertEquals(entity.getUsername(), managedObject.getUsername());
            Assert.assertEquals(entity.getPasswordGoid() == null ? null : entity.getPasswordGoid().toString(), managedObject.getPasswordId());
            Assert.assertEquals(entity.getPort(), managedObject.getPort());
            Assert.assertEquals(entity.getConnectTimeout(), managedObject.getConnectTimeout());
            Assert.assertEquals(entity.isFollowRedirects(), managedObject.isFollowRedirects());
            Assert.assertEquals(entity.getNtlmDomain(), managedObject.getNtlmDomain());
            Assert.assertEquals(entity.getNtlmHost(), managedObject.getNtlmHost());
            Assert.assertEquals(entity.getPath(), managedObject.getPath());
            Assert.assertEquals(entity.getProtocol() == null ? null : entity.getProtocol().toString(), managedObject.getProtocol() == null ? null : managedObject.getProtocol().toString());
            Assert.assertEquals(entity.getReadTimeout(), managedObject.getReadTimeout());
            Assert.assertEquals(entity.getTlsCipherSuites(), managedObject.getTlsCipherSuites());
            Assert.assertEquals(entity.getTlsKeystoreAlias(), managedObject.getTlsKeystoreAlias());
            Assert.assertEquals(entity.getTlsKeystoreGoid() == null || Goid.equals(new Goid(0,0), entity.getTlsKeystoreGoid()) ? null : entity.getTlsKeystoreGoid().toString(), managedObject.getTlsKeystoreId());
            Assert.assertEquals(entity.getTlsVersion(), managedObject.getTlsVersion());
            Assert.assertEquals(entity.getProxyUse() == null ? null : entity.getProxyUse().toString(), managedObject.getProxyUse() == null ? null : managedObject.getProxyUse().toString());

            if (entity.getProxyConfiguration() == null || entity.getProxyUse() == null || HttpConfiguration.Option.DEFAULT.equals(entity.getProxyUse()) || HttpConfiguration.Option.NONE.equals(entity.getProxyUse())) {
                Assert.assertNull(managedObject.getProxyConfiguration());
            } else {
                Assert.assertNotNull(managedObject.getProxyConfiguration());

                HttpProxyConfiguration httpProxyConfiguration = entity.getProxyConfiguration();
                Assert.assertEquals(httpProxyConfiguration.getHost(), managedObject.getProxyConfiguration().getHost());
                Assert.assertEquals(httpProxyConfiguration.getPasswordGoid().toString(), managedObject.getProxyConfiguration().getPasswordId());
                Assert.assertEquals(httpProxyConfiguration.getPort(), managedObject.getProxyConfiguration().getPort());
                Assert.assertEquals(httpProxyConfiguration.getUsername(), managedObject.getProxyConfiguration().getUsername());
            }
        }
    }

    @Override
    public Map<String, List<String>> getListQueryAndExpectedResults() throws FindException {
        return CollectionUtils.MapBuilder.<String, List<String>>builder()
                .put("", Functions.map(httpConfigurations, new Functions.Unary<String, HttpConfiguration>() {
                    @Override
                    public String call(HttpConfiguration httpConfiguration) {
                        return httpConfiguration.getId();
                    }
                }))
                .put("host=" + URLEncoder.encode(httpConfigurations.get(0).getHost()), Arrays.asList(httpConfigurations.get(0).getId()))
                .put("host=" + URLEncoder.encode(httpConfigurations.get(0).getHost()) + "&host=" + URLEncoder.encode(httpConfigurations.get(1).getHost()), Functions.map(httpConfigurations.subList(0, 2), new Functions.Unary<String, HttpConfiguration>() {
                    @Override
                    public String call(HttpConfiguration httpConfiguration) {
                        return httpConfiguration.getId();
                    }
                }))
                .put("host=banName", Collections.<String>emptyList())
                .put("ntlmDomain=ntlmDomain", Arrays.asList(httpConfigurations.get(0).getId()))
                .put("ntlmDomain=ntlmDomain2", Arrays.asList(httpConfigurations.get(1).getId()))
                .put("protocol=HTTP", Arrays.asList(httpConfigurations.get(0).getId(), httpConfigurations.get(2).getId()))
                .put("host=" + URLEncoder.encode(httpConfigurations.get(0).getHost()) + "&host=" + URLEncoder.encode(httpConfigurations.get(1).getHost()) + "&sort=host&order=desc", Arrays.asList(httpConfigurations.get(1).getId(), httpConfigurations.get(0).getId()))
                .map();
    }
}
