package com.l7tech.identity;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.adminws.ClientCredentialManager;
import com.l7tech.util.Locator;
import com.l7tech.identity.ldap.LdapConfigSettings;

import java.util.Collection;
import java.util.Iterator;
import java.net.PasswordAuthentication;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 23, 2003
 *
 */
public class IdProvConfManagerClientTest {

    public static void testListContentOfInternalIDProvider(IdProvConfManagerClient testee) throws Exception {
        IdentityProvider provider = testee.getInternalIdentityProvider();
        UserManager usermanager = provider.getUserManager();
        Collection userheaders = usermanager.findAllHeaders();
        Iterator i = userheaders.iterator();
        while (i.hasNext()) {
            EntityHeader obj = (EntityHeader)i.next();
            System.out.println(obj);
        }
    }

    public static void testAddAndDeleteIDProviderConfig(IdProvConfManagerClient testee) throws Exception {
        com.l7tech.identity.IdentityProviderConfig cfg = new com.l7tech.identity.IdentityProviderConfig(IdentityProviderType.LDAP);
        cfg.setName("spock directory");
        //cfg.setDescription("spock directory as seen from the kelowna office");
        cfg.putProperty(LdapConfigSettings.LDAP_HOST_URL, "ldap://directory.acme.com:389");
        cfg.putProperty(LdapConfigSettings.LDAP_SEARCH_BASE, "dc=layer7-tech,dc=com");
        System.out.println("created new id conf. saving it");
        long newconfid = testee.save(cfg);
        System.out.println("new id conf saved under id=" + newconfid);
        cfg.setOid(newconfid);
        System.out.println("deleting it now");
        testee.delete(cfg);
        System.out.println("done");
    }

    public static void main(String[] args) throws Exception {
        ClientCredentialManager credsManager = (ClientCredentialManager)Locator.getDefault().lookup(ClientCredentialManager.class);
        PasswordAuthentication creds = new PasswordAuthentication("ssgadmin", "ssgadminpasswd".toCharArray());
        credsManager.login(creds);

        IdProvConfManagerClient manager = new IdProvConfManagerClient();
        //testListContentOfInternalIDProvider(manager);
        testAddAndDeleteIDProviderConfig(manager);

        System.exit(0);
    }
}
