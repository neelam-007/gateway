package com.l7tech.identity.attribute;

import com.l7tech.identity.Identity;
import com.l7tech.identity.ldap.LdapGroup;
import com.l7tech.identity.ldap.LdapUser;
import com.l7tech.identity.ldap.LdapIdentity;

import javax.naming.directory.Attributes;
import javax.naming.directory.Attribute;
import javax.naming.directory.NoSuchAttributeException;
import javax.naming.NamingException;
import javax.naming.NamingEnumeration;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Maps
 */
public class LdapAttributeMapping extends IdentityMapping {
    private static final Logger logger = Logger.getLogger(LdapAttributeMapping.class.getName());
    private String attributeName;

    /**
     * TODO this method probably belongs in {@link com.l7tech.server.identity.ldap.LdapIdentityProvider}.
     * TODO what about passwords?
     * TODO what about certs?
     */
    public Object[] extractValues(Identity identity) {
        if (identity instanceof LdapUser && isValidForUsers() ||
            identity instanceof LdapGroup && isValidForGroups()) {
            LdapIdentity ldapIdentity = (LdapIdentity)identity;
            Attributes atts = ldapIdentity.getAttributes();
            if (atts == null || atts.size() < 1)
                return new Object[0];
            Attribute att = atts.get(attributeName);
            try {
                if (isMultivalued()) {
                    // TODO maybe caller should take care of cardinality!
                    ArrayList vals = new ArrayList();
                    for (NamingEnumeration ne = att.getAll(); ne.hasMore(); ) {
                        Object val = ne.next();
                        if (val != null) vals.add(val);
                    }
                    return vals.toArray();
                } else {
                    return new Object[] { att.get(0) };
                }
            } catch (NoSuchAttributeException e) {
                logger.log(Level.INFO, "No such attribute '" + attributeName + "' in identity '" + ldapIdentity.getDn() + "'");
                return new Object[0];
            } catch (NamingException e) {
                logger.log(Level.WARNING, "Couldn't get Attribute(s)", e);
                return new Object[0];
            }
        } else {
            String which = isValidForGroups() ? "groups" : "users";
            logger.fine("Not applicable - mapping is for " + which + "; identity is a " + identity.getClass().getName());
            return new Object[0];
        }
    }

    public String getAttributeName() {
        return attributeName;
    }

    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }
}
