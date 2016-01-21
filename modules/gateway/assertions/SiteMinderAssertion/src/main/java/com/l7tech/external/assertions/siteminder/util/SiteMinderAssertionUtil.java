package com.l7tech.external.assertions.siteminder.util;

import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.server.policy.variable.ExpandVariables;
import org.apache.commons.lang.StringUtils;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SiteMinderAssertionUtil {

    public static final String SMCONTEXT = "smcontext";
    //private static final Pattern HOSTNAME_PATTERN = Pattern.compile("^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])$");
    /*
     *The original specification of hostnames in RFC 952, mandated that labels could not start with a digit or with a hyphen, and must not end with a hyphen.
     * However, a subsequent specification (RFC 1123) permitted hostname labels to start with digits.
     */
    private static final Pattern HOSTNAME_PATTERN = Pattern.compile("^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])$");

    //private constructor
    private  SiteMinderAssertionUtil() {
    }

    public static String extractContextVarValue(String var, Map<String, Object> varMap, Audit audit) {
        if (StringUtils.isNotBlank(var)) {
            return ExpandVariables.process(var, varMap, audit);
        }
        return null;
    }

    public static String getCn(String s) {
        try {
            LdapName ldapName = new LdapName(s);
            for(Rdn rdn : ldapName.getRdns()) {
                if(rdn.getType().equalsIgnoreCase("CN")) {
                    return rdn.getValue().toString(); //found cn
                }
            }
        } catch (InvalidNameException e) {
        }
        return null;
    }

    public static boolean validateHostname(String text) {
        Matcher m = HOSTNAME_PATTERN.matcher(text);
        return m.matches();
    }
}