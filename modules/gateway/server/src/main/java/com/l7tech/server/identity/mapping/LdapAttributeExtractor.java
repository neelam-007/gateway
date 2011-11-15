/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.identity.mapping;

import com.l7tech.identity.Identity;
import com.l7tech.identity.ldap.LdapGroup;
import com.l7tech.identity.ldap.LdapIdentity;
import com.l7tech.identity.ldap.LdapUser;
import com.l7tech.identity.mapping.LdapAttributeMapping;
import com.l7tech.objectmodel.AttributeHeader;
import com.l7tech.server.identity.ldap.LdapUtils;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.PartialResultException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.NoSuchAttributeException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 */
public class LdapAttributeExtractor extends DefaultAttributeExtractor<LdapAttributeMapping> {
    private static final Logger logger = Logger.getLogger(LdapAttributeExtractor.class.getName());

    protected LdapAttributeExtractor(LdapAttributeMapping mapping) {
        super(mapping);
    }

    /**
     * TODO what about passwords?
     * TODO what about certs?
     */
    public Object[] extractValues(Identity identity) {
        Object[] supers = super.extractValues(identity);
        if (supers != null && supers.length > 0) return supers;

        AttributeHeader header = mapping.getAttributeConfig().getHeader();
        if (identity instanceof LdapIdentity) {
            LdapIdentity ldapIdentity = (LdapIdentity) identity;
            // This is only here because there's no Group.subjectDn property
            if (header == AttributeHeader.SUBJECT_DN) return a(ldapIdentity.getDn());
        }

        if (identity instanceof LdapUser && mapping.isValidForUsers() ||
            identity instanceof LdapGroup && mapping.isValidForGroups()) {
            LdapIdentity ldapIdentity = (LdapIdentity)identity;
            Attributes atts = ldapIdentity.getAttributes();
            if (atts == null || atts.size() < 1)
                return new Object[0];
            Attribute att = atts.get(mapping.getCustomAttributeName());
            try {
                //before proceeding make sure that the custom attribute exists for the selected LDAP
                if ( att == null ) {
                    //cannot find the attribute, so throw exception and allow catch block to handle it accordingly
                    throw new NoSuchAttributeException("Attribute '" + mapping.getCustomAttributeName() + "' not found in '" + ldapIdentity.getDn() +"'");
                }

                if (mapping.isMultivalued() && att.size() > 1) {
                    List<Object> vals = new ArrayList<Object>();
                    try {
                        for (NamingEnumeration ne = att.getAll(); ne.hasMore(); ) {
                            Object val = ne.next();
                            if (val != null) vals.add(val);
                        }
                    } catch (PartialResultException pre) {
                        LdapUtils.handlePartialResultException(pre);
                    }
                    return vals.toArray();
                } else {
                    return new Object[] { att.get(0) };
                }
            } catch (NoSuchAttributeException e) {
                logger.log(Level.INFO, "No such attribute '" + mapping.getCustomAttributeName() + "' in identity '" + ldapIdentity.getDn() + "'");
                return new Object[0];
            } catch (NamingException e) {
                logger.log(Level.WARNING, "Couldn't get Attribute(s)", e);
                return new Object[0];
            }
        } else {
            String which = mapping.isValidForGroups() ? "groups" : "users";
            logger.fine("Not applicable - mapping is for " + which + "; identity is a " + identity.getClass().getName());
            return new Object[0];
        }
    }

}
