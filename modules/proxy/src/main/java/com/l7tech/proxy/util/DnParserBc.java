package com.l7tech.proxy.util;

import com.l7tech.common.io.CertUtils;

import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.asn1.DERObjectIdentifier;

import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Contains CertUtils utility methods that require the Bouncy Castle library, and hence shouldn't be included
 * with the SSM applet.  This class is used by the Bridge and must run under JRE 1.4.
 */
public class DnParserBc implements CertUtils.DnParser {
    public DnParserBc() {
        // Force early failure if parser isn't present
        dnToAttributeMap("cn=blah");
    }

    public Map dnToAttributeMap(String dn) {
        X509Name x509name = new X509Name(dn);
        Map map = new HashMap();
        for (int i = 0; i < x509name.getOIDs().size(); i++ ) {
            final DERObjectIdentifier oid = (DERObjectIdentifier)x509name.getOIDs().get(i);

            String name = (String)X509Name.DefaultSymbols.get(oid);
            if (name == null) name = (String)X509Name.RFC2253Symbols.get(oid);
            if (name == null) name = oid.getId();

            List values = (List) map.get(name);
            if ( values == null ) {
                values = new ArrayList();
                map.put(name, values);
            }
            String value = (String)x509name.getValues().get(i);
            values.add(value);
        }
        return map;
    }

}
