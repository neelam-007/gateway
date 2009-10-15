package com.l7tech.identity.ldap;

import com.l7tech.identity.GroupManager;
import com.l7tech.identity.User;
import com.l7tech.identity.InvalidIdProviderCfgException;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.identity.ldap.*;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.objectmodel.IdentityHeader;
import com.l7tech.security.token.http.HttpBasicToken;
import org.springframework.context.ApplicationContext;

import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.Attributes;
import javax.naming.Context;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.Hashtable;

/**
 * A test class for the ldap providers
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jan 21, 2004<br/>
 */
public class LdapIdentityProviderTest {
    private LdapIdentityProviderConfig getConfigForSpock() throws IOException {
        LdapConfigTemplateManager templateManager = new LdapConfigTemplateManager();
        LdapIdentityProviderConfig spockTemplate = templateManager.getTemplate("GenericLDAP");
        //spockTemplate.setLdapUrl(new String[] {"ldap://riker:389", "ldap://localhost:3899", "ldap://sisko:389"});
        //spockTemplate.setLdapUrl(new String[] {"ldap://spock:389"});
        spockTemplate.setLdapUrl(new String[]{"ldap://spock:389"});
        spockTemplate.setSearchBase("dc=layer7-tech,dc=com");
        return spockTemplate;
    }

    private LdapIdentityProviderConfig getConfigForBones() throws IOException {
        LdapConfigTemplateManager templateManager = new LdapConfigTemplateManager();
        LdapIdentityProviderConfig bonesTemplate = templateManager.getTemplate("GenericLDAP");
        bonesTemplate.setLdapUrl(new String[]{"ldap://bones:389"});
        //bonesTemplate.setLdapUrl(new String[]{"ldap://localhost:3899"});
        bonesTemplate.setBindDN("cn=Manager,dc=layer7-tech,dc=com");
        bonesTemplate.setBindPasswd("7layer");
        bonesTemplate.setSearchBase("dc=layer7-tech,dc=com");
        return bonesTemplate;
    }

    private LdapIdentityProviderConfig getConfigForSpockWithBadSearchBase() throws IOException {
        LdapConfigTemplateManager templateManager = new LdapConfigTemplateManager();
        LdapIdentityProviderConfig spockTemplate = templateManager.getTemplate("GenericLDAP");
        spockTemplate.setLdapUrl(new String[]{"ldap://spock:3899"});
        spockTemplate.setSearchBase("dc=layer8-tech,dc=com");
        return spockTemplate;
    }

    private LdapIdentityProviderConfig getConfigForMSAD() throws IOException {
        LdapConfigTemplateManager templateManager = new LdapConfigTemplateManager();
        LdapIdentityProviderConfig msadTemplate = templateManager.getTemplate("MicrosoftActiveDirectory");
        //msadTemplate.setLdapUrl(new String[]{"ldap://localhost:3899"});
        msadTemplate.setLdapUrl(new String[]{"ldap://mail.l7tech.com:3268"});
        msadTemplate.setSearchBase("ou=Layer 7 Users,dc=L7TECH,dc=LOCAL");
        msadTemplate.setBindDN("browse");
        msadTemplate.setBindPasswd("password");
        return msadTemplate;
    }

    private LdapIdentityProviderConfig getConfigForTimTam() throws IOException {
        LdapConfigTemplateManager templateManager = new LdapConfigTemplateManager();
        LdapIdentityProviderConfig msadTemplate = templateManager.getTemplate("TivoliLdap");
        msadTemplate.setLdapUrl(new String[]{"ldap://192.168.1.120:389"});
        msadTemplate.setSearchBase("DC=IBM,DC=COM");
        msadTemplate.setBindDN("cn=root");
        msadTemplate.setBindPasswd("passw0rd");
        return msadTemplate;
    }

    private LdapIdentityProviderConfig getConfigForOracle() throws IOException {
        LdapConfigTemplateManager templateManager = new LdapConfigTemplateManager();
        LdapIdentityProviderConfig orclTemplate = templateManager.getTemplate("Oracle");
        //orclTemplate.setLdapUrl("ldap://localhost:3899");
        orclTemplate.setLdapUrl(new String[]{"ldap://hugh.l7tech.com:389"});
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
        cfg.setPasswdType( PasswdStrategy.CLEAR);
        orclTemplate.setUserMapping(cfg);
        return orclTemplate;
    }

    private LdapIdentityProvider getSpockProviderWithBadSearchBase() throws IOException, InvalidIdProviderCfgException {
        LdapIdentityProviderImpl spock = new LdapIdentityProviderImpl();
        spock.setIdentityProviderConfig(getConfigForSpockWithBadSearchBase());
        return spock;
    }

    private LdapIdentityProvider getSpockProvider() throws Exception {
        LdapIdentityProviderImpl spock = new LdapIdentityProviderImpl();
        spock.setUserManager(new LdapUserManagerImpl());
        spock.setGroupManager(new LdapGroupManagerImpl());
        spock.setIdentityProviderConfig(getConfigForSpock());
        spock.setClientCertManager((ClientCertManager)applicationContext.getBean("clientCertManager"));
        spock.setLdapRuntimeConfig(new LdapRuntimeConfig((ServerConfig)applicationContext.getBean("serverConfig")));

        spock.afterPropertiesSet();
        return spock;
    }

