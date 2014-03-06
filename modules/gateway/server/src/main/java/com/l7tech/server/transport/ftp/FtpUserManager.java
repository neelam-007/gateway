package com.l7tech.server.transport.ftp;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.User;
import org.apache.ftpserver.ftplet.Authentication;
import org.apache.ftpserver.ftplet.AuthenticationFailedException;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.usermanager.AnonymousAuthentication;
import org.apache.ftpserver.usermanager.UsernamePasswordAuthentication;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.ConcurrentLoginPermission;
import org.apache.ftpserver.usermanager.impl.TransferRatePermission;
import org.apache.ftpserver.usermanager.impl.WritePermission;
import org.jetbrains.annotations.Nullable;

/**
 * UserManager that defers authentication.
 *
 * <p>This class will permit any username/password combination in the expectation
 * that credentials will be checked when a policy is executed (if required)</p>
 *
 * @author Steve Jones
 */
class FtpUserManager implements UserManager {
    private static final Logger logger = Logger.getLogger(FtpUserManager.class.getName());

    private static final String USER_ANONYMOUS = "anonymous";

    private final int maxIdleTime;

    private final List<Authority> PERMISSIONS;

    public FtpUserManager(int maxConcurrentLogins, int maxConcurrentLoginsPerIP, int maxIdleTime) {
        this.maxIdleTime = maxIdleTime;

        PERMISSIONS = Collections.unmodifiableList(
                Arrays.asList(new ConcurrentLoginPermission(maxConcurrentLogins, maxConcurrentLoginsPerIP),
                        new TransferRatePermission(0, 0),
                        new WritePermission("/"))
        );
    }

    /**
     * Process anonymous or username/password authentication.
     *
     * @param authentication The authentication to handle
     * @return The "authenticated" user
     * @throws AuthenticationFailedException if a null username or password is given or an unknown authentication type
     */
    public User authenticate(Authentication authentication) throws AuthenticationFailedException {
        User user;

        // check input
        if (authentication instanceof AnonymousAuthentication) {
            user = buildUser(USER_ANONYMOUS, null);

            logger.log(Level.FINE, "Authenticated anonymous user.");
        } else if (authentication instanceof UsernamePasswordAuthentication) {
            UsernamePasswordAuthentication upAuthentication = (UsernamePasswordAuthentication) authentication;
            String login = upAuthentication.getUsername();
            String password = upAuthentication.getPassword();

            if((login != null) && (password != null)) {
                user = buildUser(login, password);

                logger.log(Level.FINE, "Authenticated user ''{0}''.", login);
            } else {
                throw new AuthenticationFailedException("Authentication failed (no credentials).");
            }
        } else {
            throw new AuthenticationFailedException("Unsupported credential format.");
        }

        return user;
    }

    public void delete(final String login) throws FtpException {
        throw new FtpException("Delete not supported.");
    }

    public boolean doesExist(final String login) throws FtpException {
        logger.log(Level.FINER, "User exists ''{0}''.", login);

        return true;
    }

    public String getAdminName() throws FtpException {
        logger.log(Level.FINER, "Administrator name requested.");

        return null;
    }

    public String[] getAllUserNames() throws FtpException {
        logger.log(Level.FINER, "Username list requested.");

        return null;
    }

    public User getUserByName(final String login) throws FtpException {
        User user = null;

        if (USER_ANONYMOUS.equals(login)) {
            user = buildUser(USER_ANONYMOUS, null);

            logger.log(Level.FINER, "Getting anonymous user.");
        } else if (login != null) {
            user = buildUser(login, null);

            logger.log(Level.FINER, "Getting user ''{0}''.", login);
        }

        if (user == null)
            throw new FtpException("User not found '" + login + "'.");

        return user;
    }

    public boolean isAdmin(final String login) throws FtpException {
        logger.log(Level.FINER, "User is not administrator ''{0}''.", login);

        return false;
    }

    public void save(final User user) throws FtpException {
        throw new FtpException("Save not supported.");
    }

    /**
     * Build user information
     */
    private User buildUser(String login, @Nullable String password) {
        BaseUser baseUser = new BaseUser();
        baseUser.setEnabled(true);
        baseUser.setName(login);
        baseUser.setPassword(password);
        baseUser.setHomeDirectory("/");
        baseUser.setMaxIdleTime(maxIdleTime);
        baseUser.setAuthorities(PERMISSIONS);
        return baseUser;
    }
}
