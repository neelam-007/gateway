package com.l7tech.skunkworks.rest.dependencytests;

import com.l7tech.gateway.api.DependencyListMO;
import com.l7tech.gateway.api.DependencyMO;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.identity.GroupManager;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.UserManager;
import com.l7tech.identity.external.PolicyBackedIdentityProviderConfig;
import com.l7tech.identity.internal.InternalGroup;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.skunkworks.rest.tools.DependencyTestBase;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.IgnoreOnDaily;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.UUID;
import java.util.logging.Logger;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

/**
*
*/
@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class DependencyIdentityTest extends DependencyTestBase {
    private static final Logger logger = Logger.getLogger(DependencyIdentityTest.class.getName());

    private LdapIdentityProviderConfig ldap;
    private LdapIdentityProviderConfig ldapNtlmPassword;
    private final InternalGroup internalGroup =  new InternalGroup("Test Group");
    private final SecurePassword securePassword =  new SecurePassword();
    private SecurePasswordManager securePasswordManager;
    private IdentityProviderConfigManager identityProviderConfigManager;
    private GroupManager internalGroupManager;
    private UserManager internalUserManager;
    private IdentityProviderFactory identityProviderFactory;
    private final String internalProviderId = IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID.toString();
    private PolicyBackedIdentityProviderConfig policyBackedIdentityProviderConfig;
    private RoleManager roleManager;
    private Policy policyBackedIdentityProviderPolicy;
    private Role defaultPolicyBackedIdentityProviderRole;

    @Before
    public void before() throws Exception {
        super.before();

        securePasswordManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("securePasswordManager", SecurePasswordManager.class);
        identityProviderConfigManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("identityProviderConfigManager", IdentityProviderConfigManager.class);
        identityProviderFactory = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("identityProviderFactory", IdentityProviderFactory.class);
        policyManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("policyManager", PolicyManager.class);
        roleManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("roleManager", RoleManager.class);

        //create a policy for policyBack id provider.
        policyBackedIdentityProviderPolicy = new Policy(PolicyType.IDENTITY_PROVIDER_POLICY, "PolicyBackedIdProviderPolicy", "", false);
        policyBackedIdentityProviderPolicy.setGuid(UUID.randomUUID().toString());
        policyManager.save(policyBackedIdentityProviderPolicy);

        //create role
        defaultPolicyBackedIdentityProviderRole = new Role();
        defaultPolicyBackedIdentityProviderRole.setName("IdProviderDefaultRole");
        roleManager.save(defaultPolicyBackedIdentityProviderRole);

        //create secure password
        securePassword.setName("MyPassword");
        securePassword.setEncodedPassword("password");
        securePassword.setUsageFromVariable(true);
        securePassword.setType(SecurePassword.SecurePasswordType.PASSWORD);
        securePasswordManager.save(securePassword);

        //create ldap provider
        ldap = LdapIdentityProviderConfig.newLdapIdentityProviderConfig();
        ldap.setName("Test Ldap");
        ldap.setUserLookupByCertMode(LdapIdentityProviderConfig.UserLookupByCertMode.LOGIN);
        ldap.setUserCertificateUseType(LdapIdentityProviderConfig.UserCertificateUseType.NONE);
        ldap.setLdapUrl(new String[]{"ldap://smldap.l7tech.com:389"});
        ldap.setClientAuthEnabled(false);
        ldap.setSearchBase("dc=l7tech,dc=com");
        ldap.setBindDN("dc=l7tech,dc=com");
        ldap.setBindPasswd("${secpass.MyPassword.plaintext}");
        identityProviderConfigManager.save(ldap);


        ldapNtlmPassword = new LdapIdentityProviderConfig(ldap);
        ldapNtlmPassword.setGoid(PersistentEntity.DEFAULT_GOID);
        ldapNtlmPassword.setName("Test Ldap NTML");
        ldapNtlmPassword.setUserLookupByCertMode(LdapIdentityProviderConfig.UserLookupByCertMode.LOGIN);
        ldapNtlmPassword.setUserCertificateUseType(LdapIdentityProviderConfig.UserCertificateUseType.NONE);
        ldapNtlmPassword.setLdapUrl(new String[]{"ldap://smldap.l7tech.com:389"});
        ldapNtlmPassword.setClientAuthEnabled(false);
        ldapNtlmPassword.setSearchBase("dc=l7tech,dc=com");
        ldap.setBindDN("dc=l7tech,dc=com");
        ldap.setBindPasswd("blah");
        ldapNtlmPassword.setNtlmAuthenticationProviderProperties((CollectionUtils.<String, String>mapBuilder()
                .put("service.account", "blah")
                .put("service.passwordOid", securePassword.getId())
                .put("enabled", "true")
                .put("host.netbios.name", "blah")
                .put("domain.dns.name", "blah")
                .put("server.dns.name", "blah")
                .put("domain.netbios.name", "blah")
                .put("host.dns.name", "blah").map()));
        identityProviderConfigManager.save(ldapNtlmPassword);

        // add internal group
        internalGroupManager = identityProviderFactory.getProvider(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID).getGroupManager();
        internalUserManager = identityProviderFactory.getProvider(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID).getUserManager();
        internalGroupManager.saveGroup(internalGroup);
        internalGroupManager.addUser(internalUserManager.findByLogin("admin"),internalGroup);

        policyBackedIdentityProviderConfig = new PolicyBackedIdentityProviderConfig();
        policyBackedIdentityProviderConfig.setName("PolicyBackedIdentityProvider");
        policyBackedIdentityProviderConfig.setDefaultRoleId(defaultPolicyBackedIdentityProviderRole.getGoid());
        policyBackedIdentityProviderConfig.setPolicyId(policyBackedIdentityProviderPolicy.getGoid());

        identityProviderConfigManager.save(policyBackedIdentityProviderConfig);
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        DependencyTestBase.beforeClass();
    }

    @After
    public void after() throws Exception {
        super.after();

        securePasswordManager.delete(securePassword);
        identityProviderConfigManager.delete(ldap);
        identityProviderConfigManager.delete(ldapNtlmPassword);
        identityProviderConfigManager.delete(policyBackedIdentityProviderConfig);
        internalGroupManager.delete(internalGroup.getId());
        roleManager.delete(defaultPolicyBackedIdentityProviderRole);
        policyManager.delete(policyBackedIdentityProviderPolicy);
    }

    @Test
     public void IdentityAssertionTest() throws Exception {

        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "        <L7p:Authentication>\n" +
                        "            <L7p:IdentityProviderOid goidValue=\""+ IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID+"\"/>\n" +
                        "        </L7p:Authentication>\n" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>";

        TestPolicyDependency(assXml, new Functions.UnaryVoid<Item<DependencyListMO>>(){

            @Override
            public void call(Item<DependencyListMO> dependencyItem) {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyListMO dependencyAnalysisMO = dependencyItem.getContent();

                assertEquals(1,dependencyAnalysisMO.getDependencies().size());
                DependencyMO dep  = dependencyAnalysisMO.getDependencies().get(0);
                assertEquals(internalProviderId, dep.getId());
                assertEquals("Internal Identity Provider", dep.getName());
                assertEquals(EntityType.ID_PROVIDER_CONFIG.toString(), dep.getType());
            }
        });
    }


    @Test
    public void SpecificUserAssertionTest() throws Exception {

        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:SpecificUser>\n" +
                "            <L7p:IdentityProviderOid goidValue=\""+internalProviderId+"\"/>\n" +
                "            <L7p:Target target=\"RESPONSE\"/>\n" +
                "            <L7p:UserLogin stringValue=\"admin\"/>\n" +
                "            <L7p:UserName stringValue=\"admin\"/>\n" +
                "            <L7p:UserUid stringValue=\""+new Goid(0,3).toString()+"\"/>\n" +
                "        </L7p:SpecificUser>" +
                "    </wsp:All>\n" +
                "</wsp:Policy>";

        TestPolicyDependency(assXml, new Functions.UnaryVoid<Item<DependencyListMO>>(){

            @Override
            public void call(Item<DependencyListMO> dependencyItem) {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyListMO dependencyAnalysisMO = dependencyItem.getContent();

                assertEquals(2,dependencyAnalysisMO.getDependencies().size());
                DependencyMO idDep  = getDependency(dependencyAnalysisMO, EntityType.ID_PROVIDER_CONFIG);
                assertEquals(internalProviderId, idDep.getId());
                assertEquals("Internal Identity Provider", idDep.getName());
                assertEquals(EntityType.ID_PROVIDER_CONFIG.toString(), idDep.getType());

                // verify admin user dependency
                DependencyMO adminDep  = getDependency(dependencyAnalysisMO, EntityType.USER);
                assertEquals(new Goid(0,3).toString(), adminDep.getId());
                assertEquals("admin", adminDep.getName());
                assertEquals(EntityType.USER.toString(), adminDep.getType());

                // verify policy dependency
                DependencyMO policyDep  = dependencyAnalysisMO.getSearchObjectItem();
                assertNotNull("Missing dependency:" + new Goid(0,3).toString(), getDependency(policyDep.getDependencies(), new Goid(0,3).toString()));
                assertNotNull("Missing dependency:" + internalProviderId, getDependency(policyDep.getDependencies(), internalProviderId));
            }
        });
    }

    @Test
    public void SpecificGroupAssertionTest() throws Exception {

        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "        <L7p:MemberOfGroup>\n" +
                        "            <L7p:GroupId stringValue=\""+internalGroup.getId()+"\"/>\n" +
                        "            <L7p:GroupName stringValue=\""+internalGroup.getName()+"\"/>\n" +
                        "            <L7p:IdentityProviderOid goidValue=\""+internalProviderId+"\"/>\n" +
                        "            <L7p:Target target=\"RESPONSE\"/>\n" +
                        "        </L7p:MemberOfGroup>" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>";

        TestPolicyDependency(assXml, new Functions.UnaryVoid<Item<DependencyListMO>>(){

            @Override
            public void call(Item<DependencyListMO> dependencyItem) {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyListMO dependencyAnalysisMO = dependencyItem.getContent();

                assertEquals(2,dependencyAnalysisMO.getDependencies().size());
                DependencyMO idDep  = getDependency(dependencyAnalysisMO, EntityType.ID_PROVIDER_CONFIG);
                assertNotNull(idDep);
                assertEquals(internalProviderId, idDep.getId());
                assertEquals("Internal Identity Provider", idDep.getName());
                assertEquals(EntityType.ID_PROVIDER_CONFIG.toString(), idDep.getType());

                // verify admin user dependency
                DependencyMO adminDep = getDependency(dependencyAnalysisMO, EntityType.GROUP);
                assertNotNull(adminDep);
                assertEquals(internalGroup.getId(), adminDep.getId());
                assertEquals(internalGroup.getName(), adminDep.getName());
                assertEquals(EntityType.GROUP.toString(), adminDep.getType());

                // verify policy dependency
                DependencyMO policyDep  = dependencyAnalysisMO.getSearchObjectItem();
                assertNotNull("Missing dependency:" + internalGroup.getId(), getDependency(policyDep.getDependencies(), internalGroup.getId()));
                assertNotNull("Missing dependency:" + internalProviderId, getDependency(policyDep.getDependencies(), internalProviderId));
            }
        });
    }

    @Test
    public void BuildRstrSoapResponseAssertionGroupTest() throws Exception {

        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:BuildRstrSoapResponse>\n" +
                "            <L7p:IdentityTarget IdentityTarget=\"included\">\n" +
                "                <L7p:IdentityId stringValue=\""+internalGroup.getId()+"\"/>\n" +
                "                <L7p:IdentityInfo stringValue=\""+ internalGroup.getName()+"\"/>\n" +
                "                <L7p:IdentityProviderOid goidValue=\""+ internalGroup.getProviderId()+"\"/>\n" +
                "                <L7p:TargetIdentityType identityType=\"GROUP\"/>\n" +
                "            </L7p:IdentityTarget>\n" +
                "            <L7p:ResponseForIssuance booleanValue=\"false\"/>\n" +
                "        </L7p:BuildRstrSoapResponse>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>";

        TestPolicyDependency(assXml, new Functions.UnaryVoid<Item<DependencyListMO>>(){

            @Override
            public void call(Item<DependencyListMO> dependencyItem) {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyListMO dependencyAnalysisMO = dependencyItem.getContent();

                assertEquals(2,dependencyAnalysisMO.getDependencies().size());
                DependencyMO idDep  = getDependency(dependencyAnalysisMO, EntityType.ID_PROVIDER_CONFIG);
                assertNotNull(idDep);
                assertEquals(internalProviderId, idDep.getId());
                assertEquals("Internal Identity Provider", idDep.getName());
                assertEquals(EntityType.ID_PROVIDER_CONFIG.toString(), idDep.getType());

                // verify admin user dependency
                DependencyMO adminDep = getDependency(dependencyAnalysisMO, EntityType.GROUP);
                assertNotNull(adminDep);
                assertEquals(internalGroup.getId(), adminDep.getId());
                assertEquals(internalGroup.getName(), adminDep.getName());
                assertEquals(EntityType.GROUP.toString(), adminDep.getType());

                // verify policy dependency
                DependencyMO policyDep  = dependencyAnalysisMO.getSearchObjectItem();
                assertNotNull("Missing dependency:" + internalGroup.getId(), getDependency(policyDep.getDependencies(), internalGroup.getId()));
                assertNotNull("Missing dependency:" + internalProviderId, getDependency(policyDep.getDependencies(), internalProviderId));
            }
        });
    }

    @Test
    public void BuildRstrSoapResponseAssertionUserTest() throws Exception {
        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "        <L7p:BuildRstrSoapResponse>\n" +
                        "            <L7p:IdentityTarget IdentityTarget=\"included\">\n" +
                        "                <L7p:IdentityId stringValue=\""+new Goid(0,3).toString()+"\"/>\n" +
                        "                <L7p:IdentityInfo stringValue=\"admin\"/>\n" +
                        "                <L7p:IdentityProviderOid goidValue=\""+ internalProviderId+"\"/>\n" +
                        "                <L7p:TargetIdentityType identityType=\"USER\"/>\n" +
                        "            </L7p:IdentityTarget>\n" +
                        "            <L7p:ResponseForIssuance booleanValue=\"false\"/>\n" +
                        "        </L7p:BuildRstrSoapResponse>\n" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>";

        TestPolicyDependency(assXml, new Functions.UnaryVoid<Item<DependencyListMO>>(){

            @Override
            public void call(Item<DependencyListMO> dependencyItem) {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyListMO dependencyAnalysisMO = dependencyItem.getContent();

                assertEquals(2,dependencyAnalysisMO.getDependencies().size());
                DependencyMO idDep  = getDependency(dependencyAnalysisMO, EntityType.ID_PROVIDER_CONFIG);
                assertEquals(internalProviderId, idDep.getId());
                assertEquals("Internal Identity Provider", idDep.getName());
                assertEquals(EntityType.ID_PROVIDER_CONFIG.toString(), idDep.getType());

                // verify admin user dependency
                DependencyMO adminDep  = getDependency(dependencyAnalysisMO, EntityType.USER);
                assertEquals(new Goid(0,3).toString(), adminDep.getId());
                assertEquals("admin", adminDep.getName());
                assertEquals(EntityType.USER.toString(), adminDep.getType());

                // verify policy dependency
                DependencyMO policyDep  = dependencyAnalysisMO.getSearchObjectItem();
                assertNotNull("Missing dependency:" + new Goid(0,3).toString(), getDependency(policyDep.getDependencies(), new Goid(0,3).toString()));
                assertNotNull("Missing dependency:" + internalProviderId, getDependency(policyDep.getDependencies(), internalProviderId));
            }
        });
    }

    @Test
    public void BuildRstrSoapResponseAssertionNullIdentityTest() throws Exception {
        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "        <L7p:BuildRstrSoapResponse>\n" +
                        "            <L7p:ResponseForIssuance booleanValue=\"false\"/>\n" +
                        "        </L7p:BuildRstrSoapResponse>\n" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>";

        TestPolicyDependency(assXml, new Functions.UnaryVoid<Item<DependencyListMO>>(){

            @Override
            public void call(Item<DependencyListMO> dependencyItem) {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyListMO dependencyAnalysisMO = dependencyItem.getContent();

                assertEquals(0,dependencyAnalysisMO.getDependencies().size());
            }
        });
    }

    @Test
    public void ldapIdentityAssertionTest() throws Exception {

        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "        <L7p:Authentication>\n" +
                        "            <L7p:IdentityProviderOid goidValue=\""+ ldap.getId() +"\"/>\n" +
                        "        </L7p:Authentication>\n" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>";

        TestPolicyDependency(assXml, new Functions.UnaryVoid<Item<DependencyListMO>>(){

            @Override
            public void call(Item<DependencyListMO> dependencyItem) {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyListMO dependencyAnalysisMO = dependencyItem.getContent();

                assertEquals(2,dependencyAnalysisMO.getDependencies().size());
                DependencyMO dep  = getDependency(dependencyAnalysisMO,EntityType.ID_PROVIDER_CONFIG);
                assertNotNull(dep);
                assertEquals(ldap.getId(), dep.getId());
                assertEquals(ldap.getName(), dep.getName());
                assertEquals(EntityType.ID_PROVIDER_CONFIG.toString(), dep.getType());
                assertNotNull("Missing dependency:" + securePassword.getId(), getDependency(dep.getDependencies(), securePassword.getId()));

                // verify password dependency
                DependencyMO passwordDep  = getDependency(dependencyAnalysisMO, EntityType.SECURE_PASSWORD);
                assertNotNull(passwordDep);
                assertEquals(securePassword.getId(), passwordDep.getId());
                assertEquals(securePassword.getName(), passwordDep.getName());
                assertEquals(EntityType.SECURE_PASSWORD.toString(), passwordDep.getType());
            }
        });
    }

    @Test
    public void ldapNtlmPasswordIdentityAssertionTest() throws Exception {

        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "        <L7p:Authentication>\n" +
                        "            <L7p:IdentityProviderOid goidValue=\""+ ldapNtlmPassword.getId() +"\"/>\n" +
                        "        </L7p:Authentication>\n" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>";

        TestPolicyDependency(assXml, new Functions.UnaryVoid<Item<DependencyListMO>>(){

            @Override
            public void call(Item<DependencyListMO> dependencyItem) {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyListMO dependencyAnalysisMO = dependencyItem.getContent();

                assertEquals(2,dependencyAnalysisMO.getDependencies().size());
                DependencyMO dep  = getDependency(dependencyAnalysisMO, EntityType.ID_PROVIDER_CONFIG);
                assertNotNull(dep);
                assertEquals(ldapNtlmPassword.getId(), dep.getId());
                assertEquals(ldapNtlmPassword.getName(), dep.getName());
                assertEquals(EntityType.ID_PROVIDER_CONFIG.toString(), dep.getType());
                assertNotNull("Missing dependency:" + securePassword.getId(), getDependency(dep.getDependencies(), securePassword.getId()));

                // verify password dependency
                DependencyMO passwordDep  = getDependency(dependencyAnalysisMO, EntityType.SECURE_PASSWORD);
                assertNotNull(passwordDep);
                assertEquals(securePassword.getId(), passwordDep.getId());
                assertEquals(securePassword.getName(), passwordDep.getName());
                assertEquals(EntityType.SECURE_PASSWORD.toString(), passwordDep.getType());
            }
        });
    }

    @Test
    public void identityAttributesAssertionTest() throws Exception {

        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:IdentityAttributes>\n" +
                "            <L7p:IdentityProviderOid goidValue=\""+internalProviderId+"\"/>\n" +
                "            <L7p:LookupAttributes identityMappingArray=\"included\">\n" +
                "                <L7p:item internalAttributeMapping=\"included\">\n" +
                "                    <L7p:AttributeConfig attributeConfig=\"included\">\n" +
                "                        <L7p:Header header=\"included\">\n" +
                "                            <L7p:Name stringValue=\"User Login\"/>\n" +
                "                            <L7p:Type variableDataType=\"string\"/>\n" +
                "                            <L7p:UsersOrGroups usersOrGroups=\"USERS\"/>\n" +
                "                            <L7p:VariableName stringValue=\"login\"/>\n" +
                "                        </L7p:Header>\n" +
                "                        <L7p:VariableName stringValue=\"login\"/>\n" +
                "                    </L7p:AttributeConfig>\n" +
                "                    <L7p:ProviderOid goidValue=\""+internalProviderId+"\"/>\n" +
                "                </L7p:item>\n" +
                "                <L7p:item internalAttributeMapping=\"included\">\n" +
                "                    <L7p:AttributeConfig attributeConfig=\"included\">\n" +
                "                        <L7p:Header header=\"included\">\n" +
                "                            <L7p:Name stringValue=\"Identity Provider GOID\"/>\n" +
                "                            <L7p:Type variableDataType=\"int\"/>\n" +
                "                            <L7p:UsersOrGroups usersOrGroups=\"BOTH\"/>\n" +
                "                            <L7p:VariableName stringValue=\"providerId\"/>\n" +
                "                        </L7p:Header>\n" +
                "                        <L7p:VariableName stringValue=\"providerId\"/>\n" +
                "                    </L7p:AttributeConfig>\n" +
                "                    <L7p:ProviderOid goidValue=\""+internalProviderId+"\"/>\n" +
                "                </L7p:item>\n" +
                "            </L7p:LookupAttributes>\n" +
                "        </L7p:IdentityAttributes>" +
                "    </wsp:All>\n" +
                "</wsp:Policy>";

        TestPolicyDependency(assXml, new Functions.UnaryVoid<Item<DependencyListMO>>(){

            @Override
            public void call(Item<DependencyListMO> dependencyItem) {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyListMO dependencyAnalysisMO = dependencyItem.getContent();

                assertEquals(1,dependencyAnalysisMO.getDependencies().size());
                DependencyMO dep  = dependencyAnalysisMO.getDependencies().get(0);
                assertEquals(internalProviderId, dep.getId());
                assertEquals("Internal Identity Provider", dep.getName());
                assertEquals(EntityType.ID_PROVIDER_CONFIG.toString(), dep.getType());
            }
        });
    }

    @Test
    public void ldapQueryAssertionTest() throws Exception {

        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:LDAPQuery>\n" +
                "            <L7p:AttrNames stringArrayValue=\"included\">\n" +
                "                <L7p:item stringValue=\"blah\"/>\n" +
                "            </L7p:AttrNames>\n" +
                "            <L7p:CacheSize intValue=\"100\"/>\n" +
                "            <L7p:LdapProviderOid goidValue=\""+ldap.getId()+"\"/>\n" +
                "            <L7p:QueryMappings queryAttributeMappings=\"included\">\n" +
                "                <L7p:item mapping=\"included\">\n" +
                "                    <L7p:AttributeName stringValue=\"blah\"/>\n" +
                "                    <L7p:JoinMultivalued booleanValue=\"false\"/>\n" +
                "                    <L7p:MatchingContextVariableName stringValue=\"blah\"/>\n" +
                "                </L7p:item>\n" +
                "            </L7p:QueryMappings>\n" +
                "            <L7p:SearchFilter stringValue=\"blah\"/>\n" +
                "            <L7p:SearchFilterInjectionProtected booleanValue=\"true\"/>\n" +
                "        </L7p:LDAPQuery>" +
                "    </wsp:All>\n" +
                "</wsp:Policy>";

        TestPolicyDependency(assXml, new Functions.UnaryVoid<Item<DependencyListMO>>(){

            @Override
            public void call(Item<DependencyListMO> dependencyItem) {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyListMO dependencyAnalysisMO = dependencyItem.getContent();

                assertEquals(2,dependencyAnalysisMO.getDependencies().size());
                DependencyMO dep  = getDependency(dependencyAnalysisMO, EntityType.ID_PROVIDER_CONFIG);
                assertNotNull(dep);
                assertEquals(ldap.getId(), dep.getId());
                assertEquals(ldap.getName(), dep.getName());
                assertEquals(EntityType.ID_PROVIDER_CONFIG.toString(), dep.getType());
                assertEquals(1,dep.getDependencies().size());
                assertEquals(securePassword.getId(),dep.getDependencies().get(0).getId());

                // verify password dependency
                DependencyMO passwordDep  = getDependency(dependencyAnalysisMO, EntityType.SECURE_PASSWORD);
                assertNotNull(passwordDep);
                assertEquals(securePassword.getId(), passwordDep.getId());
                assertEquals(securePassword.getName(), passwordDep.getName());
                assertEquals(EntityType.SECURE_PASSWORD.toString(), passwordDep.getType());
            }
        });
    }

    @Test
    public void requireNTLMAuthCredsAssertionTest() throws Exception {

        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:NtlmAuthentication>\n" +
                "            <L7p:LdapProviderName stringValue=\""+ldapNtlmPassword.getName()+"\"/>\n" +
                "            <L7p:LdapProviderOid goidValue=\""+ldapNtlmPassword.getId()+"\"/>\n" +
                "        </L7p:NtlmAuthentication>" +
                "    </wsp:All>\n" +
                "</wsp:Policy>";

        TestPolicyDependency(assXml, new Functions.UnaryVoid<Item<DependencyListMO>>(){

            @Override
            public void call(Item<DependencyListMO> dependencyItem) {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyListMO dependencyAnalysisMO = dependencyItem.getContent();

                assertEquals(2,dependencyAnalysisMO.getDependencies().size());
                DependencyMO dep  = getDependency(dependencyAnalysisMO, EntityType.ID_PROVIDER_CONFIG);
                assertNotNull(dep);
                assertEquals(ldapNtlmPassword.getId(), dep.getId());
                assertEquals(ldapNtlmPassword.getName(), dep.getName());
                assertEquals(EntityType.ID_PROVIDER_CONFIG.toString(), dep.getType());
                assertNotNull("Missing dependency:" + securePassword.getId(), getDependency(dep.getDependencies(), securePassword.getId()));

                // verify password dependency
                DependencyMO passwordDep  = getDependency(dependencyAnalysisMO, EntityType.SECURE_PASSWORD);
                assertNotNull(passwordDep);
                assertEquals(securePassword.getId(), passwordDep.getId());
                assertEquals(securePassword.getName(), passwordDep.getName());
                assertEquals(EntityType.SECURE_PASSWORD.toString(), passwordDep.getType());
            }
        });
    }

    @Test
    public void IdentityAssertionPolicyBackedTest() throws Exception {

        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "        <L7p:Authentication>\n" +
                        "            <L7p:IdentityProviderOid goidValue=\""+ policyBackedIdentityProviderConfig.getId()+"\"/>\n" +
                        "        </L7p:Authentication>\n" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>";

        TestPolicyDependency(assXml, new Functions.UnaryVoid<Item<DependencyListMO>>(){

            @Override
            public void call(Item<DependencyListMO> dependencyItem) {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyListMO dependencyAnalysisMO = dependencyItem.getContent();

                DependencyMO dep  = dependencyAnalysisMO.getDependencies().get(0);
                //policy or role may come first
                if(EntityType.POLICY.toString().equals(dep.getType())){
                    assertEquals(policyBackedIdentityProviderConfig.getPolicyId().toString(), dep.getId());
                    dep  = dependencyAnalysisMO.getDependencies().get(1);
                    assertEquals(EntityType.RBAC_ROLE.toString(), dep.getType());
                    assertEquals(policyBackedIdentityProviderConfig.getDefaultRoleId().toString(), dep.getId());
                } else {
                    assertEquals(EntityType.RBAC_ROLE.toString(), dep.getType());
                    assertEquals(policyBackedIdentityProviderConfig.getDefaultRoleId().toString(), dep.getId());
                    dep  = dependencyAnalysisMO.getDependencies().get(1);
                    assertEquals(EntityType.POLICY.toString(), dep.getType());
                    assertEquals(policyBackedIdentityProviderConfig.getPolicyId().toString(), dep.getId());
                }

                dep  = dependencyAnalysisMO.getDependencies().get(2);
                assertEquals(EntityType.ID_PROVIDER_CONFIG.toString(), dep.getType());
                assertEquals(policyBackedIdentityProviderConfig.getId(), dep.getId());
                assertEquals(policyBackedIdentityProviderConfig.getName(), dep.getName());
                if(policyBackedIdentityProviderConfig.getDefaultRoleId().toString().equals(dep.getDependencies().get(0).getId())){
                    assertEquals(policyBackedIdentityProviderConfig.getPolicyId().toString(), dep.getDependencies().get(1).getId());
                } else {
                    assertEquals(policyBackedIdentityProviderConfig.getPolicyId().toString(), dep.getDependencies().get(0).getId());
                    assertEquals(policyBackedIdentityProviderConfig.getDefaultRoleId().toString(), dep.getDependencies().get(1).getId());
                }
            }
        });
    }
}
