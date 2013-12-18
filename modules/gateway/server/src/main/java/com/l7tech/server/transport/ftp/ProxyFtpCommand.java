package com.l7tech.server.transport.ftp;

import com.l7tech.gateway.common.transport.ftp.FtpMethod;
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
    private FtpMethod ftpMethod;

    public ProxyFtpCommand(FtpMethod ftpMethod) {
        this.ftpMethod = ftpMethod;
    }

    @Override
    public void execute(FtpIoSession session, FtpServerContext context, FtpRequest request)
            throws IOException, FtpException {
        SsgFtpServerContext ssgContext = (SsgFtpServerContext) context;

        FtpRequestProcessor requestProcessor = ssgContext.getRequestProcessor();

        requestProcessor.process(session, context, request, ftpMethod);
    }
}
