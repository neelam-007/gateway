package com.l7tech.server.processcontroller;

import com.l7tech.common.io.ProcResult;
import com.l7tech.common.io.ProcUtils;
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
        // rpm -q ssg-appliance
        try {
            File rpm = new File("/bin/rpm");
            if (! rpm.exists())
                rpm = new File("/usr/bin/rpm");
            ProcResult result = ProcUtils.exec(rpm, new String[] {"-q", "ssg-appliance"});
            logger.log(Level.FINE, "Appliance check (rpm -q ssg-appliance) returned: " + result.getExitStatus());
            return result.getExitStatus() == 0;
        } catch (Exception e) {
            logger.log(Level.INFO, "Error encountered while trying to determine if the host is an appliance: " + ExceptionUtils.getMessage(e));
            return false;
        }
    }

    // - PRIVATE 

    private static final Logger logger = Logger.getLogger(PCUtils.class.getName());

    private PCUtils() { }
}
