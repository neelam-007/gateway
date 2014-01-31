package com.l7tech.skunkworks.rest;

import com.l7tech.gateway.api.*;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.identity.GroupManager;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.UserManager;
import com.l7tech.identity.internal.InternalGroup;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.skunkworks.rest.tools.DependencyTestBase;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.IgnoreOnDaily;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import org.junit.*;

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

    @Before
    public void before() throws Exception {
        super.before();

        securePasswordManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("securePasswordManager", SecurePasswordManager.class);
        identityProviderConfigManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("identityProviderConfigManager", IdentityProviderConfigManager.class);
        identityProviderFactory = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("identityProviderFactory", IdentityProviderFactory.class);

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
    }

    @BeforeClass
    public static void beforeClass() throws PolicyAssertionException, IllegalAccessException, InstantiationException {
        DependencyTestBase.beforeClass();
    }

    @After
    public void after() throws Exception {
        super.after();

        securePasswordManager.delete(securePassword);
        identityProviderConfigManager.delete(ldap);
        identityProviderConfigManager.delete(ldapNtlmPassword);
        internalGroupManager.delete(internalGroup.getId());
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

        TestPolicyDependency(assXml, new Functions.UnaryVoid<Item<DependencyTreeMO>>(){

            @Override
            public void call(Item<DependencyTreeMO> dependencyItem) {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyTreeMO dependencyAnalysisMO = dependencyItem.getContent();
                                
                assertEquals(1,dependencyAnalysisMO.getDependencies().size());
                DependencyMO dep  = dependencyAnalysisMO.getDependencies().get(0);
                assertEquals(internalProviderId, dep.getDependentObject().getId());
                assertEquals("Internal Identity Provider", dep.getDependentObject().getName());
                assertEquals(EntityType.ID_PROVIDER_CONFIG.toString(), dep.getDependentObject().getType());
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

        TestPolicyDependency(assXml, new Functions.UnaryVoid<Item<DependencyTreeMO>>(){

            @Override
            public void call(Item<DependencyTreeMO> dependencyItem) {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyTreeMO dependencyAnalysisMO = dependencyItem.getContent();
                                
                assertEquals(2,dependencyAnalysisMO.getDependencies().size());
                DependencyMO idDep  = getDependency(dependencyAnalysisMO.getDependencies(), EntityType.ID_PROVIDER_CONFIG);
                assertEquals(internalProviderId, idDep.getDependentObject().getId());
                assertEquals("Internal Identity Provider", idDep.getDependentObject().getName());
                assertEquals(EntityType.ID_PROVIDER_CONFIG.toString(), idDep.getDependentObject().getType());

                // verify admin user dependency
                DependencyMO adminDep  = getDependency(dependencyAnalysisMO.getDependencies(), EntityType.USER);
                assertEquals(new Goid(0,3).toString(), adminDep.getDependentObject().getId());
                assertEquals("admin", adminDep.getDependentObject().getName());
                assertEquals(EntityType.USER.toString(), adminDep.getDependentObject().getType());
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

        TestPolicyDependency(assXml, new Functions.UnaryVoid<Item<DependencyTreeMO>>(){

            @Override
            public void call(Item<DependencyTreeMO> dependencyItem) {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyTreeMO dependencyAnalysisMO = dependencyItem.getContent();
                                
                assertEquals(2,dependencyAnalysisMO.getDependencies().size());
                DependencyMO idDep  = getDependency(dependencyAnalysisMO.getDependencies(), EntityType.ID_PROVIDER_CONFIG);
                assertEquals(internalProviderId, idDep.getDependentObject().getId());
                assertEquals("Internal Identity Provider", idDep.getDependentObject().getName());
                assertEquals(EntityType.ID_PROVIDER_CONFIG.toString(), idDep.getDependentObject().getType());

                // verify admin user dependency
                DependencyMO adminDep  = getDependency(dependencyAnalysisMO.getDependencies(), EntityType.GROUP);
                assertEquals(internalGroup.getId(), adminDep.getDependentObject().getId());
                assertEquals(internalGroup.getName(), adminDep.getDependentObject().getName());
                assertEquals(EntityType.GROUP.toString(), adminDep.getDependentObject().getType());
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

        TestPolicyDependency(assXml, new Functions.UnaryVoid<Item<DependencyTreeMO>>(){

            @Override
            public void call(Item<DependencyTreeMO> dependencyItem) {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyTreeMO dependencyAnalysisMO = dependencyItem.getContent();
                                
                assertEquals(1,dependencyAnalysisMO.getDependencies().size());
                DependencyMO dep  = dependencyAnalysisMO.getDependencies().get(0);
                assertEquals(ldap.getId(), dep.getDependentObject().getId());
                assertEquals(ldap.getName(), dep.getDependentObject().getName());
                assertEquals(EntityType.ID_PROVIDER_CONFIG.toString(), dep.getDependentObject().getType());

                // verify password dependency
                assertEquals(1,dep.getDependencies().size());
                DependencyMO passwordDep  = dep.getDependencies().get(0);
                assertEquals(securePassword.getId(), passwordDep.getDependentObject().getId());
                assertEquals(securePassword.getName(), passwordDep.getDependentObject().getName());
                assertEquals(EntityType.SECURE_PASSWORD.toString(), passwordDep.getDependentObject().getType());
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

        TestPolicyDependency(assXml, new Functions.UnaryVoid<Item<DependencyTreeMO>>(){

            @Override
            public void call(Item<DependencyTreeMO> dependencyItem) {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyTreeMO dependencyAnalysisMO = dependencyItem.getContent();
                                
                assertEquals(1,dependencyAnalysisMO.getDependencies().size());
                DependencyMO dep  = dependencyAnalysisMO.getDependencies().get(0);
                assertEquals(ldapNtlmPassword.getId(), dep.getDependentObject().getId());
                assertEquals(ldapNtlmPassword.getName(), dep.getDependentObject().getName());
                assertEquals(EntityType.ID_PROVIDER_CONFIG.toString(), dep.getDependentObject().getType());

                // verify password dependency
                assertNotNull("Password dependency not found", dep.getDependencies());
                assertEquals(1,dep.getDependencies().size());
                DependencyMO passwordDep  = dep.getDependencies().get(0);
                assertEquals(securePassword.getId(), passwordDep.getDependentObject().getId());
                assertEquals(securePassword.getName(), passwordDep.getDependentObject().getName());
                assertEquals(EntityType.SECURE_PASSWORD.toString(), passwordDep.getDependentObject().getType());
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

        TestPolicyDependency(assXml, new Functions.UnaryVoid<Item<DependencyTreeMO>>(){

            @Override
            public void call(Item<DependencyTreeMO> dependencyItem) {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyTreeMO dependencyAnalysisMO = dependencyItem.getContent();
                                
                assertEquals(1,dependencyAnalysisMO.getDependencies().size());
                DependencyMO dep  = dependencyAnalysisMO.getDependencies().get(0);
                assertEquals(internalProviderId, dep.getDependentObject().getId());
                assertEquals("Internal Identity Provider", dep.getDependentObject().getName());
                assertEquals(EntityType.ID_PROVIDER_CONFIG.toString(), dep.getDependentObject().getType());
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

        TestPolicyDependency(assXml, new Functions.UnaryVoid<Item<DependencyTreeMO>>(){

            @Override
            public void call(Item<DependencyTreeMO> dependencyItem) {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyTreeMO dependencyAnalysisMO = dependencyItem.getContent();
                                
                assertEquals(1,dependencyAnalysisMO.getDependencies().size());
                DependencyMO dep  = dependencyAnalysisMO.getDependencies().get(0);
                assertEquals(ldap.getId(), dep.getDependentObject().getId());
                assertEquals(ldap.getName(), dep.getDependentObject().getName());
                assertEquals(EntityType.ID_PROVIDER_CONFIG.toString(), dep.getDependentObject().getType());

                // verify password dependency
                assertEquals(1,dep.getDependencies().size());
                DependencyMO passwordDep  = dep.getDependencies().get(0);
                assertEquals(securePassword.getId(), passwordDep.getDependentObject().getId());
                assertEquals(securePassword.getName(), passwordDep.getDependentObject().getName());
                assertEquals(EntityType.SECURE_PASSWORD.toString(), passwordDep.getDependentObject().getType());
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

        TestPolicyDependency(assXml, new Functions.UnaryVoid<Item<DependencyTreeMO>>(){

            @Override
            public void call(Item<DependencyTreeMO> dependencyItem) {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyTreeMO dependencyAnalysisMO = dependencyItem.getContent();
                                
                assertEquals(1,dependencyAnalysisMO.getDependencies().size());
                DependencyMO dep  = dependencyAnalysisMO.getDependencies().get(0);
                assertEquals(ldapNtlmPassword.getId(), dep.getDependentObject().getId());
                assertEquals(ldapNtlmPassword.getName(), dep.getDependentObject().getName());
                assertEquals(EntityType.ID_PROVIDER_CONFIG.toString(), dep.getDependentObject().getType());

                // verify password dependency
                assertNotNull("Password dependency not found", dep.getDependencies());
                assertEquals(1,dep.getDependencies().size());
                DependencyMO passwordDep  = dep.getDependencies().get(0);
                assertEquals(securePassword.getId(), passwordDep.getDependentObject().getId());
                assertEquals(securePassword.getName(), passwordDep.getDependentObject().getName());
                assertEquals(EntityType.SECURE_PASSWORD.toString(), passwordDep.getDependentObject().getType());
            }
        });
    }

}
