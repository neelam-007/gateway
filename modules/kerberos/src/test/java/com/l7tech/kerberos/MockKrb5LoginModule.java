package com.l7tech.kerberos;

import com.l7tech.util.ResourceUtils;
import org.apache.commons.codec.binary.Base64;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.kerberos.KeyTab;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Map;

public class MockKrb5LoginModule implements LoginModule {
    private static long lastModified = Calendar.getInstance().getTime().getTime();

    private Subject subject;

    /**
     * Kerberos principal object captured by real KDC Connection
     */
    private static final String KERBEROS_PRINCIPAL = "rO0ABXNyAC5qYXZheC5zZWN1cml0eS5hdXRoLmtlcmJlcm9zLktlcmJlcm9zUHJpbmNpcGFsmad9XQ8eMykDAAB4cHVyAAJbQqzzF/gGCFTgAgAAeHAAAAAiMCCgAwIBAaEZMBcbBGh0dHAbD3NzZzMubDd0ZWNoLnN1cHVxAH4AAgAAAAwbCkw3VEVDSC5TVVB4";

    /**
     * TGT object captured by real KDC Connection
     */
    private static final String KERBEROS_TICKET = "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";

    private static final String KEY_BYTES = "jD78SGcE0u5x7r5xrxTYbA==";
    private static final int KEY_TYPE = 23;
    private static final int VERSION_NUMBER = 4;
    private String servicePrincipalName = null;

    private static String keyTabBytes;

    public static void setKeyTabBytes(String keyTabValue) {
        keyTabBytes = keyTabValue;
    }




    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        this.subject = subject;
        NameCallback nameCallback = new NameCallback("Name");
        Callback[] callbacks = new Callback[1];
        callbacks[0] = nameCallback;
        try {
            callbackHandler.handle(callbacks);
            servicePrincipalName = nameCallback.getName();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (UnsupportedCallbackException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean login() throws LoginException {
        try {
            KerberosPrincipal kerberosPrincipal = new KerberosPrincipal(servicePrincipalName);
            subject.getPrincipals().add(kerberosPrincipal);
            subject.getPrivateCredentials().add(KerberosClientTest.decode(KERBEROS_TICKET));
            //In JDK8 KerberosKeys are no longer added to the private credentials directly.
            //Instead they clients should use keytab containing proper encryption keys
            File keytabFile = writeKeyTab(keyTabBytes, true);
            KeyTab keytab = KeyTab.getInstance(kerberosPrincipal, keytabFile);
            subject.getPrivateCredentials().add( keytab );
            return true;
        } catch (Exception e) {
            throw new LoginException(e.getMessage());
        }
    }

    @Override
    public boolean commit() throws LoginException {
        return true;
    }

    @Override
    public boolean abort() throws LoginException {
        return true;
    }

    @Override
    public boolean logout() throws LoginException {
        return true;
    }

    public File writeKeyTab(String keyTabData, boolean checkConfig) throws IOException, KerberosException {
        File file = KerberosTestSetup.getKeyTab();
        if (file.exists()) {
            file.delete();
        }

        OutputStream out = null;
        try {
            out = new FileOutputStream(file);
            out.write(Base64.decodeBase64(keyTabData));
        } catch (IOException ioe) {
            throw new KerberosException("Error writing Kerberos keytab.", ioe);
        } finally {
            ResourceUtils.closeQuietly(out);
        }
        lastModified = lastModified + 1000;
        file.setLastModified(lastModified);
        if (checkConfig) {
            KerberosConfig.checkConfig(null, null, false, false);
        }
        return file;
    }

}
