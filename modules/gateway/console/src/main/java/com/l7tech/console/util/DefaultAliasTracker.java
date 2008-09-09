package com.l7tech.console.util;

import com.l7tech.gateway.common.audit.LogonEvent;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.cluster.ClusterStatusAdmin;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.ExceptionUtils;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Client-side utility class for keeping track of the default SSL and CA aliases.
 */
public class DefaultAliasTracker implements ApplicationListener {
    private static final Logger logger = Logger.getLogger(DefaultAliasTracker.class.getName());

    public static final String CLUSTER_PROP_DEFAULT_SSL = "keyStore.defaultSsl.alias";
    public static final String CLUSTER_PROP_DEFAULT_CA = "keyStore.defaultCa.alias";

    private static final Pattern KEYSTORE_ID_AND_ALIAS_PATTERN = Pattern.compile("^(-?\\d+):(.*)$");

    private String defaultSslAlias = null;
    private String defaultCaAlias = null;

    /**
     * @return the current (possibly cached) default SSL alias.  Normally never null.
     */
    public String getDefaultSslAlias() {
        if (defaultSslAlias == null) updateDefaultAliases();
        return defaultSslAlias;
    }

    /**
     * @return the current (possibly cached) default CA alias.  May be null if no default CA alias is defined.
     */
    public String getDefaultCaAlias() {
        if (defaultSslAlias == null) updateDefaultAliases();
        return defaultCaAlias;
    }

    /**
     * Forget any cached information about the default SSL and CA certs.
     */
    public void invalidate() {
        defaultSslAlias = null;
        defaultCaAlias = null;
    }

    private String getAlias(String clusterPropertyName, String defaultVal) throws FindException {
        final ClusterStatusAdmin csa = Registry.getDefault().getClusterStatusAdmin();
        ClusterProperty prop = csa.findPropertyByName(clusterPropertyName);
        if (prop == null)
            return defaultVal;
        String value = prop.getValue();
        if (value == null)
            return defaultVal;
        Matcher matcher = KEYSTORE_ID_AND_ALIAS_PATTERN.matcher(value);
        if (!matcher.matches())
            return value;
        return matcher.group(2);
    }

    private void updateDefaultAliases() {
        try {
            defaultSslAlias = getAlias(CLUSTER_PROP_DEFAULT_SSL, "SSL");
        } catch (FindException e) {
            logger.log(Level.WARNING, "Unable to determine default SSL alias: " + ExceptionUtils.getMessage(e), e);
        }
        try {
            defaultCaAlias = getAlias(CLUSTER_PROP_DEFAULT_CA, null);
        } catch (FindException e) {
            logger.log(Level.WARNING, "Unable to determine default CA alias: " + ExceptionUtils.getMessage(e), e);
        }
    }

    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof LogonEvent) {
            invalidate();
        }
    }
}
