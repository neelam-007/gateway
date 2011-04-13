package com.l7tech.console.util;

import com.l7tech.gateway.common.audit.LogonEvent;
import com.l7tech.gateway.common.security.TrustedCertAdmin;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.util.ExceptionUtils;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Client-side utility class for keeping track of the default SSL and CA aliases.
 */
public class DefaultAliasTracker implements ApplicationListener {
    private static final Logger logger = Logger.getLogger(DefaultAliasTracker.class.getName());

    public static final String CLUSTER_PROP_DEFAULT_SSL = "keyStore.defaultSsl.alias";
    public static final String CLUSTER_PROP_DEFAULT_CA = "keyStore.defaultCa.alias";
    public static final String CLUSTER_PROP_AUDIT_VIEWER = "keyStore.auditViewer.alias";

    private SsgKeyEntry defaultSslAlias = null;
    private SsgKeyEntry defaultCaAlias = null;
    private SsgKeyEntry defaultAuditViewerAlias = null;
    private boolean defaultSslMutability;
    private boolean defaultCaMutability;
    private boolean defaultAuditViewerMutability;

    /**
     * @return the current (possibly cached) default SSL alias.  Normally never null.
     */
    public String getDefaultSslAlias() {
        checkUpdate();
        return defaultSslAlias == null ? null : defaultSslAlias.getAlias();
    }

    /**
     * @return the current (possibly cached) default CA alias.  May be null if no default CA alias is defined.
     */
    public String getDefaultCaAlias() {
        checkUpdate();
        return defaultCaAlias == null ? null : defaultCaAlias.getAlias();
    }

    public String getDefaultAuditViewerAlias() {
        checkUpdate();
        return defaultAuditViewerAlias == null ? null : defaultAuditViewerAlias.getAlias();
    }

    /**
     * Forget any cached information about the default SSL and CA certs.
     */
    public void invalidate() {
        defaultSslAlias = null;
        defaultCaAlias = null;
        defaultAuditViewerAlias = null;
    }

    public boolean isDefaultSslKeyMutable() {
        checkUpdate();
        return defaultSslMutability;
    }

    public boolean isDefaultCaKeyMutable() {
        checkUpdate();
        return defaultCaMutability;
    }

    public boolean isDefaultAuditViewerMutable() {
        checkUpdate();
        return defaultAuditViewerMutability;
    }

    private void checkUpdate() {
        if (defaultSslAlias == null)
            updateDefaultAliases();
    }

    private void updateDefaultAliases() {
        defaultSslMutability = Registry.getDefault().getTrustedCertManager().isDefaultKeyMutable(TrustedCertAdmin.SpecialKeyType.SSL);
        defaultCaMutability = Registry.getDefault().getTrustedCertManager().isDefaultKeyMutable(TrustedCertAdmin.SpecialKeyType.CA);
        defaultAuditViewerMutability = Registry.getDefault().getTrustedCertManager().isDefaultKeyMutable(TrustedCertAdmin.SpecialKeyType.AUDIT_VIEWER);

        try {
            defaultSslAlias = Registry.getDefault().getTrustedCertManager().findDefaultKey(TrustedCertAdmin.SpecialKeyType.SSL);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unable to determine default SSL alias using findDefaultKey: " + ExceptionUtils.getMessage(e), e);
        }
        try {
            defaultCaAlias = Registry.getDefault().getTrustedCertManager().findDefaultKey(TrustedCertAdmin.SpecialKeyType.CA);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unable to determine default CA alias using findDefaultKey: " + ExceptionUtils.getMessage(e), e);
        }
        try {
            defaultAuditViewerAlias = Registry.getDefault().getTrustedCertManager().findDefaultKey(TrustedCertAdmin.SpecialKeyType.AUDIT_VIEWER);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unable to determine default audit viewer alias using findDefaultKey: " + ExceptionUtils.getMessage(e), e);
        }
    }

    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof LogonEvent) {
            invalidate();
        }
    }
}
