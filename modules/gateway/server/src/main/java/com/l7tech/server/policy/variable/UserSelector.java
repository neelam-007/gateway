/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy.variable;

import com.l7tech.identity.Identity;
import com.l7tech.identity.User;
import com.l7tech.identity.ldap.LdapUser;
import com.l7tech.policy.variable.Syntax;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.regex.Pattern;

/** @author alex */
public class UserSelector implements ExpandVariables.Selector<User> {
    private static final String CERT = "cert";
    private static final String PREFIX_CERT = CERT + ".";

    @Override
    public Selection select(String contextName, User user, String name, Syntax.SyntaxErrorHandler handler, boolean strict) {

        int dot = name.indexOf('.');
        String remainingName = dot < 1 ? null : (name.length() > dot ? name.substring(dot + 1) : null);

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
        } else if (Identity.ATTR_PROVIDER_OID.equalsIgnoreCase(name) || name.toLowerCase().startsWith( Identity.ATTR_PROVIDER_OID.toLowerCase() + "." )) {
            return new Selection(user.getProviderId(), remainingName);
        } else if ( "providerOid".equalsIgnoreCase(name) || name.toLowerCase().startsWith( "provideroid." )) {
            return new Selection(user.getProviderId(), remainingName);
        } else if ("id".equalsIgnoreCase(name)) {
            return new Selection(user.getId());
        } else if ("login".equalsIgnoreCase(name)) {
            return new Selection(user.getLogin());
        } else if ("firstName".equalsIgnoreCase(name)) {
            return new Selection(user.getFirstName());
        } else if ("lastName".equalsIgnoreCase(name)) {
            return new Selection(user.getLastName());
        } else if ("email".equalsIgnoreCase(name)) {
            return new Selection(user.getEmail());
        } else if ("department".equalsIgnoreCase(name)) {
            return new Selection(user.getDepartment());
        } else if ("subjectDn".equalsIgnoreCase(name)) {
            if (user instanceof LdapUser) {
                return new Selection(((LdapUser)user).getDn());
            } else {
                return new Selection(user.getSubjectDn());
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