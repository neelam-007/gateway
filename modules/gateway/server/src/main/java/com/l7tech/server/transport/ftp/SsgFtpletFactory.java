package com.l7tech.server.transport.ftp;

import com.l7tech.gateway.common.transport.SsgConnector;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public class SsgFtpletFactory {

    @Autowired
    private FtpServerManager ftpServerManager;

    public SsgFtplet create() {
        return new SsgFtplet(ftpServerManager);
    }
}
