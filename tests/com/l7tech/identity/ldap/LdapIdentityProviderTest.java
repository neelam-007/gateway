package com.l7tech.identity.ldap;

import com.l7tech.identity.GroupManager;
import com.l7tech.identity.User;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.server.identity.ldap.LdapConfigTemplateManager;
import com.l7tech.server.identity.ldap.LdapIdentityProvider;
import junit.framework.TestCase;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/**
 * A test class for the ldap redesign.
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jan 21, 2004<br/>
 * $Id$<br/>
 *
 */
public class LdapIdentityProviderTest extends TestCase {

    private LdapIdentityProviderConfig getConfigForSpock() throws IOException {
        LdapConfigTemplateManager templateManager = new LdapConfigTemplateManager();
        LdapIdentityProviderConfig spockTemplate = templateManager.getTemplate("GenericLDAP");
        spockTemplate.setLdapUrl(new String[] {"ldap://localhost:3899"});
        //spockTemplate.setLdapUrl(new String[] {"ldap://spock:389"});
        spockTemplate.setSearchBase("dc=layer7-tech,dc=com");
        return spockTemplate;
    }

    private LdapIdentityProviderConfig getConfigForSpockWithBadSearchBase() throws IOException {
        LdapConfigTemplateManager templateManager = new LdapConfigTemplateManager();
        LdapIdentityProviderConfig spockTemplate = templateManager.getTemplate("GenericLDAP");
        spockTemplate.setLdapUrl(new String[] {"ldap://spock:3899"});
        spockTemplate.setSearchBase("dc=layer8-tech,dc=com");
        return spockTemplate;
    }

    private LdapIdentityProviderConfig getConfigForMSAD() throws IOException {
        LdapConfigTemplateManager templateManager = new LdapConfigTemplateManager();
        LdapIdentityProviderConfig msadTemplate = templateManager.getTemplate("MicrosoftActiveDirectory");
        //msadTemplate.setLdapUrl("ldap://localhost:3899");
        msadTemplate.setLdapUrl(new String[] {"ldap://mail.l7tech.com:3268"});
        msadTemplate.setSearchBase("ou=Layer 7 Users,dc=L7TECH,dc=LOCAL");
        msadTemplate.setBindDN("browse");
        msadTemplate.setBindPasswd("password");
        return msadTemplate;
    }

    private LdapIdentityProviderConfig getConfigForTimTam() throws IOException {
        LdapConfigTemplateManager templateManager = new LdapConfigTemplateManager();
        LdapIdentityProviderConfig msadTemplate = templateManager.getTemplate("TivoliLdap");
        msadTemplate.setLdapUrl(new String[] {"ldap://192.168.1.120:389"});
        msadTemplate.setSearchBase("DC=IBM,DC=COM");
        msadTemplate.setBindDN("cn=root");
        msadTemplate.setBindPasswd("passw0rd");
        return msadTemplate;
    }

    private LdapIdentityProviderConfig getConfigForOracle() throws IOException {
        LdapConfigTemplateManager templateManager = new LdapConfigTemplateManager();
        LdapIdentityProviderConfig orclTemplate = templateManager.getTemplate("Oracle");
        //orclTemplate.setLdapUrl("ldap://localhost:3899");
        orclTemplate.setLdapUrl(new String[] {"ldap://hugh.l7tech.com:389"});
        orclTemplate.setSearchBase("dc=l7tech,dc=com");
        orclTemplate.setBindDN("cn=orcladmin");
        orclTemplate.setBindPasswd("7layer");
        return orclTemplate;
    }

    /**
     * an orcl that adds a bogus mapping
     */
    private LdapIdentityProviderConfig getModifiedConfigForOracle() throws IOException {
        LdapIdentityProviderConfig orclTemplate = getConfigForOracle();
        UserMappingConfig cfg = new UserMappingConfig();
        cfg.setObjClass("blah");
        cfg.setNameAttrName("bzzt");
        cfg.setEmailNameAttrName("nnv");
        cfg.setFirstNameAttrName("lkj");
        cfg.setLastNameAttrName("iir");
        cfg.setLoginAttrName("poiu");
        cfg.setPasswdAttrName("p2wok");
        cfg.setPasswdType(PasswdStrategy.CLEAR);
        orclTemplate.setUserMapping(cfg);
        return orclTemplate;
    }

