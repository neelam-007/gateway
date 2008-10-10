package com.l7tech.server.security.kerberos;

import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import java.io.IOException;
import java.util.Set;

import com.l7tech.kerberos.KerberosConfigConstants;
import com.l7tech.kerberos.KerberosClient;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.Assert;

/**
 * User: vchan
 */
@Ignore("This test requires developer configuration")
public class KerberosTest implements KerberosConfigConstants {

    static final String SSG_HOME_DIR = "/ssg/test";

    static final String LOGIN_CTX_NAME = "com.l7tech.common.security.kerberos.outbound.account";

    KerberosClient krbClient;

    static {
        // override the values in the keytab
        System.setProperty("java.security.krb5.realm", "TEST2003.COM");
        System.setProperty("java.security.krb5.kdc", "192.168.1.145");
    }

    @BeforeClass
    public void setUp() throws Exception {

        System.setProperty(SYSPROP_SSG_HOME, SSG_HOME_DIR);
        System.setProperty(SYSPROP_LOGINCFG_PATH, SSG_HOME_DIR+"/login.config");
        System.setProperty(SYSPROP_KRB5CFG_PATH, SSG_HOME_DIR+"/krb5.conf");

        if (krbClient == null)
            krbClient = new KerberosClient();
    }

    /* testing...
     * 1. login as some configured user
     * 2. Instantiate server assertion
     * 3. Run the assertion
     */
    @Test
    public void testClientLogin() {

        LoginContext loginCtx = null;
        try {

            Subject krbSubject = new Subject();
            loginCtx = new LoginContext(LOGIN_CTX_NAME, krbSubject, new CallbackHandler() {

                public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {

                    for( Callback cb : callbacks ) {

                        if (cb instanceof NameCallback) {
                            NameCallback name = (NameCallback) cb;
                            name.setName( "vcwinsvr" );
                            System.out.println( "Using kerberos SPN '" + name.getName() + "'." );

                        } else if (cb instanceof PasswordCallback) {
                            PasswordCallback passwd = (PasswordCallback) cb;
                            passwd.setPassword( "p65ssw@rd".toCharArray() );
                        }
                    }

                }
            });
            Assert.assertNotNull(loginCtx);

            loginCtx.login();

            Set<?> principals = krbSubject.getPrincipals();
            Set<?> private_creds = krbSubject.getPrivateCredentials();
            Set<?> public_creds = krbSubject.getPublicCredentials();

            Assert.assertNotNull(principals);
            Assert.assertNotNull(private_creds);
            Assert.assertNotNull(public_creds);

            // logout
            loginCtx.logout();
            
        } catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail("Unexpected exception encountered: " + ex.getMessage());
        }
    }

}
