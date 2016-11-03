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

    private static String kerberosTicket;
    private String servicePrincipalName = null;
    private static final String AES256_CLIENT_PRINCIPAL = "http/wonch14mac1048.ca.com@REDMOND.LOCAL";

    private static boolean usesRainier;

    private static String keyTabBytes;

    public static void setKeyTabBytes(String keyTabValue) {
        keyTabBytes = keyTabValue;
    }

    public static void setKerberosTicket(String ticket) { kerberosTicket = ticket; }

    public static void setUsesRainier(boolean value) { usesRainier = value; }

    public static boolean getUsesRainier() { return usesRainier; }


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
            subject.getPrivateCredentials().add(KerberosClientTest.decode(kerberosTicket));
            //In JDK8 KerberosKeys are no longer added to the private credentials directly.
            //Instead they clients should use keytab containing proper encryption keys
            File keytabFile = writeKeyTab(keyTabBytes, true);
            if (usesRainier) {
                kerberosPrincipal = new KerberosPrincipal(AES256_CLIENT_PRINCIPAL);
            }
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
