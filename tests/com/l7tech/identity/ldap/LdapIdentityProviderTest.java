package com.l7tech.identity.ldap;

import junit.framework.TestCase;

import java.util.Collection;
import java.util.Iterator;
import java.io.IOException;

import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.identity.User;

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
        LdapIdentityProviderConfig spockTemplate = templateManager.getTemplate("Standard LDAP Template");
        // todo make a close of the template
        spockTemplate.setLdapUrl("ldap://localhost:3899");
        spockTemplate.setSearchBase("dc=layer7-tech,dc=com");
        return spockTemplate;
    }

    private LdapIdentityProvider getSpockProvider() throws IOException {
        LdapIdentityProvider spock =  new LdapIdentityProvider();
        spock.initialize(getConfigForSpock());
        return spock;
    }

    public void testGetUsers() throws Exception {
        Collection users = getSpockProvider().getUserManager().findAll();
        for (Iterator i = users.iterator(); i.hasNext(); ) {
            LdapUser user = (LdapUser)i.next();
            System.out.println("found user " + user);
        }
    }

    public void testAuthenticate() throws Exception {
        try {
        User notauthenticated = getSpockProvider().getUserManager().findByLogin("flascelles");
        if (notauthenticated == null) {
            System.out.println("user not found");
            return;
        } else {
            System.out.println("user found " + notauthenticated.getUniqueIdentifier());
        }
        User authenticated = getSpockProvider().authenticate(new LoginCredentials(notauthenticated.getLogin(), "rockclimbing".getBytes(), CredentialFormat.CLEARTEXT, null, null));
        if (authenticated != null) {
            System.out.println("user authenticated " + authenticated);
        }
        } catch (Exception e) {
            e.printStackTrace(System.err);
            throw e;
        }
    }
}
