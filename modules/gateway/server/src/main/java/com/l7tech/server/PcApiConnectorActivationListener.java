/*
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server;

import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.server.transport.SsgConnectorActivationListener;
import com.l7tech.util.FileUtils;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class PcApiConnectorActivationListener implements SsgConnectorActivationListener {
    private static final Logger logger = Logger.getLogger(PcApiConnectorActivationListener.class.getName());

    @Resource
    private ServerConfig serverConfig;

    @Override
    public void notifyActivated(final SsgConnector connector) {
        if (connector.offersEndpoint(SsgConnector.Endpoint.PC_NODE_API)) {
            final File varDir = serverConfig.getLocalDirectoryProperty(ServerConfig.PARAM_VAR_DIRECTORY, true);
            final File portFile = new File(varDir, "processControllerPort");
            logger.info("Writing Process Controller API port to " + portFile.getAbsolutePath());
            try {
                FileUtils.saveFileSafely(portFile.getAbsolutePath(), new FileUtils.Saver() {
                    @Override
                    public void doSave(FileOutputStream fos) throws IOException {
                        fos.write(Integer.toString(connector.getPort()).getBytes("UTF-8"));
                    }
                });
            } catch (IOException e) {
                logger.log(Level.WARNING, "Unable to write port file", e);
            }
        }
    }
}
