package com.l7tech.console.util;

import com.l7tech.gateway.common.audit.LogonEvent;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.cluster.ClusterStatusAdmin;
import com.l7tech.gateway.common.security.TrustedCertAdmin;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectNotFoundException;
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
    private boolean defaultSslMutability;
    private boolean defaultCaMutability;

    /**
     * @return the current (possibly cached) default SSL alias.  Normally never null.
     */
    public String getDefaultSslAlias() {
        checkUpdate();
        return defaultSslAlias;
    }

    /**
     * @return the current (possibly cached) default CA alias.  May be null if no default CA alias is defined.
     */
    public String getDefaultCaAlias() {
        checkUpdate();
        return defaultCaAlias;
    }

    /**
     * Forget any cached information about the default SSL and CA certs.
     */
    public void invalidate() {
        defaultSslAlias = null;
        defaultCaAlias = null;
    }

    public boolean isDefaultSslKeyMutable() {
        checkUpdate();
        return defaultSslMutability;
    }

    public boolean isDefaultCaKeyMutable() {
        checkUpdate();
        return defaultCaMutability;
    }

    private void checkUpdate() {
        if (defaultSslAlias == null)
            updateDefaultAliases();
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
        defaultSslMutability = Registry.getDefault().getTrustedCertManager().isDefaultKeyMutable(TrustedCertAdmin.SpecialKeyType.SSL);
        defaultCaMutability = Registry.getDefault().getTrustedCertManager().isDefaultKeyMutable(TrustedCertAdmin.SpecialKeyType.CA);

        try {
            defaultSslAlias = Registry.getDefault().getTrustedCertManager().findDefaultKey(TrustedCertAdmin.SpecialKeyType.SSL).getAlias();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unable to determine default SSL alias using findDefaultKey: " + ExceptionUtils.getMessage(e), e);
            try {
                defaultSslAlias = getAlias(CLUSTER_PROP_DEFAULT_SSL, "SSL");
            } catch (FindException e1) {
                logger.log(Level.WARNING, "Unable to determine default SSL alias using cluster property: " + ExceptionUtils.getMessage(e), e);
            }
        }
        try {
            defaultCaAlias = Registry.getDefault().getTrustedCertManager().findDefaultKey(TrustedCertAdmin.SpecialKeyType.CA).getAlias();
        } catch (ObjectNotFoundException e) {
            logger.info("There is currently no default CA key");
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unable to determine default CA alias using findDefaultKey: " + ExceptionUtils.getMessage(e), e);
            try {
                defaultCaAlias = getAlias(CLUSTER_PROP_DEFAULT_CA, null);
            } catch (FindException e1) {
                logger.log(Level.WARNING, "Unable to determine default CA alias using cluster property: " + ExceptionUtils.getMessage(e), e);
            }
        }
    }

    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof LogonEvent) {
            invalidate();
        }
    }
}
