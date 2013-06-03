package com.l7tech.external.assertions.ftprouting.server;

import com.l7tech.server.transport.ftp.FtpServerManager;
import org.apache.ftpserver.ftplet.*;
import org.apache.ftpserver.usermanager.*;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author nilic
 */
public class FtpUserManager implements UserManager {

    //- PUBLIC

    public FtpUserManager(FtpServerManager ftpServerManager) {
        this.ftpServerManager = ftpServerManager;
    }

    /**
     * Process anonymous or username/password authentication.
     *
     * @param authentication The authentication to handle
     * @return The "authenticated" user
     * @throws org.apache.ftpserver.ftplet.AuthenticationFailedException if a null username or password is given or an unknown authentication type
     */
    public User authenticate(Authentication authentication) throws AuthenticationFailedException {
        User user;

        if ( !ftpServerManager.isLicensed() ) {
            if (logger.isLoggable(Level.INFO))
                logger.log(Level.INFO, "Failing authentication, FTP server not licensed.");
            throw new AuthenticationFailedException("Authentication failed (FTP server not licensed).");
        }

        // check input
        if (authentication instanceof AnonymousAuthentication) {
            user = buildUser(USER_ANONYMOUS, null);
            if (logger.isLoggable(Level.FINE))
                logger.log(Level.FINE, "Authenticated anonymous user.");
        }
        else if (authentication instanceof UsernamePasswordAuthentication) {
            UsernamePasswordAuthentication upAuthentication = (UsernamePasswordAuthentication) authentication;
            String login = upAuthentication.getUsername();
            String password = upAuthentication.getPassword();

            if( (login != null) && (password != null) ) {
                user = buildUser(login, password);

                if (logger.isLoggable(Level.FINE))
                    logger.log(Level.FINE, "Authenticated user ''{0}''.", login);
            }
            else {
                throw new AuthenticationFailedException("Authentication failed (no credentials).");
            }
        }
        else {
            throw new AuthenticationFailedException("Unsupported credential format.");
        }

        return user;
    }

    public void delete(final String login) throws FtpException {
        throw new FtpException("Delete not supported.");
    }

    public boolean doesExist(final String login) throws FtpException {
        if (logger.isLoggable(Level.FINER))
            logger.log(Level.FINER, "User exists ''{0}''.", login);

        return true;
    }

    public String getAdminName() throws FtpException {
        if (logger.isLoggable(Level.FINER))
            logger.log(Level.FINER, "Administrator name requested.");

        return null;
    }

    public String[] getAllUserNames() throws FtpException {
        if (logger.isLoggable(Level.FINER))
            logger.log(Level.FINER, "Username list requested.");

        return null;
    }

    public User getUserByName(final String login) throws FtpException {
        User user = null;

        if (USER_ANONYMOUS.equals(login)) {
            user = buildUser(USER_ANONYMOUS, null);

            if (logger.isLoggable(Level.FINER))
                logger.log(Level.FINER, "Getting anonymous user.");
        } else if (login != null) {
            user = buildUser(login, null);

            if (logger.isLoggable(Level.FINER))
                logger.log(Level.FINER, "Getting user ''{0}''.", login);
        }

        if (user == null)
            throw new FtpException("User not found '" + login + "'.");

        return user;
    }

    public boolean isAdmin(final String login) throws FtpException {
        if (logger.isLoggable(Level.FINER))
            logger.log(Level.FINER, "User is not administrator ''{0}''.", login);

        return false;
    }

    public void save(final User user) throws FtpException {
        throw new FtpException("Save not supported.");
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(FtpUserManager.class.getName());

    private static final String USER_ANONYMOUS = "anonymous";

    private final FtpServerManager ftpServerManager;

    /**
     * Create generic user permissions.
     */
    private Authority[] getPermissions() {
        return new Authority[] {
                new ConcurrentLoginPermission(10, 10),
                new TransferRatePermission(0, 0),
                new WritePermission("/"),
        };
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
        baseUser.setMaxIdleTime(60);
        baseUser.setAuthorities(getPermissions());
        return baseUser;
    }
}
