/**
 * Copyright (C) 2006-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.example.manager.apidemo;

import com.l7tech.common.password.PasswordHasher;
import com.l7tech.common.password.Sha512CryptPasswordHasher;
import com.l7tech.util.Charsets;
import com.l7tech.util.HexUtils;
import com.l7tech.gateway.common.admin.IdentityAdmin;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.User;
import com.l7tech.identity.UserBean;
import com.l7tech.objectmodel.*;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A class that bulk creates users in an internal identity provider
 * of a SecureSpan Gateway.
 */
public class IdentityProvider {
    public static final String BULKUSERS_NAMEPREFIX = "nohcolber";
    public static final String BULKUSERS_PASSWDPREFIX = "euqseveltnop";
    public static final int BULKUSERS_NUMBER = 5;

    private static final Logger logger = Logger.getLogger(IdentityProvider.class.getName());
    private SsgAdminSession session;
    private final PasswordHasher passwordHasher = new Sha512CryptPasswordHasher();

    /**
     * Bulk creates users using following arguments:
     * args[0]: ssg host name
     * args[1]: ssg admin account name
     * args[2]: ssg admin account passwd
     * args[3]: number of bulk users to create
     * args[4]: bulk users' name prefix
     * args[5]: bulk users' passwd prefix
     * @param args without arguments, will run with all default settings
     * @throws Exception if something bad happens
     */
    public static void main(String[] args) throws Exception {
        final String[] fargs = args;
        Subject.doAsPrivileged(new Subject(), new PrivilegedAction() {
            public Object run() {
                try {
                    IdentityProvider.run(fargs);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "exception running Identity Provider", e);
                }
                return null;
            }
        }, null);
    }

    public static void run(String[] args) throws Exception {
        if (args != null && args.length == 6) {
            String ssghostname = args[0];
            String ssgadminname = args[1];
            String ssgadminpasswd = args[2];
            int nrusers = Integer.parseInt(args[3]);
            String bulknameprefix = args[4];
            String bulkpasswdprefix = args[5];
            IdentityProvider creator = new IdentityProvider(ssghostname, ssgadminname, ssgadminpasswd);
            try {
                creator.createBulkUsers(bulknameprefix, bulkpasswdprefix, nrusers);
                System.out.println("Users Created (" + nrusers + ")");
                String[] res = creator.listUsers();
                for (int i = 0; i < res.length; i++) {
                    String u = res[i];
                    logger.info("User found: " + u);
                }

            } catch (Exception e) {
                System.out.println("problem creating bulk users " + e);
            }
        } else if (args == null || args.length == 0) {
            IdentityProvider creator = new IdentityProvider(Main.SSGHOST, Main.ADMINACCOUNT_NAME, Main.ADMINACCOUNT_PASSWD);
            try {
                creator.createBulkUsers(BULKUSERS_NAMEPREFIX, BULKUSERS_PASSWDPREFIX, BULKUSERS_NUMBER);
                logger.info("Users Created (" + BULKUSERS_NUMBER + ")");
                String[] res = creator.listUsers();
                for (int i = 0; i < res.length; i++) {
                    String u = res[i];
                    logger.info("User found: " + u);
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "problem creating bulk users", e);
            }
        } else {
            System.out.println("Unsupported number of parameters");
        }
    }

    /* Use this demo functionality using its own internal admin session.
     * @param ssghost SecureSpan Host name to connect to
     * @param login the admin username to use
     * @param passwd the admin password to use
     */
    public IdentityProvider(String ssghost, String login, String passwd) throws MalformedURLException, LoginException, RemoteException {
        session = new SsgAdminSession(ssghost, login, passwd);
    }

    /**
     * Use this demo functionality using a pre-established admin session.
     * @param session pre-established session
     */
    public IdentityProvider(SsgAdminSession session) {
        this.session = session;
    }

    /**
     * Bulk creation of IIP users.
     * @param namePrefix the name prefix for all users created
     * @param passwdPrefix the password prefix for all users created
     * @param numberOfUsers the number of users created
     * @return a list of ids for the newly created users
     */
    public ArrayList<String> createBulkUsers(String namePrefix, String passwdPrefix, int numberOfUsers) throws SaveException, RemoteException, ObjectNotFoundException, UpdateException, InvalidPasswordException {
        ArrayList<String> output = new ArrayList<String>();
        for (int i = 0; i < numberOfUsers; i++) {
            User newUser = constructUser(namePrefix + i, passwdPrefix + i);
            String userid = saveUser(newUser);
            output.add(userid);
        }
        return output;
    }

    public void removeBulkUsers(String namePrefix) throws FindException, RemoteException, ObjectNotFoundException, DeleteException {
        IdentityAdmin identityAdmin = session.getIdentityAdmin();
        EntityHeaderSet<IdentityHeader> res = identityAdmin.findAllUsers(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID);
        for (EntityHeader header : res) {
            if (header.getName().startsWith(namePrefix)) {
                identityAdmin.deleteUser(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID, header.getStrId());
            }
        }
    }

    public String[] listUsers() throws RemoteException, FindException {
        IdentityAdmin  identityAdmin = session.getIdentityAdmin();
        EntityHeaderSet<IdentityHeader> res = identityAdmin.findAllUsers(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID);
        String[] output = new String[res.size()];
        int i = 0;
        for (EntityHeader header : res) {
            output[i++] = "User " + header.getName() + " [" + header.getGoid() + "]";
        }
        return output;
    }

    private User constructUser(final String login, final String passwd) {
        UserBean u = new UserBean();
        u.setProviderId(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID);
        u.setLogin(login);
        String passwdenc = passwordHasher.hashPassword(passwd.getBytes(Charsets.UTF8));
        u.setHashedPassword(passwdenc);
        u.setEmail(login + "@bogus.com");
        u.setName(login);
        return u;
    }

    /**
     * Create a single user
     * @param ub the user information
     * @return the object id associated to this new user
     */
    public String saveUser(final User ub) throws RemoteException, SaveException, ObjectNotFoundException, UpdateException, InvalidPasswordException {
        IdentityAdmin  identityAdmin = session.getIdentityAdmin();
        return identityAdmin.saveUser(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID, ub, null);
    }
}
