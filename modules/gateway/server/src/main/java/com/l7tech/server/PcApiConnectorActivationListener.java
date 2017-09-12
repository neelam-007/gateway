/*
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server;

import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.server.event.system.ReadyForMessages;
import com.l7tech.server.transport.SsgConnectorActivationEvent;
import com.l7tech.server.util.PostStartupApplicationListener;
import com.l7tech.util.Charsets;
import com.l7tech.util.FileUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The Process Controller API connector activation listener records the API port for PC use.
 *
 * <p>If there is no listener with the PC feature enabled then an empty file is written
 * so that the PC is aware that the port is disabled (as opposed to just not working).</p>
 */
public final class PcApiConnectorActivationListener implements PostStartupApplicationListener, InitializingBean {

    private void notifyActivated( final SsgConnector connector ) {
        if (connector.offersEndpoint(SsgConnector.Endpoint.PC_NODE_API)) {
            sawApiConnector = true;
            final File portFile = getApiPortFile();
            logger.info("Writing Process Controller API port to " + portFile.getAbsolutePath());
            try {
                FileUtils.saveFileSafely(portFile.getAbsolutePath(), new FileUtils.Saver() {
                    @Override
                    public void doSave(FileOutputStream fos) throws IOException {
                        fos.write(Integer.toString(connector.getPort()).getBytes(Charsets.UTF8));
                    }
                });
            } catch (IOException e) {
                logger.log(Level.WARNING, "Unable to write port file", e);
            }
        }
    }

    @Override
    public void onApplicationEvent( final ApplicationEvent event ) {
        if ( event instanceof ReadyForMessages ) {
            if ( !sawApiConnector ) {
                final File portFile = getApiPortFile();
                logger.info("Writing no Process Controller API port to " + portFile.getAbsolutePath());
                try {
                    FileUtils.touch(portFile);
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Unable to write port file", e);
                }
            }
        } else if ( event instanceof SsgConnectorActivationEvent ) {
            SsgConnectorActivationEvent scae  = ( SsgConnectorActivationEvent )event;
            notifyActivated( scae.getConnector() );
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        initApiPort();
    }

    private static final Logger logger = Logger.getLogger(PcApiConnectorActivationListener.class.getName());

    private boolean sawApiConnector = false;

    private File getApiPortFile() {
        final File varDir = serverConfig().getLocalDirectoryProperty( ServerConfigParams.PARAM_VAR_DIRECTORY, true);
        return new File(varDir, "processControllerPort");
    }

    private void initApiPort() {
        final File portFile = getApiPortFile();
        if ( portFile.exists() ) {
            logger.info("Deleting old Process Controller API port file to " + portFile.getAbsolutePath());
        }
        FileUtils.deleteFileSafely(portFile.getAbsolutePath());
    }

    private ServerConfig serverConfig() {
        return ServerConfig.getInstance();
    }
}
