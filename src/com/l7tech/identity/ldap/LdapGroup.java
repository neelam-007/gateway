package com.l7tech.identity.ldap;

import com.l7tech.identity.internal.imp.GroupImp;
import com.l7tech.objectmodel.EntityHeader;

import java.util.Set;
import java.util.Iterator;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 18, 2003
 *
 */
public class LdapGroup extends GroupImp {
    public String getDN() {
        return dn;
    }

    public void setDN(String dn) {
        this.dn = dn;
    }

    public String toString() {
        String out = "com.l7tech.identity.ldap.LdapGroup."
                + "\n\tName=" + getName()
                + "\n\tDN=" + getDN();

        Set membershipHeaders = getMemberHeaders();
        Iterator i = membershipHeaders.iterator();
        out += "\n\tMembers: ";
        while (i.hasNext()) {
            EntityHeader header = (EntityHeader)i.next();
            out += header.getName() + "[" + header.getStrId() + "]" + ", ";
        }
        return out;
    }

    // ************************************************
    // PRIVATES
    // ************************************************

    private String dn = null;
}
