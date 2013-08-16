package com.l7tech.server.event.system;

import java.util.logging.Level;

/**
 * @author Jamie Williams - wilja33 - jamie.williams2@ca.com
 */
public class LicenseChangeEvent extends LicenseEvent {

    public LicenseChangeEvent(Object source, Level level, String action, String message) {
        super(source, level, action, message);
    }
}
