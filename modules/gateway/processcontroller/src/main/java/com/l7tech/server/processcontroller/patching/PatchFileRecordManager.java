package com.l7tech.server.processcontroller.patching;

import com.l7tech.server.processcontroller.ConfigService;

import javax.annotation.Resource;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.FileHandler;

import org.springframework.beans.factory.InitializingBean;

/**
 * (Log)File-based implementation for the PatchRecordManager
 *
 * @author jbufu
 */
public class PatchFileRecordManager implements PatchRecordManager, InitializingBean {

    // - PUBLIC

    @Override
    public void afterPropertiesSet() throws Exception {
        FileHandler fileHandler = new FileHandler(configService.getPatchesLog(), true);
        patchLogger = Logger.getLogger(PATCH_LOGGER_NAME);
        patchLogger.setUseParentHandlers(false);
        patchLogger.addHandler(fileHandler);
    }

    @Override
    public void save(PatchRecord record) {
        patchLogger.log(Level.INFO, record.toString());
    }

    // - PRIVATE

    private Logger patchLogger;

    private static final String PATCH_LOGGER_NAME = "patch.record.logger";

    @Resource
    private ConfigService configService;
}
