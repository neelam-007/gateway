package com.l7tech.console.auditalerts;

/**
 * User: megery
 * Date: Nov 2, 2006
 * Time: 4:41:19 PM
 */
public interface AuditWatcher {
    void auditsViewed();
    void alertsAvailable(boolean alertsAreAvailable, long alertTime);
    void alertSettingsChanged(AuditAlertConfigBean bean);
}
