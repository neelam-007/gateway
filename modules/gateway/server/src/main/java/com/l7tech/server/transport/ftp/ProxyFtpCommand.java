package com.l7tech.server.transport.ftp;

import com.l7tech.common.ftp.FtpCommand;
import org.apache.ftpserver.command.AbstractCommand;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.impl.FtpIoSession;
import org.apache.ftpserver.impl.FtpServerContext;

import java.io.IOException;

/**
 * @author jwilliams
 */
class ProxyFtpCommand extends AbstractCommand {
    private FtpCommand command;

    public ProxyFtpCommand(FtpCommand command) {
        this.command = command;
    }

    @Override
    public void execute(FtpIoSession session, FtpServerContext context, FtpRequest request)
            throws IOException, FtpException {
        SsgFtpServerContext ssgContext = (SsgFtpServerContext) context;

        FtpRequestProcessor requestProcessor = ssgContext.getRequestProcessor();

        requestProcessor.process(session, context, request, command);
    }
}
