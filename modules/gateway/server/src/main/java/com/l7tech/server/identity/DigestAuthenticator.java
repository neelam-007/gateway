/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.identity;

import com.l7tech.identity.*;
import com.l7tech.identity.ldap.LdapUser;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.util.HexUtils;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bean that knows how to do HTTP Digest authentication.
 */
public class DigestAuthenticator {
    private static final Logger logger = Logger.getLogger(DigestAuthenticator.class.getName());

    public static AuthenticationResult authenticateDigestCredentials(LoginCredentials pc, User user)
            throws MissingCredentialsException, BadCredentialsException 
    {
        Object payload = pc.getPayload();
        String hashedPass;
        if (user instanceof InternalUser) {
            hashedPass = ((InternalUser) user).getHashedPassword();
        } else if (user instanceof LdapUser) {
            // Unlikely to work... most LDAPs have passwords that are either invisible to us, or hashed using some
            // algorithm other than ours.
            LdapUser ldapUser = (LdapUser) user;
            hashedPass = HexUtils.encodePasswd(ldapUser.getLogin(), ldapUser.getPassword(), HttpDigest.REALM);
        } else {
            throw new BadCredentialsException("User does not have a usable password for digest authentication");
        }

        char[] credentials = pc.getCredentials();
        Map authParams = (Map)payload;
        if (authParams == null) {
            String msg = "No Digest authentication parameters found in LoginCredentials payload!";
            logger.log(Level.SEVERE, msg);
            throw new MissingCredentialsException(msg);
        }

        String qop = (String)authParams.get(HttpDigest.PARAM_QOP);
        String nonce = (String)authParams.get(HttpDigest.PARAM_NONCE);

        //noinspection RedundantCast,RedundantCast
        String a2 = (String)authParams.get(HttpDigest.PARAM_METHOD) + ":" +
          (String)authParams.get(HttpDigest.PARAM_URI);

        String ha2 = HexUtils.encodeMd5Digest(HexUtils.getMd5Digest(a2.getBytes()));

        String serverDigestValue;
        if (!HttpDigest.QOP_AUTH.equals(qop))
            serverDigestValue = hashedPass + ":" + nonce + ":" + ha2;
        else {
            String nc = (String)authParams.get(HttpDigest.PARAM_NC);
            String cnonce = (String)authParams.get(HttpDigest.PARAM_CNONCE);

            serverDigestValue = hashedPass + ":" + nonce + ":" + nc + ":"
              + cnonce + ":" + qop + ":" + ha2;
        }

        String expectedResponse = HexUtils.encodeMd5Digest(HexUtils.getMd5Digest(serverDigestValue.getBytes()));
        String response = new String(credentials);

        String login = pc.getLogin();
        if (response.equals(expectedResponse)) {
            logger.info("User " + login + " authenticated successfully with digest credentials.");
            return new AuthenticationResult(user, pc.getSecurityTokens());
        } else {
            String msg = "User " + login + " failed to match.";
            logger.warning(msg);
            throw new BadCredentialsException(msg);
        }
    }

}
