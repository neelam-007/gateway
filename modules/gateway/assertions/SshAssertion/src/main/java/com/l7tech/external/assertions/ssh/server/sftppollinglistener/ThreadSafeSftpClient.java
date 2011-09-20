package com.l7tech.external.assertions.ssh.server.sftppollinglistener;

import com.jscape.inet.sftp.Sftp;
import com.jscape.inet.sftp.SftpException;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Thread safe SFTP client for polling listener.
 */
public class ThreadSafeSftpClient implements Closeable {
    private static final Logger logger = Logger.getLogger(ThreadSafeSftpClient.class.getName());
    private final Sftp client;

    public ThreadSafeSftpClient(Sftp client) {
        this.client = client;
    }

    public void setDir(String remoteDirectory) throws SftpException {
        synchronized (client) {
            client.setDir(remoteDirectory);
        }
    }

    public Enumeration getDirListing() throws IOException {
        synchronized (client) {
            return client.getDirListing();
        }
    }

    public void renameFile(String remoteFile, String newFile) throws IOException {
        synchronized (client) {
            client.renameFile(remoteFile, newFile);
        }
    }

    public void deleteFile(String remoteFile) throws IOException {
        synchronized (client) {
            client.deleteFile(remoteFile);
        }
    }

    public void download(OutputStream out, String remoteFile) throws IOException {
        synchronized (client) {
            client.download(out, remoteFile);
        }
    }

    public void upload(byte[] data, String remoteFile) throws IOException {
        synchronized (client) {
            client.upload(data, remoteFile);
        }
    }

    public long getFilesize(String remoteFile) throws IOException {
        synchronized (client) {
            return client.getFilesize(remoteFile);
        }
    }

    public boolean isConnected() throws IOException {
        synchronized (client) {
            return client.isConnected();
        }
    }

    public void connect() throws SftpException {
        synchronized (client) {
            client.connect();
        }
    }

    public void close() {
        if ( client != null ) {
            try {
                client.disconnect();
            } catch ( Exception e ) {
                logger.log(Level.WARNING, "Exception while closing ThreadSafeSftpClient client", e);
            }
        }
    }
}