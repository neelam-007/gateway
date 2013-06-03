package com.l7tech.external.assertions.ftprouting.server;

import org.apache.ftpserver.FtpSessionImpl;
import org.apache.ftpserver.command.AbstractCommand;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpReplyOutput;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.listener.Connection;

import java.io.IOException;

/**
 * @author nilic
 */
public abstract class BaseCommand extends AbstractCommand {

    private FtpMethod ftpMethod;

    public BaseCommand() { /* compiled code */ }

    public BaseCommand(FtpMethod ftpMethod) {
        this.ftpMethod = ftpMethod;
    }

    /**
     * Execute command.
     */
    public void execute(Connection connection,
                        FtpRequest request,
                        FtpSessionImpl session,
                        FtpReplyOutput out)
            throws IOException, FtpException {

        MessageProcessingFtpletSubsystem ftplet = (MessageProcessingFtpletSubsystem) connection.getServerContext().getFtpletContainer();
        ftplet.onCommandStart(session, request, out, ftpMethod);
    }
}
