/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.identity.msad;

import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.identity.ldap.LdapIdentityProviderServer;
import com.l7tech.logging.LogManager;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;

/**
 * @author alex
 * @version $Revision$
 */
public class MsadIdentityProviderServer extends LdapIdentityProviderServer {
    public void initialize( IdentityProviderConfig config ) {
                if (config.type() != IdentityProviderType.MSAD) {
            throw new IllegalArgumentException("Expecting MSAD config type");
        }
        cfg = config;
        groupManager = new MsadGroupManagerServer(cfg);
        userManager = new MsadUserManagerServer(cfg);
        logger = LogManager.getInstance().getSystemLogger();
        try {
            _md5 = MessageDigest.getInstance( "MD5" );
        } catch (NoSuchAlgorithmException e) {
            logger.log( Level.SEVERE, e.getMessage(), e );
            throw new RuntimeException( e );
        }
    }
}
