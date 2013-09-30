package com.l7tech.server.transport.ftp;

import com.l7tech.gateway.common.transport.ftp.FtpMethod;
import org.apache.ftpserver.FtpSessionImpl;
import org.apache.ftpserver.command.AbstractCommand;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpReplyOutput;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.listener.Connection;

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
    public void execute(Connection connection,
                        FtpRequest request,
                        FtpSessionImpl session,
                        FtpReplyOutput out)
            throws IOException, FtpException {
        MessageProcessingFtplet ftplet = (MessageProcessingFtplet) connection.getServerContext().getFtpletContainer();

        ftplet.onCommandStart(session, request, out, ftpMethod);
    }
}
