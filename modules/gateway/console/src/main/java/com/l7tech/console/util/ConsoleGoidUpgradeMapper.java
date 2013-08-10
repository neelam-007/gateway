package com.l7tech.console.util;

import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.GoidUpgradeMapper;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Upgrade mapper for use within the SSM.
 */
public class ConsoleGoidUpgradeMapper extends GoidUpgradeMapper {
    private static final Logger logger = Logger.getLogger(ConsoleGoidUpgradeMapper.class.getName());

    /**
     * Contact the Gateway and download an updated OID -> GOID upgrade map.
     */
    public static void updatePrefixesFromGateway() {
        try {
            if (Registry.getDefault().isAdminContextPresent()) {
                Map<String, Long> goidMap = Registry.getDefault().getClusterStatusAdmin().getGoidUpgradeMap();
                if (goidMap != null)
                    new ConsoleGoidUpgradeMapper().setPrefixes(goidMap);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error obtaining GOID upgrade map: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        }
    }
}
