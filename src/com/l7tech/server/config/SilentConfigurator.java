package com.l7tech.server.config;

import com.l7tech.server.config.db.DBInformation;

import java.util.logging.Logger;

/**
 * User: megery
 * Date: Dec 4, 2007
 * Time: 1:56:02 PM
 */
public class SilentConfigurator {
    private static final Logger logger = Logger.getLogger(SilentConfigurator.class.getName());

    public void loadConfigFromDb(DBInformation dbinfo) {
        logger.info("Connecting to Database using " + dbinfo.getUsername() + "@" + dbinfo.getHostname() + "/" + dbinfo.getDbName());
    }

    public void decryptConfigSettings(String passphrase) {
        logger.info("Decrypting the configuration data using " + passphrase);
    }
}
