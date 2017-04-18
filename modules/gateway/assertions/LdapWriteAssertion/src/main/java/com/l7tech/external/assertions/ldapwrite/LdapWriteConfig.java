package com.l7tech.external.assertions.ldapwrite;

import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by chaja24 on 2/1/2017.
 * This module configuration related information.
 */
public final class LdapWriteConfig {

    public static final String VARIABLE_OUTPUT_SUFFIX_ERROR_MSG = ".error.msg";
    public static final String LDAP_IDENTITY_PROVIDER_LIST_WITH_WRITE_ACCESS = "ldap.identity.provider.write.permission";
    public static final String CLUSTER_PROP_LDAP_IDENTITY_PROVIDER_LIST_WITH_WRITE_ACCESS = "ldapIdentityProviderWritePermission";

    private static final Logger logger = Logger.getLogger(LdapWriteConfig.class.getName());


    private LdapWriteConfig() {
    }

    public static Properties loadPropertyFile(String propFile) {

        Properties prop = new Properties();

        try (InputStream stream = LdapWriteAssertion.class.getClassLoader().getResourceAsStream(propFile)) {
            if (stream == null) {
                logger.log(Level.WARNING, "Unable to load the property file:{0}. The file does not exist.", propFile);
            } else {
                prop.load(stream);

            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Unable to load the property file:{0}. Reason:{1}", new String[]{propFile, e.toString()});
        }
        return prop;
    }


    public static String getProperty(Properties prop, String propKey, String propDefaultString) {

        String stringValue = prop.getProperty(propKey);
        if (StringUtils.isEmpty(stringValue)) {
            stringValue = propDefaultString;
            logger.log(Level.WARNING, "Could not load property:{0}. Using default:\"{1}\"", new String[]{propKey, propDefaultString});
        }
        return stringValue;
    }
}
