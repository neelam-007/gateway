package com.l7tech.external.assertions.siteminder.util;

import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.server.policy.variable.ExpandVariables;
import org.apache.commons.lang.StringUtils;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import java.util.Map;

public class SiteMinderAssertionUtil {

    public static final String SMCONTEXT = "smcontext";

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
}