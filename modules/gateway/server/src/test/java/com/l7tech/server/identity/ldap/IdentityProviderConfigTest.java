package com.l7tech.server.identity.ldap;

import com.l7tech.identity.ldap.*;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

/**
 * A test to test loading of the ldap templates using {@link com.l7tech.util.SafeXMLDecoder}.
 */
public class IdentityProviderConfigTest {

    @Test
    public void loadTemplates(){
        try {
            LdapConfigTemplateManager ldapConfigTemplateManager = new LdapConfigTemplateManager();
            for(String template : ldapConfigTemplateManager.getTemplateNames()){
                ldapConfigTemplateManager.getTemplate(template);
            }
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void loadGenericLdap(){
        try {
            LdapConfigTemplateManager ldapConfigTemplateManager = new LdapConfigTemplateManager();
            LdapIdentityProviderConfig generic = ldapConfigTemplateManager.getTemplate("GenericLDAP");
            Assert.assertEquals("search_base", generic.getSearchBase());
            UserMappingConfig userMappingConfig = generic.getUserMapping("inetorgperson");
            Assert.assertEquals("userCertificate;binary", userMappingConfig.getUserCertAttrName());
            Assert.assertTrue(PasswdStrategy.class.getName().equals(userMappingConfig.getPasswdType().getClass().getName()));

            GroupMappingConfig groupMappingConfig = generic.getGroupMapping("posixgroup");
            Assert.assertTrue(MemberStrategy.class.getName().equals(groupMappingConfig.getMemberStrategy().getClass().getName()));
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void loadMicrosoftActiveDirectory(){
        try {
            LdapConfigTemplateManager ldapConfigTemplateManager = new LdapConfigTemplateManager();
            LdapIdentityProviderConfig ms = ldapConfigTemplateManager.getTemplate("MicrosoftActiveDirectory");
            GroupMappingConfig groupMappingConfig = ms.getGroupMapping("group");
            Assert.assertEquals(0, groupMappingConfig.getMemberStrategy().getVal());

        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void loadOracle(){
        try {
            LdapConfigTemplateManager ldapConfigTemplateManager = new LdapConfigTemplateManager();
            LdapIdentityProviderConfig oracle = ldapConfigTemplateManager.getTemplate("Oracle");
            GroupMappingConfig groupMappingConfig = oracle.getGroupMapping("orclgroup");
            Assert.assertEquals("uniqueMember", groupMappingConfig.getMemberAttrName());

        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void loadTivoliLdap(){
        try {
            LdapConfigTemplateManager ldapConfigTemplateManager = new LdapConfigTemplateManager();
            LdapIdentityProviderConfig tivoli = ldapConfigTemplateManager.getTemplate("TivoliLDAP");
            GroupMappingConfig groupMappingConfig = tivoli.getGroupMapping("groupofuniquenames");
            Assert.assertEquals(2, groupMappingConfig.getMemberStrategy().getVal());

        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }
}
