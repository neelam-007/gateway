package com.l7tech.server.transport.ftp;

import com.l7tech.common.ftp.FtpCommand;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.impl.FtpIoSession;
import org.apache.ftpserver.impl.FtpServerContext;

/**
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public interface FtpRequestProcessor {
    void process(FtpIoSession session, FtpServerContext context,
                 FtpRequest request, FtpCommand command) throws FtpException;

    void dispose();
}
