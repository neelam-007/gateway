package com.l7tech.server.processcontroller;

import com.l7tech.util.ExceptionUtils;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author jbufu
 */
public class PCUtils {

    // - PUBLIC

    public static boolean isAppliance() {

        try {
            File applianceDir = new File(APPLIANCE_DIR);
            return applianceDir.exists() && applianceDir.isDirectory();
        } catch (Exception e) {
            logger.log(Level.INFO, "Error encountered while trying to determine if the host is an appliance; assuming it's a software install. " + ExceptionUtils.getMessage(e));
            return false;
        }
    }

    // - PRIVATE 

    private static final Logger logger = Logger.getLogger(PCUtils.class.getName());

    private static final String APPLIANCE_DIR = "/opt/SecureSpan/Appliance";

    private PCUtils() { }
}
