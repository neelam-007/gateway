/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.identity;

import com.l7tech.identity.*;
import com.l7tech.identity.ldap.LdapUser;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.util.HexUtils;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bean that knows how to do HTTP Digest authentication.
 */
public class DigestAuthenticator {
    private static final Logger logger = Logger.getLogger(DigestAuthenticator.class.getName());

    // Some handy constants for the Authorization and WWW-Authenticate headers
    public static final String PARAM_USERNAME = "username";
    public static final String PARAM_RESPONSE = "response";
    public static final String PARAM_NONCE = "nonce";
    public static final String PARAM_CNONCE = "cnonce";
    public static final String PARAM_QOP = "qop";
    public static final String PARAM_ALGORITHM = "algorithm";
    public static final String PARAM_NC = "nc";
    public static final String PARAM_URI = "uri";
    public static final String PARAM_OPAQUE = "opaque";
    public static final String PARAM_METHOD = "method";

    public static final String SCHEME = "Digest";

    // Some values that are commonly found in Authorization and WWW-Authenticate headers
    public static final String QOP_AUTH = "auth";
    public static final String QOP_AUTH_INT = "auth-int";
    public static final String ALGORITHM_MD5 = "md5";

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
            hashedPass = HexUtils.encodePasswd(ldapUser.getLogin(), ldapUser.getPassword(), HexUtils.REALM);
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

        String qop = (String)authParams.get(PARAM_QOP);
        String nonce = (String)authParams.get(PARAM_NONCE);

        //noinspection RedundantCast,RedundantCast
        String a2 = (String)authParams.get(PARAM_METHOD) + ":" +
          (String)authParams.get(PARAM_URI);

        String ha2 = HexUtils.encodeMd5Digest(HexUtils.getMd5Digest(a2.getBytes()));

        String serverDigestValue;
        if (!QOP_AUTH.equals(qop))
            serverDigestValue = hashedPass + ":" + nonce + ":" + ha2;
        else {
            String nc = (String)authParams.get(PARAM_NC);
            String cnonce = (String)authParams.get(PARAM_CNONCE);

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
