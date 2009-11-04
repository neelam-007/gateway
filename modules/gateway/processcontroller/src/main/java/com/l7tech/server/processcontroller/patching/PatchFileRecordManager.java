package com.l7tech.server.processcontroller.patching;

import com.l7tech.server.processcontroller.ConfigService;

import javax.annotation.Resource;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.FileHandler;
import java.io.IOException;

import org.springframework.beans.factory.InitializingBean;

/**
 * (Log)File-based implementation for the PatchRecordManager
 *
 * @author jbufu
 */
public class PatchFileRecordManager implements PatchRecordManager, InitializingBean {

    // - PUBLIC

    public PatchFileRecordManager() { }

    public PatchFileRecordManager(String patchLog) throws IOException {
        init(patchLog);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        init(configService.getPatchesLog());
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

    private void init(String patchLog) throws IOException {
        FileHandler fileHandler = new FileHandler(patchLog, true);
        patchLogger = Logger.getLogger(PATCH_LOGGER_NAME);
        patchLogger.setUseParentHandlers(false);
        patchLogger.addHandler(fileHandler);
    }
}
