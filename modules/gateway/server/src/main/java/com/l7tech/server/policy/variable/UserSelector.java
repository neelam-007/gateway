/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy.variable;

import com.l7tech.identity.User;
import com.l7tech.identity.ldap.LdapUser;
import com.l7tech.policy.variable.Syntax;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/** @author alex */
public class UserSelector implements ExpandVariables.Selector<User> {
    private static final String CERT = "cert";
    private static final String PREFIX_CERT = CERT + ".";

    @Override
    public Selection select(User user, String name, Syntax.SyntaxErrorHandler handler, boolean strict) {
        if (name.startsWith(PREFIX_CERT) || name.equals(CERT)) {
            if (user instanceof LdapUser) {
                LdapUser ldapUser = (LdapUser)user;
                try {
                    final X509Certificate cert = ldapUser.getCertificate();
                    if (cert == null) return new Selection(null);
                    // If they ask for the cert alone, return the cert with no remaining name.  If they wanted cert.*, return the cert plus the remaining name.
                    return new Selection(cert, name.equals(CERT) ? null : name.substring(PREFIX_CERT.length()));
                } catch (CertificateException e) {
                    throw new RuntimeException(e); // Can't happen
                }
            } else {
                return new Selection(null); // TODO support user.cert for other types of user?
            }
        } else {
            return null;
        }
    }

    @Override
    public Class<User> getContextObjectClass() {
        return User.class;
    }
}