    private LdapIdentityProvider getSpockProviderWithBadSearchBase() throws IOException {
        LdapIdentityProvider spock =  new LdapIdentityProvider(getConfigForSpockWithBadSearchBase());
        return spock;
    }

    private LdapIdentityProvider getSpockProvider() throws IOException {
        LdapIdentityProvider spock =  new LdapIdentityProvider(getConfigForSpock());
        return spock;
    }

    private LdapIdentityProvider getTimTamProvider() throws IOException {
        LdapIdentityProvider timtam =  new LdapIdentityProvider(getConfigForTimTam());
        return timtam;
    }

    private LdapIdentityProvider getMSADProvider() throws IOException {
        LdapIdentityProvider spock =  new LdapIdentityProvider(getConfigForMSAD());
        return spock;
    }

    private LdapIdentityProvider getOracleProvider() throws IOException {
        LdapIdentityProvider orcl =  new LdapIdentityProvider(getConfigForOracle());
        return orcl;
    }

    private LdapIdentityProvider getModifiedOracleProvider() throws IOException {
        LdapIdentityProvider orcl =  new LdapIdentityProvider(getModifiedConfigForOracle());
        return orcl;
    }

    public void testGetUsers() throws Exception {
        if (localProvider == null) {
            System.out.println("NOT READY");
            return;
        }
        Collection users = localProvider.getUserManager().findAll();
        for (Iterator i = users.iterator(); i.hasNext(); ) {
            LdapUser user = (LdapUser)i.next();
            System.out.println("found user " + user);
        }
    }

    public void testGetGroupsAndMembers() throws Exception {
        if (localProvider == null) {
            System.out.println("NOT READY");
            return;
        }
        GroupManager manager = localProvider.getGroupManager();
        Collection groups = manager.findAll();
        for (Iterator i = groups.iterator(); i.hasNext(); ) {
            LdapGroup grp = (LdapGroup)i.next();
            System.out.println("found group " + grp);
            Set userheaders = manager.getUserHeaders(grp);
            for (Iterator ii = userheaders.iterator(); ii.hasNext(); ) {
                System.out.println("group member " + ii.next());
            }
        }
    }

    public void testAuthenticate(String username, String passwd) throws Exception {
        try {
            User notauthenticated = localProvider.getUserManager().findByLogin(username);
            if (notauthenticated == null) {
                System.out.println("user not found");
                return;
            } else {
                System.out.println("user found " + notauthenticated.getUniqueIdentifier());
            }
            User authenticated = null;
            try {
                authenticated = localProvider.authenticate(
                        new LoginCredentials(notauthenticated.getLogin(),
                        passwd.toCharArray(), CredentialFormat.CLEARTEXT, HttpBasic.class, null, null));
            } catch (Exception e) {
                System.out.println("creds do not authenticate.");
            }
            if (authenticated != null) {
                System.out.println("user authenticated " + authenticated);
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
            throw e;
        }
    }

    public static void main(String[] args) throws Exception {
        LdapIdentityProviderTest me = new LdapIdentityProviderTest();

        me.localProvider = me.getSpockProvider();
        //me.localProvider = me.getSpockProviderWithBadSearchBase();
        //me.localProvider = me.getMSADProvider();
        //me.localProvider = me.getOracleProvider();
        //me.localProvider = me.getModifiedOracleProvider();
        //me.localProvider = me.getTimTamProvider();
        //me.testAuthenticate("test_user", "passw0rd");
        //me.localProvider.test();
        //me.testGetUsers();
        //me.testGetGroupsAndMembers();
        LoginCredentials creds = new LoginCredentials("flascelles", "rockclimbing".toCharArray(), WssBasic.class);
        me.localProvider.authenticate(creds);
    }

    private LdapIdentityProvider localProvider;
}
