package com.l7tech.skunkworks.rest.resourcetests;

import com.l7tech.gateway.api.IdentityProviderMO;
import com.l7tech.gateway.api.Link;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.identity.external.PolicyBackedIdentityProviderConfig;
import com.l7tech.identity.fed.FederatedIdentityProviderConfig;
import com.l7tech.identity.ldap.*;
import com.l7tech.objectmodel.*;
import com.l7tech.security.types.CertificateValidationType;
import com.l7tech.skunkworks.rest.tools.RestEntityTests;
import com.l7tech.skunkworks.rest.tools.RestResponse;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.IgnoreOnDaily;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import junit.framework.Assert;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Before;

import java.net.URLEncoder;
import java.util.*;

@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class IdentityProviderRestEntityResourceTest extends RestEntityTests<IdentityProviderConfig, IdentityProviderMO> {
    private IdentityProviderConfigManager identityProviderConfigManager;
    private List<IdentityProviderConfig> identityProviderConfigs = new ArrayList<>();
    private IdentityProviderConfig internalIdentityProvider;

    @Before
    public void before() throws SaveException, FindException {
        identityProviderConfigManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("identityProviderConfigManager", IdentityProviderConfigManager.class);
        //Create the active connectors

        BindOnlyLdapIdentityProviderConfig bindOnlyLdapIdentityProviderConfig = new BindOnlyLdapIdentityProviderConfig();
        bindOnlyLdapIdentityProviderConfig.setName("BindOnlyLdapIdentityProviderConfig 1");
        bindOnlyLdapIdentityProviderConfig.setBindPatternPrefix("bindPrefixPattern");
        bindOnlyLdapIdentityProviderConfig.setBindPatternSuffix("suffixPattern");
        bindOnlyLdapIdentityProviderConfig.setLdapUrl(new String[]{"LdapUrl1", "LdapUrl2"});
        bindOnlyLdapIdentityProviderConfig.setClientAuthEnabled(true);
        bindOnlyLdapIdentityProviderConfig.setKeyAlias("test");
        bindOnlyLdapIdentityProviderConfig.setKeystoreId(new Goid(111, 222));

        identityProviderConfigManager.save(bindOnlyLdapIdentityProviderConfig);
        identityProviderConfigs.add(bindOnlyLdapIdentityProviderConfig);

        FederatedIdentityProviderConfig federatedIdentityProviderConfig = new FederatedIdentityProviderConfig();
        federatedIdentityProviderConfig.setName("FederatedIdentityProviderConfig 2");
        federatedIdentityProviderConfig.setDescription("IDProvider 2 Description");
        federatedIdentityProviderConfig.setAdminEnabled(false);
        federatedIdentityProviderConfig.setCertificateValidationType(CertificateValidationType.PATH_VALIDATION);
        federatedIdentityProviderConfig.setSamlSupported(false);
        federatedIdentityProviderConfig.setX509Supported(true);

        identityProviderConfigManager.save(federatedIdentityProviderConfig);
        identityProviderConfigs.add(federatedIdentityProviderConfig);

        LdapIdentityProviderConfig ldapIdentityProviderConfig = new LdapIdentityProviderConfig();
        //providerConfiguration
        ldapIdentityProviderConfig.setName("LdapIdentityProviderConfig 3");
        ldapIdentityProviderConfig.setAdminEnabled(false);
        ldapIdentityProviderConfig.setBindDN("bind DN");
        ldapIdentityProviderConfig.setBindPasswd("password");
        ldapIdentityProviderConfig.setSearchBase("searchBase");
        ldapIdentityProviderConfig.setLdapUrl(new String[]{"LdapUrl1", "LdapUrl2"});
        ldapIdentityProviderConfig.setTemplateName("GenericLDAP");
        ldapIdentityProviderConfig.setClientAuthEnabled(true);
        ldapIdentityProviderConfig.setKeyAlias("test");
        ldapIdentityProviderConfig.setKeystoreId(new Goid(111, 222));
        //Group Object Classes
        GroupMappingConfig groupMappingConfig = new GroupMappingConfig();
        groupMappingConfig.setObjClass("groupOfUniqueNames");
        groupMappingConfig.setNameAttrName("cn");
        groupMappingConfig.setMemberAttrName("uniqueMember");
        groupMappingConfig.setMemberStrategy(MemberStrategy.MEMBERS_ARE_NVPAIR);
        ldapIdentityProviderConfig.setGroupMapping(groupMappingConfig);
        groupMappingConfig = new GroupMappingConfig();
        groupMappingConfig.setObjClass("posixGroup");
        groupMappingConfig.setNameAttrName("cn");
        groupMappingConfig.setMemberAttrName("memberUid");
        groupMappingConfig.setMemberStrategy(MemberStrategy.MEMBERS_ARE_LOGIN);
        ldapIdentityProviderConfig.setGroupMapping(groupMappingConfig);
        groupMappingConfig = new GroupMappingConfig();
        groupMappingConfig.setObjClass("organizationalUnit");
        groupMappingConfig.setNameAttrName("ou");
        groupMappingConfig.setMemberStrategy(MemberStrategy.MEMBERS_BY_OU);
        ldapIdentityProviderConfig.setGroupMapping(groupMappingConfig);
        //User Object Classes
        UserMappingConfig userMappingConfig = new UserMappingConfig();
        userMappingConfig.setObjClass("inetOrgPerson");
        userMappingConfig.setNameAttrName("cn");
        userMappingConfig.setLoginAttrName("uid");
        userMappingConfig.setPasswdAttrName("userPassword");
        userMappingConfig.setFirstNameAttrName("givenName");
        userMappingConfig.setLastNameAttrName("sn");
        userMappingConfig.setEmailNameAttrName("mail");
        userMappingConfig.setUserCertAttrName("userCertificate;binary");
        userMappingConfig.setKerberosAttrName("uid");
        ldapIdentityProviderConfig.setUserMapping(userMappingConfig);
        userMappingConfig = new UserMappingConfig();
        userMappingConfig.setObjClass("inetOrgPerson");
        userMappingConfig.setNameAttrName("cn");
        userMappingConfig.setLoginAttrName("uid");
        userMappingConfig.setPasswdAttrName("userPassword");
        userMappingConfig.setFirstNameAttrName("givenName");
        userMappingConfig.setLastNameAttrName("sn");
        userMappingConfig.setEmailNameAttrName("mail");
        userMappingConfig.setUserCertAttrName("userCertificate;binary");
        userMappingConfig.setKerberosAttrName("uid");
        userMappingConfig.setKerberosEnterpriseAttrName("enteruid");
        userMappingConfig.setPasswdType(PasswdStrategy.CLEAR);
        ldapIdentityProviderConfig.setUserMapping(userMappingConfig);
        //Advanced Configuration
        ldapIdentityProviderConfig.setGroupCacheSize(100);
        ldapIdentityProviderConfig.setGroupCacheMaxAge(1L);
        ldapIdentityProviderConfig.setGroupCacheMaxAgeUnit(com.l7tech.util.TimeUnit.MINUTES);
        ldapIdentityProviderConfig.setGroupMaxNesting(5);
        ldapIdentityProviderConfig.setGroupMembershipCaseInsensitive(false);
        ldapIdentityProviderConfig.setReturningAttributes(new String[]{"test", "test2"});
        //ntlm configuration
        TreeMap<String, String> ntlmProps = new TreeMap<String, String>();
        ntlmProps.put("enabled", "true");
        ntlmProps.put("server.dns.name", "serverNameValue");
        ntlmProps.put("service.account", "ServiceAccount");
        ntlmProps.put("service.passwordOid", new Goid(444, 555).toString());
        ntlmProps.put("domain.dns.name", "domainDNSNAme");
        ntlmProps.put("domain.netbios.name", "netbiosName");
        ntlmProps.put("host.dns.name", "hostDNSname");
        ntlmProps.put("host.netbios.name", "hostBios");
        ntlmProps.put("otherAdvancedProperty", "advancedPropValue");
        ldapIdentityProviderConfig.setNtlmAuthenticationProviderProperties(ntlmProps);
        //certificate settings
        ldapIdentityProviderConfig.setUserCertificateIndexSearchFilter("(userCertificate;binary=*)");
        ldapIdentityProviderConfig.setUserCertificateSKISearchFilter("certski");
        ldapIdentityProviderConfig.setUserCertificateIssuerSerialSearchFilter("certSerial");
        ldapIdentityProviderConfig.setUserCertificateUseType(LdapIdentityProviderConfig.UserCertificateUseType.INDEX_CUSTOM);
        ldapIdentityProviderConfig.setUserLookupByCertMode(LdapIdentityProviderConfig.UserLookupByCertMode.CERT);
        ldapIdentityProviderConfig.setCertificateValidationType(CertificateValidationType.PATH_VALIDATION);

        identityProviderConfigManager.save(ldapIdentityProviderConfig);
        identityProviderConfigs.add(ldapIdentityProviderConfig);

        PolicyBackedIdentityProviderConfig policyBackedIdentityProviderConfig = new PolicyBackedIdentityProviderConfig();
        policyBackedIdentityProviderConfig.setName("PolicyBackedIdentityProviderConfig 4");
        policyBackedIdentityProviderConfig.setDescription("IDProvider 4 Description");
        policyBackedIdentityProviderConfig.setAdminEnabled(false);
        policyBackedIdentityProviderConfig.setPolicyId(new Goid(123, 456));
        policyBackedIdentityProviderConfig.setDefaultRoleId(new Goid(666, 777));

        identityProviderConfigManager.save(policyBackedIdentityProviderConfig);
        identityProviderConfigs.add(policyBackedIdentityProviderConfig);

        internalIdentityProvider = identityProviderConfigManager.findByPrimaryKey(new Goid(0, -2));
    }

    @After
    public void after() throws FindException, DeleteException {
        Collection<IdentityProviderConfig> all = identityProviderConfigManager.findAll();
        for (IdentityProviderConfig identityProviderConfig : all) {
            if (IdentityProviderType.INTERNAL.toVal() != identityProviderConfig.getTypeVal()) {
                identityProviderConfigManager.delete(identityProviderConfig.getGoid());
            }
        }
    }

    @Override
    public List<String> getRetrievableEntityIDs() {
        List<IdentityProviderConfig> allIdPs = new ArrayList<>(identityProviderConfigs);
        allIdPs.add(internalIdentityProvider);
        return Functions.map(allIdPs, new Functions.Unary<String, IdentityProviderConfig>() {
            @Override
            public String call(IdentityProviderConfig identityProviderConfig) {
                return identityProviderConfig.getId();
            }
        });
    }

    @Override
    public List<IdentityProviderMO> getCreatableManagedObjects() {
        List<IdentityProviderMO> identityProviderMOs = new ArrayList<>();

        IdentityProviderMO identityProviderMO = ManagedObjectFactory.createIdentityProvider();
        identityProviderMO.setId(getGoid().toString());
        identityProviderMO.setName("My New ID Provider");
        identityProviderMO.setIdentityProviderType(IdentityProviderMO.IdentityProviderType.BIND_ONLY_LDAP);
        identityProviderMO.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("certificateValidation", "Validate Certificate Path")
                .map());
        IdentityProviderMO.BindOnlyLdapIdentityProviderDetail detailsBindOnly = identityProviderMO.getBindOnlyLdapIdentityProviderDetail();
        detailsBindOnly.setServerUrls(Arrays.asList("server1", "server2"));
        detailsBindOnly.setUseSslClientAuthentication(true);
        detailsBindOnly.setBindPatternPrefix("prefix Pattern");
        detailsBindOnly.setBindPatternSuffix("suffix Pattern");
        identityProviderMOs.add(identityProviderMO);

        identityProviderMO = ManagedObjectFactory.createIdentityProvider();
        identityProviderMO.setId(getGoid().toString());
        identityProviderMO.setName("My New ID Provider 2");
        identityProviderMO.setIdentityProviderType(IdentityProviderMO.IdentityProviderType.LDAP);
        identityProviderMO.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("certificateValidation", "Validate Certificate Path")
                .put("userCertificateUsage", "None")
                .put("groupCacheSize", 101)
                .put("groupCacheMaximumAge", 99L)
                .put("groupCacheMaximumAgeUnit", "days")
                .put("groupMaximumNesting", 23)
                .put("groupMembershipCaseInsensitive", true)
                .put("userLookupByCertMode", "Common Name from Certificate")
                .map());
        IdentityProviderMO.LdapIdentityProviderDetail detailsLdap = identityProviderMO.getLdapIdentityProviderDetail();
        detailsLdap.setServerUrls(Arrays.asList("server1", "server2"));
        detailsLdap.setUseSslClientAuthentication(true);
        detailsLdap.setSearchBase("Search Base");
        detailsLdap.setBindDn("BindDN");
        detailsLdap.setSourceType("GenericLDAP");
        IdentityProviderMO.LdapIdentityProviderMapping ldapIdentityProviderMapping = ManagedObjectFactory.createLdapIdentityProviderMapping();
        ldapIdentityProviderMapping.setObjectClass("MyObjectClass");
        ldapIdentityProviderMapping.setMappings(CollectionUtils.MapBuilder.<String, Object>builder().put("objClass", "MyObjectClass").map());
        ldapIdentityProviderMapping.setProperties(Collections.<String, Object>emptyMap());
        detailsLdap.setUserMappings(Arrays.asList(ldapIdentityProviderMapping));
        ldapIdentityProviderMapping = ManagedObjectFactory.createLdapIdentityProviderMapping();
        ldapIdentityProviderMapping.setObjectClass("MyObjectClass");
        ldapIdentityProviderMapping.setMappings(CollectionUtils.MapBuilder.<String, Object>builder().put("objClass", "MyObjectClass").map());
        ldapIdentityProviderMapping.setProperties(CollectionUtils.MapBuilder.<String, Object>builder().put("memberStrategy", "Member is User DN").map());
        detailsLdap.setGroupMappings(Arrays.asList(ldapIdentityProviderMapping));
        identityProviderMOs.add(identityProviderMO);

        identityProviderMO = ManagedObjectFactory.createIdentityProvider();
        identityProviderMO.setId(getGoid().toString());
        identityProviderMO.setName("My New ID Provider 3");
        identityProviderMO.setIdentityProviderType(IdentityProviderMO.IdentityProviderType.FEDERATED);
        identityProviderMO.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("certificateValidation", "Validate Certificate Path")
                .put("enableCredentialType.x509", true)
                .put("enableCredentialType.saml", true)
                .map());
        IdentityProviderMO.FederatedIdentityProviderDetail federatedDetails = identityProviderMO.getFederatedIdentityProviderDetail();
        federatedDetails.setCertificateReferences(Arrays.asList(new Goid(123, 456).toString()));
        identityProviderMOs.add(identityProviderMO);

        identityProviderMO = ManagedObjectFactory.createIdentityProvider();
        identityProviderMO.setId(getGoid().toString());
        identityProviderMO.setName("My New ID Provider 4");
        identityProviderMO.setIdentityProviderType(IdentityProviderMO.IdentityProviderType.POLICY_BACKED);
        identityProviderMO.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("adminEnabled", false)
                .map());
        IdentityProviderMO.PolicyBackedIdentityProviderDetail policyBackedIdentityProviderDetail = identityProviderMO.getPolicyBackedIdentityProviderDetail();
        policyBackedIdentityProviderDetail.setAuthenticationPolicyId(new Goid(1, 2).toString());
        policyBackedIdentityProviderDetail.setDefaultRoleAssignmentId(new Goid(555, 666).toString());
        identityProviderMOs.add(identityProviderMO);

        return identityProviderMOs;
    }

    @Override
    public List<IdentityProviderMO> getUpdateableManagedObjects() {
        List<IdentityProviderMO> identityProviderMOs = new ArrayList<>();

        BindOnlyLdapIdentityProviderConfig identityProviderConfig = (BindOnlyLdapIdentityProviderConfig)this.identityProviderConfigs.get(0);
        IdentityProviderMO identityProviderMO = ManagedObjectFactory.createIdentityProvider();
        identityProviderMO.setId(identityProviderConfig.getId());
        identityProviderMO.setName(identityProviderConfig.getName() + " Updated");
        identityProviderMO.setIdentityProviderType(IdentityProviderMO.IdentityProviderType.BIND_ONLY_LDAP);
        identityProviderMO.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("certificateValidation", "Validate Certificate Path")
                .map());
        IdentityProviderMO.BindOnlyLdapIdentityProviderDetail detailsBindOnly = identityProviderMO.getBindOnlyLdapIdentityProviderDetail();
        detailsBindOnly.setServerUrls(Arrays.asList(identityProviderConfig.getLdapUrl()));
        detailsBindOnly.setUseSslClientAuthentication(identityProviderConfig.canIssueCertificates());
        detailsBindOnly.setBindPatternPrefix(identityProviderConfig.getBindPatternPrefix());
        detailsBindOnly.setBindPatternSuffix(identityProviderConfig.getBindPatternSuffix());
        identityProviderMOs.add(identityProviderMO);

        //update twice
        identityProviderMO = ManagedObjectFactory.createIdentityProvider();
        identityProviderMO.setId(identityProviderConfig.getId());
        identityProviderMO.setName(identityProviderConfig.getName() + " Updated");
        identityProviderMO.setIdentityProviderType(IdentityProviderMO.IdentityProviderType.BIND_ONLY_LDAP);
        identityProviderMO.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("certificateValidation", "Validate Certificate Path")
                .map());
        detailsBindOnly = identityProviderMO.getBindOnlyLdapIdentityProviderDetail();
        detailsBindOnly.setServerUrls(Arrays.asList(identityProviderConfig.getLdapUrl()));
        detailsBindOnly.setUseSslClientAuthentication(identityProviderConfig.canIssueCertificates());
        detailsBindOnly.setBindPatternPrefix(identityProviderConfig.getBindPatternPrefix());
        detailsBindOnly.setBindPatternSuffix(identityProviderConfig.getBindPatternSuffix());
        identityProviderMOs.add(identityProviderMO);

        PolicyBackedIdentityProviderConfig policyBackedIdentityProviderConfig = (PolicyBackedIdentityProviderConfig)this.identityProviderConfigs.get(3);
        identityProviderMO = ManagedObjectFactory.createIdentityProvider();
        identityProviderMO.setId(policyBackedIdentityProviderConfig.getId());
        identityProviderMO.setName(policyBackedIdentityProviderConfig.getName() + " Updated");
        identityProviderMO.setIdentityProviderType(IdentityProviderMO.IdentityProviderType.POLICY_BACKED);
        identityProviderMO.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("adminEnabled", !policyBackedIdentityProviderConfig.isAdminEnabled())
                .map());
        IdentityProviderMO.PolicyBackedIdentityProviderDetail policyBackedIdentityProviderDetail = identityProviderMO.getPolicyBackedIdentityProviderDetail();
        policyBackedIdentityProviderDetail.setAuthenticationPolicyId(new Goid(789, 123).toString());
        policyBackedIdentityProviderDetail.setDefaultRoleAssignmentId(new Goid(555, 666).toString());
        identityProviderMOs.add(identityProviderMO);

        return identityProviderMOs;
    }

    @Override
    public Map<IdentityProviderMO, Functions.BinaryVoid<IdentityProviderMO, RestResponse>> getUnCreatableManagedObjects() {
        CollectionUtils.MapBuilder<IdentityProviderMO, Functions.BinaryVoid<IdentityProviderMO, RestResponse>> builder = CollectionUtils.MapBuilder.builder();

        IdentityProviderMO identityProviderMO = ManagedObjectFactory.createIdentityProvider();
        identityProviderMO.setName("My Internal Identity Provider New");
        identityProviderMO.setIdentityProviderType(IdentityProviderMO.IdentityProviderType.INTERNAL);
        identityProviderMO.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .map());

        builder.put(identityProviderMO, new Functions.BinaryVoid<IdentityProviderMO, RestResponse>() {
            @Override
            public void call(IdentityProviderMO IdentityProviderMO, RestResponse restResponse) {
                Assert.assertEquals(403, restResponse.getStatus());
            }
        });

        identityProviderMO = ManagedObjectFactory.createIdentityProvider();
        identityProviderMO.setName("My Bad PolicyBacked Id provider");
        identityProviderMO.setIdentityProviderType(IdentityProviderMO.IdentityProviderType.POLICY_BACKED);
        identityProviderMO.getPolicyBackedIdentityProviderDetail().setAuthenticationPolicyId(null);
        identityProviderMO.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("adminEnabled", false)
                .map());

        builder.put(identityProviderMO, new Functions.BinaryVoid<IdentityProviderMO, RestResponse>() {
            @Override
            public void call(IdentityProviderMO IdentityProviderMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        return builder.map();
    }

    @Override
    public Map<IdentityProviderMO, Functions.BinaryVoid<IdentityProviderMO, RestResponse>> getUnUpdateableManagedObjects() {
        CollectionUtils.MapBuilder<IdentityProviderMO, Functions.BinaryVoid<IdentityProviderMO, RestResponse>> builder = CollectionUtils.MapBuilder.builder();

        IdentityProviderMO identityProviderMO = ManagedObjectFactory.createIdentityProvider();
        identityProviderMO.setId(identityProviderConfigs.get(0).getId());
        identityProviderMO.setName(identityProviderConfigs.get(1).getName());

        builder.put(identityProviderMO, new Functions.BinaryVoid<IdentityProviderMO, RestResponse>() {
            @Override
            public void call(IdentityProviderMO IdentityProviderMO, RestResponse restResponse) {
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

        builder.put(new Goid(0, -2).toString(), new Functions.BinaryVoid<String, RestResponse>() {
            @Override
            public void call(String id, RestResponse restResponse) {
                Assert.assertEquals(403, restResponse.getStatus());
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
        return Functions.map(identityProviderConfigs, new Functions.Unary<String, IdentityProviderConfig>() {
            @Override
            public String call(IdentityProviderConfig identityProviderConfig) {
                return identityProviderConfig.getId();
            }
        });
    }

    @Override
    public String getResourceUri() {
        return "identityProviders";
    }

    @Override
    public String getType() {
        return EntityType.ID_PROVIDER_CONFIG.name();
    }

    @Override
    public String getExpectedTitle(String id) throws FindException {
        IdentityProviderConfig entity = identityProviderConfigManager.findByPrimaryKey(Goid.parseGoid(id));
        Assert.assertNotNull(entity);
        return entity.getName();
    }

    @Override
    public void verifyLinks(String id, List<Link> links) throws FindException {
        IdentityProviderConfig entity = identityProviderConfigManager.findByPrimaryKey(Goid.parseGoid(id));
        Assert.assertNotNull(entity);
    }

    @Override
    public void verifyEntity(String id, IdentityProviderMO managedObject) throws FindException {
        IdentityProviderConfig entity = identityProviderConfigManager.findByPrimaryKey(Goid.parseGoid(id));
        if (managedObject == null) {
            Assert.assertNull(entity);
        } else {
            Assert.assertNotNull(entity);

            Assert.assertEquals(entity.getId(), managedObject.getId());
            Assert.assertEquals(entity.getName(), managedObject.getName());

            switch (entity.getTypeVal()) {
                case 1:
                    //Internal Identity Provider.
                    Assert.assertEquals(IdentityProviderMO.IdentityProviderType.INTERNAL, managedObject.getIdentityProviderType());
                    break;
                case 2:
                    //LDAP Identity Provider.
                    Assert.assertEquals(IdentityProviderMO.IdentityProviderType.LDAP, managedObject.getIdentityProviderType());
                    LdapIdentityProviderConfig ldapIdentityProviderConfig = (LdapIdentityProviderConfig) entity;
                    org.junit.Assert.assertArrayEquals(ldapIdentityProviderConfig.getLdapUrl(), managedObject.getLdapIdentityProviderDetail().getServerUrls().toArray());
                    Assert.assertEquals(ldapIdentityProviderConfig.isClientAuthEnabled(), managedObject.getLdapIdentityProviderDetail().isUseSslClientClientAuthentication());
                    if(ldapIdentityProviderConfig.getKeystoreId() != null) {
                        Assert.assertEquals(ldapIdentityProviderConfig.getKeystoreId().toString() + ":" + ldapIdentityProviderConfig.getKeyAlias(), managedObject.getLdapIdentityProviderDetail().getSslKeyId());
                    }
                    Assert.assertEquals(ldapIdentityProviderConfig.getBindDN(), managedObject.getLdapIdentityProviderDetail().getBindDn());
                    checkPassword(ldapIdentityProviderConfig.getBindPasswd(), managedObject.getLdapIdentityProviderDetail().getBindPassword());
                    if(ldapIdentityProviderConfig.isAdminEnabled()) {
                        Assert.assertTrue((Boolean) managedObject.getProperties().get("adminEnabled"));
                    } else {
                        Assert.assertTrue(managedObject.getProperties().get("adminEnabled") == null || !Boolean.valueOf( managedObject.getProperties().get("adminEnabled").toString() ) );
                    }
                    verifyMappings(ldapIdentityProviderConfig.getGroupMappings(), managedObject.getLdapIdentityProviderDetail().getGroupMappings());
                    verifyMappings(ldapIdentityProviderConfig.getUserMappings(), managedObject.getLdapIdentityProviderDetail().getUserMappings());
                    Assert.assertEquals(ldapIdentityProviderConfig.getGroupCacheSize(), managedObject.getProperties().get("groupCacheSize"));
                    Assert.assertEquals(ldapIdentityProviderConfig.getGroupCacheMaxAge(), managedObject.getProperties().get("groupCacheMaximumAge"));
                    Assert.assertEquals(ldapIdentityProviderConfig.getGroupCacheMaxAgeUnit().getName(), managedObject.getProperties().get("groupCacheMaximumAgeUnit"));
                    Assert.assertEquals(ldapIdentityProviderConfig.getGroupMaxNesting(), managedObject.getProperties().get("groupMaximumNesting"));
                    Assert.assertEquals(ldapIdentityProviderConfig.isGroupMembershipCaseInsensitive(), managedObject.getProperties().get("groupMembershipCaseInsensitive"));
                    org.junit.Assert.assertArrayEquals(ldapIdentityProviderConfig.getReturningAttributes(), managedObject.getLdapIdentityProviderDetail().getSpecifiedAttributes().toArray());
                    if (ldapIdentityProviderConfig.getNtlmAuthenticationProviderProperties() != null && ldapIdentityProviderConfig.getNtlmAuthenticationProviderProperties().size() > 0) {
                        Assert.assertNotNull(managedObject.getLdapIdentityProviderDetail().getNtlmProperties());
                        Assert.assertEquals(ldapIdentityProviderConfig.getNtlmAuthenticationProviderProperties().size(), managedObject.getLdapIdentityProviderDetail().getNtlmProperties().size());
                        for (String ntlmKey : ldapIdentityProviderConfig.getNtlmAuthenticationProviderProperties().keySet()) {
                            Assert.assertEquals(ldapIdentityProviderConfig.getNtlmAuthenticationProviderProperties().get(ntlmKey), managedObject.getLdapIdentityProviderDetail().getNtlmProperties().get(ntlmKey));
                        }
                    }
                    verifyUserCertificateUseType(ldapIdentityProviderConfig.getUserCertificateUseType(), (String) managedObject.getProperties().get("userCertificateUsage"));
                    Assert.assertEquals(ldapIdentityProviderConfig.getUserCertificateIndexSearchFilter(), managedObject.getProperties().get("userCertificateIndexSearchFilter"));
                    Assert.assertEquals(ldapIdentityProviderConfig.getUserCertificateIssuerSerialSearchFilter(), managedObject.getProperties().get("userCertificateIssuerSerialSearchFilter"));
                    Assert.assertEquals(ldapIdentityProviderConfig.getUserCertificateSKISearchFilter(), managedObject.getProperties().get("userCertificateSKISearchFilter"));
                    verifyUserLookupByCertMode(ldapIdentityProviderConfig.getUserLookupByCertMode(), (String) managedObject.getProperties().get("userLookupByCertMode"));

                    validateCertificateType(managedObject.getProperties().get("certificateValidation").toString(), entity.getCertificateValidationType());
                    break;
                case 3:
                    //Federated Identity Provider.
                    Assert.assertEquals(IdentityProviderMO.IdentityProviderType.FEDERATED, managedObject.getIdentityProviderType());
                    FederatedIdentityProviderConfig federatedIdentityProviderConfig = (FederatedIdentityProviderConfig) entity;
                    Assert.assertEquals(federatedIdentityProviderConfig.isX509Supported(), managedObject.getProperties().get("enableCredentialType.x509"));
                    Assert.assertEquals(federatedIdentityProviderConfig.isSamlSupported(), managedObject.getProperties().get("enableCredentialType.saml"));
                    org.junit.Assert.assertArrayEquals(Functions.map(Arrays.asList(federatedIdentityProviderConfig.getTrustedCertGoids()), new Functions.Unary<String, Goid>() {
                        @Override
                        public String call(Goid goid) {
                            return goid.toString();
                        }
                    }).toArray(), managedObject.getFederatedIdentityProviderDetail().getCertificateReferences().toArray());
                    validateCertificateType(managedObject.getProperties().get("certificateValidation").toString(), federatedIdentityProviderConfig.getCertificateValidationType());
                    break;
                case 4:
                    //Bind Only LDAP Identity Provider.
                    Assert.assertEquals(IdentityProviderMO.IdentityProviderType.BIND_ONLY_LDAP, managedObject.getIdentityProviderType());
                    BindOnlyLdapIdentityProviderConfig bindOnlyLdapIdentityProviderConfig = (BindOnlyLdapIdentityProviderConfig) entity;
                    Assert.assertEquals(bindOnlyLdapIdentityProviderConfig.getBindPatternPrefix(), managedObject.getBindOnlyLdapIdentityProviderDetail().getBindPatternPrefix());
                    Assert.assertEquals(bindOnlyLdapIdentityProviderConfig.getBindPatternSuffix(), managedObject.getBindOnlyLdapIdentityProviderDetail().getBindPatternSuffix());
                    org.junit.Assert.assertArrayEquals(bindOnlyLdapIdentityProviderConfig.getLdapUrl(), managedObject.getBindOnlyLdapIdentityProviderDetail().getServerUrls().toArray());
                    Assert.assertEquals(bindOnlyLdapIdentityProviderConfig.isClientAuthEnabled(), managedObject.getBindOnlyLdapIdentityProviderDetail().isUseSslClientClientAuthentication());
                    if(bindOnlyLdapIdentityProviderConfig.getKeystoreId() != null) {
                        Assert.assertEquals(bindOnlyLdapIdentityProviderConfig.getKeystoreId().toString() + ":" + bindOnlyLdapIdentityProviderConfig.getKeyAlias(), managedObject.getBindOnlyLdapIdentityProviderDetail().getSslKeyId());
                    }
                    break;
                case 5:
                    //Policy Backed Identity Provider.
                    Assert.assertEquals(IdentityProviderMO.IdentityProviderType.POLICY_BACKED, managedObject.getIdentityProviderType());
                    PolicyBackedIdentityProviderConfig policyBackedIdentityProviderConfig = (PolicyBackedIdentityProviderConfig) entity;
                    Assert.assertEquals(policyBackedIdentityProviderConfig.isAdminEnabled(), managedObject.getProperties().get("adminEnabled"));
                    Assert.assertEquals(policyBackedIdentityProviderConfig.getPolicyId().toString(), managedObject.getPolicyBackedIdentityProviderDetail().getAuthenticationPolicyId());
                    Assert.assertEquals(policyBackedIdentityProviderConfig.getDefaultRoleId().toString(), managedObject.getPolicyBackedIdentityProviderDetail().getDefaultRoleAssignmentId());
                    break;
                default:
                    Assert.fail("Unknown entity type: " + entity.getTypeVal());
            }
        }
    }

    private void verifyUserLookupByCertMode(LdapIdentityProviderConfig.UserLookupByCertMode userLookupByCertMode, String userLookupByCertModeString) {
        switch (userLookupByCertMode) {
            case LOGIN:
                Assert.assertEquals("Common Name from Certificate", userLookupByCertModeString);
                break;
            case CERT:
                Assert.assertEquals("Entire Certificate", userLookupByCertModeString);
                break;
            default:
                Assert.fail("Unknown UserCertificateUseType" + userLookupByCertMode);
        }
    }

    private void verifyUserCertificateUseType(LdapIdentityProviderConfig.UserCertificateUseType userCertificateUseType, String userCertificateUsage) {
        switch (userCertificateUseType) {
            case NONE:
                Assert.assertEquals("None", userCertificateUsage);
                break;
            case INDEX:
                Assert.assertEquals("Index", userCertificateUsage);
                break;
            case INDEX_CUSTOM:
                Assert.assertEquals("Custom Index", userCertificateUsage);
                break;
            case SEARCH:
                Assert.assertEquals("Search", userCertificateUsage);
                break;
            default:
                Assert.fail("Unknown UserCertificateUseType" + userCertificateUseType);
        }
    }

    private void verifyMappings(UserMappingConfig[] userMappings, List<IdentityProviderMO.LdapIdentityProviderMapping> mappings) {
        Assert.assertEquals(userMappings.length, mappings.size());
        for (int i = 0; i < userMappings.length; i++) {
            UserMappingConfig userMappingConfig = userMappings[i];
            IdentityProviderMO.LdapIdentityProviderMapping ldapIdentityProviderMapping = mappings.get(i);
            Assert.assertEquals(userMappingConfig.getObjClass(), ldapIdentityProviderMapping.getObjectClass());
            Assert.assertEquals(userMappingConfig.getNameAttrName(), ldapIdentityProviderMapping.getMappings().get("nameAttrName"));
            Assert.assertEquals(userMappingConfig.getLoginAttrName(), ldapIdentityProviderMapping.getMappings().get("loginAttrName"));
            Assert.assertEquals(userMappingConfig.getPasswdAttrName(), ldapIdentityProviderMapping.getMappings().get("passwdAttrName"));
            Assert.assertEquals(userMappingConfig.getFirstNameAttrName(), ldapIdentityProviderMapping.getMappings().get("firstNameAttrName"));
            Assert.assertEquals(userMappingConfig.getLastNameAttrName(), ldapIdentityProviderMapping.getMappings().get("lastNameAttrName"));
            Assert.assertEquals(userMappingConfig.getEmailNameAttrName(), ldapIdentityProviderMapping.getMappings().get("emailNameAttrName"));
            Assert.assertEquals(userMappingConfig.getUserCertAttrName(), ldapIdentityProviderMapping.getMappings().get("userCertAttrName"));
            Assert.assertEquals(userMappingConfig.getKerberosAttrName(), ldapIdentityProviderMapping.getMappings().get("kerberosAttrName"));
            Assert.assertEquals(userMappingConfig.getKerberosEnterpriseAttrName(), ldapIdentityProviderMapping.getMappings().get("kerberosEnterpriseAttrName"));
        }
    }

    /**
     * This checks password. They will not be returned by the api.
     */
    private void checkPassword(@Nullable final String expectedPassword, @Nullable final String password) {
        if (password == null)
            return;
        Assert.assertEquals(expectedPassword, password);
    }

    private void verifyMappings(GroupMappingConfig[] groupMappings, List<IdentityProviderMO.LdapIdentityProviderMapping> mappings) {
        Assert.assertEquals(groupMappings.length, mappings.size());
        for (int i = 0; i < groupMappings.length; i++) {
            GroupMappingConfig groupMappingConfig = groupMappings[i];
            IdentityProviderMO.LdapIdentityProviderMapping ldapIdentityProviderMapping = mappings.get(i);
            Assert.assertEquals(groupMappingConfig.getObjClass(), ldapIdentityProviderMapping.getObjectClass());
            Assert.assertEquals(groupMappingConfig.getNameAttrName(), ldapIdentityProviderMapping.getMappings().get("nameAttrName"));
            Assert.assertEquals(groupMappingConfig.getMemberAttrName(), ldapIdentityProviderMapping.getMappings().get("memberAttrName"));
            verifyMemberStrategy(groupMappingConfig.getMemberStrategy(), ldapIdentityProviderMapping.getProperties().get("memberStrategy").toString());
        }
    }

    private void verifyMemberStrategy(final MemberStrategy memberStrategy, String strategy) {
        if (MemberStrategy.MEMBERS_ARE_DN.equals(memberStrategy)) {
            Assert.assertEquals("Member is User DN", strategy);
        } else if (MemberStrategy.MEMBERS_ARE_LOGIN.equals(memberStrategy)) {
            Assert.assertEquals("Member is User Login", strategy);
        } else if (MemberStrategy.MEMBERS_ARE_NVPAIR.equals(memberStrategy)) {
            Assert.assertEquals("Member is NV Pair", strategy);
        } else if (MemberStrategy.MEMBERS_BY_OU.equals(memberStrategy)) {
            Assert.assertEquals("OU Group", strategy);
        } else {
            Assert.fail("Unknown member strategy: " + memberStrategy.getVal());
        }
    }

    private void validateCertificateType(String certificateType, CertificateValidationType certificateValidationType) {
        switch (certificateValidationType) {
            case REVOCATION:
                Assert.assertEquals("Revocation Checking", certificateType);
                break;
            case CERTIFICATE_ONLY:
                Assert.assertEquals("Validate", certificateType);
                break;
            case PATH_VALIDATION:
                Assert.assertEquals("Validate Certificate Path", certificateType);
                break;
        }
    }

    @Override
    public Map<String, List<String>> getListQueryAndExpectedResults() throws FindException {
        List<IdentityProviderConfig> allIdPs = new ArrayList<>();
        allIdPs.add(internalIdentityProvider);
        allIdPs.addAll(identityProviderConfigs);
        return CollectionUtils.MapBuilder.<String, List<String>>builder()
                .put("", Functions.map(allIdPs, new Functions.Unary<String, IdentityProviderConfig>() {
                    @Override
                    public String call(IdentityProviderConfig identityProviderConfig) {
                        return identityProviderConfig.getId();
                    }
                }))
                .put("name=" + URLEncoder.encode(identityProviderConfigs.get(0).getName()), Arrays.asList(identityProviderConfigs.get(0).getId()))
                .put("name=" + URLEncoder.encode(identityProviderConfigs.get(0).getName()) + "&name=" + URLEncoder.encode(identityProviderConfigs.get(1).getName()), Functions.map(identityProviderConfigs.subList(0, 2), new Functions.Unary<String, IdentityProviderConfig>() {
                    @Override
                    public String call(IdentityProviderConfig identityProviderConfig) {
                        return identityProviderConfig.getId();
                    }
                }))
                .put("name=banName", Collections.<String>emptyList())
                .put("type=LDAP", Arrays.asList(identityProviderConfigs.get(2).getId()))
                .put("type=Internal", Arrays.asList(internalIdentityProvider.getId()))
                .put("type=Federated", Arrays.asList(identityProviderConfigs.get(1).getId()))
                .put("type=Simple%20LDAP", Arrays.asList(identityProviderConfigs.get(0).getId()))
                .put("type=Policy-Backed", Arrays.asList(identityProviderConfigs.get(3).getId()))
                .put("name=" + URLEncoder.encode(identityProviderConfigs.get(0).getName()) + "&name=" + URLEncoder.encode(identityProviderConfigs.get(1).getName()) + "&sort=name&order=desc", Arrays.asList(identityProviderConfigs.get(1).getId(), identityProviderConfigs.get(0).getId()))
                .map();
    }
}
