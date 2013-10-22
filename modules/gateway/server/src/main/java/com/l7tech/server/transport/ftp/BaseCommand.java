package com.l7tech.server.transport.ftp;

import com.l7tech.gateway.common.transport.ftp.FtpMethod;
import org.apache.ftpserver.command.AbstractCommand;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.ftplet.FtpSession;
import org.apache.ftpserver.ftplet.Ftplet;
import org.apache.ftpserver.impl.FtpIoSession;
import org.apache.ftpserver.impl.FtpServerContext;

import java.io.IOException;

/**
 * @author nilic
 * @author jwilliams
 */
abstract class BaseCommand extends AbstractCommand {

    private FtpMethod ftpMethod;

    public BaseCommand(FtpMethod ftpMethod) {
        this.ftpMethod = ftpMethod;
    }

    @Override
    public void execute(FtpIoSession session, FtpServerContext context, FtpRequest request)
            throws IOException, FtpException {
        MessageProcessingFtplet ftplet = (MessageProcessingFtplet) context.getFtpletContainer();

        ftplet.onCommandStart(session.getFtpletSession(), request, ftpMethod);

//        execute(session.getFtpletSession(), (MessageProcessingFtplet) context.getFtpletContainer(), request);
    }

//    public void execute(FtpSession session, MessageProcessingFtplet ftplet, FtpRequest request)
//            throws FtpException, IOException {
//        ftplet.onCommandStart(session, request, ftpMethod); // TODO jwilliams: refactor all Ftplet methods to use FtpIoSession instead of FtpSession - gives access to Listener
//    }
}
