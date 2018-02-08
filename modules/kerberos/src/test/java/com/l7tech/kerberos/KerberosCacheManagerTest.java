package com.l7tech.kerberos;

import com.l7tech.kerberos.delegate.KerberosDelegateClient;
import com.l7tech.util.FileUtils;
import org.junit.*;
import sun.security.krb5.KrbException;
import sun.security.krb5.PrincipalName;
import sun.security.krb5.RealmException;

import javax.security.auth.kerberos.KerberosTicket;
import java.io.File;
import java.io.IOException;

import static junit.framework.Assert.assertTrue;

/**
 * Created by agram03 on 2017-11-23.
 * class to add tests related to KerberosCacheManager, which stores only self service and proxy service tickets.
 */
public class KerberosCacheManagerTest {

    private static final String FOLDER_NAME_KERBEROS = "kerberos";
    private static final String REALM_L7TECH_DEV = "L7TECH.DEV";
    private static final String KERBEROS_USER = "kerb_user";
    private static final String KEYTAB_PRINCIPAL = "http/ssg5.l7tech.dev@L7TECH.DEV";
    private static final String TICKET_EXPIRED = "ticket=Expired";
    //    Use base64 encoded keytab and tgt ticket.
    private static final String KEYTAB = "BQIAAABCAAIACkw3VEVDSC5ERVYABGh0dHAAD3NzZzUubDd0ZWNoLmRldgAAAAAAAAAAAwAXABCRqKd12yDxqQHeS3wzsafD";
    private static final String KERBEROS_TGT = "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";

    private static File tmpDir;
    private static KerberosDelegateClient client;

    @Before
    public void setup() throws IOException, KerberosException {
        tmpDir = FileUtils.createTempDirectory(FOLDER_NAME_KERBEROS, null, null, true);
        KerberosTestSetup.init(tmpDir);
        MockKrb5LoginModule.setKeyTabBytes(KEYTAB);
        MockKrb5LoginModule.setUsesRainier(false);
        MockKrb5LoginModule.setKerberosTicket(KERBEROS_TGT);
    }

    @After
    public void tearDown() throws Exception {
        MockKrb5LoginModule.setKeyTabBytes(null);
        MockKrb5LoginModule.setKerberosTicket(null);
    }

    /**
     * Setup for mock test
     * @throws IOException
     */
    public static void setupMock(final String realm) throws IOException {
        client = new KerberosDelegateClient() {
            private String defaultRealm = realm;

            @Override
            protected KerberosTicket getS4U2SelfTicket(final KerberosTicket tgt, final String servicePrincipal,
                                                       final String user, final String userRealm)
                    throws KrbException, IOException {
//                return a fake service ticket
                return tgt;
            }

            @Override
            protected KerberosTicket getS4U2ProxyTicket(final KerberosTicket tgt, final String servicePrincipalName,
                                                        final PrincipalName clientPrincipal, final Object o)
                    throws KrbException, IOException {
                return tgt;
            }

            @Override
            public PrincipalName getPrincipalName(final String user, final String userRealm) throws RealmException {
                final PrincipalName clientPrincipalName;
                    clientPrincipalName = new PrincipalName(user, userRealm != null ? userRealm : defaultRealm);
                return clientPrincipalName;
            }
        };
        KerberosTestSetup.setUpLoginConfig(tmpDir);
    }


    /**
     *
     * Kerberos uses Jboss cache to store service tickets.
     * Key used to store and retrieve these tickets is used to compare during eviction process.
     * Due to concurrency issue, TGT inside key was destroyed by other threads.
     * This unit test will make sure even if ticket is destroyed toString() will return valid response.
     * Rally ticket for reference - DE324840
     *
     * */
    @Test
    public void shouldReturnValidResponseOnToStringWhenTicketIsDestroyed() throws Exception {
        setupMock(REALM_L7TECH_DEV);
        PrincipalName clientPrincipalName = client.getPrincipalName(KERBEROS_USER, REALM_L7TECH_DEV);
        KerberosServiceTicket serviceTicket = client.getKerberosSelfServiceTicket(KEYTAB_PRINCIPAL, KERBEROS_USER);
        KerberosCacheManager.Key key = new KerberosCacheManager.Key(clientPrincipalName,
                serviceTicket.getDelegatedKerberosTicket());
        assertTrue(key.toString() != null && !key.toString().contains(TICKET_EXPIRED));
        serviceTicket.getDelegatedKerberosTicket().destroy();
        assertTrue(key.toString() != null && key.toString().contains(TICKET_EXPIRED));
    }
}
