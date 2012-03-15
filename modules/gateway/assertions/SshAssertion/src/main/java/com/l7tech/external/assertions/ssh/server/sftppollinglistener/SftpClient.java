package com.l7tech.external.assertions.ssh.server.sftppollinglistener;

import com.jscape.inet.sftp.Sftp;
import com.jscape.inet.sftp.SftpConfiguration;
import com.jscape.inet.sftp.SftpException;
import com.jscape.inet.sftp.SftpFileNotFoundException;
import com.jscape.inet.ssh.transport.TransportException;
import com.jscape.inet.ssh.util.SshParameters;
import com.l7tech.external.assertions.ssh.server.client.SshClientConfiguration;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions.UnaryThrows;
import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.external.assertions.ssh.server.SshAssertionMessages.SSH_ALGORITHM_EXCEPTION;
import static com.l7tech.gateway.common.audit.AssertionMessages.SSH_ROUTING_ERROR;
import static com.l7tech.util.ExceptionUtils.causedBy;
import static com.l7tech.util.ExceptionUtils.getDebugException;

/**
 * SFTP client for polling listener.
 */
class SftpClient implements Closeable {
    private static final Logger logger = Logger.getLogger(SftpClient.class.getName());
    private final Sftp client;
    private final SshParameters parameters;
    private final String remoteDirectory;
    private final SftpConnectionListener listener;

    SftpClient( @NotNull final SshParameters parameters,
                @NotNull final String remoteDirectory,
                @NotNull final Set<String> ciphers,
                @NotNull final SftpConnectionListener listener ) {
        this.client = new Sftp(parameters, new SftpConfiguration(new SshClientConfiguration(parameters, ciphers)));
        this.client.disableFileAccessLogging();
        this.parameters = parameters;
        this.remoteDirectory = remoteDirectory;
        this.listener = listener;
    }

    <R> R doWork( final UnaryThrows<R,Sftp,IOException> callback ) throws IOException {
        synchronized ( client ) {
            checkConnect();
            return callback.call( client );
        }
    }

    void renameFile( final String remoteFile, final String newFile ) throws IOException {
        doWork( new UnaryThrows<Long, Sftp, IOException>() {
            @Override
            public Long call( final Sftp sftp ) throws IOException {
                client.renameFile(remoteFile, newFile);
                return null;
            }
        } );
    }

    void deleteFile( final String remoteFile ) throws IOException {
        doWork( new UnaryThrows<Long, Sftp, IOException>() {
            @Override
            public Long call( final Sftp sftp ) throws IOException {
                client.deleteFile(remoteFile);
                return null;
            }
        } );
    }

    void download( final OutputStream out,
                   final String remoteDirectory,
                   final String remoteFile ) throws IOException {
        doWork( new UnaryThrows<Long, Sftp, IOException>() {
            @Override
            public Long call( final Sftp sftp ) throws IOException {
                client.setDir(remoteDirectory);
                client.download( out, remoteFile );
                return null;
            }
        } );
    }

    void upload( final InputStream in,
                 final String remoteDirectory,
                 final String remoteFile ) throws IOException {
        doWork( new UnaryThrows<Long, Sftp, IOException>() {
            @Override
            public Long call( final Sftp sftp ) throws IOException {
                client.setDir(remoteDirectory);
                client.upload( in, remoteFile );
                return null;
            }
        } );
    }

    long getFilesize( final String remoteFile ) throws IOException {
        return doWork( new UnaryThrows<Long, Sftp, IOException>() {
            @Override
            public Long call( final Sftp sftp ) throws IOException {
                return sftp.getFilesize(remoteFile);
            }
        } );
    }

    @Override
    public void close() {
        try {
            client.disconnect();
        } catch ( Exception e ) {
            logger.log(Level.WARNING, "Exception while closing SftpClient client", e);
        }
    }

    /**
     * Listener interface for connection notifications
     */
    static interface SftpConnectionListener {
        void notifyConnected();
        void notifyConnectionError( String message );
    }

    private void checkConnect() throws IOException {
        final boolean wasConnected = client.isConnected();
        boolean ok = false;
        String message = null;
        try {
            if (!wasConnected) client.connect();
            client.setDir( remoteDirectory );
            ok = true;
        } catch (SftpFileNotFoundException e) {
            message = "Directory not found.";
            throw e;
        } catch (SftpException e) {
            message = ExceptionUtils.getMessage(e);
            if ("com.jscape.inet.sftp.SftpException".equals(message)) {
                final String host = parameters.getSshHostname();
                final int port = parameters.getSshPort();
                message = "Unable to connect to " + host + ":" + port;
                throw new SftpException( message, e );
            } else if ( causedBy( e, TransportException.class ) && causedBy( e, NoSuchElementException.class ) && "no common elements found".equals(ExceptionUtils.unnestToRoot( e ).getMessage())){
                message = SSH_ALGORITHM_EXCEPTION;
                throw new SftpException( message, e );
            }
            throw e;
        } catch (IOException e) {
            message = ExceptionUtils.getMessage(e);
            throw e;
        } catch (RuntimeException e) {
            message = ExceptionUtils.getMessage(e);
            throw e;
        } finally {
            if (ok) {
                if (!wasConnected) {
                    listener.notifyConnected();
                }
            } else {
                listener.notifyConnectionError(message);
            }
        }
    }
}