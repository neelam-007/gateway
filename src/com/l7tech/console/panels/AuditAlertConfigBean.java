package com.l7tech.console.panels;

import org.apache.commons.lang.StringUtils;

import java.util.logging.Level;
import java.util.Properties;
import java.io.IOException;

import com.l7tech.console.util.TopComponents;
import com.l7tech.console.util.SsmPreferences;

/**
 * User: megery
 * Date: Nov 8, 2006
 * Time: 2:07:21 PM
 */
public class AuditAlertConfigBean {
    public static final String AUDIT_ALERT_ENABLED_PREFERENCE_KEY = "com.l7tech.auditalerts.enablealerts";
    public static final String AUDIT_ALERT_LEVEL_PREFERENCE_KEY = "com.l7tech.auditalerts.warninglevel";
    public static final String AUDIT_ALERT_INTERVAL_PREFERENCE_KEY = "com.l7tech.auditalerts.checkinterval";

    private boolean isEnabled = true;
    private Level auditAlertLevel = Level.WARNING;
    private int auditCheckInterval = 10;
    private SsmPreferences preferences;

    public AuditAlertConfigBean() {
        this(null);
    }

    public AuditAlertConfigBean(SsmPreferences ssmPrefs) {
        this.preferences = ssmPrefs;
        if (preferences != null) {
            String temp = preferences.getString(AUDIT_ALERT_ENABLED_PREFERENCE_KEY);
            if (StringUtils.isNotEmpty(temp)) isEnabled = Boolean.valueOf(temp);

            temp = preferences.getString(AUDIT_ALERT_LEVEL_PREFERENCE_KEY);
            if (StringUtils.isNotEmpty(temp)) auditAlertLevel = Level.parse(temp);

            temp = preferences.getString(AUDIT_ALERT_INTERVAL_PREFERENCE_KEY);
            if (StringUtils.isNotEmpty(temp)) auditCheckInterval = Integer.valueOf(temp); 
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
            preferences.putProperty(AUDIT_ALERT_ENABLED_PREFERENCE_KEY, String.valueOf(isEnabled()));
            preferences.putProperty(AUDIT_ALERT_INTERVAL_PREFERENCE_KEY, String.valueOf(getAuditCheckInterval()));
            preferences.putProperty(AUDIT_ALERT_LEVEL_PREFERENCE_KEY, getAuditAlertLevel().getLocalizedName());
            preferences.store();
        }
    }
}
