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

    public Map<String, List<String>> dnToAttributeMap(String dn) {
        final LdapName name;
        try {
            name = new LdapName(dn);
        } catch (InvalidNameException e) {
            throw new IllegalArgumentException("Invalid DN", e);
        }

        Map<String, List<String>> map = new HashMap<String, List<String>>();
        List<Rdn> rdns = name.getRdns();
        for (Rdn rdn : rdns) {
            String type = rdn.getType().toUpperCase();

            List<String> values = map.get(type);
            if (values == null) {
                values = new ArrayList<String>();
                map.put(type, values);
            }

            values.add(rdn.getValue().toString());
        }

        return map;
    }
}
