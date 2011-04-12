/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.server.identity.internal;

import com.l7tech.common.password.IncorrectPasswordException;
import com.l7tech.common.password.PasswordHasher;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.util.Charsets;
import com.l7tech.util.Config;
import com.l7tech.util.HexUtils;

import java.util.logging.Level;
import java.util.logging.Logger;

public class InternalUserPasswordManagerImpl implements InternalUserPasswordManager{
    private final PasswordHasher passwordHasher;
    private final Config config;

    public InternalUserPasswordManagerImpl(Config config, PasswordHasher passwordHasher) {
        this.passwordHasher = passwordHasher;
        this.config = config;
    }

    @Override
    public boolean configureUserPasswordHashes(InternalUser internalUser, String clearTextPassword) {
        final String userHashedPassword = internalUser.getHashedPassword();
        final boolean userHasHashedPassword = userHashedPassword != null && !userHashedPassword.trim().isEmpty();

        boolean userWasUpdated = false;
        final StringBuilder fineLogMsg = new StringBuilder("Updating password storage for login '" + internalUser.getLogin() + "'. ");

        //first check to see if this admin user is new or being upgraded, in which case the user needs a new hash
        if(!userHasHashedPassword){
            internalUser.setHashedPassword(passwordHasher.hashPassword(clearTextPassword.getBytes(Charsets.UTF8)));
            userWasUpdated = true;
            fineLogMsg.append("Set password hash. ");
        } else {
            //does the existing hash verify for the incoming password?
            boolean isNewPassword = true;
            try {
                if(passwordHasher.isVerifierRecognized(userHashedPassword)){
                    passwordHasher.verifyPassword(clearTextPassword.getBytes(Charsets.UTF8), userHashedPassword);
                    isNewPassword = false;
                }
            } catch (IncorrectPasswordException e) {
                //fall through ok - hashedPassword property has been modified.
            }

            if(isNewPassword){
                internalUser.setHashedPassword(passwordHasher.hashPassword(clearTextPassword.getBytes(Charsets.UTF8)));
                internalUser.setChangePassword(false);
                userWasUpdated = true;
            }
        }

        //update digest if state has changed.
        final String httpDigestEnable = config.getProperty("httpDigest.enable", "false");
        boolean isHttpDigestEnabled = httpDigestEnable != null && Boolean.valueOf(httpDigestEnable);

        final String userDigest = internalUser.getHttpDigest();
        final boolean userHasDigest = userDigest != null && !userDigest.trim().isEmpty();
        final String calculatedDigest = HexUtils.encodePasswd(internalUser.getLogin(), clearTextPassword, HexUtils.REALM);

        if (isHttpDigestEnabled) {
            if (!userHasDigest || userWasUpdated) {//if hashed password changed then update the digest if enabled
                internalUser.setHttpDigest(calculatedDigest);
                userWasUpdated = true;
                fineLogMsg.append("Set digest property. ");
            }
        } else if (userHasDigest) {
            internalUser.setHttpDigest(null);
            userWasUpdated = true;
            fineLogMsg.append("Cleared digest property. ");
        }

        if(logger.isLoggable(Level.FINE)){
            logger.log(Level.FINE, fineLogMsg.toString());
        }

        return userWasUpdated;
    }

    // - PRIVATE
    private final Logger logger = Logger.getLogger(getClass().getName());
}
