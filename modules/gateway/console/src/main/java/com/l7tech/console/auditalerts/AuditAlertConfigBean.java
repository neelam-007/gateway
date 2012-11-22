package com.l7tech.console.auditalerts;

import com.l7tech.console.util.SsmPreferences;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User: megery
 * Date: Nov 8, 2006
 * Time: 2:07:21 PM
 */
public class AuditAlertConfigBean {
    public static final String AUDIT_ALERT_ENABLED_PREFERENCE_KEY = "com.l7tech.auditalerts.enablealerts";
    public static final String AUDIT_ALERT_LEVEL_PREFERENCE_KEY = "com.l7tech.auditalerts.warninglevel";
    public static final String AUDIT_ALERT_INTERVAL_PREFERENCE_KEY = "com.l7tech.auditalerts.checkinterval";
    public static final int DEFAULT_CHECK_INTERVAL = 30;

    private static final Logger logger = Logger.getLogger(AuditAlertConfigBean.class.getName());

    private boolean isEnabled = true;
    private Level auditAlertLevel = Level.WARNING;
    private int auditCheckInterval = DEFAULT_CHECK_INTERVAL;
    private SsmPreferences preferences;

    public AuditAlertConfigBean(SsmPreferences ssmPrefs) {
        this.preferences = ssmPrefs;
        if (preferences != null) {
            String temp = preferences.getString(AUDIT_ALERT_ENABLED_PREFERENCE_KEY);
            if (StringUtils.isNotEmpty(temp))
                isEnabled = Boolean.valueOf(temp);

            temp = preferences.getString(AUDIT_ALERT_LEVEL_PREFERENCE_KEY);
            if (StringUtils.isNotEmpty(temp))
                auditAlertLevel = Level.parse(temp);

            temp = preferences.getString(AUDIT_ALERT_INTERVAL_PREFERENCE_KEY);
            if (StringUtils.isNotEmpty(temp)) {
                try {
                    auditCheckInterval = Integer.valueOf(temp);
                } catch (NumberFormatException nfe) {
                    auditCheckInterval = DEFAULT_CHECK_INTERVAL;
                }
            }
        }
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    public Level getAuditAlertLevel() {
        return auditAlertLevel;
    }

    public void setAuditAlertLevel(Level auditAlertLevel) {
        this.auditAlertLevel = auditAlertLevel;
    }

    public int getAuditCheckInterval() {
        return auditCheckInterval;
    }

    public void setAuditCheckInterval(int auditCheckInterval) {
        this.auditCheckInterval = auditCheckInterval;
    }

    public void savePreferences() throws IOException {
        if (preferences != null) {
            if(!preferences.getString(AUDIT_ALERT_ENABLED_PREFERENCE_KEY).equals(String.valueOf(isEnabled()))) {
                preferences.putProperty(AUDIT_ALERT_ENABLED_PREFERENCE_KEY, String.valueOf(isEnabled()));
                logger.info("Audit Alerts " + (isEnabled() ? "enabled." : "disabled."));
            }

            if(!preferences.getString(AUDIT_ALERT_INTERVAL_PREFERENCE_KEY).equals(String.valueOf(getAuditCheckInterval()))) {
                preferences.putProperty(AUDIT_ALERT_INTERVAL_PREFERENCE_KEY, String.valueOf(getAuditCheckInterval()));
                logger.info("Audit Alert Check Interval set to " + getAuditCheckInterval() + " seconds.");
            }

            if(!preferences.getString(AUDIT_ALERT_LEVEL_PREFERENCE_KEY).equals(getAuditAlertLevel().getName())) {
                preferences.putProperty(AUDIT_ALERT_LEVEL_PREFERENCE_KEY, getAuditAlertLevel().getName());
                logger.info("Audit Alert Level set to " + getAuditAlertLevel().getName() + ".");
            }

            preferences.store();
        }
    }
}
