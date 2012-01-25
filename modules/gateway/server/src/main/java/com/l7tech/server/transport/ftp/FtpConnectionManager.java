package com.l7tech.server.transport.ftp;

import org.apache.ftpserver.FtpSessionImpl;
import org.apache.ftpserver.ftplet.FtpSession;
import org.apache.ftpserver.listener.Connection;
import org.apache.ftpserver.listener.ConnectionManagerImpl;

/**
 * Extends standard connection manager for session initialization.
 */
public class FtpConnectionManager extends ConnectionManagerImpl {

    @Override
    public void newConnection( final Connection connection ) {
        final FtpSession ftpSession = connection.getSession();
        // TODO This addresses bug 11570 and can be removed (along with the rest of this class) once we update the Apache FTP library
        if ( ftpSession instanceof FtpSessionImpl ) {
            ((FtpSessionImpl)ftpSession).updateLastAccessTime();
        }
        super.newConnection( connection );
    }
}
