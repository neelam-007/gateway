package com.l7tech.common.io;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import java.util.*;

/**
 * Contains CertUtils utility methods that require Java 1.5 or higher, and hence can't be included with the Bridge.
 */
class DnParserJava15 implements CertUtils.DnParser {
    public DnParserJava15() {
        // Force early failure if parser not working
        dnToAttributeMap("cn=blah");
    }

    public Map dnToAttributeMap(String dn) {
        LdapName name = null;
        try {
            name = new LdapName(dn);
        } catch (InvalidNameException e) {
            throw new IllegalArgumentException("Invalid DN", e);
        }

        Map map = new HashMap();
        List rdns = name.getRdns();
        for (Iterator i = rdns.iterator(); i.hasNext();) {
            Rdn rdn = (Rdn)i.next();
            String type = rdn.getType().toUpperCase();

            List values = (List)map.get(type);
            if (values == null) {
                values = new ArrayList();
                map.put(type, values);
            }

            values.add(rdn.getValue());
        }

        return map;
    }
}
