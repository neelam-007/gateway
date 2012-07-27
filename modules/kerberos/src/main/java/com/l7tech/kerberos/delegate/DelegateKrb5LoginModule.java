package com.l7tech.kerberos.delegate;

import sun.security.jgss.krb5.Krb5Util;
import sun.security.krb5.*;
import sun.security.krb5.internal.KDCOptions;

import javax.security.auth.DestroyFailedException;
import javax.security.auth.Subject;
import javax.security.auth.callback.*;
import javax.security.auth.kerberos.KerberosKey;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.kerberos.KerberosTicket;
import javax.security.auth.kerberos.KeyTab;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

/**
 * Implementation for Kerberos login Module, the current com.sun.security.auth.module.Krb5LoginModule
 * implementation does not support forwable TGT, this re-implementation does not support all the
 * options and features provided by com.sun.security.auth.module.Krb5LoginModule, it customized for
 * the implementation for Constrained Delegation. If com.sun.security.auth.module.Krb5LoginModule
 * support forwarable TGT, this implementation can be removed and replace with the new
 * com.sun.security.auth.module.Krb5LoginModule module.
 *
 * On the other hand, for obtaining credentials of the client to be delegatable to a serivce, we can
 * add forwardable=true in the [libdefaults] section of krb5.ini, however with this approach, all
 * client credentials will be forwardable, not only for constrained delegation.
 */
public class DelegateKrb5LoginModule implements LoginModule {

    private Subject subject;
    private CallbackHandler callbackHandler;
    private Map sharedState;
    private Map<String, ?> options;

    private String keyTabPath = null;
    private KeyTab keyTab = null;
    private Credentials tgtCreds = null;
    private KerberosTicket tgt = null;
    private PrincipalName clientPrincipal = null;
    private EncryptionKey[] ekeys = null;

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        this.subject = subject;
        this.callbackHandler = callbackHandler;
        this.sharedState = sharedState;
        this.options = options;

        this.keyTabPath = (String) options.get("keyTab");
        if (keyTabPath != null) {
            keyTab = KeyTab.getInstance(new File(keyTabPath));
        }

    }

    @Override
    public boolean login() throws LoginException {

        try {
            sun.security.krb5.Config.refresh();
        } catch (KrbException e) {
            LoginException le = new LoginException(e.getMessage());
            le.initCause(e);
            throw le;
        }

        try {
            KrbAsReqBuilder reqBuilder = null;
            clientPrincipal = getClientPrincipal();
            if (keyTab == null) {
                char[] password = getPassword();
                reqBuilder = new KrbAsReqBuilder(clientPrincipal, password);
            } else {
                reqBuilder = new KrbAsReqBuilder(clientPrincipal, keyTab);
            }
            KDCOptions o = new KDCOptions();
            o.set(KDCOptions.FORWARDABLE, true);
            reqBuilder.setOptions(o);
            tgtCreds = reqBuilder.action().getCreds();
            if (keyTab == null) {
                ekeys = reqBuilder.getKeys();
            }
            reqBuilder.destroy();

            if (tgtCreds == null) {
                throw new LoginException("Unable to obtain TGT for: " + clientPrincipal);
            }

        } catch (Exception e) {
            LoginException le = new LoginException(e.getMessage());
            le.initCause(e);
            throw le;
        }

        return true;
    }

    @Override
    public boolean commit() throws LoginException {
        KerberosPrincipal kerberosPrincipal = new KerberosPrincipal(clientPrincipal.getName());

        tgt = Krb5Util.credsToTicket(tgtCreds);

        if (!subject.getPrincipals().contains(kerberosPrincipal)) {
            subject.getPrincipals().add(kerberosPrincipal);
        }

        if (tgt != null) {
            if (!subject.getPrivateCredentials().contains(tgt))
                subject.getPrivateCredentials().add(tgt);
        }

        if (ekeys != null) {
            for (int i = 0; i < ekeys.length; i++) {
                EncryptionKey ekey = ekeys[i];

                KerberosKey kKey = new KerberosKey(
                        new KerberosPrincipal(clientPrincipal.getName()),
                        ekey.getBytes(),
                        ekey.getEType(),
                        (ekey.getKeyVersionNumber() == null ? 0 : ekey.getKeyVersionNumber()));
                subject.getPrivateCredentials().add(kKey);
                ekey.destroy();
            }
        } else {
            if (!subject.getPrivateCredentials().contains(keyTab)) {
                subject.getPrivateCredentials().add(keyTab);
                for (KerberosKey key: keyTab.getKeys(new KerberosPrincipal(clientPrincipal.getName()))) {
                    subject.getPrivateCredentials().add(new Krb5Util.KeysFromKeyTab(key));
                }
            }
        }
        return true;

    }

    @Override
    public boolean abort() throws LoginException {
        if (tgt != null) {
            try {
                tgt.destroy();
            } catch (DestroyFailedException e) {
                LoginException le = new LoginException(e.getMessage());
                le.initCause(e);
                throw le;
            }
        }
        return true;
    }

    @Override
    public boolean logout() throws LoginException {

        subject.getPrincipals().remove(clientPrincipal);
        Iterator<Object> it = subject.getPrivateCredentials().iterator();
        while (it.hasNext()) {
            Object o = it.next();
            if (o instanceof KerberosTicket) {
                try {
                    ((KerberosTicket) o).destroy();
                } catch (DestroyFailedException e) {
                    e.printStackTrace();
                    LoginException le = new LoginException(e.getMessage());
                    le.initCause(e);
                    throw le;
                }
                it.remove();
            }
        }
        return true;
    }

    private PrincipalName getClientPrincipal() throws UnsupportedCallbackException, IOException, RealmException {
        Callback[] callbacks = new Callback[1];
        callbacks[0] = new NameCallback("UserName:");
        callbackHandler.handle(callbacks);
        String username = ((NameCallback) callbacks[0]).getName();
        if (username == null || username.length() == 0) {
            throw new IllegalArgumentException("No user name is specified.");
        }
        return new PrincipalName(username);
    }

    private char[] getPassword() throws UnsupportedCallbackException, IOException {
        Callback[] callbacks = new Callback[1];
        callbacks[0] = new PasswordCallback("Password:", false);
        callbackHandler.handle(callbacks);
        char[] tmpPassword = ((PasswordCallback)
                callbacks[0]).getPassword();
        if (tmpPassword == null) {
            tmpPassword = new char[0];
        }
        char[] password = new char[tmpPassword.length];
        System.arraycopy(tmpPassword, 0, password, 0, tmpPassword.length);
        ((PasswordCallback) callbacks[0]).clearPassword();

        for (int i = 0; i < tmpPassword.length; i++)
            tmpPassword[i] = ' ';
        tmpPassword = null;
        return password;
    }
}
