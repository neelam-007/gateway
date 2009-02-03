package com.l7tech.server.ems.monitoring;

/**
 * The exception thrown when there exists an invalid value assigned to a system monitoring setup setting.
 *
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Feb 2, 2009
 * @since Enterprise Manager 1.0
 */
public class InvalidMonitoringSetupSettingException extends Exception {
    public InvalidMonitoringSetupSettingException(String message) {
        super(message);
    }
}