    private LdapIdentityProvider getBonesProvider() throws Exception {
        LdapIdentityProviderImpl bones = new LdapIdentityProviderImpl();
        bones.setUserManager(new LdapUserManagerImpl());
        bones.setGroupManager(new LdapGroupManagerImpl());
        bones.setIdentityProviderConfig(getConfigForBones());        
        bones.setClientCertManager((ClientCertManager)applicationContext.getBean("clientCertManager"));
        bones.setLdapRuntimeConfig(new LdapRuntimeConfig((ServerConfig)applicationContext.getBean("serverConfig")));

        bones.afterPropertiesSet();
        return bones;
    }

    private LdapIdentityProvider getTimTamProvider() throws IOException, InvalidIdProviderCfgException {
        LdapIdentityProviderImpl timtam = new LdapIdentityProviderImpl();
        timtam.setIdentityProviderConfig(getConfigForTimTam());        
        return timtam;
    }

    private LdapIdentityProvider getMSADProvider() throws Exception {
        LdapIdentityProviderImpl msad = new LdapIdentityProviderImpl();
        msad.setUserManager(new LdapUserManagerImpl());
        msad.setGroupManager(new LdapGroupManagerImpl());
        msad.setIdentityProviderConfig(getConfigForMSAD());        
        msad.setClientCertManager((ClientCertManager)applicationContext.getBean("clientCertManager"));
        msad.setLdapRuntimeConfig(new LdapRuntimeConfig((ServerConfig)applicationContext.getBean("serverConfig")));
        msad.afterPropertiesSet();
        return msad;
    }

    private LdapIdentityProvider getOracleProvider() throws IOException, InvalidIdProviderCfgException {
        LdapIdentityProviderImpl orcl = new LdapIdentityProviderImpl();
        orcl.setIdentityProviderConfig(getConfigForOracle());
        return orcl;
    }

    private LdapIdentityProvider getModifiedOracleProvider() throws IOException, InvalidIdProviderCfgException {
        LdapIdentityProviderImpl orcl = new LdapIdentityProviderImpl();
        orcl.setIdentityProviderConfig(getModifiedConfigForOracle());
        return orcl;
    }

    public void testGetUsers() throws Exception {
        if (localProvider == null) {
            System.out.println("NOT READY");
            return;
        }
        Collection<IdentityHeader> users = localProvider.getUserManager().findAllHeaders();
        for (Iterator<IdentityHeader> i = users.iterator(); i.hasNext();) {
            IdentityHeader h = i.next();
            System.out.println("found user " + h);
        }
    }

    public void testGetGroupsAndMembers() throws Exception {
        if (localProvider == null) {
            System.out.println("NOT READY");
            return;
        }
        GroupManager manager = localProvider.getGroupManager();
        Collection groups = manager.findAll();
        for (Iterator i = groups.iterator(); i.hasNext();) {
            LdapGroup grp = (LdapGroup)i.next();
            System.out.println("found group " + grp);
            Set userheaders = manager.getUserHeaders(grp);
            for (Iterator ii = userheaders.iterator(); ii.hasNext();) {
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
                System.out.println("user found " + notauthenticated.getId());
            }
            AuthenticationResult authResult = null;
            try {
                authResult = localProvider.authenticate(LoginCredentials.makeLoginCredentials(
                        new HttpBasicToken(notauthenticated.getLogin(), passwd.toCharArray()), HttpBasic.class));
            } catch (Exception e) {
                System.out.println("creds do not authenticate.");
            }
            if (authResult != null) {
                System.out.println("user authenticated " + authResult.getUser());
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
            throw e;
        }
    }

    public static void main(String[] args) throws Exception {
        LdapIdentityProviderTest me = new LdapIdentityProviderTest();
        /*//me.localProvider = me.getSpockProvider();
        me.localProvider = me.getBonesProvider();
        //me.localProvider = me.getSpockProviderWithBadSearchBase();
        //me.localProvider = me.getMSADProvider();
        //me.localProvider = me.getOracleProvider();
        //me.localProvider = me.getModifiedOracleProvider();
        //me.localProvider = me.getTimTamProvider();
        //me.testAuthenticate("test_user", "passw0rd");
        //me.localProvider.test();
        //me.testGetUsers();
        //me.testGetGroupsAndMembers();
        LoginCredentials creds = new LoginCredentials("flascelles", "".toCharArray(), null);
        User authenticated = me.localProvider.authenticate(creds);
        System.out.println("USER " + authenticated);*/

        me.checkSasl("ldap://localhost:3899");
    }

    private void checkSasl(String url) throws Exception {

        Hashtable<? super String, ? super String> env = LdapUtils.newEnvironment();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, url);
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, "cn=Manager,dc=layer7-tech,dc=com");
        env.put(Context.SECURITY_CREDENTIALS, "7layer");
        LdapUtils.lock( env );
        DirContext cntx = new InitialDirContext(env);
        Attributes attrs = cntx.getAttributes(url, new String[]{"supportedSASLMechanisms"});
        System.out.println(attrs);
    }

    private ApplicationContext applicationContext = null;//ApplicationContexts.getTestApplicationContext();
    private LdapIdentityProvider localProvider;
}
