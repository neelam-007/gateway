package com.l7tech.server.transport.ftp;

import com.l7tech.gateway.common.transport.ftp.FtpMethod;
import org.apache.ftpserver.command.AbstractCommand;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.impl.FtpIoSession;
import org.apache.ftpserver.impl.FtpServerContext;

import java.io.IOException;

/**
 * @author nilic
 * @author jwilliams
 */
abstract class BaseCommand extends AbstractCommand { // TODO jwilliams: rename to ProxyFtpCommand or something? Add to command factory, creating each instance with the ftpMethod constructor

    private FtpMethod ftpMethod;

    public BaseCommand(FtpMethod ftpMethod) {
        this.ftpMethod = ftpMethod;
    }

    @Override
    public void execute(FtpIoSession session, FtpServerContext context, FtpRequest request)
            throws IOException, FtpException {
        SsgFtpServerContext ssgContext = (SsgFtpServerContext) context;

        FtpRequestProcessor requestProcessor = ssgContext.getRequestProcessor();

        requestProcessor.process(getFtpMethod(), request, session);
    }

    protected FtpMethod getFtpMethod() {
        return ftpMethod;
    }
}
