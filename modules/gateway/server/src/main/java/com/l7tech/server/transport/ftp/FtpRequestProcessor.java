package com.l7tech.server.transport.ftp;

import com.l7tech.common.ftp.FtpCommand;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.impl.FtpIoSession;
import org.apache.ftpserver.impl.FtpServerContext;

/**
 * Created with IntelliJ IDEA.
 * User: jwilliams
 * Date: 2/25/14
 * Time: 1:41 PM
 * To change this template use File | Settings | File Templates.
 */
public interface FtpRequestProcessor {
    void process(FtpIoSession session, FtpServerContext context,
                 FtpRequest request, FtpCommand command) throws FtpException;

    void dispose();
}
